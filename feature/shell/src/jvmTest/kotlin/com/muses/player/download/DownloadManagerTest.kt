package com.muses.player.download

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.muses.player.core.data.repository.*
import com.muses.player.core.download.DownloadQueueStore
import com.muses.player.core.lxsdk.*
import com.muses.player.core.lxsdk.crypto.LxCryptoJvm
import com.muses.player.core.model.*
import com.muses.player.core.model.download.*
import com.muses.player.core.model.online.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.mockwebserver.*
import okio.Buffer
import kotlin.test.*

class DownloadManagerTest {
    /** [resolveTimeoutMs] 与生产默认一致；「引擎卡死」用例传更小的值来压缩等待 */
    private class Harness(
        private val resolveTimeoutMs: Long = 120_000L,
        private val client: okhttp3.OkHttpClient = okhttp3.OkHttpClient.Builder().build(),
    ) : java.io.Closeable {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val root = Files.createTempDirectory("muses-download-test").toFile()
        val server = MockWebServer().apply { start() }
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { File(root, "queue.preferences_pb") }
        val store = DownloadQueueStore(dataStore)
        val settings = DataStoreSettingsRepository(dataStore)
        val added = mutableListOf<Song>()
        val target = DownloadTarget(DownloadTargetKind.LOCAL, "local", File(root, "target").absolutePath, "测试目录")
        val source = Source("webdav", "测试 WebDAV", SourceType.WEBDAV, server.url("/dav/").toString(), "/Music", createdAt = 0, updatedAt = 0)
        val sources = object : SourceRepository {
            override fun observeSources() = flowOf(listOf(source))
            override suspend fun getSource(id: String) = source.takeIf { id == it.id }
            override suspend fun upsert(source: Source) = Unit
            override suspend fun deleteById(id: String) = Unit
        }
        val songs = object : SongRepository {
            override fun observeSongs() = flowOf(emptyList<Song>())
            override suspend fun replaceSourceSongs(sourceId: String, songs: List<Song>): ScanMergeResult = error("不能全源替换")
            override suspend fun deleteSourceSongs(sourceId: String) = error("不能删除曲库")
            override suspend fun rebuildDerivedIndexes() = Unit
            override suspend fun getSong(id: String): Song? = null
            override suspend fun upsert(song: Song) { added += song }
        }
        val credentials = object : CredentialsRepository {
            override suspend fun savePassword(sourceId: String, password: String) = Unit
            override suspend fun getPassword(sourceId: String) = "测试密码"
            override suspend fun clearPassword(sourceId: String) = Unit
        }
        var resolves = 0
        val lx = LxScriptRepository(LxCryptoJvm())
        /** 覆盖直链解析行为（默认恒定成功），用于模拟音源后端超时/拒绝 */
        var resolveOverride: (suspend (LxQuality) -> LxMusicUrl)? = null
        val manager = DownloadManager(store, settings, sources, credentials, songs, lx,
            NoOpOnlineTrackMetadataResolver, null, object : DownloadStorage by createDownloadStorage(settings) {
                override val cacheDirectory = File(root, "cache")
            }, resolveTimeoutMs = resolveTimeoutMs, scope = scope, client = client, resolveAudio = { _, quality ->
                resolves++
                resolveOverride?.invoke(quality) ?: LxMusicUrl(server.url("/audio").toString(), LxQuality.Q_128K)
            })
        fun song(id: String = "online:kw:1") = Song(id, "online", OnlineTrackRef("kw", "{}", "online").encode(),
            "测试歌曲", "测试歌手", "测试专辑", 1000, 1, lyrics = "[00:00.00]测试歌词", sourceType = SourceType.ONLINE)
        suspend fun await(status: DownloadStatus): DownloadTask = withTimeout(15_000) {
            manager.tasks.first { list -> list.any { it.status == status } }.first { it.status == status }
        }
        override fun close() { scope.cancel(); server.close(); root.deleteRecursively() }
    }

    @Test fun enqueueDoesNotResolveOrDownloadAndDeduplicates() = runBlocking {
        Harness().use { h ->
            h.manager.enqueue(h.song()).join(); h.manager.enqueue(h.song()).join()
            assertEquals(1, h.store.load().size)
            assertEquals(DownloadStatus.WAITING, h.store.load().single().status)
            assertEquals(0, h.resolves); assertEquals(0, h.server.requestCount)
            h.settings.setDownloadPreferredQuality("flac")
            h.manager.enqueue(h.song("online:kw:2")).join()
            assertEquals(listOf("320k", "flac"), h.store.load().map { it.quality })
        }
    }

    @Test fun startEmbedsMetadataAndSavesOnlyAudio() = runBlocking {
        Harness().use { h ->
            val coverBytes = java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aWZ0AAAAASUVORK5CYII=")
            val cover = File(h.root, "cover.png").apply { writeBytes(coverBytes) }
            h.server.enqueue(MockResponse().setBody(Buffer().write(wav())))
            h.manager.enqueue(h.song().copy(coverUri = cover.toPath().toUri().toString())).join()
            h.manager.start(setOf(h.store.load().single().id), h.target).join()
            val completed = h.await(DownloadStatus.COMPLETED)
            assertEquals("128k", completed.requestedQuality)
            val directory = File(h.target.directory)
            val audio = directory.listFiles().orEmpty().single()
            assertEquals("wav", audio.extension)
            assertEquals("测试歌手 - 测试歌曲.wav", audio.name)
            val tags = com.muses.player.core.scrape.ports.JaudiotaggerTagPort.readTags(audio)!!
            assertEquals("测试歌曲", tags.title)
            assertEquals("测试歌手", tags.artist)
            assertEquals("测试专辑", tags.album)
            assertTrue(tags.lyrics!!.contains("测试歌词"))
            assertContentEquals(coverBytes, tags.cover)
            assertEquals(1, h.added.size)
            assertEquals(com.muses.player.core.media.scanner.WebDavLibraryScanner.stableSongId("local", completed.savedLocation!!), h.added.single().id)
            assertEquals(1, h.server.requestCount)
        }
    }

    @Test fun newWaitingSongIsNotStartedByExistingBatch() = runBlocking {
        Harness().use { h ->
            h.server.enqueue(MockResponse().setBody(Buffer().write(wav())).setBodyDelay(500, TimeUnit.MILLISECONDS))
            h.manager.enqueue(h.song()).join()
            val first = h.store.load().single()
            h.manager.start(setOf(first.id), h.target).join()
            h.manager.enqueue(h.song("online:kw:2")).join()
            h.await(DownloadStatus.COMPLETED)
            assertEquals(DownloadStatus.WAITING, h.store.load().last().status)
            assertEquals(1, h.resolves)
        }
    }

    @Test fun htmlResponseFailsWithoutSavingAndCanRetry() = runBlocking {
        Harness().use { h ->
            h.server.enqueue(MockResponse().setBody("<html>音源拒绝</html>"))
            h.manager.enqueue(h.song()).join()
            val id = h.store.load().single().id
            h.manager.start(setOf(id), h.target).join()
            h.await(DownloadStatus.FAILED)
            assertFalse(File(h.target.directory).exists()); assertTrue(h.added.isEmpty())
            h.server.enqueue(MockResponse().setBody(Buffer().write(wav())))
            h.manager.start(setOf(id), h.target).join()
            assertEquals(DownloadStatus.COMPLETED, h.await(DownloadStatus.COMPLETED).status)
        }
    }

    @Test fun pauseCancelsActiveNetworkAndContinueUsesValidatedRange() = runBlocking {
        Harness().use { h ->
            val payload = wav(512 * 1024)
            h.server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val offset = request.getHeader("Range")?.removePrefix("bytes=")?.removeSuffix("-")?.toInt()
                    return if (offset == null) MockResponse().setHeader("ETag", "\"test-audio\"")
                        .setBody(Buffer().write(payload)).throttleBody(16384, 100, TimeUnit.MILLISECONDS)
                    else MockResponse().setResponseCode(206).setHeader("ETag", "\"test-audio\"")
                        .setHeader("Content-Range", "bytes $offset-${payload.lastIndex}/${payload.size}")
                        .setBody(Buffer().write(payload, offset, payload.size - offset))
                }
            }
            h.manager.enqueue(h.song()).join(); val id = h.store.load().single().id
            h.manager.start(setOf(id), h.target).join()
            withTimeout(5000) { h.manager.tasks.first { it.any { t -> t.downloadedBytes > 0 } } }
            h.manager.pause(id).join()
            assertEquals(DownloadStatus.PAUSED, h.store.load().single().status)
            assertFalse(File(h.target.directory).exists())
            h.manager.start(setOf(id), h.target).join(); h.await(DownloadStatus.COMPLETED)
            h.server.takeRequest(2, TimeUnit.SECONDS)
            val resumed = h.server.takeRequest(2, TimeUnit.SECONDS)!!
            assertNotNull(resumed.getHeader("Range")); assertEquals("\"test-audio\"", resumed.getHeader("If-Range"))
        }
    }

    @Test fun restartRecoveryNeverStartsQueueAndPreservesTarget() = runBlocking {
        Harness().use { h ->
            h.manager.enqueue(h.song()).join()
            h.store.setDefaultTarget(h.target)
            h.store.update { it.map { t -> t.copy(status = DownloadStatus.DOWNLOADING, downloadedBytes = 42) } }
            val recreated = DownloadQueueStore(h.dataStore)
            recreated.recoverInterrupted()
            assertEquals(DownloadStatus.PAUSED, recreated.load().single().status)
            assertEquals(42, recreated.load().single().downloadedBytes)
            assertEquals(h.target, recreated.defaultTarget.first())
            assertEquals(0, h.server.requestCount)
        }
    }

    @Test fun uploadStreamsOnlyAudioWithoutOverwritingExistingFiles() = runBlocking {
        Harness().use { h ->
            h.server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest) = when (request.method) {
                    "GET" -> MockResponse().setBody(Buffer().write(wav()))
                    "HEAD" -> MockResponse().setResponseCode(404)
                    "PUT" -> MockResponse().setResponseCode(201)
                    else -> MockResponse().setResponseCode(405)
                }
            }
            h.manager.enqueue(h.song()).join()
            h.manager.start(setOf(h.store.load().single().id), DownloadTarget(DownloadTargetKind.WEBDAV, "webdav", "/Music", "测试 WebDAV")).join()
            val done = h.await(DownloadStatus.COMPLETED)
            assertTrue(done.savedLocation!!.contains("/dav/Music/"))
            val requests = (1..h.server.requestCount).map { h.server.takeRequest(2, TimeUnit.SECONDS)!! }
            val puts = requests.filter { it.method == "PUT" }
            assertEquals(1, puts.size)
            assertTrue(puts.all { it.getHeader("If-None-Match") == "*" && it.bodySize > 0 })
            assertEquals(1, h.added.size)
        }
    }

    @Test fun uploadWaitsForGatewayCommitBeyondDownloadReadTimeout() = runBlocking {
        val client = okhttp3.OkHttpClient.Builder().readTimeout(50, TimeUnit.MILLISECONDS).build()
        Harness(client = client).use { h ->
            h.server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest) = when (request.method) {
                    "GET" -> MockResponse().setBody(Buffer().write(wav()))
                    "HEAD" -> MockResponse().setResponseCode(404)
                    "PUT" -> MockResponse().setResponseCode(201).setHeadersDelay(300, TimeUnit.MILLISECONDS)
                    else -> MockResponse().setResponseCode(405)
                }
            }
            h.manager.enqueue(h.song()).join()
            h.manager.start(setOf(h.store.load().single().id),
                DownloadTarget(DownloadTargetKind.WEBDAV, "webdav", "/Music")).join()
            assertEquals(DownloadStatus.COMPLETED, h.await(DownloadStatus.COMPLETED).status)
            val requests = (1..h.server.requestCount).map { h.server.takeRequest(2, TimeUnit.SECONDS)!! }
            assertEquals(1, requests.count { it.method == "PUT" })
        }
    }

    @Test fun uploadResponseDisconnectConfirmsRemoteContentWithoutRepeatingPut() = runBlocking {
        Harness().use { h ->
            var uploaded: ByteArray? = null
            var audioPuts = 0
            var checks = 0
            h.server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when {
                    request.method == "HEAD" -> MockResponse().setResponseCode(404)
                    request.method == "GET" && request.path == "/audio" -> MockResponse().setBody(Buffer().write(wav()))
                    request.method == "GET" -> if (++checks == 1) MockResponse().setResponseCode(404)
                        else MockResponse().setBody(Buffer().write(uploaded!!))
                    request.method == "PUT" && request.path!!.endsWith(".wav") -> {
                        audioPuts++
                        uploaded = request.body.readByteArray()
                        MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)
                    }
                    else -> MockResponse().setResponseCode(201)
                }
            }
            h.manager.enqueue(h.song()).join()
            h.manager.start(setOf(h.store.load().single().id),
                DownloadTarget(DownloadTargetKind.WEBDAV, "webdav", "/Music")).join()
            val done = h.await(DownloadStatus.COMPLETED)
            assertEquals(1, audioPuts)
            assertEquals(2, checks)
            assertTrue(done.warnings.any { it.contains("已核验") })
            assertEquals(1, h.added.size)
        }
    }

    @Test fun uploadResponseDisconnectDoesNotAcceptDifferentContentOfSameSize() = runBlocking {
        Harness().use { h ->
            var uploaded: ByteArray? = null
            var recover = false
            h.server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when {
                    request.method == "HEAD" && recover && request.path!!.endsWith(".wav") -> MockResponse().setResponseCode(200)
                    request.method == "HEAD" -> MockResponse().setResponseCode(404)
                    request.method == "GET" && request.path == "/audio" -> MockResponse().setBody(Buffer().write(wav()))
                    request.method == "GET" && recover -> MockResponse().setBody(Buffer().write(uploaded!!))
                    request.method == "GET" -> MockResponse().setBody(Buffer().write(uploaded!!.copyOf().apply { this[0] = 0 }))
                    request.method == "PUT" && recover -> MockResponse().setResponseCode(201)
                    else -> {
                        uploaded = request.body.readByteArray()
                        MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)
                    }
                }
            }
            h.manager.enqueue(h.song()).join()
            h.manager.start(setOf(h.store.load().single().id),
                DownloadTarget(DownloadTargetKind.WEBDAV, "webdav", "/Music")).join()
            val failed = h.await(DownloadStatus.FAILED)
            assertEquals(DownloadStatus.UPLOADING, failed.failureStage)
            assertTrue(h.added.isEmpty())
            recover = true
            h.manager.start(setOf(failed.id), failed.target!!).join()
            val done = h.await(DownloadStatus.COMPLETED)
            assertTrue(done.warnings.any { it.contains("上次上传") })
        }
    }

    @Test fun uploadRedirectDoesNotCountRequestBodyTwice() = runBlocking {
        Harness().use { h ->
            h.server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest) = when (request.method) {
                    "GET" -> MockResponse().setBody(Buffer().write(wav()))
                    "HEAD" -> MockResponse().setResponseCode(404)
                    "PUT" -> if (request.path!!.startsWith("/dav/")) {
                        MockResponse().setResponseCode(307)
                            .setHeader("Location", h.server.url("/received/${request.path!!.substringAfterLast('/')}"))
                    } else MockResponse().setResponseCode(201).setHeadersDelay(300, TimeUnit.MILLISECONDS)
                    else -> MockResponse().setResponseCode(405)
                }
            }
            val progress = java.util.concurrent.CopyOnWriteArrayList<DownloadTask>()
            val observer = h.scope.launch(start = CoroutineStart.UNDISPATCHED) {
                h.manager.tasks.collect { tasks -> tasks.filter { it.status == DownloadStatus.UPLOADING }.forEach { progress += it } }
            }
            try {
                h.manager.enqueue(h.song()).join()
                h.manager.start(setOf(h.store.load().single().id),
                    DownloadTarget(DownloadTargetKind.WEBDAV, "webdav", "/Music")).join()
                val done = h.await(DownloadStatus.COMPLETED)
                val requests = (1..h.server.requestCount).map { h.server.takeRequest(2, TimeUnit.SECONDS)!! }
                val puts = requests.filter { it.method == "PUT" }
            assertEquals(2, puts.size)
                assertTrue(progress.isNotEmpty())
                assertTrue(progress.all { it.transferredBytes <= it.transferTotalBytes!! })
                assertEquals(puts.filter { it.path!!.startsWith("/received/") }.sumOf { it.bodySize }, done.transferTotalBytes)
                assertEquals(done.transferTotalBytes, done.transferredBytes)
            } finally { observer.cancelAndJoin() }
        }
    }

    @Test fun stalePartialAudioIsReDownloadedInsteadOfFailingWith416() = runBlocking {
        Harness().use { h ->
            val payload = wav()
            var uploadAllowed = false
            val seenAgent = java.util.concurrent.CopyOnWriteArrayList<String>()
            h.server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    if (request.method == "GET") seenAgent += request.getHeader("User-Agent").orEmpty()
                    return when (request.method) {
                        // 残留分片越界：续传请求会被拒绝，必须丢掉分片重下
                        "GET" -> if (request.getHeader("Range") != null) MockResponse().setResponseCode(416)
                        else MockResponse().setHeader("ETag", "\"part-1\"").setBody(Buffer().write(payload))
                        "HEAD" -> MockResponse().setResponseCode(404)
                        "PUT" -> if (uploadAllowed) MockResponse().setResponseCode(201) else MockResponse().setResponseCode(405)
                        else -> MockResponse().setResponseCode(405)
                    }
                }
            }
            val target = DownloadTarget(DownloadTargetKind.WEBDAV, "webdav", "/Music")
            h.manager.enqueue(h.song()).join()
            val id = h.store.load().single().id
            h.manager.start(setOf(id), target).join()
            h.await(DownloadStatus.FAILED)
            // 上传阶段失败后，音频分片与续传校验器都还留着，这正是重试发 416 的场景
            val part = File(File(h.root, "cache"), id + File.separator + "audio.part")
            assertEquals(payload.size.toLong(), part.length())
            // 音源直链必须带音源生态 UA：okhttp 默认 UA 会被网易 CDN 403
            assertTrue(seenAgent.any { it == "lx-music" } && seenAgent.none { it.startsWith("okhttp") })
            assertNotNull(h.store.load().single().resumeValidator)

            uploadAllowed = true
            h.manager.start(setOf(id), target).join()
            assertEquals(DownloadStatus.COMPLETED, h.await(DownloadStatus.COMPLETED).status)
        }
    }

    @Test fun filenameCannotEscapeDirectory() {
        val track = DownloadTrack("1", "online", "", "../../CON:*?", "../歌手")
        val name = downloadBaseName(track)
        assertFalse(name.contains('/')); assertFalse(name.contains('\\')); assertFalse(name.contains(':'))
        assertEquals("_CON", downloadBaseName(track.copy(title = "CON", artist = null)))
    }

    private fun wav(size: Int = 16384, sampleRate: Int = 44100): ByteArray {
        val bytes = ByteArray(size)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()); buffer.putInt(size - 8); buffer.put("WAVEfmt ".toByteArray())
        buffer.putInt(16); buffer.putShort(1); buffer.putShort(1); buffer.putInt(sampleRate); buffer.putInt(sampleRate * 2)
        buffer.putShort(2); buffer.putShort(16); buffer.put("data".toByteArray()); buffer.putInt(size - 44)
        return bytes
    }

    @Test fun localExistingAudioIsNeverOverwritten() = runBlocking {
        Harness().use { h ->
            h.manager.enqueue(h.song()).join(); val task = h.store.load().single()
            val directory = File(h.target.directory).apply { mkdirs() }
            val existing = File(directory, downloadBaseName(task.track) + ".wav").apply { writeText("用户已有文件") }
            h.server.enqueue(MockResponse().setBody(Buffer().write(wav())))
            h.manager.start(setOf(task.id), h.target).join(); h.await(DownloadStatus.FAILED)
            assertEquals("用户已有文件", existing.readText())
            assertTrue(h.added.isEmpty())
        }
    }

    @Test fun webdavSameQualityIsMarkedAndSkipped() = runBlocking {
        for (existingRate in listOf(44100, 48000)) Harness().use { h ->
            h.server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest) = when {
                    request.method == "HEAD" -> MockResponse().setResponseCode(if (request.path!!.endsWith(".wav")) 200 else 404)
                    request.method == "GET" -> MockResponse().setBody(Buffer().write(wav(sampleRate = if (request.path == "/audio") 44100 else existingRate)))
                    else -> MockResponse().setResponseCode(405)
                }
            }
            h.manager.enqueue(h.song()).join()
            h.manager.start(setOf(h.store.load().single().id), DownloadTarget(DownloadTargetKind.WEBDAV, "webdav", "/Music")).join()
            val done = h.await(DownloadStatus.COMPLETED)
            assertTrue(done.skippedExisting)
            assertTrue(done.warnings.any { it.contains("同名") })
            val requests = (1..h.server.requestCount).map { h.server.takeRequest(2, TimeUnit.SECONDS)!! }
            assertTrue(requests.none { it.method == "PUT" || it.method == "DELETE" })
            assertTrue(h.added.isEmpty())
        }
    }

    @Test fun webdavHigherActualQualityOverwritesSameFile() = runBlocking {
        Harness().use { h ->
            h.server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest) = when {
                    request.method == "HEAD" -> MockResponse().setResponseCode(if (request.path!!.endsWith(".wav")) 200 else 404).setHeader("ETag", "\"old\"")
                    request.method == "GET" -> MockResponse().setBody(Buffer().write(wav(sampleRate = if (request.path == "/audio") 44100 else 22050)))
                    request.method == "PUT" -> MockResponse().setResponseCode(201)
                    else -> MockResponse().setResponseCode(405)
                }
            }
            h.manager.enqueue(h.song()).join()
            h.manager.start(setOf(h.store.load().single().id), DownloadTarget(DownloadTargetKind.WEBDAV, "webdav", "/Music")).join()
            val done = h.await(DownloadStatus.COMPLETED)
            assertFalse(done.skippedExisting)
            val requests = (1..h.server.requestCount).map { h.server.takeRequest(2, TimeUnit.SECONDS)!! }
            val put = requests.single { it.method == "PUT" }
            assertEquals("\"old\"", put.getHeader("If-Match"))
            assertNull(put.getHeader("If-None-Match"))
            assertEquals(1, h.added.size)
        }
    }

    @Test fun completedSongCanBeRequeuedForHigherRequestedQuality() = runBlocking {
        Harness().use { h ->
            h.server.enqueue(MockResponse().setBody(Buffer().write(wav())))
            h.manager.enqueue(h.song()).join()
            h.manager.start(setOf(h.store.load().single().id), h.target).join()
            val completed = h.await(DownloadStatus.COMPLETED)
            h.settings.setDownloadPreferredQuality("flac")
            h.manager.enqueue(h.song()).join()
            val queued = h.store.load().single()
            assertEquals(DownloadStatus.WAITING, queued.status)
            assertEquals("flac", queued.quality)
            assertNotEquals(completed.id, queued.id)
            assertTrue(File(completed.savedLocation!!).isFile)
        }
    }

    @Test fun crossedLosslessMetricsAndDifferentLossyCodecsDoNotAutoUpgrade() {
        val lossless = com.muses.player.core.media.scanner.DownloadAudioQuality(true, 16, 96000, 1000, "flac")
        assertFalse(lossless.copy(bitDepth = 24, sampleRate = 44100).higherThan(lossless))
        assertFalse(lossless.copy(bitDepth = 0).higherThan(lossless))
        val lossy = lossless.copy(lossless = false, codec = "mp3", bitrate = 320)
        assertFalse(lossy.copy(codec = "aac", bitrate = 512).higherThan(lossy))
        assertTrue(lossless.higherThan(lossy))
    }

    @Test fun webdavUnknownExistingQualityIsSkipped() = runBlocking {
        Harness().use { h ->
            h.server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest) = when {
                    request.method == "HEAD" -> MockResponse().setResponseCode(if (request.path!!.endsWith(".wav")) 200 else 404)
                    request.path == "/audio" -> MockResponse().setBody(Buffer().write(wav()))
                    request.method == "GET" -> MockResponse().setBody("无法读取的音频")
                    else -> MockResponse().setResponseCode(405)
                }
            }
            h.manager.enqueue(h.song()).join()
            h.manager.start(setOf(h.store.load().single().id), DownloadTarget(DownloadTargetKind.WEBDAV, "webdav", "/Music")).join()
            assertTrue(h.await(DownloadStatus.COMPLETED).skippedExisting)
            val requests = (1..h.server.requestCount).map { h.server.takeRequest(2, TimeUnit.SECONDS)!! }
            assertTrue(requests.none { it.method == "PUT" || it.method == "DELETE" })
        }
    }

    @Test fun crossFormatUpgradeRemovesOldFileOnlyAfterSuccessfulPut() = runBlocking {
        for (putSucceeds in listOf(false, true)) Harness().use { h ->
            var saved = false
            val old = ByteBuffer.allocate(128).order(ByteOrder.BIG_ENDIAN).apply {
                put("fLaC".toByteArray()); put(0x80.toByte()); put(0); put(0); put(34)
                putShort(4096); putShort(4096); put(ByteArray(6))
                putLong((22050L shl 44) or (15L shl 36) or 16384L)
                put(ByteArray(16))
            }.array()
            h.server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest) = when {
                    request.method == "HEAD" -> MockResponse().setResponseCode(if (request.path!!.endsWith(".flac")) 200 else 404).setHeader("ETag", "\"old\"")
                    request.method == "GET" && request.path == "/audio" -> MockResponse().setBody(Buffer().write(wav()))
                    request.method == "GET" -> MockResponse().setBody(Buffer().write(old)).setHeader("ETag", "\"old\"")
                    request.method == "PUT" -> { saved = putSucceeds; MockResponse().setResponseCode(if (putSucceeds) 201 else 500) }
                    request.method == "DELETE" -> { assertTrue(saved); MockResponse().setResponseCode(204) }
                    else -> MockResponse().setResponseCode(405)
                }
            }
            h.manager.enqueue(h.song()).join()
            h.manager.start(setOf(h.store.load().single().id), DownloadTarget(DownloadTargetKind.WEBDAV, "webdav", "/Music")).join()
            val result = h.await(if (putSucceeds) DownloadStatus.COMPLETED else DownloadStatus.FAILED)
            assertFalse(result.skippedExisting)
            val requests = (1..h.server.requestCount).map { h.server.takeRequest(2, TimeUnit.SECONDS)!! }
            assertEquals(if (putSucceeds) 1 else 0, requests.count { it.method == "DELETE" })
            if (putSucceeds) assertEquals("\"old\"", requests.single { it.method == "DELETE" }.getHeader("If-Match"))
        }
    }

    @Test fun webdavReceivesEmbeddedLyricsWithoutSidecars() = runBlocking {
        Harness().use { h ->
            h.server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest) = when (request.method) {
                    "GET" -> MockResponse().setBody(Buffer().write(wav()))
                    "HEAD" -> MockResponse().setResponseCode(404)
                    "PUT" -> MockResponse().setResponseCode(if (request.path!!.endsWith(".wav")) 201 else 500)
                    else -> MockResponse().setResponseCode(405)
                }
            }
            h.manager.enqueue(h.song()).join()
            h.manager.start(setOf(h.store.load().single().id), DownloadTarget(DownloadTargetKind.WEBDAV, "webdav", "/Music")).join()
            val done = h.await(DownloadStatus.COMPLETED)
            val requests = (1..h.server.requestCount).map { h.server.takeRequest(2, TimeUnit.SECONDS)!! }
            val put = requests.filter { it.method == "PUT" }.single()
            val received = File(h.root, "received.wav").apply { writeBytes(put.body.readByteArray()) }
            val tags = com.muses.player.core.scrape.ports.JaudiotaggerTagPort.readTags(received)!!
            assertEquals("测试歌曲", tags.title)
            assertTrue(tags.lyrics!!.contains("测试歌词"))
            assertTrue(done.savedLocation!!.endsWith(".wav")); assertEquals(1, h.added.size)
        }
    }

    @Test fun completedSongIsNotEnqueuedAgainUntilRemoved() = runBlocking {
        Harness().use { h ->
            h.server.enqueue(MockResponse().setBody(Buffer().write(wav())))
            h.manager.enqueue(h.song()).join()
            val id = h.store.load().single().id
            h.manager.start(setOf(id), h.target).join()
            h.await(DownloadStatus.COMPLETED)
            // 已下载过：再次添加不产生新任务，也不重新发起解析
            h.manager.enqueue(h.song()).join()
            assertEquals(1, h.store.load().size)
            assertEquals(1, h.resolves)
            // 移出队列后可以重新添加（用户显式要求重下）
            h.manager.remove(id).join()
            h.manager.enqueue(h.song()).join()
            assertEquals(1, h.store.load().size)
            assertEquals(DownloadStatus.WAITING, h.store.load().single().status)
        }
    }

    @Test fun resolveFailureFallsBackToLowerQuality() = runBlocking {
        Harness().use { h ->
            // 模拟截图里的情形：脚本后端超时。档位回退由仓库内部完成，这里要求失败原因可直接读
            h.resolveOverride = { quality ->
                if (quality.rank >= LxQuality.FLAC.rank) {
                    throw IllegalStateException("后端聚合失败: Request timeout has expired [url=https://yy.example/lx/api/]")
                }
                LxMusicUrl(h.server.url("/audio").toString(), quality)
            }
            h.settings.setDownloadPreferredQuality(LxQuality.FLAC_24BIT.key)
            h.server.enqueue(MockResponse().setBody(Buffer().write(wav())))
            h.manager.enqueue(h.song()).join()
            h.manager.start(setOf(h.store.load().single().id), h.target).join()
            val failed = h.await(DownloadStatus.FAILED)
            assertEquals("下载失败：音源响应超时，请稍后重试或改用较低音质", failed.error)
            assertEquals(1, h.resolves)
            assertTrue(h.added.isEmpty())
        }
    }

    @Test fun wedgedResolverFailsInsteadOfHangingForever() = runBlocking {
        Harness(resolveTimeoutMs = 500).use { h ->
            // 脚本引擎卡住不再回调：任务要落到「失败 + 可重试」，不能永远停在准备下载
            h.resolveOverride = { kotlinx.coroutines.awaitCancellation() }
            h.manager.enqueue(h.song()).join()
            h.manager.start(setOf(h.store.load().single().id), h.target).join()
            val failed = h.await(DownloadStatus.FAILED)
            assertEquals("下载失败：音源响应超时，请稍后重试或改用较低音质", failed.error)
            assertEquals(0, h.server.requestCount)
        }
    }

    @Test fun customDeviceDirectoryIsUsedForDeviceTarget() = runBlocking {
        Harness().use { h ->
            val custom = File(h.root, "自定义下载目录")
            h.settings.setDownloadDeviceDirectory(custom.absolutePath)
            h.server.enqueue(MockResponse().setBody(Buffer().write(wav())))
            h.manager.enqueue(h.song()).join()
            h.manager.start(setOf(h.store.load().single().id), DownloadTarget()).join()
            h.await(DownloadStatus.COMPLETED)
            assertTrue(custom.listFiles().orEmpty().any { it.extension == "wav" })
            assertEquals("wav", custom.listFiles().orEmpty().single().extension)
            // 自定义目录不是音源，不写入曲库
            assertTrue(h.added.isEmpty())
        }
    }

    @Test fun deletedDownloadedFileAllowsReenqueue() = runBlocking {
        Harness().use { h ->
            val custom = File(h.root, "device-dir")
            h.settings.setDownloadDeviceDirectory(custom.absolutePath)
            h.server.enqueue(MockResponse().setBody(Buffer().write(wav())))
            h.manager.enqueue(h.song()).join()
            val first = h.store.load().single().id
            h.manager.start(setOf(first), DownloadTarget()).join()
            val done = h.await(DownloadStatus.COMPLETED)

            // 文件还在：再次添加被拒绝，队列不变
            h.manager.enqueue(h.song()).join()
            assertEquals(1, h.store.load().size)
            assertEquals(first, h.store.load().single().id)

            // 用户把文件删了：旧记录被替换，可以重新下载
            assertTrue(File(done.savedLocation!!).delete())
            h.server.enqueue(MockResponse().setBody(Buffer().write(wav())))
            h.manager.enqueue(h.song()).join()
            assertEquals(1, h.store.load().size)
            assertNotEquals(first, h.store.load().single().id)
            assertEquals(DownloadStatus.WAITING, h.store.load().single().status)
        }
    }

    @Test fun deletedWebdavFileAllowsReenqueue() = runBlocking {
        Harness().use { h ->
            h.server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest) = when (request.method) {
                    "GET" -> MockResponse().setBody(Buffer().write(wav()))
                    "HEAD" -> MockResponse().setResponseCode(404)
                    "PUT" -> MockResponse().setResponseCode(201)
                    else -> MockResponse().setResponseCode(405)
                }
            }
            val target = DownloadTarget(DownloadTargetKind.WEBDAV, "webdav", "/Music", "测试 WebDAV")
            h.manager.enqueue(h.song()).join()
            val first = h.store.load().single().id
            h.manager.start(setOf(first), target).join()
            h.await(DownloadStatus.COMPLETED)

            // 远端查不到该文件（HEAD 404）时允许重新入队
            h.manager.enqueue(h.song()).join()
            assertEquals(1, h.store.load().size)
            assertNotEquals(first, h.store.load().single().id)
            assertEquals(DownloadStatus.WAITING, h.store.load().single().status)
        }
    }
}

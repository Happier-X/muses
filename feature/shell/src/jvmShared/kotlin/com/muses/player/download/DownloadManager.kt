package com.muses.player.download

import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.data.repository.SettingsRepository
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.data.repository.SourceRepository
import com.muses.player.core.data.log.ErrorLogStore
import com.muses.player.core.media.scanner.DownloadAudioQuality
import com.muses.player.core.download.DownloadQueueStore
import com.muses.player.core.lxsdk.LxQuality
import com.muses.player.core.lxsdk.LxScriptRepository
import com.muses.player.core.lyrics.LyricsMatcher
import com.muses.player.core.lyrics.matchDocument
import com.muses.player.core.lyrics.parser.LxLyricParser
import com.muses.player.core.model.Song
import com.muses.player.core.model.SourceType
import com.muses.player.core.model.download.*
import com.muses.player.core.model.online.OnlineTrackMetadataResolver
import com.muses.player.core.model.online.OnlineTrackRef
import com.muses.player.core.model.scrape.ScrapeChanges
import com.muses.player.core.scrape.ports.JaudiotaggerTagPort
import com.muses.player.core.ui.components.MusesSnackbar
import com.muses.player.core.util.RefreshableState
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.select
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okio.BufferedSink

class DownloadManager(
    private val store: DownloadQueueStore,
    private val settings: SettingsRepository,
    private val sources: SourceRepository,
    private val credentials: CredentialsRepository,
    private val songs: SongRepository,
    private val lx: LxScriptRepository,
    private val metadata: OnlineTrackMetadataResolver,
    private val lyricsMatcher: LyricsMatcher?,
    private val storage: DownloadStorage,
    /**
     * 单次「解析直链」的等待上限。
     * 音源脚本引擎在极端情况下会卡住不再回调（实测：脚本后端超时后，后续请求一直停在准备下载），
     * 这里兜一道硬上限，让任务落到「失败 + 可重试」，而不是永远转圈。
     */
    private val resolveTimeoutMs: Long = 120_000L,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).callTimeout(10, TimeUnit.MINUTES).build(),
    private val runningChanged: (Boolean) -> Unit = {},
    private val urlProbe: com.muses.player.core.model.online.OnlinePlayableUrlProbe? = null,
    private val errorLog: ErrorLogStore? = null,
    private val resolveAudio: suspend (OnlineTrackRef, LxQuality) -> com.muses.player.core.lxsdk.LxMusicUrl = { ref, quality ->
        lx.resolveMusicUrlInfo(ref.platform, ref.musicInfoJson, quality, downloadFallback = true,
            acceptUrl = { url -> urlProbe == null || withTimeoutOrNull(4_000) { urlProbe.canOpen(url) } == true })
    },
    private val uploadConfirmDelayMs: Long = 30_000L,
) {
    // WebDAV 使用 HTTP/1.1，提高部分 Android / 代理组合上传时的连接兼容性。
    private val webDavClient = client.newBuilder().protocols(listOf(Protocol.HTTP_1_1))
        // PUT 可能已经落盘，断连后先核验；禁止底层在未确认结果时自动重发。
        .retryOnConnectionFailure(false)
        // 文件查询与核验不沿用上传的长等待，避免一次 HEAD / GET 阻塞队列 15 分钟。
        .readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS).build()
    private val webDavUploadClient = webDavClient.newBuilder()
        // 网关接收完请求体后仍需完成上游保存，单独保留 PUT 的长等待。
        .readTimeout(15, TimeUnit.MINUTES)
        .callTimeout(15, TimeUnit.MINUTES).build()
    private val ready = scope.async { store.recoverInterrupted() }
    private val taskState = RefreshableState(store.tasks, scope, emptyList(), SharingStarted.Eagerly)
    val tasks = taskState.state
    private val sourceState = RefreshableState(sources.observeSources(), scope, emptyList(), SharingStarted.Eagerly)
    val availableSources = sourceState.state
    val defaultTarget = store.defaultTarget.stateIn(scope, SharingStarted.Eagerly, DownloadTarget())
    fun setDefaultTarget(target: DownloadTarget) = scope.launch { store.setDefaultTarget(target) }

    /** 只重读队列和保存位置，不启动、暂停或恢复下载。 */
    suspend fun refresh() {
        ready.await()
        taskState.refresh()
        sourceState.refresh()
    }
    private var batch: Job? = null
    private var active: Job? = null
    private var activeId: String? = null
    private val pendingStarts = mutableSetOf<String>()
    private val schedulerLock = Any()

    fun enqueue(song: Song) = scope.launch {
        ready.await()
        if (OnlineTrackRef.parse(song.path) == null) { MusesSnackbar.show("仅支持下载 LX 音源歌曲"); return@launch }
        // 已经下过、且文件还在的曲子不再重复入队；文件已被删掉则替换旧记录重新下
        val existing = store.load().firstOrNull { it.track.id == song.id }
        when {
            existing == null -> enqueueNew(song)
            existing.status != DownloadStatus.COMPLETED -> MusesSnackbar.show("歌曲已在下载队列中")
            savedDownloadStillExists(existing) &&
                (LxQuality.fromKey(settings.downloadPreferredQuality.first())?.rank ?: 0) <=
                (LxQuality.fromKey(existing.quality)?.rank ?: 0) ->
                MusesSnackbar.show("这首歌已经下载过了")
            else -> {
                // 文件已不存在或用户选择了更高档位，重新入队；是否能升级仍以实际文件音质为准。
                store.update { list -> list.filterNot { task -> task.id == existing.id } }
                enqueueNew(song)
            }
        }
    }

    private suspend fun enqueueNew(song: Song) {
        store.enqueue(DownloadTask(UUID.randomUUID().toString(), DownloadTrack.from(song), settings.downloadPreferredQuality.first()))
        MusesSnackbar.show("添加成功")
    }

    /** 已完成任务的产物是否还在；WebDAV 用一次 HEAD 问远端。 */
    private suspend fun savedDownloadStillExists(task: DownloadTask): Boolean {
        val location = task.savedLocation ?: return false
        if (!location.startsWith("http://") && !location.startsWith("https://")) return storage.exists(task)
        val source = availableSources.value.firstOrNull { source ->
            source.type == SourceType.WEBDAV && location.startsWith(source.url.orEmpty().trimEnd('/'))
        } ?: return true
        val authorization = Credentials.basic(source.username.orEmpty(), credentials.getPassword(source.id).orEmpty())
        return runCatching {
            network(Request.Builder().url(location.toHttpUrl()).header("Authorization", authorization).head().build(), webDavClient) { response ->
                response.code != 404 && response.code != 410
            }
        }.getOrDefault(true)
    }

    fun start(ids: Set<String>, defaultTarget: DownloadTarget) = scope.launch {
        ready.await()
        store.update { list -> list.map { if (it.id in ids && !it.status.active && it.status != DownloadStatus.COMPLETED)
            it.copy(target = it.target ?: defaultTarget, error = null) else it } }
        synchronized(schedulerLock) {
            pendingStarts.addAll(ids)
            if (batch?.isActive != true) {
                try { runningChanged(true) } catch (_: Exception) {
                    pendingStarts.clear()
                    MusesSnackbar.show("无法启动后台下载服务，请保持应用在前台后重试")
                    return@synchronized
                }
                batch = scope.launch {
                    while (true) {
                        val snapshot = store.load()
                        val next = synchronized(schedulerLock) {
                            val id = pendingStarts.firstOrNull()
                            if (id == null) { batch = null; runningChanged(false); null }
                            else {
                                pendingStarts.remove(id)
                                val task = snapshot.firstOrNull { it.id == id && !it.status.active && it.status != DownloadStatus.COMPLETED }
                                val job = launch(start = CoroutineStart.LAZY) { if (task != null) execute(task) }
                                activeId = id
                                active = job
                                job
                            }
                        } ?: break
                        next.start()
                        next.join()
                        synchronized(schedulerLock) { if (active === next) { activeId = null; active = null } }
                    }
                }
            }
        }
    }

    fun pause(id: String? = null) = scope.launch {
        ready.await()
        val stopping = synchronized(schedulerLock) {
            if (id == null) pendingStarts.clear() else pendingStarts.remove(id)
            if (id == null || activeId == id) active?.also { it.cancel() } else null
        }
        stopping?.join()
        store.update { list -> list.map { if ((id == null || it.id == id) && it.status != DownloadStatus.COMPLETED && it.status != DownloadStatus.FAILED)
            it.copy(status = DownloadStatus.PAUSED) else it } }
    }

    fun remove(id: String) = scope.launch {
        pause(id).join()
        store.update { it.filterNot { task -> task.id == id } }
    }

    fun configure(id: String, target: DownloadTarget? = null, quality: String? = null) = scope.launch {
        ready.await()
        store.update { it.map { task -> if (task.id == id && !task.status.active && task.status != DownloadStatus.COMPLETED)
            task.copy(target = target ?: task.target, quality = quality ?: task.quality) else task } }
    }

    private suspend fun change(id: String, transform: (DownloadTask) -> DownloadTask) = store.update {
        it.map { task -> if (task.id == id) transform(task) else task }
    }

    private suspend fun execute(task: DownloadTask) {
        try {
            change(task.id) { it.copy(status = DownloadStatus.PREPARING,
                transferredBytes = 0, transferTotalBytes = null, warnings = emptyList(), error = null, failureStage = null, skippedExisting = false, transferMessage = null) }
            val ref = OnlineTrackRef.parse(task.track.reference) ?: error("在线歌曲信息无效，请重新加入队列")
            val requested = LxQuality.fromKey(task.quality) ?: LxQuality.DEFAULT
            val directory = File(storage.cacheDirectory, task.id).apply { mkdirs() }
            val scratch = File(directory, "audio.part")
            downloadAudio(task.id, ref, requested, scratch)
            val extension = detectAudioExtension(scratch) ?: error("返回内容不是支持的音频格式，未保存到目标")
            val base = downloadBaseName(task.track)
            val audio = File(directory, "$base.$extension")
            scratch.copyTo(audio, overwrite = true)
            change(task.id) { it.copy(status = DownloadStatus.METADATA) }
            val warnings = mutableListOf<String>()
            val (lyrics, cover) = collectMetadata(task, ref, warnings)
            val tags = JaudiotaggerTagPort.writeTags(audio, ScrapeChanges(title = task.track.title,
                artist = task.track.artist, album = task.track.album, lyrics = lyrics), cover)
            check(tags.ok) { "歌曲信息内嵌失败，音频尚未保存，请重试或更换音质" }
            val files = listOf(audio)
            val target = task.target ?: error("请先选择保存位置")
            val total = files.sumOf { it.length() }
            change(task.id) { it.copy(status = if (target.kind == DownloadTargetKind.WEBDAV) DownloadStatus.UPLOADING else DownloadStatus.SAVING,
                transferredBytes = 0, transferTotalBytes = total, warnings = warnings,
                transferMessage = if (target.kind == DownloadTargetKind.WEBDAV) "检查目标文件" else null) }
            val recoverUpload = task.target == target && task.transferTotalBytes != null &&
                task.transferredBytes >= audio.length()
            val uploaded = if (target.kind == DownloadTargetKind.WEBDAV) upload(files, target, task.id, recoverUpload) else null
            // 提交阶段不可取消：否则暂停会留下已落盘的音频，却把任务标成失败。
            withContext(NonCancellable) {
                val saved = uploaded ?: storage.save(files, target) { bytes, size ->
                    change(task.id) { it.copy(transferredBytes = bytes, transferTotalBytes = size) }
                }
                warnings.addAll(saved.warnings)
                if (!saved.skippedExisting) syncLibrary(target, saved, task, lyrics, warnings)
                change(task.id) {
                    it.copy(
                        status = DownloadStatus.COMPLETED,
                        savedLocation = saved.location,
                        warnings = warnings,
                        transferredBytes = total,
                        resumeValidator = null,
                        skippedExisting = saved.skippedExisting,
                        transferMessage = null,
                    )
                }
            }
            // 结算后回收本任务在私有缓存中的临时文件。
            scratch.delete()
            files.forEach { it.delete() }
        } catch (e: CancellationException) {
            withContext(NonCancellable) { change(task.id) { if (it.status == DownloadStatus.COMPLETED) it else it.copy(status = DownloadStatus.PAUSED, error = null) } }
            throw e
        } catch (e: Exception) {
            val stage = store.load().firstOrNull { it.id == task.id }?.status
            val message = when (e) {
                is java.net.SocketTimeoutException -> if (stage == DownloadStatus.UPLOADING)
                    "WebDAV 响应超时，尚未确认远端保存结果，请稍后重试" else "连接超时，请重试"
                is java.net.UnknownHostException -> "网络不可用，请检查连接后重试"
                is javax.net.ssl.SSLException -> if (stage == DownloadStatus.UPLOADING)
                    "WebDAV 上传连接中断，尚未确认远端文件完整，请重试" else
                    "安全连接中断，请检查网络或代理后重试"
                is java.io.IOException -> "网络或文件写入失败，请检查连接和保存权限"
                else -> downloadFailureMessage(e)
            }
            // 不记录地址、认证信息或脚本响应，只记录异常类型和调用位置。
            val diagnostic = generateSequence(e as Throwable) { it.cause }.joinToString("\n") { cause ->
                val detail = if (cause is javax.net.ssl.SSLException || cause is java.net.SocketTimeoutException)
                    cause.message.orEmpty().replace(Regex("https?://\\S+"), "[地址已隐藏]") else ""
                cause.javaClass.name + (if (detail.isNotBlank()) ": $detail\n" else "\n") +
                    cause.stackTrace.joinToString("\n") { "    at $it" }
            }
            errorLog?.log(ErrorLogStore.Level.ERROR, "Download", "阶段=$stage；$message\n$diagnostic")
            change(task.id) { it.copy(status = DownloadStatus.FAILED, error = message, failureStage = stage) }
        }
    }

    /**
     * 逐档抓取音频：先按用户在队列里选定的档位，地址失效或内容不是音频时**只向更低档位**回退。
     * 不静默升档——更高档位可能返回加密容器或远超预期的体积。
     */
    private suspend fun downloadAudio(
        taskId: String,
        ref: OnlineTrackRef,
        requested: LxQuality,
        scratch: File,
    ): com.muses.player.core.lxsdk.LxMusicUrl {
        var candidate: LxQuality? = requested
        var lastError: Exception? = null
        while (candidate != null) {
            val asking = candidate
            // 档位与多脚本回退由仓库内部完成（downloadFallback）；这一层硬上限只防「脚本引擎卡住不回调」
            val url = try {
                withTimeout(resolveTimeoutMs) { resolveAudio(ref, asking) }
            } catch (e: TimeoutCancellationException) {
                // 用户暂停时外层 Job 已取消，不能把取消改写成失败
                currentCoroutineContext().ensureActive()
                throw IllegalStateException("音源无响应（等待超时），请稍后重试或更换音源脚本")
            }
            change(taskId) { it.copy(requestedQuality = url.quality?.key, status = DownloadStatus.DOWNLOADING) }
            try {
                fetchAudio(taskId, url, scratch)
                return url
            } catch (e: CancellationException) {
                throw e
            } catch (e: DownloadQualityUnavailable) {
                // 直链拿到了但内容不可用：换更低档位再来一次
                lastError = e
                candidate = LxQuality.ordered().lastOrNull { it.rank < (url.quality ?: asking).rank }
            }
        }
        throw lastError ?: IllegalStateException("音源没有可用的下载档位")
    }

    /** 单次音频抓取（音源支持时断点续传）；失败由 [downloadAudio] 决定是否降档重试。 */
    private suspend fun fetchAudio(taskId: String, resolved: com.muses.player.core.lxsdk.LxMusicUrl, scratch: File) {
        val current = store.load().firstOrNull { it.id == taskId }
        val validator = current?.resumeValidator
        val resume = if (validator != null && current.requestedQuality == resolved.quality?.key) scratch.length() else 0L
        try {
            fetchAudioPart(taskId, resolved, scratch, if (resume > 0) validator else null, resume)
        } catch (e: StaleDownloadPart) {
            // 残留分片越界（上一轮音频其实已下完，失败发生在上传等后续阶段）：丢掉分片，从头再取一次
            scratch.delete()
            change(taskId) { it.copy(downloadedBytes = 0, totalBytes = null, resumeValidator = null) }
            fetchAudioPart(taskId, resolved, scratch, null, 0L)
        }
    }

    private suspend fun fetchAudioPart(
        taskId: String,
        resolved: com.muses.player.core.lxsdk.LxMusicUrl,
        scratch: File,
        validator: String?,
        resume: Long,
    ) {
        // 音源侧直链（网易等 CDN）会把 okhttp 的默认 UA 当爬虫直接 403，必须带音源生态自己的 UA
        val request = Request.Builder().url(resolved.url.toHttpUrl())
            .header("Accept-Encoding", "identity")
            .header("User-Agent", AUDIO_USER_AGENT)
            .apply {
            if (resume > 0) {
                header("Range", "bytes=$resume-")
                header("If-Range", validator!!)
            }
        }.build()
        network(request) { response ->
            // 416：本地分片已经到文件末尾，续传范围不可能满足，交给外层丢掉分片重下
            if (response.code == 416) throw StaleDownloadPart()
            if (response.code == 403 || response.code == 404 || response.code == 410) {
                throw DownloadQualityUnavailable("下载地址不可用：HTTP ${response.code}")
            }
            check(response.isSuccessful) { "HTTP ${response.code}" }
            val body = response.body
            val append = response.code == 206 && resume > 0
            if (response.code == 206) {
                check(response.header("Content-Range")?.startsWith("bytes $resume-") == true) {
                    "音源返回的续传位置无效，请重试"
                }
            }
            val offset = if (append) resume else 0L
            val length = body.contentLength().takeIf { it >= 0 }?.plus(offset)
            val newValidator = response.header("ETag")?.takeUnless { it.startsWith("W/") }
                ?: response.header("Last-Modified")
            change(taskId) { it.copy(totalBytes = length, downloadedBytes = offset, resumeValidator = newValidator) }
            var downloaded = offset
            var reported = 0L
            body.byteStream().use { input ->
                FileOutputStream(scratch, append).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        check(downloaded <= 4L * 1024 * 1024 * 1024) { "音频文件超过支持的大小" }
                        val now = System.nanoTime()
                        if (now - reported > 250_000_000) {
                            change(taskId) { it.copy(downloadedBytes = downloaded) }
                            reported = now
                        }
                    }
                }
            }
            check(downloaded > 0 && (length == null || downloaded == length)) { "音频文件未完整下载，请重试" }
            change(taskId) { it.copy(downloadedBytes = downloaded) }
        }
    }

    /**
     * 存进本地/WebDAV 音源后立即入库，用户不必等下一次扫描。
     * 封面留给扫描器或播放时懒扫描补齐——这里写不了曲库用的封面缓存，硬塞缓存路径会被系统清理后留下坏图。
     */
    private suspend fun syncLibrary(
        target: DownloadTarget,
        saved: SavedDownload,
        task: DownloadTask,
        lyrics: String?,
        warnings: MutableList<String>,
    ) {
        val sourceId = target.sourceId
        if (target.kind == DownloadTargetKind.DEVICE || sourceId == null) return
        val stable = java.security.MessageDigest.getInstance("SHA-256")
            .digest("$sourceId|${saved.physicalPath ?: saved.location}".toByteArray())
            .joinToString("") { "%02x".format(it) }
        try {
            songs.upsert(
                Song(
                    stable, sourceId, saved.location, task.track.title, task.track.artist,
                    task.track.album, task.track.durationMs, task.track.durationMs / 1000, lyrics = lyrics,
                    sourceType = if (target.kind == DownloadTargetKind.WEBDAV) SourceType.WEBDAV else SourceType.LOCAL,
                ),
            )
            songs.rebuildDerivedIndexes()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            warnings += "文件已保存，曲库同步失败，请重新扫描音源"
        }
    }

    private suspend fun collectMetadata(task: DownloadTask, ref: OnlineTrackRef, warnings: MutableList<String>): Pair<String?, ByteArray?> {
        var lyrics = task.track.lyrics
        var coverUrl = task.track.coverUri
        try {
            val raw = withTimeoutOrNull(12_000) { metadata.resolveLyrics(ref) }
            val script = raw?.let { LxLyricParser.parse(it.lyric, it.tlyric, it.rlyric, it.lxlyric) }
            val doc = if (script?.lines?.any { it.syllables.isNotEmpty() } == true) script else
                withTimeoutOrNull(20_000) { lyricsMatcher?.matchDocument(task.track.id, task.track.title,
                    task.track.artist, task.track.album, task.track.durationMs) } ?: script
            if (doc != null) lyrics = doc.lines.filter { it.timeMs >= 0 }.joinToString("\n") { line ->
                val time = "[%02d:%02d.%02d]".format(line.timeMs / 60000, line.timeMs / 1000 % 60, line.timeMs / 10 % 100)
                time + line.text + (line.translation?.takeIf { it.isNotBlank() }?.let { "\n$time$it" } ?: "")
            }
        } catch (e: CancellationException) { throw e } catch (_: Exception) { warnings += "歌词获取失败" }
        if (lyrics.isNullOrBlank()) warnings += "未找到歌词"
        try { coverUrl = withTimeoutOrNull(8000) { metadata.resolveCover(ref) }?.takeIf { it.isNotBlank() } ?: coverUrl }
        catch (e: CancellationException) { throw e } catch (_: Exception) { }
        var cover: ByteArray? = null
        try {
            if (!coverUrl.isNullOrBlank()) {
                cover = if (coverUrl!!.startsWith("file://")) File(java.net.URI(coverUrl)).inputStream().use { it.readBounded(8 * 1024 * 1024) }
                else network(Request.Builder().url(coverUrl!!.toHttpUrl()).build()) { response ->
                    check(response.isSuccessful)
                    response.body?.byteStream()?.use { it.readBounded(8 * 1024 * 1024) }
                }
            }
        } catch (e: CancellationException) { throw e } catch (_: Exception) { }
        if (cover == null) warnings += "未获取到封面"
        return lyrics to cover
    }

    private suspend fun upload(files: List<File>, target: DownloadTarget, taskId: String, recoverUpload: Boolean): SavedDownload {
        val source = target.sourceId?.let { sources.getSource(it) } ?: error("WebDAV 音源已不存在")
        val base = source.url?.toHttpUrl() ?: error("WebDAV 地址无效")
        check(source.type == SourceType.WEBDAV) { "保存目标不是 WebDAV 音源" }
        val authorization = Credentials.basic(source.username.orEmpty(), credentials.getPassword(source.id) ?: error("WebDAV 密码不存在，请重新配置该音源"))
        val parts = target.directory.split('/').filter { it.isNotBlank() }
        require(parts.none { it == "." || it == ".." }) { "保存目录无效" }
        val directory = base.newBuilder().apply {
            if (base.pathSegments.lastOrNull() != "") addPathSegment("")
            parts.forEach { addPathSegment(it) }
            if (parts.isNotEmpty()) addPathSegment("")
        }.build()
        var transferred = 0L
        var audioLocation = ""
        val warnings = mutableListOf<String>()
        val job = currentCoroutineContext().job
        for ((index, file) in files.withIndex()) {
            try {
            val url = directory.newBuilder().addPathSegment(file.name).build()
            val head = Request.Builder().url(url).header("Authorization", authorization).head().build()
            val existing = network(head, webDavClient) { response ->
                check(response.code == 404 || response.isSuccessful) { "无法检查目标文件：HTTP ${response.code}" }
                if (response.isSuccessful) RemoteDownloadFile(url, response.header("ETag"), response.header("Last-Modified")) else null
            }
            if (existing != null && index == 0 && recoverUpload && withTimeoutOrNull(90_000) {
                    reportTransferMessage(taskId, "核验上次上传")
                    confirmUploadedFile(url, authorization, file)
                } == true) {
                transferred += file.length()
                audioLocation = url.toString()
                reportTransferred(taskId, transferred)
                warnings += "已核验上次上传的音频"
                continue
            }
            val collisions = mutableListOf<RemoteDownloadFile>()
            existing?.let { collisions += it }
            // 同一歌手和歌名的不同音频格式也属于同名歌曲，不能另存一份低音质版本。
            for (extension in listOf("mp3", "flac", "m4a", "wav", "aac", "ogg", "opus", "ape", "aiff", "wma")) {
                if (extension == file.extension.lowercase()) continue
                val alternate = directory.newBuilder().addPathSegment("${file.nameWithoutExtension}.$extension").build()
                val found = network(Request.Builder().url(alternate).header("Authorization", authorization).head().build(), webDavClient) {
                    check(it.code == 404 || it.isSuccessful) { "无法检查同名文件：HTTP ${it.code}" }
                    if (it.isSuccessful) RemoteDownloadFile(alternate, it.header("ETag"), it.header("Last-Modified")) else null
                }
                found?.let { collisions += it }
            }
            if (collisions.isNotEmpty()) {
                val incoming = DownloadAudioQuality.read(file)
                for (collision in collisions) {
                    val previous = remoteAudioQuality(collision, authorization, file.parentFile)
                    if (incoming == null || previous == null || !incoming.higherThan(previous)) {
                        val reason = if (incoming == null || previous == null) "无法确认音质" else "已有文件音质相同、更高或无法确认升级"
                        return SavedDownload(collision.url.toString(), warnings = listOf("同名歌曲已存在，$reason，已跳过上传"), skippedExisting = true)
                    }
                }
                warnings += "发现同名歌曲，新文件音质更高，已替换"
            }
            val completedBytes = transferred
            val bodySent = CompletableDeferred<Unit>()
            val body = object : RequestBody() {
                override fun contentType() = "application/octet-stream".toMediaType()
                override fun contentLength() = file.length()
                override fun writeTo(sink: BufferedSink) {
                    runBlocking { reportTransferMessage(taskId, null) }
                    var last = 0L
                    // 重定向或网络重试会再次写入同一个请求体，只统计本次文件位置。
                    var fileTransferred = 0L
                    file.inputStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            job.ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            sink.write(buffer, 0, count); fileTransferred += count
                            val now = System.nanoTime()
                            if (now - last > 250_000_000) { runBlocking { reportTransferred(taskId, completedBytes + fileTransferred) }; last = now }
                        }
                    }
                    runBlocking { reportTransferred(taskId, completedBytes + fileTransferred) }
                    sink.flush()
                    bodySent.complete(Unit)
                    runBlocking { reportTransferMessage(taskId, "等待服务器保存") }
                }
            }
            try {
                val put = Request.Builder().url(url).header("Authorization", authorization).apply {
                    if (existing == null) header("If-None-Match", "*")
                    else {
                        existing.etag?.let { header("If-Match", it) }
                        existing.lastModified?.let { header("If-Unmodified-Since", it) }
                    }
                }.put(body).build()
                val verified = supervisorScope {
                    val uploading = async {
                        network(put, webDavUploadClient) {
                            check(it.isSuccessful) {
                                if (it.code == 423) "服务器锁定了目标文件（HTTP 423），请等待服务器任务结束后重试"
                                else "上传失败：HTTP ${it.code}，已保存的文件不会被删除"
                            }
                        }
                        false
                    }
                    val verifying = async {
                        bodySent.await()
                        while (true) {
                            delay(uploadConfirmDelayMs)
                            reportTransferMessage(taskId, "核验服务器保存结果")
                            val confirmed = try {
                                withTimeoutOrNull(90_000) { confirmUploadedFile(url, authorization, file) } == true
                            } catch (e: CancellationException) { throw e }
                            catch (_: Exception) { false }
                            if (confirmed) return@async true
                            reportTransferMessage(taskId, "等待服务器保存")
                        }
                        @Suppress("UNREACHABLE_CODE")
                        false
                    }
                    try {
                        select<Boolean> {
                            uploading.onAwait { it }
                            verifying.onAwait { it }
                        }
                    } finally {
                        uploading.cancel()
                        verifying.cancel()
                    }
                }
                if (verified) warnings += "服务器响应较慢，已核验远端文件完整"
            } catch (e: java.io.IOException) {
                reportTransferMessage(taskId, "核验远端文件")
                // PUT 已发送完但响应断连时，读回文件逐字节核验；不能仅凭大小认定成功，也不能盲目重传。
                val confirmed = bodySent.isCompleted && withTimeoutOrNull(90_000) {
                    confirmAfterDisconnect(url, authorization, file)
                } == true
                if (!confirmed) throw e
                warnings += "上传响应中断，已核验远端文件完整"
            }
            // 用户授权的跨格式升级：仅在新音频完整保存后清理对应的低音质旧文件。
            for (collision in collisions.filter { it.url != url }) {
                try {
                    val unchanged = network(Request.Builder().url(collision.url).header("Authorization", authorization).head().build(), webDavClient) {
                        it.isSuccessful && when {
                            collision.etag != null -> it.header("ETag") == collision.etag
                            collision.lastModified != null -> it.header("Last-Modified") == collision.lastModified
                            else -> false
                        }
                    }
                    check(unchanged) { "无法确认旧文件仍未变化" }
                    val remove = Request.Builder().url(collision.url).header("Authorization", authorization).apply {
                        collision.etag?.let { header("If-Match", it) }
                        collision.lastModified?.let { header("If-Unmodified-Since", it) }
                    }.delete().build()
                    network(remove, webDavClient) { check(it.isSuccessful || it.code == 404) { "旧文件替换失败" } }
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { warnings += "高音质文件已保存，低音质旧文件未移除，请检查同名文件" }
            }
            transferred = completedBytes + file.length()
            if (index == 0) audioLocation = url.toString()
            } catch (e: CancellationException) {
                if (index == 0) throw e
                warnings += "部分歌曲信息未上传，音频已保存"
                break
            } catch (e: Exception) {
                if (index == 0) throw e
                warnings += "${file.extension} 附属信息上传失败，音频已保存"
            }
        }
        return SavedDownload(audioLocation, warnings = warnings)
    }

    private data class RemoteDownloadFile(val url: HttpUrl, val etag: String?, val lastModified: String?)

    private suspend fun remoteAudioQuality(remote: RemoteDownloadFile, authorization: String, directory: File): DownloadAudioQuality? {
        val extension = remote.url.pathSegments.last().substringAfterLast('.')
        val temporary = File.createTempFile("existing-quality-", ".$extension", directory)
        return try {
            network(Request.Builder().url(remote.url).header("Authorization", authorization).get().build(), webDavClient) {
                if (it.code != 200 || (remote.etag != null && it.header("ETag") != null && it.header("ETag") != remote.etag)) return@network null
                val stream = it.body?.byteStream() ?: return@network null
                stream.use { input -> temporary.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytes = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        bytes += count
                        check(bytes <= 4L * 1024 * 1024 * 1024) { "已有音频超过支持的大小" }
                        output.write(buffer, 0, count)
                    }
                } }
                DownloadAudioQuality.read(temporary)
            }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { null }
        finally { temporary.delete() }
    }

    private suspend fun confirmAfterDisconnect(url: HttpUrl, authorization: String, file: File): Boolean {
        // 只重查，不重传：OpenList 等网关的上游保存可能尚未完成。
        repeat(3) { attempt ->
            if (attempt > 0) delay(3_000)
            try {
                if (confirmUploadedFile(url, authorization, file)) return true
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (e: Exception) {
                val detail = if (e is javax.net.ssl.SSLException || e is java.net.SocketTimeoutException)
                    e.message.orEmpty().replace(Regex("https?://\\S+"), "[地址已隐藏]") else ""
                errorLog?.log(ErrorLogStore.Level.WARN, "Download",
                    "上传后第 ${attempt + 1} 次核验连接失败：${e.javaClass.simpleName} $detail")
            }
        }
        return false
    }

    private suspend fun confirmUploadedFile(url: HttpUrl, authorization: String, file: File): Boolean {
        val request = Request.Builder().url(url).header("Authorization", authorization)
            .header("Accept-Encoding", "identity").get().build()
        return network(request, webDavClient) { response ->
            if (response.code != 200) {
                errorLog?.log(ErrorLogStore.Level.WARN, "Download", "上传后核验返回 HTTP ${response.code}")
                return@network false
            }
            val body = response.body ?: return@network false
            if (body.contentLength() >= 0 && body.contentLength() != file.length()) {
                errorLog?.log(ErrorLogStore.Level.WARN, "Download",
                    "上传后核验大小不一致：远端 ${body.contentLength()}，本地 ${file.length()}")
                return@network false
            }
            val job = currentCoroutineContext().job
            body.byteStream().use { remote -> file.inputStream().use { local ->
                val expected = ByteArray(64 * 1024)
                val actual = ByteArray(expected.size)
                while (true) {
                    job.ensureActive()
                    val count = local.read(expected)
                    if (count < 0) return@network remote.read() == -1
                    var offset = 0
                    while (offset < count) {
                        val read = remote.read(actual, offset, count - offset)
                        if (read < 0) return@network false
                        offset += read
                    }
                    for (i in 0 until count) if (expected[i] != actual[i]) {
                        errorLog?.log(ErrorLogStore.Level.WARN, "Download", "上传后核验内容不一致")
                        return@network false
                    }
                }
                @Suppress("UNREACHABLE_CODE")
                false
            } }
        }
    }

    /**
     * 上传进度只是附带信息：写进度失败不能让已经发出去的 PUT 变成失败（否则 OkHttp 会中断请求，
     * 服务端只看到连接被掐断，用户则看到一句没有信息量的网络错误）。
     */
    private suspend fun reportTransferred(taskId: String, bytes: Long) {
        try {
            change(taskId) { it.copy(transferredBytes = bytes) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private suspend fun reportTransferMessage(taskId: String, message: String?) {
        try { change(taskId) { it.copy(transferMessage = message) } }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { }
    }

    private suspend fun <T> network(request: Request, httpClient: OkHttpClient = client, block: suspend (Response) -> T): T = coroutineScope {
        val call = httpClient.newCall(request)
        val watcher = launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
            try { awaitCancellation() } finally { call.cancel() }
        }
        try { withContext(Dispatchers.IO) { call.execute().use { block(it) } } }
        catch (e: java.io.IOException) {
            // call.cancel() 导致的断连保留协程取消语义，避免完成核验后误走失败恢复。
            currentCoroutineContext().ensureActive()
            throw e
        }
        finally { watcher.cancel() }
    }
}

/** 音源脚本生态约定的 UA；用 okhttp 默认 UA 会被网易 CDN 直接拒绝 */
private const val AUDIO_USER_AGENT = "lx-music"

private class DownloadQualityUnavailable(message: String) : Exception(message)

/** 本地残留分片与远端对不上（例如上一轮音频已下完、这次续传越界），需要丢掉分片重下 */
private class StaleDownloadPart : Exception("续传位置无效")

/** 展示根因而不是「下载失败」四个字：音源拒绝、格式不支持等都要能直接看到。 */
internal fun downloadFailureMessage(e: Exception): String {
    val chain = generateSequence(e as Throwable) { it.cause }.toList()
    val joined = chain.mapNotNull { it.message }.joinToString(" ")
    // 音源脚本的后端接口超时最常发生：给一句能指导操作的提示，别把整串脚本报错糊在卡片上
    if (joined.contains("超时") || joined.contains("timeout", ignoreCase = true)) {
        return "下载失败：音源响应超时，请稍后重试或改用较低音质"
    }
    val root = generateSequence(e as Throwable) { it.cause }
        .mapNotNull { it.message?.takeIf { message -> message.isNotBlank() } }
        .lastOrNull()
    val detail = (root ?: e::class.simpleName.orEmpty()).replace(Regex("\\s+"), " ").trim()
    return if (detail.isBlank()) "下载失败，请检查音源与保存位置后重试" else "下载失败：${detail.take(160)}"
}

internal fun detectAudioExtension(file: File): String? {
    val bytes = file.inputStream().use { input -> ByteArray(16).let { bytes -> val size = input.read(bytes); bytes.copyOf(size.coerceAtLeast(0)) } }
    val magic = bytes.toString(Charsets.ISO_8859_1)
    return when {
        magic.startsWith("fLaC") -> "flac"
        magic.startsWith("ID3") -> "mp3"
        magic.startsWith("OggS") -> "ogg"
        magic.startsWith("RIFF") && magic.substring(8).startsWith("WAVE") -> "wav"
        magic.length >= 8 && magic.substring(4, 8) == "ftyp" -> "m4a"
        bytes.size >= 2 && bytes[0].toInt() and 0xff == 0xff && bytes[1].toInt() and 0xe0 == 0xe0 ->
            if (bytes[1].toInt() and 0xf6 == 0xf0) "aac" else "mp3"
        else -> null
    }
}

private fun java.io.InputStream.readBounded(limit: Int): ByteArray? {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val size = read(buffer)
        if (size < 0) break
        if (output.size() + size > limit) return null
        output.write(buffer, 0, size)
    }
    return output.toByteArray()
}

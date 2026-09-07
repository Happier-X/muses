package com.muses.player.desktop.di

import com.muses.player.core.data.crypto.PlatformCryptoEngine
import com.muses.player.core.data.db.MusesDatabase
import com.muses.player.core.data.db.createJvmDatabase
import com.muses.player.core.data.mapper.toDomain
import com.muses.player.core.data.platform.PlatformDirs
import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.data.repository.RoomAlbumRepository
import com.muses.player.core.data.repository.RoomArtistRepository
import com.muses.player.core.data.repository.RoomSongRepository
import com.muses.player.core.media.scanner.PlaybackLazyScan
import com.muses.player.core.model.SourceType
import com.muses.player.core.scrape.ports.JaudiotaggerTagPort
import com.muses.player.desktop.cache.DesktopWebDavAudioCache
import com.muses.player.desktop.playback.DesktopErrorLog
import com.muses.player.desktop.playback.JvmPlayerPort
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

/**
 * S2 桌面装配（S3 接线入口；无 Koin/Compose 依赖，纯工厂函数）。
 *
 * 底座复用（S1）：
 * - [createJvmDatabase]：`<appDataDir>/muses.db` 单例（DB 名/迁移链冻结）；
 * - [PlatformDirs.cacheDir]：[DesktopWebDavAudioCache] spiller 落盘处（500MB LRU）；
 * - [PlatformDirs.errorLogDir]：`crash-latest.txt`（[DesktopErrorLog]）。
 *
 * 凭据：密码经 [PlatformCryptoEngine] 解密后短生命周期持有（见 [DesktopCredentials]）。
 */
object DesktopContainer {

    @Volatile private var database: MusesDatabase? = null

    /** DB 单例（S1 底座，不重复建库）。 */
    fun database(): MusesDatabase =
        database ?: synchronized(this) {
            database ?: createJvmDatabase().also { database = it }
        }

    /**
     * U23：进程级共享 Preferences DataStore 单例——同文件多 DataStore 实例会抛
     * multiple DataStores active，凭据（DesktopCredentials）与设置/播放状态/
     * 最近播放仓库（Koin 绑定）必须共用本实例。
     */
    val settingsStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> by lazy {
        com.muses.player.core.data.store.createDataStore()
    }

    fun audioCache(): DesktopWebDavAudioCache = DesktopWebDavAudioCache()

    /** 凭据仓库（commonMain [CredentialsRepository] 实现；DataStore 单实例由类内 lazy 保证）。 */
    fun credentials(): DesktopCredentials = DesktopCredentials()

    /**
     * 播放端口装配：曲库/音源/密码三查默认走 Room + DataStore + DPAPI 文件密钥。
         * S3 可按需传入自定义 lookup（测试/多库场景）。
     */
    suspend fun playerPort(
        songLookup: (suspend (songId: String) -> JvmPlayerPort.SongRef?)? = null,
        sourceLookup: (suspend (sourceId: String) -> JvmPlayerPort.SourceRef?)? = null,
        passwordLookup: (suspend (sourceId: String) -> String?)? = null,
    ): JvmPlayerPort {
        val db = database()
        val credentials = DesktopCredentials()
        val defaultSongLookup: suspend (String) -> JvmPlayerPort.SongRef? = { songId ->
            db.songDao().getById(songId)?.let { e ->
                JvmPlayerPort.SongRef(
                    id = e.id,
                    sourceId = e.sourceId,
                    path = e.path,
                    title = e.title,
                    artist = e.artist,
                    album = e.albumTitle,
                    coverUri = e.coverUri,
                    sourceType = runCatching { SourceType.valueOf(e.sourceType) }
                        .getOrDefault(SourceType.LOCAL),
                )
            }
        }
        val defaultSourceLookup: suspend (String) -> JvmPlayerPort.SourceRef? = { sourceId ->
            db.sourceDao().getById(sourceId)?.let { e ->
                JvmPlayerPort.SourceRef(id = e.id, url = e.url, username = e.username)
            }
        }
        // U26 播放懒扫描：读本地已缓存文件标签（JaudiotaggerTagPort）→ 共用编排融合 →
        // RoomSongRepository.upsert 回写（与安卓 PlaybackService 同契约：已刮削字段保护 + tagsVersion 抬升）→
        // 回传文件标签快照供端口发布 currentMeta（对齐安卓 ExoPlayer 内嵌标签经 MediaMetadata
        // 回流：未刮削歌曲重播时展示用上文件侧更新数据，而非只读库）
        val songRepository = RoomSongRepository(db.songDao(), db.albumDao(), db.artistDao())
        val lazyScan: suspend (String, java.io.File) -> PlaybackLazyScan.FileTags? = { songId, localFile ->
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val tags = JaudiotaggerTagPort.readTags(localFile)
                    val coverUri = tags?.cover?.takeIf { it.isNotEmpty() }?.let { bytes ->
                        writeLazyScanCover(songId, bytes)
                    }
                    val fileTags = tags?.let {
                        PlaybackLazyScan.FileTags(
                            title = it.title,
                            artist = it.artist,
                            album = it.album,
                            lyrics = it.lyrics,
                            coverUri = coverUri,
                            durationMs = it.durationMs,
                        )
                    }
                    val song = songRepository.getSong(songId)
                    if (song != null) {
                        val merged = PlaybackLazyScan.merge(song, fileTags)
                        if (merged != null) {
                            songRepository.upsert(merged)
                        } else {
                            // 版本已齐但库内缺封面：文件侧有图则回填
                            // （后补内嵌/首次漏读/缓存清理，不再永久缺席；刮削封面决策不受影响）
                            PlaybackLazyScan.coverBackfill(song, fileTags?.coverUri)?.let {
                                songRepository.upsert(it)
                            }
                        }
                    }
                    fileTags
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // 静默失败保持 tagsVersion=0 下次重试，不阻塞播放
                    com.muses.player.desktop.playback.DesktopErrorLog.log(
                        "PlaybackLazyScan",
                        "懒扫描失败 id=$songId path=${localFile.path.take(80)}: ${e.message}",
                        e,
                    )
                    null
                }
            }
        }
        return JvmPlayerPort.createDefault(
            db = db,
            songLookup = songLookup ?: defaultSongLookup,
            sourceLookup = sourceLookup ?: defaultSourceLookup,
            passwordLookup = passwordLookup ?: { sourceId -> credentials.getPassword(sourceId) },
            onPlaybackStarted = lazyScan,
            // 同文件多 DataStore 实例会抛 multiple DataStores active：复用进程单例，
            // 与 DesktopCredentials（凭据）/Koin 设置仓储/播放状态共用同一实例
            dataStore = settingsStore,
        )
    }

    /** 供测试注入内存库/隔离路径后重置单例。 */
    fun resetForTest() {
        synchronized(this) {
            runCatching { database?.close() }
            database = null
        }
    }

    /**
     * 懒扫描封面落盘（对齐安卓 CoverCacheWriter：cache/covers/<sha256(songId)>.jpg，返回 file:// URI）。
     * 失败返回 null（封面缺失不阻塞扫描）。
     *
     * URI 形状必须与安卓 `Uri.fromFile()` 一致（`file:///C:/…` 三斜杠）：
     * Java `File.toURI()` 在 Windows 上生成 `file:/C:/…` 单斜杠，coil 桌面端解析失败导致封面不展示。
     */
    private fun writeLazyScanCover(songId: String, bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        return runCatching {
            val directory = java.io.File(PlatformDirs.cacheDir(), "covers").apply { mkdirs() }
            val digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(songId.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            val file = java.io.File(directory, "$digest.jpg")
            file.writeBytes(bytes)
            // 三斜杠 file URI（对齐安卓 Uri.fromFile；不用 File.toURI()，Windows 下少两条斜杠）
            "file:///" + file.absolutePath.replace('\\', '/')
        }.getOrNull()
    }
}

/**
 * 桌面凭据仓库（对齐安卓侧 `AndroidKeyStoreCredentialsRepository` 语义）：
 * DataStore 存 base64 加密串（key `credential.<sourceId>`），加解密委托 [PlatformCryptoEngine]
 *（DPAPI，失败回退文件密钥，见 S1 jvmMain actual）；明文只在调用方短生命周期内存在。
 *
 * W4 桌面装配（任务 09-05-scrape-kmp）：直接实现 commonMain [CredentialsRepository] 接口，
 * 刮削写回链（WebDavAudioTagFileWriter）无需适配层即可注入。
 */
class DesktopCredentials : CredentialsRepository {

    private companion object {
        // 进程级单实例：同文件（muses_settings）多 DataStore 实例会抛 multiple DataStores active，
        // U23 起统一走 DesktopContainer.settingsStore（凭据/设置/播放状态/最近播放共享）
        val store = DesktopContainer.settingsStore
    }

    override suspend fun savePassword(sourceId: String, password: String) {
        require(password.isNotEmpty()) { "密码不能为空" }
        val encrypted = PlatformCryptoEngine.encrypt(password.toByteArray(Charsets.UTF_8))
        val encoded = java.util.Base64.getEncoder().encodeToString(encrypted)
        store.edit { prefs -> prefs[keyFor(sourceId)] = encoded }
    }

    override suspend fun getPassword(sourceId: String): String? {
        val encoded = store.data.first()[keyFor(sourceId)] ?: return null
        return runCatching {
            String(
                PlatformCryptoEngine.decrypt(java.util.Base64.getDecoder().decode(encoded)),
                Charsets.UTF_8,
            )
        }.getOrNull()
    }

    override suspend fun clearPassword(sourceId: String) {
        store.edit { prefs -> prefs.remove(keyFor(sourceId)) }
    }

    private fun keyFor(sourceId: String) =
        stringPreferencesKey("credential.$sourceId")
}

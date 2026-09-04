package com.muses.player.desktop.di

import com.muses.player.core.data.crypto.PlatformCryptoEngine
import com.muses.player.core.data.db.MusesDatabase
import com.muses.player.core.data.db.createJvmDatabase
import com.muses.player.core.data.platform.PlatformDirs
import com.muses.player.core.model.SourceType
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

    fun audioCache(): DesktopWebDavAudioCache = DesktopWebDavAudioCache()

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
        return JvmPlayerPort.createDefault(
            db = db,
            songLookup = songLookup ?: defaultSongLookup,
            sourceLookup = sourceLookup ?: defaultSourceLookup,
            passwordLookup = passwordLookup ?: { sourceId -> credentials.getPassword(sourceId) },
        )
    }

    /** 供测试注入内存库/隔离路径后重置单例。 */
    fun resetForTest() {
        synchronized(this) {
            runCatching { database?.close() }
            database = null
        }
    }
}

/**
 * 桌面凭据仓库（对齐安卓侧 `AndroidKeyStoreCredentialsRepository` 语义）：
 * DataStore 存 base64 加密串（key `credential.<sourceId>`），加解密委托 [PlatformCryptoEngine]
 *（DPAPI，失败回退文件密钥，见 S1 jvmMain actual）；明文只在调用方短生命周期内存在。
 */
class DesktopCredentials {
    private val store by lazy { com.muses.player.core.data.store.createDataStore() }

    suspend fun savePassword(sourceId: String, password: String) {
        require(password.isNotEmpty()) { "密码不能为空" }
        val encrypted = PlatformCryptoEngine.encrypt(password.toByteArray(Charsets.UTF_8))
        val encoded = java.util.Base64.getEncoder().encodeToString(encrypted)
        store.edit { prefs -> prefs[keyFor(sourceId)] = encoded }
    }

    suspend fun getPassword(sourceId: String): String? {
        val encoded = store.data.first()[keyFor(sourceId)] ?: return null
        return runCatching {
            String(
                PlatformCryptoEngine.decrypt(java.util.Base64.getDecoder().decode(encoded)),
                Charsets.UTF_8,
            )
        }.getOrNull()
    }

    suspend fun clearPassword(sourceId: String) {
        store.edit { prefs -> prefs.remove(keyFor(sourceId)) }
    }

    private fun keyFor(sourceId: String) =
        stringPreferencesKey("credential.$sourceId")
}

package com.muses.player.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.muses.player.core.data.repository.*
import com.muses.player.core.lxsdk.store.LxScriptStore
import com.muses.player.core.model.Song
import com.muses.player.core.model.Source
import com.muses.player.core.model.SourceType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import java.security.MessageDigest
import java.util.UUID

class WebDavSyncManager(
    private val dataStore: DataStore<Preferences>,
    private val credentials: CredentialsRepository,
    private val sources: SourceRepository,
    private val songs: SongRepository,
    private val history: RecentPlaysRepository,
    private val stats: PlayStatsRepository,
    private val scripts: LxScriptStore,
    private val transport: WebDavSyncTransport = WebDavSyncTransport(),
    private val scriptRepository: com.muses.player.core.lxsdk.LxScriptRepository? = null,
) {
    companion object {
        const val CREDENTIAL_ID = "muses-data-sync"
        private val CONFIG = stringPreferencesKey("webdav_sync_config")
        private val DEVICE_ID = stringPreferencesKey("webdav_sync_device_id")
        private val LAST_SYNC = longPreferencesKey("webdav_sync_last_success")
        private val SELECTION = stringPreferencesKey("webdav_sync_selection")
        val BOOLEAN_KEYS = setOf("builtin_lx_sources_enabled", "mini_player_lyrics_enabled", "notification_lyrics_enabled",
            "cover_content_color_enabled", "lyric_translation_enabled", "lyric_romanization_enabled")
        val STRING_KEYS = setOf("online_preferred_quality", "download_quality")
        private val SCRIPT_ID = Regex("[A-Za-z0-9_-]{1,128}")
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status.asStateFlow()
    val config: Flow<WebDavSyncConfig> = dataStore.data.map {
        runCatching { json.decodeFromString<WebDavSyncConfig>(it[CONFIG] ?: "{}") }.getOrDefault(WebDavSyncConfig())
    }
    val lastSync: Flow<Long> = dataStore.data.map { it[LAST_SYNC] ?: 0L }
    val selection: Flow<SyncSelection> = dataStore.data.map { decodeSelection(it) }

    private fun decodeSelection(prefs: Preferences): SyncSelection {
        val saved = json.parseToJsonElement(prefs[SELECTION] ?: "{}").jsonObject
        val selection = json.decodeFromJsonElement<SyncSelection>(saved)
        // 旧版本任一音源开关关闭时，合并后保持关闭，避免自动扩大同步范围。
        return selection.copy(sources = selection.sources && (saved["scripts"]?.jsonPrimitive?.booleanOrNull ?: true))
    }

    suspend fun setContentEnabled(content: SyncContent, enabled: Boolean) {
        dataStore.edit { it[SELECTION] = json.encodeToString(decodeSelection(it).withEnabled(content, enabled)) }
    }

    suspend fun save(config: WebDavSyncConfig, password: String) {
        transport.root(config)
        require(config.username.isNotBlank()) { "请输入 WebDAV 账号" }
        // 密码留空沿用已保存值，连接信息和口令不加入同步快照。
        val old = this.config.first()
        require(password.isNotEmpty() || (old.url == config.url.trim() && old.username == config.username.trim())) {
            "更换地址或账号后，请重新填写密码"
        }
        if (password.isNotEmpty()) credentials.savePassword(CREDENTIAL_ID, password)
        require(!credentials.getPassword(CREDENTIAL_ID).isNullOrEmpty()) { "请输入 WebDAV 密码或应用密码" }
        dataStore.edit {
            it[CONFIG] = json.encodeToString(config.copy(url = config.url.trim(), username = config.username.trim(), directory = config.directory.trim()))
            if (old != config) it[LAST_SYNC] = 0L
        }
    }

    suspend fun testConnection() = runOperation {
        val config = config.first()
        val password = password()
        transport.list(config, password)
        _status.value = "连接成功，写入权限将在同步时验证"
    }

    suspend fun synchronize() = runOperation {
        val selection = selection.first()
        if (!selection.anyEnabled) {
            _status.value = "请先选择要同步的内容"
            return@runOperation
        }
        val config = config.first()
        val password = password()
        val startedAt = System.currentTimeMillis()
        val deviceId = deviceId()
        val baselineKey = stringPreferencesKey("webdav_sync_baseline_${hash(transport.root(config).toString() + "|" + config.username)}")
        val prefs = dataStore.data.first()
        val baseline = prefs[baselineKey]?.let { json.decodeFromString<SyncSnapshot>(it) }
        _status.value = "正在读取远端数据…"
        val files = transport.list(config, password)
        val remote = mutableListOf<SyncSnapshot>()
        var ownDocument: WebDavSyncTransport.Document? = null
        var totalBytes = 0
        for (file in files) {
            currentCoroutineContext().ensureActive()
            val document = transport.read(config, password, file.name) ?: error("远端文件已变化，请重新同步")
            totalBytes += document.body.toByteArray().size
            require(totalBytes <= 50 * 1024 * 1024) { "远端同步数据总量过大" }
            val snapshot = json.decodeFromString<SyncSnapshot>(document.body)
            validate(snapshot)
            require(snapshot.deviceId == file.deviceId) { "远端设备标识不一致，请检查同步目录" }
            remote += snapshot
            if (file.deviceId == deviceId) ownDocument = document
        }
        // 即使目录列表缓存尚未出现本设备文件，也独立读取，避免误判成首次上传。
        if (ownDocument == null) {
            ownDocument = transport.read(config, password, "device-$deviceId.json")
            ownDocument?.let {
                val snapshot = json.decodeFromString<SyncSnapshot>(it.body)
                validate(snapshot)
                require(snapshot.deviceId == deviceId) { "远端设备标识不一致" }
                remote += snapshot
            }
        }
        _status.value = "正在合并应用数据…"
        val ownSnapshot = remote.firstOrNull { it.deviceId == deviceId }
        val localSources = sources.observeSources().first()
        val sourceById = localSources.associateBy { it.id }
        val localHistory = if (selection.history) history.load() else emptyList()
        if (selection.stats && baseline == null) ownSnapshot?.let {
            stats.restoreLocalContributionIfEmpty(it.stats)
        }
        val localStats = if (selection.stats) stats.loadLocalContribution() else PlayStats.Empty
        val referencedSongs = songs.getSongs((localHistory.map { it.songId } + localStats.songs.keys).distinct())
        val tracks = referencedSongs.values.mapNotNull { track(it, sourceById) }
        val exportIds = tracks.associate { original ->
            val localSong = referencedSongs.values.first { it.path == original.path &&
                (it.sourceType == SourceType.ONLINE || sourceById[it.sourceId]?.let(::sourceKey) == original.sourceId) }
            localSong.id to original.id
        }
        val local = SyncSnapshot(
            deviceId = deviceId, updatedAt = startedAt,
            preferences = if (selection.preferences) (BOOLEAN_KEYS + STRING_KEYS).mapNotNull { key ->
                val value = if (key in BOOLEAN_KEYS) prefs[booleanPreferencesKey(key)]?.let(::JsonPrimitive)
                    else prefs[stringPreferencesKey(key)]?.let(::JsonPrimitive)
                value?.let { key to stamp(it, baseline?.preferences?.get(key), startedAt, deviceId) }
            }.toMap() else emptyMap(),
            sources = if (selection.sources) localSources.filter { it.type == SourceType.WEBDAV && publicHttpUrl(it.url.orEmpty()) }.associate { source ->
                val id = sourceKey(source)
                id to stamp(json.encodeToJsonElement(SyncSource(id, source.name, source.url.orEmpty(), source.path, source.username)),
                    baseline?.sources?.get(id), startedAt, deviceId)
            } else emptyMap(),
            scripts = if (selection.sources) withContext(Dispatchers.IO) { scripts.list().filter { !it.isBuiltin }.associate { script ->
                script.id to stamp(json.encodeToJsonElement(SyncScript(script.id, script.source, script.enabled, script.sourceUrl)),
                    baseline?.scripts?.get(script.id), startedAt, deviceId)
            } } else emptyMap(),
            tracks = tracks,
            history = localHistory.map { it.copy(songId = exportIds[it.songId] ?: it.songId, coverUri = publicCover(it.coverUri)) },
            stats = remapStats(localStats, exportIds),
        )
        val snapshots = remote + local
        val mergedPreferences = if (selection.preferences) mergeSyncValues(snapshots.map { it.preferences }) else ownSnapshot?.preferences.orEmpty()
        val mergedSources = if (selection.sources) mergeSyncValues(snapshots.map { it.sources }) else ownSnapshot?.sources.orEmpty()
        val mergedScripts = if (selection.sources) mergeSyncValues(snapshots.map { it.scripts }) else ownSnapshot?.scripts.orEmpty()
        val outgoingHistory = if (selection.history) snapshots.flatMap { it.history }
            .filter { it.playedAt >= startedAt - RecentPlaysRepository.RETENTION_MS }
            .sortedByDescending { it.playedAt }.distinctBy { it.songId } else ownSnapshot?.history.orEmpty()
        val outgoingStats = if (selection.stats) local.stats else ownSnapshot?.stats ?: PlayStats.Empty
        val outgoingIds = (outgoingHistory.map { it.songId } + outgoingStats.songs.keys).toSet()
        // 每台设备只写自己的文件，其他设备同时写入不会相互覆盖。
        val outgoing = local.copy(preferences = mergedPreferences, sources = mergedSources, scripts = mergedScripts,
            tracks = snapshots.flatMap { it.tracks }.filter {
                it.id in outgoingIds && (it.online || it.sourceId in mergedSources)
            }.distinctBy { it.id },
            history = outgoingHistory, stats = outgoingStats)
        _status.value = "正在上传并校验同步数据…"
        transport.write(config, password, "device-$deviceId.json", json.encodeToString(outgoing), ownDocument)
        currentCoroutineContext().ensureActive()
        _status.value = "正在应用同步数据…"
        val sourceMappings = localSources.filter { it.type == SourceType.WEBDAV }
            .associate { sourceKey(it) to it.id }.toMutableMap()
        for ((id, value) in if (selection.sources) mergedSources else emptyMap()) {
            val remoteSource = json.decodeFromJsonElement<SyncSource>(value.value)
            val existing = localSources.firstOrNull { it.type == SourceType.WEBDAV && sourceKey(it) == id }
            val localId = existing?.id ?: id
            sourceMappings[id] = localId
            val current = sources.getSource(localId)
            if (current == existing) sources.upsert(Source(
                localId, remoteSource.name, SourceType.WEBDAV, remoteSource.url, remoteSource.path,
                remoteSource.username, existing?.createdAt ?: value.changedAt, value.changedAt,
            ))
        }
        val idMappings = mutableMapOf<String, String>()
        val incomingIds = ((if (selection.history) snapshots.flatMap { it.history }.map { it.songId } else emptyList()) +
            (if (selection.stats) snapshots.flatMap { it.stats.songs.keys } else emptyList())).toSet()
        for (track in snapshots.flatMap { it.tracks }.filter { it.id in incomingIds }.distinctBy { it.id }) {
            val localSourceId = if (track.online) track.sourceId else sourceMappings[track.sourceId] ?: continue
            val id = if (track.online) track.id else hash("$localSourceId|${track.path}")
            idMappings[track.id] = id
            if (songs.getSong(id) == null) songs.upsert(Song(id, localSourceId, track.path, track.title,
                track.artist, track.album, track.durationMs, track.durationMs / 1000,
                publicCover(track.coverUri), sourceType = if (track.online) SourceType.ONLINE else SourceType.WEBDAV))
        }
        if (selection.sources) withContext(Dispatchers.IO) {
            val builtinIds = scripts.builtinIds()
            mergedScripts.forEach { (id, value) ->
                if (id !in builtinIds) {
                    val script = json.decodeFromJsonElement<SyncScript>(value.value)
                    val existing = scripts.get(id)
                    val captured = local.scripts[id]?.value?.let { json.decodeFromJsonElement<SyncScript>(it) }
                    val unchanged = if (captured == null) existing == null else existing != null &&
                        existing.source == captured.source && existing.enabled == captured.enabled && existing.sourceUrl == captured.sourceUrl
                    if (unchanged && (existing == null ||
                        (existing.source != script.source || existing.enabled != script.enabled || existing.sourceUrl != script.sourceUrl))) {
                        scripts.save(id, script.source, script.enabled, script.sourceUrl)
                    }
                }
            }
        }
        if (selection.history) history.merge(snapshots.flatMap { it.history }.map { it.copy(songId = idMappings[it.songId] ?: it.songId, coverUri = publicCover(it.coverUri)) })
        if (selection.sources) scriptRepository?.refreshFromStore()
        if (selection.stats) stats.replaceRemoteContributions(remote.filter { it.deviceId != deviceId }.associate {
            it.deviceId to remapStats(it.stats, idMappings)
        })
        dataStore.edit { current ->
            if (selection.preferences) mergedPreferences.forEach { (key, value) ->
                // 同步期间新修改的设置留待下次同步，不覆盖用户刚做的操作。
                if (key in BOOLEAN_KEYS && current[booleanPreferencesKey(key)] == prefs[booleanPreferencesKey(key)])
                    current[booleanPreferencesKey(key)] = value.value.jsonPrimitive.boolean
                if (key in STRING_KEYS && current[stringPreferencesKey(key)] == prefs[stringPreferencesKey(key)])
                    current[stringPreferencesKey(key)] = value.value.jsonPrimitive.content
            }
            current[baselineKey] = json.encodeToString(outgoing)
            current[LAST_SYNC] = System.currentTimeMillis()
        }
        _status.value = "同步完成；新增音源请扫描并填写密码"
    }

    private suspend fun runOperation(operation: suspend () -> Unit) {
        if (!mutex.tryLock()) return
        _busy.value = true
        try { operation() }
        catch (e: CancellationException) { _status.value = "同步已取消，可重新同步"; throw e }
        catch (_: Exception) {
            // 不直接显示网络异常对象，防止 URL 或认证信息进入界面日志。
            _status.value = "同步失败，请检查地址、账号、目录权限和网络后重试"
        }
        finally { _busy.value = false; mutex.unlock() }
    }

    private suspend fun password(): String = credentials.getPassword(CREDENTIAL_ID)?.takeIf { it.isNotEmpty() }
        ?: error("请先保存同步配置")

    private suspend fun deviceId(): String {
        dataStore.edit { if (it[DEVICE_ID] == null) it[DEVICE_ID] = UUID.randomUUID().toString() }
        return dataStore.data.first()[DEVICE_ID]!!
    }

    private fun stamp(value: JsonElement, old: SyncValue?, now: Long, device: String): SyncValue =
        old?.takeIf { it.value == value } ?: SyncValue(value, now, device)

    private fun sourceKey(source: Source): String = "webdav-" + hash(source.url.orEmpty().trim().trimEnd('/') + "|" +
        source.path.orEmpty().trim('/') + "|" + source.username.orEmpty())

    private fun track(song: Song, sources: Map<String, Source>): SyncTrack? {
        if (song.sourceType == SourceType.LOCAL) return null
        if (song.sourceType == SourceType.WEBDAV && (!publicHttpUrl(song.path) ||
            !publicHttpUrl(sources[song.sourceId]?.url.orEmpty()))) return null
        if (song.sourceType == SourceType.ONLINE && !song.path.startsWith("muslx://")) return null
        val sourceId = if (song.sourceType == SourceType.ONLINE) song.sourceId
            else sources[song.sourceId]?.let(::sourceKey) ?: return null
        val id = if (song.sourceType == SourceType.ONLINE) song.id else hash("$sourceId|${song.path}")
        return SyncTrack(id, sourceId, song.path, song.title, song.artist, song.album, song.durationMs,
            publicCover(song.coverUri), song.sourceType == SourceType.ONLINE)
    }

    private fun remapStats(stats: PlayStats, ids: Map<String, String>): PlayStats = stats.copy(songs =
        stats.songs.values.groupBy { ids[it.songId] ?: it.songId }.mapValues { (id, entries) ->
            entries.first().copy(songId = id, playCount = entries.sumOf { it.playCount.toLong() }.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                coverUri = publicCover(entries.first().coverUri))
        })

    private fun validate(snapshot: SyncSnapshot) {
        require(snapshot.version == 1) { "同步数据版本不兼容" }
        require(Regex("[a-f0-9-]{36}").matches(snapshot.deviceId))
        val latest = System.currentTimeMillis() + 5 * 60_000L
        require(snapshot.updatedAt <= latest)
        (snapshot.preferences.values + snapshot.sources.values + snapshot.scripts.values).forEach {
            require(it.changedAt in 0..latest && Regex("[a-f0-9-]{36}").matches(it.deviceId))
        }
        require(snapshot.updatedAt >= 0 && snapshot.history.size <= 100_000 && snapshot.tracks.size <= 100_000)
        snapshot.preferences.forEach { (key, value) ->
            require(key in BOOLEAN_KEYS || key in STRING_KEYS)
            require(value.changedAt >= 0)
            if (key in BOOLEAN_KEYS) require(value.value.jsonPrimitive.booleanOrNull != null)
            else require(value.value.jsonPrimitive.isString && value.value.jsonPrimitive.content.length < 100)
        }
        snapshot.sources.forEach { (id, value) ->
            val source = json.decodeFromJsonElement<SyncSource>(value.value)
            require(id == source.id && sourceKey(Source(id, source.name, SourceType.WEBDAV, source.url, source.path,
                source.username, 0, 0)) == id)
            // 地址仅作为音源配置导入，不使用远端地址发送同步凭据。
            require(publicHttpUrl(source.url))
        }
        snapshot.scripts.forEach { (id, value) ->
            require(SCRIPT_ID.matches(id))
            val script = json.decodeFromJsonElement<SyncScript>(value.value)
            require(script.id == id && script.source.length <= 2_000_000)
        }
        require(snapshot.stats.totalListenMs in 0..3_200_000_000_000L && snapshot.stats.totalPlayCount in 0..100_000_000)
        require(snapshot.stats.days.size <= PlayStatsRepository.MAX_DAYS && snapshot.stats.songs.size <= PlayStatsRepository.MAX_SONGS)
        require(snapshot.stats.days.keys.all { Regex("\\d{4}-\\d{2}-\\d{2}").matches(it) })
        require(snapshot.stats.days.values.all { it.listenMs in 0..3_200_000_000_000L && it.playCount in 0..100_000_000 })
        require(snapshot.stats.songs.all { (key, value) -> value.songId == key && value.playCount in 0..100_000_000 })
        require(snapshot.history.all { it.playedAt in 0..latest })
        snapshot.tracks.forEach {
            require(it.durationMs >= 0)
            if (it.online) require(it.path.startsWith("muslx://") && it.id.startsWith("online:"))
            else {
                val source = snapshot.sources[it.sourceId]?.value?.let { json.decodeFromJsonElement<SyncSource>(it) }
                require(source != null && publicHttpUrl(it.path) &&
                    it.path.startsWith(source.url.trimEnd('/') + "/") && it.id == hash("${it.sourceId}|${it.path}"))
            }
        }
    }

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

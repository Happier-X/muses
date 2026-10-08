package com.muses.player.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.ai.AI_API_KEY_SOURCE_ID
import com.muses.player.core.ai.AiException
import com.muses.player.core.ai.AiServiceUrlException
import com.muses.player.core.ai.AiRecommendConfig
import com.muses.player.core.ai.AiRecommendResult
import com.muses.player.core.ai.AiRecommendService
import com.muses.player.core.ai.DailyRecommendSnapshot
import com.muses.player.core.ai.LibraryProfileBuilder
import com.muses.player.core.ai.excludingOwnedSongs
import com.muses.player.core.ai.localRecommendDay
import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.data.repository.SettingsRepository
import com.muses.player.core.lxsdk.LxQuality
import com.muses.player.core.lxsdk.LxAction
import com.muses.player.core.lxsdk.LxScriptRepository
import com.muses.player.core.model.Song
import com.muses.player.core.model.online.OnlineTrackSession
import com.muses.player.core.playback.PlaybackPort
import com.muses.player.core.search.OnlineChart
import com.muses.player.core.search.OnlineChartService
import com.muses.player.core.search.OnlineSearchResult
import com.muses.player.core.search.PlatformChartsOutcome
import com.muses.player.core.search.OnlinePlaylist
import com.muses.player.core.search.OnlinePlaylistService
import com.muses.player.core.search.OnlineSongVersionService
import com.muses.player.core.ui.components.MusesSnackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** 排行榜区块状态 */
data class ChartSectionState(
    val platforms: List<String> = emptyList(),
    val platformNames: Map<String, String> = emptyMap(),
    val selectedPlatform: String? = null,
    /** 当前平台的榜单列表（切平台时从缓存取，不重复请求） */
    val charts: List<OnlineChart> = emptyList(),
    val loadingCharts: Boolean = false,
    val refreshingCharts: Boolean = false,
    val error: String? = null,
)

/** 「猜你喜欢」区块状态 */
data class RecommendSectionState(
    /** 地址/模型/Key 是否齐备（未配置时显示「去配置」） */
    val configured: Boolean = false,
    val loading: Boolean = false,
    val result: AiRecommendResult? = null,
    val day: String? = null,
    val error: String? = null,
) {
    /** 首次进入且尚未取过结果 */
    val idle: Boolean get() = result == null && !loading
}

/** 首页整体状态 */
data class HomeUiState(
    val featured: FeaturedPlaylistState = FeaturedPlaylistState(),
    val chart: ChartSectionState = ChartSectionState(),
    val recommend: RecommendSectionState = RecommendSectionState(),
    /** 一次性提示（如「该平台没有可用音源脚本」），展示后可清除 */
    val message: String? = null,
)

data class FeaturedPlaylistState(
    val items: List<OnlinePlaylist> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

/**
 * 首页 ViewModel：排行榜 + 猜你喜欢。
 *
 * 职责边界：
 * - 榜单与推荐都产出 [OnlineSearchResult]，经同一 `play` 链路入队（脚本校验 → 会话登记 → 播放端口），
 *   与在线搜索页行为一致；
 * - AI Key 现取现用，不在 VM 里长持（见 [readAiConfig]）。
 */
class HomeViewModel(
    private val chartService: OnlineChartService,
    private val recommendService: AiRecommendService,
    private val profileBuilder: LibraryProfileBuilder,
    private val settingsRepository: SettingsRepository,
    private val credentialsRepository: CredentialsRepository,
    private val scriptRepository: LxScriptRepository,
    private val playback: PlaybackPort,
    private val songRepository: SongRepository,
    private val chartCacheStore: OnlineChartCacheStore,
    private val playlistService: OnlinePlaylistService,
    private val playlistCacheStore: FeaturedPlaylistCacheStore,
    private val songVersionService: OnlineSongVersionService,
    /** 在线曲目归属的音源标识（与在线搜索页同值，仅作标识） */
    private val onlineSourceId: String = "online",
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** 各平台榜单缓存：目录有独立更新时间，切平台无需重复请求。 */
    private val chartsByPlatform = mutableMapOf<String, List<OnlineChart>>()
    private val catalogUpdatedAt = mutableMapOf<String, Long>()
    private var recommendJob: Job? = null

    /** 首选音质（与在线搜索页共用同一设置项） */
    private val preferredQuality: StateFlow<String> = settingsRepository.onlinePreferredQuality
        .stateIn(viewModelScope, SharingStarted.Eagerly, LxQuality.DEFAULT.key)

    init {
        viewModelScope.launch {
            songRepository.observeSongs().drop(1).collect {
                val current = _state.value.recommend
                val result = current.result ?: return@collect
                val profile = runCatching { profileBuilder.build() }.getOrNull() ?: return@collect
                val filtered = result.excludingOwnedSongs(profile)
                if (filtered.tracks.size == result.tracks.size) return@collect
                _state.value = _state.value.copy(
                    recommend = current.copy(
                        result = filtered,
                        error = if (filtered.tracks.isEmpty()) "今日推荐歌曲已全部存在于曲库中" else current.error,
                    ),
                )
                if (current.day == localRecommendDay()) {
                    runCatching {
                        settingsRepository.setAiDailyRecommend(DailyRecommendSnapshot.encode(current.day, filtered))
                    }
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                if (_state.value.recommend.day != null && _state.value.recommend.day != localRecommendDay()) {
                    refreshRecommend()
                }
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun loadFeaturedPlaylists(forceRefresh: Boolean = false) {
        if (_state.value.featured.loading) return
        _state.value = _state.value.copy(featured = _state.value.featured.copy(loading = true, error = null))
        viewModelScope.launch {
            try {
                val cached = try {
                    playlistCacheStore.load()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
                if (cached != null) {
                    _state.value = _state.value.copy(featured = _state.value.featured.copy(items = cached.items.take(3)))
                    if (!forceRefresh && cached.isFresh()) return@launch
                }
                val selectionDay = localRecommendDay()
                val items = playlistService.featured().distinctBy { it.id }.shuffled().take(3)
                check(items.isNotEmpty()) { "暂时没有可用歌单" }
                _state.value = _state.value.copy(featured = FeaturedPlaylistState(items))
                try {
                    playlistCacheStore.save(items, selectionDay)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // 缓存写入失败不影响本次已加载的歌单。
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(featured = _state.value.featured.copy(error = e.message ?: "歌单加载失败，请重试"))
            } finally {
                _state.value = _state.value.copy(featured = _state.value.featured.copy(loading = false))
            }
        }
    }

    // ── 排行榜 ──

    /** 拉取各平台榜单列表；榜单歌曲由对应的详情页按需加载。 */
    fun loadCharts(forceRefresh: Boolean = false) {
        if (_state.value.chart.loadingCharts || _state.value.chart.refreshingCharts) return
        _state.value = _state.value.copy(
            chart = _state.value.chart.copy(
                loadingCharts = _state.value.chart.charts.isEmpty(),
                refreshingCharts = _state.value.chart.charts.isNotEmpty(),
                error = null,
            ),
        )
        viewModelScope.launch {
            val cached = runCatching { chartCacheStore.loadCatalogs() }.getOrDefault(emptyMap())
            cached.forEach { (platform, catalog) ->
                chartsByPlatform[platform] = catalog.charts
                catalogUpdatedAt[platform] = catalog.updatedAt
            }

            val availablePlatforms = chartService.platforms.filter { it == "wy" }
            if (availablePlatforms.isEmpty()) {
                _state.value = _state.value.copy(
                    chart = ChartSectionState(
                        platformNames = chartService.platformNames,
                        error = "网易云榜单暂时不可用，请稍后重试",
                    ),
                )
                return@launch
            }
            val cachedPlatforms = availablePlatforms.filter { it in chartsByPlatform }
            if (cachedPlatforms.isNotEmpty()) {
                val selected = _state.value.chart.selectedPlatform?.takeIf { it in cachedPlatforms } ?: cachedPlatforms.first()
                _state.value = _state.value.copy(
                    chart = ChartSectionState(
                        platforms = cachedPlatforms,
                        platformNames = chartService.platformNames,
                        selectedPlatform = selected,
                        charts = chartsByPlatform[selected].orEmpty(),
                    ),
                )
            } else {
                _state.value = _state.value.copy(chart = _state.value.chart.copy(loadingCharts = true, error = null))
            }
            val now = Clock.System.now().toEpochMilliseconds()
            val needsRefresh = availablePlatforms.filter { platform ->
                forceRefresh || !chartCacheStore.isCatalogFresh(catalogUpdatedAt[platform] ?: 0L, now)
            }
            if (needsRefresh.isEmpty()) {
                val platform = _state.value.chart.selectedPlatform?.takeIf { it in availablePlatforms } ?: availablePlatforms.first()
                _state.value = _state.value.copy(
                    chart = _state.value.chart.copy(
                        platforms = availablePlatforms.filter { it in chartsByPlatform },
                        platformNames = chartService.platformNames,
                        selectedPlatform = platform,
                        charts = chartsByPlatform[platform].orEmpty(),
                        loadingCharts = false,
                        refreshingCharts = false,
                        error = null,
                    ),
                )
                return@launch
            }
            _state.value = _state.value.copy(
                chart = _state.value.chart.copy(
                    loadingCharts = _state.value.chart.charts.isEmpty(),
                    refreshingCharts = true,
                ),
            )
            val outcomes = runCatching { chartService.charts(needsRefresh) }.getOrDefault(emptyList())
            val success = outcomes.filterIsInstance<PlatformChartsOutcome.Success>()
            if (success.isEmpty() && cachedPlatforms.isEmpty()) {
                val failure = outcomes.filterIsInstance<PlatformChartsOutcome.Failure>().firstOrNull()
                _state.value = _state.value.copy(
                    chart = _state.value.chart.copy(
                        loadingCharts = false,
                        refreshingCharts = false,
                        error = failure?.message ?: "排行榜加载失败，请检查网络",
                    ),
                )
                return@launch
            }
            success.forEach {
                chartsByPlatform[it.platform] = it.charts
                catalogUpdatedAt[it.platform] = now
            }
            if (success.isNotEmpty()) runCatching {
                chartCacheStore.saveCatalogs(
                    chartsByPlatform.mapValues { (platform, charts) ->
                        CachedChartCatalog(charts, catalogUpdatedAt[platform] ?: now)
                    },
                )
            }

            // 保持用户已选平台（刷新场景），否则取第一个可用平台
            val platforms = availablePlatforms.filter { it in chartsByPlatform }
            val platform = _state.value.chart.selectedPlatform?.takeIf { it in platforms } ?: platforms.first()
            val charts = chartsByPlatform[platform].orEmpty()
            _state.value = _state.value.copy(
                chart = _state.value.chart.copy(
                    platforms = platforms,
                    platformNames = chartService.platformNames,
                    selectedPlatform = platform,
                    charts = charts,
                    loadingCharts = false,
                    refreshingCharts = false,
                    error = null,
                ),
            )
        }
    }

    fun selectPlatform(platform: String) {
        if (platform !in _state.value.chart.platforms) return
        if (_state.value.chart.selectedPlatform == platform) return
        val charts = chartsByPlatform[platform].orEmpty()
        _state.value = _state.value.copy(
            chart = _state.value.chart.copy(
                selectedPlatform = platform,
                charts = charts,
                error = null,
            ),
        )
    }

    // ── 猜你喜欢 ──

    /** 每日只生成一次；失败可重试，切换日期后自动重新生成。 */
    fun refreshRecommend() {
        val today = localRecommendDay()
        val current = _state.value.recommend
        if (recommendJob?.isActive == true) return
        if (current.day == today && (current.result != null || current.error != null)) return
        recommendJob = viewModelScope.launch {
            _state.value = _state.value.copy(
                recommend = _state.value.recommend.copy(loading = true, error = null),
            )

            try {
            withTimeout(90_000) {
            val cachedToday = DailyRecommendSnapshot.decode(settingsRepository.aiDailyRecommend.first(), today)
            if (cachedToday != null) {
                _state.value = _state.value.copy(recommend = _state.value.recommend.copy(
                    loading = false, configured = true, result = cachedToday, day = today, error = null))
                val filtered = cachedToday.excludingOwnedSongs(profileBuilder.build())
                val updated = if (filtered.tracks.any { it.result.platform == "wy" && it.result.coverUrl.isNullOrBlank() }) {
                    val songs = songVersionService.enrich(filtered.tracks.map { it.result })
                    filtered.copy(tracks = filtered.tracks.mapIndexed { index, track -> track.copy(result = songs[index]) })
                } else filtered
                _state.value = _state.value.copy(recommend = _state.value.recommend.copy(result = updated))
                if (updated != cachedToday) {
                    settingsRepository.setAiDailyRecommend(DailyRecommendSnapshot.encode(today, updated))
                }
                return@withTimeout
            }
            val config = readAiConfig()
            if (!config.isUsable) {
                _state.value = _state.value.copy(
                    recommend = _state.value.recommend.copy(configured = false, loading = false, result = null, day = null, error = null),
                )
                return@withTimeout
            }

            _state.value = _state.value.copy(
                recommend = _state.value.recommend.copy(configured = true),
            )

            val profile = profileBuilder.build()
            if (profile.isEmpty) {
                _state.value = _state.value.copy(
                    recommend = _state.value.recommend.copy(
                        loading = false,
                        day = today,
                        error = "曲库还没有歌曲：先扫描本地/WebDAV 音源后再试",
                    ),
                )
                return@withTimeout
            }

            runCatching { recommendService.recommend(profile, config) }.fold(
                onSuccess = { result ->
                    if (result.allUnmatched) throw AiException("推荐歌曲暂时无法在各平台匹配，请稍后重试")
                    val latestProfile = profileBuilder.build()
                    val filteredResult = result.excludingOwnedSongs(latestProfile)
                    if (localRecommendDay() == today) {
                        runCatching {
                            settingsRepository.setAiDailyRecommend(DailyRecommendSnapshot.encode(today, filteredResult))
                        }
                    }
                    _state.value = _state.value.copy(
                        recommend = _state.value.recommend.copy(
                            loading = false,
                            result = filteredResult,
                            day = today,
                            error = if (filteredResult.tracks.isEmpty() && result.tracks.isNotEmpty()) {
                                "今日推荐歌曲已全部存在于曲库中"
                            } else if (filteredResult.allUnmatched) {
                                "AI 推荐的歌都没能在平台上匹配到，可点重试"
                            } else {
                                null
                            },
                        ),
                    )
                },
                onFailure = { e ->
                    if (e is CancellationException) throw e
                    if (e is AiServiceUrlException) {
                        MusesSnackbar.show(e.message.orEmpty())
                        _state.value = _state.value.copy(
                            recommend = _state.value.recommend.copy(loading = false, configured = false, result = null, error = null),
                        )
                        return@fold
                    }
                    _state.value = _state.value.copy(
                        recommend = _state.value.recommend.copy(loading = false, day = today, error = aiErrorMessage(e)),
                    )
                },
            )
            }
            } catch (e: TimeoutCancellationException) {
                _state.value = _state.value.copy(recommend = _state.value.recommend.copy(
                    day = today, error = "推荐请求超时，请稍后点击重试"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.value = _state.value.copy(recommend = _state.value.recommend.copy(
                    day = today, error = aiErrorMessage(e)))
            } finally {
                _state.value = _state.value.copy(recommend = _state.value.recommend.copy(loading = false))
            }
        }
    }

    /** 失败后允许用户手动重试；当天已有结果时继续使用缓存。 */
    fun retryRecommend() {
        if (recommendJob?.isActive == true) return
        if (_state.value.recommend.result == null) {
            _state.value = _state.value.copy(recommend = _state.value.recommend.copy(day = null, error = null))
        }
        refreshRecommend()
    }

    fun playRecommend(index: Int) {
        val tracks = _state.value.recommend.result?.tracks.orEmpty()
        val target = tracks.getOrNull(index) ?: return
        val results = tracks.map { it.result }
        playResults(results, index, target.result.platform)
    }

    /** 随机播放当前推荐列表，与歌曲详情页工具条行为一致。 */
    fun shuffleRecommend() {
        val results = _state.value.recommend.result?.tracks.orEmpty().map { it.result }
        if (results.isEmpty()) return
        val index = results.indices.random()
        playResults(results, index, results[index].platform, shuffle = true)
    }

    /** 随心听：优先从曲库随机播放，曲库为空时使用今日推荐。 */
    fun playRandom() {
        viewModelScope.launch {
            val library = songRepository.observeSongs().first()
            if (library.isNotEmpty()) {
                playback.play(library.random().id, library)
                playback.setShuffleEnabled(true)
                return@launch
            }
            if (_state.value.recommend.result == null) {
                refreshRecommend()
                recommendJob?.join()
            }
            val tracks = _state.value.recommend.result?.tracks.orEmpty()
            if (tracks.isNotEmpty()) {
                val results = tracks.map { it.result }
                val index = results.indices.random()
                playResults(results, index, results[index].platform, shuffle = true)
            } else {
                MusesSnackbar.show("还没有可播放的歌曲，请先添加曲库歌曲或生成每日推荐")
            }
        }
    }

    // ── 内部 ──

    /**
     * 统一播放入口：脚本可用性校验 → 队列整体登记会话 → 交给播放端口。
     *
     * 为什么先校验脚本：直链解析发生在播放链路内部，没有脚本时用户点下去毫无反馈，
     * 体验上等同「坏了」；此处提前给出可操作提示（与在线搜索页同一取舍）。
     */
    private fun playResults(results: List<OnlineSearchResult>, index: Int, platform: String, shuffle: Boolean = false) {
        val target = results.getOrNull(index) ?: return
        val quality = LxQuality.fromKey(preferredQuality.value)?.key
        viewModelScope.launch {
            if (!hasUsableScript(platform)) {
                MusesSnackbar.show("还没有可用的在线音源脚本，请到「在线音源脚本」导入后再播放。")
                return@launch
            }
            // 队列 = 当前列表全量（点哪首播哪首，后续按列表顺序走）
            val songs: List<Song> = results.map { it.toSong(onlineSourceId, quality) }
            OnlineTrackSession.remember(songs)
            playback.play(target.toSong(onlineSourceId, quality).id, songs)
            playback.setShuffleEnabled(shuffle)
        }
    }

    /** 是否存在已加载且声明了该平台的脚本（与在线搜索页同判据） */
    private suspend fun hasUsableScript(platform: String): Boolean =
        runCatching {
            scriptRepository.loadAll().any { script ->
                script.loadError == null && script.sources.values.any { it.supports(LxAction.MUSIC_URL) }
            }
        }.getOrDefault(false)

    /** 现取现用 AI 配置：Key 从加密凭据库读，不常驻 VM 字段 */
    private suspend fun readAiConfig(): AiRecommendConfig {
        val baseUrl = runCatching { settingsRepository.aiBaseUrl.first() }.getOrDefault("")
        val model = runCatching { settingsRepository.aiModel.first() }.getOrDefault("")
        val apiKey = runCatching { credentialsRepository.getPassword(AI_API_KEY_SOURCE_ID) }
            .getOrNull()
            .orEmpty()
        return AiRecommendConfig(
            baseUrl = baseUrl,
            model = model,
            apiKey = apiKey,
        )
    }

    private fun aiErrorMessage(e: Throwable): String = when (e) {
        is AiException -> e.message ?: "AI 推荐失败"
        else -> "AI 推荐失败：${e.message ?: "未知错误"}"
    }

}

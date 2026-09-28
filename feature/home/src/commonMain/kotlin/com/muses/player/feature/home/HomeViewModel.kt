package com.muses.player.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.ai.AI_API_KEY_SOURCE_ID
import com.muses.player.core.ai.AiException
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
import kotlinx.coroutines.Job
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

/** 排行榜区块状态 */
data class ChartSectionState(
    val platforms: List<String> = emptyList(),
    val platformNames: Map<String, String> = emptyMap(),
    val selectedPlatform: String? = null,
    /** 当前平台的榜单列表（切平台时从缓存取，不重复请求） */
    val charts: List<OnlineChart> = emptyList(),
    val loadingCharts: Boolean = false,
    val error: String? = null,
)

/** 「猜你喜欢」区块状态 */
data class RecommendSectionState(
    /** 设置页是否已打开 AI 推荐开关 */
    val enabled: Boolean = false,
    /** 地址/模型/Key 是否齐备（决定 UI 是「去配置」还是「去开启」） */
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
    val chart: ChartSectionState = ChartSectionState(),
    val recommend: RecommendSectionState = RecommendSectionState(),
    /** 一次性提示（如「该平台没有可用音源脚本」），展示后可清除 */
    val message: String? = null,
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
    /** 在线曲目归属的音源标识（与在线搜索页同值，仅作标识） */
    private val onlineSourceId: String = "online",
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** 各平台榜单缓存：切平台回看时不重复请求（榜单列表变动很慢） */
    private val chartsByPlatform = mutableMapOf<String, List<OnlineChart>>()
    private var recommendJob: Job? = null

    /** 首选音质（与在线搜索页共用同一设置项） */
    private val preferredQuality: StateFlow<String> = settingsRepository.onlinePreferredQuality
        .stateIn(viewModelScope, SharingStarted.Eagerly, LxQuality.DEFAULT.key)

    init {
        loadCharts()
        refreshRecommend()
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
                if (filtered.tracks.isNotEmpty() && current.day == localRecommendDay()) {
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

    // ── 排行榜 ──

    /** 拉取各平台榜单列表；榜单歌曲由对应的详情页按需加载。 */
    fun loadCharts() {
        if (_state.value.chart.loadingCharts) return
        viewModelScope.launch {
            _state.value = _state.value.copy(chart = _state.value.chart.copy(loadingCharts = true, error = null))

            val supportedPlatforms = runCatching {
                scriptRepository.loadAll()
                    .filter { it.loadError == null }
                    .flatMap { script ->
                        script.sources.filterValues { it.supports(LxAction.MUSIC_URL) }.keys
                    }
                    .toSet()
            }.getOrDefault(emptySet())
            val availablePlatforms = chartService.platforms.filter { it in supportedPlatforms }
            if (availablePlatforms.isEmpty()) {
                _state.value = _state.value.copy(
                    chart = ChartSectionState(
                        platformNames = chartService.platformNames,
                        error = "暂无可用的在线榜单音源，请先导入支持榜单平台的 LX 音源脚本",
                    ),
                )
                return@launch
            }
            val outcomes = runCatching { chartService.charts(availablePlatforms) }.getOrDefault(emptyList())
            val success = outcomes.filterIsInstance<PlatformChartsOutcome.Success>()
            if (success.isEmpty()) {
                val failure = outcomes.filterIsInstance<PlatformChartsOutcome.Failure>().firstOrNull()
                _state.value = _state.value.copy(
                    chart = _state.value.chart.copy(
                        loadingCharts = false,
                        error = failure?.message ?: "排行榜加载失败，请检查网络",
                    ),
                )
                return@launch
            }
            success.forEach { chartsByPlatform[it.platform] = it.charts }

            // 保持用户已选平台（刷新场景），否则取第一个可用平台
            val platforms = success.map { it.platform }
            val platform = _state.value.chart.selectedPlatform?.takeIf { it in platforms } ?: platforms.first()
            val charts = chartsByPlatform[platform].orEmpty()
            _state.value = _state.value.copy(
                chart = _state.value.chart.copy(
                    platforms = platforms,
                    platformNames = chartService.platformNames,
                    selectedPlatform = platform,
                    charts = charts,
                    loadingCharts = false,
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
        if (_state.value.recommend.loading) return
        recommendJob?.cancel()
        recommendJob = viewModelScope.launch {
            _state.value = _state.value.copy(
                recommend = _state.value.recommend.copy(loading = true, error = null),
            )

            val enabled = runCatching { settingsRepository.aiRecommendEnabled.first() }.getOrDefault(false)
            val config = readAiConfig()
            if (!enabled) {
                _state.value = _state.value.copy(
                    recommend = _state.value.recommend.copy(
                        enabled = false,
                        configured = config.isUsable,
                        loading = false,
                        result = null,
                        day = null,
                        error = null,
                    ),
                )
                return@launch
            }
            if (!config.isUsable) {
                _state.value = _state.value.copy(
                    recommend = _state.value.recommend.copy(enabled = true, configured = false, loading = false, result = null, day = null, error = null),
                )
                return@launch
            }

            _state.value = _state.value.copy(
                recommend = _state.value.recommend.copy(enabled = true, configured = true),
            )

            val profile = runCatching { profileBuilder.build() }.getOrNull()
            if (profile == null || profile.isEmpty) {
                _state.value = _state.value.copy(
                    recommend = _state.value.recommend.copy(
                        loading = false,
                        error = "曲库还没有歌曲：先扫描本地/WebDAV 音源后再试",
                    ),
                )
                return@launch
            }

            val today = localRecommendDay()
            val cached = DailyRecommendSnapshot.decode(
                runCatching { settingsRepository.aiDailyRecommend.first() }.getOrDefault(""), today, profile,
            )
            if (cached != null) {
                _state.value = _state.value.copy(
                    recommend = _state.value.recommend.copy(loading = false, result = cached, day = today, error = null),
                )
                return@launch
            }

            runCatching { recommendService.recommend(profile, config) }.fold(
                onSuccess = { result ->
                    val latestProfile = runCatching { profileBuilder.build() }.getOrNull() ?: profile
                    val filteredResult = result.excludingOwnedSongs(latestProfile)
                    if (filteredResult.tracks.isNotEmpty() && localRecommendDay() == today) {
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
                    _state.value = _state.value.copy(
                        recommend = _state.value.recommend.copy(loading = false, error = aiErrorMessage(e)),
                    )
                },
            )
        }
    }

    fun playRecommend(index: Int) {
        val tracks = _state.value.recommend.result?.tracks.orEmpty()
        val target = tracks.getOrNull(index) ?: return
        val results = tracks.map { it.result }
        playResults(results, index, target.result.platform)
    }

    // ── 内部 ──

    /**
     * 统一播放入口：脚本可用性校验 → 队列整体登记会话 → 交给播放端口。
     *
     * 为什么先校验脚本：直链解析发生在播放链路内部，没有脚本时用户点下去毫无反馈，
     * 体验上等同「坏了」；此处提前给出可操作提示（与在线搜索页同一取舍）。
     */
    private fun playResults(results: List<OnlineSearchResult>, index: Int, platform: String) {
        val target = results.getOrNull(index) ?: return
        val quality = LxQuality.fromKey(preferredQuality.value)?.key
        viewModelScope.launch {
            if (!hasUsableScript(platform)) {
                val name = _state.value.chart.platformNames[platform] ?: platform
                _state.value = _state.value.copy(
                    message = "还没有能解析「$name」的音源脚本，请到「在线音源脚本」导入后再播放。",
                )
                return@launch
            }
            // 队列 = 当前列表全量（点哪首播哪首，后续按列表顺序走）
            val songs: List<Song> = results.map { it.toSong(onlineSourceId, quality) }
            OnlineTrackSession.remember(songs)
            playback.play(target.toSong(onlineSourceId, quality).id, songs)
        }
    }

    /** 是否存在已加载且声明了该平台的脚本（与在线搜索页同判据） */
    private suspend fun hasUsableScript(platform: String): Boolean =
        runCatching {
            scriptRepository.loadAll().any { script ->
                script.loadError == null && script.sources[platform]?.supports(LxAction.MUSIC_URL) == true
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

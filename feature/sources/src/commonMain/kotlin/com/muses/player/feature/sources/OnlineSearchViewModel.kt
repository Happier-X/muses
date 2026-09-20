package com.muses.player.feature.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.model.Song
import com.muses.player.core.data.repository.SettingsRepository
import com.muses.player.core.lxsdk.LxQuality
import com.muses.player.core.lxsdk.LxScriptRepository
import com.muses.player.core.model.online.OnlineTrackSession
import com.muses.player.core.playback.PlaybackPort
import com.muses.player.core.search.OnlineSearchResult
import com.muses.player.core.search.OnlineSearchService
import com.muses.player.core.search.PlatformSearchOutcome
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 单个平台在本次搜索中的状态（结果 + 分页 + 错误） */
data class PlatformSearchState(
    val platform: String,
    val displayName: String,
    val results: List<OnlineSearchResult> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = false,
    /** 已加载页数（用于加载更多） */
    val loadedPage: Int = 0,
)

/** 在线搜索页状态 */
data class OnlineSearchUiState(
    val keyword: String = "",
    val searching: Boolean = false,
    /** 已发起过搜索（用于区分「未搜」与「无结果」） */
    val searched: Boolean = false,
    val platforms: List<PlatformSearchState> = emptyList(),
    /** 当前平台筛选；null = 全部平台 */
    val filterPlatform: String? = null,
    val message: String? = null,
) {
    /** 按筛选过滤后的平台列表 */
    val visiblePlatforms: List<PlatformSearchState>
        get() = filterPlatform?.let { f -> platforms.filter { it.platform == f } } ?: platforms

    val totalResults: Int get() = platforms.sumOf { it.results.size }
}

/**
 * 在线搜索 ViewModel。
 *
 * 能力：
 * - 多平台并行搜索（部分平台失败不影响其它平台，失败原因按平台展示）；
 * - 按平台筛选；
 * - 分页加载更多（**注意咪咕固定每页 20 条**，见 MgSearchProvider）；
 * - 点结果直接播放：构造内存态 [Song]（不落库）→ 登记会话临时表 → 交给播放端口。
 *
 * 播放为什么登记 [OnlineTrackSession]：在线曲目不入库，而桌面播放链路按 songId 查库，
 * 登记后 songLookup 才有回退来源（详见该类注释）。
 */
class OnlineSearchViewModel(
    private val searchService: OnlineSearchService,
    private val playback: PlaybackPort,
    /** 音源脚本仓库：播放前校验该平台是否有可用脚本，避免「点了没反应」 */
    private val scriptRepository: LxScriptRepository,
    private val settingsRepository: SettingsRepository,
    /** 在线曲目归属的音源 id（脚本仓库按平台路由，此值仅作标识） */
    private val onlineSourceId: String = "online",
) : ViewModel() {

    private val _state = MutableStateFlow(
        OnlineSearchUiState(
            platforms = searchService.platforms.map {
                PlatformSearchState(platform = it, displayName = searchService.platformNames.getValue(it))
            },
        ),
    )
    val state: StateFlow<OnlineSearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    /**
     * 当前首选音质（DataStore 持久化；key 见 [LxQuality]）。
     * 仅作偏好：脚本未声明该档位时会就近回退。
     */
    val preferredQuality: StateFlow<String> = settingsRepository.onlinePreferredQuality
        .stateIn(viewModelScope, SharingStarted.Eagerly, LxQuality.DEFAULT.key)

    /**
     * 可选音质档位：取**已加载脚本声明的并集**（按档位升序）。
     *
     * 不从固定列表取：不同音源脚本支持档位差异大（如咪咕只到 flac，QQ 才有多档全景声），
     * 声明并集才能避免用户选到「脚本根本不支持」的档位。
     * 尚未加载任何脚本时为空 → UI 隐藏音质选择行。
     */
    private val _availableQualities = MutableStateFlow<List<LxQuality>>(emptyList())
    val availableQualities: StateFlow<List<LxQuality>> = _availableQualities.asStateFlow()

    fun setPreferredQuality(qualityKey: String) {
        viewModelScope.launch { settingsRepository.setOnlinePreferredQuality(qualityKey) }
    }

    /** 从已加载脚本汇总可选音质（供 UI 展示） */
    fun refreshAvailableQualities() {
        viewModelScope.launch {
            _availableQualities.value = runCatching {
                scriptRepository.loadAll()
                    .filter { it.loadError == null }
                    .flatMap { script -> script.sources.values.flatMap { it.qualitys } }
                    .mapNotNull(LxQuality::fromKey)
                    .distinct()
                    .sortedBy { it.rank }
            }.getOrDefault(emptyList())
        }
    }

    private fun currentQuality(): LxQuality? = LxQuality.fromKey(preferredQuality.value)

    fun updateKeyword(value: String) {
        _state.value = _state.value.copy(keyword = value)
    }

    /**
     * 预填关键词（首页搜索框带入）。
     * 只改 keyword，不动已搜结果：调用方随后自行调 [search] 触发新搜索。
     */
    fun prefillKeyword(value: String) {
        _state.value = _state.value.copy(keyword = value)
    }

    fun setFilter(platform: String?) {
        _state.value = _state.value.copy(filterPlatform = platform)
    }

    /** 发起搜索（覆盖式）：重置全部平台状态后并行搜索 */
    fun search() {
        val keyword = _state.value.keyword.trim()
        if (keyword.isEmpty()) {
            _state.value = _state.value.copy(message = "请输入搜索关键词")
            return
        }
        refreshAvailableQualities()
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.value = _state.value.copy(
                searching = true,
                searched = true,
                message = null,
                platforms = searchService.platforms.map { p ->
                    PlatformSearchState(
                        platform = p,
                        displayName = searchService.platformNames.getValue(p),
                        loading = true,
                    )
                },
            )
            val outcomes = searchService.searchAll(keyword, page = 1, pageSize = DEFAULT_PAGE_SIZE)
            applyOutcomes(outcomes, page = 1)
            _state.value = _state.value.copy(searching = false)
        }
    }

    /** 加载更多（仅对当前筛选的平台，或全部平台） */
    fun loadMore(platform: String) {
        val keyword = _state.value.keyword.trim()
        if (keyword.isEmpty()) return
        val current = _state.value.platforms.firstOrNull { it.platform == platform } ?: return
        if (current.loading || current.loadingMore || !current.hasMore) return

        viewModelScope.launch {
            updatePlatform(platform) { it.copy(loadingMore = true) }
            val nextPage = current.loadedPage + 1
            val outcome = runCatching {
                searchService.search(platform, keyword, page = nextPage, pageSize = DEFAULT_PAGE_SIZE)
            }
            applyOutcomes(
                listOf(
                    outcome.fold(
                        onSuccess = { PlatformSearchOutcome.Success(it) },
                        onFailure = { e ->
                            PlatformSearchOutcome.Failure(
                                platform = platform,
                                displayName = current.displayName,
                                message = e.message ?: "加载失败",
                                cause = e,
                            )
                        },
                    ),
                ),
                page = nextPage,
                append = true,
            )
        }
    }

    /**
     * 播放某平台结果中的第 [index] 首：该平台的当前结果集即播放队列。
     * 在线直链会过期，故每次播放都重新解析（由播放链路负责）。
     */
    fun playResult(platform: String, index: Int) {
        val platformState = _state.value.platforms.firstOrNull { it.platform == platform } ?: return
        val target = platformState.results.getOrNull(index) ?: return
        val quality = currentQuality()?.key
        val songs: List<Song> = platformState.results.map { it.toSong(onlineSourceId, quality) }
        viewModelScope.launch {
            // 播放前校验：该平台是否有脚本能解析直链。
            // 否则用户点播放后毫无反馈（直链解析在播放链路内异步失败），体验很差。
            if (!hasUsableScript(platform)) {
                _state.value = _state.value.copy(
                    message = "还没有能解析「${platformLabel(platform)}」的音源脚本。" +
                        "请到「在线音源脚本」导入对应脚本后再播放。",
                )
                return@launch
            }
            // 先登记再播放：桌面 songLookup 依赖此回退
            OnlineTrackSession.remember(songs)
            playback.play(target.toSong(onlineSourceId, quality).id, songs)
        }
    }

    /** 是否存在已加载且声明了该平台的脚本 */
    private suspend fun hasUsableScript(platform: String): Boolean =
        runCatching {
            scriptRepository.loadAll().any { script ->
                script.loadError == null && script.sources.containsKey(platform)
            }
        }.getOrDefault(false)

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    // ── 内部 ──

    private fun applyOutcomes(
        outcomes: List<PlatformSearchOutcome>,
        page: Int,
        append: Boolean = false,
    ) {
        var next = _state.value
        outcomes.forEach { outcome ->
            val existing = next.platforms.firstOrNull { it.platform == outcome.platform }
            val updated = when (outcome) {
                is PlatformSearchOutcome.Success -> {
                    val merged = if (append) {
                        // 去重追加：平台内可能出现重复条目
                        val seen = existing?.results?.map { it.songId }?.toSet().orEmpty()
                        existing?.results.orEmpty() + outcome.page.results.filter { it.songId !in seen }
                    } else {
                        outcome.page.results
                    }
                    (existing ?: PlatformSearchState(outcome.platform, outcome.platform)).copy(
                        results = merged,
                        loading = false,
                        loadingMore = false,
                        error = null,
                        hasMore = outcome.page.hasMore ?: (outcome.page.results.size >= DEFAULT_PAGE_SIZE),
                        loadedPage = page,
                    )
                }
                is PlatformSearchOutcome.Failure -> {
                    (existing ?: PlatformSearchState(outcome.platform, outcome.displayName)).copy(
                        loading = false,
                        loadingMore = false,
                        error = outcome.message,
                    )
                }
            }
            next = next.copy(
                platforms = next.platforms.map { if (it.platform == updated.platform) updated else it },
            )
        }
        _state.value = next
    }

    private fun updatePlatform(platform: String, transform: (PlatformSearchState) -> PlatformSearchState) {
        _state.value = _state.value.copy(
            platforms = _state.value.platforms.map { if (it.platform == platform) transform(it) else it },
        )
    }

    private companion object {
        /** 默认每页条数；咪咕会固定返回 20（其 provider 内部处理） */
        const val DEFAULT_PAGE_SIZE = 20

        fun platformLabel(platform: String): String = when (platform) {
            "kw" -> "酷我音乐"
            "kg" -> "酷狗音乐"
            "tx" -> "QQ音乐"
            "wy" -> "网易云音乐"
            "mg" -> "咪咕音乐"
            else -> platform
        }
    }
}

package com.muses.player.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.repository.SettingsRepository
import com.muses.player.core.lxsdk.LxAction
import com.muses.player.core.lxsdk.LxQuality
import com.muses.player.core.lxsdk.LxScriptRepository
import com.muses.player.core.model.Song
import com.muses.player.core.model.online.OnlineTrackSession
import com.muses.player.core.playback.PlaybackPort
import com.muses.player.core.search.OnlineChartService
import com.muses.player.core.search.OnlineSearchResult
import com.muses.player.core.ui.components.MusesSnackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class ChartDetailUiState(
    val songs: List<OnlineSearchResult> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
)

/** 单个排行榜歌曲页状态与播放逻辑。 */
class ChartDetailViewModel(
    private val chartService: OnlineChartService,
    private val chartCacheStore: OnlineChartCacheStore,
    private val scriptRepository: LxScriptRepository,
    private val settingsRepository: SettingsRepository,
    private val playback: PlaybackPort,
    private val platform: String,
    private val chartId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(ChartDetailUiState())
    val state: StateFlow<ChartDetailUiState> = _state.asStateFlow()
    private val preferredQuality = settingsRepository.onlinePreferredQuality.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        LxQuality.DEFAULT.key,
    )
    private var page = 0
    private var loadJob: Job? = null
    private var loadSeq = 0L

    init { loadPage(1, append = false) }

    fun retry() = loadPage(1, append = false, forceRefresh = true)

    fun refresh() = loadPage(1, append = false, forceRefresh = true)

    fun loadMore() {
        if (!_state.value.hasMore || _state.value.loadingMore || _state.value.loading || _state.value.refreshing) return
        loadPage(page + 1, append = true)
    }

    private fun loadPage(targetPage: Int, append: Boolean, forceRefresh: Boolean = false) {
        val requestSeq = ++loadSeq
        loadJob?.cancel()
        _state.value = _state.value.copy(
            loading = !append && _state.value.songs.isEmpty(),
            refreshing = !append && _state.value.songs.isNotEmpty(),
            loadingMore = append,
            error = if (append) _state.value.error else null,
        )
        loadJob = viewModelScope.launch {
            var cached: CachedChartSongs? = null
            var updateInfo: String? = null
            if (!append) {
                cached = runCatching { chartCacheStore.loadSongs(platform, chartId) }.getOrNull()
                updateInfo = runCatching {
                    chartCacheStore.loadCatalogs()[platform]?.charts?.firstOrNull { it.chartId == chartId }?.updateInfo
                }.getOrNull()
                if (requestSeq != loadSeq) return@launch
                if (cached != null) {
                    page = cached.page
                    _state.value = _state.value.copy(
                        songs = cached.songs,
                        loading = false,
                        refreshing = forceRefresh || !chartCacheStore.isSongsFresh(cached.updatedAt, updateInfo),
                        hasMore = cached.hasMore,
                        error = null,
                    )
                    if (!forceRefresh && chartCacheStore.isSongsFresh(cached.updatedAt, updateInfo) &&
                        cached.songs.none { it.platform == "wy" && !it.musicInfoJson.contains("\"catalogQuality\"") }) return@launch
                }
            }
            _state.value = _state.value.copy(
                loading = !append && _state.value.songs.isEmpty(),
                refreshing = !append && _state.value.songs.isNotEmpty(),
                loadingMore = append,
                error = null,
            )
            runCatching {
                chartService.chartSongs(platform, chartId, page = targetPage, pageSize = PAGE_SIZE)
            }.onSuccess { result ->
                if (requestSeq != loadSeq) return@launch
                page = targetPage
                val hasMore = result.hasMore ?: (result.results.size >= PAGE_SIZE)
                val songs = if (append) _state.value.songs + result.results else result.results
                val updatedAt = Clock.System.now().toEpochMilliseconds()
                runCatching {
                    chartCacheStore.saveSongs(
                        platform,
                        chartId,
                        CachedChartSongs(songs, targetPage, hasMore, updatedAt),
                    )
                }
                if (requestSeq != loadSeq) return@launch
                _state.value = _state.value.copy(
                    songs = songs,
                    loading = false,
                    refreshing = false,
                    loadingMore = false,
                    hasMore = hasMore,
                    error = null,
                )
            }.onFailure { failure ->
                if (failure is kotlinx.coroutines.CancellationException) throw failure
                if (requestSeq != loadSeq) return@launch
                val message = failure.message ?: "排行榜歌曲加载失败，请稍后重试"
                _state.value = _state.value.copy(
                    loading = false,
                    refreshing = false,
                    loadingMore = false,
                    error = if (append || cached != null) null else message,
                )
                if (append) MusesSnackbar.show(message)
            }
        }
    }

    fun play(index: Int) = playAt(index, shuffle = false)

    fun shuffle() {
        val songs = _state.value.songs
        if (songs.isNotEmpty()) playAt(songs.indices.random(), shuffle = true)
    }

    private fun playAt(index: Int, shuffle: Boolean) {
        val results = _state.value.songs
        val target = results.getOrNull(index) ?: return
        val quality = LxQuality.fromKey(preferredQuality.value)?.key
        viewModelScope.launch {
            val hasUsableScript = runCatching {
                scriptRepository.loadAll().any { script ->
                    script.loadError == null && script.sources.values.any { it.supports(LxAction.MUSIC_URL) }
                }
            }.getOrDefault(false)
            if (!hasUsableScript) {
                MusesSnackbar.show("还没有可用的在线音源脚本，请先导入脚本。")
                return@launch
            }
            val songs: List<Song> = results.map { it.toSong(ONLINE_SOURCE_ID, quality) }
            OnlineTrackSession.remember(songs)
            playback.play(target.toSong(ONLINE_SOURCE_ID, quality).id, songs)
            if (shuffle) playback.setShuffleEnabled(true)
        }
    }

    private companion object {
        const val PAGE_SIZE = 30
        const val ONLINE_SOURCE_ID = "online"
    }
}

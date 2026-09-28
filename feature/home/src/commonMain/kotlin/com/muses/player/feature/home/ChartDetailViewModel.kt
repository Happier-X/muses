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

data class ChartDetailUiState(
    val songs: List<OnlineSearchResult> = emptyList(),
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
)

/** 单个排行榜歌曲页状态与播放逻辑。 */
class ChartDetailViewModel(
    private val chartService: OnlineChartService,
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

    init {
        loadFirstPage()
    }

    fun retry() = loadFirstPage()

    fun loadMore() {
        if (!_state.value.hasMore || _state.value.loadingMore || _state.value.loading) return
        loadPage(page + 1, append = true)
    }

    private fun loadFirstPage() {
        loadPage(1, append = false)
    }

    private fun loadPage(targetPage: Int, append: Boolean) {
        loadJob?.cancel()
        _state.value = _state.value.copy(
            loading = !append,
            loadingMore = append,
            error = if (append) _state.value.error else null,
        )
        loadJob = viewModelScope.launch {
            runCatching {
                chartService.chartSongs(platform, chartId, page = targetPage, pageSize = PAGE_SIZE)
            }.onSuccess { result ->
                page = targetPage
                val hasMore = result.hasMore ?: (result.results.size >= PAGE_SIZE)
                _state.value = _state.value.copy(
                    songs = if (append) _state.value.songs + result.results else result.results,
                    loading = false,
                    loadingMore = false,
                    hasMore = hasMore,
                    error = null,
                )
            }.onFailure { failure ->
                val message = failure.message ?: "排行榜歌曲加载失败，请稍后重试"
                _state.value = _state.value.copy(
                    loading = false,
                    loadingMore = false,
                    error = if (append) null else message,
                )
                if (append) MusesSnackbar.show(message)
            }
        }
    }

    fun play(index: Int) {
        val results = _state.value.songs
        val target = results.getOrNull(index) ?: return
        val quality = LxQuality.fromKey(preferredQuality.value)?.key
        viewModelScope.launch {
            val hasUsableScript = runCatching {
                scriptRepository.loadAll().any { script ->
                    script.loadError == null && script.sources[platform]?.supports(LxAction.MUSIC_URL) == true
                }
            }.getOrDefault(false)
            if (!hasUsableScript) {
                MusesSnackbar.show("还没有能解析此排行榜音源的 LX 脚本，请先导入对应脚本。")
                return@launch
            }
            val songs: List<Song> = results.map { it.toSong(ONLINE_SOURCE_ID, quality) }
            OnlineTrackSession.remember(songs)
            playback.play(target.toSong(ONLINE_SOURCE_ID, quality).id, songs)
        }
    }

    private companion object {
        const val PAGE_SIZE = 30
        const val ONLINE_SOURCE_ID = "online"
    }
}

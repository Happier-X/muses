package com.muses.player.feature.scrape

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.model.Song
import com.muses.player.core.scrape.editmeta.EditCloudMetaResult
import com.muses.player.core.scrape.editmeta.EditCloudMetaSearch
import com.muses.player.core.scrape.editmeta.EditCloudMetaQuery
import com.muses.player.core.scrape.writeback.WritebackOrchestrator
import com.muses.player.core.model.scrape.ScrapeCandidate
import com.muses.player.core.model.scrape.ScrapeChanges
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** EditMetaSheet UI 状态 */
data class EditMetaUiState(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val searching: Boolean = false,
    /** 云搜失败标记（网络等异常；UI 显示重试提示） */
    val searchFailed: Boolean = false,
    val result: EditCloudMetaResult? = null,
    /** 选中的封面候选下标；null = 不改封面 */
    val selectedCoverIndex: Int? = null,
    val songId: String? = null,
)

/**
 * 编辑歌曲信息弹窗编排：editmeta 三维云搜（文本/封面/歌词）→ 用户确认 → WritebackOrchestrator 写回。
 * 歌词维度在 LyricsSearchPort 未注入时由数据层降级（status != OK），UI 显示跳过文案。
 */
class EditMetaViewModel constructor(
    private val editCloudMetaSearch: EditCloudMetaSearch,
    private val songRepository: SongRepository,
    private val writebackOrchestrator: WritebackOrchestrator,
) : ViewModel() {

    private val _ui = MutableStateFlow(EditMetaUiState())
    val ui: StateFlow<EditMetaUiState> = _ui.asStateFlow()

    private var currentSong: Song? = null

    fun load(song: Song) {
        currentSong = song
        _ui.value = EditMetaUiState(
            title = song.title,
            artist = song.artist.orEmpty(),
            album = song.album.orEmpty(),
            songId = song.id,
        )
    }

    fun updateTitle(v: String) { _ui.value = _ui.value.copy(title = v) }
    fun updateArtist(v: String) { _ui.value = _ui.value.copy(artist = v) }
    fun updateAlbum(v: String) { _ui.value = _ui.value.copy(album = v) }

    fun selectCover(index: Int) {
        _ui.value = _ui.value.copy(selectedCoverIndex = index)
    }

    /** 三维云搜：以当前输入为查询词 */
    fun search() {
        val state = _ui.value
        if (state.songId == null || state.title.isBlank()) return
        viewModelScope.launch {
            _ui.value = state.copy(searching = true)
            try {
                val result = editCloudMetaSearch.search(
                    EditCloudMetaQuery(
                        songId = state.songId!!,
                        title = state.title.trim(),
                        artist = state.artist.trim().ifEmpty { null },
                        album = state.album.trim().ifEmpty { null },
                    ),
                )
                // 命中文本候选时自动回填输入框（用户可再改）
                val bestTitle = result.text.items.firstOrNull()?.title ?: state.title
                val bestArtist = result.text.items.firstOrNull()?.artist ?: state.artist.ifEmpty { null }
                val bestAlbum = result.text.items.firstOrNull()?.album ?: state.album.ifEmpty { null }
                _ui.value = _ui.value.copy(
                    searching = false,
                    searchFailed = false,
                    result = result,
                    selectedCoverIndex = 0, // 默认选中最优封面
                    title = bestTitle,
                    artist = bestArtist.orEmpty(),
                    album = bestAlbum.orEmpty(),
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                // 前置 rethrow：VM 销毁时交回结构化并发（spec 陷阱 #14，禁止吞取消）
                throw e
            } catch (e: Exception) {
                // 网络等失败：保持输入，清结果并置失败标记（FailureCopy 语义在写回层）
                _ui.value = _ui.value.copy(searching = false, searchFailed = true, result = null)
            }
        }
    }

    /** 应用：三维结果组装 ScrapeChanges 走 WritebackOrchestrator（journal 可撤销） */
    fun apply() {
        val state = _ui.value
        val songId = state.songId ?: return
        viewModelScope.launch {
            val song = songRepository.getSong(songId) ?: return@launch
            val changes = ScrapeChanges(
                title = state.title.takeIf { it.isNotBlank() && it != song.title },
                artist = state.artist.takeIf { it.isNotEmpty() }?.takeIf { it != song.artist },
                album = state.album.takeIf { it.isNotEmpty() }?.takeIf { it != song.album },
                coverRemoteUrl = state.selectedCoverIndex
                    ?.let { idx -> state.result?.cover?.items?.getOrNull(idx)?.remoteUrl },
                lyrics = state.result?.lyrics?.takeIf { it.status == com.muses.player.core.scrape.editmeta.EditDimStatus.OK }
                    ?.items?.firstOrNull()?.text?.takeIf { it != song.lyrics },
            )
            writebackOrchestrator.applyScrapeChanges(
                candidates = listOf(ScrapeCandidate(songId = song.id, song = song)),
                checkedIds = setOf(song.id),
                changesMap = mapOf(song.id to changes),
            )
        }
    }
}

package com.muses.player.feature.scrape

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.model.Song
import com.muses.player.core.model.scrape.OnlineTextMatchFailReason
import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.model.scrape.ScrapeCandidate
import com.muses.player.core.model.scrape.ScrapeChanges
import com.muses.player.core.scrape.cover.CoverMatcher
import com.muses.player.core.scrape.cover.OnlineCoverMatchFailReason
import com.muses.player.core.scrape.cover.OnlineCoverQuery
import com.muses.player.core.scrape.text.TextMetaMatcher
import com.muses.player.core.scrape.writeback.WritebackOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SingleScrapeState {
    data object Idle : SingleScrapeState
    data class Searching(val songTitle: String) : SingleScrapeState
    data class HasCandidates(val candidates: List<PreviewCandidate>, val selectedIndex: Int = 0) : SingleScrapeState
    data class Empty(val reason: String) : SingleScrapeState
    data class Writing(val count: Int = 1) : SingleScrapeState
    data object Success : SingleScrapeState
}

@HiltViewModel
class SingleScrapeViewModel @Inject constructor(
    private val textMetaMatcher: TextMetaMatcher,
    private val coverMatcher: CoverMatcher,
    private val songRepository: SongRepository,
    private val writebackOrchestrator: WritebackOrchestrator,
) : ViewModel() {

    private val _state = MutableStateFlow<SingleScrapeState>(SingleScrapeState.Idle)
    val state: StateFlow<SingleScrapeState> = _state.asStateFlow()

    private var currentSong: Song? = null

    fun search(song: Song) {
        currentSong = song
        viewModelScope.launch {
            _state.value = SingleScrapeState.Searching(song.title)
            try {
                val textOk = try {
                    textMetaMatcher.match(
                        OnlineTextQuery(
                            songId = song.id,
                            title = song.title,
                            path = song.path,
                            artist = song.artist,
                            album = song.album,
                            durationSec = song.durationSec.takeIf { it > 0 }?.toDouble(),
                            metaSources = song.metaSources,
                        ),
                    )
                } catch (e: CancellationException) { throw e } catch (_: Exception) {
                    com.muses.player.core.model.scrape.OnlineTextMatchResult.Fail(OnlineTextMatchFailReason.NETWORK)
                }
                val coverOk = try {
                    coverMatcher.match(OnlineCoverQuery(songId = song.id, title = song.title, artist = song.artist, album = song.album))
                } catch (e: CancellationException) { throw e } catch (_: Exception) {
                    com.muses.player.core.scrape.cover.OnlineCoverMatchResult.Fail(OnlineCoverMatchFailReason.NETWORK)
                }

                val hit = (textOk as? com.muses.player.core.model.scrape.OnlineTextMatchResult.Ok)?.hit
                val coverUrl = (coverOk as? com.muses.player.core.scrape.cover.OnlineCoverMatchResult.Ok)?.remoteUrl
                if (hit == null && coverUrl == null) {
                    val isNetwork = (textOk is com.muses.player.core.model.scrape.OnlineTextMatchResult.Fail && textOk.reason == OnlineTextMatchFailReason.NETWORK) ||
                        (coverOk is com.muses.player.core.scrape.cover.OnlineCoverMatchResult.Fail && coverOk.reason == OnlineCoverMatchFailReason.NETWORK)
                    _state.value = SingleScrapeState.Empty(if (isNetwork) "触发限流，稍后重试" else "暂无匹配")
                    return@launch
                }
                val confidence = (textOk as? com.muses.player.core.model.scrape.OnlineTextMatchResult.Ok)?.confidence?.name
                val candidate = PreviewCandidate(
                    songId = song.id,
                    songTitle = song.title,
                    currentTitle = song.title,
                    currentArtist = song.artist,
                    currentAlbum = song.album,
                    currentLyrics = song.lyrics,
                    matchedTitle = hit?.title,
                    matchedArtist = hit?.artist,
                    matchedAlbum = hit?.album,
                    matchedLyrics = null,
                    confidence = confidence,
                    coverUrl = coverUrl,
                    checked = true,
                )
                _state.value = SingleScrapeState.HasCandidates(listOf(candidate), 0)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = SingleScrapeState.Empty("搜索失败，请重试")
            }
        }
    }

    fun select(index: Int) {
        val s = _state.value as? SingleScrapeState.HasCandidates ?: return
        if (index !in s.candidates.indices) return
        _state.value = s.copy(selectedIndex = index)
    }

    fun updateCandidateEdit(title: String?, artist: String?, album: String?, lyrics: String?) {
        val s = _state.value as? SingleScrapeState.HasCandidates ?: return
        val idx = s.selectedIndex
        val list = s.candidates.toMutableList()
        val cur = list[idx]
        list[idx] = cur.copy(editTitle = title, editArtist = artist, editAlbum = album, editLyrics = lyrics)
        _state.value = s.copy(candidates = list)
    }

    fun applySelected(onSuccess: () -> Unit = {}) {
        val s = _state.value as? SingleScrapeState.HasCandidates ?: return
        val candidate = s.candidates.getOrNull(s.selectedIndex) ?: return
        val song = currentSong ?: return
        viewModelScope.launch {
            _state.value = SingleScrapeState.Writing(1)
            try {
                val changes = ScrapeChanges(
                    title = candidate.resolvedTitle(),
                    artist = candidate.resolvedArtist(),
                    album = candidate.resolvedAlbum(),
                    coverRemoteUrl = candidate.coverUrl,
                    lyrics = candidate.resolvedLyrics(),
                )
                writebackOrchestrator.applyScrapeChanges(
                    candidates = listOf(ScrapeCandidate(songId = song.id, song = song)),
                    checkedIds = setOf(song.id),
                    changesMap = mapOf(song.id to changes),
                )
                _state.value = SingleScrapeState.Success
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = SingleScrapeState.Empty("写回失败，请重试")
            }
        }
    }

    fun reset() {
        _state.value = SingleScrapeState.Idle
        currentSong = null
    }
}

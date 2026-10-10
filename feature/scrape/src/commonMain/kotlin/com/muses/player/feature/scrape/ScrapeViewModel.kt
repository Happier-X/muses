package com.muses.player.feature.scrape

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.model.Song
import com.muses.player.core.model.scrape.OnlineTextMatchFailReason
import com.muses.player.core.model.scrape.OnlineTextMatchResult
import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.model.scrape.ScrapeCandidate
import com.muses.player.core.model.scrape.ScrapeChanges
import com.muses.player.core.model.scrape.WritebackStatus
import com.muses.player.core.scrape.cover.CoverMatcher
import com.muses.player.core.scrape.cover.OnlineCoverMatchFailReason
import com.muses.player.core.scrape.cover.OnlineCoverMatchResult
import com.muses.player.core.scrape.cover.OnlineCoverQuery
import com.muses.player.core.scrape.queue.ScrapeQueueStore
import com.muses.player.core.scrape.text.TextMetaMatcher
import com.muses.player.core.scrape.writeback.WritebackOrchestrator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScrapeViewModel(
    private val queueStore: ScrapeQueueStore,
    private val textMetaMatcher: TextMetaMatcher,
    private val coverMatcher: CoverMatcher,
    private val writebackOrchestrator: WritebackOrchestrator,
    private val songRepository: SongRepository,
) : ViewModel() {
    private val _queueSongIds = MutableStateFlow<List<String>>(emptyList())
    val queueSongIds: StateFlow<List<String>> = _queueSongIds.asStateFlow()
    private val _queueTitles = MutableStateFlow<Map<String, String>>(emptyMap())
    val queueTitles: StateFlow<Map<String, String>> = _queueTitles.asStateFlow()
    private val _pageState = MutableStateFlow<ScrapePageState>(ScrapePageState.Queue)
    val pageState: StateFlow<ScrapePageState> = _pageState.asStateFlow()
    private val _throttleMessage = MutableStateFlow<String?>(null)
    val throttleMessage: StateFlow<String?> = _throttleMessage.asStateFlow()
    private val _throttledIds = MutableStateFlow<List<String>>(emptyList())
    val throttledIds: StateFlow<List<String>> = _throttledIds.asStateFlow()
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    private val _undoing = MutableStateFlow(false)
    val undoing: StateFlow<Boolean> = _undoing.asStateFlow()
    private var matchingJob: Job? = null
    private var interruptedPreview = ScrapePageState.Preview(emptyList())
    private var resultPreview = ScrapePageState.Preview(emptyList())
    private val reviewTracker = ReviewQueueTracker()
    val pendingReviewQueue: StateFlow<List<String>> = reviewTracker.queue
    var lastJournalId: String? = null
        private set

    init {
        reloadQueue()
        viewModelScope.launch { queueStore.updated.collect { reloadQueue() } }
    }

    fun reloadQueue() = viewModelScope.launch {
            try {
                val ids = queueStore.load().map { it.songId }
                _queueSongIds.value = ids
                _queueTitles.value = songRepository.getSongs(ids).mapValues { it.value.title }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _errorMessage.value = "读取待刮削歌曲失败，请返回页面重试"
            }
    }

    fun removeFromQueue(songIds: List<String>) {
        viewModelScope.launch { queueStore.remove(songIds) }
    }

    fun clearQueue() {
        viewModelScope.launch { queueStore.clear() }
    }

    private suspend fun matchTextAndCover(song: Song): Pair<OnlineTextMatchResult, OnlineCoverMatchResult> =
        coroutineScope {
            val text = async {
                try {
                    textMetaMatcher.match(
                        OnlineTextQuery(
                            songId = song.id, title = song.title, path = song.path,
                            artist = song.artist, album = song.album,
                            durationSec = song.durationSec.takeIf { it > 0 }?.toDouble(),
                            metaSources = song.metaSources,
                        ),
                    )
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { OnlineTextMatchResult.Fail(OnlineTextMatchFailReason.NETWORK) }
            }
            val cover = async {
                try {
                    coverMatcher.match(OnlineCoverQuery(song.id, song.title, song.artist, song.album))
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { OnlineCoverMatchResult.Fail(OnlineCoverMatchFailReason.NETWORK) }
            }
            text.await() to cover.await()
        }

    fun startMatching() {
        if (_pageState.value != ScrapePageState.Queue) return
        matchSongs(_queueSongIds.value, ScrapePageState.Preview(emptyList()), retry = false)
    }

    fun retrySingle(songId: String) {
        val base = _pageState.value as? ScrapePageState.Preview ?: return
        matchSongs(listOf(songId), base, retry = true)
    }

    /** 文件失败时沿用本次核对过的变更，不能因曲库已更新而丢失重试内容。 */
    fun reviewWritebackFailure(songId: String) {
        if (_pageState.value !is ScrapePageState.Result || _undoing.value) return
        _pageState.value = resultPreview.copy(items = resultPreview.items.map {
            if (it.songId == songId) it else it.copy(checked = false, checkedFields = emptySet())
        })
    }

    fun retryThrottled() {
        val base = _pageState.value as? ScrapePageState.Preview ?: return
        matchSongs(_throttledIds.value, base, retry = true)
    }

    /** 首次匹配与重试共用一条路径；结果、人工选择和未匹配分组始终保留。 */
    private fun matchSongs(ids: List<String>, base: ScrapePageState.Preview, retry: Boolean) {
        if (ids.isEmpty() || matchingJob?.isActive == true || _undoing.value) return
        _errorMessage.value = null
        if (!retry) {
            _throttledIds.value = emptyList()
            _throttleMessage.value = null
        }
        interruptedPreview = base
        _pageState.value = ScrapePageState.Matching(0, ids.size, "准备匹配…")
        matchingJob = viewModelScope.launch {
            val items = base.items.toMutableList()
            val noMatch = base.noMatchIds.toMutableSet()
            val networkFailed = _throttledIds.value.toMutableSet()
            fun retainProgress() {
                interruptedPreview = ScrapePageState.Preview(items.toList(), noMatch.toList())
                _throttledIds.value = networkFailed.toList()
                _throttleMessage.value = networkFailed.takeIf { it.isNotEmpty() }
                    ?.let { "${it.size} 首网络请求失败或服务繁忙，可稍后重试" }
            }
            try {
                ids.forEachIndexed { index, songId ->
                    _pageState.value = ScrapePageState.Matching(index, ids.size, _queueTitles.value[songId] ?: "正在读取歌曲…")
                    // 读取异常不能当成歌曲已删除，外层给出错误并保留已完成结果。
                    val song = songRepository.getSong(songId)
                    if (song == null) {
                        queueStore.remove(listOf(songId))
                        items.removeAll { it.songId == songId }
                        noMatch.remove(songId)
                        networkFailed.remove(songId)
                        retainProgress()
                        return@forEachIndexed
                    }
                    _pageState.value = ScrapePageState.Matching(index, ids.size, "正在匹配：${song.title}")
                    if (retry) {
                        textMetaMatcher.invalidateNegativeCache(songId)
                        coverMatcher.invalidateNegativeCache(songId)
                    }
                    val (text, cover) = matchTextAndCover(song)
                    val hit = (text as? OnlineTextMatchResult.Ok)?.hit
                    val coverUrl = (cover as? OnlineCoverMatchResult.Ok)?.remoteUrl
                    val textNetwork = text is OnlineTextMatchResult.Fail && text.reason == OnlineTextMatchFailReason.NETWORK
                    val coverNetwork = cover is OnlineCoverMatchResult.Fail && cover.reason == OnlineCoverMatchFailReason.NETWORK
                    val failedRequests = buildSet {
                        if (textNetwork) add("text")
                        if (coverNetwork) add("cover")
                    }
                    noMatch.remove(songId)
                    networkFailed.remove(songId)
                    if (failedRequests.isNotEmpty()) networkFailed.add(songId)
                    if (hit == null && coverUrl == null) {
                        if (failedRequests.isEmpty()) noMatch.add(songId)
                    } else {
                        val candidate = PreviewCandidate(
                            songId = song.id, songTitle = song.title, currentTitle = song.title,
                            currentArtist = song.artist, currentAlbum = song.album, currentLyrics = song.lyrics,
                            matchedTitle = hit?.title, matchedArtist = hit?.artist, matchedAlbum = hit?.album,
                            confidence = (text as? OnlineTextMatchResult.Ok)?.confidence?.name,
                            coverUrl = coverUrl, currentCoverUri = song.coverUri, failedRequests = failedRequests,
                        )
                        val previousIndex = items.indexOfFirst { it.songId == songId }
                        if (previousIndex >= 0) {
                            val previous = items[previousIndex]
                            val merged = candidate.copy(
                                matchedTitle = if (textNetwork) previous.matchedTitle else candidate.matchedTitle,
                                matchedArtist = if (textNetwork) previous.matchedArtist else candidate.matchedArtist,
                                matchedAlbum = if (textNetwork) previous.matchedAlbum else candidate.matchedAlbum,
                                coverUrl = if (coverNetwork) previous.coverUrl else candidate.coverUrl,
                                confidence = if (textNetwork) previous.confidence else candidate.confidence,
                                editTitle = previous.editTitle, editArtist = previous.editArtist,
                                editAlbum = previous.editAlbum, editLyrics = previous.editLyrics,
                            )
                            val selected = previous.checkedFields.intersect(merged.availableFields())
                            items[previousIndex] = merged.copy(checked = selected.isNotEmpty(), checkedFields = selected)
                        } else items.add(candidate)
                    }
                    retainProgress()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _errorMessage.value = "匹配中断，已完成的结果已保留；返回队列可重新匹配"
            }
            _pageState.value = interruptedPreview
        }
    }

    /** 停止网络匹配，保留已完成的候选；未处理歌曲仍在队列中。 */
    fun stopMatching() {
        if (_pageState.value !is ScrapePageState.Matching) return
        matchingJob?.cancel()
        _pageState.value = interruptedPreview
    }

    fun toggleChecked(songId: String) = updatePreview { it.copy(items = it.items.toggleChecked(songId)) }
    fun setAllChecked(checked: Boolean) = updatePreview { it.copy(items = it.items.setAllChecked(checked)) }
    fun toggleField(songId: String, field: String) = updatePreview { it.copy(items = it.items.toggleField(songId, field)) }
    fun setAllFields(field: String, checked: Boolean) = updatePreview { it.copy(items = it.items.setAllFields(field, checked)) }
    fun selectFields(fields: Set<String>) = updatePreview { it.copy(items = it.items.selectFields(fields)) }

    fun updatePreviewItem(songId: String, title: String?, artist: String?, album: String?, lyrics: String? = null) =
        updatePreview { it.copy(items = it.items.updateItem(songId, title, artist, album, lyrics)) }

    private inline fun updatePreview(transform: (ScrapePageState.Preview) -> ScrapePageState.Preview) {
        val state = _pageState.value as? ScrapePageState.Preview ?: return
        _pageState.value = transform(state)
    }

    fun dismissError() { _errorMessage.value = null }

    fun confirmWriteback() {
        val state = _pageState.value as? ScrapePageState.Preview ?: return
        val selected = state.items.map { it.copy(checkedFields = it.checkedFields.intersect(it.availableFields())) }
            .filter { it.checkedFields.isNotEmpty() }
        if (selected.isEmpty()) return
        _errorMessage.value = null
        _pageState.value = ScrapePageState.Writing(selected.size)
        viewModelScope.launch {
            try {
                val songs = songRepository.getSongs(selected.map { it.songId })
                val changes = selected.filter { it.songId in songs }.associate { item ->
                    item.songId to ScrapeChanges(
                        title = item.resolvedTitle().takeIf { "title" in item.checkedFields },
                        artist = item.resolvedArtist().takeIf { "artist" in item.checkedFields },
                        album = item.resolvedAlbum().takeIf { "album" in item.checkedFields },
                        coverRemoteUrl = item.coverUrl.takeIf { "cover" in item.checkedFields },
                        lyrics = item.resolvedLyrics().takeIf { "lyrics" in item.checkedFields },
                    )
                }
                if (changes.isEmpty()) {
                    _errorMessage.value = "所选歌曲已不在曲库中，请返回队列刷新"
                    _pageState.value = state
                    return@launch
                }
                val applied = writebackOrchestrator.applyScrapeChanges(
                    candidates = songs.values.map { ScrapeCandidate(it.id, it) },
                    checkedIds = changes.keys, changesMap = changes,
                )
                lastJournalId = applied.journalId
                val successful = applied.results.filter { it.status == WritebackStatus.SUCCESS }.map { it.songId }.toSet()
                resultPreview = state.copy(
                    items = state.items.filter { it.songId !in successful },
                    noMatchIds = state.noMatchIds.filter { it !in successful },
                )
                _throttledIds.value = _throttledIds.value.filter { it !in successful }
                _throttleMessage.value = _throttledIds.value.takeIf { it.isNotEmpty() }
                    ?.let { "${it.size} 首网络请求失败或服务繁忙，可稍后重试" }
                // 歌名快照不会随成功歌曲出队后的刷新丢失。
                _pageState.value = ScrapePageState.Result(
                    applied.results, applied.journalId, state.items.associate { it.songId to it.songTitle },
                )
                try {
                    queueStore.remove(successful.toList())
                    reloadQueue()
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { _errorMessage.value = "更新已完成，但队列刷新失败，请返回队列重试" }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _errorMessage.value = "应用失败，请检查文件或 WebDAV 连接后重试"
                _pageState.value = state
            }
        }
    }

    fun undoLastWriteback() {
        val state = _pageState.value as? ScrapePageState.Result ?: return
        if (_undoing.value) return
        _undoing.value = true
        viewModelScope.launch {
            try {
                val result = writebackOrchestrator.revertScrapeJournal(state.journalId)
                if (result.failed > 0) {
                    _errorMessage.value = "已恢复 ${result.reverted} 首，${result.failed} 首文件未保存，可重试或到下载页补传"
                    return@launch
                }
                lastJournalId = null
                _pageState.value = ScrapePageState.Queue
                reloadQueue()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _errorMessage.value = "恢复曲库失败，请重试" }
            finally { _undoing.value = false }
        }
    }

    fun backToQueue() {
        if (_pageState.value is ScrapePageState.Writing || _undoing.value) return
        matchingJob?.cancel()
        _pageState.value = ScrapePageState.Queue
        _errorMessage.value = null
        reloadQueue()
    }

    fun startReviewQueue(): String? {
        val preview = _pageState.value as? ScrapePageState.Preview ?: return null
        val ids = (preview.items.map { it.songId } + preview.noMatchIds + _throttledIds.value).distinct()
        return if (ids.isEmpty()) null else reviewTracker.start(ids)
    }

    fun advanceReview(songId: String): String? = reviewTracker.advance(songId)
    fun cancelReviewQueue() { reviewTracker.cancel() }

    fun refreshAfterExternalWriteback(songId: String) {
        updatePreview { state ->
            state.copy(items = state.items.filter { it.songId != songId }, noMatchIds = state.noMatchIds.filter { it != songId })
        }
        _throttledIds.value = _throttledIds.value.filter { it != songId }
        if (_throttledIds.value.isEmpty()) _throttleMessage.value = null
        viewModelScope.launch {
            try { queueStore.remove(listOf(songId)) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _errorMessage.value = "更新已完成，但队列刷新失败" }
            reloadQueue()
        }
    }
}

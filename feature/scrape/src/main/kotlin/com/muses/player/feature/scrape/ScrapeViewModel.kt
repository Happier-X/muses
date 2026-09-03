package com.muses.player.feature.scrape

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.model.scrape.OnlineTextMatchFailReason
import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.model.scrape.ScrapeCandidate
import com.muses.player.core.model.scrape.ScrapeChanges
import com.muses.player.core.model.scrape.WritebackResult
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.scrape.cover.CoverMatcher
import com.muses.player.core.scrape.cover.OnlineCoverMatchFailReason
import com.muses.player.core.scrape.cover.OnlineCoverQuery
import com.muses.player.core.scrape.queue.ScrapeQueueStore
import com.muses.player.core.scrape.text.TextMetaMatcher
import com.muses.player.core.scrape.writeback.WritebackOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 页面四态（对照 ScrapePage.vue pageState: queue/matching/preview/result） */
sealed interface ScrapePageState {
    /** 待刮削队列 */
    data object Queue : ScrapePageState

    /** 匹配中：currentItem 为正在匹配的歌名 */
    data class Matching(val current: Int, val total: Int, val currentItem: String) : ScrapePageState

    /** 候选预览确认（checkedIds 默认空 = 全不选，写回安全红线） */
    data class Preview(val items: List<PreviewCandidate>) : ScrapePageState

    /** 写回中：点“写回选中”后、文件/DB 落盘期间的过渡态，避免无反馈 */
    data class Writing(val count: Int) : ScrapePageState

    /** 写回结果 + 可撤销 journalId */
    data class Result(val results: List<WritebackResult>, val journalId: String) : ScrapePageState
}

/** 预览行：歌曲 + 匹配到的变更 + 封面候选 + 勾选态（09-03 可编辑：保留原值供对比，edit* 为用户覆写副本） */
data class PreviewCandidate(
    val songId: String,
    val songTitle: String,
    val currentTitle: String = songTitle,
    val currentArtist: String?,
    val currentAlbum: String? = null,
    val currentLyrics: String? = null,
    val matchedTitle: String?,
    val matchedArtist: String?,
    val matchedAlbum: String?,
    val matchedLyrics: String? = null,
    /** 匹配置信度展示（HIGH/MEDIUM/LOW），null = 文本链未命中 */
    val confidence: String?,
    val coverUrl: String?,
    val checked: Boolean = false,
    val editTitle: String? = null,
    val editArtist: String? = null,
    val editAlbum: String? = null,
    val editLyrics: String? = null,
) {
    fun resolvedTitle(): String? = editTitle ?: matchedTitle
    fun resolvedArtist(): String? = editArtist ?: matchedArtist
    fun resolvedAlbum(): String? = editAlbum ?: matchedAlbum
    fun resolvedLyrics(): String? = editLyrics ?: matchedLyrics
    fun hasLyricsChange(): Boolean = !resolvedLyrics().isNullOrBlank()
}

@HiltViewModel
class ScrapeViewModel @Inject constructor(
    private val queueStore: ScrapeQueueStore,
    private val textMetaMatcher: TextMetaMatcher,
    private val coverMatcher: CoverMatcher,
    private val writebackOrchestrator: WritebackOrchestrator,
    private val songRepository: SongRepository,
) : ViewModel() {

    // ---- queue 态数据 ----
    private val _queueSongIds = MutableStateFlow<List<String>>(emptyList())
    val queueSongIds: StateFlow<List<String>> = _queueSongIds.asStateFlow()

    /** songId → 歌名（队列只持久化 songId，展示时反查库；缺失回退占位文案） */
    private val _queueTitles = MutableStateFlow<Map<String, String>>(emptyMap())
    val queueTitles: StateFlow<Map<String, String>> = _queueTitles.asStateFlow()

    // ---- 四态机 ----
    private val _pageState = MutableStateFlow<ScrapePageState>(ScrapePageState.Queue)
    val pageState: StateFlow<ScrapePageState> = _pageState.asStateFlow()

    /** 撤销入口可用性（最近一次写回的 journalId） */
    var lastJournalId: String? = null
        private set

    // ── 限流可观察状态（任务 08-27-scrape-throttle-429） ──────────────
    private val _throttleMessage = MutableStateFlow<String?>(null)
    val throttleMessage: StateFlow<String?> = _throttleMessage.asStateFlow()

    /** 因限流/网络未命中的歌曲 id 集合，供 preview/result 展示“稍后重试”。 */
    private val _throttledIds = MutableStateFlow<List<String>>(emptyList())
    val throttledIds: StateFlow<List<String>> = _throttledIds.asStateFlow()

    init {
        reloadQueue()
        // 队列存储变化（入队/移除）自动刷新列表
        viewModelScope.launch {
            queueStore.updated.collect { reloadQueue() }
        }
    }

    fun reloadQueue() {
        viewModelScope.launch {
            val ids = queueStore.load().map { it.songId }
            _queueSongIds.value = ids
            // 对齐 Web 版队列行显示歌名（ScrapePage.vue 队列项 title）；查不到的由 UI 回退
            _queueTitles.value = ids.mapNotNull { id ->
                songRepository.getSong(id)?.let { id to it.title }
            }.toMap()
        }
    }

    /** 单曲移除（queue 态行内按钮） */
    fun removeFromQueue(songIds: List<String>) {
        viewModelScope.launch { queueStore.remove(songIds) }
    }

    /** 清空队列 */
    fun clearQueue() {
        viewModelScope.launch { queueStore.clear() }
    }

    /**
     * 「全部开始」：逐曲跑文本+封面匹配 → 聚合候选进 preview 态。
     * 匹配失败的歌不进预览（Web 语义：仅命中的进入人工确认）。
     * 若因 NETWORK（含 429）未命中，则记录限流提示与可重试列表，不阻塞后续歌曲。
     */
    fun startMatching() {
        val ids = _queueSongIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            // 重置限流提示
            _throttleMessage.value = null
            _throttledIds.value = emptyList()
            val throttledMutable = mutableListOf<String>()
            val items = mutableListOf<PreviewCandidate>()
            var index = 0
            for (songId in ids) {
                index++
                val song = try {
                    songRepository.getSong(songId)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
                if (song == null) {
                    // 已不在库（懒清理竞态）：直接出队
                    try {
                        queueStore.remove(listOf(songId))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {}
                    continue
                }
                _pageState.value = ScrapePageState.Matching(index, ids.size, song.title)

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
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    com.muses.player.core.model.scrape.OnlineTextMatchResult.Fail(OnlineTextMatchFailReason.NETWORK)
                }
                val coverOk = try {
                    coverMatcher.match(
                        OnlineCoverQuery(songId = song.id, title = song.title, artist = song.artist, album = song.album),
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    com.muses.player.core.scrape.cover.OnlineCoverMatchResult.Fail(OnlineCoverMatchFailReason.NETWORK)
                }

                val hit = (textOk as? com.muses.player.core.model.scrape.OnlineTextMatchResult.Ok)?.hit
                val coverUrl = (coverOk as? com.muses.player.core.scrape.cover.OnlineCoverMatchResult.Ok)?.remoteUrl
                if (hit == null && coverUrl == null) {
                    // 双链均未命中：区分 NETWORK 限流与普通无匹配
                    val isNetwork = (textOk is com.muses.player.core.model.scrape.OnlineTextMatchResult.Fail && textOk.reason == OnlineTextMatchFailReason.NETWORK) ||
                        (coverOk is com.muses.player.core.scrape.cover.OnlineCoverMatchResult.Fail && coverOk.reason == OnlineCoverMatchFailReason.NETWORK)
                    if (isNetwork) {
                        throttledMutable.add(songId)
                        _throttledIds.value = throttledMutable.toList()
                        _throttleMessage.value = "等待限流恢复…"
                        // 2s 后自动清除提示（不阻塞主循环）
                        viewModelScope.launch {
                            try {
                                delay(2000)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {}
                            // 若仍为同一提示则清除，避免覆盖后续提示
                            if (_throttleMessage.value == "等待限流恢复…") {
                                _throttleMessage.value = null
                            }
                        }
                    }
                    continue
                }

                val confidence = (textOk as? com.muses.player.core.model.scrape.OnlineTextMatchResult.Ok)?.confidence?.name
                items += PreviewCandidate(
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
                    checked = false,
                )
            }
            // 若有命中进预览；若全限流未命中，给出最终可重试提示
            if (throttledMutable.isNotEmpty() && items.isEmpty()) {
                _throttleMessage.value = "触发限流，稍后重试"
            } else if (throttledMutable.isNotEmpty()) {
                // 部分限流：保留短期提示供 preview 展示
                _throttleMessage.value = "${throttledMutable.size} 首触发限流，可单独重试"
            }
            _throttledIds.value = throttledMutable.toList()
            _pageState.value = ScrapePageState.Preview(items)
        }
    }

    /**
     * 单曲重试（复用 startMatching 的单曲路径）。
     * 清除该首的负缓存后重跑文本+封面匹配，命中则进入预览，其余给出限流提示。
     */
    fun retrySingle(songId: String) {
        viewModelScope.launch {
            try {
                textMetaMatcher.invalidateNegativeCache(songId)
                coverMatcher.invalidateNegativeCache(songId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {}
            val song = try {
                songRepository.getSong(songId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            } ?: return@launch
            _pageState.value = ScrapePageState.Matching(1, 1, song.title)
            _throttleMessage.value = null
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
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                com.muses.player.core.model.scrape.OnlineTextMatchResult.Fail(OnlineTextMatchFailReason.NETWORK)
            }
            val coverOk = try {
                coverMatcher.match(
                    OnlineCoverQuery(songId = song.id, title = song.title, artist = song.artist, album = song.album),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                com.muses.player.core.scrape.cover.OnlineCoverMatchResult.Fail(OnlineCoverMatchFailReason.NETWORK)
            }
            val hit = (textOk as? com.muses.player.core.model.scrape.OnlineTextMatchResult.Ok)?.hit
            val coverUrl = (coverOk as? com.muses.player.core.scrape.cover.OnlineCoverMatchResult.Ok)?.remoteUrl
            if (hit == null && coverUrl == null) {
                val isNetwork = (textOk is com.muses.player.core.model.scrape.OnlineTextMatchResult.Fail && textOk.reason == OnlineTextMatchFailReason.NETWORK) ||
                    (coverOk is com.muses.player.core.scrape.cover.OnlineCoverMatchResult.Fail && coverOk.reason == OnlineCoverMatchFailReason.NETWORK)
                _throttleMessage.value = if (isNetwork) "触发限流，稍后重试" else "暂无匹配"
                // 保留在 preview 空态以便继续重试
                val currentPreview = _pageState.value as? ScrapePageState.Preview
                if (currentPreview != null) {
                    // 保持空预览以展示重试入口
                    _pageState.value = ScrapePageState.Preview(currentPreview.items)
                } else {
                    _pageState.value = ScrapePageState.Preview(emptyList())
                }
                // 将该首重新加入可重试集合（若限流）
                if (isNetwork) {
                    val cur = _throttledIds.value.toMutableList()
                    if (!cur.contains(songId)) cur.add(songId)
                    _throttledIds.value = cur
                }
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
                checked = false,
            )
            // 合并到现有预览（若已有则追加去重）
            val existing = (_pageState.value as? ScrapePageState.Preview)?.items ?: emptyList()
            val merged = (existing.filter { it.songId != songId } + candidate)
            // 从限流集合移除已成功者
            _throttledIds.value = _throttledIds.value.filter { it != songId }
            if (_throttledIds.value.isEmpty()) _throttleMessage.value = null
            _pageState.value = ScrapePageState.Preview(merged)
        }
    }

    /** 重试所有限流未命中的歌曲（批量）。 */
    fun retryThrottled() {
        val ids = _throttledIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            // 清理负缓存
            ids.forEach {
                try {
                    textMetaMatcher.invalidateNegativeCache(it)
                    coverMatcher.invalidateNegativeCache(it)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {}
            }
            _throttleMessage.value = null
            _throttledIds.value = emptyList()
            val items = mutableListOf<PreviewCandidate>()
            // 复用当前预览已命中项
            val existing = (_pageState.value as? ScrapePageState.Preview)?.items?.toMutableList() ?: mutableListOf()
            items.addAll(existing)
            var index = 0
            val throttledRemain = mutableListOf<String>()
            for (songId in ids) {
                index++
                val song = try { songRepository.getSong(songId) } catch (e: CancellationException) { throw e } catch (_: Exception) { null } ?: continue
                _pageState.value = ScrapePageState.Matching(index, ids.size, song.title)
                val textOk = try {
                    textMetaMatcher.match(OnlineTextQuery(songId = song.id, title = song.title, path = song.path, artist = song.artist, album = song.album, durationSec = song.durationSec.takeIf { it > 0 }?.toDouble(), metaSources = song.metaSources))
                } catch (e: CancellationException) { throw e } catch (_: Exception) { com.muses.player.core.model.scrape.OnlineTextMatchResult.Fail(OnlineTextMatchFailReason.NETWORK) }
                val coverOk = try { coverMatcher.match(OnlineCoverQuery(songId = song.id, title = song.title, artist = song.artist, album = song.album)) } catch (e: CancellationException) { throw e } catch (_: Exception) { com.muses.player.core.scrape.cover.OnlineCoverMatchResult.Fail(OnlineCoverMatchFailReason.NETWORK) }
                val hit = (textOk as? com.muses.player.core.model.scrape.OnlineTextMatchResult.Ok)?.hit
                val coverUrl = (coverOk as? com.muses.player.core.scrape.cover.OnlineCoverMatchResult.Ok)?.remoteUrl
                if (hit == null && coverUrl == null) {
                    val isNetwork = (textOk is com.muses.player.core.model.scrape.OnlineTextMatchResult.Fail && textOk.reason == OnlineTextMatchFailReason.NETWORK) || (coverOk is com.muses.player.core.scrape.cover.OnlineCoverMatchResult.Fail && coverOk.reason == OnlineCoverMatchFailReason.NETWORK)
                    if (isNetwork) throttledRemain.add(songId)
                    continue
                }
                val confidence = (textOk as? com.muses.player.core.model.scrape.OnlineTextMatchResult.Ok)?.confidence?.name
                // 去重追加
                if (items.none { it.songId == songId }) {
                    items.add(PreviewCandidate(songId = song.id, songTitle = song.title, currentTitle = song.title, currentArtist = song.artist, currentAlbum = song.album, currentLyrics = song.lyrics, matchedTitle = hit?.title, matchedArtist = hit?.artist, matchedAlbum = hit?.album, matchedLyrics = null, confidence = confidence, coverUrl = coverUrl, checked = false))
                }
            }
            _throttledIds.value = throttledRemain
            _throttleMessage.value = if (throttledRemain.isNotEmpty()) "${throttledRemain.size} 首仍触发限流，可稍后重试" else null
            _pageState.value = ScrapePageState.Preview(items)
        }
    }

    /** 预览行勾选切换 */
    fun toggleChecked(songId: String) {
        val state = _pageState.value as? ScrapePageState.Preview ?: return
        _pageState.value = state.copy(
            items = state.items.map { if (it.songId == songId) it.copy(checked = !it.checked) else it },
        )
    }

    /** 全选 / 全不选 */
    fun setAllChecked(checked: Boolean) {
        val state = _pageState.value as? ScrapePageState.Preview ?: return
        _pageState.value = state.copy(items = state.items.map { it.copy(checked = checked) })
    }

    /** 更新预览行编辑值（空串已在调用方转 null 表示回退匹配值） */
    fun updatePreviewItem(songId: String, title: String?, artist: String?, album: String?, lyrics: String? = null) {
        val state = _pageState.value as? ScrapePageState.Preview ?: return
        _pageState.value = state.copy(
            items = state.items.map { if (it.songId == songId) it.copy(editTitle = title, editArtist = artist, editAlbum = album, editLyrics = lyrics) else it },
        )
    }

    /** 确认写回：仅勾选项；逐曲结果进 result 态 */
    fun confirmWriteback() {
        val state = _pageState.value as? ScrapePageState.Preview ?: return
        val checkedItems = state.items.filter { it.checked }
        if (checkedItems.isEmpty()) return
        // 立即切到写回中态，给用户明确反馈（WebDAV 上传/本地落盘需数秒）
        _pageState.value = ScrapePageState.Writing(checkedItems.size)
        viewModelScope.launch {
            try {
                val candidates = mutableListOf<ScrapeCandidate>()
                val changesMap = mutableMapOf<String, ScrapeChanges>()
                for (item in checkedItems) {
                    val song = songRepository.getSong(item.songId) ?: continue
                    candidates += ScrapeCandidate(songId = song.id, song = song)
                    changesMap[song.id] = ScrapeChanges(
                        title = item.resolvedTitle(),
                        artist = item.resolvedArtist(),
                        album = item.resolvedAlbum(),
                        coverRemoteUrl = item.coverUrl,
                        lyrics = item.resolvedLyrics(),
                    )
                }
                val applyResult = writebackOrchestrator.applyScrapeChanges(
                    candidates = candidates,
                    checkedIds = changesMap.keys,
                    changesMap = changesMap,
                )
                lastJournalId = applyResult.journalId
                // 写回完成后出队已处理歌曲并刷新
                queueStore.remove(changesMap.keys.toList())
                reloadQueue()
                _pageState.value = ScrapePageState.Result(applyResult.results, applyResult.journalId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // 异常时回退到预览态，避免卡死在 Writing
                _pageState.value = state
            }
        }
    }

    /** 撤销上次写回：journal 回放恢复库旧值（文件不动，对齐 Web 撤销语义） */
    fun undoLastWriteback() {
        val journalId = lastJournalId ?: return
        viewModelScope.launch {
            writebackOrchestrator.revertScrapeJournal(journalId)
            reloadQueue()
            _pageState.value = ScrapePageState.Queue
        }
    }

    /** 返回队列态 */
    fun backToQueue() {
        _pageState.value = ScrapePageState.Queue
        reloadQueue()
    }
}

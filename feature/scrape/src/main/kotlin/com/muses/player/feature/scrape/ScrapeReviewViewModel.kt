package com.muses.player.feature.scrape

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.model.Song
import com.muses.player.core.model.scrape.LyricsFormat
import com.muses.player.core.model.scrape.ScrapeCandidate
import com.muses.player.core.model.scrape.ScrapeChanges
import com.muses.player.core.model.scrape.TextMetaHit
import com.muses.player.core.scrape.editmeta.AbortSignal
import com.muses.player.core.scrape.editmeta.EditCloudMetaQuery
import com.muses.player.core.scrape.editmeta.EditCloudMetaResult
import com.muses.player.core.scrape.editmeta.EditCloudMetaSearch
import com.muses.player.core.scrape.editmeta.EditCoverCandidate
import com.muses.player.core.scrape.editmeta.EditDimResult
import com.muses.player.core.scrape.editmeta.EditDimStatus
import com.muses.player.core.scrape.editmeta.EditLyricsCandidate
import com.muses.player.core.scrape.writeback.WritebackOrchestrator
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 刮削审核页状态机（design §2.2）：Searching → Review / Empty → Writing → Success。
 * 由 SingleScrapeViewModel 升级改造而来：查询引擎换为 EditCloudMetaSearch（三维多候选），
 * 支持文本候选整体切换、逐字段手改覆写、改关键词重搜（AbortSignal 中止前次）。
 */
sealed interface ScrapeReviewState {
    /** 三维查询中 */
    data object Searching : ScrapeReviewState

    /**
     * 多候选审核态。
     *
     * @param song 本地歌曲快照（「本地值」列的数据来源）
     * @param text 文本候选（defaultIndex 为智能推荐）
     * @param cover 封面候选
     * @param lyrics 歌词候选
     * @param selectedTextIndex 文本候选下标（三个文本字段跟随同一候选）
     * @param selectedCoverIndex 封面候选下标；null = 未选
     * @param selectedLyricsIndex 歌词候选下标；null = 未选
     * @param checkedFields 勾选的写回字段（title/artist/album/cover/lyrics）；
     *   默认 = 推荐候选中有值且有差异的字段
     * @param editTitle/editArtist/editAlbum 逐字段手改覆写（优先于候选值；切换候选时清空）
     * @param keyword 本次搜索使用的搜索词快照（输入框草稿在 VM 级 [ScrapeReviewViewModel.keyword]）
     * @param nextSongId 批量模式：队列中下一首 songId（S3「应用并下一首」接线备用；单曲模式 null）
     */
    data class Review(
        val song: Song,
        val text: EditDimResult<TextMetaHit>,
        val cover: EditDimResult<EditCoverCandidate>,
        val lyrics: EditDimResult<EditLyricsCandidate>,
        val selectedTextIndex: Int = 0,
        val selectedCoverIndex: Int? = null,
        val selectedLyricsIndex: Int? = null,
        val checkedFields: Set<String> = emptySet(),
        val editTitle: String? = null,
        val editArtist: String? = null,
        val editAlbum: String? = null,
        val keyword: ReviewKeyword = ReviewKeyword(),
        val nextSongId: String? = null,
    ) : ScrapeReviewState {
        /** 当前选中文本候选 */
        val selectedHit: TextMetaHit? get() = text.items.getOrNull(selectedTextIndex)

        /** 解析后的标题（覆写优先） */
        fun resolvedTitle(): String? = editTitle ?: selectedHit?.title

        /** 解析后的歌手（覆写优先） */
        fun resolvedArtist(): String? = editArtist ?: selectedHit?.artist

        /** 解析后的专辑（覆写优先） */
        fun resolvedAlbum(): String? = editAlbum ?: selectedHit?.album

        /** 当前选中封面候选 */
        val selectedCover: EditCoverCandidate?
            get() = selectedCoverIndex?.let { cover.items.getOrNull(it) }

        /** 当前选中歌词候选 */
        val selectedLyrics: EditLyricsCandidate?
            get() = selectedLyricsIndex?.let { lyrics.items.getOrNull(it) }
    }

    /**
     * 空态。
     * @param reason 展示文案：「暂无匹配」/「触发限流，稍后重试」/「搜索失败，请重试」等
     */
    data class Empty(val reason: String) : ScrapeReviewState

    /** 写回中（WebDAV 需数秒） */
    data object Writing : ScrapeReviewState

    /**
     * 写回成功。
     * @param nextSongId 批量模式下一首（S3 接线「应用并下一首」；单曲模式 null）
     */
    data class Success(val nextSongId: String? = null) : ScrapeReviewState
}

/** 搜索词（title/artist/album 三输入，默认预填本地值） */
data class ReviewKeyword(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
) {
    val titleBlank: Boolean get() = title.isBlank()
}

/**
 * 单曲刮削审核编排：EditCloudMetaSearch 三维多候选查询 → 审核切换/勾选 → WritebackOrchestrator 写回。
 *
 * 协程红线：所有 catch 前置 rethrow CancellationException。引擎内部已把 AbortSignal 折算为
 * ABORTED 状态正常返回（EditSearchAbortedException 被引擎消化），VM 层只需保证
 * 重搜取消前次 job 时不吞其它 CancellationException。
 */
class ScrapeReviewViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val editCloudMetaSearch: EditCloudMetaSearch,
    private val songRepository: SongRepository,
    private val writebackOrchestrator: WritebackOrchestrator,
    private val queueStore: com.muses.player.core.scrape.queue.ScrapeQueueStore,
) : ViewModel() {

    companion object {
        const val KEY_SONG_ID = "songId"
        const val KEY_QUEUE = "queue"
    }

    private val _state = MutableStateFlow<ScrapeReviewState>(ScrapeReviewState.Searching)
    val state: StateFlow<ScrapeReviewState> = _state.asStateFlow()

    /** 搜索词输入（三输入框绑定源；横跨 Searching/Review/Empty 各态可编辑） */
    private val _keyword = MutableStateFlow(ReviewKeyword())
    val keyword: StateFlow<ReviewKeyword> = _keyword.asStateFlow()

    /** 审核目标歌曲（写回时组装 ScrapeCandidate 快照） */
    var currentSong: Song? = null
        private set

    /** 最近一次写回的 songId（S3「应用并下一首」宿主推进待审队列用） */
    var lastWrittenSongId: String? = null
        private set

    /** 批量模式队列上下文（design §2.1：逗号分隔 songId；S3「应用并下一首」接线备用） */
    val reviewQueue: List<String>

    /** 队列中当前 songId 的下一首（无则 null） */
    private val nextSongId: String?

    /** 搜索代数：旧结果不得覆盖新结果 */
    private var searchSeq: Int = 0

    /** 当前搜索 job（重搜时取消前次） */
    private var searchJob: Job? = null

    /** 当前搜索的 abort 标志（重搜时置 true 中止前次查询的 provider 检查点） */
    private var searchAbortFlag: AtomicBoolean = AtomicBoolean(false)

    init {
        val songId = savedStateHandle.get<String>(KEY_SONG_ID)
        val queue = savedStateHandle.get<String>(KEY_QUEUE)
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        reviewQueue = queue
        nextSongId = songId?.let { id -> queue.dropWhile { it != id }.drop(1).firstOrNull() }
        viewModelScope.launch {
            if (songId.isNullOrBlank()) {
                _state.value = ScrapeReviewState.Empty("缺少歌曲参数")
                return@launch
            }
            val song = songRepository.getSong(songId)
            if (song == null) {
                _state.value = ScrapeReviewState.Empty("歌曲不存在或已删除")
                return@launch
            }
            currentSong = song
            // 搜索词预填本地值
            _keyword.value = ReviewKeyword(
                title = song.title,
                artist = song.artist.orEmpty(),
                album = song.album.orEmpty(),
            )
            search()
        }
    }

    // ── 搜索词编辑 ─────────────────────────────────────────

    fun updateKeywordTitle(v: String) { _keyword.value = _keyword.value.copy(title = v) }
    fun updateKeywordArtist(v: String) { _keyword.value = _keyword.value.copy(artist = v) }
    fun updateKeywordAlbum(v: String) { _keyword.value = _keyword.value.copy(album = v) }

    // ── 查询（进入自动 + 改词重搜共用）──────────────────────

    /**
     * 三维云搜。重搜前：取消前次 job（挂起点立即取消）+ 置位 abort 标志
     * （引擎 provider 间检查点立即中止，折叠为 ABORTED 状态返回；旧结果按代数丢弃）。
     */
    fun search() {
        val song = currentSong ?: return
        val kw = _keyword.value
        if (kw.titleBlank) return

        // 中止前次：abort 标志置位 + job 取消
        searchAbortFlag.set(true)
        searchJob?.cancel()
        val seq = ++searchSeq
        val abortFlag = AtomicBoolean(false)
        searchAbortFlag = abortFlag
        val signal = AbortSignal { abortFlag.get() }

        _state.value = ScrapeReviewState.Searching
        searchJob = viewModelScope.launch {
            try {
                val result = editCloudMetaSearch.search(
                    EditCloudMetaQuery(
                        songId = song.id,
                        title = kw.title.trim(),
                        artist = kw.artist.trim().ifEmpty { null },
                        album = kw.album.trim().ifEmpty { null },
                        durationSec = song.durationSec.takeIf { it > 0 }?.toDouble(),
                    ),
                    signal = signal,
                )
                if (seq != searchSeq) return@launch // 旧结果丢弃，不覆盖新结果
                _state.value = buildReviewState(song, result, kw)
            } catch (e: CancellationException) {
                // 前置 rethrow：job 取消 / VM 销毁交回结构化并发（spec 协程红线，禁止吞取消）
                throw e
            } catch (e: Exception) {
                if (seq != searchSeq) return@launch
                _state.value = ScrapeReviewState.Empty("搜索失败，请重试")
            }
        }
    }

    /** 三维结果 → Review / Empty（reason 区分暂无匹配与限流，FailureCopy 语义沿用现有文案） */
    private fun buildReviewState(
        song: Song,
        result: EditCloudMetaResult,
        kw: ReviewKeyword,
    ): ScrapeReviewState {
        val text = result.text
        val cover = result.cover
        val lyrics = result.lyrics
        val hasAny = text.items.isNotEmpty() || cover.items.isNotEmpty() || lyrics.items.isNotEmpty()
        if (!hasAny) {
            val throttled = text.status == EditDimStatus.NETWORK ||
                cover.status == EditDimStatus.NETWORK ||
                lyrics.status == EditDimStatus.NETWORK
            return ScrapeReviewState.Empty(if (throttled) "触发限流，稍后重试" else "暂无匹配")
        }
        val review = ScrapeReviewState.Review(
            song = song,
            text = text,
            cover = cover,
            lyrics = lyrics,
            selectedTextIndex = if (text.items.isEmpty()) 0 else text.defaultIndex.coerceIn(0, text.items.size - 1),
            selectedCoverIndex = cover.items.indices.firstOrNull(),
            selectedLyricsIndex = lyrics.items.indices.firstOrNull(),
            checkedFields = defaultCheckedFields(song, text, cover, lyrics),
            keyword = kw.copy(title = kw.title.trim(), artist = kw.artist.trim(), album = kw.album.trim()),
            nextSongId = nextSongId,
        )
        return review
    }

    /** 默认勾选：推荐候选（defaultIndex）中有值且有差异的字段 */
    private fun defaultCheckedFields(
        song: Song,
        text: EditDimResult<TextMetaHit>,
        cover: EditDimResult<EditCoverCandidate>,
        lyrics: EditDimResult<EditLyricsCandidate>,
    ): Set<String> {
        val hit = text.items.getOrNull(text.defaultIndex)
        val lyric = lyrics.items.getOrNull(lyrics.defaultIndex)
        return buildSet {
            hit?.title?.takeIf { it.isNotBlank() && it != song.title }?.let { add("title") }
            hit?.artist?.takeIf { it.isNotBlank() && it != song.artist }?.let { add("artist") }
            hit?.album?.takeIf { it.isNotBlank() && it != song.album }?.let { add("album") }
            if (cover.items.isNotEmpty()) add("cover")
            lyric?.text?.takeIf { it.isNotBlank() && it != song.lyrics }?.let { add("lyrics") }
        }
    }

    // ── 候选切换 / 勾选 / 覆写 ──────────────────────────────

    /**
     * 文本候选整体切换（Tagger 语义：候选即一条匹配记录）：三个文本字段跟随该候选，
     * edit* 覆写清空，文本字段勾选按新候选「有值且有差异」重算；封面/歌词勾选保持。
     */
    fun selectTextCandidate(index: Int) {
        val s = _state.value as? ScrapeReviewState.Review ?: return
        if (index !in s.text.items.indices || index == s.selectedTextIndex) return
        val hit = s.text.items[index]
        val song = s.song
        val textChecked = buildSet {
            hit.title?.takeIf { it.isNotBlank() && it != song.title }?.let { add("title") }
            hit.artist?.takeIf { it.isNotBlank() && it != song.artist }?.let { add("artist") }
            hit.album?.takeIf { it.isNotBlank() && it != song.album }?.let { add("album") }
        }
        _state.value = s.copy(
            selectedTextIndex = index,
            editTitle = null,
            editArtist = null,
            editAlbum = null,
            checkedFields = (s.checkedFields - "title" - "artist" - "album") + textChecked,
        )
    }

    /** 封面候选切换（独立于文本候选） */
    fun selectCover(index: Int) {
        val s = _state.value as? ScrapeReviewState.Review ?: return
        if (index !in s.cover.items.indices) return
        _state.value = s.copy(selectedCoverIndex = index)
    }

    /** 歌词候选切换（独立于文本候选） */
    fun selectLyrics(index: Int) {
        val s = _state.value as? ScrapeReviewState.Review ?: return
        if (index !in s.lyrics.items.indices) return
        _state.value = s.copy(selectedLyricsIndex = index)
    }

    /** 字段勾选切换（仅候选/覆写有值的字段可勾，由 UI enabled 保证） */
    fun toggleField(field: String) {
        val s = _state.value as? ScrapeReviewState.Review ?: return
        val newChecked = s.checkedFields.toMutableSet()
        if (field in newChecked) newChecked.remove(field) else newChecked.add(field)
        _state.value = s.copy(checkedFields = newChecked)
    }

    /** 逐字段手改覆写：标题（编辑即勾选该字段） */
    fun updateEditTitle(v: String) {
        val s = _state.value as? ScrapeReviewState.Review ?: return
        _state.value = s.copy(
            editTitle = v.trim().takeIf { it.isNotEmpty() },
            checkedFields = s.checkedFields + "title",
        )
    }

    /** 逐字段手改覆写：歌手（编辑即勾选该字段） */
    fun updateEditArtist(v: String) {
        val s = _state.value as? ScrapeReviewState.Review ?: return
        _state.value = s.copy(
            editArtist = v.trim().takeIf { it.isNotEmpty() },
            checkedFields = s.checkedFields + "artist",
        )
    }

    /** 逐字段手改覆写：专辑（编辑即勾选该字段） */
    fun updateEditAlbum(v: String) {
        val s = _state.value as? ScrapeReviewState.Review ?: return
        _state.value = s.copy(
            editAlbum = v.trim().takeIf { it.isNotEmpty() },
            checkedFields = s.checkedFields + "album",
        )
    }

    // ── 写回 ───────────────────────────────────────────────

    /**
     * 写回勾选字段：Empty/无勾选时不写回（安全语义）；
     * 组装 ScrapeChanges 仅含勾选字段 → applyScrapeChanges 单曲批次（journal 可撤销）。
     */
    fun apply() {
        val s = _state.value as? ScrapeReviewState.Review ?: return
        if (s.checkedFields.isEmpty()) return
        val song = s.song
        val checked = s.checkedFields
        val lyricCandidate = s.selectedLyrics
        val changes = ScrapeChanges(
            title = s.resolvedTitle().takeIf { "title" in checked && it != song.title },
            artist = s.resolvedArtist().takeIf { "artist" in checked && it != song.artist },
            album = s.resolvedAlbum().takeIf { "album" in checked && it != song.album },
            coverRemoteUrl = s.selectedCover?.remoteUrl.takeIf { "cover" in checked },
            lyrics = lyricCandidate?.text.takeIf { "lyrics" in checked && lyricCandidate?.text != song.lyrics },
            lyricsFormat = lyricCandidate?.format
                ?.let { wire -> LyricsFormat.entries.firstOrNull { it.wire == wire } }
                .takeIf { "lyrics" in checked },
        )
        viewModelScope.launch {
            _state.value = ScrapeReviewState.Writing
            try {
                writebackOrchestrator.applyScrapeChanges(
                    candidates = listOf(ScrapeCandidate(songId = song.id, song = song)),
                    checkedIds = setOf(song.id),
                    changesMap = mapOf(song.id to changes),
                )
                lastWrittenSongId = song.id
                // 写回成功即出队（与批量 confirmWriteback 语义一致，避免队列残留已处理歌曲）
                try {
                    queueStore.remove(listOf(song.id))
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // 出队失败不阻断成功态（队列页 reloadQueue 有懒清理）
                }
                _state.value = ScrapeReviewState.Success(nextSongId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = ScrapeReviewState.Empty("写回失败，请重试")
            }
        }
    }
}

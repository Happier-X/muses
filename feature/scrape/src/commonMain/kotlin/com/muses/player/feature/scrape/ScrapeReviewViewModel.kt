package com.muses.player.feature.scrape

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.ai.AiRecommendConfig
import com.muses.player.core.ai.AiScrapeInput
import com.muses.player.core.ai.AiScrapeMatcher
import com.muses.player.core.ai.AiServiceUrlException
import com.muses.player.core.ui.components.MusesSnackbar
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.model.Song
import com.muses.player.core.model.scrape.LyricsFormat
import com.muses.player.core.model.scrape.ScrapeCandidate
import com.muses.player.core.model.scrape.ScrapeChanges
import com.muses.player.core.model.scrape.TextMetaHit
import com.muses.player.core.scrape.editmeta.AbortSignal
import com.muses.player.core.scrape.editmeta.CloudPlatformId
import com.muses.player.core.scrape.editmeta.CloudLyricsPlatformId
import com.muses.player.core.scrape.editmeta.EditCloudMetaQuery
import com.muses.player.core.scrape.editmeta.EditCloudMetaResult
import com.muses.player.core.scrape.editmeta.EditCloudMetaSearch
import com.muses.player.core.scrape.editmeta.EditCoverCandidate
import com.muses.player.core.scrape.editmeta.EditDimResult
import com.muses.player.core.scrape.editmeta.EditDimStatus
import com.muses.player.core.scrape.editmeta.EditLyricsCandidate
import com.muses.player.core.scrape.writeback.WritebackOrchestrator
import com.muses.player.core.scrape.writeback.readLocalAudioRevision
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
     * @param selectedTextIndex 整组文本候选下标，未单独选择来源的字段跟随此候选
     * @param textFieldIndices 用户逐字段选择的候选下标；AI 匹配保留这些选择
     * @param selectedCoverIndex 封面候选下标；null = 未选
     * @param selectedLyricsIndex 歌词候选下标；null = 未选
     * @param checkedFields 勾选的写回字段（title/artist/album/cover/lyrics）；
     *   文本默认勾选无身份或版本疑点的候选中有值且有差异的字段
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
        val textFieldIndices: Map<ReviewTextField, Int> = emptyMap(),
        val retryingFailures: Boolean = false,
        val sources: ScrapeSearchSources = ScrapeSearchSources(),
        val restoredDraft: Boolean = false,
        val draftBaselineChanged: Boolean = false,
    ) : ScrapeReviewState {
        /** 当前选中文本候选 */
        val selectedHit: TextMetaHit? get() = text.items.getOrNull(selectedTextIndex)

        fun fieldHit(field: ReviewTextField): TextMetaHit? =
            text.items.getOrNull(textFieldIndices[field] ?: selectedTextIndex)

        fun editedValue(field: ReviewTextField): String? = when (field) {
            ReviewTextField.TITLE -> editTitle
            ReviewTextField.ARTIST -> editArtist
            ReviewTextField.ALBUM -> editAlbum
        }

        /** 解析后的标题（覆写优先） */
        fun resolvedTitle(): String? = editTitle ?: fieldHit(ReviewTextField.TITLE)?.title

        /** 解析后的歌手（覆写优先） */
        fun resolvedArtist(): String? = editArtist ?: fieldHit(ReviewTextField.ARTIST)?.artist

        /** 解析后的专辑（覆写优先） */
        fun resolvedAlbum(): String? = editAlbum ?: fieldHit(ReviewTextField.ALBUM)?.album

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
    data class Success(val nextSongId: String? = null, val message: String = "已更新") : ScrapeReviewState
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
    songId: String?,
    queueCsv: String?,
    private val editCloudMetaSearch: EditCloudMetaSearch,
    private val songRepository: SongRepository,
    private val writebackOrchestrator: WritebackOrchestrator,
    private val queueStore: com.muses.player.core.scrape.queue.ScrapeQueueStore,
    private val aiMatcher: AiScrapeMatcher,
    private val readAiConfig: suspend () -> AiRecommendConfig,
    private val draftStore: ScrapeReviewDraftStore? = null,
    private val readDraftFileRevision: suspend (Song) -> String? = ::readLocalAudioRevision,
    private val lrcApiSettings: com.muses.player.core.scrape.international.LrcApiSettingsStore? = null,
) : ViewModel() {
    private val _state = MutableStateFlow<ScrapeReviewState>(ScrapeReviewState.Searching)
    val state: StateFlow<ScrapeReviewState> = _state.asStateFlow()
    private var draftFileRevision: String? = null
    private var draftSongBaseline: String? = null
    private var lastReview: ScrapeReviewState.Review? = null
    private var reviewState: ScrapeReviewState
        get() = _state.value
        set(value) {
            _state.value = value
            if (value is ScrapeReviewState.Review) {
                lastReview = value
                saveDraft()
            } else if (value is ScrapeReviewState.Success) {
                lastReview = null
                currentSong?.id?.let { draftStore?.clear(it) }
            }
        }

    private fun saveDraft() {
        lastReview?.let { draftStore?.save(reviewDraft(it, _keyword.value, _searchSources.value, draftFileRevision).copy(baseline = draftSongBaseline ?: draftBaseline(it.song))) }
    }

    private val _aiState = MutableStateFlow<ScrapeAiState>(ScrapeAiState.Idle)
    val aiState: StateFlow<ScrapeAiState> = _aiState.asStateFlow()
    private var aiJob: Job? = null
    private var aiSeq = 0

    /** 搜索词输入（三输入框绑定源；横跨 Searching/Review/Empty 各态可编辑） */
    private val _keyword = MutableStateFlow(ReviewKeyword())
    val keyword: StateFlow<ReviewKeyword> = _keyword.asStateFlow()
    private val _searchSources = MutableStateFlow(ScrapeSearchSources())
    val searchSources: StateFlow<ScrapeSearchSources> = _searchSources.asStateFlow()

    private val _diagnostics = MutableStateFlow(SourceDiagnosticsState())
    val diagnostics: StateFlow<SourceDiagnosticsState> = _diagnostics.asStateFlow()
    private var diagnosticJob: Job? = null
    private var diagnosticSeq = 0

    fun startSourceDiagnostics() {
        if (_state.value == ScrapeReviewState.Writing || _state.value == ScrapeReviewState.Searching ||
            (_state.value as? ScrapeReviewState.Review)?.retryingFailures == true) return
        val song = currentSong ?: return
        val kw = _keyword.value
        if (kw.titleBlank) return
        stopSourceDiagnostics()
        val seq = ++diagnosticSeq
        val sources = _searchSources.value
        val query = EditCloudMetaQuery(song.id, kw.title.trim(), kw.artist.trim().ifEmpty { null }, kw.album.trim().ifEmpty { null }, durationSec = song.reviewDurationSec())
        _diagnostics.value = SourceDiagnosticsState(query = query, sources = sources, running = true,
            targets = editCloudMetaSearch.diagnosticTargets(sources.searchOptions()))
        diagnosticJob = viewModelScope.launch {
            try {
                editCloudMetaSearch.diagnose(query, sources.searchOptions()) { row ->
                    if (seq == diagnosticSeq) _diagnostics.value = _diagnostics.value.copy(rows = _diagnostics.value.rows + row)
                }
            } finally {
                if (seq == diagnosticSeq) _diagnostics.value = _diagnostics.value.copy(running = false)
            }
        }
    }

    fun stopSourceDiagnostics() {
        ++diagnosticSeq
        diagnosticJob?.cancel()
        diagnosticJob = null
        _diagnostics.value = _diagnostics.value.copy(running = false)
    }

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
        val queue = queueCsv
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        reviewQueue = queue
        nextSongId = songId?.let { id -> queue.dropWhile { it != id }.drop(1).firstOrNull() }
        viewModelScope.launch {
            if (songId.isNullOrBlank()) {
                reviewState = ScrapeReviewState.Empty("缺少歌曲参数")
                return@launch
            }
            val song = songRepository.getSong(songId)
            if (song == null) {
                reviewState = ScrapeReviewState.Empty("歌曲不存在或已删除")
                return@launch
            }
            currentSong = song
            if (draftStore != null) draftFileRevision = readDraftFileRevision(song)
            val draft = draftStore?.load(songId)
            if (draft != null) {
                try {
                    val restored = draft.restore(song, nextSongId, draftFileRevision)
                    draftFileRevision = draft.fileRevision
                    draftSongBaseline = draft.baseline
                    _keyword.value = draft.input.keyword()
                    _searchSources.value = draft.inputSources.sources()
                    reviewState = restored
                    return@launch
                } catch (_: Exception) {
                    draftStore?.clear(songId)
                    MusesSnackbar.show("审核草稿已不兼容，将重新搜索")
                }
            }
            // 搜索词预填本地值
            _keyword.value = ReviewKeyword(
                title = song.title,
                artist = song.artist.orEmpty(),
                album = song.album.orEmpty(),
            )
            search()
        }
    }

    val lrcApiBaseUrl = lrcApiSettings?.baseUrl ?: kotlinx.coroutines.flow.flowOf(com.muses.player.core.scrape.international.DEFAULT_LRC_API_URL)
    fun saveLrcApiBaseUrl(value: String) {
        viewModelScope.launch {
            try {
                lrcApiSettings?.setBaseUrl(value)
                MusesSnackbar.show("LrcApi 地址已保存，重新搜索后生效")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { MusesSnackbar.show(e.message ?: "LrcApi 地址保存失败，请重试") }
        }
    }

    // ── 搜索词编辑 ─────────────────────────────────────────

    fun updateKeywordTitle(v: String) { cancelAiMatching(); _keyword.value = _keyword.value.copy(title = v); saveDraft() }
    fun updateKeywordArtist(v: String) { cancelAiMatching(); _keyword.value = _keyword.value.copy(artist = v); saveDraft() }
    fun updateKeywordAlbum(v: String) { cancelAiMatching(); _keyword.value = _keyword.value.copy(album = v); saveDraft() }

    fun updateMetadataSource(source: CloudPlatformId) {
        cancelAiMatching()
        _searchSources.value = _searchSources.value.copy(metadata = source)
        saveDraft()
    }

    fun updateLyricsSource(source: CloudLyricsPlatformId) {
        cancelAiMatching()
        _searchSources.value = _searchSources.value.copy(lyrics = source)
        saveDraft()
    }

    /** 用户主动调用；AI 返回候选判断后只更新本页，写回仍走手动应用。 */
    fun matchWithAi() {
        val review = _state.value as? ScrapeReviewState.Review ?: return
        if (review.retryingFailures) return
        if (_aiState.value == ScrapeAiState.Matching) return
        if (review.text.items.isEmpty() && review.lyrics.items.isEmpty()) {
            _aiState.value = ScrapeAiState.Failed("没有可供 AI 比较的歌曲信息或歌词候选")
            return
        }
        val seq = ++aiSeq
        _aiState.value = ScrapeAiState.Matching
        aiJob = viewModelScope.launch {
            try {
                val config = readAiConfig()
                if (!config.isUsable) {
                    if (seq == aiSeq) _aiState.value = ScrapeAiState.Failed("请先在设置中填写 AI 服务地址、模型和 API Key")
                    return@launch
                }
                val result = aiMatcher.match(
                    AiScrapeInput(
                        song = review.song,
                        searchTitle = review.keyword.title, searchArtist = review.keyword.artist,
                        searchAlbum = review.keyword.album,
                        textCandidates = review.text.items, lyricsCandidates = review.lyrics.items,
                    ), config,
                )
                // 绑定请求代数和审核快照，人工操作、重搜或离开审核态后，旧结果不得回写。
                if (seq != aiSeq || _state.value !== review) return@launch
                reviewState = review.withAiSelection(result)
                _aiState.value = ScrapeAiState.Ready(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (seq == aiSeq && _state.value === review) {
                    if (e is AiServiceUrlException) {
                        MusesSnackbar.show(e.message.orEmpty())
                        _aiState.value = ScrapeAiState.Idle
                    } else {
                        _aiState.value = ScrapeAiState.Failed("AI 匹配失败，原选择已保留，请检查配置或网络后重试")
                    }
                }
            }
        }
    }

    fun cancelAiMatching() {
        ++aiSeq
        aiJob?.cancel()
        _aiState.value = ScrapeAiState.Idle
    }

    // ── 查询（进入自动 + 改词重搜共用）──────────────────────

    /**
     * 三维云搜。重搜前：取消前次 job（挂起点立即取消）+ 置位 abort 标志
     * （引擎 provider 间检查点立即中止，折叠为 ABORTED 状态返回；旧结果按代数丢弃）。
     */
    fun search() {
        stopSourceDiagnostics()
        if (_state.value == ScrapeReviewState.Writing) return
        val song = currentSong ?: return
        val kw = _keyword.value
        val sources = _searchSources.value
        if (kw.titleBlank) return
        cancelAiMatching()

        // 中止前次：abort 标志置位 + job 取消
        searchAbortFlag.set(true)
        searchJob?.cancel()
        val seq = ++searchSeq
        val abortFlag = AtomicBoolean(false)
        searchAbortFlag = abortFlag
        val signal = AbortSignal { abortFlag.get() }

        reviewState = ScrapeReviewState.Searching
        searchJob = viewModelScope.launch {
            try {
                val freshSong = songRepository.getSong(song.id) ?: error("歌曲已不在曲库中")
                val revision = readDraftFileRevision(freshSong)
                if (seq != searchSeq) return@launch
                val song = freshSong
                val result = editCloudMetaSearch.search(
                    EditCloudMetaQuery(
                        songId = song.id,
                        title = kw.title.trim(),
                        artist = kw.artist.trim().ifEmpty { null },
                        album = kw.album.trim().ifEmpty { null },
                        durationSec = song.reviewDurationSec(),
                    ),
                    options = sources.searchOptions(),
                    signal = signal,
                )
                if (seq != searchSeq) return@launch // 旧结果丢弃，不覆盖新结果
                // 新候选与新基准一起提交；搜索期间保存输入不能把旧候选配上新文件版本。
                currentSong = freshSong
                draftFileRevision = revision
                draftSongBaseline = draftBaseline(freshSong)
                reviewState = buildReviewState(song, result, kw, sources)
            } catch (e: CancellationException) {
                // 前置 rethrow：job 取消 / VM 销毁交回结构化并发（spec 协程红线，禁止吞取消）
                throw e
            } catch (e: Exception) {
                if (seq != searchSeq) return@launch
                reviewState = ScrapeReviewState.Empty("搜索失败，请重试")
            }
        }
    }

    /** 使用审核结果的搜索词重试，输入框里尚未提交的草稿不参与此次查询。 */
    fun retryFailedSources() {
        stopSourceDiagnostics()
        val review = _state.value as? ScrapeReviewState.Review ?: return
        if (review.retryingFailures) return
        if ((review.text.failedSources + review.cover.failedSources + review.lyrics.failedSources).isEmpty()) return
        cancelAiMatching()
        searchAbortFlag.set(true)
        searchJob?.cancel()
        val seq = ++searchSeq
        val abortFlag = AtomicBoolean(false)
        searchAbortFlag = abortFlag
        reviewState = review.copy(retryingFailures = true)
        searchJob = viewModelScope.launch {
            try {
                val kw = review.keyword
                val result = editCloudMetaSearch.retryFailed(
                    EditCloudMetaQuery(
                        songId = review.song.id, title = kw.title,
                        artist = kw.artist.ifEmpty { null }, album = kw.album.ifEmpty { null },
                        durationSec = review.song.reviewDurationSec(),
                    ),
                    EditCloudMetaResult(review.text, review.cover, review.lyrics),
                    AbortSignal { abortFlag.get() },
                )
                if (seq != searchSeq) return@launch
                val latest = _state.value as? ScrapeReviewState.Review ?: return@launch
                // 重试期间仍可审核；只补充候选和失败状态，保留最新的人工操作。
                reviewState = latest.copy(text = result.text, cover = result.cover, lyrics = result.lyrics)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (seq == searchSeq) MusesSnackbar.show("重试失败，已保留候选和编辑，请稍后重试")
            } finally {
                if (seq == searchSeq) {
                    val latest = _state.value as? ScrapeReviewState.Review
                    if (latest != null) reviewState = latest.copy(retryingFailures = false)
                }
            }
        }
    }

    /** 三维结果 → Review / Empty（reason 区分暂无匹配与限流，FailureCopy 语义沿用现有文案） */
    private fun buildReviewState(
        song: Song,
        result: EditCloudMetaResult,
        kw: ReviewKeyword,
        sources: ScrapeSearchSources,
    ): ScrapeReviewState {
        val text = result.text
        val cover = result.cover
        val lyrics = result.lyrics
        val hasAny = text.items.isNotEmpty() || cover.items.isNotEmpty() || lyrics.items.isNotEmpty()
        if (!hasAny) {
            val networkFailure = text.status == EditDimStatus.NETWORK ||
                cover.status == EditDimStatus.NETWORK ||
                lyrics.status == EditDimStatus.NETWORK
            val failures = describeScrapeFailures(result)
            return ScrapeReviewState.Empty(if (networkFailure) failures.takeIf { it.isNotEmpty() }?.joinToString("\n")
                ?: "部分来源请求失败，请检查网络后重试" else "暂无匹配")
        }
        val review = ScrapeReviewState.Review(
            song = song,
            text = text,
            cover = cover,
            lyrics = lyrics,
            selectedTextIndex = if (text.items.isEmpty()) 0 else text.defaultIndex.coerceIn(0, text.items.size - 1),
            selectedCoverIndex = cover.items.indices.firstOrNull(),
            selectedLyricsIndex = lyrics.items.indices.firstOrNull(),
            checkedFields = defaultCheckedFields(song, text, cover, lyrics, kw),
            keyword = kw.copy(title = kw.title.trim(), artist = kw.artist.trim(), album = kw.album.trim()),
            nextSongId = nextSongId,
            sources = sources,
        )
        return review
    }

    /** 默认勾选：文本先检查匹配疑点，封面和歌词沿用候选差异规则。 */
    private fun defaultCheckedFields(
        song: Song,
        text: EditDimResult<TextMetaHit>,
        cover: EditDimResult<EditCoverCandidate>,
        lyrics: EditDimResult<EditLyricsCandidate>,
        keyword: ReviewKeyword,
    ): Set<String> {
        val hit = text.items.getOrNull(text.defaultIndex)
        val lyric = lyrics.items.getOrNull(lyrics.defaultIndex)
        return buildSet {
            addAll(defaultTextCheckedFields(song, hit, keyword))
            if (cover.items.isNotEmpty()) add("cover")
            lyric?.text?.takeIf { it.isNotBlank() && it != song.lyrics }?.let { add("lyrics") }
        }
    }

    // ── 候选切换 / 勾选 / 覆写 ──────────────────────────────

    /**
     * 文本候选整体切换（Tagger 语义：候选即一条匹配记录）：三个文本字段跟随该候选，
     * edit* 覆写清空，文本字段按匹配疑点及差异重算勾选；封面/歌词勾选保持。
     */
    fun selectTextCandidate(index: Int) {
        val s = _state.value as? ScrapeReviewState.Review ?: return
        if (index !in s.text.items.indices) return
        cancelAiMatching()
        val hit = s.text.items[index]
        val song = s.song
        val textChecked = defaultTextCheckedFields(song, hit, s.keyword)
        reviewState = s.copy(
            selectedTextIndex = index,
            editTitle = null,
            editArtist = null,
            editAlbum = null,
            textFieldIndices = emptyMap(),
            checkedFields = (s.checkedFields - "title" - "artist" - "album") + textChecked,
        )
    }

    /** 用户明确选择单个字段来源时，只更新该字段。 */
    fun selectTextFieldCandidate(field: ReviewTextField, index: Int) {
        val s = _state.value as? ScrapeReviewState.Review ?: return
        val updated = s.withTextFieldCandidate(field, index)
        if (updated == s) return
        cancelAiMatching()
        reviewState = updated
    }

    /** 封面候选切换（独立于文本候选） */
    fun selectCover(index: Int) {
        val s = _state.value as? ScrapeReviewState.Review ?: return
        if (index !in s.cover.items.indices) return
        cancelAiMatching()
        reviewState = s.copy(selectedCoverIndex = index)
    }

    /** 歌词候选切换（独立于文本候选） */
    fun selectLyrics(index: Int) {
        val s = _state.value as? ScrapeReviewState.Review ?: return
        if (index !in s.lyrics.items.indices) return
        cancelAiMatching()
        reviewState = s.copy(selectedLyricsIndex = index)
    }

    /** 字段勾选切换（仅候选/覆写有值的字段可勾，由 UI enabled 保证） */
    fun toggleField(field: String) {
        val s = _state.value as? ScrapeReviewState.Review ?: return
        if (field !in s.checkedFields && field !in selectableFields(s)) return
        cancelAiMatching()
        val newChecked = s.checkedFields.toMutableSet()
        if (field in newChecked) newChecked.remove(field) else newChecked.add(field)
        reviewState = s.copy(checkedFields = newChecked)
    }

    /** 逐字段手改覆写：标题（编辑即勾选该字段） */
    fun updateEditTitle(v: String) {
        val s = _state.value as? ScrapeReviewState.Review ?: return
        cancelAiMatching()
        reviewState = s.copy(
            editTitle = v.trim().takeIf { it.isNotEmpty() },
            checkedFields = s.checkedFields + "title",
        )
    }

    /** 逐字段手改覆写：歌手（编辑即勾选该字段） */
    fun updateEditArtist(v: String) {
        val s = _state.value as? ScrapeReviewState.Review ?: return
        cancelAiMatching()
        reviewState = s.copy(
            editArtist = v.trim().takeIf { it.isNotEmpty() },
            checkedFields = s.checkedFields + "artist",
        )
    }

    /** 逐字段手改覆写：专辑（编辑即勾选该字段） */
    fun updateEditAlbum(v: String) {
        val s = _state.value as? ScrapeReviewState.Review ?: return
        cancelAiMatching()
        reviewState = s.copy(
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
        stopSourceDiagnostics()
        val s = _state.value as? ScrapeReviewState.Review ?: return
        if (s.retryingFailures) return
        if (_aiState.value == ScrapeAiState.Matching) return
        if (s.checkedFields.isEmpty()) return
        val requiresRevision = s.song.sourceType == com.muses.player.core.model.SourceType.LOCAL && !s.song.path.startsWith("content:", true)
        if (requiresRevision && (draftFileRevision == null || s.draftBaselineChanged)) {
            reviewState = s.copy(checkedFields = emptySet(), draftBaselineChanged = true)
            MusesSnackbar.show("无法沿用审核时的文件版本，请重新搜索并审核后保存")
            return
        }
        cancelAiMatching()
        val song = s.song
        val checked = s.checkedFields
        val lyricCandidate = s.selectedLyrics
        val lyrics = lyricCandidate?.text.takeIf { "lyrics" in checked && !it.isNullOrBlank() && it != song.lyrics }
        val changes = ScrapeChanges(
            title = s.resolvedTitle().takeIf { "title" in checked && !it.isNullOrBlank() && it != song.title },
            artist = s.resolvedArtist().takeIf { "artist" in checked && !it.isNullOrBlank() && it != song.artist },
            album = s.resolvedAlbum().takeIf { "album" in checked && !it.isNullOrBlank() && it != song.album },
            coverRemoteUrl = s.selectedCover?.remoteUrl.takeIf { "cover" in checked },
            lyrics = lyrics,
            lyricsFormat = lyricCandidate?.format
                ?.let { wire -> LyricsFormat.entries.firstOrNull { it.wire == wire } }
                .takeIf { lyrics != null },
        )
        if (changes == ScrapeChanges()) {
            MusesSnackbar.show("所选内容与现有信息一致，无需保存")
            return
        }
        // 立即离开审核态，阻止协程调度前的连点重复启动文件写入。
        reviewState = ScrapeReviewState.Writing
        viewModelScope.launch {
            try {
                val result = writebackOrchestrator.applyScrapeChanges(
                    candidates = listOf(ScrapeCandidate(songId = song.id, song = song, expectedFileRevision = draftFileRevision)),
                    checkedIds = setOf(song.id),
                    changesMap = mapOf(song.id to changes),
                )
                val row = result.results.singleOrNull()
                if (row?.status != com.muses.player.core.model.scrape.WritebackStatus.SUCCESS) {
                    reviewState = if (row?.fileResult?.code in setOf("review_changed", "file_changed", "review_check_unsupported"))
                        s.copy(checkedFields = emptySet(), draftBaselineChanged = true) else s
                    MusesSnackbar.show(row?.error ?: row?.fileResult?.message ?: "文件未保存，请重试")
                    return@launch
                }
                lastWrittenSongId = song.id
                // 写回成功即出队（与批量 confirmWriteback 语义一致，避免队列残留已处理歌曲）
                try {
                    queueStore.remove(listOf(song.id))
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // 出队失败不阻断成功态（队列页 reloadQueue 有懒清理）
                }
                reviewState = ScrapeReviewState.Success(nextSongId, singleWritebackSuccessMessage(row?.coverSkipped == true))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                reviewState = s
                MusesSnackbar.show(e.message ?: "写回失败，请重试")
            }
        }
    }
}

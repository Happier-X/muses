package com.muses.player.core.model.scrape

/**
 * 刮削引擎领域模型（任务 08-25-native-m3-scrape-engine / S0）。
 *
 * 规格书 = Web 层源码，逐字段翻译：
 * - src/features/metadata/types.ts（OnlineTextQuery / TextMetaHit / OnlineTextMatchResult）
 * - src/features/scrape/writeback.ts（RollbackEntry / RollbackJournal / WritebackStatus / WritebackResult / ScrapeChanges）
 * - src/features/scrape/queue.ts（ScrapeQueueItem / ScrapeQueueSnapshot）
 * - src/features/scrape/history.ts（ScrapeHistoryEntry / ScrapeHistorySnapshot）
 * - src/features/lyrics/score.ts（MatchConfidence）
 */

// ── 置信度（score.ts）──────────────────────────────────────

/** 匹配置信度：high=自动写库采纳门槛；low=仅进候选供刮削页人工选择 */
enum class MatchConfidence {
    HIGH,
    LOW,
}

// ── 文本元数据在线补缺（metadata/types.ts）─────────────────

/** 在线文本元信息来源；wire 值与 Web 保持一致 */
enum class OnlineTextSource(val wire: String) {
    KW("kw"),
    TX("tx"),
    WY("wy"),
    KG("kg"),
    MG("mg"),
}

/** 元数据字段来源标记（writeback.ts metaSources 语义） */
enum class MetaFieldSource(val wire: String) {
    EMBEDDED("embedded"),
    CLOUD("cloud"),
    MANUAL("manual"),
}

/** 各字段的来源标记（Web 为 Partial<Record>，此处逐字段可空对齐） */
data class MetaSources(
    val title: MetaFieldSource? = null,
    val artist: MetaFieldSource? = null,
    val album: MetaFieldSource? = null,
    val cover: MetaFieldSource? = null,
)

/** 歌词格式（SongItem.lyricsFormat = SongLyricsFormat 对齐） */
enum class LyricsFormat(val wire: String) {
    LRC("lrc"),
    TTML("ttml"),
    YRC("yrc"),
    QRC("qrc"),
}

/** 歌词来源（library/types.ts LyricsSource 对齐；online 为历史遗留值，仅兼容存量） */
enum class LyricsSource(val wire: String) {
    EMBEDDED("embedded"),
    SIDECAR("sidecar"),
    SCRAPE("scrape"),
    ONLINE("online"),
}

/** 在线文本元信息查询 */
data class OnlineTextQuery(
    val songId: String,
    val title: String,
    /** 用于弱 title 判定（与去扩展名文件名比较） */
    val path: String? = null,
    val artist: String? = null,
    val album: String? = null,
    /** 查询曲目时长（秒）；可选，参与匹配质量时长约束（child4） */
    val durationSec: Double? = null,
    /**
     * 可选：现字段来源标记（child4 R4-2）。
     * 用于 needsOnlineTextMeta 的 cloud 来源再补约束；服务层 search 不读此字段。
     */
    val metaSources: MetaSources? = null,
)

/** 单源命中的文本元信息 */
data class TextMetaHit(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val source: OnlineTextSource,
)

/** 匹配失败原因 */
enum class OnlineTextMatchFailReason {
    NO_MATCH,
    NETWORK,
    NOT_NEEDED,
}

/** 五源链匹配结果（OnlineTextMatchOk | OnlineTextMatchFail 的密封建模） */
sealed interface OnlineTextMatchResult {
    /**
     * @param confidence 命中置信度（child4 R4-2）：自动写库路径应校验为 HIGH，
     *   低置信进候选供刮削页人工选择。可空以向后兼容旧调用方（缺省视为 HIGH）。
     */
    data class Ok(
        val hit: TextMetaHit,
        val confidence: MatchConfidence? = null,
    ) : OnlineTextMatchResult

    data class Fail(val reason: OnlineTextMatchFailReason) : OnlineTextMatchResult
}

// ── 写回编排（scrape/writeback.ts）─────────────────────────

/** 回滚快照中的歌曲旧值（RollbackEntry.songBefore，Pick<SongItem,...> 对齐） */
data class RollbackSongSnapshot(
    val title: String,
    val artist: String?,
    val album: String?,
    val coverUri: String?,
    val lyrics: String?,
    val lyricsFormat: LyricsFormat?,
    val lyricsSource: LyricsSource?,
    val metaSources: MetaSources?,
)

/** 单曲回滚条目 */
data class RollbackEntry(
    val songId: String,
    val songBefore: RollbackSongSnapshot,
    /** ISO 时间 */
    val createdAt: String,
)

/** 回滚 journal 持久化 snapshot（version=1，上限 200 条由存储层保证） */
data class RollbackJournal(
    val version: Int,
    val journalId: String,
    val entries: List<RollbackEntry>,
)

/** 写回状态：success=文件+库均成功；file-failed=库已更新但文件写入失败；failed=整体异常 */
enum class WritebackStatus(val wire: String) {
    SUCCESS("success"),
    FILE_FAILED("file-failed"),
    FAILED("failed"),
}

/** 音频标签文件写入结果（Web WriteMetadataResult 对齐：ok=true 成功；失败带 code 便于文案，不抛异常） */
data class FileWriteResult(
    val ok: Boolean,
    val code: String? = null,
    val message: String? = null,
)

/** 批量写回的单曲结果（逐行返回，互不影响） */
data class WritebackResult(
    val songId: String,
    val status: WritebackStatus,
    /** 文件写入结果（成功/失败） */
    val fileResult: FileWriteResult,
    /** 库是否更新 */
    val libraryUpdated: Boolean,
    /** 错误信息 */
    val error: String? = null,
)

/** 写回变更集合（undefined 字段表示不修改该字段；空串语义按 Web：清空） */
data class ScrapeChanges(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val coverUri: String? = null,
    val coverRemoteUrl: String? = null,
    val lyrics: String? = null,
    val lyricsFormat: LyricsFormat? = null,
)

// ── 待刮削队列（scrape/queue.ts）───────────────────────────

data class ScrapeQueueItem(
    val songId: String,
    /** ISO 时间 */
    val addedAt: String,
)

/** 队列持久化 snapshot（version=1；入队幂等、懒清理由存储层保证） */
data class ScrapeQueueSnapshot(
    val version: Int,
    val items: List<ScrapeQueueItem>,
)

// ── 刮削历史（scrape/history.ts）───────────────────────────

/** 历史条目状态复用写回状态三值（Web 中两个 union 字面量一致） */
typealias ScrapeHistoryStatus = WritebackStatus

/**
 * 刮削历史条目：自带歌名快照，删歌后仍可展示。
 * 滚动上限 200 条由存储层保证。
 */
data class ScrapeHistoryEntry(
    /** 唯一 id */
    val id: String,
    /** 写回批次号（对应回滚 journal 的 journalId，重试会产生新批次） */
    val journalId: String,
    val songId: String,
    /** 歌名快照（防删歌后无法展示） */
    val songTitle: String,
    /** 艺术家快照 */
    val songArtist: String? = null,
    /** ISO 时间 */
    val at: String,
    val status: WritebackStatus,
    /** 失败原因（复用 describeWritebackFailure 文案），成功时缺省 */
    val failureReason: String? = null,
    /** 本次写回字段：title/artist/album/cover/lyrics */
    val changedFields: List<String>,
)

/** 历史持久化 snapshot（version=1） */
data class ScrapeHistorySnapshot(
    val version: Int,
    val entries: List<ScrapeHistoryEntry>,
)

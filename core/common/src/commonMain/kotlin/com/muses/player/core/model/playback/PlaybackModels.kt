package com.muses.player.core.model.playback

/**
 * 播放持久化领域模型（任务 08-25-native-playback-persistence / P0）。
 *
 * 规格书 = Web 层 src/features/player/{queue,session,recent}.ts 的类型段，逐字段翻译。
 */

/** 循环模式（queue.ts RepeatMode）；wire 值与 Web 一致 */
enum class RepeatMode(val wire: String) {
    ONE("one"),
    ALL("all"),
}

/**
 * 播放器配置（queue.ts PlayerConfig）。
 * 默认值对齐 defaultConfig()：all / false。
 */
data class PlayerConfig(
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val shuffleEnabled: Boolean = false,
)

/** 队列条目：仅存 songId，恢复时按曲库解析完整歌曲（queue.ts QueueItem） */
data class QueueItem(val songId: String)

/**
 * 队列快照（queue.ts QueueData）：
 * items=当前生效序列；originalOrder=未洗牌原始序；shuffleOrder=洗牌序（未启用为 null）。
 */
data class QueueSnapshotData(
    val items: List<QueueItem> = emptyList(),
    val originalOrder: List<QueueItem> = emptyList(),
    val shuffleOrder: List<QueueItem>? = null,
)

/**
 * 冷启动播放会话（session.ts PlaybackSession）：当前曲 + 进度。
 */
data class PlaybackSessionInfo(
    val currentSongId: String,
    /** 毫秒，>= 0 */
    val positionMs: Long = 0L,
)

/**
 * 最近播放条目（recent.ts RecentPlayEntry）：同曲去重置顶，上限 50；
 * 仅存展示所需元数据，点击播放时按 songId 从曲库解析完整歌曲。
 */
data class RecentPlayEntry(
    val songId: String,
    val title: String,
    /** artist - album 形式的副标题 */
    val subtitle: String,
    val coverUri: String? = null,
    val playedAt: Long,
)

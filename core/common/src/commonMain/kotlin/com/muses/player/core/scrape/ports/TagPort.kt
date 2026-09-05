package com.muses.player.core.scrape.ports

import com.muses.player.core.model.scrape.FileWriteResult
import com.muses.player.core.model.scrape.ScrapeChanges
import java.io.File

/**
 * 音频标签读写端口（W3 写回链 KMP 化，design.md §3）。
 *
 * 写回链（WritebackOrchestrator/SongFileWriters）对 TagWriter（core:media，jaudiotagger）
 * 与 AudioTagReader（core:data）的依赖收口；commonMain 只定接口，实现双端注入：
 * - jaudiotagger 双端共用实现（:core:common jvmShared [JaudiotaggerTagPort]）
 * - 懒扫描安卓壳留守 core:data（AudioTagReader 不随本端口上收）
 *
 * 语义冻结（写回 spec）：
 * - 读：文件不存在 / 格式不支持 → 返回 null，不抛异常。
 * - 写：不抛异常，失败折叠为 `FileWriteResult(ok=false)`；`ScrapeChanges` 中 null 字段 =
 *   不修改，空串 = 显式清空（lyrics=="" → clearLyrics、coverUri=="" → clearCover），
 *   与 Web 层 writeback.ts 的 WriteMetadataOptions 语义逐字段对齐。
 */
interface TagPort {

    /** 读取音频文件标签（文件 → 标签字段）；解析失败返回 null */
    fun readTags(file: File): TagPortTags?

    /**
     * 写入刮削变更到音频文件（文件 + ScrapeChanges + 远程封面字节 → 写结果）。
     * @param coverBytes 远程封面原始字节（编排层经 CoverBytesFetcher 获取）；null/空 = 不写封面
     */
    fun writeTags(file: File, changes: ScrapeChanges, coverBytes: ByteArray?): FileWriteResult
}

/** 标签字段快照（readTags 返回值；与 AudioTags 字段对齐，commonMain 无安卓依赖） */
data class TagPortTags(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val lyrics: String? = null,
    val cover: ByteArray? = null,
    val durationMs: Long = 0L,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TagPortTags) return false
        return title == other.title &&
            artist == other.artist &&
            album == other.album &&
            lyrics == other.lyrics &&
            cover.contentEquals(other.cover) &&
            durationMs == other.durationMs
    }

    override fun hashCode(): Int {
        var result = title?.hashCode() ?: 0
        result = 31 * result + (artist?.hashCode() ?: 0)
        result = 31 * result + (album?.hashCode() ?: 0)
        result = 31 * result + (lyrics?.hashCode() ?: 0)
        result = 31 * result + (cover?.contentHashCode() ?: 0)
        result = 31 * result + durationMs.hashCode()
        return result
    }
}

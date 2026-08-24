package com.muses.player.core.model

/**
 * 歌曲。
 * id 为稳定 ID（sourceId + 路径哈希），供队列/播放列表引用；
 * sourceType 标记来源；tagsVersion 记录标签解析器版本，扫描器升级后可增量重扫。
 */
data class Song(
    val id: String,
    val sourceId: String,
    val path: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long = 0L,
    /** 秒级时长（durationMs/1000 的冗余便捷字段） */
    val durationSec: Long = 0L,
    /** 封面安全 URI（file:// 缓存路径），可为空 */
    val coverUri: String? = null,
    /** 内嵌歌词原文（M1 仅存储，渲染在 M2 接入） */
    val lyrics: String? = null,
    val sourceType: SourceType = SourceType.LOCAL,
    /** 标签解析器版本号；每次扫描写入当前版本 */
    val tagsVersion: Int = 0,
)

/** 专辑索引 */
data class Album(
    val id: String,
    val title: String,
    val artist: String? = null,
    val year: Int? = null,
    val songCount: Int = 0,
)

/** 艺术家索引 */
data class Artist(
    val id: String,
    val name: String,
    val albumCount: Int = 0,
    val songCount: Int = 0,
)

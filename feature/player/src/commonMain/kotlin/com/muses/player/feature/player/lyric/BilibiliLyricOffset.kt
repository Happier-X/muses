package com.muses.player.feature.player.lyric

/**
 * B 站音源歌词偏移扩展点（当前恒返回 0/false，调用方已接线）。
 *
 * TODO(lyric-offset)：B 站部分稿件音频与歌词存在固定偏移，待补充按稿件维度的偏移表后，
 * 在此实现 [rememberBilibiliLyricOffset] 的查表逻辑（调用方无需改动）。
 */
import androidx.compose.runtime.Composable

@Composable
fun rememberBilibiliLyricOffset(mediaId: String?): Int = 0

fun effectiveBilibiliLyricAdvance(globalAdvanceMs: Int, trackOffsetMs: Int): Long = 0L

fun isBilibiliMediaId(mediaId: String?): Boolean = false

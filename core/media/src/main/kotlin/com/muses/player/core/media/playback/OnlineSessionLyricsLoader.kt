package com.muses.player.core.media.playback

import com.muses.player.core.lyrics.DesktopLyricsState
import com.muses.player.core.lyrics.model.LyricsDocument
import com.muses.player.core.lyrics.parser.LxLyricParser
import com.muses.player.core.model.Song
import com.muses.player.core.model.online.OnlineTrackLyrics
import com.muses.player.core.model.online.OnlineTrackRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** 播放服务独立加载在线歌词；页面已解析的文档优先，脚本缓存晚到时有限重试。 */
internal class OnlineSessionLyricsLoader(
    private val resolveLyrics: suspend (OnlineTrackRef) -> OnlineTrackLyrics?,
    private val matchLyrics: suspend (Song) -> LyricsDocument?,
    private val sharedLyrics: (String) -> LyricsDocument? = { id ->
        DesktopLyricsState.snapshot.value.takeIf { it.songId == id }?.document
    },
) {
    suspend fun load(song: Song, publish: suspend (LyricsDocument) -> Unit) {
        val ref = OnlineTrackRef.parse(song.path) ?: return
        var selected = withContext(Dispatchers.Default) {
            com.muses.player.feature.player.lyric.LyricsParser.parseDocument(song.lyrics)
        }?.takeIf { it.lines.isNotEmpty() }
        selected?.let { publish(it) }
        for (attempt in 0..2) {
            if (attempt > 0) delay(if (attempt == 1) 1500 else 3000)
            sharedLyrics(song.id)?.takeIf { it.lines.isNotEmpty() }?.let {
                publish(it)
                return
            }
            val raw = try {
                withTimeoutOrNull(8000) { resolveLyrics(ref) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { null }
            val script = withContext(Dispatchers.Default) {
                raw?.let { LxLyricParser.parse(it.lyric, it.tlyric, it.rlyric, it.lxlyric) }
            }?.takeIf { it.lines.isNotEmpty() }
            if (script?.lines?.any { it.syllables.isNotEmpty() } == true) selected = script
            else if (attempt == 0) {
                val matched = if (selected != null) null else try {
                    withTimeoutOrNull(8000) { matchLyrics(song) }
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { null }
                selected = selected ?: matched ?: script
            }
            else if (selected == null) selected = script
            selected?.let { publish(it) }
            if (selected?.lines?.any { it.syllables.isNotEmpty() } == true) return
        }
    }
}

/** 车机全文字段使用标准行级 LRC；当前行仍按原文档时间轴定位。 */
internal fun LyricsDocument.sessionLrc(): String = lines.filter { it.timeMs >= 0 }.joinToString("\n") {
    val minutes = (it.timeMs / 60000).toString().padStart(2, '0')
    val seconds = (it.timeMs / 1000 % 60).toString().padStart(2, '0')
    val millis = (it.timeMs % 1000).toString().padStart(3, '0')
    "[$minutes:$seconds.$millis]${it.text}"
}

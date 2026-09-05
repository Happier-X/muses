package com.muses.player.core.lyrics.client

import com.muses.player.core.lyrics.http.LyricsHttp
import com.muses.player.core.lyrics.model.LyricsDocument
import com.muses.player.core.lyrics.parser.TtmlLyricsParser
import java.io.IOException
import kotlinx.coroutines.CancellationException

/**
 * AMLL TTML database adapter（P2c：OkHttp 直连 → LyricsHttp/Ktor，AC1 清零 okhttp3）。
 * 错误文案冻结：非 2xx 仍抛 `AMLL TTML HTTP <code>`（历史消息形状，调用方无，保留防回归）。
 */
class AmlldbLyricsClient(
    private val http: LyricsHttp = LyricsHttp(),
) {
    suspend fun lyrics(neteaseSongId: Long): LyricsDocument {
        val body = try {
            http.getText("https://amlldb.bikonoo.com/ncm-lyrics/$neteaseSongId.ttml")
        } catch (e: CancellationException) {
            throw e
        } catch (e: kotlinx.io.IOException) {
            // LyricsHttp 抛 `http <code>`；本适配器历史文案为 `AMLL TTML HTTP <code>`，原样映射
            val code = e.message?.removePrefix("http ")?.trim() ?: "?"
            throw IOException("AMLL TTML HTTP $code")
        }
        if (body.isBlank() || body.trim() == "歌词不存在") return LyricsDocument(emptyList())
        return TtmlLyricsParser.parse(body)
    }
}

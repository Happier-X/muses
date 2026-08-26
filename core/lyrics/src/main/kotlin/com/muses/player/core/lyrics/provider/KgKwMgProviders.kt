package com.muses.player.core.lyrics.provider

import com.muses.player.core.lyrics.http.LyricsHttp
import com.muses.player.core.model.lyrics.OnlineLyricsFormat
import com.muses.player.core.model.lyrics.OnlineLyricsProviderHit
import com.muses.player.core.model.lyrics.OnlineLyricsQuery
import kotlinx.serialization.json.JsonObject

/**
 * 酷狗 / 酷我 / 咪咕 歌词 provider（规格书 = src/features/lyrics/providers/{kg,kw,mg}.ts，逐函数翻译）。
 */

// ── 酷狗（kg.ts）：song_search_v2 → lyrics.kugou.com search → download；MVP 取 lrc 明文 ──

private val KG_HEADERS = mapOf(
    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0",
    "Referer" to "https://www.kugou.com/",
    "Accept" to "application/json,text/plain,*/*",
)

private val KG_LYRIC_HEADERS = mapOf(
    "User-Agent" to "KuGou2012-9020-ExpandSearchManager",
    "KG-RC" to "1",
    "KG-THash" to "expand_search_manager.cpp:852736169:451",
)

/** base64 → UTF-8 文本（kg.ts decodeBase64Utf8） */
private fun decodeBase64Utf8(b64: String): String = runCatching {
    String(java.util.Base64.getDecoder().decode(b64), Charsets.UTF_8)
}.getOrDefault("")

private data class KgTrack(val hash: String, val durationMs: Long, val keyword: String) : ScoreableHit {
    override val title: String? = null
    override val artist: String? = null
    override val album: String? = null
}

private suspend fun searchKgTrack(http: LyricsHttp, query: OnlineLyricsQuery): KgTrack? {
    val keyword = buildKeyword(query)
    if (keyword.isEmpty()) return null
    val url = "https://songsearch.kugou.com/song_search_v2?keyword=${enc(keyword)}" +
        "&page=1&pagesize=10&userid=0&clientver=&platform=WebFilter" +
        "&filter=2&iscorrection=1&privilege_filter=0&area_code=1"

    val root = try {
        http.getJson(url, KG_HEADERS) as? JsonObject ?: return null
    } catch (_: Exception) {
        return null
    }
    val items = root.obj("data")?.obj("lists")?.get("lists").asObjArray()
    data class KgItem(
        val hash: String,
        val durationMs: Long,
        val fileNameKeyword: String,
        override val title: String?,
        override val artist: String?,
        override val album: String?,
    ) : ScoreableHit

    val list = items.mapNotNull { item ->
        val hash = item.str("FileHash")?.trim()?.takeUnless { h -> h.isEmpty() } ?: return@mapNotNull null
        KgItem(
            hash = hash,
            durationMs = ((item.str("Duration")?.toDoubleOrNull() ?: 0.0) * 1000).toLong().coerceAtLeast(0),
            fileNameKeyword = item.str("FileName") ?: item.str("SongName") ?: keyword,
            title = item.str("SongName") ?: item.str("OriSongName"),
            artist = item.str("SingerName"),
            album = item.str("AlbumName"),
        )
    }
    val best = pickBest(list, query) ?: return null
    return KgTrack(best.hash, best.durationMs, best.fileNameKeyword)
}

/** kg 搜索主流程（kg.ts searchKgLyrics）；失败返回 null 由链上下一源承接 */
suspend fun searchKgLyrics(http: LyricsHttp, query: OnlineLyricsQuery): OnlineLyricsProviderHit? {
    val track = searchKgTrack(http, query) ?: return null

    val searchUrl = "http://lyrics.kugou.com/search?ver=1&man=yes&client=pc" +
        "&keyword=${enc(track.keyword)}" +
        "&hash=${enc(track.hash)}" +
        "&timelength=${track.durationMs}&lrctxt=1"

    val searchBody = try {
        http.getJson(searchUrl, KG_LYRIC_HEADERS) as? JsonObject ?: return null
    } catch (_: Exception) {
        return null
    }
    val cand = searchBody.obj("candidates")?.get("candidates").asObjArray().firstOrNull()
        ?: return null
    val id = cand.str("id")?.takeIf(String::isNotEmpty) ?: return null
    val accesskey = cand.str("accesskey")?.takeIf(String::isNotEmpty) ?: return null

    // fmt=lrc 优先明文
    val dlUrl = "http://lyrics.kugou.com/download?ver=1&client=pc&id=${enc(id)}" +
        "&accesskey=${enc(accesskey)}&fmt=lrc&charset=utf8"

    val dl = try {
        http.getJson(dlUrl, KG_LYRIC_HEADERS) as? JsonObject ?: return null
    } catch (_: Exception) {
        return null
    }
    val content = dl.str("content")?.trim()?.takeUnless { it.isEmpty() } ?: return null

    val text = if (content.contains('[')) content else decodeBase64Utf8(content)
    if (text.isBlank() || !Regex("\\[\\d+:\\d+").containsMatchIn(text)) return null
    return OnlineLyricsProviderHit(text = text, format = OnlineLyricsFormat.LRC)
}

// ── 酷我（kw.ts）：搜索 + openapi getlyric（行级 LRC）──

private val KW_HEADERS = mapOf(
    "User-Agent" to "Mozilla/5.0",
    "Accept" to "application/json,text/plain,*/*",
    "Referer" to "https://www.kuwo.cn/",
)

/** kw.ts extractId：去掉 MUSIC_ 前缀 */
private fun extractId(rid: String?): String? {
    val raw = rid?.trim()?.takeUnless { it.isEmpty() } ?: return null
    return raw.replace(Regex("^MUSIC_", RegexOption.IGNORE_CASE), "").ifEmpty { null }
}

private suspend fun searchKwMusicId(http: LyricsHttp, query: OnlineLyricsQuery): String? {
    val keyword = buildKeyword(query)
    if (keyword.isEmpty()) return null
    val url = "https://search.kuwo.cn/r.s?client=kt&all=${enc(keyword)}" +
        "&pn=0&rn=10&uid=794762570&ver=kwplayer_ar_9.2.2.1&vipver=1" +
        "&show_copyright_off=1&newver=1&ft=music&cluster=0&strategy=2012" +
        "&encoding=utf8&rformat=json&vermerge=1&mobi=1&issubtitle=1"

    val body = try {
        http.getJson(url, KW_HEADERS) as? JsonObject ?: return null
    } catch (_: Exception) {
        return null
    }
    data class KwItem(override val title: String?, override val artist: String?, override val album: String?, val id: String) : ScoreableHit
    val list = body.obj("abslist")?.get("abslist").asObjArray().mapNotNull { item ->
        val id = extractId(item.str("MUSICRID")) ?: return@mapNotNull null
        KwItem(title = item.str("SONGNAME"), artist = item.str("ARTIST"), album = item.str("ALBUM"), id = id)
    }
    return pickBest(list, query)?.id
}

/** kw 搜索主流程（kw.ts searchKwLyrics） */
suspend fun searchKwLyrics(http: LyricsHttp, query: OnlineLyricsQuery): OnlineLyricsProviderHit? {
    val musicId = searchKwMusicId(http, query) ?: return null

    val lyricUrl = "https://www.kuwo.cn/openapi/v1/www/lyric/getlyric?musicId=${enc(musicId)}"
    val body = try {
        http.getJson(lyricUrl, KW_HEADERS) as? JsonObject ?: return null
    } catch (_: Exception) {
        return null
    }
    val rows = body.obj("data")?.obj("lrclist")?.get("lrclist").asObjArray()
    val lrc = linesToLrc(
        rows.map { row -> LrcLine(time = row.str("time")?.toDoubleOrNull() ?: row.str("time"), text = row.str("lineLyric")) },
    )
    if (lrc.isBlank()) return null
    return OnlineLyricsProviderHit(text = lrc, format = OnlineLyricsFormat.LRC)
}

// ── 咪咕（mg.ts）：移动搜索结果的歌词 URL 或内嵌歌词；失败返回 null 由链上下一源承接 ──

private val MIGU_HEADERS = mapOf(
    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0",
    "Referer" to "https://m.music.migu.cn/",
    "Accept" to "application/json,text/plain,*/*",
)

private data class MiguItem(
    override val title: String?,
    override val artist: String?,
    override val album: String?,
    val lrcUrl: String?,
    val lyrics: String?,
) : ScoreableHit

/** mg 搜索主流程（mg.ts searchMgLyrics） */
suspend fun searchMgLyrics(http: LyricsHttp, query: OnlineLyricsQuery): OnlineLyricsProviderHit? {
    val keyword = buildKeyword(query)
    if (keyword.isEmpty()) return null

    val searchUrl = "https://m.music.migu.cn/migu/remoting/scr_search_tag?rows=10&type=2" +
        "&keyword=${enc(keyword)}&pgc=1"

    val root = try {
        http.getJson(searchUrl, MIGU_HEADERS) as? JsonObject ?: return null
    } catch (_: Exception) {
        return null
    }

    val list = root.get("musics").asObjArray().map { item ->
        MiguItem(
            title = item.str("songName"),
            artist = item.str("singerName"),
            album = item.str("albumName"),
            lrcUrl = item.str("lrcUrl"),
            lyrics = item.str("lyrics"),
        )
    }

    val best = pickBest(list, query) ?: return null

    // 内嵌歌词优先
    best.lyrics?.trim()?.let { lyrics ->
        if (lyrics.isNotEmpty() && Regex("\\[\\d+:\\d+").containsMatchIn(lyrics)) {
            return OnlineLyricsProviderHit(text = lyrics, format = OnlineLyricsFormat.LRC)
        }
    }

    // 其次 lrcUrl 直取
    best.lrcUrl?.trim()?.takeUnless { s -> s.isEmpty() }?.let { lrcUrl ->
        try {
            val text = http.getText(lrcUrl, MIGU_HEADERS).trim()
            if (text.isNotEmpty() && Regex("\\[\\d+:\\d+").containsMatchIn(text)) {
                return OnlineLyricsProviderHit(text = text, format = OnlineLyricsFormat.LRC)
            }
        } catch (_: Exception) {
            return null
        }
    }

    return null
}

internal fun enc(value: String): String =
    java.net.URLEncoder.encode(value, "UTF-8")

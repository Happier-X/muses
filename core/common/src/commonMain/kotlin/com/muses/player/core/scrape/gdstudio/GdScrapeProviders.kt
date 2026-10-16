package com.muses.player.core.scrape.gdstudio

import com.muses.player.core.data.store.platformNowMs
import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.model.scrape.OnlineTextSource
import com.muses.player.core.model.scrape.TextMetaHit
import com.muses.player.core.scrape.cover.CoverProvider
import com.muses.player.core.scrape.cover.OnlineCoverQuery
import com.muses.player.core.scrape.cover.OnlineCoverSource
import com.muses.player.core.scrape.editmeta.EditCloudMetaQuery
import com.muses.player.core.scrape.editmeta.LyricsHit
import com.muses.player.core.scrape.editmeta.LyricsSearchPort
import com.muses.player.core.scrape.http.ScrapeHttp
import com.muses.player.core.network.ProviderHttpException
import com.muses.player.core.scrape.text.TextMetaProvider
import com.muses.player.core.scrape.text.normalizeNfkc
import com.muses.player.core.scrape.text.pickBestHit
import com.muses.player.core.scrape.text.provider.urlEncode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

/** GD 音乐台（music.gdstudio.xyz）刮削接口，三个维度共用搜索结果及请求预算。 */
class GdScrapeClient(
    private val http: ScrapeHttp,
    private val enabled: suspend () -> Boolean = { true },
    private val now: () -> Long = { platformNowMs() },
) {
    internal data class Track(val id: String, val title: String, val artist: String,
        val album: String?, val lyricId: String, val pictureId: String?)
    private val searchMutex = Mutex()
    private val budgetMutex = Mutex()
    private val requestTimes = ArrayDeque<Long>()
    private val cache = linkedMapOf<String, Pair<Long, List<Track>>>()

    private suspend fun request(parameters: String): JsonElement {
        budgetMutex.withLock {
            val time = now()
            while (requestTimes.firstOrNull()?.let { it <= time - 300_000 } == true) requestTimes.removeFirst()
            if (requestTimes.size >= 50) throw ProviderHttpException(429)
            requestTimes.addLast(time)
        }
        return http.getJson("https://music-api.gdstudio.xyz/api.php?source=netease&$parameters")
    }

    internal suspend fun search(title: String, artist: String?): List<Track> {
        if (!enabled() || title.isBlank()) return emptyList()
        val keyword = listOf(title.trim(), artist?.trim().orEmpty()).filter { it.isNotEmpty() }.joinToString(" ")
        return searchMutex.withLock {
            cache[keyword]?.takeIf { now() - it.first < 300_000 }?.let { return@withLock it.second }
            // 优先使用官方当前稳定的网易云目录，不把其他平台的 ID 交叉透传。
            val response = request("types=search&name=${urlEncode(keyword)}&count=20&pages=1")
            val rows = response as? JsonArray ?: error("GD 音乐台搜索返回结构无效")
            val tracks = rows.mapNotNull { row ->
                val item = row as? JsonObject ?: return@mapNotNull null
                fun value(key: String) = (item[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
                if (value("source")?.let { it != "netease" } == true) return@mapNotNull null
                val id = value("id") ?: return@mapNotNull null
                val name = value("name") ?: return@mapNotNull null
                val singer = (item["artist"] as? JsonArray)?.mapNotNull {
                    (it as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)
                }?.joinToString(" / ") ?: return@mapNotNull null
                if (singer.isBlank()) return@mapNotNull null
                Track(id, name, singer, value("album"), value("lyric_id") ?: id, value("pic_id"))
            }
            if (cache.size >= 32) cache.remove(cache.keys.first())
            cache[keyword] = now() to tracks
            tracks
        }
    }

    private fun normalize(value: String) = normalizeNfkc(value).lowercase().filter { it.isLetterOrDigit() }
    private fun artists(value: String) = value.split(Regex("[/、,，&;；]+"))
        .map(::normalize).filter(String::isNotEmpty).toSet()

    internal suspend fun matched(title: String, artist: String?, album: String?): List<Track> {
        if (artist.isNullOrBlank()) return emptyList()
        return search(title, artist).filter {
            normalize(it.title) == normalize(title) && artists(it.artist) == artists(artist)
        }.sortedByDescending { !album.isNullOrBlank() && normalize(it.album.orEmpty()) == normalize(album) }
    }

    internal suspend fun lyrics(track: Track): LyricsHit? {
        if (!enabled()) return null
        val data = request("types=lyric&id=${urlEncode(track.lyricId)}").jsonObject
        val lyric = (data["lyric"] as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) ?: return null
        val translation = (data["tlyric"] as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)
        return LyricsHit(lyric, "lrc", translation)
    }

    internal suspend fun cover(track: Track): String? {
        if (!enabled()) return null
        val id = track.pictureId ?: return null
        val data = request("types=pic&id=${urlEncode(id)}&size=500").jsonObject
        return (data["url"] as? JsonPrimitive)?.contentOrNull?.trim()
            ?.takeIf { Regex("^https?://\\S+$").matches(it) }
    }
}

class GdTextProvider(private val client: GdScrapeClient) : TextMetaProvider {
    override val id = OnlineTextSource.GD
    override suspend fun searchCandidates(query: OnlineTextQuery): List<TextMetaHit> =
        client.search(query.title, query.artist).map { TextMetaHit(it.title, it.artist, it.album, id) }
    override suspend fun search(query: OnlineTextQuery) = pickBestHit(searchCandidates(query), query)
}

class GdCoverProvider(private val client: GdScrapeClient) : CoverProvider {
    override val id = OnlineCoverSource.GD
    override suspend fun searchCoverUrl(query: OnlineCoverQuery): String? {
        for (track in client.matched(query.title, query.artist, query.album)) client.cover(track)?.let { return it }
        return null
    }
}

class GdLyricsPort(private val client: GdScrapeClient) : LyricsSearchPort {
    override val id = "gdstudio"
    override suspend fun searchLyrics(query: EditCloudMetaQuery): LyricsHit? {
        for (track in client.matched(query.title, query.artist, query.album)) client.lyrics(track)?.let { return it }
        return null
    }
}

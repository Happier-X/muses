package com.muses.player.core.search

import com.muses.player.core.search.http.SearchHttp
import com.muses.player.core.search.provider.arr
import com.muses.player.core.search.provider.long
import com.muses.player.core.search.provider.obj
import com.muses.player.core.search.provider.str
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** 平台歌单元信息，封面和标题始终来自同一个歌单。 */
@Serializable
data class OnlinePlaylist(
    val platform: String,
    val id: String,
    val title: String,
    val coverUrl: String,
    val creator: String? = null,
    val description: String? = null,
    val trackCount: Int? = null,
)

class OnlinePlaylistService(private val http: SearchHttp = SearchHttp()) {
    suspend fun featured(): List<OnlinePlaylist> {
        val root = http.parseObject(http.getText(
            "https://music.163.com/api/playlist/highquality/list?limit=30",
            referer = "https://music.163.com/",
        ))
        check(root.long("code") == 200L) { "精选歌单暂时不可用，请稍后重试" }
        val items = root.arr("playlists") ?: error("歌单数据格式异常")
        return items.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val id = item.long("id")?.toString() ?: return@mapNotNull null
            val title = item.str("name")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val cover = item.str("coverImgUrl")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            OnlinePlaylist(
                platform = "wy", id = id, title = title,
                coverUrl = if (cover.startsWith("http://")) "https://" + cover.removePrefix("http://") else cover,
                creator = item.obj("creator")?.str("nickname"),
                description = item.str("description"),
                trackCount = item.long("trackCount")?.toInt(),
            )
        }.distinctBy { it.id }.take(30)
    }
}

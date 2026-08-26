package com.muses.player.core.data.index

import com.muses.player.core.model.Song
import java.security.MessageDigest

/** 专辑/艺术家索引推导结果 */
data class LibraryIndexes(
    val albums: List<AlbumRow>,
    val artists: List<ArtistRow>,
    val songAlbumRefs: List<Pair<String, String>>,
    val songArtistRefs: List<Pair<String, String>>,
)

data class AlbumRow(val id: String, val title: String, val artist: String?, val year: Int?, val songCount: Int)

data class ArtistRow(val id: String, val name: String, val albumCount: Int, val songCount: Int)

/**
 * 从歌曲列表重建专辑/艺术家索引（纯函数，可 JVM 单测）。
 *
 * 规则：
 * - 专辑分组键 = (albumTitle 小写归一化, albumArtist 归一化)；albumArtist 缺失时回退 artist，
 *   再缺失归入「合辑」（Various Artists），避免同名专辑被无主艺术家错误合并。
 * - 艺术家按 track artist 拆分（`;` / `/` 分隔的多艺术家各建一条索引）。
 * - id = 内容哈希，保证跨扫描稳定。
 */
object LibraryIndexer {

    const val VARIOUS_ARTISTS = "Various Artists"

    fun build(songs: List<Song>): LibraryIndexes {
        // 专辑：groupKey -> (title, artist, songs)
        data class AlbumAcc(var title: String, var artist: String?, var songIds: MutableSet<String>, var year: Int?)

        val albums = LinkedHashMap<String, AlbumAcc>()
        val songAlbumRefs = mutableListOf<Pair<String, String>>()
        // 艺术家：nameLower -> 展示名（albumCount/songCount 由 refs 统计得出）
        val artists = LinkedHashMap<String, String>()
        val songArtistRefs = linkedSetOf<Pair<String, String>>()


        for (song in songs) {
            // ---- 专辑 ----
            val albumTitle = song.album?.trim().orEmpty()
            if (albumTitle.isNotEmpty()) {
                val albumArtistRaw = song.artist?.trim()
                val groupArtist = albumArtistRaw?.ifEmpty { null }?.lowercase() ?: VARIOUS_ARTISTS.lowercase()
                val key = "${albumTitle.lowercase()}||$groupArtist"
                val acc = albums.getOrPut(key) {
                    AlbumAcc(albumTitle, albumArtistRaw?.ifEmpty { null } ?: VARIOUS_ARTISTS, mutableSetOf(), null)
                }
                acc.songIds.add(song.id)
                songAlbumRefs.add(song.id to albumId(key))
            }

            // ---- 艺术家 ----
            val artistNames = splitArtists(song.artist)
            if (artistNames.isEmpty()) continue
            for (rawName in artistNames) {
                val lower = rawName.lowercase()
                artists.putIfAbsent(lower, rawName)
                songArtistRefs.add(song.id to artistId(lower))
            }
        }

        // 艺术家 albumCount 需按其名下歌曲实际所属专辑去重统计——重扫一遍 refs 更可靠
        val songIdToAlbumKeys = HashMap<String, MutableSet<String>>()
        songs.forEach { song ->
            val albumTitle = song.album?.trim().orEmpty()
            if (albumTitle.isNotEmpty()) {
                val albumArtistRaw = song.artist?.trim()?.ifEmpty { null }
                val groupArtist = albumArtistRaw?.lowercase() ?: VARIOUS_ARTISTS.lowercase()
                val key = "${albumTitle.lowercase()}||$groupArtist"
                songIdToAlbumKeys.getOrPut(song.id) { mutableSetOf() }.add(key)
            }
        }

        val artistRows = artists.map { (lower, displayName) ->
            val ownedSongs = songArtistRefs.filter { it.second == artistId(lower) }.map { it.first }
            val albumCount = ownedSongs.flatMap { songIdToAlbumKeys[it].orEmpty() }.distinct().size
            ArtistRow(
                id = artistId(lower),
                name = displayName,
                albumCount = albumCount,
                songCount = ownedSongs.distinct().size,
            )
        }

        val albumRows = albums.map { (key, acc) ->
            AlbumRow(
                id = albumId(key),
                title = acc.title,
                artist = acc.artist,
                year = acc.year,
                songCount = acc.songIds.size,
            )
        }

        return LibraryIndexes(
            albums = albumRows,
            artists = artistRows,
            songAlbumRefs = songAlbumRefs,
            songArtistRefs = songArtistRefs.map { it.first to it.second },
        )
    }

    /** 多艺术家拆分：`;`、`/`、`、`分隔；空串返回空列表 */
    fun splitArtists(raw: String?): List<String> =
        raw.orEmpty()
            .split(';', '/', '、')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun albumId(groupKey: String): String = sha256("album:$groupKey")

    private fun artistId(nameLower: String): String = sha256("artist:$nameLower")

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}

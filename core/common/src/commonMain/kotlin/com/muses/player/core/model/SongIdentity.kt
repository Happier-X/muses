package com.muses.player.core.model

internal expect fun normalizeSongIdentity(value: String): String

/** 歌名保留版本文字；跨平台匹配必须同时具备相同歌手，空歌手只按歌曲 ID 判断。 */
fun sameSong(left: Song, right: Song): Boolean {
    if (left.id == right.id) return true
    fun artists(value: String?) = value.orEmpty().split(Regex("[/、,，&;；]+"))
        .map(::normalizeSongIdentity).filter { it.isNotEmpty() }.toSet()
    val name = normalizeSongIdentity(left.title)
    val artist = artists(left.artist)
    return name.isNotEmpty() && artist.isNotEmpty() &&
        name == normalizeSongIdentity(right.title) && artist == artists(right.artist)
}

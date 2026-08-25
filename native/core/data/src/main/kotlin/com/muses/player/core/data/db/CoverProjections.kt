package com.muses.player.core.data.db

/** 专辑封面投影：每张专辑任取一首有封面的歌（Web 版 getAlbumCoverSrc 同语义） */
data class AlbumCoverProjection(val albumId: String, val coverUri: String)

/** 艺术家封面投影：每位艺术家任取一首有封面的歌 */
data class ArtistCoverProjection(val artistId: String, val coverUri: String)

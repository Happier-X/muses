package com.muses.player.nativem1.navigation

import androidx.annotation.StringRes
import com.muses.player.nativem1.R

/** 顶层路由 */
enum class NavDestination(
    val route: String,
    @StringRes val labelRes: Int,
) {
    Songs("songs", R.string.nav_songs),
    Albums("albums", R.string.nav_albums),
    Artists("artists", R.string.nav_artists),
    Playlists("playlists", R.string.nav_playlists),
    Sources("sources", R.string.nav_sources),
    NowPlaying("now-playing", R.string.nav_now_playing),
    Queue("queue", R.string.nav_queue),
    ;

    companion object {
        fun fromRoute(route: String?): NavDestination? =
            entries.firstOrNull { it.route == route }
    }
}

/** 详情页路由（非顶层，需要参数） */
object DetailRoutes {
    const val ALBUM_DETAIL = "album/{albumId}"
    const val ARTIST_DETAIL = "artist/{artistId}"

    fun albumDetail(albumId: String) = "album/$albumId"
    fun artistDetail(artistId: String) = "artist/$artistId"
}

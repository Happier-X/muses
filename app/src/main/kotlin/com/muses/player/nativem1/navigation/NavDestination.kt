package com.muses.player.nativem1.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import com.muses.player.nativem1.R

/**
 * 顶层路由 —— 条目/顺序/分组一比一对照 `TabsPage.vue` 的 `navItems`：
 *
 *   primary   = 歌曲(musicalNotes)/专辑(albums=Disc3)/艺术家(person=MicVocal)/歌单(list=ListMusic)
 *   secondary = 刮削(listChecks)/音源(radio→Folder 语义，Web 层即用 Folder 图标)/设置(settings)
 *
 * childPrefixes 对照 Web 层同名语义：详情页打开时对应导航项保持激活态
 * （isNavActive：route 精确匹配 / route+`/` 前缀 / childPrefixes 前缀）。
 */
enum class NavDestination(
    val route: String,
    @StringRes val labelRes: Int,
    /** Web 层同款语义的 Material 图标（括号内为 lucide 原名） */
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    /** 子路由前缀（对照 Web 层 NavigationItem.childPrefixes） */
    val childPrefixes: List<String> = emptyList(),
) {
    // ---- 主菜单（曲库）：primaryNavItems = navItems.slice(0, 4) ----
    Songs("songs", R.string.nav_songs, Icons.Filled.MusicNote),            // musicalNotes (Music)
    Albums("albums", R.string.nav_albums, Icons.Filled.Album,              // albums (Disc3)
        childPrefixes = listOf(DetailRoutes.ALBUM_DETAIL_PREFIX)),
    Artists("artists", R.string.nav_artists, Icons.Filled.Person,          // person (MicVocal)
        childPrefixes = listOf(DetailRoutes.ARTIST_DETAIL_PREFIX)),
    Playlists("playlists", R.string.nav_playlists, Icons.Filled.QueueMusic, // list (ListMusic)
        childPrefixes = listOf("playlist/")),

    // ---- 次菜单（工具）：secondaryNavItems = navItems.slice(4) ----
    Scrape("scrape", R.string.nav_scrape, Icons.Filled.Checklist),         // listChecks
    Sources("sources", R.string.nav_sources, Icons.Filled.Folder,          // radio (Web 层映射 Folder)
        childPrefixes = listOf("sources/webdav")),
    Settings("settings", R.string.nav_settings, Icons.Filled.Settings),    // settings

    // ---- 非 tabs 导航项（播放页/队列页，不出现在侧边栏）----
    NowPlaying("now-playing", R.string.nav_now_playing, Icons.Filled.MusicNote),
    Queue("queue", R.string.nav_queue, Icons.Filled.QueueMusic),
    ;

    /** 对照 TabsPage.vue 的 isNavActive(item) */
    fun isActive(currentRoute: String?): Boolean {
        if (currentRoute == null) return false
        if (currentRoute == route || currentRoute.startsWith("$route/")) return true
        return childPrefixes.any { currentRoute.startsWith(it) }
    }

    companion object {
        fun fromRoute(route: String?): NavDestination? =
            entries.firstOrNull { it.route == route }

        /** 侧边栏主导航组（曲库），顺序对照 primaryNavItems */
        val Primary: List<NavDestination> = listOf(Songs, Albums, Artists, Playlists)

        /** 侧边栏辅助导航组（工具），顺序对照 secondaryNavItems */
        val Secondary: List<NavDestination> = listOf(Scrape, Sources, Settings)
    }
}

/** 详情页路由（非顶层，需要参数） */
object DetailRoutes {
    const val ALBUM_DETAIL = "album/{albumId}"
    const val ARTIST_DETAIL = "artist/{artistId}"

    // ---- P5 音源域子路由（对照 Web 层 /tabs/sources/webdav*）----
    /** WebDAV 添加表单 */
    const val SOURCE_WEBDAV_ADD = "sources/webdav"
    /** WebDAV 编辑表单（sourceId 定位音源） */
    const val SOURCE_WEBDAV_EDIT = "sources/webdav/{sourceId}"
    /** WebDAV 目录浏览页（连接信息经导航参数传入） */
    const val SOURCE_WEBDAV_BROWSE = "sources/webdav/browse"

    /** NavDestination.childPrefixes 激活判定用（不带参数占位的前缀） */
    const val ALBUM_DETAIL_PREFIX = "album/"
    const val ARTIST_DETAIL_PREFIX = "artist/"

    fun albumDetail(albumId: String) = "album/$albumId"
    fun artistDetail(artistId: String) = "artist/$artistId"

    fun sourceWebdavEdit(sourceId: String) = "sources/webdav/$sourceId"
}

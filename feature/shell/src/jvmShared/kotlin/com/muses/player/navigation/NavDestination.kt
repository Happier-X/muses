package com.muses.player.navigation

import com.muses.player.core.ui.icons.TablerIcons
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * 顶层导航项 —— 文案/图标/分组一比一对照 `TabsPage.vue` 的 `navItems`：
 *
 *   primary   = 歌曲/专辑/艺术家/歌单
 *   secondary = 刮削/音源/设置
 *
 * 路由本体已类型化（[MusesRoute]，miuix-nav @Serializable 密封层级）；
 * 本 enum 只保留展示层（label/icon）与分组/激活判定：
 * - [routeKey]：tab 本体 key（详情页激活由 [isActive] 按 key 类型判定，对照 Web 层 childPrefixes）
 * - [isActive]：对照 TabsPage.vue 的 isNavActive
 *
 * U22 上收：labelRes（安卓资源 id）改为共享文案常量（工程 UI 文案统一中文硬编码，
 * 与 commonMain 各 Page 层一致，避免引入 CMP 资源基建）。
 */
enum class NavDestination(
    val routeKey: MusesRoute,
    val label: String,
    /** Web 层同款语义的图标（括号内为 Tabler 原名） */
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    // ---- 主菜单（曲库）：primaryNavItems = navItems.slice(0, 4) ----
    Songs(MusesRoute.Songs, "歌曲", TablerIcons.MusicNote),            // music (Music)
    Albums(MusesRoute.Albums, "专辑", TablerIcons.Album),               // albums (Disc)
    Artists(MusesRoute.Artists, "艺术家", TablerIcons.Person),          // user (MicVocal)
    Playlists(MusesRoute.Playlists, "歌单", TablerIcons.QueueMusic),    // playlist (ListMusic)

    // ---- 次菜单（工具）：secondaryNavItems = navItems.slice(4) ----
    Scrape(MusesRoute.Scrape, "刮削", TablerIcons.Checklist),          // listCheck
    Sources(MusesRoute.Sources, "音源", TablerIcons.Folder),           // broadcast (Web 层映射 Folder)
    Settings(MusesRoute.Settings, "设置", TablerIcons.Settings),       // settings
    ;

    /** 对照 TabsPage.vue 的 isNavActive(item)：tab 本体 + 其详情/子页均算激活 */
    fun isActive(key: NavKey?): Boolean = when (this) {
        Songs -> key == MusesRoute.Songs
        Albums -> key == MusesRoute.Albums || key is MusesRoute.AlbumDetail
        Artists -> key == MusesRoute.Artists || key is MusesRoute.ArtistDetail
        Playlists -> key == MusesRoute.Playlists || key is MusesRoute.PlaylistDetail
        Scrape -> key == MusesRoute.Scrape || key is MusesRoute.ScrapeReview
        Sources -> key == MusesRoute.Sources || key is MusesRoute.WebDavAdd ||
            key is MusesRoute.WebDavEdit || key is MusesRoute.WebDavBrowse
        Settings -> key == MusesRoute.Settings
    }

    companion object {
        /** 侧边栏主导航组（曲库），顺序对照 primaryNavItems */
        val Primary: List<NavDestination> = listOf(Songs, Albums, Artists, Playlists)

        /** 侧边栏辅助导航组（工具），顺序对照 secondaryNavItems */
        val Secondary: List<NavDestination> = listOf(Scrape, Sources, Settings)
    }
}

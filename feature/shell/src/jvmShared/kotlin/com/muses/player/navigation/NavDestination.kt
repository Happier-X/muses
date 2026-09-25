package com.muses.player.navigation

import com.muses.player.core.ui.icons.TablerIcons
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * 顶层导航项：底栏为探索/曲库/我的，曲库内部提供歌曲/专辑/艺术家，
 * 我的页面提供音源/刮削/统计/历史记录/设置。
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
    // ---- 探索（U27 新增：搜索框 + 排行榜 + 猜你喜欢，启动默认页）----
    Home(MusesRoute.Home, "探索", TablerIcons.Compass),

    // ---- 主菜单（曲库）：primaryNavItems = navItems.slice(0, 4) ----
    Songs(MusesRoute.Songs, "歌曲", TablerIcons.MusicNote),            // music (Music)
    Albums(MusesRoute.Albums, "专辑", TablerIcons.Album),               // albums (Disc)
    Artists(MusesRoute.Artists, "艺术家", TablerIcons.Person),          // user (MicVocal)

    // ---- 我的与工具 ----
    Scrape(MusesRoute.Scrape, "刮削", TablerIcons.Checklist),          // listCheck
    Mine(MusesRoute.Mine, "我的", TablerIcons.Person),
    ;

    /** 对照 TabsPage.vue 的 isNavActive(item)：tab 本体 + 其详情/子页均算激活 */
    fun isActive(key: NavKey?): Boolean = when (this) {
        Home -> key == MusesRoute.Home
        Songs -> key == MusesRoute.Songs
        Albums -> key == MusesRoute.Albums || key is MusesRoute.AlbumDetail
        Artists -> key == MusesRoute.Artists || key is MusesRoute.ArtistDetail
        Scrape -> key == MusesRoute.Scrape || key is MusesRoute.ScrapeReview
        Mine -> key == MusesRoute.Mine || key == MusesRoute.Settings ||
            key == MusesRoute.Statistics || key == MusesRoute.History ||
            key is MusesRoute.AiSettings ||
            key == MusesRoute.Sources || key is MusesRoute.WebDavAdd ||
            key is MusesRoute.WebDavEdit || key is MusesRoute.WebDavBrowse ||
            key is MusesRoute.LxScripts || key == MusesRoute.Scrape ||
            key is MusesRoute.ScrapeReview
    }

    companion object {
        /** 主导航组（探索 + 曲库）：顺序对照 primaryNavItems、前置探索 */
        val Primary: List<NavDestination> = listOf(Home, Songs, Albums, Artists)

        /** 工具与我的入口 */
        val Secondary: List<NavDestination> = listOf(Scrape, Mine)
    }
}

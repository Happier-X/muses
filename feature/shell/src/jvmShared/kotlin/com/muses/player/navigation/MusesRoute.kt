package com.muses.player.navigation

import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * 类型化路由（miuix-nav）：密封接口 + @Serializable data 路由。
 *
 * 对照原 string 路由表（NavDestination.route / DetailRoutes）一比一映射：
 * - 顶层 tabs：Songs/Albums/Artists/Playlists/Scrape/Sources/Settings（无参 data object）
 * - 详情/表单：AlbumDetail/ArtistDetail/PlaylistDetail/WebDavEdit（单 id 参数）
 * - 刮削审核：ScrapeReview(songId + queueCsv，逗号分隔队列上下文，对照原 URL query）
 * - WebDAV 浏览：WebDavBrowse（连接信息直传字段，原 URLEncoder query 入参）
 *
 * 约束（miuix-nav 存栈恢复硬性要求）：路由类必须 @Serializable data（值语义 toString），
 * 同栈内相同值 key 禁止重复 push（调用方 navigateToTab/pushUnique 负责幂等）。
 */
@Serializable
sealed interface MusesRoute : NavKey {
    // ---- 主菜单（曲库）----
    @Serializable
    data object Songs : MusesRoute

    @Serializable
    data object Albums : MusesRoute

    @Serializable
    data object Artists : MusesRoute

    @Serializable
    data class AlbumDetail(val albumId: String) : MusesRoute

    @Serializable
    data class ArtistDetail(val artistId: String) : MusesRoute

    @Serializable
    data object Playlists : MusesRoute

    @Serializable
    data class PlaylistDetail(val playlistId: String) : MusesRoute

    // ---- 次菜单（工具）----
    @Serializable
    data object Scrape : MusesRoute

    /**
     * 刮削审核页（songId 必传、queueCsv 可选逗号分隔队列上下文，供「应用并下一首」推进）。
     * 对照原 `"scrape_review?songId={songId}&queue={queue}"`（见旧 DetailRoutes.scrapeReview）。
     */
    @Serializable
    data class ScrapeReview(val songId: String, val queueCsv: String = "") : MusesRoute

    @Serializable
    data object Sources : MusesRoute

    @Serializable
    data object WebDavAdd : MusesRoute

    @Serializable
    data class WebDavEdit(val sourceId: String) : MusesRoute

    /**
     * WebDAV 目录浏览页（连接信息直传字段；原 URL query 入参，对照旧 navigateToWebdavBrowse）。
     * 含密码字段：与原方案同等暴露面（存栈序列化），调用方不得打日志。
     */
    @Serializable
    data class WebDavBrowse(
        val mode: String = "multiple",
        val initialPath: String = "/",
        val serverUrl: String = "",
        val username: String = "",
        val password: String = "",
    ) : MusesRoute

    @Serializable
    data object Settings : MusesRoute
}

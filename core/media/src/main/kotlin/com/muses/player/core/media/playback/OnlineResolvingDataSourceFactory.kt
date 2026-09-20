package com.muses.player.core.media.playback

import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import com.muses.player.core.model.online.OnlineTrackRef
import com.muses.player.core.model.online.OnlineTrackResolver
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 在线曲目直链解析数据源：MediaItem 保留 `muslx://` 引用，
 * 在 **DataSource 真正打开时才**换取 HTTP 直链。
 *
 * ## 为什么必须这样做（而不是入队前批量解析）
 * 初版实现在入队前对**整队**逐首解析，实测 20 首队列需串行等待约 30 秒才开始播放
 * （每首含一次脚本调用 + 一次网络请求），用户点播放后长期无反应 = 功能不可用。
 * 且在线直链**带时效签名**，预解析的地址在真正播到时可能已失效。
 *
 * 交给 Media3 的 [ResolvingDataSource] 后：
 * - 点播放立即出声（只为当前曲解析一次）；
 * - 切歌/重试/拖动时按需解析，每次 open 都是新鲜直链，天然规避过期；
 * - 队列语义与本地曲目完全一致（MediaItem 仍持有稳定 `muslx://` 标识）。
 *
 * 线程：`resolveDataSpec` 在 Media3 的加载线程调用（非主线程），
 * 故可用 [runBlocking] 桥接 suspend 解析器；带超时兜底避免加载线程被脚本挂死。
 */
class OnlineResolvingDataSourceFactory(
    private val upstreamFactory: DataSource.Factory,
    private val resolver: OnlineTrackResolver,
    /** 单次解析超时；超时视为打开失败，交 Media3 播放错误链处理 */
    private val resolveTimeoutMs: Long = 20_000L,
    /** 解析失败/超时的上报（供 R2 埋点） */
    private val onResolveError: (trackUri: String, error: Throwable?) -> Unit = { _, _ -> },
) : DataSource.Factory {

    override fun createDataSource(): DataSource =
        ResolvingDataSource(upstreamFactory.createDataSource()) { dataSpec ->
            val trackUri = dataSpec.uri.toString()
            val ref = OnlineTrackRef.parse(trackUri) ?: return@ResolvingDataSource dataSpec
            val resolved = try {
                runBlocking { withTimeoutOrNull(resolveTimeoutMs) { resolver.resolve(ref) } }
            } catch (e: Exception) {
                onResolveError(trackUri, e)
                null
            }
            if (resolved == null) {
                onResolveError(trackUri, null)
                // 保留原始 URI：交由 Media3 报「无法打开」并走既有失败恢复链，
                // 而不是在此抛错（抛错会被包装成难以归因的加载异常）
                return@ResolvingDataSource dataSpec
            }
            dataSpec.withUri(Uri.parse(resolved.url))
        }
}

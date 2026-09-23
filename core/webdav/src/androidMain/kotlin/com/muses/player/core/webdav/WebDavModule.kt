package com.muses.player.core.webdav

import java.net.Proxy
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** 流播专用 OkHttpClient 限定名：只注入 Basic Auth，不施加 4 rps 限流（见 [webdavModule]）。 */
const val STREAMING_OKHTTP_QUALIFIER = "streamingOkHttp"

/**
 * WebDAV 安卓侧装配（U11 KMP 拆分后仅剩 OkHttp/磁盘缓存绑定；AuthRegistry/Ktor 客户端/
 * 限流器已上收 jvmShared [webdavCoreModule]，安卓经 AppKoinModule 双模块装载）。
 * 两个 OkHttpClient 绑定明确保留（P2c 豁免）：
 * 流播 `@StreamingOkHttp` 喂 Media3 `OkHttpDataSource`，默认绑定喂 `AudioTagReader` Range 下载。
 */
val webdavModule = module {

    singleOf(::DiskWebDavAudioCache)
    single<WebDavAudioCache> { get<DiskWebDavAudioCache>() }

    /**
     * 流播专用 OkHttpClient：只注入 Basic Auth，**不施加 4 rps 限流**。
     *
     * 流播是 ExoPlayer 对 WebDAV URL 的单连接持续读取（边播边读），请求量极小、串行、
     * 不构成 burst，无需节流；若套 4 rps 限流反而会把流播 Range 请求和扫描/预取挤在
     * 同一桶里饿死/超时重试 → 叠加 429。
     * 限流只作用于扫描/批量预取等可能并发突发的链路（[OkHttpClient] 默认绑定）。
     *
     * 用户决策 2026-08-27：保持流式播放 + CacheDataSource 边播边缓存，流播链路不套限流
     * （对齐 cross-layer 限流显式化：流播单连接请求率远低于 CDN 阈值，不构成错配）。
     */
    single<OkHttpClient>(named(STREAMING_OKHTTP_QUALIFIER)) {
        val registry: WebDavAuthRegistry = get()
        OkHttpClient.Builder()
            // 安卓设备可能配置了只供浏览器使用的系统 HTTP 代理（例如 MuMu/Clash）。
            // WebDAV HTTPS 流播经该代理会在 CONNECT 隧道阶段断开；此客户端直连源站。
            .proxy(Proxy.NO_PROXY)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val auth = if (request.header("Authorization") == null) {
                    registry.authorizationHeader(request.url.toString())
                } else {
                    null
                }
                chain.proceed(
                    if (auth != null) request.newBuilder().header("Authorization", auth).build() else request,
                )
            }
            .build()
    }

    /** WebDAV 服务器常见于家庭 NAS，超时放宽 */
    single<OkHttpClient> {
        val registry: WebDavAuthRegistry = get()
        val rateLimiter: WebDavRateLimiter = get()
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            // ① 共享限流：所有经此 OkHttpClient 的 WebDAV/播放流播请求统一 4 rps
            //    （WebDavClient 的挂起 acquire 与此处阻塞共用同一桶；已在 WebDavClient 层限过的不重复限）
            .addInterceptor { chain ->
                val request = chain.request()
                // WebDavClient 已在协程层 acquire 的请求打标，限流层跳过避免 double-delay
                // P2c 注：Ktor 链路不再发送此头（仅历史 OkHttp 链路残留），此处仅 strip 防污染服务端。
                if (request.header("X-Muses-Rate-Limited") != null) {
                    return@addInterceptor chain.proceed(request.newBuilder().removeHeader("X-Muses-Rate-Limited").build())
                }
                // P2c：阻塞等待改经 reserveBlockingDelayMs（限流器本体已进 commonMain，无 Thread.sleep）
                val waitMs = rateLimiter.reserveBlockingDelayMs()
                if (waitMs > 0L) Thread.sleep(waitMs)
                chain.proceed(request)
            }
            // ② 播放流播统一注入 Basic Auth（interceptor 在 OkHttp IO 线程执行）；
            // 请求已自带 Authorization 时（如扫描器经 client.authenticate 手动设置）不覆盖，避免旧凭据表反客为主
            .addInterceptor { chain ->
                val request = chain.request()
                val auth = if (request.header("Authorization") == null) {
                    registry.authorizationHeader(request.url.toString())
                } else {
                    null
                }
                chain.proceed(
                    if (auth != null) request.newBuilder().header("Authorization", auth).build() else request,
                )
            }
            .build()
    }
}

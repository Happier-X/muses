package com.muses.player.core.webdav

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** 流播专用 OkHttpClient 限定名：只注入 Basic Auth，不施加 4 rps 限流（见 [webdavModule]）。 */
const val STREAMING_OKHTTP_QUALIFIER = "streamingOkHttp"

/**
 * WebDAV 装配（P2a Hilt→Koin：原 `@Module @InstallIn(SingletonComponent)` + `@Binds`）。
 * 原 `@Qualifier @StreamingOkHttp` 改为 Koin `named("streamingOkHttp")`。
 */
val webdavModule = module {

    singleOf(::WebDavAuthRegistry)

    singleOf(::OkHttpWebDavClient)
    single<WebDavClient> { get<OkHttpWebDavClient>() }

    singleOf(::DiskWebDavAudioCache)
    single<WebDavAudioCache> { get<DiskWebDavAudioCache>() }

    /** 共享限流器：WebDAV 播放 + 刮削全局 4 rps，供 OkHttp 流播与 WebDavClient 双链路共享 */
    single { WebDavRateLimiter() }

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
                if (request.header("X-Muses-Rate-Limited") != null) {
                    return@addInterceptor chain.proceed(request.newBuilder().removeHeader("X-Muses-Rate-Limited").build())
                }
                rateLimiter.acquireBlocking()
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

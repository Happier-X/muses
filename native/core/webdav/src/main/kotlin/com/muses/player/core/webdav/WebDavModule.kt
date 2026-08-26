package com.muses.player.core.webdav

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
internal abstract class WebDavClientModule {

    @Binds
    abstract fun bindWebDavClient(impl: OkHttpWebDavClient): WebDavClient

    @Binds
    abstract fun bindWebDavAudioCache(impl: DiskWebDavAudioCache): WebDavAudioCache

    companion object {
        /** WebDAV 服务器常见于家庭 NAS，超时放宽 */
        @Provides
        @Singleton
        fun provideOkHttpClient(registry: WebDavAuthRegistry): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            // 播放流播统一注入 Basic Auth（interceptor 在 OkHttp IO 线程执行）；
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

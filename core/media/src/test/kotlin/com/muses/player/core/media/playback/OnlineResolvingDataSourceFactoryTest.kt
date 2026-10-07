package com.muses.player.core.media.playback

import android.net.Uri
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.muses.player.core.model.online.OnlinePlayableUrl
import com.muses.player.core.model.online.OnlinePlaybackHttp
import com.muses.player.core.model.online.OnlineTrackRef
import com.muses.player.core.model.online.OnlineTrackResolver
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class OnlineResolvingDataSourceFactoryTest {
    @Test
    fun `在线解析后补齐播放标识并保留范围和原请求头`() {
        val upstream = RecordingDataSource()
        val source = factory(upstream).createDataSource()
        val uri = OnlineTrackRef("wy", "{}", "script").encode()
        source.open(DataSpec.Builder().setUri(uri).setPosition(1024)
            .setHttpRequestHeaders(mapOf("X-Test" to "keep")).build())
        assertEquals("https://cdn.test/audio", upstream.spec.uri.toString())
        assertEquals(1024L, upstream.spec.position)
        assertEquals(mapOf("X-Test" to "keep", "User-Agent" to OnlinePlaybackHttp.USER_AGENT),
            upstream.spec.httpRequestHeaders)
        source.close()
    }

    @Test
    fun `WebDAV地址不添加LX请求标识`() {
        val upstream = RecordingDataSource()
        val source = factory(upstream).createDataSource()
        val headers = mapOf("Authorization" to "Basic test")
        source.open(DataSpec.Builder().setUri("https://dav.test/audio")
            .setHttpRequestHeaders(headers).build())
        assertEquals("https://dav.test/audio", upstream.spec.uri.toString())
        assertEquals(headers, upstream.spec.httpRequestHeaders)
        source.close()
    }

    private fun factory(upstream: RecordingDataSource) = OnlineResolvingDataSourceFactory(
        upstreamFactory = DataSource.Factory { upstream },
        resolver = object : OnlineTrackResolver {
            override suspend fun resolve(ref: OnlineTrackRef) = OnlinePlayableUrl("https://cdn.test/audio")
        },
    )

    private class RecordingDataSource : BaseDataSource(false) {
        lateinit var spec: DataSpec
        override fun open(dataSpec: DataSpec): Long { spec = dataSpec; return 0 }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int = -1
        override fun getUri(): Uri = spec.uri
        override fun close() = Unit
    }
}

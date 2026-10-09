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
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.CancellationException
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

    @Test
    fun `加载线程中断不记为解析失败且保留中断语义`() {
        val entered = CountDownLatch(1)
        val failure = AtomicReference<Throwable>()
        val interrupted = AtomicReference(false)
        val errors = mutableListOf<Throwable?>()
        val source = factory(RecordingDataSource(), resolveTrack = {
            entered.countDown()
            awaitCancellation()
        }, onError = { _, error -> errors += error }).createDataSource()
        val worker = Thread {
            try { source.open(onlineSpec()) }
            catch (e: Throwable) { failure.set(e) }
            finally { interrupted.set(Thread.interrupted()) }
        }
        worker.start()
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            worker.interrupt()
            worker.join(5000)
            assertFalse(worker.isAlive)
            assertTrue(failure.get() is InterruptedIOException)
            assertTrue(failure.get().cause is InterruptedException)
            assertTrue(interrupted.get())
            assertTrue(errors.isEmpty())
        } finally {
            worker.interrupt()
            worker.join(5000)
        }
    }

    @Test
    fun `协程取消不记为音源失败`() {
        val errors = mutableListOf<Throwable?>()
        val source = factory(RecordingDataSource(), resolveTrack = { throw CancellationException("取消") },
            onError = { _, error -> errors += error }).createDataSource()
        try {
            source.open(onlineSpec())
            fail("取消后应停止加载")
        } catch (e: InterruptedIOException) {
            assertTrue(e.cause is CancellationException)
        }
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `真实解析失败仍上报错误`() {
        val failure = IOException("网络失败")
        val errors = mutableListOf<Throwable?>()
        val source = factory(RecordingDataSource(), resolveTrack = { throw failure },
            onError = { _, error -> errors += error }).createDataSource()
        try {
            source.open(onlineSpec())
            fail("解析失败应抛出异常")
        } catch (e: IOException) {
            assertTrue(e.cause is IOException)
            assertEquals("网络失败", e.cause?.message)
        }
        assertEquals(1, errors.size)
        assertTrue(errors.single() is IOException)
        assertEquals("网络失败", errors.single()?.message)
    }

    private fun onlineSpec() = DataSpec.Builder().setUri(OnlineTrackRef("wy", "{}", "script").encode()).build()

    private fun factory(upstream: RecordingDataSource,
        resolveTrack: suspend (OnlineTrackRef) -> OnlinePlayableUrl = { OnlinePlayableUrl("https://cdn.test/audio") },
        onError: (String, Throwable?) -> Unit = { _, _ -> }) = OnlineResolvingDataSourceFactory(
        upstreamFactory = DataSource.Factory { upstream },
        resolver = object : OnlineTrackResolver {
            override suspend fun resolve(ref: OnlineTrackRef) = resolveTrack(ref)
        },
        onResolveError = onError,
    )

    private class RecordingDataSource : BaseDataSource(false) {
        lateinit var spec: DataSpec
        override fun open(dataSpec: DataSpec): Long { spec = dataSpec; return 0 }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int = -1
        override fun getUri(): Uri = spec.uri
        override fun close() = Unit
    }
}

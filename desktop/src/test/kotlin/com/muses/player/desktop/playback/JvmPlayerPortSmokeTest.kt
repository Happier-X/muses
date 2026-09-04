package com.muses.player.desktop.playback

import com.muses.player.core.data.repository.PlaybackStateRepository
import com.muses.player.core.data.repository.RecentPlaysRepository
import com.muses.player.core.data.store.createDataStore
import com.muses.player.core.model.SourceType
import com.muses.player.desktop.cache.DesktopWebDavAudioCache
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume
import org.junit.Test
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * S2 VLCJ 实播冒烟（需随包 VLC 原生库 + 测试音频，缺失时自动跳过，不阻塞 CI）。
 *
 * 前置：
 * - `MUSES_VLC_DIR` 指向 VLC 便携目录（含 libvlc.dll；缺失则跳过）；
 * - `MUSES_SMOKE_MEDIA` 指向可播音频绝对路径（缺失则跳过）。
 *
 * 覆盖：播放→playing/进度推进→暂停→暂停态 seek 落点→音量。
 */
class JvmPlayerPortSmokeTest {

    @Test
    fun 前台播放闭环冒烟() = runBlocking {
        val vlcDir = System.getenv("MUSES_VLC_DIR")?.takeIf { it.isNotBlank() } ?: run {
            println("SKIP smoke: MUSES_VLC_DIR 未设置"); return@runBlocking
        }
        val media = System.getenv("MUSES_SMOKE_MEDIA")?.takeIf { it.isNotBlank() } ?: run {
            println("SKIP smoke: MUSES_SMOKE_MEDIA 未设置"); return@runBlocking
        }
        Assume.assumeTrue(File(vlcDir, "libvlc.dll").exists())
        Assume.assumeTrue(File(media).exists())

        val tmp = Files.createTempDirectory("muses-smoke").toFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            // 隔离 DataStore：JvmPlayerPort.createDefault 用真实路径，此处直构内存无关双仓
            val dsFile = File(tmp, "smoke.preferences_pb").absolutePath
            System.setProperty("muses.smoke.ds", dsFile)
            val store = createDataStore("smoke-${System.nanoTime()}.preferences_pb")
            val ref = JvmPlayerPort.SongRef(
                id = "s1", sourceId = "src", path = File(media).absolutePath,
                title = "冒烟", artist = null, album = null, coverUri = null,
                sourceType = SourceType.LOCAL,
            )
            System.setProperty("jna.library.path", vlcDir)
            val port = JvmPlayerPort(
                songLookup = { id -> if (id == "s1") ref else null },
                playbackStateRepository = PlaybackStateRepository(store),
                recentPlaysRepository = RecentPlaysRepository(store),
                audioCache = DesktopWebDavAudioCache(File(tmp, "cache")),
                errorLog = { tag, msg, _ -> println("LOG $tag $msg") },
                scope = scope,
                factoryProvider = { MediaPlayerFactory("--no-video", "--aout=directsound") },
            )
            try {
                // DataStore 真实路径在 APPDATA；冒烟只验证播放闭环，不验证持久化落盘位置
                port.enqueue(listOf("s1"), 0)
                val gotPlaying = withTimeoutOrNull(20_000) {
                    while (!(port.playbackState.value == JvmPlaybackStates.STATE_READY && port.isPlaying.value)) {
                        if (port.playbackError.value != null) break
                        delay(300)
                    }
                    port.isPlaying.value && port.playbackError.value == null
                } ?: false
                assertTrue(gotPlaying, "应进入播放中，err=${port.playbackError.value}")
                delay(1500)
                assertTrue(port.positionMs.value > 0, "进度应推进")
                assertTrue(port.durationMs.value > 0, "时长应解析")
                port.pause()
                delay(800)
                assertEquals(false, port.isPlaying.value)
                port.seekTo(5000L)
                delay(2000)
                val errMs = kotlin.math.abs(port.positionMs.value - 5000L)
                assertTrue(errMs <= 1500, "暂停态 seek 落点误差应≤1.5s，实际 err=$errMs pos=${port.positionMs.value}")
                port.setVolume(50)
                assertEquals(50, port.volume.value)
            } finally {
                port.release()
            }
        } finally {
            scope.cancel()
            tmp.deleteRecursively()
        }
    }
}

package com.muses.player.desktop.playback

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * VLC 原生库目录发现单测（`JvmPlayerPort.resolveVlcDir`）。
 *
 * 覆盖 S4 随包内置 VLC 的两条新增路径（`compose.application.resources.dir/vlc`）与既有显式配置路径，
 * 以及「目录存在但不含 libvlc.dll 时必须跳过」的判定口径（否则 VLCJ 会拿到无效目录后报链接错）。
 *
 * 不覆盖：Windows 注册表 InstallDir 与 %ProgramFiles% 约定路径——它们读真实机器状态，
 * 断言会随开发机是否装 VLC 而漂移；该分支由 `installedVlcDirs()` 的 isWindows + runCatching 兜底。
 */
class VlcNativeDiscoveryTest {

    private val touchedProperties = mutableListOf<String>()

    @AfterTest
    fun 清理系统属性() {
        touchedProperties.forEach { System.clearProperty(it) }
        touchedProperties.clear()
    }

    private fun setProperty(key: String, value: String?) {
        touchedProperties += key
        if (value == null) System.clearProperty(key) else System.setProperty(key, value)
    }

    /** 造一个「看起来像 VLC 目录」的临时目录（含空的 libvlc.dll）。 */
    private fun fakeVlcDir(prefix: String): File {
        val dir = Files.createTempDirectory(prefix).toFile()
        File(dir, "libvlc.dll").writeText("stub")
        dir.deleteOnExit()
        return dir
    }

    @Test
    fun 显式目录优先且命中含dll的目录() {
        val dir = fakeVlcDir("muses-vlc-explicit")
        setProperty("muses.vlc.dir", dir.absolutePath)

        assertEquals(dir.canonicalFile, JvmPlayerPort.resolveVlcDir()?.canonicalFile)
    }

    @Test
    fun 随包内置目录经resources属性命中() {
        // jpackage 把 jvmArgs -Dcompose.application.resources.dir=$APPDIR/resources 注入运行期，
        // 内置 VLC 位于其下的 vlc/ 子目录（打包链路见 composeApp/build.gradle.kts prepareVlcRuntime）
        val resources = Files.createTempDirectory("muses-vlc-resources").toFile()
        val vlc = File(resources, "vlc").apply { mkdirs() }
        File(vlc, "libvlc.dll").writeText("stub")
        resources.deleteOnExit()
        setProperty("compose.application.resources.dir", resources.absolutePath)
        setProperty("muses.vlc.dir", null)

        assertEquals(vlc.canonicalFile, JvmPlayerPort.resolveVlcDir()?.canonicalFile)
    }

    @Test
    fun 内置目录优先于仓库便携版() {
        val resources = Files.createTempDirectory("muses-vlc-prio").toFile()
        val vlc = File(resources, "vlc").apply { mkdirs() }
        File(vlc, "libvlc.dll").writeText("stub")
        resources.deleteOnExit()
        setProperty("compose.application.resources.dir", resources.absolutePath)
        setProperty("muses.vlc.dir", null)

        val candidates = JvmPlayerPort.vlcDirCandidates().map { it.canonicalFile }
        val bundledIdx = candidates.indexOf(vlc.canonicalFile)
        val repoIdx = candidates.indexOfFirst { it.path.replace('\\', '/').endsWith("spike-vlcj/vlc-portable/vlc-3.0.21") }

        assertEquals(0, bundledIdx, "内置目录应排在候选表最前（显式配置未设置时）")
        if (repoIdx >= 0) {
            assertTrue(bundledIdx < repoIdx, "内置目录必须先于仓库便携版被检查")
        }
    }

    @Test
    fun 目录缺libvlc_dll时跳过() {
        // 常见误配：MUSES_VLC_DIR 指到 VLC 上层目录或其 plugins 目录
        val notVlc = Files.createTempDirectory("muses-vlc-bad").toFile()
        notVlc.deleteOnExit()
        setProperty("muses.vlc.dir", notVlc.absolutePath)
        setProperty("compose.application.resources.dir", null)

        assertNotEquals(notVlc.canonicalFile, JvmPlayerPort.resolveVlcDir()?.canonicalFile)
    }

    @Test
    fun 候选表首项为显式配置() {
        val dir = fakeVlcDir("muses-vlc-order")
        setProperty("muses.vlc.dir", dir.absolutePath)

        assertEquals(dir.canonicalFile, JvmPlayerPort.vlcDirCandidates().first().canonicalFile)
    }
}

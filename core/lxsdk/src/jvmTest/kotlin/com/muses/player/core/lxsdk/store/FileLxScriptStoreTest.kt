package com.muses.player.core.lxsdk.store

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** [FileLxScriptStore] 测试：脚本落盘、启禁用、删除、元信息解析 */
class FileLxScriptStoreTest {

    private val tempDir: File = Files.createTempDirectory("muses-lxstore-test").toFile()
    private val store = FileLxScriptStore(rootDir = tempDir)

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun script(name: String, version: String = "1.0.0") = """
        /**
         * @name $name
         * @version $version
         * @author 测试作者
         */
        const a = 1;
    """.trimIndent()

    @Test
    fun `空目录返回空列表`() {
        assertTrue(store.list().isEmpty())
    }

    @Test
    fun `保存后可从列表读回`() {
        val saved = store.save("kw-source", script("酷我源"))
        assertEquals("kw-source", saved.id)
        assertEquals("酷我源", saved.name)
        assertEquals("1.0.0", saved.meta.version)
        assertTrue(saved.enabled)

        val list = store.list()
        assertEquals(1, list.size)
        assertEquals("酷我源", list.first().name)
        // 原文完整保留
        assertTrue(list.first().source.contains("const a = 1;"))
    }

    @Test
    fun `按 id 读取`() {
        store.save("s1", script("源一"))
        assertNotNull(store.get("s1"))
        assertNull(store.get("不存在"))
    }

    @Test
    fun `元信息缺失时以 id 作名称兜底`() {
        val saved = store.save("no-meta", "const x = 1;")
        assertEquals("no-meta", saved.name)
        assertNull(saved.meta.name)
    }

    @Test
    fun `覆盖保存同一 id 更新内容`() {
        store.save("s1", script("旧名", version = "1.0.0"))
        store.save("s1", script("新名", version = "2.0.0"))
        val list = store.list()
        assertEquals(1, list.size, "同 id 应覆盖而非新增")
        assertEquals("新名", list.first().name)
        assertEquals("2.0.0", list.first().meta.version)
    }

    @Test
    fun `禁用与启用`() {
        store.save("s1", script("源一"))
        assertTrue(store.list().first().enabled)

        assertTrue(store.setEnabled("s1", false))
        assertFalse(store.list().first().enabled, "禁用后 enabled 应为 false")

        assertTrue(store.setEnabled("s1", true))
        assertTrue(store.list().first().enabled)
    }

    @Test
    fun `禁用后原文仍保留`() {
        store.save("s1", script("源一"))
        store.setEnabled("s1", false)
        // 禁用只加标记文件，不删原文
        assertTrue(store.get("s1")!!.source.contains("const a = 1;"))
    }

    @Test
    fun `对不存在的脚本启禁用返回 false`() {
        assertFalse(store.setEnabled("不存在", false))
    }

    @Test
    fun `删除脚本`() {
        store.save("s1", script("源一"))
        assertTrue(store.delete("s1"))
        assertTrue(store.list().isEmpty())
        assertNull(store.get("s1"))
    }

    @Test
    fun `删除不存在的脚本返回 false`() {
        assertFalse(store.delete("不存在"))
    }

    @Test
    fun `删除已禁用脚本会一并清掉标记文件`() {
        store.save("s1", script("源一"))
        store.setEnabled("s1", false)
        assertTrue(store.delete("s1"))
        // 标记文件不应残留（否则同名重导入会继承禁用状态）
        val leftovers = tempDir.listFiles()?.map { it.name } ?: emptyList()
        assertTrue(leftovers.none { it.endsWith(".disabled") }, "残留：$leftovers")
    }

    @Test
    fun `多脚本按导入时间排序`() {
        store.save("a", script("甲"))
        Thread.sleep(15) // 文件时间戳粒度
        store.save("b", script("乙"))
        val list = store.list()
        assertEquals(listOf("a", "b"), list.map { it.id })
    }

    @Test
    fun `中文与 emoji 脚本内容无损落盘`() {
        val content = script("中文源🎵")
        store.save("cn", content)
        assertEquals(content, store.get("cn")!!.source)
    }

    @Test
    fun `根目录不存在时自动创建`() {
        val nested = File(tempDir, "a/b/c")
        val nestedStore = FileLxScriptStore(rootDir = nested)
        nestedStore.save("s1", script("源一"))
        assertTrue(nested.isDirectory)
        assertEquals(1, nestedStore.list().size)
    }
}
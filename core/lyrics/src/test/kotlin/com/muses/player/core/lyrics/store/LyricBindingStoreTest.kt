package com.muses.player.core.lyrics.store

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * LyricBindingStore 存储格式冻结用例（任务 09-05-lyrics-kmp X1）。
 *
 * 样本 = 旧 Android org.json 实现的写入语义：字段 put 顺序 source/provider/resourceValue/title/artist/
 * durationMs、紧凑输出、provider 为 null 时写 ""、读取缺省字段按 opt 语义回退（provider→null、
 * title/artist→""、durationMs→0）。存储文件名固定 muses_lyric_bindings.json，绑定 key = stableKey()。
 */
class LyricBindingStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val store get() = LyricBindingStore(File(tempFolder.root, "bindings"))

    private val storeFile get() = File(tempFolder.root, "bindings/muses_lyric_bindings.json")

    /** 冻结样本：旧 org.json 写入格式（provider=null → ""，紧凑无空格，六字段固定顺序）。 */
    private val legacyEntryJson =
        """{"source":"AmlL","provider":"","resourceValue":"res-1","title":"晴天","artist":"周杰伦","durationMs":262066}"""

    @Test
    fun `write 序列化与旧 org json 格式逐字节冻结`() {
        val binding = LyricBinding(
            source = BoundLyricSource.AmlL,
            provider = null,
            resourceValue = "res-1",
            title = "晴天",
            artist = "周杰伦",
            durationMs = 262066,
        )
        store.write(binding.stableKey(), binding)

        val entry = Json.parseToJsonElement(storeFile.readText()).jsonObject
        assertEquals(1, entry.size)
        assertEquals(legacyEntryJson, entry.getValue("AmlL::res-1").toString())
    }

    @Test
    fun `write 转义字段 roundtrip 等值且转义形式冻结`() {
        val binding = LyricBinding(
            source = BoundLyricSource.Provider,
            provider = "kw",
            resourceValue = """res/"with\quote""",
            title = "Ti\"tle\\x",
            artist = "Art`ist",
            durationMs = 1,
        )
        val key = binding.stableKey()
        store.write(key, binding)

        assertEquals(binding, store.read(key))
        // org.json 与 kotlinx.serialization 的必需转义一致：" → \"、\ → \\
        assertTrue(storeFile.readText().contains("""res/\"with\\quote"""))
    }

    @Test
    fun `read 解析冻结样本文件`() {
        writeFile(
            """{"Provider:kw:abc":{"source":"Provider","provider":"kw","resourceValue":"abc",""" +
                """"title":"T","artist":"A","durationMs":123}}""",
        )
        assertEquals(
            LyricBinding(BoundLyricSource.Provider, "kw", "abc", "T", "A", 123),
            store.read("Provider:kw:abc"),
        )
    }

    @Test
    fun `read 缺省字段按旧 opt 语义回退`() {
        // 缺 provider/title/artist/durationMs → null/""/""/0
        writeFile("""{"k":{"source":"AmlL","resourceValue":"x"}}""")
        assertEquals(
            LyricBinding(BoundLyricSource.AmlL, null, "x", "", "", 0),
            store.read("k"),
        )
        // 显式空 provider 视为未绑定（null）
        writeFile("""{"k":{"source":"AmlL","provider":"","resourceValue":"x"}}""")
        assertNull(store.read("k")?.provider)
    }

    @Test
    fun `read 损坏数据返回 null 不抛`() {
        // 整文件损坏
        writeFile("not a json")
        assertNull(store.read("k"))
        // 未知 source 枚举
        writeFile("""{"k":{"source":"Other","resourceValue":"x"}}""")
        assertNull(store.read("k"))
        // entry 非对象
        writeFile("""{"k":"text"}""")
        assertNull(store.read("k"))
        // 缺必填字段
        writeFile("""{"k":{"source":"AmlL"}}""")
        assertNull(store.read("k"))
    }

    @Test
    fun `read 无文件返回 null`() {
        assertNull(store.read("anything"))
    }

    @Test
    fun `write 覆盖同名 key 且多 key 共存`() {
        val first = LyricBinding(BoundLyricSource.AmlL, null, "r1", "t1", "a1", 10)
        val second = LyricBinding(BoundLyricSource.Provider, "kw", "r2", "t2", "a2", 20)
        val updated = first.copy(title = "t1-new", durationMs = 99)
        store.write(first.stableKey(), first)
        store.write(second.stableKey(), second)
        store.write(first.stableKey(), updated)

        assertEquals(updated, store.read(first.stableKey()))
        assertEquals(second, store.read(second.stableKey()))
        assertEquals(2, Json.parseToJsonElement(storeFile.readText()).jsonObject.size)
    }

    @Test
    fun `clear 清空全部绑定`() {
        val binding = LyricBinding(BoundLyricSource.AmlL, null, "r1", "t", "a", 1)
        store.write(binding.stableKey(), binding)
        store.clear()

        assertNull(store.read(binding.stableKey()))
        assertEquals("{}", storeFile.readText())
    }

    private fun writeFile(text: String) {
        storeFile.parentFile?.mkdirs()
        storeFile.writeText(text)
    }
}

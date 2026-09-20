package com.muses.player.core.lxsdk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** [LxQuality] 档位定义测试：key 必须与脚本 qualitys 字面量一致 */
class LxQualityTest {

    @Test
    fun `覆盖脚本实际声明的全部档位`() {
        // 实测「星海音乐源」声明：wy/kg/kw 含 hires/atmos/master，tx 另有 192k/atmos_plus
        val declaredByRealScript = listOf(
            "128k", "192k", "320k", "flac", "flac24bit",
            "hires", "atmos", "atmos_plus", "master",
        )
        declaredByRealScript.forEach { key ->
            assertTrue(LxQuality.fromKey(key) != null, "缺少档位：$key")
        }
        assertEquals(declaredByRealScript.size, LxQuality.entries.size)
    }

    @Test
    fun `key 与洛雪规范字面量一致`() {
        assertEquals("128k", LxQuality.Q_128K.key)
        assertEquals("192k", LxQuality.Q_192K.key)
        assertEquals("320k", LxQuality.Q_320K.key)
        assertEquals("flac", LxQuality.FLAC.key)
        assertEquals("flac24bit", LxQuality.FLAC_24BIT.key)
        assertEquals("hires", LxQuality.HIRES.key)
        assertEquals("atmos", LxQuality.ATMOS.key)
        assertEquals("atmos_plus", LxQuality.ATMOS_PLUS.key)
        assertEquals("master", LxQuality.MASTER.key)
    }

    @Test
    fun `未知 key 返回 null`() {
        assertNull(LxQuality.fromKey("unknown"))
        assertNull(LxQuality.fromKey(""))
        assertNull(LxQuality.fromKey("FLAC")) // 大小写敏感
    }

    @Test
    fun `默认档位为 320k 且属安全档`() {
        assertEquals(LxQuality.Q_320K, LxQuality.DEFAULT)
        assertFalse(LxQuality.DEFAULT.isHighTier)
    }

    @Test
    fun `高音质档标记正确`() {
        assertFalse(LxQuality.Q_128K.isHighTier)
        assertFalse(LxQuality.Q_320K.isHighTier)
        assertTrue(LxQuality.FLAC.isHighTier)
        assertTrue(LxQuality.FLAC_24BIT.isHighTier)
        assertTrue(LxQuality.HIRES.isHighTier)
        assertTrue(LxQuality.ATMOS.isHighTier)
        assertTrue(LxQuality.MASTER.isHighTier)
    }

    @Test
    fun `ordered 按档位升序`() {
        val ordered = LxQuality.ordered()
        assertEquals(LxQuality.Q_128K, ordered.first())
        assertEquals(LxQuality.MASTER, ordered.last())
        assertEquals(ordered, ordered.sortedBy { it.rank })
    }
}

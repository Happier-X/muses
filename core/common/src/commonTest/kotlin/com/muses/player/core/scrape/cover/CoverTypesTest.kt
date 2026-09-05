package com.muses.player.core.scrape.cover

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

// 规格 = src/features/cover/types.ts 类型定义与 wire 值

class CoverTypesTest {

    /** 供其他测试复用：按 CoverMatcher.buildQueryKey 同规则构造 queryKey */
    object Dummy {
        private val json = Json
        private val stringListSerializer = ListSerializer(String.serializer())

        fun key(query: OnlineCoverQuery): String =
            json.encodeToString(
                stringListSerializer,
                listOf(
                    query.title.trim(),
                    query.artist?.trim().orEmpty(),
                    query.album?.trim().orEmpty(),
                ),
            )
    }

    @Test
    fun `wire值对齐Web字符串`() {
        assertEquals("itunes", OnlineCoverSource.ITUNES.wire)
        assertEquals("kw", OnlineCoverSource.KW.wire)
        assertEquals("mg", OnlineCoverSource.MG.wire)
        assertEquals("kg", OnlineCoverSource.KG.wire)
        assertEquals("tx", OnlineCoverSource.TX.wire)
        assertEquals("wy", OnlineCoverSource.WY.wire)

        assertEquals("no-match", OnlineCoverMatchFailReason.NO_MATCH.wire)
        assertEquals("network", OnlineCoverMatchFailReason.NETWORK.wire)
        assertEquals("aborted", OnlineCoverMatchFailReason.ABORTED.wire)
    }

    @Test
    fun `queryKey构造对齐match_ts的buildQueryKey`() {
        val q = OnlineCoverQuery(songId = "s1", title = " Title ", artist = " A ", album = null)
        // JSON.stringify([title.trim(), artist?.trim() || '', album?.trim() || ''])
        assertEquals("""["Title","A",""]""", Dummy.key(q))
    }
}

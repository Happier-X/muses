package com.muses.player.core.scrape.cover

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

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

    @Suppress("JUnitTestAnnotation")
    @org.junit.Test
    fun `wire值对齐Web字符串`() {
        org.junit.Assert.assertEquals("itunes", OnlineCoverSource.ITUNES.wire)
        org.junit.Assert.assertEquals("kw", OnlineCoverSource.KW.wire)
        org.junit.Assert.assertEquals("mg", OnlineCoverSource.MG.wire)
        org.junit.Assert.assertEquals("kg", OnlineCoverSource.KG.wire)
        org.junit.Assert.assertEquals("tx", OnlineCoverSource.TX.wire)
        org.junit.Assert.assertEquals("wy", OnlineCoverSource.WY.wire)

        org.junit.Assert.assertEquals("no-match", OnlineCoverMatchFailReason.NO_MATCH.wire)
        org.junit.Assert.assertEquals("network", OnlineCoverMatchFailReason.NETWORK.wire)
        org.junit.Assert.assertEquals("aborted", OnlineCoverMatchFailReason.ABORTED.wire)
    }

    @org.junit.Test
    fun `queryKey构造对齐match_ts的buildQueryKey`() {
        val q = OnlineCoverQuery(songId = "s1", title = " Title ", artist = " A ", album = null)
        // JSON.stringify([title.trim(), artist?.trim() || '', album?.trim() || ''])
        org.junit.Assert.assertEquals("""["Title","A",""]""", Dummy.key(q))
    }
}

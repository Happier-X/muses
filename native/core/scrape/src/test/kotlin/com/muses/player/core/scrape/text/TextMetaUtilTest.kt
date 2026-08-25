package com.muses.player.core.scrape.text

import com.muses.player.core.model.scrape.MetaFieldSource
import com.muses.player.core.model.scrape.MetaSources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 规格 = src/features/metadata/util.ts（needsOnlineTextMeta / titlesRelated / isWeakTitle 等） */
class TextMetaUtilTest {

    // ── isWeakTitle 边界 ────────────────────────────────────

    @Test
    fun `isWeakTitle 与去扩展名文件名 normalize 后相等`() {
        assertTrue(isWeakTitle("Shape of You", "/music/Ed Sheeran/Shape of You.mp3"))
    }

    @Test
    fun `isWeakTitle 大小写与全半角差异仍算弱`() {
        // NFKC + 小写后相等
        assertTrue(isWeakTitle("ＬＯＶＥ Story", "C:\\music\\love story.flac"))
    }

    @Test
    fun `isWeakTitle 非弱标题返回 false`() {
        assertFalse(isWeakTitle("Real Title", "/music/shape_of_you.mp3"))
    }

    @Test
    fun `isWeakTitle title 或 path 为空白返回 false`() {
        assertFalse(isWeakTitle("  ", "/music/a.mp3"))
        assertFalse(isWeakTitle("a", "  "))
    }

    @Test
    fun `隐藏文件名不去扩展名但仍按原样比较`() {
        // getTitleFromPath 仅在 extensionIndex > 0 时去扩展名：".mp3" 基名保持 ".mp3"
        assertTrue(isWeakTitle(".mp3", "/music/.mp3"))
        // path 尾部分隔符 → 最后一段为 music，与 abc 不等 → 非弱
        assertFalse(isWeakTitle("abc", "/music/"))
    }

    // ── titlesRelated ───────────────────────────────────────

    @Test
    fun `titlesRelated 相等或互相包含`() {
        assertTrue(titlesRelated("Love Story", "love story"))
        assertTrue(titlesRelated("Love Story (Live)", "love story"))
        assertTrue(titlesRelated("love", "love story"))
        assertFalse(titlesRelated("love", "hate"))
        assertFalse(titlesRelated(null, "x"))
        assertFalse(titlesRelated("x", ""))
    }

    // ── needsOnlineTextMeta 各缺口分支 ──────────────────────

    private val base = OnlineTextNeedQuery(title = "T")

    @Test
    fun `空 artist 或空 album 需要补缺`() {
        assertTrue(needsOnlineTextMeta(base.copy(artist = null)))
        assertTrue(needsOnlineTextMeta(base.copy(artist = "", album = "")))
        // 空白字符串按 isBlank 判定
        assertTrue(needsOnlineTextMeta(base.copy(album = "   ")))
    }

    @Test
    fun `三字段齐备且非弱title不需要补缺`() {
        val q = OnlineTextNeedQuery(title = "T", artist = "A", album = "B")
        assertFalse(needsOnlineTextMeta(q))
    }

    @Test
    fun `弱title需要补缺`() {
        val q = OnlineTextNeedQuery(
            title = "Shape of You",
            artist = "Ed Sheeran",
            album = "÷",
            path = "/music/Shape of You.mp3",
        )
        assertTrue(needsOnlineTextMeta(q))
    }

    @Test
    fun `手改字段不参与缺口判定`() {
        // artist 手改：即使为空也不算缺口（album 已齐备以隔离变量）
        val q1 = OnlineTextNeedQuery(title = "T", artist = "", album = "B", userEditedFields = listOf("artist"))
        assertFalse(needsOnlineTextMeta(q1))
        // 三字段全手改早退
        val q2 = OnlineTextNeedQuery(
            title = "T",
            artist = "",
            album = "",
            userEditedFields = listOf("title", "artist", "album"),
        )
        assertFalse(needsOnlineTextMeta(q2))
    }

    @Test
    fun `cloud来源弱title需齐备duration与artist才再补_R4-2`() {
        // cloud 弱 title + 无 duration → 阻断
        // artist/album 齐备以隔离变量（空白字段会独立触发缺口）
        val blockedNoDuration = OnlineTextNeedQuery(
            title = "Shape of You",
            path = "/music/Shape of You.mp3",
            artist = "Ed Sheeran",
            album = "B",
            metaSources = MetaSources(title = MetaFieldSource.CLOUD),
        )
        assertFalse(needsOnlineTextMeta(blockedNoDuration))

        // cloud 弱 title + duration=0 视同缺失（JS !duration）
        val blockedZeroDuration = blockedNoDuration.copy(durationSec = 0.0)
        assertFalse(needsOnlineTextMeta(blockedZeroDuration))

        // cloud 弱 title + duration 齐备但 artist 空 → 仍阻断（artist 手改保护以隔离缺口）
        val blockedNoArtist = blockedNoDuration.copy(
            durationSec = 200.0,
            artist = "",
            userEditedFields = listOf("artist"),
        )
        assertFalse(needsOnlineTextMeta(blockedNoArtist))

        // cloud 弱 title + duration + artist 齐备 → 放行再补
        val allowed = blockedNoDuration.copy(durationSec = 200.0, artist = "Ed Sheeran")
        assertTrue(needsOnlineTextMeta(allowed))

        // 非 cloud 来源的弱 title 不受约束
        val embeddedTitle = OnlineTextNeedQuery(
            title = "Shape of You",
            path = "/music/Shape of You.mp3",
            metaSources = MetaSources(title = MetaFieldSource.EMBEDDED),
        )
        assertTrue(needsOnlineTextMeta(embeddedTitle))
    }

    // ── buildKeyword / hitFillsMissing ─────────────────────

    @Test
    fun `buildKeyword 过滤空白并以空格连接`() {
        assertEquals("T A B", buildKeyword(FakeQueries.query("T", "A", "B")))
        assertEquals("T", buildKeyword(FakeQueries.query("T", "", null)))
    }

    @Test
    fun `hitFillsMissing 补空字段或相关弱title命中`() {
        val needArtist = OnlineTextNeedQuery(title = "T", artist = "")
        assertTrue(hitFillsMissing(FakeHits.hit(title = null, artist = "A"), needArtist))

        // 已有 artist 的命中对「无需补」的查询无用
        val full = OnlineTextNeedQuery(title = "T", artist = "A", album = "B")
        assertFalse(hitFillsMissing(FakeHits.hit(title = "Other", artist = "X"), full))

        // 弱 title + 相关命中 title
        val weak = OnlineTextNeedQuery(title = "Shape of You", path = "/music/Shape of You.mp3")
        assertTrue(hitFillsMissing(FakeHits.hit(title = "Shape Of You (Live)"), weak))
        assertFalse(hitFillsMissing(FakeHits.hit(title = "Unrelated"), weak))
    }

    // ── mergeTextMetaFillEmpty ──────────────────────────────

    @Test
    fun `merge 仅补空 artist 与 album`() {
        val latest = TextMetaMergeInput(title = "T", artist = "", album = "Keep")
        val outcome = mergeTextMetaFillEmpty(latest, FakeHits.hit(title = null, artist = "New A", album = "New B"))
        assertTrue(outcome.changed)
        assertEquals("New A", outcome.next.artist)
        // album 非空不覆盖
        assertEquals("Keep", outcome.next.album)
    }

    @Test
    fun `merge 弱title可被相关hit改写`() {
        val latest = TextMetaMergeInput(title = "shape of you", path = "/music/shape of you.mp3")
        val outcome = mergeTextMetaFillEmpty(latest, FakeHits.hit(title = "Shape of You"))
        assertTrue(outcome.changed)
        assertEquals("Shape of You", outcome.next.title)

        // 不相关的 hit.title 不改写
        val unchanged = mergeTextMetaFillEmpty(latest, FakeHits.hit(title = "Whatever"))
        assertFalse(unchanged.changed)
    }

    @Test
    fun `merge 手改title保护`() {
        val latest = TextMetaMergeInput(
            title = "shape of you",
            path = "/music/shape of you.mp3",
            userEditedFields = listOf("title"),
        )
        val outcome = mergeTextMetaFillEmpty(latest, FakeHits.hit(title = "Shape of You"))
        assertFalse(outcome.changed)
        assertEquals("shape of you", outcome.next.title)
    }

    // ── pickBestHit 打分排序 ───────────────────────────────

    @Test
    fun `pickBestHit 按 scoreTextHit 取最高分`() {
        val query = FakeQueries.query("Love Story", "Taylor Swift", "Fearless")
        val exact = FakeHits.hit(title = "Love Story", artist = "Taylor Swift", album = "Fearless")
        val partial = FakeHits.hit(title = "Love Story", artist = "Someone Else")
        val best = pickBestHit(listOf(partial, exact), query)
        assertEquals(exact, best)
    }

    @Test
    fun `pickBestHit 空列表返回 null`() {
        assertNull(pickBestHit(emptyList(), FakeQueries.query("T")))
    }
}

private object FakeQueries {
    fun query(title: String?, artist: String? = null, album: String? = null) =
        com.muses.player.core.model.scrape.OnlineTextQuery(
            songId = "s1",
            title = title ?: "",
            artist = artist,
            album = album,
        )
}

private object FakeHits {
    fun hit(title: String? = null, artist: String? = null, album: String? = null) =
        com.muses.player.core.model.scrape.TextMetaHit(
            title = title,
            artist = artist,
            album = album,
            source = com.muses.player.core.model.scrape.OnlineTextSource.KW,
        )
}

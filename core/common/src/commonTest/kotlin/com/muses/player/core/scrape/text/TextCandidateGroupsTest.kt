package com.muses.player.core.scrape.text

import com.muses.player.core.model.scrape.*
import kotlin.test.*

class TextCandidateGroupsTest {
    private fun hit(title: String = "曲名", artist: String? = "歌手", album: String? = "专辑", duration: Double? = 180.0,
        source: OnlineTextSource = OnlineTextSource.WY, recording: String? = null, release: String? = null) =
        TextMetaHit(title, artist, album, source, duration, recording, release)

    @Test fun `跨来源录音证据接近归组且不同专辑分别保留发行`() {
        val groups = groupTextCandidates(listOf(hit(), hit(source = OnlineTextSource.ITUNES, duration = 181.0),
            hit(source = OnlineTextSource.MUSICBRAINZ, album = "另一张专辑")))
        assertEquals(listOf(0, 1, 2), groups.single().candidateIndices)
        assertEquals(listOf(listOf(0, 1), listOf(2)), groups.single().releases.map { it.candidateIndices })
    }

    @Test fun `分组保留非连续原始索引且不删除重复来源`() {
        val groups = groupTextCandidates(listOf(hit(), hit(artist = "另一歌手"), hit(source = OnlineTextSource.KW)))
        assertEquals(listOf(listOf(0, 2), listOf(1)), groups.map { it.candidateIndices })
        assertEquals(listOf(0, 2), groups.first().releases.single().candidateIndices)
    }

    @Test fun `同名异人近似歌手与独唱合唱不混组`() {
        for ((a, b) in listOf("王菲" to "王菲菲", "甲" to "甲、乙", "甲" to "乙")) {
            assertEquals(2, groupTextCandidates(listOf(hit(artist = a), hit(artist = b))).size)
        }
    }

    @Test fun `缺失歌手或时长不能仅凭同名同专辑合并`() {
        for (a in listOf(hit(artist = null), hit(artist = " "), hit(duration = null), hit(title = ""))) {
            assertEquals(2, groupTextCandidates(listOf(a, hit())).size)
        }
        assertEquals(2, groupTextCandidates(listOf(hit(duration = null), hit(duration = null))).size)
    }

    @Test fun `真实录音ID可补缺失证据但不覆盖明确冲突`() {
        val a = hit(recording = "musicbrainz:录音")
        assertEquals(1, groupTextCandidates(listOf(a, hit(duration = null, recording = "musicbrainz:录音"))).size)
        for (b in listOf(hit(artist = "其他歌手"), hit(title = "曲名 (Live)"), hit(duration = 210.0))) {
            assertEquals(2, groupTextCandidates(listOf(a, b.copy(recordingId = a.recordingId))).size)
        }
    }

    @Test fun `不同版本年份地点及普通歌名保持独立`() {
        for ((a, b) in listOf("曲名" to "曲名 (Live)", "曲名 (Live)" to "曲名 (Remix)",
            "曲名 (2020 Remaster)" to "曲名 (2024 Remaster)",
            "曲名 (Live at Wembley)" to "曲名 (Live at Tokyo)", "Live Forever" to "Forever")) {
            assertEquals(2, groupTextCandidates(listOf(hit(title = a), hit(title = b))).size)
        }
    }

    @Test fun `时长近似不形成传递闭包`() {
        val groups = groupTextCandidates(listOf(hit(duration = 180.0), hit(duration = 183.0), hit(duration = 186.0)))
        assertEquals(listOf(listOf(0, 1), listOf(2)), groups.map { it.candidateIndices })
    }

    @Test fun `无ID候选不桥接两个冲突录音ID`() {
        val groups = groupTextCandidates(listOf(hit(recording = "musicbrainz:甲"), hit(), hit(recording = "musicbrainz:乙")))
        assertEquals(listOf(listOf(0, 1), listOf(2)), groups.map { it.candidateIndices })
        // 不同命名空间的相同尾部不能当作相同录音的强证据。
        assertEquals(2, groupTextCandidates(listOf(hit(recording = "wy:123", duration = null),
            hit(recording = "musicbrainz:123", duration = null))).size)
    }

    @Test fun `发行ID冲突不桥接而空专辑独立保留`() {
        val releases = groupTextCandidates(listOf(hit(release = "musicbrainz:甲"), hit(),
            hit(release = "musicbrainz:乙"), hit(album = null), hit(album = null))).single().releases
        assertEquals(listOf(listOf(0, 1), listOf(2), listOf(3), listOf(4)), releases.map { it.candidateIndices })
    }

    @Test fun `无效时长不能参与合并或提供身份得分`() {
        for (duration in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertEquals(2, groupTextCandidates(listOf(hit(duration = duration), hit())).size)
            val evidence = evaluateTextMatch(hit(duration = duration), OnlineTextQuery("曲", "曲名", artist = "歌手", durationSec = 180.0))
            assertTrue("时长接近" !in evidence.reasons)
        }
    }

    @Test fun `完整度和发行匹配不补偿身份不符`() {
        val query = OnlineTextQuery("曲", "曲名", artist = "歌手", album = "专辑", durationSec = 180.0)
        val incomplete = evaluateTextMatch(hit(album = null), query)
        val complete = evaluateTextMatch(hit(), query)
        val wrongArtist = evaluateTextMatch(hit(artist = "其他歌手"), query)
        assertEquals(incomplete.identityScore, complete.identityScore)
        assertEquals(2, incomplete.completeness)
        assertEquals(3, complete.completeness)
        assertNull(incomplete.releaseScore)
        assertEquals(100, complete.releaseScore)
        assertTrue(incomplete.identityScore > wrongArtist.identityScore)
        assertTrue(wrongArtist.requiresReview)
    }

    @Test fun `时长冲突阻止默认采纳而未知不假装匹配`() {
        val query = OnlineTextQuery("曲", "曲名", artist = "歌手", durationSec = 180.0)
        assertTrue(evaluateTextMatch(hit(duration = 210.0), query).requiresReview)
        assertFalse(evaluateTextMatch(hit(duration = null), query).requiresReview)
        assertTrue(evaluateTextMatch(hit(duration = 181.0), query).identityScore >
            evaluateTextMatch(hit(duration = null), query).identityScore)
    }

    @Test fun `重制年份或现场地点不同需人工确认且降低身份匹配分`() {
        for ((left, right) in listOf("曲名 (2020 Remaster)" to "曲名 (2024 Remaster)",
            "曲名 (Live at Wembley)" to "曲名 (Live at Tokyo)")) {
            val query = OnlineTextQuery("曲", left, artist = "歌手", durationSec = 180.0)
            val conflict = evaluateTextMatch(hit(title = right), query)
            assertTrue(conflict.requiresReview)
            assertTrue(conflict.identityScore < evaluateTextMatch(hit(title = left), query).identityScore)
        }
    }
    @Test fun `已知版本细节缺失不能声称版本完全一致`() {
        for ((specific, generic) in listOf("曲名 (2020 Remaster)" to "曲名 (Remaster)",
            "曲名 (Live at Wembley)" to "曲名 (Live)")) {
            for ((queryTitle, candidateTitle) in listOf(specific to generic, generic to specific)) {
                val query = OnlineTextQuery("曲", queryTitle, artist = "歌手")
                val evidence = evaluateTextMatch(hit(title = candidateTitle), query)
                assertTrue(evidence.requiresReview)
                assertTrue(evidence.warnings.any { "信息不完整" in it })
            }
        }
    }

    @Test fun `来源额外版本说明参与身份判断与分组但不改写标题`() {
        val regular = hit()
        val live = hit().copy(versionInfo = "live at Wembley")
        assertEquals(2, groupTextCandidates(listOf(regular, live)).size)
        val evidence = evaluateTextMatch(live, OnlineTextQuery("曲", "曲名", artist = "歌手"))
        assertTrue(evidence.requiresReview)
        assertTrue(evidence.warnings.any { "现场" in it })
        assertEquals("曲名", live.title)
        assertTrue(evaluateTextMatch(hit().copy(versionInfo = "alternate take"),
            OnlineTextQuery("曲", "曲名", artist = "歌手")).requiresReview)
    }

    @Test fun `缺失标题与版本细节不会使身份分低于零`() {
        val evidence = evaluateTextMatch(hit().copy(title = null, artist = null, durationSec = null),
            OnlineTextQuery("曲", "曲名 (2020 Remaster)", artist = "歌手"))
        assertEquals(0, evidence.identityScore)
        assertTrue(evidence.requiresReview)
    }

}

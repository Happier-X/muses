package com.muses.player.feature.scrape

import com.muses.player.core.model.scrape.TextMetaHit
import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.model.Song
import com.muses.player.core.scrape.text.evaluateTextMatch

internal fun Song.reviewDurationSec(): Double? =
    durationMs.takeIf { it > 0 }?.div(1000.0) ?: durationSec.takeIf { it > 0 }?.toDouble()

/** 全选与单项勾选共用：空白候选不可用，不能通过全选清空已有信息。 */
internal fun selectableFields(state: ScrapeReviewState.Review): List<String> = listOfNotNull(
    "title".takeIf { !state.resolvedTitle().isNullOrBlank() },
    "artist".takeIf { !state.resolvedArtist().isNullOrBlank() },
    "album".takeIf { !state.resolvedAlbum().isNullOrBlank() },
    "cover".takeIf { !state.selectedCover?.remoteUrl.isNullOrBlank() },
    "lyrics".takeIf { !state.selectedLyrics?.text.isNullOrBlank() },
)

/** 只给没有明确身份或版本冲突的候选预勾选；人工逐字段选择仍可明确应用。 */
internal fun defaultTextCheckedFields(song: Song, hit: TextMetaHit?, keyword: ReviewKeyword): Set<String> {
    if (hit == null) return emptySet()
    val evidence = evaluateTextMatch(hit, OnlineTextQuery(song.id, keyword.title,
        artist = keyword.artist, album = keyword.album,
        durationSec = song.reviewDurationSec()))
    if (evidence.requiresReview) return emptySet()
    return buildSet {
        hit.title?.takeIf { it.isNotBlank() && it != song.title }?.let { add("title") }
        hit.artist?.takeIf { it.isNotBlank() && it != song.artist }?.let { add("artist") }
        hit.album?.takeIf { it.isNotBlank() && it != song.album }?.let { add("album") }
    }
}

enum class ReviewTextField(val key: String, val label: String) {
    TITLE("title", "标题"), ARTIST("artist", "歌手"), ALBUM("album", "专辑");

    fun value(hit: TextMetaHit): String? = when (this) {
        TITLE -> hit.title
        ARTIST -> hit.artist
        ALBUM -> hit.album
    }
}

/** 只替换指定字段的候选与勾选状态，其他字段、手改内容、歌词和封面保持原样。 */
internal fun ScrapeReviewState.Review.withTextFieldCandidate(
    field: ReviewTextField,
    index: Int,
): ScrapeReviewState.Review {
    val hit = text.items.getOrNull(index) ?: return this
    val value = field.value(hit)?.takeIf { it.isNotBlank() } ?: return this
    val original = when (field) {
        ReviewTextField.TITLE -> song.title
        ReviewTextField.ARTIST -> song.artist
        ReviewTextField.ALBUM -> song.album
    }
    return copy(
        textFieldIndices = textFieldIndices + (field to index),
        editTitle = if (field == ReviewTextField.TITLE) null else editTitle,
        editArtist = if (field == ReviewTextField.ARTIST) null else editArtist,
        editAlbum = if (field == ReviewTextField.ALBUM) null else editAlbum,
        checkedFields = (checkedFields - field.key) + if (value != original) setOf(field.key) else emptySet(),
    )
}

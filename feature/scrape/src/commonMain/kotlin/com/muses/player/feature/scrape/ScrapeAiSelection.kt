package com.muses.player.feature.scrape

import com.muses.player.core.ai.AiScrapeDecision

sealed interface ScrapeAiState {
    data object Idle : ScrapeAiState
    data object Matching : ScrapeAiState
    data class Ready(val decision: AiScrapeDecision) : ScrapeAiState
    data class Failed(val message: String) : ScrapeAiState
}

/** 只修改当前审核页的选择，封面与未作判断的维度保持原样。 */
internal fun ScrapeReviewState.Review.withAiSelection(decision: AiScrapeDecision): ScrapeReviewState.Review {
    var review = this
    decision.text.index?.let { index ->
        val hit = text.items.getOrNull(index) ?: return@let
        if (listOf(hit.title, hit.artist, hit.album).all { it.isNullOrBlank() }) return@let
        val fields = buildSet {
            if (!hit.title.isNullOrBlank() && hit.title != song.title) add("title")
            if (!hit.artist.isNullOrBlank() && hit.artist != song.artist) add("artist")
            if (!hit.album.isNullOrBlank() && hit.album != song.album) add("album")
        }
        val editedFields = buildSet {
            if (editTitle != null) add("title")
            if (editArtist != null) add("artist")
            if (editAlbum != null) add("album")
        }
        review = review.copy(
            selectedTextIndex = index,
            checkedFields = (review.checkedFields - setOf("title", "artist", "album")) +
                (fields - editedFields) + review.checkedFields.intersect(editedFields),
        )
    }
    decision.lyrics.index?.let { index ->
        val lyric = lyrics.items.getOrNull(index) ?: return@let
        if (lyric.text.isBlank()) return@let
        review = review.copy(
            selectedLyricsIndex = index,
            checkedFields = (review.checkedFields - "lyrics") +
                if (lyric.text != song.lyrics) setOf("lyrics") else emptySet(),
        )
    }
    return review
}

package com.muses.player.feature.scrape

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.muses.player.core.model.Song
import com.muses.player.core.model.scrape.*
import com.muses.player.core.scrape.cover.OnlineCoverSource
import com.muses.player.core.scrape.editmeta.*
import com.muses.player.core.ui.components.MusesSnackbar
import java.security.MessageDigest
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 候选与人工决定一同落盘，恢复时不重新搜索，也不会自动写回。 */
@Serializable
internal data class ReviewDraft(
    val version: Int = 1,
    val songId: String,
    val baseline: String,
    val fileRevision: String?,
    val text: List<TextDraft>,
    val cover: List<CoverDraft>,
    val lyrics: List<LyricsDraft>,
    val textIndex: Int,
    val coverIndex: Int?,
    val lyricsIndex: Int?,
    val checked: Set<String>,
    val title: String?, val artist: String?, val album: String?,
    val fields: Map<String, Int>,
    val query: List<String>, val sources: List<String>,
    val input: List<String>, val inputSources: List<String>,
    val failures: List<List<String>>,
    val statuses: List<String>, val defaults: List<Int>,
    val failureDetails: List<List<FailureDraft>>,
) {
    fun restore(song: Song, nextId: String?, currentRevision: String?): ScrapeReviewState.Review {
        require(version == 1 && song.id == songId)
        val unchanged = baseline == draftBaseline(song) && fileRevision != null && fileRevision == currentRevision
        val textItems = text.map { TextMetaHit(it.title, it.artist, it.album, OnlineTextSource.valueOf(it.source), it.durationSec, it.recordingId, it.releaseId, it.candidateId, it.versionInfo) }
        val coverItems = cover.map { EditCoverCandidate(it.url, OnlineCoverSource.valueOf(it.source)) }
        val lyricsItems = lyrics.map { EditLyricsCandidate(it.text, it.format, it.source, it.translation) }
        require(textItems.isEmpty() || textIndex in textItems.indices)
        require(coverIndex == null || coverIndex in coverItems.indices)
        require(lyricsIndex == null || lyricsIndex in lyricsItems.indices)
        require(fields.values.all { it in textItems.indices })
        fun <T> dim(items: List<T>, index: Int) = EditDimResult(
            EditDimStatus.valueOf(statuses[index]), items, defaultIndex = defaults[index],
            failedSources = failures.getOrElse(index) { emptyList() },
            sourceFailures = failureDetails[index].map { EditSourceFailure(it.source, com.muses.player.core.network.ProviderFailureKind.valueOf(it.kind), it.httpStatus) },
        )
        return ScrapeReviewState.Review(
            song, dim(textItems, 0), dim(coverItems, 1), dim(lyricsItems, 2),
            selectedTextIndex = textIndex, selectedCoverIndex = coverIndex, selectedLyricsIndex = lyricsIndex,
            checkedFields = if (unchanged) checked else emptySet(),
            editTitle = title, editArtist = artist, editAlbum = album,
            textFieldIndices = fields.mapKeys { ReviewTextField.valueOf(it.key) },
            keyword = query.keyword(), sources = sources.sources(), nextSongId = nextId,
            restoredDraft = true, draftBaselineChanged = !unchanged,
        )
    }
}

@Serializable internal data class FailureDraft(val source: String, val kind: String, val httpStatus: Int?)
@Serializable internal data class TextDraft(val title: String?, val artist: String?, val album: String?, val source: String,
    val durationSec: Double? = null, val recordingId: String? = null, val releaseId: String? = null, val candidateId: String? = null, val versionInfo: String? = null)
@Serializable internal data class CoverDraft(val url: String, val source: String)
@Serializable internal data class LyricsDraft(val text: String, val format: String, val source: String, val translation: String?)
internal fun List<String>.keyword() = ReviewKeyword(getOrElse(0) { "" }, getOrElse(1) { "" }, getOrElse(2) { "" })
internal fun List<String>.sources() = ScrapeSearchSources(CloudPlatformId.valueOf(this[0]), CloudLyricsPlatformId.valueOf(this[1]))
private fun ReviewKeyword.values() = listOf(title, artist, album)
private fun ScrapeSearchSources.values() = listOf(metadata.name, lyrics.name)
internal fun draftBaseline(song: Song): String = MessageDigest.getInstance("SHA-256")
    .digest(song.toString().toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

internal fun reviewDraft(s: ScrapeReviewState.Review, input: ReviewKeyword, sources: ScrapeSearchSources, fileRevision: String?) = ReviewDraft(
    songId = s.song.id, baseline = draftBaseline(s.song), fileRevision = fileRevision,
    text = s.text.items.map { TextDraft(it.title, it.artist, it.album, it.source.name, it.durationSec, it.recordingId, it.releaseId, it.candidateId, it.versionInfo) },
    cover = s.cover.items.map { CoverDraft(it.remoteUrl, it.source.name) },
    lyrics = s.lyrics.items.map { LyricsDraft(it.text, it.format, it.source, it.translationText) },
    textIndex = s.selectedTextIndex, coverIndex = s.selectedCoverIndex, lyricsIndex = s.selectedLyricsIndex,
    checked = s.checkedFields, title = s.editTitle, artist = s.editArtist, album = s.editAlbum,
    fields = s.textFieldIndices.mapKeys { it.key.name }, query = s.keyword.values(), sources = s.sources.values(),
    input = input.values(), inputSources = sources.values(),
    failures = listOf(s.text.failedSources, s.cover.failedSources, s.lyrics.failedSources),
    statuses = listOf(s.text.status.name, s.cover.status.name, s.lyrics.status.name),
    defaults = listOf(s.text.defaultIndex, s.cover.defaultIndex, s.lyrics.defaultIndex),
    failureDetails = listOf(s.text.sourceFailures, s.cover.sourceFailures, s.lyrics.sourceFailures)
        .map { list -> list.map { FailureDraft(it.source, it.kind.name, it.httpStatus) } },
)

/** 单一顺序队列独立于页面生命周期，防止退出页面丢写或旧保存复活已清理的草稿。 */
class ScrapeReviewDraftStore(
    private val dataStore: DataStore<Preferences>,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val operations = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    init {
        scope.launch {
            for (operation in operations) {
                try { operation() }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { MusesSnackbar.show("审核草稿未能更新，请在离开前重试操作") }
            }
        }
    }

    internal suspend fun load(songId: String): ReviewDraft? {
        val result = CompletableDeferred<ReviewDraft?>()
        operations.send {
            try {
                val raw = dataStore.data.first()[key(songId)]
                result.complete(raw?.let { json.decodeFromString<ReviewDraft>(it) })
            } catch (e: CancellationException) { result.cancel(e); throw e }
            catch (_: Exception) { result.complete(null); MusesSnackbar.show("审核草稿无法读取，将重新搜索") }
        }
        return result.await()
    }

    internal fun save(draft: ReviewDraft) {
        operations.trySend {
            val raw = json.encodeToString(draft)
            // 不截断歌词或丢弃候选，超出上限时明确提示。
            require(raw.toByteArray(Charsets.UTF_8).size <= 4 * 1024 * 1024)
            dataStore.edit { it[key(draft.songId)] = raw }
        }
    }

    fun clear(songId: String) { operations.trySend { dataStore.edit { it.remove(key(songId)) } } }
    private fun key(songId: String) = stringPreferencesKey("scrape_review_draft_$songId")
}

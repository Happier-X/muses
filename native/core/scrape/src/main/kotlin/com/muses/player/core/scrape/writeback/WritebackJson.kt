package com.muses.player.core.scrape.writeback

import com.muses.player.core.model.scrape.LyricsFormat
import com.muses.player.core.model.scrape.LyricsSource
import com.muses.player.core.model.scrape.MetaFieldSource
import com.muses.player.core.model.scrape.MetaSources
import com.muses.player.core.model.scrape.RollbackEntry
import com.muses.player.core.model.scrape.RollbackJournal
import com.muses.player.core.model.scrape.RollbackSongSnapshot
import com.muses.player.core.model.scrape.ScrapeHistoryEntry
import com.muses.player.core.model.scrape.ScrapeHistorySnapshot
import com.muses.player.core.model.scrape.ScrapeQueueItem
import com.muses.player.core.model.scrape.ScrapeQueueSnapshot
import com.muses.player.core.model.scrape.WritebackStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 刮削存储 snapshot 的 JSON 编解码（不引 @Serializable 插件，手工对齐 Web 字段名）。
 *
 * schema 均带 version 字段；解码宽松——结构不符的字段/条目跳过或回退空表，
 * 与 Web localStorage 读取的 isRecord 防御风格一致。
 */
internal object WritebackJson {

    private val json = Json

    // ── 通用取值 ──────────────────────────────────────────

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

    private fun JsonObject.long(key: String): Long? = str(key)?.toLongOrNull()

    private fun JsonObject.int(key: String): Int? = str(key)?.toIntOrNull()

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.arr(key: String): JsonArray? = this[key] as? JsonArray

    /** 枚举按 wire 宽松解析，未知值回退 null */
    private inline fun <reified E : Enum<E>> enumOf(
        wire: String?,
        wireOf: (E) -> String,
    ): E? = wire?.let { w -> enumValues<E>().firstOrNull { wireOf(it) == w } }

    private fun metaSourcesOf(o: JsonObject?): MetaSources? {
        if (o == null) return null
        val parsed = MetaSources(
            title = enumOf(o.str("title")) { it.wire },
            artist = enumOf(o.str("artist")) { it.wire },
            album = enumOf(o.str("album")) { it.wire },
            cover = enumOf(o.str("cover")) { it.wire },
        )
        return parsed.takeIf {
            it.title != null || it.artist != null || it.album != null || it.cover != null
        }
    }

    private fun MetaSources.toJson(): JsonObject = buildJsonObject {
        title?.let { put("title", JsonPrimitive(it.wire)) }
        artist?.let { put("artist", JsonPrimitive(it.wire)) }
        album?.let { put("album", JsonPrimitive(it.wire)) }
        cover?.let { put("cover", JsonPrimitive(it.wire)) }
    }

    // ── RollbackJournal ───────────────────────────────────

    fun encodeJournal(journal: RollbackJournal): String {
        val body = buildJsonObject {
            put("version", JsonPrimitive(journal.version.toString()))
            put("journalId", JsonPrimitive(journal.journalId))
            put("entries", buildJsonArray {
                for (entry in journal.entries) {
                    add(buildJsonObject {
                        put("songId", JsonPrimitive(entry.songId))
                        put("createdAt", JsonPrimitive(entry.createdAt))
                        put("songBefore", buildJsonObject {
                            entry.songBefore.title.let { put("title", JsonPrimitive(it)) }
                            entry.songBefore.artist?.let { put("artist", JsonPrimitive(it)) }
                            entry.songBefore.album?.let { put("album", JsonPrimitive(it)) }
                            entry.songBefore.coverUri?.let { put("coverUri", JsonPrimitive(it)) }
                            entry.songBefore.lyrics?.let { put("lyrics", JsonPrimitive(it)) }
                            entry.songBefore.lyricsFormat?.let { put("lyricsFormat", JsonPrimitive(it.wire)) }
                            entry.songBefore.lyricsSource?.let { put("lyricsSource", JsonPrimitive(it.wire)) }
                            entry.songBefore.metaSources?.let { put("metaSources", it.toJson()) }
                        })
                    })
                }
            })
        }
        return json.encodeToString(JsonObject.serializer(), body)
    }

    /** 解码失败/结构不符 → null（Web readRollbackJournal 防御语义） */
    fun decodeJournal(raw: String?): RollbackJournal? {
        if (raw.isNullOrEmpty()) return null
        return runCatching {
            val root = json.parseToJsonElement(raw).jsonObject
            val entries = root.arr("entries") ?: return@runCatching null
            RollbackJournal(
                version = root.int("version") ?: 1,
                journalId = root.str("journalId").orEmpty(),
                entries = entries.mapNotNull { el ->
                    val o = el as? JsonObject ?: return@mapNotNull null
                    val before = o.obj("songBefore") ?: return@mapNotNull null
                    RollbackEntry(
                        songId = o.str("songId") ?: return@mapNotNull null,
                        songBefore = RollbackSongSnapshot(
                            title = before.str("title") ?: return@mapNotNull null,
                            artist = before.str("artist"),
                            album = before.str("album"),
                            coverUri = before.str("coverUri"),
                            lyrics = before.str("lyrics"),
                            lyricsFormat = enumOf<LyricsFormat>(before.str("lyricsFormat")) { it.wire },
                            lyricsSource = enumOf<LyricsSource>(before.str("lyricsSource")) { it.wire },
                            metaSources = metaSourcesOf(before.obj("metaSources")),
                        ),
                        createdAt = o.str("createdAt") ?: "",
                    )
                },
            )
        }.getOrNull()
    }

    // ── ScrapeQueueSnapshot（S4 使用）────────────────────

    fun encodeQueue(snapshot: ScrapeQueueSnapshot): String {
        val body = buildJsonObject {
            put("version", JsonPrimitive(snapshot.version.toString()))
            put("items", buildJsonArray {
                for (item in snapshot.items) {
                    add(buildJsonObject {
                        put("songId", JsonPrimitive(item.songId))
                        put("addedAt", JsonPrimitive(item.addedAt))
                    })
                }
            })
        }
        return json.encodeToString(JsonObject.serializer(), body)
    }

    fun decodeQueue(raw: String?): ScrapeQueueSnapshot? {
        if (raw.isNullOrEmpty()) return null
        return runCatching {
            val root = json.parseToJsonElement(raw).jsonObject
            val items = root.arr("items") ?: return@runCatching null
            ScrapeQueueSnapshot(
                version = root.int("version") ?: 1,
                items = items.mapNotNull { el ->
                    val o = el as? JsonObject ?: return@mapNotNull null
                    ScrapeQueueItem(
                        songId = o.str("songId") ?: return@mapNotNull null,
                        addedAt = o.str("addedAt") ?: "",
                    )
                },
            )
        }.getOrNull()
    }

    // ── ScrapeHistorySnapshot（S4 使用）──────────────────

    fun encodeHistory(snapshot: ScrapeHistorySnapshot): String {
        val body = buildJsonObject {
            put("version", JsonPrimitive(snapshot.version.toString()))
            put("entries", buildJsonArray {
                for (entry in snapshot.entries) {
                    add(buildJsonObject {
                        put("id", JsonPrimitive(entry.id))
                        put("journalId", JsonPrimitive(entry.journalId))
                        put("songId", JsonPrimitive(entry.songId))
                        put("songTitle", JsonPrimitive(entry.songTitle))
                        entry.songArtist?.let { put("songArtist", JsonPrimitive(it)) }
                        put("at", JsonPrimitive(entry.at))
                        put("status", JsonPrimitive(entry.status.wire))
                        entry.failureReason?.let { put("failureReason", JsonPrimitive(it)) }
                        put("changedFields", buildJsonArray {
                            for (field in entry.changedFields) {
                                add(JsonPrimitive(field))
                            }
                        })
                    })
                }
            })
        }
        return json.encodeToString(JsonObject.serializer(), body)
    }

    fun decodeHistory(raw: String?): ScrapeHistorySnapshot? {
        if (raw.isNullOrEmpty()) return null
        return runCatching {
            val root = json.parseToJsonElement(raw).jsonObject
            val entries = root.arr("entries") ?: return@runCatching null
            ScrapeHistorySnapshot(
                version = root.int("version") ?: 1,
                entries = entries.mapNotNull { el ->
                    val o = el as? JsonObject ?: return@mapNotNull null
                    val statusWire = o.str("status") ?: return@mapNotNull null
                    val status = enumOf<WritebackStatus>(statusWire) { it.wire } ?: return@mapNotNull null
                    ScrapeHistoryEntry(
                        id = o.str("id") ?: return@mapNotNull null,
                        journalId = o.str("journalId").orEmpty(),
                        songId = o.str("songId") ?: return@mapNotNull null,
                        songTitle = o.str("songTitle") ?: "",
                        songArtist = o.str("songArtist"),
                        at = o.str("at") ?: "",
                        status = status,
                        failureReason = o.str("failureReason"),
                        changedFields = o.arr("changedFields")
                            ?.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p !is JsonNull }?.content }
                            .orEmpty(),
                    )
                },
            )
        }.getOrNull()
    }
}

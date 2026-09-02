package com.muses.player.core.lyrics.store

import android.content.Context
import org.json.JSONObject

enum class BoundLyricSource { AmlL, Provider }

data class LyricBinding(
    val source: BoundLyricSource,
    val provider: String? = null,
    val resourceValue: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
) {
    fun stableKey(): String = listOf(source.name, provider.orEmpty(), resourceValue).joinToString(":")
}

object LyricBindingStore {
    private const val PreferencesName = "muses_lyric_bindings"

    fun read(context: Context, key: String): LyricBinding? {
        val raw = preferences(context).getString(key, null) ?: return null
        return runCatching {
            val value = JSONObject(raw)
            LyricBinding(
                source = BoundLyricSource.valueOf(value.getString("source")),
                provider = value.optString("provider").takeIf(String::isNotBlank),
                resourceValue = value.getString("resourceValue"),
                title = value.optString("title"),
                artist = value.optString("artist"),
                durationMs = value.optLong("durationMs"),
            )
        }.getOrNull()
    }

    fun write(context: Context, key: String, binding: LyricBinding) {
        val value = JSONObject()
            .put("source", binding.source.name)
            .put("provider", binding.provider ?: "")
            .put("resourceValue", binding.resourceValue)
            .put("title", binding.title)
            .put("artist", binding.artist)
            .put("durationMs", binding.durationMs)
        preferences(context).edit().putString(key, value.toString()).apply()
    }

    fun clear(context: Context) {
        preferences(context).edit().clear().apply()
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
}

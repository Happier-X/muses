package com.example.muses.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.muses.data.model.AudioTrack
import com.example.muses.data.model.TrackSource
import org.json.JSONArray
import org.json.JSONObject

object TrackStore {

    private const val PREFS_NAME = "track_store"
    private const val KEY_TRACKS = "tracks"

    fun saveTracks(context: Context, tracks: List<AudioTrack>) {
        val jsonArray = JSONArray()
        for (track in tracks) {
            jsonArray.put(trackToJson(track))
        }
        getPrefs(context).edit()
            .putString(KEY_TRACKS, jsonArray.toString())
            .apply()
    }

    fun loadTracks(context: Context): List<AudioTrack> {
        val json = getPrefs(context).getString(KEY_TRACKS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).mapNotNull { index ->
                jsonToTrack(jsonArray.getJSONObject(index))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clear(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    private fun trackToJson(track: AudioTrack): JSONObject {
        return JSONObject().apply {
            put("id", track.id)
            put("uri", track.uri.toString())
            put("title", track.title)
            put("artist", track.artist)
            put("album", track.album)
            put("durationMs", track.durationMs)
            put("source", track.source.name)
            put("sizeBytes", track.sizeBytes)
        }
    }

    private fun jsonToTrack(json: JSONObject): AudioTrack? {
        return try {
            AudioTrack(
                id = json.getString("id"),
                uri = android.net.Uri.parse(json.getString("uri")),
                title = json.getString("title"),
                artist = json.optString("artist", ""),
                album = json.optString("album", ""),
                durationMs = json.optLong("durationMs", 0L),
                source = TrackSource.valueOf(json.getString("source")),
                sizeBytes = json.optLong("sizeBytes", 0L)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
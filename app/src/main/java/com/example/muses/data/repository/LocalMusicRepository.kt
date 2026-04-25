package com.example.muses.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.muses.data.model.AudioTrack
import com.example.muses.data.model.TrackSource

/**
 * Queries the device MediaStore for local audio files.
 */
class LocalMusicRepository(private val contentResolver: ContentResolver) {

    companion object {
        private const val TAG = "LocalMusicRepo"
    }

    /**
     * Returns all audio tracks from MediaStore, sorted by title.
     */
    fun loadTracks(): Result<List<AudioTrack>> {
        return try {
            val tracks = mutableListOf<AudioTrack>()

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%'"
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.MIME_TYPE
            )

            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = Uri.withAppendedPath(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )
                    val title = cursor.getString(titleCol)
                        ?: MediaStore.UNKNOWN_STRING
                    val artist = cursor.getString(artistCol)
                        ?: MediaStore.UNKNOWN_STRING
                    val album = cursor.getString(albumCol)
                        ?: MediaStore.UNKNOWN_STRING
                    val duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)

                    tracks.add(
                        AudioTrack(
                            id = id.toString(),
                            uri = uri,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            sizeBytes = size,
                            source = TrackSource.LOCAL
                        )
                    )
                }
            }

            Log.i(TAG, "Loaded ${tracks.size} local tracks")
            Result.success(tracks)
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load tracks", e)
            Result.failure(e)
        }
    }
}

package com.example.muses.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.DocumentsContract
import android.util.Log
import com.example.muses.data.model.AudioTrack
import com.example.muses.data.model.TrackSource
import java.io.File

class LocalMusicRepository(
    private val contentResolver: ContentResolver,
    private val context: Context? = null
) {

    companion object {
        private const val TAG = "LocalMusicRepo"
        private val AUDIO_EXTENSIONS = setOf(
            "mp3", "flac", "wav", "ogg", "m4a", "aac", "wma", "opus", "3gp", "mid", "midi"
        )
    }

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

    fun loadTracksFromTreeUri(treeUri: Uri): Result<List<AudioTrack>> {
        return try {
            val tracks = mutableListOf<AudioTrack>()
            traverseTreeUri(treeUri, tracks)
            Log.i(TAG, "Loaded ${tracks.size} tracks from tree URI")
            Result.success(tracks)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load tracks from tree URI", e)
            Result.failure(e)
        }
    }

    private fun traverseTreeUri(treeUri: Uri, tracks: MutableList<AudioTrack>) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )

        contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            ),
            null,
            null,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

            while (cursor.moveToNext()) {
                val docId = cursor.getString(idCol)
                val name = cursor.getString(nameCol) ?: continue
                val mimeType = cursor.getString(mimeCol) ?: ""
                val size = cursor.getLong(sizeCol)

                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    val subTreeUri = DocumentsContract.buildTreeDocumentUri(
                        treeUri.authority,
                        docId
                    )
                    traverseTreeUri(subTreeUri, tracks)
                } else if (isAudioFile(name)) {
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    val trackId = "local_$docId"
                    val title = name.substringBeforeLast('.')
                    tracks.add(
                        AudioTrack(
                            id = trackId,
                            uri = docUri,
                            title = title,
                            artist = "",
                            album = "",
                            durationMs = 0L,
                            sizeBytes = size,
                            source = TrackSource.LOCAL
                        )
                    )
                }
            }
        }
    }

    private fun isAudioFile(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in AUDIO_EXTENSIONS
    }
}
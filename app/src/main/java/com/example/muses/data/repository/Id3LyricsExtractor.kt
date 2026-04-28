package com.example.muses.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Extracts embedded lyrics from audio files using ID3 tags.
 * Supports USLT (Unsynchronized Lyrics) frames in ID3v2 tags.
 *
 * Note: This is a simplified implementation that handles basic USLT frames.
 * For full ID3 support with all frame types, consider using a library like:
 * - mp3agic (requires JitPack)
 * - jaudiotagger
 */
object Id3LyricsExtractor {
    private const val TAG = "Id3LyricsExtractor"

    // ID3v2 frame IDs
    private const val FRAME_USLT = "USLT"  // Unsynchronized lyrics
    private const val FRAME_SYLT = "SYLT"  // Synchronized lyrics
    private const val FRAME_TXXX = "TXXX"  // User defined text information
    private const val FRAME_COMM = "COMM"  // Comments

    /**
     * Extract embedded lyrics from a local audio file.
     * @param uri URI to the audio file
     * @return Lyrics string, or null if not found
     */
    fun extractFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                extractFromStream(input)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract ID3 lyrics from $uri", e)
            null
        }
    }

    /**
     * Extract embedded lyrics from an InputStream.
     * @param input InputStream of the audio file
     * @return Lyrics string, or null if not found
     */
    fun extractFromStream(input: InputStream): String? {
        return try {
            // Read enough bytes to find ID3v2 header and frames
            val headerBytes = ByteArray(10)
            val bytesRead = input.read(headerBytes)
            Log.d(TAG, "extractFromStream: read $bytesRead bytes for header")
            if (bytesRead < 10) {
                Log.d(TAG, "Not enough bytes for ID3 header")
                return null
            }

            // Check for ID3v2 header: "ID3"
            if (headerBytes[0] != 'I'.code.toByte() ||
                headerBytes[1] != 'D'.code.toByte() ||
                headerBytes[2] != '3'.code.toByte()) {
                Log.d(TAG, "No ID3v2 header found: ${headerBytes[0]}, ${headerBytes[1]}, ${headerBytes[2]}")
                return null
            }

            // Parse ID3v2 tag size (syncsafe integer)
            val tagSize = parseSyncsafeInt(headerBytes, 6)
            Log.d(TAG, "ID3v2 tag size: $tagSize")
            if (tagSize <= 0 || tagSize > 10 * 1024 * 1024) {  // Max 10MB
                Log.d(TAG, "Invalid ID3v2 tag size: $tagSize")
                return null
            }

            // Read the entire tag
            val tagData = ByteArray(tagSize)
            val tagBytesRead = input.read(tagData)
            Log.d(TAG, "Read $tagBytesRead bytes of tag data")

            // Find USLT frame
            val result = findLyricsFrame(tagData)
            Log.d(TAG, "Lyrics extraction result: ${result?.take(100)}...")
            result
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract ID3 lyrics from stream", e)
            null
        }
    }

    /**
     * Parse a syncsafe integer (ID3v2 format).
     */
    private fun parseSyncsafeInt(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0x7F) shl 21) or
               ((data[offset + 1].toInt() and 0x7F) shl 14) or
               ((data[offset + 2].toInt() and 0x7F) shl 7) or
               (data[offset + 3].toInt() and 0x7F)
    }

    /**
     * Find and extract lyrics from USLT frame.
     */
    private fun findLyricsFrame(tagData: ByteArray): String? {
        var offset = 0
        while (offset < tagData.size - 10) {
            // Read frame header
            val frameId = String(tagData, offset, 4, Charsets.ISO_8859_1)

            // Check for valid frame ID (all uppercase letters or digits)
            if (!frameId.all { it.isUpperCase() || it.isDigit() }) {
                // Padding reached or invalid frame
                break
            }

            // Frame size (big-endian for ID3v2.3, syncsafe for ID3v2.4)
            val frameSize = ByteBuffer.wrap(tagData, offset + 4, 4)
                .order(ByteOrder.BIG_ENDIAN)
                .int

            if (frameSize <= 0 || frameSize > tagData.size - offset) {
                break
            }

            Log.d(TAG, "Found frame: $frameId, size: $frameSize")

            // Check for USLT frame (lyrics)
            if (frameId == FRAME_USLT) {
                val lyrics = parseUsltFrame(tagData, offset + 10, frameSize)
                if (!lyrics.isNullOrBlank()) {
                    Log.d(TAG, "Found USLT lyrics")
                    return lyrics
                }
            }

            // Check for TXXX frame (might contain lyrics)
            if (frameId == FRAME_TXXX) {
                val txxxContent = parseTxxxFrame(tagData, offset + 10, frameSize)
                if (!txxxContent.isNullOrBlank() && isLikelyLyrics(txxxContent)) {
                    Log.d(TAG, "Found TXXX lyrics")
                    return txxxContent
                }
            }

            // Move to next frame
            offset += 10 + frameSize
        }

        Log.d(TAG, "No lyrics frame found in ID3 tag")
        return null
    }

    /**
     * Check if text content looks like lyrics.
     */
    private fun isLikelyLyrics(text: String): Boolean {
        // Lyrics typically have multiple lines and reasonable length
        val lines = text.lines().filter { it.isNotBlank() }
        return lines.size >= 2 && text.length > 20
    }

    /**
     * Parse TXXX (User defined text) frame.
     * Format: [encoding byte] [description] [null] [value]
     */
    private fun parseTxxxFrame(data: ByteArray, offset: Int, size: Int): String? {
        if (size < 2) return null

        val encoding = data[offset].toInt() and 0xFF
        val contentStart = offset + 1
        val contentEnd = offset + size

        // Find null terminator for description
        var valueStart = contentStart
        when (encoding) {
            0, 3 -> {  // ISO-8859-1 or UTF-8
                while (valueStart < contentEnd && data[valueStart] != 0.toByte()) {
                    valueStart++
                }
                if (valueStart < contentEnd) valueStart++  // Skip null
            }
            1, 2 -> {  // UTF-16
                while (valueStart < contentEnd - 1) {
                    if (data[valueStart] == 0.toByte() && data[valueStart + 1] == 0.toByte()) {
                        valueStart += 2
                        break
                    }
                    valueStart += 2
                }
            }
        }

        if (valueStart >= contentEnd) return null

        return try {
            val valueBytes = data.copyOfRange(valueStart, contentEnd)
            when (encoding) {
                0 -> String(valueBytes, Charsets.ISO_8859_1)
                1 -> String(valueBytes, Charsets.UTF_16)
                2 -> String(valueBytes, Charsets.UTF_16BE)
                3 -> String(valueBytes, Charsets.UTF_8)
                else -> String(valueBytes, Charsets.UTF_8)
            }.trim().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode TXXX frame", e)
            null
        }
    }

    /**
     * Parse USLT (Unsynchronized Lyrics) frame.
     * Format: [encoding byte] [language (3 bytes)] [description] [null] [lyrics text]
     */
    private fun parseUsltFrame(data: ByteArray, offset: Int, size: Int): String? {
        if (size < 4) return null

        val encoding = data[offset].toInt() and 0xFF
        // Skip: encoding (1) + language (3)
        val contentStart = offset + 4
        val contentEnd = offset + size

        // Find null terminator for description, then lyrics start after it
        var lyricsStart = contentStart
        when (encoding) {
            0, 3 -> {  // ISO-8859-1 or UTF-8
                while (lyricsStart < contentEnd && data[lyricsStart] != 0.toByte()) {
                    lyricsStart++
                }
                if (lyricsStart < contentEnd) lyricsStart++  // Skip null
            }
            1, 2 -> {  // UTF-16 with BOM or UTF-16BE
                while (lyricsStart < contentEnd - 1) {
                    if (data[lyricsStart] == 0.toByte() && data[lyricsStart + 1] == 0.toByte()) {
                        lyricsStart += 2
                        break
                    }
                    lyricsStart += 2
                }
            }
        }

        if (lyricsStart >= contentEnd) return null

        // Decode lyrics text
        return try {
            val lyricsBytes = data.copyOfRange(lyricsStart, contentEnd)
            when (encoding) {
                0 -> String(lyricsBytes, Charsets.ISO_8859_1)
                1 -> String(lyricsBytes, Charsets.UTF_16)  // With BOM
                2 -> String(lyricsBytes, Charsets.UTF_16BE)
                3 -> String(lyricsBytes, Charsets.UTF_8)
                else -> String(lyricsBytes, Charsets.UTF_8)
            }.trim().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode lyrics text", e)
            null
        }
    }
}

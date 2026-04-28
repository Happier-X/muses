package com.example.muses.ui.util

import android.util.Log
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Represents a single line of lyrics with its timestamp.
 */
data class LyricLine(
    val timeMs: Long,
    val text: String
)

/**
 * Represents parsed lyrics with metadata.
 */
data class ParsedLyrics(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val lines: List<LyricLine>
) {
    /**
     * Find the lyric line index for a given playback position.
     * Returns -1 if no matching line found.
     */
    fun findLineIndex(positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        // Binary search for efficiency
        var low = 0
        var high = lines.size - 1
        while (low < high) {
            val mid = (low + high + 1) / 2
            if (lines[mid].timeMs <= positionMs) {
                low = mid
            } else {
                high = mid - 1
            }
        }
        return if (lines[low].timeMs <= positionMs) low else -1
    }

    /**
     * Get the current lyric line text for a given position.
     */
    fun getCurrentLine(positionMs: Long): String? {
        val index = findLineIndex(positionMs)
        return if (index >= 0) lines[index].text else null
    }
}

/**
 * Parser for standard LRC (Lyric) files.
 *
 * Supports:
 * - Standard timestamps: [mm:ss.xx], [mm:ss.xxx], [hh:mm:ss.xx]
 * - Metadata tags: [ti:...], [ar:...], [al:...]
 * - Word-level timestamps: <mm:ss.xx>word</mm:ss.xx> (simplified to line-level)
 * - Encrypted lyrics marker: [re:...]
 * - Offset tag: [offset:xxx] (applied to all timestamps)
 */
object LrcParser {
    private const val TAG = "LrcParser"

    // Pattern for standard timestamp: [mm:ss.xx] or [mm:ss.xxx] or [hh:mm:ss.xx]
    private val TIMESTAMP_PATTERN = Pattern.compile(
        "\\[(\\d{1,2}):(\\d{2})(?::(\\d{2}))?\\.(\\d{1,3})]"
    )

    // Pattern for metadata tags
    private val METADATA_PATTERN = Pattern.compile(
        "\\[(ti|tg|ar|al|by|offset|re|ve):(.+)]",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Parse LRC content from an InputStream.
     */
    fun parse(inputStream: InputStream): ParsedLyrics? {
        return try {
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                parseLines(reader.lineSequence())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse LRC from stream", e)
            null
        }
    }

    /**
     * Parse LRC content from a string.
     */
    fun parse(content: String): ParsedLyrics? {
        return try {
            parseLines(content.lineSequence())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse LRC from string", e)
            null
        }
    }

    private fun parseLines(lines: Sequence<String>): ParsedLyrics {
        val lyricLines = mutableListOf<LyricLine>()
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var offsetMs = 0L

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // Check for metadata
            val metaMatcher = METADATA_PATTERN.matcher(trimmed)
            if (metaMatcher.matches()) {
                val tag = metaMatcher.group(1)!!.lowercase()
                val value = metaMatcher.group(2)!!.trim()
                when (tag) {
                    "ti" -> title = value
                    "ar" -> artist = value
                    "al" -> album = value
                    "offset" -> offsetMs = value.toLongOrNull() ?: 0L
                }
                continue
            }

            // Extract timestamps and text
            val timestampMatcher = TIMESTAMP_PATTERN.matcher(trimmed)
            if (!timestampMatcher.find()) continue

            // Reset matcher to find all timestamps in this line
            timestampMatcher.reset()
            val timestamps = mutableListOf<Long>()
            var lastEnd = 0

            while (timestampMatcher.find()) {
                val timeMs = parseTimestamp(timestampMatcher)
                if (timeMs != null) {
                    timestamps.add(timeMs)
                }
                lastEnd = timestampMatcher.end()
            }

            if (timestamps.isEmpty()) continue

            // Get text after last timestamp
            var text = trimmed.substring(lastEnd).trim()
            // Remove leading delimiter if present (e.g., "> ")
            if (text.startsWith(">") || text.startsWith("：") || text.startsWith(":")) {
                text = text.drop(1).trim()
            }

            // Skip empty lines (instrumental sections)
            if (text.isEmpty()) continue

            // Create a lyric line for each timestamp
            for (timestamp in timestamps) {
                lyricLines.add(LyricLine(
                    timeMs = (timestamp + offsetMs).coerceAtLeast(0),
                    text = text
                ))
            }
        }

        // Sort by timestamp
        lyricLines.sortBy { it.timeMs }

        return ParsedLyrics(
            title = title,
            artist = artist,
            album = album,
            lines = lyricLines
        )
    }

    private fun parseTimestamp(matcher: Matcher): Long? {
        val minutes = matcher.group(1)?.toLongOrNull() ?: return null
        val seconds = matcher.group(2)?.toLongOrNull() ?: return null
        val hundredths = matcher.group(3)?.let { s ->
            // Handle both :ss.xx and .xx formats
            if (s.length == 2) s.toLongOrNull() ?: 0 else null
        } ?: 0L
        val centiseconds = matcher.group(4)?.let { s ->
            // Pad to 3 digits for milliseconds
            when (s.length) {
                1 -> s.toLongOrNull()?.times(100) ?: 0
                2 -> s.toLongOrNull()?.times(10) ?: 0
                3 -> s.toLongOrNull() ?: 0
                else -> 0
            }
        } ?: 0L

        return (minutes * 60 + seconds) * 1000 + centiseconds
    }
}

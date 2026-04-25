package com.example.muses.ui.util

/**
 * Shared formatting utilities for the Muses UI.
 */
fun formatDurationMs(ms: Long): String {
    if (ms <= 0) return "--:--"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

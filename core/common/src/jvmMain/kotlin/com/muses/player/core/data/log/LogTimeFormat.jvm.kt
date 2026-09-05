package com.muses.player.core.data.log

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal actual fun formatLogTime(timestampMs: Long, pattern: String): String =
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestampMs))

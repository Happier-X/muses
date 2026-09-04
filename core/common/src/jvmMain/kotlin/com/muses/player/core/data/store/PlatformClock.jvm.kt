package com.muses.player.core.data.store

actual fun platformNowIso(): String = java.time.Instant.now().toString()

actual fun platformNowMs(): Long = System.currentTimeMillis()

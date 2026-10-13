package com.muses.player.core.model

import java.text.Normalizer

internal actual fun normalizeSongIdentity(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFKC).lowercase().filter { it.isLetterOrDigit() }

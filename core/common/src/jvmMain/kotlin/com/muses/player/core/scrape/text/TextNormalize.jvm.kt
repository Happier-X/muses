package com.muses.player.core.scrape.text

import java.text.Normalizer

/** Android/JVM 同源实现：JDK NFKC 归一化（行为与原 core:scrape 版逐字节一致）。 */
internal actual fun normalizeNfkc(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFKC)

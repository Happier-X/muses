package com.muses.player.core.scrape.text.provider

import java.net.URLEncoder

/**
 * JS encodeURIComponent 对齐：空格转 %20（URLEncoder 会转成 +，需替换）；
 * 用 charset 名重载以兼容 minSdk 26（Charset 重载需 API 33）。
 * Android/JVM 同源实现，行为与原 core:scrape 版逐字节一致。
 */
actual fun urlEncode(value: String): String =
    URLEncoder.encode(value, "UTF-8").replace("+", "%20")

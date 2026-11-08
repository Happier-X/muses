package com.muses.player.core.appupdate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * 应用更新检查（U15 从安卓装配层上收 jvmShared：java.net/JVM 双端可用）。
 *
 * 数据面收敛为「Release 里的一个安装包资产」：桌面取 `.msi`，安卓按渠道取 `.apk`
 * （产物命名见 .github/workflows/release.yml），解析与网络请求双端共用。
 */
data class ReleaseInfo(
    /** 如 "v0.9.6" */
    val tag: String,
    /** Release 页面地址（下载失败/无合适资产时的回退入口） */
    val htmlUrl: String,
    /** 资产直链（release asset 的 browser_download_url） */
    val url: String,
    /** 资产文件名（本地缓存文件名用） */
    val name: String,
    /** asset 声明字节数；<=0 表示未知（进度条转不定态） */
    val sizeBytes: Long,
    /** Release 正文（更新日志 markdown 原文，可能为空） */
    val notes: String,
)

private const val RELEASE_BASE = "https://github.com/Happier-X/muses/releases"

private val lenientJson = Json { ignoreUnknownKeys = true }

/**
 * 纯解析：GitHub `releases/latest` 响应正文 → 名为 [assetNameForTag] 的资产；
 * 缺 tag / html_url / 该资产时返回 null（调用方统一按失败提示）。纯函数可单测。
 */
fun parseRelease(body: String, assetNameForTag: (String) -> String): ReleaseInfo? {
    val root = runCatching { lenientJson.parseToJsonElement(body).jsonObject }.getOrNull()
        ?: return null
    val tag = root["tag_name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() }
        ?: return null
    val htmlUrl = root["html_url"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() }
        ?: return null
    val notes = root["body"]?.jsonPrimitive?.contentOrNull.orEmpty()
    val expectedName = assetNameForTag(tag)
    val assets = runCatching { root["assets"]?.jsonArray }.getOrNull() ?: return null
    for (element in assets) {
        val asset = runCatching { element.jsonObject }.getOrNull() ?: continue
        val name = asset["name"]?.jsonPrimitive?.contentOrNull ?: continue
        if (!name.equals(expectedName, ignoreCase = true)) continue
        val url = asset["browser_download_url"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotEmpty() } ?: continue
        val size = asset["size"]?.jsonPrimitive?.longOrNull ?: -1L
        return ReleaseInfo(tag, htmlUrl, url, name, size, notes)
    }
    return null
}

/**
 * 拉取最新 Release 的指定资产；网络/格式异常返回 null。有新版与否由调用方经 [compareVersions] 判定。
 *
 * 先走 GitHub API（能带出更新日志与包大小）；API 对共享出口 IP 限流（403）时退回
 * `releases/latest` 的 302 跳转 + 产物命名约定——包大小未知（进度条转不定态）、无日志。
 */
suspend fun fetchRelease(currentVersion: String, assetNameForTag: (String) -> String): ReleaseInfo? {
    latestReleaseBody(currentVersion)?.let { body ->
        parseRelease(body, assetNameForTag)?.let { return it }
    }
    val tag = latestTag(currentVersion) ?: return null
    val name = assetNameForTag(tag)
    return ReleaseInfo(
        tag = tag,
        htmlUrl = "$RELEASE_BASE/tag/$tag",
        url = "$RELEASE_BASE/download/$tag/$name",
        name = name,
        sizeBytes = -1L,
        notes = "",
    )
}

/** GitHub releases/latest 正文；网络异常/非 200 返回 null（不细分提示，调用方统一文案）。 */
private suspend fun latestReleaseBody(currentVersion: String): String? {
    return withContext(Dispatchers.IO) {
        var connection: java.net.HttpURLConnection? = null
        try {
            connection = java.net.URL("https://api.github.com/repos/Happier-X/muses/releases/latest")
                .openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "Muses/$currentVersion")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            if (connection.responseCode != 200) {
                return@withContext null
            }
            connection.inputStream.bufferedReader().readText()
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}

/** `releases/latest` 的 302 Location 里取最新 tag（API 限流时的退路，不带日志/大小）。 */
private suspend fun latestTag(currentVersion: String): String? {
    return withContext(Dispatchers.IO) {
        var connection: java.net.HttpURLConnection? = null
        try {
            connection = java.net.URL("$RELEASE_BASE/latest")
                .openConnection() as java.net.HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "Muses/$currentVersion")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.responseCode
            connection.getHeaderField("Location")
                ?.substringAfterLast("/tag/")
                ?.takeIf { it.isNotEmpty() && it != connection.url.toString() }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}

/** 版本号比较（语义同 Web 层 compareVersions）：>0 表示 a 更新 */
fun compareVersions(a: String, b: String): Int {
    val partsA = a.split('.').map { it.toIntOrNull() ?: 0 }
    val partsB = b.split('.').map { it.toIntOrNull() ?: 0 }
    val len = maxOf(partsA.size, partsB.size)
    for (i in 0 until len) {
        val va = partsA.getOrElse(i) { 0 }
        val vb = partsB.getOrElse(i) { 0 }
        if (va > vb) return 1
        if (va < vb) return -1
    }
    return 0
}

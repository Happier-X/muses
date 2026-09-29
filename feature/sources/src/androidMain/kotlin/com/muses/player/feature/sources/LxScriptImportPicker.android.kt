package com.muses.player.feature.sources

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@Composable
actual fun rememberLxScriptFilePicker(onPicked: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        }.getOrNull()?.let(onPicked)
    }
    return { launcher.launch(arrayOf("text/*", "application/javascript", "application/octet-stream")) }
}

@Composable
actual fun rememberLxScriptUrlImporter(onImported: (Result<String>) -> Unit): (String) -> Unit {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    return { url ->
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    require(url.startsWith("https://") || url.startsWith("http://")) { "请输入有效的 HTTP/HTTPS 地址" }
                    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 30000
                    }
                    try {
                        val status = connection.responseCode
                        if (status !in 200..299) {
                            throw IOException(if (status == 404) "脚本地址不存在（HTTP 404）" else "下载脚本失败（HTTP $status）")
                        }
                        connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } finally {
                        connection.disconnect()
                    }
                }
            }
            onImported(result)
        }
    }
}

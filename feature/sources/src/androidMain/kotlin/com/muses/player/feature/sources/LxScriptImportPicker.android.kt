package com.muses.player.feature.sources

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                    URL(url).openConnection().apply { connectTimeout = 15000; readTimeout = 30000 }
                        .getInputStream().bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
            }
            onImported(result)
        }
    }
}

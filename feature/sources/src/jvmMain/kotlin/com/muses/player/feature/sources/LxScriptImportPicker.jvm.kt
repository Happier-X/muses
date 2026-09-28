package com.muses.player.feature.sources

import androidx.compose.runtime.Composable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

@Composable
actual fun rememberLxScriptFilePicker(onPicked: (String) -> Unit): () -> Unit = {
    SwingUtilities.invokeLater {
        val chooser = JFileChooser().apply { dialogTitle = "选择洛雪音源脚本"; isMultiSelectionEnabled = false }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            runCatching { chooser.selectedFile.readText(Charsets.UTF_8) }.getOrNull()?.let(onPicked)
        }
    }
}

@Composable
actual fun rememberLxScriptUrlImporter(onImported: (Result<String>) -> Unit): (String) -> Unit {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    return { url -> scope.launch {
        onImported(runCatching { withContext(Dispatchers.IO) {
            require(url.startsWith("https://") || url.startsWith("http://")) { "请输入有效的 HTTP/HTTPS 地址" }
            URL(url).openConnection().apply { connectTimeout = 15000; readTimeout = 30000 }
                .getInputStream().bufferedReader(Charsets.UTF_8).use { it.readText() }
        } })
    } }
}

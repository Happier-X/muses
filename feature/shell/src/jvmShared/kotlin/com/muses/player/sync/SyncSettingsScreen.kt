package com.muses.player.sync

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muses.player.core.ui.components.*
import com.muses.player.core.ui.theme.LocalBottomChromePadding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.preference.SwitchPreference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncSettingsScreen(onBack: () -> Unit) {
    val manager = koinInject<WebDavSyncManager>()
    val config by manager.config.collectAsState(WebDavSyncConfig())
    val busy by manager.busy.collectAsState()
    val status by manager.status.collectAsState()
    val lastSync by manager.lastSync.collectAsState(0L)
    val selection by manager.selection.collectAsState(SyncSelection())
    var url by remember(config) { mutableStateOf(config.url) }
    var username by remember(config) { mutableStateOf(config.username) }
    var directory by remember(config) { mutableStateOf(config.directory) }
    var password by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val blocked = busy || saving

    fun run(action: suspend () -> Unit) {
        if (blocked) return
        scope.launch {
            saving = true
            error = null
            try {
                manager.save(WebDavSyncConfig(url, username, directory), password)
                password = ""
                action()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = "配置保存失败，请检查地址、账号、密码和目录" }
            finally { saving = false }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = { MusesTopBar(title = "WebDAV 同步", onBack = onBack) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp + LocalBottomChromePadding.current), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsBlockTitle("连接配置")
            Card(Modifier.padding(horizontal = 12.dp)) {
                SourceFormInput("WebDAV 地址", url, readOnly = blocked, onValueChange = { url = it })
                SourceFormInput("账号", username, readOnly = blocked, onValueChange = { username = it })
                SourceFormInput("密码", password, isPassword = true, readOnly = blocked,
                    onValueChange = { password = it })
                SourceFormInput("同步目录", directory, readOnly = blocked, onValueChange = { directory = it })
                Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MusesButton(modifier = Modifier.weight(1f), enabled = !blocked, onClick = { run { manager.testConnection() } }) {
                        Text("测试连接")
                    }
                    MusesButton(modifier = Modifier.weight(1f), enabled = !blocked && selection.anyEnabled, onClick = { run { manager.synchronize() } }) {
                        Text(if (blocked) "正在处理…" else "立即同步")
                    }
                }
            }
            val message = error ?: status
            if (message.isNotBlank()) {
                Text(message, modifier = Modifier.padding(horizontal = 26.dp))
            }
            if (lastSync > 0L) {
                Text("上次同步：" + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(lastSync)),
                    modifier = Modifier.padding(horizontal = 26.dp))
            }
            SettingsBlockTitle("同步内容")
            Card(Modifier.padding(horizontal = 12.dp)) {
                SwitchPreference(title = "应用设置", checked = selection.preferences, enabled = !blocked,
                    onCheckedChange = { scope.launch { manager.setContentEnabled(SyncContent.PREFERENCES, it) } })
                SwitchPreference(title = "音源", checked = selection.sources, enabled = !blocked,
                    onCheckedChange = { scope.launch { manager.setContentEnabled(SyncContent.SOURCES, it) } })
                SwitchPreference(title = "播放历史", checked = selection.history, enabled = !blocked,
                    onCheckedChange = { scope.launch { manager.setContentEnabled(SyncContent.HISTORY, it) } })
                SwitchPreference(title = "听歌统计", checked = selection.stats, enabled = !blocked,
                    onCheckedChange = { scope.launch { manager.setContentEnabled(SyncContent.STATS, it) } })
            }
        }
    }
}

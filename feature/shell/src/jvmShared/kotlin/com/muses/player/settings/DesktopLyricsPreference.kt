package com.muses.player.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.muses.player.core.data.repository.SettingsRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
expect fun DesktopLyricsPreference()

@Composable
fun DesktopLyricsLockPreference() {
    val settings = koinInject<SettingsRepository>()
    val locked by settings.desktopLyricsLocked.collectAsState(false)
    val scope = rememberCoroutineScope()
    SwitchPreference(title = "锁定桌面歌词", summary = "锁定后点击穿透，关闭此开关即可重新拖动和操作歌词",
        checked = locked, onCheckedChange = { scope.launch { settings.setDesktopLyricsLocked(it) } })
}

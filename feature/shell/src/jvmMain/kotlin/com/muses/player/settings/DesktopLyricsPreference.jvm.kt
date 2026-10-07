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
actual fun DesktopLyricsPreference() {
    val settings = koinInject<SettingsRepository>()
    val enabled by settings.desktopLyricsEnabled.collectAsState(false)
    val scope = rememberCoroutineScope()
    SwitchPreference(
        title = "桌面歌词",
        checked = enabled,
        onCheckedChange = { scope.launch { settings.setDesktopLyricsEnabled(it) } },
    )
}

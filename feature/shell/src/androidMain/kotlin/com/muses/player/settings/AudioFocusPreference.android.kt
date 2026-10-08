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
actual fun AudioFocusPreference() {
    val settings = koinInject<SettingsRepository>()
    val enabled by settings.audioFocusEnabled.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    SwitchPreference(
        title = "音频焦点",
        summary = "关闭后可与其他应用同时播放",
        checked = enabled,
        onCheckedChange = { scope.launch { settings.setAudioFocusEnabled(it) } },
    )
}

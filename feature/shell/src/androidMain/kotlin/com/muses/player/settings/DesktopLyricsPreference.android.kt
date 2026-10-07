package com.muses.player.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.muses.player.core.data.repository.SettingsRepository
import com.muses.player.core.ui.components.MusesSnackbar
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
actual fun DesktopLyricsPreference() {
    val context = LocalContext.current
    val settings = koinInject<SettingsRepository>()
    val enabled by settings.desktopLyricsEnabled.collectAsState(false)
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !Settings.canDrawOverlays(context)) {
                scope.launch { settings.setDesktopLyricsEnabled(false) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val allowed = Settings.canDrawOverlays(context)
        scope.launch { settings.setDesktopLyricsEnabled(allowed) }
        if (!allowed) MusesSnackbar.show("请允许显示在其他应用上层后开启桌面歌词")
    }
    SwitchPreference(
        title = "桌面歌词",
        checked = enabled,
        onCheckedChange = { value ->
            if (!value || Settings.canDrawOverlays(context)) {
                scope.launch { settings.setDesktopLyricsEnabled(value) }
            } else {
                runCatching {
                    launcher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                }.onFailure { MusesSnackbar.show("无法打开悬浮窗授权页，请在系统设置中授权") }
            }
        },
    )
}

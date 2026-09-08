package com.muses.player.feature.shell.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.muses.player.core.data.platform.HyperOsSupport
import com.muses.player.core.data.repository.SettingsRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.preference.SwitchPreference

/**
 * 小米超级岛设置行（miuix SwitchPreference 版，与播放区开关同样式）。
 * 非 HyperOS 设备不渲染（开关无意义，不占设置项）；桌面为空实现。
 */
@Composable
actual fun XiaomiIslandSettingRow() {
    if (!HyperOsSupport.isHyperOS()) return
    val settingsRepository = koinInject<SettingsRepository>()
    val scope = rememberCoroutineScope()
    val enabled by settingsRepository.xiaomiIslandEnabled.collectAsState(initial = true)
    SwitchPreference(
        title = "小米超级岛",
        summary = "焦点通知展示播放信息（需系统白名单，否则仅普通通知）",
        checked = enabled,
        onCheckedChange = { scope.launch { settingsRepository.setXiaomiIslandEnabled(it) } },
    )
}

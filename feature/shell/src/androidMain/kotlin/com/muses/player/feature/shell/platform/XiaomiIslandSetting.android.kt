package com.muses.player.feature.shell.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.muses.player.core.data.platform.HyperOsSupport
import com.muses.player.core.data.repository.SettingsRepository
import com.muses.player.core.ui.components.SaltListItem
import com.muses.player.core.ui.components.SaltToggle
import com.muses.player.core.ui.components.SettingsIcon
import com.muses.player.core.ui.icons.TablerIcons
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
actual fun XiaomiIslandSettingRow() {
    // 非 HyperOS 设备不渲染（开关无意义，不占设置项）
    if (!HyperOsSupport.isHyperOS()) return
    val settingsRepository = koinInject<SettingsRepository>()
    val scope = rememberCoroutineScope()
    val enabled by settingsRepository.xiaomiIslandEnabled.collectAsState(initial = true)
    SaltListItem(
        title = "小米超级岛",
        subtitle = "焦点通知展示播放信息（需系统白名单，否则仅普通通知）",
        onClick = { scope.launch { settingsRepository.setXiaomiIslandEnabled(!enabled) } },
        leading = {
            SettingsIcon(icon = TablerIcons.MusicNote)
        },
        after = {
            SaltToggle(
                checked = enabled,
                onCheckedChange = { scope.launch { settingsRepository.setXiaomiIslandEnabled(it) } },
            )
        },
    )
}

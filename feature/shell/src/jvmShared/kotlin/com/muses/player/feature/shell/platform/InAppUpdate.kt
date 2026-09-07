package com.muses.player.feature.shell.platform

import androidx.compose.runtime.Composable

/**
 * 平台应用内更新抽口（Windows MSI 下载安装）：
 * - 桌面 Windows（jvmMain actual）：完整的检查→下载→安装卡片；
 * - 安卓（androidMain actual）：空实现（店内/侧载更新走系统能力，
 *   设置页共享「检查更新」外链项继续承担版本提示）。
 */
expect val supportsInAppUpdate: Boolean

@Composable
expect fun InAppUpdateSection(
    versionName: String,
    onOpenUrl: (String) -> Unit,
)

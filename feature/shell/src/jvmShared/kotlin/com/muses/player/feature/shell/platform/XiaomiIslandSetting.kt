package com.muses.player.feature.shell.platform

import androidx.compose.runtime.Composable

/**
 * 小米超级岛设置行（MeloX 式可选适配）：
 * - 安卓 actual：仅 HyperOS 设备渲染开关行（其他设备空实现，不打扰）；
 * - 桌面（jvmMain actual）：空实现（与 Windows/macOS 无关）。
 */
@Composable
expect fun XiaomiIslandSettingRow()

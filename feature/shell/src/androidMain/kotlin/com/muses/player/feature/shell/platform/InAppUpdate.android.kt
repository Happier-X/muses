package com.muses.player.feature.shell.platform

import androidx.compose.runtime.Composable

actual val supportsInAppUpdate: Boolean = false

@Composable
actual fun InAppUpdateSection(
    versionName: String,
    onOpenUrl: (String) -> Unit,
) {
    // 安卓无应用内更新：空实现，版本提示走共享「检查更新」外链项
}

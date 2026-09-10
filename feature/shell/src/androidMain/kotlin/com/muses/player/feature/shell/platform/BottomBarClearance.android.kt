package com.muses.player.feature.shell.platform

import android.provider.Settings
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
actual fun Modifier.smartBottomBarInsetsPadding(): Modifier {
    val context = LocalContext.current
    // NAVIGATION_MODE：0=手势 1=双键 2=三键；-1=读取失败（按需避让兜底）
    val mode = remember {
        Settings.Secure.getInt(context.contentResolver, "navigation_mode", -1)
    }
    return if (mode == 0) {
        // 手势导航：压缩到 8dp，避免 inset 虚高导致胶囊条离屏底过远
        padding(bottom = 8.dp)
    } else {
        // 三键/双键导航或未知：完整避让系统按钮区
        navigationBarsPadding()
    }
}

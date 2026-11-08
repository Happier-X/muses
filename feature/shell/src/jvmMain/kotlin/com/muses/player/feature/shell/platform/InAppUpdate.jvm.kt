package com.muses.player.feature.shell.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.muses.player.core.appupdate.launchWindowsInstaller
import com.muses.player.core.appupdate.windowsUpdateDir
import com.muses.player.core.ui.components.MusesSnackbar

/** 桌面：更新包为 jpackage 产出的 MSI，装到位后调 msiexec 覆盖升级（upgradeUuid 固定）。 */
@Composable
actual fun rememberUpdatePlatform(): UpdatePlatform = remember {
    UpdatePlatform(
        assetNameForTag = { tag -> "Muses-$tag.msi" },
        downloadDir = windowsUpdateDir(),
        install = { file ->
            if (launchWindowsInstaller(file)) {
                MusesSnackbar.show("安装程序已启动，请按向导完成更新")
            } else {
                MusesSnackbar.show("启动安装程序失败，请前往下载页手动安装")
            }
        },
    )
}

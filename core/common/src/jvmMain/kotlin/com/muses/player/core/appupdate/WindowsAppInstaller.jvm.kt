package com.muses.player.core.appupdate

import java.io.File

/**
 * 启动 MSI 安装（纯桌面 jvmMain：jvmShared 随 androidMain 编译，DISCARD 在安卓桩不可用）。
 *
 * detached 调起 `msiexec /i "<msi>" /passive`，UAC 由系统按需弹出；
 * MSI upgradeUuid 固定故为覆盖升级。返回 true = 已成功调起（调用方提示用户按向导完成；
 * 安装程序遇到运行中应用会自行提示关闭，无需调用方强杀进程）。
 */
fun launchWindowsInstaller(msi: File): Boolean {
    return runCatching {
        ProcessBuilder("msiexec", "/i", msi.absolutePath, "/passive")
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        true
    }.getOrDefault(false)
}

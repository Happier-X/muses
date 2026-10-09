package com.muses.player.core.appupdate

import java.io.File

/**
 * 启动 MSI 安装（纯桌面 jvmMain：jvmShared 随 androidMain 编译，DISCARD 在安卓桩不可用）。
 *
 * detached 调起 `msiexec /i "<msi>" /passive /norestart`，UAC 由系统按需弹出；
 * 即使文件被占用、安装程序判定需要重启，也禁止它重启用户电脑。
 * MSI upgradeUuid 固定故为覆盖升级。返回 true = 已成功调起（调用方提示用户按向导完成；
 * 安装程序遇到运行中应用会自行提示关闭，无需调用方强杀进程）。
 */
fun launchWindowsInstaller(msi: File): Boolean {
    return runCatching {
        windowsInstallerProcess(msi)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        true
    }.getOrDefault(false)
}

/** 单独构造进程，回归测试无需真正安装或触发系统重启。 */
internal fun windowsInstallerProcess(msi: File): ProcessBuilder =
    ProcessBuilder("msiexec", "/i", msi.absolutePath, "/passive", "/norestart")

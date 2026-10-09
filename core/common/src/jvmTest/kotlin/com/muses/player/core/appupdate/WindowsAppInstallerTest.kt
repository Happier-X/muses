package com.muses.player.core.appupdate

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WindowsAppInstallerTest {
    @Test
    fun 更新安装禁止重启且保留含空格的完整路径() {
        val installer = File("更新缓存/Muses 更新.msi")
        val command = windowsInstallerProcess(installer).command()

        assertTrue("/norestart" in command, "被动安装必须禁止 Windows Installer 自动重启")
        assertTrue("/passive" in command)
        assertEquals(installer.absolutePath, command[command.indexOf("/i") + 1])
    }
}

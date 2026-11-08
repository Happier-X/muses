package com.muses.player.feature.shell.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.muses.player.core.ui.components.MusesSnackbar
import java.io.File

/**
 * 安卓：更新包为 CI 产出的 APK（主包 / `-mi` MIUI 定制包），下载后交给系统安装器覆盖安装。
 * 只有同签名的包才能升级，故 release 包必须用发版密钥签名（CI 的 KEYSTORE_*）。
 */
@Composable
actual fun rememberUpdatePlatform(): UpdatePlatform {
    val context = LocalContext.current
    return remember(context) {
        val miuiChannel = context.packageName == "com.miui.player"
        UpdatePlatform(
            // 产物命名见 release.yml：主包 muses-v<x>.apk，MIUI 定制包 muses-v<x>-mi.apk
            assetNameForTag = { tag -> if (miuiChannel) "muses-$tag-mi.apk" else "muses-$tag.apk" },
            downloadDir = File(context.filesDir, "updates"),
            // 拷贝 20MB+ 装包不能占主线程；进度交给系统安装器的确认界面
            install = { file -> Thread { installApk(context, file) }.start() },
        )
    }
}

/**
 * 走系统 PackageInstaller 会话安装：不弹「打开方式」选择器，也不会被第三方商店（应用宝等）
 * 接管；系统要求用户确认时由 [UpdateInstallReceiver] 拉起确认界面。
 * 未授予「安装未知应用」时跳转该设置页，用户返回后点「安装更新」重试。
 */
private fun installApk(context: Context, file: File) {
    if (!context.packageManager.canRequestPackageInstalls()) {
        MusesSnackbar.show("请先允许 Muses 安装未知应用，返回后重试")
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        return
    }
    runCatching {
        val installer = context.packageManager.packageInstaller
        val sessionId = installer.createSession(
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL),
        )
        installer.openSession(sessionId).use { session ->
            session.openWrite("base.apk", 0, file.length()).use { output ->
                file.inputStream().use { it.copyTo(output) }
                session.fsync(output)
            }
            val result = Intent(context, UpdateInstallReceiver::class.java)
            val pending = android.app.PendingIntent.getBroadcast(
                context,
                sessionId,
                result,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE,
            )
            session.commit(pending.intentSender)
        }
        MusesSnackbar.show("已提交系统安装器，按提示确认即可")
    }.onFailure { MusesSnackbar.show("安装失败，请前往下载页手动安装") }
}

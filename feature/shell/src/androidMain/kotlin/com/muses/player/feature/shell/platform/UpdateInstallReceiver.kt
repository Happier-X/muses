package com.muses.player.feature.shell.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import com.muses.player.core.ui.components.MusesSnackbar

/**
 * PackageInstaller 会话结果（应用内更新）：
 * - PENDING_USER_ACTION：系统要用户确认，拉起确认界面；
 * - SUCCESS：装好后进程通常已被替换，这里兜住非覆盖场景的提示；
 * - 其余：提示失败原因，卡片里的「前往下载页」仍是兜底入口。
 */
class UpdateInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                runCatching { confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let(context::startActivity) }
            }
            PackageInstaller.STATUS_SUCCESS -> MusesSnackbar.show("安装完成")
            else -> MusesSnackbar.show(failureMessage(status))
        }
    }

    /** 系统原始报文对用户不可读，只翻常见失败码；未知码保留原文便于排查。 */
    private fun failureMessage(status: Int): String = when (status) {
        PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "签名与已安装版本不一致，需卸载后重装"
        PackageInstaller.STATUS_FAILURE_CONFLICT -> "与已安装版本冲突（多为签名不一致），需卸载后重装"
        PackageInstaller.STATUS_FAILURE_STORAGE -> "存储空间不足，安装失败"
        PackageInstaller.STATUS_FAILURE_INVALID -> "安装包无效或已损坏，请重新下载"
        PackageInstaller.STATUS_FAILURE_BLOCKED -> "安装被系统拦截"
        PackageInstaller.STATUS_FAILURE_ABORTED -> "安装已中止"
        else -> "安装失败（$status）"
    }
}

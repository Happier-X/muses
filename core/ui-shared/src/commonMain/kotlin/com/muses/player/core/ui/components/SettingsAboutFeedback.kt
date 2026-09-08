package com.muses.player.core.ui.components

import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.uishared.platform.PlatformToast
import kotlinx.coroutines.launch

/**
 * 设置页「关于 + 反馈」扩展区块（U15 上收：原安卓装配层私有实现，桌面端接入后
 * 两端设置页扩展内容完全一致）。
 *
 * 纯 UI + 平台动作回调注入：
 * - [onCheckUpdate]：检查更新（core:common [checkLatestRelease]），返回 (tag, url) 或 null=失败；
 * - [onOpenUrl]：打开新版本链接（安卓 Intent / 桌面 Desktop.browse）；
 * - [errorLogSummary] / [onDumpLogs]：报错日志摘要与全文（双端同源 ErrorLogStore）；
 * - [onCopyToClipboard]：剪贴板写入（平台动作）；
 * - 提示统一走 [PlatformToast]（安卓 Toast / 桌面 DesktopToastOverlay 浮层）。
 */
@Composable
fun SettingsAboutFeedbackContent(
    versionName: String,
    onOpenUrl: (String) -> Unit,
    onCopyToClipboard: (String) -> Unit,
    onCheckUpdate: suspend (String) -> Pair<String, String>?,
    errorLogSummary: String?,
    onDumpLogs: suspend () -> String?,
    /** 桌面 Windows 用应用内更新卡片替代此外链检查项时置 false（安卓保持 true） */
    showCheckUpdate: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    val scheme = MiuixTheme.colorScheme

    // ---- 关于 ----
    SettingsBlockTitle(text = "关于")
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .background(scheme.surface, RoundedCornerShape(12.dp))
            .padding(vertical = 4.dp),
    ) {
        // Muses 版本
        MusesListRow(
            title = "Muses",
            subtitle = "应用版本 $versionName",
            onClick = null,
            leading = {
                SettingsIcon(icon = TablerIcons.Info)
            },
        )
        // 检查更新（桌面应用内更新接管时隐藏，避免重复入口）
        if (showCheckUpdate) {
        MusesListRow(
            title = "检查更新",
            subtitle = if (checking) "正在检查更新…" else null,
            onClick = {
                if (checking) return@MusesListRow
                checking = true
                scope.launch {
                    val result = onCheckUpdate(versionName)
                    if (result == null) {
                        PlatformToast.show("检查更新失败，请稍后重试")
                    } else {
                        val (tag, url) = result
                        val latestVer = tag.removePrefix("v")
                        val currentVer = versionName
                            .removeSuffix("-miui")
                            .substringBefore("-")
                        if (compareVersionsLocal(latestVer, currentVer) <= 0) {
                            PlatformToast.show("已是最新版本")
                        } else {
                            onOpenUrl(url)
                            PlatformToast.show("发现新版本 $tag")
                        }
                    }
                    checking = false
                }
            },
            leading = {
                SettingsIcon(icon = TablerIcons.Refresh)
            },
        )
        }
    }

    // ---- 反馈 ----（任务 08-26-settings-log-viewer）
    SettingsBlockTitle(text = "反馈")
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .background(scheme.surface, RoundedCornerShape(12.dp))
            .padding(vertical = 4.dp),
    ) {
        MusesListRow(
            title = "复制报错日志",
            subtitle = errorLogSummary ?: "暂无报错记录",
            onClick = {
                scope.launch {
                    val text = onDumpLogs()
                    if (text == null) {
                        PlatformToast.show("暂无可复制的日志")
                    } else {
                        onCopyToClipboard(text)
                        PlatformToast.show("已复制报错日志")
                    }
                }
            },
            leading = {
                SettingsIcon(icon = TablerIcons.BugReport)
            },
        )
    }
}

/** 版本号比较（语义同 Web 层 compareVersions；ui-shared 不依赖 core:common，本地私有实现） */
private fun compareVersionsLocal(a: String, b: String): Int {
    val partsA = a.split('.').map { it.toIntOrNull() ?: 0 }
    val partsB = b.split('.').map { it.toIntOrNull() ?: 0 }
    val len = maxOf(partsA.size, partsB.size)
    for (i in 0 until len) {
        val va = partsA.getOrElse(i) { 0 }
        val vb = partsB.getOrElse(i) { 0 }
        if (va > vb) return 1
        if (va < vb) return -1
    }
    return 0
}

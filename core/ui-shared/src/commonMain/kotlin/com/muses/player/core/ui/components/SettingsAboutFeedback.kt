package com.muses.player.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muses.player.core.ui.components.MusesSnackbar
import com.muses.player.core.ui.icons.TablerIcons
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 设置页「关于 + 反馈」扩展区块（U15 上收：原安卓装配层私有实现，桌面端接入后
 * 两端设置页扩展内容完全一致）。
 *
 * 视觉走 miuix 官方 Settings 范式：[Card]（surface 底 + 16dp 平滑圆角）内叠放
 * [ArrowPreference] / [BasicComponent]，保持简洁的信息行与操作行。
 *
 * 纯 UI + 平台动作回调注入：
 * - [onCheckUpdate]：检查更新（core:common [checkLatestRelease]），返回 (tag, url) 或 null=失败；
 * - [onOpenUrl]：打开新版本链接（安卓 Intent / 桌面 Desktop.browse）；
 * - [onDumpLogs]：读取报错日志全文（双端同源 ErrorLogStore）；
 * - [onCopyToClipboard]：剪贴板写入（平台动作）；
 * - 提示统一走 [MusesSnackbar]（miuix Snackbar，挂在 MusesApp 根 Scaffold 槽）。
 */
@Composable
fun SettingsAboutFeedbackContent(
    versionName: String,
    onOpenUrl: (String) -> Unit,
    onCopyToClipboard: (String) -> Unit,
    onCheckUpdate: suspend (String) -> Pair<String, String>?,
    onDumpLogs: suspend () -> String?,
    /** 桌面 Windows 用应用内更新卡片替代此外链检查项时置 false（安卓保持 true） */
    showCheckUpdate: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }

    // ---- 关于 ----
    SettingsBlockTitle(text = "关于")
    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
        // Muses 版本（纯展示行，无点击）
        BasicComponent(
            title = "Muses",
            endActions = {
                Text(text = versionName.removeSuffix("-miui").substringBefore("-"))
            },
        )
        // 检查更新（桌面应用内更新接管时隐藏，避免重复入口）
        if (showCheckUpdate) {
            BasicComponent(
                title = "检查更新",
                enabled = !checking,
                endActions = {
                    Box(
                        modifier = Modifier.size(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (checking) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Icon(
                                imageVector = TablerIcons.ChevronRight,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                },
                onClick = {
                    if (checking) return@BasicComponent
                    checking = true
                    scope.launch {
                        val result = onCheckUpdate(versionName)
                        if (result == null) {
                            MusesSnackbar.show("检查更新失败，请稍后重试")
                        } else {
                            val (tag, url) = result
                            val latestVer = tag.removePrefix("v")
                            val currentVer = versionName
                                .removeSuffix("-miui")
                                .substringBefore("-")
                            if (compareVersionsLocal(latestVer, currentVer) <= 0) {
                                MusesSnackbar.show("已是最新版本")
                            } else {
                                onOpenUrl(url)
                                MusesSnackbar.show("发现新版本 $tag")
                            }
                        }
                        checking = false
                    }
                },
            )
        }
    }

    // ---- 反馈 ----（任务 08-26-settings-log-viewer）
    SettingsBlockTitle(text = "反馈")
    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
        ArrowPreference(
            title = "报错日志",
            onClick = {
                scope.launch {
                    val text = onDumpLogs()
                    if (text == null) {
                        MusesSnackbar.show("暂无可复制的日志")
                    } else {
                        onCopyToClipboard(text)
                        MusesSnackbar.show("已复制报错日志")
                    }
                }
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

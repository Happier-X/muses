package com.muses.player.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muses.player.core.ui.components.MusesSnackbar
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference

/**
 * 设置页「关于 + 反馈」扩展区块（U15 上收：原安卓装配层私有实现，桌面端接入后
 * 两端设置页扩展内容完全一致）。
 *
 * 视觉走 miuix 官方 Settings 范式：[Card]（surface 底 + 16dp 平滑圆角）内叠放
 * [ArrowPreference] / [BasicComponent]，保持简洁的信息行与操作行。
 *
 * 纯 UI + 平台动作回调注入：
 * - [onOpenUrl]：打开链接（安卓 Intent / 桌面 Desktop.browse）；
 * - [onDumpLogs]：读取报错日志全文（双端同源 ErrorLogStore）；
 * - [onCopyToClipboard]：剪贴板写入（平台动作）；
 * - 提示统一走 [MusesSnackbar]（miuix Snackbar，挂在 MusesApp 根 Scaffold 槽）。
 *
 * 检查更新不在本区块：双端已有应用内更新卡片（[com.muses.player.feature.shell.platform.InAppUpdateSection]）。
 */
@Composable
fun SettingsAboutFeedbackContent(
    versionName: String,
    onOpenUrl: (String) -> Unit,
    onCopyToClipboard: (String) -> Unit,
    onDumpLogs: suspend () -> String?,
) {
    val scope = rememberCoroutineScope()

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

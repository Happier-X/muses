package com.muses.player.feature.shell.platform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muses.player.core.appupdate.ReleaseInfo
import com.muses.player.core.data.repository.SettingsRepository
import com.muses.player.core.ui.components.MusesDialog
import com.muses.player.core.ui.components.MusesTextButton
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 启动静默检查更新：同一天只自动查一次（日期落 DataStore，跨进程重启也生效），
 * 有新版就弹窗展示更新日志，可直接下载安装。
 * 「稍后」只推迟到下次自动检查；已是最新/没网时什么都不显示。
 *
 * 按钮自绘而非用 [MusesDialog] 的双按钮：那套会把文案固定成「取消 / 确定」。
 */
@Composable
fun StartupUpdatePrompt(versionName: String) {
    val platform = rememberUpdatePlatform()
    val scope = rememberCoroutineScope()
    val settings: SettingsRepository = koinInject()
    val session = remember(platform, versionName) { UpdateSession(platform, versionName, scope) }
    var dismissed by rememberSaveable { mutableStateOf(false) }

    // 同一天只查一次：先记日期再查（网络失败也不当天重试，避免频繁请求）
    LaunchedEffect(session, settings) {
        val today = LocalDate.now().toEpochDay()
        if (settings.lastUpdateCheckDay.first() == today) return@LaunchedEffect
        settings.setLastUpdateCheckDay(today)
        session.check(silent = true)
    }

    if (dismissed) return
    when (val ui = session.ui) {
        is UpdateUi.Available -> MusesDialog(
            onDismiss = { dismissed = true },
            title = "发现新版本 ${ui.info.tag}",
        ) {
            UpdateNotes(ui.info)
            DialogActions(
                dismissText = "稍后",
                confirmText = "立即更新",
                onDismiss = { dismissed = true },
                onConfirm = { session.download(ui.info) },
            )
        }
        is UpdateUi.Downloading -> MusesDialog(
            onDismiss = { session.cancelDownload() },
            title = "正在下载更新",
            message = downloadProgressText(ui),
        ) {
            DialogActions(confirmText = "取消下载", onConfirm = { session.cancelDownload() })
        }
        is UpdateUi.Ready -> MusesDialog(
            onDismiss = { dismissed = true },
            title = "更新包已就绪",
            message = ui.file.name,
        ) {
            DialogActions(
                dismissText = "稍后",
                confirmText = "立即安装",
                onDismiss = { dismissed = true },
                // 不关弹窗：安装成功会重启进程；失败/取消后仍能在这里重试
                onConfirm = { session.install(ui.file) },
            )
        }
        else -> Unit
    }
}

@Composable
private fun DialogActions(
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String? = null,
    onDismiss: () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (dismissText != null) {
            Arrangement.spacedBy(8.dp)
        } else {
            Arrangement.spacedBy(8.dp, Alignment.End)
        },
    ) {
        if (dismissText != null) {
            MusesTextButton(
                text = dismissText,
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
        }
        MusesTextButton(
            text = confirmText,
            onClick = onConfirm,
            primary = true,
            modifier = if (dismissText != null) Modifier.weight(1f) else Modifier,
        )
    }
}

/** 更新日志：Release 正文是 markdown 原文，去掉标题/列表符号后按行展示；超高可滚动。 */
@Composable
private fun UpdateNotes(info: ReleaseInfo) {
    val notes = plainNotes(info.notes).take(1500)
    if (notes.isEmpty()) {
        Text(
            text = "暂无更新日志，可前往下载页查看。",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
        )
    } else {
        Column(Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
            Text(
                text = notes,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        }
    }
}

/** markdown 压缩成纯文本：去掉标题井号、列表符号与分隔线，保留换行结构。 */
internal fun plainNotes(markdown: String): String = markdown.lineSequence()
    .map { it.trim() }
    .filter { it.isNotEmpty() && it != "---" }
    .joinToString("\n") { line ->
        val text = line.removePrefix("### ").removePrefix("## ").removePrefix("# ")
        val body = text.removePrefix("- ").removePrefix("* ")
        if (body != text) "· $body" else body
    }
    .replace("**", "")
    .replace("`", "")

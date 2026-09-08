package com.muses.player.feature.shell.platform

import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.appupdate.WindowsReleaseInfo
import com.muses.player.core.appupdate.compareVersions
import com.muses.player.core.appupdate.downloadWindowsInstaller
import com.muses.player.core.appupdate.fetchWindowsRelease
import com.muses.player.core.appupdate.launchWindowsInstaller
import com.muses.player.core.appupdate.windowsUpdateDir
import com.muses.player.core.ui.components.MusesListRow
import com.muses.player.core.ui.components.SettingsBlockTitle
import com.muses.player.core.ui.components.SettingsIcon
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.uishared.platform.PlatformToast
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

actual val supportsInAppUpdate: Boolean = true

/** 更新卡片状态机：空闲 → 检查中 → 有新版 → 下载中 → 待安装（失败一律 Toast 后回退）。 */
private sealed interface UpdateUi {
    data object Idle : UpdateUi
    data object Checking : UpdateUi
    data class Available(val info: WindowsReleaseInfo) : UpdateUi
    data class Downloading(val info: WindowsReleaseInfo, val downloaded: Long, val total: Long) : UpdateUi
    data class Ready(val info: WindowsReleaseInfo, val file: File) : UpdateUi
}

private fun normalizeVersion(versionName: String): String =
    versionName.removeSuffix("-miui").substringBefore("-")

private fun formatMB(bytes: Long): String =
    if (bytes <= 0) "未知大小" else "%.1f MB".format(bytes / 1024f / 1024f)

@Composable
actual fun InAppUpdateSection(
    versionName: String,
    onOpenUrl: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var ui by remember { mutableStateOf<UpdateUi>(UpdateUi.Idle) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    val scheme = MiuixTheme.colorScheme

    fun check() {
        if (ui != UpdateUi.Idle) return
        scope.launch {
            ui = UpdateUi.Checking
            val info = fetchWindowsRelease(versionName)
            if (info == null) {
                PlatformToast.show("检查更新失败，请稍后重试")
                ui = UpdateUi.Idle
                return@launch
            }
            val latest = info.tag.removePrefix("v")
            if (compareVersions(latest, normalizeVersion(versionName)) <= 0) {
                PlatformToast.show("已是最新版本")
                ui = UpdateUi.Idle
            } else {
                ui = UpdateUi.Available(info)
            }
        }
    }

    fun download(info: WindowsReleaseInfo) {
        downloadJob?.cancel()
        downloadJob = scope.launch {
            val dest = File(windowsUpdateDir(), info.msiName)
            ui = UpdateUi.Downloading(info, 0L, info.msiSizeBytes)
            var lastReport = 0L
            val result = downloadWindowsInstaller(info.msiUrl, dest, info.msiSizeBytes) { downloaded, total ->
                // IO 线程回调：节流（256KB）后切主线程刷新，避免每 64KB 包都重组
                if (downloaded - lastReport > 256 * 1024 || (total > 0 && downloaded >= total)) {
                    lastReport = downloaded
                    scope.launch { ui = UpdateUi.Downloading(info, downloaded, total) }
                }
            }
            result
                .onSuccess {
                    PlatformToast.show("下载完成，点击安装")
                    ui = UpdateUi.Ready(info, it)
                }
                .onFailure {
                    // 取消不提示（用户主动点取消下载，状态已回退）
                    if (downloadJob?.isCancelled == false) {
                        PlatformToast.show("下载失败，请重试")
                        ui = UpdateUi.Available(info)
                    }
                }
        }
    }

    SettingsBlockTitle(text = "应用更新")
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .background(scheme.surface, RoundedCornerShape(12.dp))
            .padding(vertical = 4.dp),
    ) {
        // 主行：检查更新
        MusesListRow(
            title = "检查更新",
            subtitle = when (ui) {
                is UpdateUi.Checking -> "正在检查更新…"
                is UpdateUi.Downloading -> "正在下载更新包…"
                else -> "当前版本 $versionName"
            },
            onClick = if (ui == UpdateUi.Idle) ::check else null,
            leading = {
                SettingsIcon(icon = TablerIcons.Refresh)
            },
        )

        // 有新版：版本信息 + 更新内容预览 + 下载/下载页入口
        val available = ui as? UpdateUi.Available
        if (available != null) {
            val info = available.info
            MusesListRow(
                title = "发现新版本 ${info.tag}",
                subtitle = "安装包约 ${formatMB(info.msiSizeBytes)}",
                onClick = null,
                leading = {
                    SettingsIcon(icon = TablerIcons.Info)
                },
            )
            val notes = info.notes.trim().take(400)
            if (notes.isNotEmpty()) {
                Text(
                    text = notes,
                    fontSize = 13.sp,
                    lineHeight = (13f * 1.5f).sp,
                    color = scheme.onBackgroundVariant,
                    maxLines = 5,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            MusesListRow(
                title = "下载更新",
                subtitle = "下载完成后可直接安装",
                onClick = { download(info) },
                leading = {
                    SettingsIcon(icon = TablerIcons.Download)
                },
            )
            MusesListRow(
                title = "前往下载页",
                subtitle = "浏览器打开 Release 页面手动下载",
                onClick = { onOpenUrl(info.htmlUrl) },
                leading = {
                    SettingsIcon(icon = TablerIcons.Info)
                },
            )
        }

        // 下载中：进度条 + 百分比 + 取消
        val downloading = ui as? UpdateUi.Downloading
        if (downloading != null) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                val total = downloading.total
                val percent = if (total > 0) (downloading.downloaded * 100 / total).toInt() else -1
                Text(
                    text = if (percent >= 0) {
                        "正在下载 ${formatMB(downloading.downloaded)} / ${formatMB(total)}（$percent%）"
                    } else {
                        "正在下载 ${formatMB(downloading.downloaded)}"
                    },
                    fontSize = 13.sp,
                    color = scheme.onBackgroundVariant,
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = if (total > 0) downloading.downloaded.toFloat() / total else 0f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            MusesListRow(
                title = "取消下载",
                onClick = {
                    downloadJob?.cancel()
                    downloadJob = null
                    ui = UpdateUi.Available(downloading.info)
                },
                leading = {
                    SettingsIcon(icon = TablerIcons.Refresh)
                },
            )
        }

        // 待安装：安装入口 + 重新下载
        val ready = ui as? UpdateUi.Ready
        if (ready != null) {
            MusesListRow(
                title = "安装更新",
                subtitle = "${ready.file.name}（${formatMB(ready.file.length())}）已就绪",
                onClick = {
                    if (launchWindowsInstaller(ready.file)) {
                        PlatformToast.show("安装程序已启动，请按向导完成更新")
                    } else {
                        PlatformToast.show("启动安装程序失败，请前往下载页手动安装")
                        onOpenUrl(ready.info.htmlUrl)
                    }
                },
                leading = {
                    SettingsIcon(icon = TablerIcons.CheckCircle)
                },
            )
            MusesListRow(
                title = "重新下载",
                onClick = {
                    ready.file.delete()
                    download(ready.info)
                },
                leading = {
                    SettingsIcon(icon = TablerIcons.Download)
                },
            )
        }
    }
}

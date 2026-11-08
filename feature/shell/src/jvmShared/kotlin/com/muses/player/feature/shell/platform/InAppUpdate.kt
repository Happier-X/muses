package com.muses.player.feature.shell.platform

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.appupdate.ReleaseInfo
import com.muses.player.core.appupdate.compareVersions
import com.muses.player.core.appupdate.downloadReleaseFile
import com.muses.player.core.appupdate.fetchRelease
import com.muses.player.core.ui.components.MusesSnackbar
import com.muses.player.core.ui.components.SettingsBlockTitle
import com.muses.player.core.ui.components.SettingsIcon
import com.muses.player.core.ui.icons.TablerIcons
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.muses.player.core.ui.components.MarqueeText as Text

/**
 * 平台差异（安卓/桌面的应用内更新只在这三处不同）：
 * - [assetNameForTag] 安装包命名约定：桌面 `Muses-<tag>.msi`，安卓 `<tag>` 主包 / `-mi` 定制包；
 * - [downloadDir] 更新包落盘目录；
 * - [install] 启动安装：桌面 msiexec，安卓系统安装器（未授权「未知来源」时引导授权）。
 * 安装成败提示由平台给出（卡片内另有「前往下载页」兜底入口）。
 */
class UpdatePlatform(
    /** 由 tag 推出资产文件名（与 release.yml 产物一致；API 限流时按它拼直链） */
    val assetNameForTag: (String) -> String,
    val downloadDir: File,
    val install: (File) -> Unit,
)

@Composable
expect fun rememberUpdatePlatform(): UpdatePlatform

/** 更新流程状态机：空闲 → 检查中 → 有新版 → 下载中 → 待安装（失败一律 Toast 后回退）。 */
sealed interface UpdateUi {
    data object Idle : UpdateUi
    data object Checking : UpdateUi
    data class Available(val info: ReleaseInfo) : UpdateUi
    data class Downloading(val info: ReleaseInfo, val downloaded: Long, val total: Long) : UpdateUi
    data class Ready(val info: ReleaseInfo, val file: File) : UpdateUi
}

private fun normalizeVersion(versionName: String): String =
    versionName.removeSuffix("-miui").substringBefore("-")

internal fun formatMB(bytes: Long): String =
    if (bytes <= 0) "未知大小" else "%.1f MB".format(bytes / 1024f / 1024f)

/** 下载进度文案（卡片与启动弹窗共用）。 */
internal fun downloadProgressText(ui: UpdateUi.Downloading): String {
    val percent = if (ui.total > 0) (ui.downloaded * 100 / ui.total).toInt() else -1
    return if (percent >= 0) {
        "正在下载 ${formatMB(ui.downloaded)} / ${formatMB(ui.total)}（$percent%）"
    } else {
        "正在下载 ${formatMB(ui.downloaded)}"
    }
}

/**
 * 检查 → 下载 → 安装的状态机（设置卡片与启动弹窗各持一份，流程与文案同源）：
 * 静默检查不弹「已是最新 / 检查失败」，只在有新版时进入 [UpdateUi.Available]。
 */
class UpdateSession(
    private val platform: UpdatePlatform,
    private val versionName: String,
    private val scope: CoroutineScope,
) {
    var ui: UpdateUi by mutableStateOf(UpdateUi.Idle)
        private set

    private var downloadJob: Job? = null

    fun check(silent: Boolean = false) {
        if (ui != UpdateUi.Idle) return
        scope.launch {
            ui = UpdateUi.Checking
            val info = fetchRelease(versionName, platform.assetNameForTag)
            if (info == null) {
                if (!silent) MusesSnackbar.show("检查更新失败，请稍后重试")
                ui = UpdateUi.Idle
                return@launch
            }
            val latest = info.tag.removePrefix("v")
            if (compareVersions(latest, normalizeVersion(versionName)) <= 0) {
                if (!silent) MusesSnackbar.show("已是最新版本")
                ui = UpdateUi.Idle
            } else {
                ui = UpdateUi.Available(info)
            }
        }
    }

    fun download(info: ReleaseInfo) {
        downloadJob?.cancel()
        downloadJob = scope.launch {
            val dest = File(platform.downloadDir, info.name)
            ui = UpdateUi.Downloading(info, 0L, info.sizeBytes)
            var lastReport = 0L
            val result = downloadReleaseFile(info.url, dest, info.sizeBytes) { downloaded, total ->
                // IO 线程回调：节流（256KB）后切主线程刷新，避免每 64KB 包都重组
                if (downloaded - lastReport > 256 * 1024 || (total > 0 && downloaded >= total)) {
                    lastReport = downloaded
                    scope.launch { ui = UpdateUi.Downloading(info, downloaded, total) }
                }
            }
            result
                .onSuccess {
                    MusesSnackbar.show("下载完成，点击安装")
                    ui = UpdateUi.Ready(info, it)
                }
                .onFailure {
                    // 取消不提示（用户主动点取消下载，状态已回退）
                    if (downloadJob?.isCancelled == false) {
                        MusesSnackbar.show("下载失败，请重试")
                        ui = UpdateUi.Available(info)
                    }
                }
        }
    }

    fun cancelDownload() {
        val info = (ui as? UpdateUi.Downloading)?.info ?: return
        downloadJob?.cancel()
        downloadJob = null
        ui = UpdateUi.Available(info)
    }

    fun install(file: File) = platform.install(file)
}

/**
 * 应用内更新卡片（jvmShared 双端共享）：检查 GitHub Release → 下载安装包 → 交给平台安装。
 * 平台差异经 [rememberUpdatePlatform] 注入，流程与文案两端一致。
 */
@Composable
fun InAppUpdateSection(
    versionName: String,
    onOpenUrl: (String) -> Unit,
) {
    val platform = rememberUpdatePlatform()
    val scope = rememberCoroutineScope()
    val session = remember(platform, versionName) { UpdateSession(platform, versionName, scope) }
    val ui = session.ui
    val scheme = MiuixTheme.colorScheme

    SettingsBlockTitle(text = "应用更新")
    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
        // 主行：检查更新
        ArrowPreference(
            title = "检查更新",
            summary = when (ui) {
                is UpdateUi.Checking -> "正在检查更新…"
                is UpdateUi.Downloading -> "正在下载更新包…"
                else -> "当前版本 $versionName"
            },
            enabled = ui == UpdateUi.Idle,
            startAction = { SettingsIcon(icon = TablerIcons.Refresh) },
            onClick = if (ui == UpdateUi.Idle) ({ session.check() }) else null,
        )

        // 有新版：版本信息 + 更新内容预览 + 下载/下载页入口
        val available = ui as? UpdateUi.Available
        if (available != null) {
            val info = available.info
            BasicComponent(
                title = "发现新版本 ${info.tag}",
                // API 退路拿不到包大小（-1），不显示「约 未知大小」
                summary = if (info.sizeBytes > 0) "安装包约 ${formatMB(info.sizeBytes)}" else "可直接下载安装",
                startAction = { SettingsIcon(icon = TablerIcons.Info) },
            )
            val notes = plainNotes(info.notes).take(400)
            if (notes.isNotEmpty()) {
                Text(
                    text = notes,
                    style = MiuixTheme.textStyles.footnote1,
                    lineHeight = (13f * 1.5f).sp,
                    color = scheme.onBackgroundVariant,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            ArrowPreference(
                title = "下载更新",
                summary = "下载完成后可直接安装",
                startAction = { SettingsIcon(icon = TablerIcons.Download) },
                onClick = { session.download(info) },
            )
            ArrowPreference(
                title = "前往下载页",
                summary = "浏览器打开 Release 页面手动下载",
                startAction = { SettingsIcon(icon = TablerIcons.Share) },
                onClick = { onOpenUrl(info.htmlUrl) },
            )
        }

        // 下载中：进度条 + 百分比 + 取消
        val downloading = ui as? UpdateUi.Downloading
        if (downloading != null) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = downloadProgressText(downloading),
                    style = MiuixTheme.textStyles.footnote1,
                    color = scheme.onBackgroundVariant,
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = if (downloading.total > 0) downloading.downloaded.toFloat() / downloading.total else 0f,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
            }
            ArrowPreference(
                title = "取消下载",
                startAction = { SettingsIcon(icon = TablerIcons.Close) },
                onClick = { session.cancelDownload() },
            )
        }

        // 待安装：安装入口 + 重新下载
        val ready = ui as? UpdateUi.Ready
        if (ready != null) {
            ArrowPreference(
                title = "安装更新",
                summary = "${ready.file.name}（${formatMB(ready.file.length())}）已就绪",
                startAction = { SettingsIcon(icon = TablerIcons.CheckCircle) },
                onClick = { session.install(ready.file) },
            )
            ArrowPreference(
                title = "重新下载",
                startAction = { SettingsIcon(icon = TablerIcons.Download) },
                onClick = {
                    ready.file.delete()
                    session.download(ready.info)
                },
            )
        }
    }
}

package com.muses.player.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muses.player.core.lxsdk.LxQuality
import com.muses.player.core.model.Source
import com.muses.player.core.model.SourceType
import com.muses.player.core.model.decodeWebDavSourcePaths
import com.muses.player.core.model.download.DownloadStatus
import com.muses.player.core.model.download.DownloadTarget
import com.muses.player.core.model.download.DownloadTargetKind
import com.muses.player.core.ui.components.MusesActionItem
import com.muses.player.core.ui.components.MusesActionsSheet
import com.muses.player.core.ui.components.MusesTopBar
import com.muses.player.core.ui.components.MusesRefreshableContent
import com.muses.player.core.ui.theme.LocalBottomChromePadding
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import com.muses.player.core.ui.components.MarqueeText as Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 下载队列：只管任务本身。
 *
 * 保存位置、下载品质这类「全局默认」收在「设置 → 下载」；这里只保留队列操作
 * （全部下载 / 全部暂停 / 单任务继续暂停）、进度与失败原因。
 */
@Composable
fun DownloadQueueScreen(onBack: () -> Unit, manager: DownloadManager = koinInject()) {
    val tasks by manager.tasks.collectAsState()
    val sources by manager.availableSources.collectAsState()
    val defaultTarget by manager.defaultTarget.collectAsState()
    val scrapeUploads by manager.pendingScrapeUploads.collectAsState()
    val uploadingScrapeId by manager.uploadingScrapeId.collectAsState()
    var targetPickerId by remember { mutableStateOf<String?>(null) }
    var targetPickerOpen by remember { mutableStateOf(false) }
    var actionsId by remember { mutableStateOf<String?>(null) }
    var qualityId by remember { mutableStateOf<String?>(null) }
    val pending = tasks.filter { !it.status.active && it.status != DownloadStatus.COMPLETED }
    val active = tasks.any { it.status.active } || uploadingScrapeId != null
    val scheme = MiuixTheme.colorScheme
    val targets = remember(sources) { downloadTargetOptions(sources) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = scheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { MusesTopBar(title = "下载", onBack = onBack) },
    ) { padding ->
        MusesRefreshableContent(onRefresh = manager::refresh, modifier = Modifier.padding(padding)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 8.dp,
                bottom = 16.dp + LocalBottomChromePadding.current,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item("controls") {
                Card {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(onClick = { manager.pause() }, enabled = active, modifier = Modifier.weight(1f)) {
                            Text("全部暂停")
                        }
                        Button(
                            onClick = { manager.start(pending.map { it.id }.toSet(), defaultTarget) },
                            enabled = pending.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColorsPrimary(),
                        ) {
                            Text("全部下载")
                        }
                    }
                }
            }

            if (tasks.any { it.status == DownloadStatus.PENDING_UPLOAD } || scrapeUploads.isNotEmpty()) {
                item("upload-controls") {
                    Card {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("本地已保存，待上传", style = MiuixTheme.textStyles.title4)
                            Text("上传失败的音频会保留在本机。补传直接使用已保存的文件，也可以导出后自行上传。", color = scheme.onSurfaceVariantSummary)
                            Button(onClick = {
                                manager.start(tasks.filter { it.status == DownloadStatus.PENDING_UPLOAD }.map { it.id }.toSet(), defaultTarget)
                                manager.retryScrapeUploads(scrapeUploads.map { it.id }.toSet())
                            }, enabled = !active && uploadingScrapeId == null) { Text("全部补传") }
                        }
                    }
                }
            }
            items(scrapeUploads, key = { "scrape-${it.id}" }) { task ->
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(task.title, style = MiuixTheme.textStyles.title4)
                        Text(if (uploadingScrapeId == task.id) "刮削 · 上传与核验中" else "刮削 · 本地已保存，待上传")
                        task.error?.let { Text(it, color = scheme.error) }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { manager.exportScrapeUpload(task.id) }, enabled = uploadingScrapeId != task.id,
                                modifier = Modifier.weight(1f)) { Text("导出到本地") }
                            Button(onClick = { manager.retryScrapeUploads(setOf(task.id)) }, enabled = uploadingScrapeId == null,
                                modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColorsPrimary()) { Text("补传") }
                        }
                    }
                }
            }

            if (tasks.isEmpty()) {
                item("empty") {
                    Text(
                        text = "在歌曲操作里添加到下载队列，再到这里开始下载。保存位置与下载品质在「设置 → 下载」中调整。",
                        modifier = Modifier.padding(16.dp),
                        color = scheme.onSurfaceVariantSummary,
                    )
                }
            }

            items(tasks, key = { it.id }) { task ->
                Card {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            task.track.title,
                            marqueeEnabled = com.muses.player.core.ui.components.LocalSongMarqueeState.current.matches(task.track.id),
                            style = MiuixTheme.textStyles.title4,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            listOfNotNull(task.track.artist, task.track.album).joinToString(" · "),
                            color = scheme.onSurfaceVariantSummary,
                        )
                        // 实际请求档位与队列里选的档位不一致时，写明已经降档，避免用户以为拿到了原档位
                        val label = qualityLabel(task.quality)
                        val requested = task.requestedQuality
                        Text(
                            text = if (requested != null && requested != task.quality)
                                "$label → ${qualityLabel(requested)}（已降低请求档位）" else label,
                            color = scheme.onSurfaceVariantSummary,
                        )
                        Text(
                            (task.target ?: defaultTarget).displayLabel(sources),
                            color = scheme.onSurfaceVariantSummary,
                        )
                        // 旧队列没有失败阶段，用已有传输总量兼容判断。
                        val failedTransfer = task.status == DownloadStatus.FAILED &&
                            (task.failureStage == DownloadStatus.UPLOADING || task.failureStage == DownloadStatus.SAVING ||
                                (task.failureStage == null && task.transferTotalBytes != null))
                        val transfer = task.status == DownloadStatus.UPLOADING || task.status == DownloadStatus.SAVING || task.status == DownloadStatus.PENDING_UPLOAD || failedTransfer
                        val bytes = if (transfer) task.transferredBytes else task.downloadedBytes
                        val total = if (transfer) task.transferTotalBytes else task.totalBytes
                        Text(
                            (if (failedTransfer) {
                                if (task.target?.kind == DownloadTargetKind.WEBDAV) "上传失败" else "保存失败"
                            } else if (task.skippedExisting) "同名歌曲，已跳过"
                            else if (task.status == DownloadStatus.UPLOADING) task.transferMessage ?: statusLabel(task.status)
                            else statusLabel(task.status)) +
                                if (bytes > 0) " · ${formatBytes(bytes)}" + (total?.let { " / ${formatBytes(it)}" } ?: "") else "",
                        )
                        if (task.status.active) {
                            LinearProgressIndicator(
                                progress = if (task.status == DownloadStatus.PREPARING || task.status == DownloadStatus.METADATA ||
                                    (task.status == DownloadStatus.UPLOADING && task.transferMessage != null)) {
                                    null
                                } else {
                                    total?.takeIf { it > 0 }?.let { (bytes.toFloat() / it).coerceIn(0f, 1f) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        task.error?.let { Text(it, color = scheme.error) }
                        task.warnings.forEach { Text(it, color = scheme.onSurfaceVariantSummary) }
                        if (task.status == DownloadStatus.COMPLETED && !task.skippedExisting) {
                            Text("保存成功", color = scheme.primary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Button(onClick = { actionsId = task.id }, modifier = Modifier.weight(1f)) { Text("更多") }
                            if (task.status != DownloadStatus.COMPLETED) {
                                Button(
                                    onClick = {
                                        if (task.status.active) manager.pause(task.id)
                                        else manager.start(setOf(task.id), defaultTarget)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColorsPrimary(),
                                ) {
                                    Text(
                                        when {
                                            task.status.active -> "暂停"
                                            task.status == DownloadStatus.PENDING_UPLOAD -> "补传"
                                            task.status == DownloadStatus.FAILED -> "重试"
                                            task.status == DownloadStatus.PAUSED -> "继续"
                                            else -> "下载"
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    val selected = tasks.firstOrNull { it.id == actionsId }
    MusesActionsSheet(
        opened = selected != null,
        onDismiss = { actionsId = null },
        label = "下载任务",
        items = buildList {
            if (selected != null && !selected.status.active && selected.status != DownloadStatus.COMPLETED) {
                add(MusesActionItem("更改保存位置") { targetPickerId = selected.id; actionsId = null; targetPickerOpen = true })
                if (selected.localAudioPath == null) add(MusesActionItem("更改下载品质") { qualityId = selected.id; actionsId = null })
                if (selected.localAudioPath != null) add(MusesActionItem("导出到本地") { manager.exportDownload(selected.id); actionsId = null })
            }
            selected?.let { task -> add(MusesActionItem("移出队列") { manager.remove(task.id); actionsId = null }) }
        },
    )
    MusesActionsSheet(
        opened = targetPickerOpen,
        onDismiss = { targetPickerOpen = false },
        label = "保存位置",
        items = targets.map { target ->
            MusesActionItem(target.label) {
                targetPickerId?.let { manager.configure(it, target = target) }
                targetPickerOpen = false
            }
        },
    )
    MusesActionsSheet(
        opened = qualityId != null,
        onDismiss = { qualityId = null },
        label = "下载品质",
        items = LxQuality.entries.map { quality ->
            MusesActionItem(quality.label) {
                qualityId?.let { manager.configure(it, quality = quality.key) }
                qualityId = null
            }
        },
    )
}

/**
 * 可选保存位置：设备下载目录 + 每个本地/WebDAV 音源。
 * 设置页与队列页共用同一份选项，避免两处口径不一致。
 */
internal fun downloadTargetOptions(sources: List<Source>): List<DownloadTarget> =
    listOf(DownloadTarget()) + sources.flatMap { source ->
        when (source.type) {
            SourceType.LOCAL -> source.path?.takeIf { it.isNotBlank() }
                ?.let { listOf(DownloadTarget(DownloadTargetKind.LOCAL, source.id, it, source.name)) }
                .orEmpty()
            SourceType.WEBDAV -> {
                val directories = decodeWebDavSourcePaths(source.path).ifEmpty { listOf("/") }
                directories.map { directory ->
                    // 直接显示音源名称；同一音源配了多个目录时才补路径，避免两条同名项分不清
                    val label = if (directories.size == 1) source.name else "${source.name} · $directory"
                    DownloadTarget(DownloadTargetKind.WEBDAV, source.id, directory, label)
                }
            }
            else -> emptyList()
        }
    }

/**
 * 展示用名称：按当前音源配置重新算，
 * 这样早期存进队列/设置里的「音源名 · 路径」旧标签也会跟着显示成音源名。
 */
internal fun DownloadTarget.displayLabel(sources: List<Source>): String =
    downloadTargetOptions(sources)
        .firstOrNull { it.kind == kind && it.sourceId == sourceId && it.directory == directory }
        ?.label
        ?: label

internal fun qualityLabel(key: String): String = LxQuality.fromKey(key)?.label ?: key

internal fun formatBytes(bytes: Long): String =
    if (bytes < 1024 * 1024) "${bytes / 1024} KB" else "%.1f MB".format(bytes / (1024.0 * 1024))

internal fun statusLabel(status: DownloadStatus): String = when (status) {
    DownloadStatus.PENDING_UPLOAD -> "本地已保存，待上传"
    DownloadStatus.WAITING -> "待下载"
    DownloadStatus.PREPARING -> "准备下载"
    DownloadStatus.DOWNLOADING -> "下载中"
    DownloadStatus.METADATA -> "保存歌曲信息"
    DownloadStatus.SAVING -> "保存中"
    DownloadStatus.UPLOADING -> "上传中"
    DownloadStatus.PAUSED -> "已暂停"
    DownloadStatus.FAILED -> "下载失败"
    DownloadStatus.COMPLETED -> "已完成"
}

package com.muses.player.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muses.player.core.lxsdk.LxQuality
import com.muses.player.core.model.Source
import com.muses.player.core.model.SourceType
import com.muses.player.core.model.decodeWebDavSourcePaths
import com.muses.player.core.model.download.DownloadStatus
import com.muses.player.core.model.download.DownloadTarget
import com.muses.player.core.model.download.DownloadTargetKind
import com.muses.player.core.model.download.downloadTargetAvailable
import com.muses.player.core.ui.components.MusesActionItem
import com.muses.player.core.ui.components.MusesActionsSheet
import com.muses.player.core.ui.components.MusesIconButton
import com.muses.player.core.ui.components.MusesListRow
import com.muses.player.core.ui.components.MusesIconButtonSize
import com.muses.player.core.ui.components.SongListLayout
import com.muses.player.core.ui.components.MusesCover
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.ui.components.MusesTopBar
import com.muses.player.core.ui.components.MusesRefreshableContent
import com.muses.player.core.ui.theme.LocalBottomChromePadding
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
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
    var expandedTasks by remember { mutableStateOf(emptySet<String>()) }
    val pending = tasks.filter { !it.status.active && it.status != DownloadStatus.COMPLETED }
    val active = tasks.any { it.status.active } || uploadingScrapeId != null
    val scheme = MiuixTheme.colorScheme
    val favoritesStore = koinInject<com.muses.player.core.data.repository.FavoriteSongsStore>()
    val favorites by favoritesStore.state.collectAsState()
    val preparingFavorites by manager.preparingFavoriteDownloads.collectAsState()
    val missingFavorites = favorites.missingOnlineSongs
    val targets = remember(sources) { downloadTargetOptions(sources) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = scheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { MusesTopBar(title = "下载", onBack = onBack) },
    ) { padding ->
        MusesRefreshableContent(onRefresh = manager::refresh, modifier = Modifier.padding(padding)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 16.dp + LocalBottomChromePadding.current,
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    item("controls") {
                        Row(Modifier.fillMaxWidth().padding(start = 12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("全部任务 · ${tasks.size + scrapeUploads.size}", modifier = Modifier.weight(1f),
                                style = MiuixTheme.textStyles.footnote1, color = scheme.onSurfaceVariantSummary)
                            MusesIconButton(contentDescription = if (active) "全部暂停" else "全部下载",
                                imageVector = if (active) TablerIcons.Pause else TablerIcons.Download,
                                enabled = active || pending.isNotEmpty(), tint = scheme.primary,
                                onClick = {
                                    if (active) manager.pause()
                                    else manager.start(pending.map { it.id }.toSet(), defaultTarget)
                                })
                        }
                    }
                    item("favorites") {
                        MusesListRow(title = "下载我喜欢", subtitle = when {
                            preparingFavorites -> "正在准备…"
                            !favorites.loaded -> "正在读取…"
                            missingFavorites.isEmpty() -> "已全部入库"
                            else -> "${missingFavorites.size} 首未入库"
                        }, leading = {
                            Icon(TablerIcons.Heart, null, Modifier.padding(end = 12.dp).size(24.dp),
                                tint = scheme.onBackgroundVariant)
                        }, after = {
                            MusesIconButton(onClick = { manager.downloadFavorites(favoritesStore) },
                                imageVector = TablerIcons.Download, contentDescription = "下载未入库的喜欢歌曲",
                                enabled = favorites.loaded && missingFavorites.isNotEmpty() && !preparingFavorites,
                                tint = scheme.primary)
                        }, dividers = false)
                        top.yukonga.miuix.kmp.basic.HorizontalDivider(Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }

                    if (tasks.any { it.status == DownloadStatus.PENDING_UPLOAD } || scrapeUploads.isNotEmpty()) {
                        item("upload-controls") {
                            Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("待补传 · ${tasks.count { it.status == DownloadStatus.PENDING_UPLOAD } + scrapeUploads.size}",
                                    modifier = Modifier.weight(1f), style = MiuixTheme.textStyles.footnote1, color = scheme.onSurfaceVariantSummary)
                                MusesIconButton(onClick = {
                                    manager.start(tasks.filter { it.status == DownloadStatus.PENDING_UPLOAD }.map { it.id }.toSet(), defaultTarget)
                                    manager.retryScrapeUploads(scrapeUploads.map { it.id }.toSet())
                                }, enabled = !active, imageVector = TablerIcons.Upload, contentDescription = "全部补传", tint = scheme.primary)
                            }
                        }
                    }
                    items(scrapeUploads, key = { "scrape-${it.id}" }) { task ->
                        Column {
                            MusesListRow(title = task.title,
                                subtitle = if (uploadingScrapeId == task.id) "刮削 · 上传与核验中" else "刮削 · 本地已保存，待上传",
                                songLayout = true,
                                after = {
                                    MusesIconButton(onClick = { manager.exportScrapeUpload(task.id) }, enabled = uploadingScrapeId != task.id,
                                        imageVector = TablerIcons.Download, contentDescription = "导出到本地", size = MusesIconButtonSize.SM)
                                    MusesIconButton(onClick = { manager.retryScrapeUploads(setOf(task.id)) }, enabled = uploadingScrapeId == null,
                                        imageVector = TablerIcons.Upload, contentDescription = "补传", size = MusesIconButtonSize.SM, tint = scheme.primary)
                                })
                            task.error?.let { Text(it, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MiuixTheme.textStyles.footnote1, color = scheme.error) }
                        }
                    }

                    if (tasks.isEmpty() && scrapeUploads.isEmpty()) {
                        item("empty") {
                            Text("还没有下载任务", modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                                style = MiuixTheme.textStyles.footnote1, color = scheme.onSurfaceVariantSummary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }

                    val groups = listOf(
                        "进行中" to tasks.filter { it.status.active },
                        "待处理" to tasks.filter { !it.status.active && it.status != DownloadStatus.COMPLETED },
                        "已完成" to tasks.filter { it.status == DownloadStatus.COMPLETED },
                    )
                    groups.filter { it.second.isNotEmpty() }.forEach { (title, entries) ->
                        item("section-$title") {
                            Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(title, style = MiuixTheme.textStyles.footnote1, color = scheme.onSurfaceVariantSummary)
                                Text(entries.size.toString(), style = MiuixTheme.textStyles.footnote1,
                                    color = scheme.onSurfaceVariantSummary)
                            }
                        }
                        items(entries, key = { it.id }) { task ->
                            DownloadTaskRow(task, defaultTarget, sources,
                                expanded = task.id in expandedTasks,
                                onToggleDetails = { expandedTasks = if (task.id in expandedTasks) expandedTasks - task.id else expandedTasks + task.id },
                                onAction = {
                                    if (task.status.active) manager.pause(task.id)
                                    else manager.start(setOf(task.id), defaultTarget)
                                },
                                onMore = { actionsId = task.id })
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
        songInfo = selected?.track?.let { com.muses.player.core.ui.components.MusesActionSongInfo(it.title, it.artist, it.album, it.coverUri) },
        items = buildList {
            selected?.let { task ->
                add(MusesActionItem(if (task.id in expandedTasks) "收起详情" else "查看详情") {
                    expandedTasks = if (task.id in expandedTasks) expandedTasks - task.id else expandedTasks + task.id
                    actionsId = null
                })
            }
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

/** 先展示歌曲和进度，品质与保存位置作为次要信息，异常单独保留。 */
@Composable
private fun DownloadTaskRow(
    task: com.muses.player.core.model.download.DownloadTask,
    defaultTarget: DownloadTarget,
    sources: List<Source>,
    expanded: Boolean,
    onToggleDetails: () -> Unit,
    onAction: () -> Unit,
    onMore: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    val failedTransfer = task.status == DownloadStatus.FAILED &&
        (task.failureStage == DownloadStatus.UPLOADING || task.failureStage == DownloadStatus.SAVING ||
            (task.failureStage == null && task.transferTotalBytes != null))
    val transfer = task.status == DownloadStatus.UPLOADING || task.status == DownloadStatus.SAVING ||
        task.status == DownloadStatus.PENDING_UPLOAD || failedTransfer
    val bytes = if (transfer) task.transferredBytes else task.downloadedBytes
    val total = if (transfer) task.transferTotalBytes else task.totalBytes
    val progress = if (task.status == DownloadStatus.PREPARING || task.status == DownloadStatus.METADATA ||
        (task.status == DownloadStatus.UPLOADING && task.transferMessage != null)) null
        else total?.takeIf { it > 0 }?.let { (bytes.toFloat() / it).coerceIn(0f, 1f) }
    val status = when {
        failedTransfer -> if (task.target?.kind == DownloadTargetKind.WEBDAV) "上传失败" else "保存失败"
        task.skippedExisting -> "同名歌曲，已跳过"
        task.status == DownloadStatus.UPLOADING -> task.transferMessage ?: statusLabel(task.status)
        else -> statusLabel(task.status)
    }
    val statusColor = when {
        task.status == DownloadStatus.FAILED -> scheme.error
        task.status.active || task.status == DownloadStatus.COMPLETED -> scheme.primary
        else -> scheme.onSurfaceVariantSummary
    }
    val marquee = com.muses.player.core.ui.components.LocalSongMarqueeState.current.matches(task.track.id)
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        val subtitle = listOfNotNull(task.track.artist, task.track.album).filter { it.isNotBlank() }.joinToString(" · ")
        MusesListRow(title = task.track.title, subtitle = subtitle, songLayout = true,
            songId = task.track.id, marqueeEnabled = marquee, dividers = false,
            onClick = onToggleDetails,
            leading = {
                MusesCover(task.track.coverUri, size = SongListLayout.coverSize,
                    modifier = Modifier.padding(end = SongListLayout.coverGap), contentDescription = task.track.title + "封面")
            }, after = {
                if (task.status != DownloadStatus.COMPLETED) {
                    val action = when {
                        task.status.active -> "暂停"
                        task.status == DownloadStatus.PENDING_UPLOAD -> "补传"
                        task.status == DownloadStatus.FAILED -> "重试"
                        task.status == DownloadStatus.PAUSED -> "继续"
                        else -> "下载"
                    }
                    MusesIconButton(onClick = onAction, size = MusesIconButtonSize.SM,
                        imageVector = when {
                            task.status.active -> TablerIcons.Pause
                            task.status == DownloadStatus.FAILED || task.status == DownloadStatus.PENDING_UPLOAD -> TablerIcons.Refresh
                            else -> TablerIcons.Download
                        }, tint = scheme.primary, contentDescription = "$action ${task.track.title}")
                }
                MusesIconButton(onClick = onMore, imageVector = TablerIcons.MoreVert, size = MusesIconButtonSize.SM,
                    contentDescription = "${task.track.title}的更多下载操作")
            })
        Column(Modifier.fillMaxWidth().padding(start = 68.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val quality = task.actualQualityLabel ?: if (task.status == DownloadStatus.COMPLETED) "音质未核实"
                    else "所选${qualityLabel(task.quality)}"
                Text("$status · $quality", style = MiuixTheme.textStyles.footnote1,
                    color = statusColor, marqueeEnabled = false)
                val amount = if (bytes > 0) formatBytes(bytes) + (total?.let { " / ${formatBytes(it)}" } ?: "") else ""
                if (amount.isNotEmpty()) Text(amount + if (task.status.active && progress != null) " · ${(progress * 100).toInt()}%" else "",
                    style = MiuixTheme.textStyles.footnote1, color = scheme.onSurfaceVariantSummary,
                    marqueeEnabled = false)
            }
            if (task.status.active) LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
            if (expanded) {
                val requested = task.requestedQuality
                Text("所选：${qualityLabel(task.quality)}" + when {
                    task.actualQualityLabel != null -> " · 实际：${task.actualQualityLabel}"
                    requested != null && requested != task.quality ->
                        " · 请求：${qualityLabel(requested)}（已降档，实际音质待核实）"
                    task.status == DownloadStatus.COMPLETED -> " · 实际音质未核实"
                    else -> ""
                }, style = MiuixTheme.textStyles.footnote1, color = scheme.onSurfaceVariantSummary,
                    marqueeEnabled = false)
                val target = task.target ?: defaultTarget
                val available = downloadTargetAvailable(target, sources)
                Text("保存到：${target.displayLabel(sources)}" + if (available) "" else "（音源已删除）",
                    style = MiuixTheme.textStyles.footnote1,
                    color = if (available) scheme.onSurfaceVariantSummary else scheme.error,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, marqueeEnabled = false)
            } else {
                val target = task.target ?: defaultTarget
                val available = downloadTargetAvailable(target, sources)
                Text("保存到：${target.displayLabel(sources)}" + if (available) "" else "（已失效）",
                    style = MiuixTheme.textStyles.footnote1,
                    color = if (available) scheme.onSurfaceVariantSummary else scheme.error,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, marqueeEnabled = false)
            }
            task.error?.let { Text(it, color = scheme.error, style = MiuixTheme.textStyles.footnote1, marqueeEnabled = false) }
            task.qualityWarning?.let { Text(it, color = scheme.error, style = MiuixTheme.textStyles.footnote1, marqueeEnabled = false) }
            task.warnings.filter { it != task.qualityWarning }.forEach {
                Text(it, color = scheme.onSurfaceVariantSummary, style = MiuixTheme.textStyles.footnote1, marqueeEnabled = false)
            }
        }
    }
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

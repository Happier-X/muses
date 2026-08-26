package com.muses.player.feature.sources

import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.muses.player.core.model.Source
import androidx.compose.material.icons.filled.Radio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.muses.player.core.ui.components.SaltActionsSheet
import com.muses.player.core.ui.components.SaltActionItem
import com.muses.player.core.ui.components.SaltEmpty
import com.muses.player.core.ui.components.SaltIconButton
import com.muses.player.core.ui.components.SaltIconButtonSize
import com.muses.player.core.ui.components.SaltNavbar
import com.muses.player.core.ui.components.SaltTextButton
import com.muses.player.core.ui.components.SaltToggle
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltRadius
import com.muses.player.core.ui.theme.SaltSpacing
import com.muses.player.core.model.SourceType

// ── 主入口 ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    modifier: Modifier = Modifier,
    /** 跳转 WebDAV 添加表单页（P5：对照 Web 层 /tabs/sources/webdav） */
    onOpenWebdavAdd: () -> Unit = {},
    /** 跳转 WebDAV 编辑表单页（对照 /tabs/sources/webdav/:id） */
    onOpenWebdavEdit: (sourceId: String) -> Unit = {},
    viewModel: SourcesViewModel = hiltViewModel(),
) {
    val salt = LocalSaltColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val sources by viewModel.sources.collectAsState()
    val showAddForm by viewModel.showAddForm.collectAsState()
    // 扫描进度弹窗观察 scanner 内部进度流
    val scanProgress by viewModel.scanProgress.collectAsState()

    // ---- .sources-page__navbar-wrap ----
    Column(modifier = modifier.fillMaxSize()) {
        SaltNavbar(
            title = "音源",
            right = {
                // .sources-page__add-btn：32dp clear rounded + add 图标 16px
                SaltIconButton(
                    onClick = { viewModel.openAddActionSheet() },
                    size = SaltIconButtonSize.SM,
                    contentDescription = "添加音源",
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            },
        )

        // ---- .sources-page__content ----
        if (sources.isEmpty()) {
            // m-empty：还没有音源（icon=radio）
            SaltEmpty(
                title = "还没有音源",
                description = "点击右上角加号添加本地文件夹或 WebDAV 文件夹。",
                icon = Icons.Filled.Radio,
                modifier = Modifier.weight(1f),
            )
        } else {
            SourceCardList(
                sources = sources,
                modifier = Modifier.fillMaxSize(),
                onEdit = { source ->
                    // WebDAV：跳独立编辑表单页；本地：打开编辑表单弹窗
                    if (source.type == SourceType.WEBDAV) {
                        onOpenWebdavEdit(source.id)
                    } else {
                        viewModel.openEditForm(source)
                    }
                },
                onDelete = { source -> viewModel.confirmDelete(source) },
                onScan = { source -> viewModel.openScanSettings(source) },
            )
        }
    }

    // ---- 系统目录选择器（添加本地文件夹）：SAF tree uri → 物理路径前缀 → 建源 ----
    val dirPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            // 持久化读写权限：重启后扫描/播放仍可访问该目录
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            viewModel.saveLocalSourceFromTreeUri(treeUri, context)
        }
    }

    // ---- m-actions：添加音源 ----
    if (viewModel.isAddActionSheetOpen) {
        SaltActionsSheet(
            opened = true,
            onDismiss = { viewModel.closeAddActionSheet() },
            label = "添加音源",
            items = listOf(
                SaltActionItem(label = "添加本地文件夹", onClick = {
                    viewModel.closeAddActionSheet()
                    // 系统目录选择器（SAF）：选完回调内建源，对齐 Web FilePicker.pickDirectory 语义
                    dirPickerLauncher.launch(null)
                }),
                SaltActionItem(label = "添加 WebDAV 文件夹", onClick = {
                    viewModel.closeAddActionSheet()
                    onOpenWebdavAdd()
                }),
                SaltActionItem(label = "取消", onClick = { viewModel.closeAddActionSheet() }),
            ),
        )
    }

    // ---- m-dialog：删除确认（deleteAlertMessage 文案逐字对齐）----
    viewModel.pendingDelete?.let { source ->
        val credentialNote = if (source.type == SourceType.WEBDAV) "与安全存储凭据" else ""
        AlertDialog(
            onDismissRequest = { viewModel.dismissDelete() },
            title = { Text("删除音源") },
            text = {
                Text(
                    "确定删除「${source.name}」吗？将同时清理该音源下的歌曲$credentialNote。",
                    color = salt.text2,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSource(source)
                    viewModel.dismissDelete()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDelete() }) { Text("取消") }
            },
        )
    }

    // ---- 编辑音源表单（m-dialog：显示名称 / 目录）----
    viewModel.pendingEdit?.let { source ->
        var editName by remember(source.id) { mutableStateOf(source.name) }
        var editPath by remember(source.id) { mutableStateOf(source.path.orEmpty()) }
        AlertDialog(
            onDismissRequest = { viewModel.dismissEdit() },
            title = { Text("编辑音源") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        singleLine = true,
                        label = { Text("显示名称") },
                        placeholder = { Text("显示名称") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = editPath,
                        onValueChange = { editPath = it },
                        singleLine = true,
                        label = { Text("目录") },
                        placeholder = { Text("目录") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = editName.isNotBlank() && editPath.isNotBlank(),
                    onClick = {
                        viewModel.updateEditedSource(source, editName.trim(), editPath.trim())
                        viewModel.dismissEdit()
                    },
                ) { Text("保存修改") }
            },
            dismissButton = {
                TextButton(enabled = true, onClick = { viewModel.dismissEdit() }) { Text("取消") }
            },
        )
    }

    // ---- m-dialog：扫描设置（对照 SourcesPage.vue scanSettings 弹窗）----
    // KDoc：内容区对应 .sources-page__hint-text；确认按钮对应 .sources-page__scan-start-btn
    viewModel.pendingScanSource?.let {
        AlertDialog(
            onDismissRequest = { viewModel.closeScanSettings() },
            title = { Text("扫描设置") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 「读取音乐标签」+ SaltToggle（对照 Web m-toggle）
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("读取音乐标签", fontSize = 15.sp, color = salt.text)
                        Spacer(Modifier.width(12.dp))
                        SaltToggle(
                            checked = viewModel.scanReadTags,
                            onCheckedChange = { viewModel.updateScanReadTags(it) },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    // __hint-text：13sp text2 居中提示
                    Text(
                        "开启后会逐个文件读取标题、歌手、专辑和时长；读取失败会回退为文件名。",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = salt.text2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.startScan() }) { Text("开始扫描") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeScanSettings() }) { Text("取消") }
            },
        )
    }

    // ---- m-dialog：扫描进度（对照 SourcesPage.vue 扫描进度弹窗：preloader + 阶段 h2 + 当前文件 + 统计行）----
    if (viewModel.isScanProgressOpen) {
        val scanError = viewModel.scanError
        // 阶段文案映射（对齐 Web stage 计算）：错误 > 查找 > 入库 > 完成
        val stageText = when {
            scanError != null -> "扫描失败"
            scanProgress.total == 0 && !scanProgress.finished -> "正在查找文件"
            !scanProgress.finished -> "正在扫描入库"
            else -> "扫描完成"
        }
        AlertDialog(
            // 进行中禁止关闭：不给 onDismissRequest 任何关闭路径，也不渲染关闭按钮
            onDismissRequest = { },
            title = { Text("扫描进度") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 阶段 h2 文案
                    Text(stageText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = salt.text)
                    Spacer(Modifier.height(12.dp))
                    when {
                        scanError != null -> {
                            Text(scanError, fontSize = 13.sp, color = salt.danger)
                        }
                        !scanProgress.finished -> {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                            Spacer(Modifier.height(12.dp))
                            // 当前文件单行省略
                            scanProgress.currentFile?.let {
                                Text(
                                    it,
                                    fontSize = 13.sp,
                                    color = salt.text2,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            // 统计行
                            Text(
                                "已处理 ${scanProgress.current} / ${scanProgress.total}",
                                fontSize = 13.sp,
                                color = salt.text2,
                            )
                        }
                        else -> {
                            // 项目无统一 toast 组件，选最简方案：汇总文案直接在进度弹窗完成态内展示，
                            // 关闭时经 dismissScanProgress() 一并置空（不额外引入 SnackbarHost 脚手架）
                            viewModel.scanResultMessage?.let {
                                Text(it, fontSize = 13.sp, color = salt.text2)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                // 结束态（成功/失败）才给「关闭」按钮
                if (scanProgress.finished || scanError != null) {
                    TextButton(onClick = { viewModel.dismissScanProgress() }) { Text("关闭") }
                }
            },
        )
    }

    // ---- AddSourceSheet（保留既有底部弹窗表单）----
    if (showAddForm) {
        AddSourceSheet(
            form = viewModel.addForm.collectAsState().value,
            onDismiss = { viewModel.dismissAddForm() },
            onTestConnection = { viewModel.testConnection() },
            onSave = { viewModel.saveSource() },
            onUpdateName = { viewModel.updateFormName(it) },
            onUpdateType = { viewModel.updateFormType(it) },
            onUpdateLocalPath = { viewModel.updateFormLocalPath(it) },
            onUpdateWebdavUrl = { viewModel.updateFormWebdavUrl(it) },
            onUpdateWebdavUsername = { viewModel.updateFormWebdavUsername(it) },
            onUpdateWebdavPassword = { viewModel.updateFormWebdavPassword(it) },
        )
    }
}

// ── 音源卡片列表（.sources-page__list / __card）──────────────

/**
 * 卡片：surface-1 背景 + radius-card + hairline 边框，min-height 100；
 * name 17/600 → subtitle 13 text2（「本地文件夹」/「WebDAV · user@server」）→
 * path 单行省略 → actions 右对齐（编辑 outline / 删除 danger / 扫描）。
 */
@Composable
private fun SourceCardList(
    sources: List<Source>,
    modifier: Modifier = Modifier,
    onEdit: (Source) -> Unit,
    onDelete: (Source) -> Unit,
    /** 扫描入口（对照 Web .sources-page__scan-btn） */
    onScan: (Source) -> Unit,
) {
    val salt = LocalSaltColors.current
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = SaltSpacing.spacingSub,
            end = SaltSpacing.spacingSub,
            top = 8.dp,
            bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(sources, key = { it.id }) { source ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(salt.surface1, RoundedCornerShape(SaltRadius.card))
                    .border(1.dp, salt.hairline, RoundedCornerShape(SaltRadius.card))
                    .padding(16.dp),
            ) {
                Text(
                    text = source.name,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = salt.text,
                )
                Text(
                    text = when (source.type) {
                        SourceType.LOCAL -> "本地文件夹"
                        SourceType.WEBDAV -> source.username?.let { "WebDAV · $it@${source.url.orEmpty().removePrefix("https://").removePrefix("http://")}" }
                            ?: ("WebDAV · " + source.url.orEmpty())
                    },
                    fontSize = 13.sp,
                    color = salt.text2,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = source.path ?: source.url.orEmpty(),
                    fontSize = 13.sp,
                    color = salt.text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = SaltSpacing.spacingSub),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SaltTextButton(text = "编辑", onClick = { onEdit(source) })
                    Spacer(Modifier.width(SaltSpacing.spacingSub))
                    SaltTextButton(
                        text = "删除",
                        onClick = { onDelete(source) },
                        destructive = true,
                    )
                    Spacer(Modifier.width(SaltSpacing.spacingSub))
                    // .sources-page__scan-btn：扫描入口 → 打开「扫描设置」弹窗
                    SaltTextButton(text = "扫描", onClick = { onScan(source) })
                }
            }
        }
    }
}

// ── 添加音源底部弹窗 ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSourceSheet(
    form: AddSourceForm,
    onDismiss: () -> Unit,
    onTestConnection: () -> Unit,
    onSave: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateType: (SourceType) -> Unit,
    onUpdateLocalPath: (String) -> Unit,
    onUpdateWebdavUrl: (String) -> Unit,
    onUpdateWebdavUsername: (String) -> Unit,
    onUpdateWebdavPassword: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BottomSheetDefaults.ContainerColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text("添加音源", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            // 名称
            OutlinedTextField(
                value = form.name,
                onValueChange = onUpdateName,
                label = { Text("音源名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            // 类型选择
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = form.type == SourceType.LOCAL,
                    onClick = { onUpdateType(SourceType.LOCAL) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text("本地目录") }
                SegmentedButton(
                    selected = form.type == SourceType.WEBDAV,
                    onClick = { onUpdateType(SourceType.WEBDAV) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text("WebDAV") }
            }
            Spacer(Modifier.height(16.dp))

            // 根据类型显示不同表单
            AnimatedVisibility(visible = form.type == SourceType.LOCAL) {
                Column {
                    OutlinedTextField(
                        value = form.localPath,
                        onValueChange = onUpdateLocalPath,
                        label = { Text("目录路径（SAF tree URI）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("content://com.android.externalstorage...") },
                    )
                }
            }

            AnimatedVisibility(visible = form.type == SourceType.WEBDAV) {
                Column {
                    OutlinedTextField(
                        value = form.webdavUrl,
                        onValueChange = onUpdateWebdavUrl,
                        label = { Text("服务器地址") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://dav.example.com/music/") },
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = form.webdavUsername,
                        onValueChange = onUpdateWebdavUsername,
                        label = { Text("用户名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = form.webdavPassword,
                        onValueChange = onUpdateWebdavPassword,
                        label = { Text("密码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                    Spacer(Modifier.height(12.dp))

                    // 测试连接按钮
                    when (form.testState) {
                        is TestState.Testing -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("正在测试连接…", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        is TestState.Success -> {
                            Text(
                                "连接成功 ✓",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        is TestState.Failure -> {
                            Text(
                                (form.testState as TestState.Failure).message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        is TestState.Idle -> { /* no-op */ }
                    }

                    OutlinedButton(
                        onClick = onTestConnection,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = form.testState !is TestState.Testing,
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("测试连接")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 保存按钮
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = form.name.isNotBlank() && (
                    (form.type == SourceType.LOCAL && form.localPath.isNotBlank()) ||
                        (form.type == SourceType.WEBDAV && form.webdavUrl.isNotBlank())
                    ),
            ) {
                Text("保存")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}


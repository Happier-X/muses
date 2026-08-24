package com.muses.player.feature.sources

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.muses.player.core.model.SourceType
import com.muses.player.core.webdav.WebDavItem

// ── 主入口 ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    modifier: Modifier = Modifier,
    viewModel: SourcesViewModel = hiltViewModel(),
) {
    val sources by viewModel.sources.collectAsState()
    val showAddForm by viewModel.showAddForm.collectAsState()
    val browseState by viewModel.browseState.collectAsState()

    // 浏览态优先展示
    if (browseState != null) {
        WebDavBrowseScreen(
            state = browseState!!,
            onBack = { viewModel.closeBrowse() },
            onRefresh = { viewModel.openBrowse(Source(
                id = browseState!!.sourceId,
                name = browseState!!.sourceName,
                type = SourceType.WEBDAV,
                url = browseState!!.currentUrl,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )) },
        )
        return
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("音源管理") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showAddForm() },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("添加音源") },
            )
        },
    ) { innerPadding ->
        if (sources.isEmpty()) {
            EmptySourcesHint(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onAddClick = { viewModel.showAddForm() },
            )
        } else {
            SourceList(
                sources = sources,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onDelete = { viewModel.deleteSource(it) },
                onBrowse = { viewModel.openBrowse(it) },
            )
        }
    }

    // 添加音源底部弹窗
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

// ── 空态提示 ──────────────────────────────────────────

@Composable
private fun EmptySourcesHint(modifier: Modifier = Modifier, onAddClick: () -> Unit) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
        Spacer(Modifier.height(16.dp))
        Text("尚未添加任何音源", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "添加本地目录或 WebDAV 服务器来开始浏览音乐",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onAddClick, modifier = Modifier.padding(top = 24.dp)) {
            Text("添加音源")
        }
    }
}

// ── 音源列表 ──────────────────────────────────────────

@Composable
private fun SourceList(
    sources: List<Source>,
    modifier: Modifier = Modifier,
    onDelete: (Source) -> Unit,
    onBrowse: (Source) -> Unit,
) {
    var sourceToDelete by remember { mutableStateOf<Source?>(null) }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(sources, key = { it.id }) { source ->
            SourceItem(
                source = source,
                onClick = { onBrowse(source) },
                onDelete = { sourceToDelete = source },
            )
        }
    }

    // 删除确认弹窗
    sourceToDelete?.let { source ->
        AlertDialog(
            onDismissRequest = { sourceToDelete = null },
            title = { Text("删除音源") },
            text = { Text("确定要删除音源「${source.name}」吗？关联的歌曲将从曲库中移除。") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(source)
                    sourceToDelete = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { sourceToDelete = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SourceItem(
    source: Source,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when (source.type) {
                SourceType.LOCAL -> Icons.Filled.Folder
                SourceType.WEBDAV -> Icons.Filled.Cloud
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = source.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = when (source.type) {
                    SourceType.LOCAL -> source.path ?: ""
                    SourceType.WEBDAV -> source.url ?: ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
            )
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

// ── WebDAV 目录浏览页 ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDavBrowseScreen(
    state: WebDavBrowseState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.sourceName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            state.currentPath,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("正在加载…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            state.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "加载失败",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        state.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = onRefresh, modifier = Modifier.padding(top = 16.dp)) {
                        Text("重试")
                    }
                }
            }
            state.items.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("目录为空", style = MaterialTheme.typography.titleMedium)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(state.items, key = { it.url }) { item ->
                        WebDavItemRow(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun WebDavItemRow(item: WebDavItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (item.isDirectory) Icons.Filled.Folder else Icons.Filled.Cloud,
            contentDescription = null,
            tint = if (item.isDirectory) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!item.isDirectory && item.contentLength > 0) {
                Text(
                    text = formatFileSize(item.contentLength),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
        else -> "${"%.2f".format(bytes / (1024.0 * 1024 * 1024))} GB"
    }
}

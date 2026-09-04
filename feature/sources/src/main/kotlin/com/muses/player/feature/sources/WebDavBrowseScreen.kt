package com.muses.player.feature.sources

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import com.muses.player.core.ui.icons.TablerIcons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.muses.player.core.ui.components.SaltNavbar
import com.muses.player.core.ui.components.SaltTextButton
import com.muses.player.core.ui.components.SaltTextButtonSize
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltRadius
import com.muses.player.core.ui.theme.SaltSpacing
/**
 * WebDAV 目录浏览页 —— 一比一翻译自 SourceWebDavBrowsePage.vue + WebDavDirectoryBrowser.vue。
 *
 * 模式：
 * - single：单选确认（编辑回填流程）
 * - multiple：多选确认（添加流程）
 *
 * 参数由导航参数传入（connection, initialPath, mode）
 */
@Composable
fun WebDavBrowseScreen(
    mode: String, // "single" 或 "multiple"
    initialPath: String,
    serverUrl: String,
    username: String,
    password: String,
    onBack: () -> Unit,
    onConfirm: (paths: List<String>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WebDavBrowseViewModel = hiltViewModel(),
) {
    val salt = LocalSaltColors.current
    val browseState by viewModel.browseState.collectAsState()

    // 初始化
    viewModel.init(mode, initialPath, serverUrl, username, password)

    /** 确认选择：结果写入跨页会话后回退（对照 setWebDavBrowseResult） */
    val confirmSelection: (List<String>) -> Unit = { paths ->
        WebDavBrowseResultHolder.set(
            WebDavBrowseResultHolder.BrowseResult(
                paths = paths,
                serverUrl = serverUrl,
                username = username,
                password = password,
            ),
        )
        onConfirm(paths)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(salt.surface),
    ) {
        // .source-webdav-browse-page__navbar-wrap
        SaltNavbar(
            title = if (mode == "single") "选择目录" else "选择文件夹",
            left = {
                SaltTextButton(
                    text = "返回",
                    onClick = {
                        viewModel.clearSelection()
                        onBack()
                    },
                    size = SaltTextButtonSize.SMALL,
                )
            },
        )

        // .source-webdav-browse-page__content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = SaltSpacing.spacingSub)
                .padding(top = 8.dp),
        ) {
            // .webdav-browser__nav（路径导航）
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 返回上级按钮
                SaltTextButton(
                    text = "返回上级",
                    onClick = { viewModel.goToParent() },
                    enabled = viewModel.parentPath != null && !browseState.isLoading,
                    size = SaltTextButtonSize.SMALL,
                )
                Spacer(Modifier.width(8.dp))
                // 当前路径
                Text(
                    text = browseState.currentPath,
                    fontSize = 13.sp,
                    color = salt.text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(8.dp))

            // 加载中
            if (browseState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "正在读取目录…",
                        fontSize = 14.sp,
                        color = salt.text2,
                    )
                }
            } else {
                // 目录列表
                if (browseState.directories.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = if (mode == "multiple") {
                                "当前目录没有可添加的子文件夹。"
                            } else {
                                "当前目录没有子文件夹。"
                            },
                            fontSize = 13.sp,
                            color = salt.text2,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        // 底部预留 MiniPlayerBar 空间（项目惯例 96dp，对齐 SongsPage/SourcesScreen）
                        contentPadding = PaddingValues(bottom = 96.dp),
                    ) {
                        items(browseState.directories, key = { it.path }) { directory ->
                            DirectoryRow(
                                directory = directory,
                                isSelected = browseState.selectedPaths.contains(directory.path),
                                mode = mode,
                                onSelect = { viewModel.toggleSelection(directory.path) },
                                onOpen = { viewModel.openDirectory(directory.path) },
                                onConfirm = {
                                    if (mode == "single") {
                                        confirmSelection(listOf(directory.path))
                                    } else {
                                        viewModel.openDirectory(directory.path)
                                    }
                                },
                            )
                        }
                    }

                    // 多选模式：底部确认按钮
                    if (mode == "multiple" && browseState.selectedPaths.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        SaltTextButton(
                            text = "添加选中的 ${browseState.selectedPaths.size} 个文件夹",
                            onClick = { confirmSelection(browseState.selectedPaths.toList()) },
                            // 底部避让全局悬浮 MiniPlayerBar（64dp 高 + 间距）
                            modifier = Modifier.fillMaxWidth().padding(bottom = 96.dp),
                        )
                    }
                }
            }

            // 错误对话框
            browseState.errorMessage?.let { message ->
                AlertDialog(
                    onDismissRequest = { viewModel.dismissError() },
                    title = { Text("错误") },
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.dismissError() }) {
                            Text("确定")
                        }
                    },
                )
            }
        }
    }
}

/**
 * 目录行 —— 对照 .webdav-browser__row
 */
@Composable
private fun DirectoryRow(
    directory: WebDavDirectoryItem,
    isSelected: Boolean,
    mode: String,
    onSelect: () -> Unit,
    onOpen: () -> Unit,
    onConfirm: () -> Unit,
) {
    val salt = LocalSaltColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(salt.surface1, RoundedCornerShape(SaltRadius.sm))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 多选模式：复选框
        if (mode == "multiple") {
            Icon(
                imageVector = if (isSelected) TablerIcons.CheckBox else TablerIcons.CheckBoxOutlineBlank,
                contentDescription = if (isSelected) "取消选择" else "选择",
                tint = if (isSelected) salt.primary else salt.text2,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onSelect() },
            )
            Spacer(Modifier.width(8.dp))
        }

        // 目录图标
        Icon(
            imageVector = TablerIcons.Folder,
            contentDescription = null,
            tint = salt.primary,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(12.dp))

        // 目录信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = directory.basename,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = salt.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = directory.path,
                fontSize = 13.sp,
                color = salt.text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // 操作按钮
        SaltTextButton(
            text = if (mode == "single") "选择" else "进入",
            onClick = onConfirm,
            size = SaltTextButtonSize.SMALL,
        )
    }
}

/** WebDAV 目录项（UI 模型） */
data class WebDavDirectoryItem(
    val basename: String,
    val path: String,
)

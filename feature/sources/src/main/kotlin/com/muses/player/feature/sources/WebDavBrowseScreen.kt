package com.muses.player.feature.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.muses.player.core.ui.components.SaltNavbar
import com.muses.player.core.ui.components.SaltTextButton
import com.muses.player.core.ui.components.SaltTextButtonSize
import com.muses.player.core.ui.components.WebDavBrowseItem
import com.muses.player.core.ui.components.WebDavBrowseList
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltSpacing
/**
 * WebDAV 目录浏览页 —— 一比一翻译自 SourceWebDavBrowsePage.vue + WebDavDirectoryBrowser.vue。
 *
 * 模式：
 * - single：单选确认（编辑回填流程）
 * - multiple：多选确认（添加流程）
 *
 * 参数由导航参数传入（connection, initialPath, mode）
 *
 * 浏览页共用化：目录列表/路径导航/加载态/空态经 ui-shared [WebDavBrowseList] 渲染，
 * 本页只保留导航栏 + ViewModel 接线 + 错误对话框（行为冻结）。
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
    viewModel: WebDavBrowseViewModel = koinViewModel(),
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
            WebDavBrowseList(
                mode = mode,
                currentPath = browseState.currentPath,
                directories = browseState.directories.map { it.toShared() },
                selectedPaths = browseState.selectedPaths,
                isLoading = browseState.isLoading,
                canGoParent = viewModel.parentPath != null && !browseState.isLoading,
                onGoParent = { viewModel.goToParent() },
                onToggleSelection = { viewModel.toggleSelection(it) },
                onOpenDirectory = { viewModel.openDirectory(it) },
                onConfirmSingle = { confirmSelection(listOf(it)) },
                onConfirmMultiple = { confirmSelection(it) },
                modifier = Modifier.weight(1f),
            )

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

/** 安卓目录项（UI 模型；共用组件只认映射后的 [WebDavBrowseItem]）。 */
data class WebDavDirectoryItem(
    val basename: String,
    val path: String,
)

/** 安卓目录项 → 共用浏览条目映射（path 即共用 url 键）。 */
private fun WebDavDirectoryItem.toShared() = WebDavBrowseItem(
    name = basename,
    url = path,
)

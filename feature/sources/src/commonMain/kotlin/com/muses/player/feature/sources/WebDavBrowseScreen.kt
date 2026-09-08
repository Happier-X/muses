package com.muses.player.feature.sources

import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import com.muses.player.core.ui.components.MusesDialog
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.muses.player.core.ui.components.MusesNavbar
import com.muses.player.core.ui.components.MusesTextButton
import com.muses.player.core.ui.components.MusesTextButtonSize
import com.muses.player.core.ui.components.WebDavBrowseItem
import com.muses.player.core.ui.components.WebDavBrowseList
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
    val scheme = MiuixTheme.colorScheme
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
            .background(scheme.background),
    ) {
        // .source-webdav-browse-page__navbar-wrap
        MusesNavbar(
            title = if (mode == "single") "选择目录" else "选择文件夹",
            left = {
                MusesTextButton(
                    text = "返回",
                    onClick = {
                        viewModel.clearSelection()
                        onBack()
                    },
                    size = MusesTextButtonSize.SMALL,
                )
            },
        )

        // .source-webdav-browse-page__content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
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

            // 错误对话框（miuix MusesDialog）
            browseState.errorMessage?.let { message ->
                MusesDialog(
                    onDismiss = { viewModel.dismissError() },
                    title = "错误",
                    message = message,
                    confirmText = "确定",
                    onConfirm = { viewModel.dismissError() },
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

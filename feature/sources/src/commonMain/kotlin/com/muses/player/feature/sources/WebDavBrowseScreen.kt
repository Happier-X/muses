package com.muses.player.feature.sources

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.muses.player.core.ui.components.MusesBottomSheet
import com.muses.player.core.ui.theme.LocalBottomChromePadding
import com.muses.player.core.ui.components.WebDavBrowseItem
import com.muses.player.core.ui.components.WebDavBrowseList
/** 仅在表单打开面板期间持有连接信息，不进入导航栈或持久化。 */
class WebDavBrowseRequest(
    val mode: String,
    val initialPath: String,
    val serverUrl: String,
    val username: String,
    val password: String,
    val selectedPaths: List<String>,
)

/** 当前表单上的目录选择面板；确认回填，关闭丢弃临时选择。 */
@Composable
fun WebDavBrowseSheet(
    request: WebDavBrowseRequest,
    onDismiss: () -> Unit,
    onConfirm: (paths: List<String>) -> Unit,
    viewModel: WebDavBrowseViewModel = koinViewModel(),
) {
    val browseState by viewModel.browseState.collectAsState()

    LaunchedEffect(request) {
        viewModel.init(request.mode, request.initialPath, request.serverUrl, request.username, request.password, request.selectedPaths)
    }
    DisposableEffect(viewModel) {
        onDispose { viewModel.endSession() }
    }
    val sheetHeight = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.height.toDp() * 0.65f
    }.coerceAtMost(560.dp)
    MusesBottomSheet(title = "选择目录", onDismiss = onDismiss) {
        CompositionLocalProvider(LocalBottomChromePadding provides 0.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .padding(horizontal = 12.dp)
                .padding(top = 8.dp),
        ) {
            WebDavBrowseList(
                mode = request.mode,
                currentPath = browseState.currentPath,
                directories = browseState.directories.map { it.toShared() },
                selectedPaths = browseState.selectedPaths,
                isLoading = browseState.isLoading,
                isSubmitting = false,
                onToggleSelection = { viewModel.toggleSelection(it) },
                onOpenDirectory = { viewModel.openDirectory(it) },
                onConfirmSingle = { onConfirm(listOf(it)) },
                onConfirmMultiple = onConfirm,
                onNavigatePath = { viewModel.navigateTo(it) },
                onRefresh = viewModel::refresh,
                modifier = Modifier.weight(1f),
                errorText = browseState.errorMessage,
                onDismissError = viewModel::dismissError,
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

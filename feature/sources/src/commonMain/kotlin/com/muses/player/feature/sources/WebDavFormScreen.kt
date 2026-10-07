package com.muses.player.feature.sources

import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.muses.player.core.ui.components.MusesDialog
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.ui.components.MusesSnackbar
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.muses.player.core.ui.components.MusesTopBar
import com.muses.player.core.ui.components.MusesButton
import com.muses.player.core.ui.components.SourceFormCard
import com.muses.player.core.ui.components.SourceFormInput
import kotlinx.coroutines.delay

/**
 * WebDAV 添加/编辑表单页 —— 一比一翻译自 SourceWebDavPage.vue。
 *
 * 模式由 sourceId 决定：
 * - null = 添加模式（选择目录 → 验证连接并全屏多选批量建源）
 * - 非 null = 编辑模式（改名称/地址/密码/目录；密码留空保留原密码）
 */
@Composable
fun WebDavFormScreen(
    sourceId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WebDavFormViewModel = koinViewModel(),
) {
    val scheme = MiuixTheme.colorScheme
    val formState by viewModel.formState.collectAsState()
    val isEditMode = sourceId != null
    var browseRequest by remember(sourceId) { mutableStateOf<WebDavBrowseRequest?>(null) }
    val openBrowse: (String, String, String, String, String) -> Unit = { mode, path, url, username, password ->
        browseRequest = WebDavBrowseRequest(mode, path, url, username, password, viewModel.formState.value.selectedPaths)
    }

    // 编辑模式初始化（副作用收敛到 LaunchedEffect，不在组合期直调）
    LaunchedEffect(sourceId) {
        if (sourceId != null) {
            viewModel.initEditMode(sourceId)
        }
    }

    // 新增或编辑成功后显示提示，再返回音源列表。
    formState.successMessage?.let { message ->
        LaunchedEffect(message) {
            MusesSnackbar.show(message)
            delay(800)
            viewModel.dismissSuccess()
            onBack()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = scheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            MusesTopBar(
                title = if (isEditMode) "编辑 WebDav 音源" else "添加 WebDav 音源",
                // m-navbar-back-link：返回箭头按钮
                navigationIcon = { MusesIconButtonBack(onClick = onBack) },
            )
        },
    ) { padding ->
        // .source-webdav-page__content：表单可滚动（content 槽顶替原外层 Column，层级 1:1）
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(top = 8.dp)
                // 末项避让底部悬浮件（悬浮件高度见 BottomChrome）
                .padding(bottom = com.muses.player.core.ui.theme.LocalBottomChromePadding.current),
        ) {
            SourceFormCard(
                modifier = Modifier.padding(horizontal = 12.dp),
                name = formState.name,
                onNameChange = { viewModel.updateName(it) },
                showNameField = true,
                nameError = formState.nameError,
                url = formState.serverUrl,
                onUrlChange = { viewModel.updateServerUrl(it) },
                urlError = formState.serverUrlError,
                username = formState.username,
                onUsernameChange = { viewModel.updateUsername(it) },
                usernameError = formState.usernameError,
                password = formState.password,
                onPasswordChange = { viewModel.updatePassword(it) },
                passwordLabel = "密码",
                passwordError = formState.passwordError,
                busy = formState.isVerifying || formState.isSubmitting,
                saveBusy = formState.isSubmitting,
                saveText = "保存",
                busyText = "保存",
                primarySave = true,
                showBusyIndicator = formState.isSubmitting,
                showSaveButton = true,
                onSave = {
                    if (isEditMode) viewModel.submitEdit() else viewModel.submitAdd()
                },
                extraContent = {
                    // 新增和编辑共用目录行；新增点击后先验证连接，再进入多选浏览。
                    SourceFormInput(
                        label = "目录",
                        value = formState.path,
                        error = formState.pathError,
                        readOnly = true,
                        onValueChange = {},
                        trailingContent = {
                            MusesButton(
                                modifier = Modifier.fillMaxHeight(),
                                onClick = {
                                    if (isEditMode) viewModel.startEditBrowse(openBrowse)
                                    else viewModel.startAddBrowse(openBrowse)
                                },
                                enabled = !formState.isVerifying && !formState.isSubmitting,
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (formState.isVerifying) {
                                        CircularProgressIndicator(size = 16.dp, strokeWidth = 2.dp)
                                    }
                                    Text("选择目录")
                                }
                            }
                        },
                    )
                },
            )

        }
        browseRequest?.let { request ->
            WebDavBrowseSheet(
                request = request,
                onDismiss = { browseRequest = null },
                onConfirm = { paths ->
                    viewModel.selectBrowsePaths(paths)
                    browseRequest = null
                },
            )
        }
    }

    // 错误提示（MusesDialog 对话框）
    formState.errorMessage?.let { message ->
        MusesDialog(
            onDismiss = { viewModel.dismissError() },
            title = "错误",
            message = message,
            confirmText = "确定",
            onConfirm = { viewModel.dismissError() },
        )
    }
}

/** navbar 返回箭头（对照 m-navbar-back-link） */
@Composable
private fun MusesIconButtonBack(onClick: () -> Unit) {
    com.muses.player.core.ui.components.MusesIconButton(
        onClick = onClick,
        contentDescription = "返回",
    ) {
        Icon(
            imageVector = TablerIcons.ArrowBack,
            contentDescription = null,
        )
    }
}

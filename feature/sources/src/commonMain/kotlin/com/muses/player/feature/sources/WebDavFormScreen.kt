package com.muses.player.feature.sources

import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.muses.player.core.ui.components.MusesDialog
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.ui.components.MusesSnackbar
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.muses.player.core.ui.components.MusesTopBar
import com.muses.player.core.ui.components.MusesTextButton
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
    onBrowse: (mode: String, initialPath: String, serverUrl: String, username: String, password: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WebDavFormViewModel = koinViewModel(),
) {
    val scheme = MiuixTheme.colorScheme
    val formState by viewModel.formState.collectAsState()
    val isEditMode = sourceId != null

    // 编辑模式初始化（副作用收敛到 LaunchedEffect，不在组合期直调）
    LaunchedEffect(sourceId) {
        if (sourceId != null) {
            viewModel.initEditMode(sourceId)
        }
    }

    // 消费浏览页带回的结果（single 回填目录 / multiple 批量建源）。
    // 观察 holder 的 StateFlow 而不是用 LaunchedEffect(Unit)：miuix-nav 在浏览页期间保留
    // 表单页组合时，返回不会重新组合，一次性副作用会漏掉结果——表现为「点了添加没反应，
    // 第二次进来才提示添加成功」（结果残留在 holder，被下一次进入消费）。
    val pendingBrowse = WebDavBrowseResultHolder.result.collectAsState().value
    LaunchedEffect(pendingBrowse) {
        if (pendingBrowse != null) {
            viewModel.consumeBrowseResult()
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
                .padding(horizontal = 12.dp)
                .padding(top = 8.dp)
                // 末项避让底部悬浮件（悬浮件高度见 BottomChrome）
                .padding(bottom = com.muses.player.core.ui.theme.LocalBottomChromePadding.current),
        ) {
            // .source-webdav-page__form-fields：共用 SourceFormCard（受控字段经 VM 回调注入）
            SourceFormCard(
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
                saveText = if (isEditMode) "编辑" else "添加",
                busyText = if (isEditMode) "编辑" else "添加",
                showBusyIndicator = formState.isSubmitting,
                primarySave = true,
                showSaveButton = true,
                onSave = {
                    if (isEditMode) viewModel.submitEdit() else viewModel.submitAdd()
                },
                extraContent = {
                    // 新增和编辑共用目录行；新增点击后先验证连接，再进入多选浏览。
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        SourceFormInput(
                            label = "目录",
                            value = formState.path,
                            error = formState.pathError,
                            readOnly = true,
                            modifier = Modifier.weight(1f),
                            onValueChange = {},
                        )
                        MusesTextButton(
                            text = "选择文件夹",
                            modifier = Modifier.height(56.dp),
                            onClick = {
                                if (isEditMode) viewModel.startEditBrowse(onBrowse)
                                else viewModel.startAddBrowse(onBrowse)
                            },
                            enabled = !formState.isVerifying && !formState.isSubmitting,
                        )
                    }
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

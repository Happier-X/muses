package com.muses.player.feature.sources

import top.yukonga.miuix.kmp.theme.MiuixTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.muses.player.core.ui.components.MusesDialog
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.uishared.platform.PlatformToast
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import com.muses.player.core.ui.components.MusesTopBar
import com.muses.player.core.ui.components.MusesTextButton
import com.muses.player.core.ui.components.SourceFormCard
import com.muses.player.core.ui.components.SourceFormInput
import kotlinx.coroutines.delay

/**
 * WebDAV 添加/编辑表单页 —— 一比一翻译自 SourceWebDavPage.vue。
 *
 * 模式由 sourceId 决定：
 * - null = 添加模式（验证连接 → 全屏目录浏览多选批量建源）
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

    // 从浏览页返回：消费带回的结果（single 回填目录 / multiple 批量建源）
    LaunchedEffect(Unit) {
        viewModel.consumeBrowseResult()
    }

    // 成功提示后稍作停留再返回音源列表（对照 scheduleLeave 800ms）
    formState.successMessage?.let { message ->
        LaunchedEffect(message) {
            PlatformToast.show(message)
            delay(800)
            viewModel.dismissSuccess()
            onBack()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = scheme.background,
        topBar = {
            MusesTopBar(
                title = if (isEditMode) "编辑 WebDAV" else "添加 WebDAV",
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
                .padding(top = 8.dp),
        ) {
            // .source-webdav-page__form-fields：共用 SourceFormCard（受控字段经 VM 回调注入）
            SourceFormCard(
                name = formState.name,
                onNameChange = { viewModel.updateName(it) },
                showNameField = isEditMode,
                nameError = formState.nameError,
                url = formState.serverUrl,
                onUrlChange = { viewModel.updateServerUrl(it) },
                urlError = formState.serverUrlError,
                username = formState.username,
                onUsernameChange = { viewModel.updateUsername(it) },
                usernameError = formState.usernameError,
                password = formState.password,
                onPasswordChange = { viewModel.updatePassword(it) },
                passwordLabel = if (isEditMode) "新密码" else "密码",
                passwordInfo = if (isEditMode) "留空则保留原密码" else null,
                passwordError = formState.passwordError,
                busy = formState.isVerifying || formState.isSubmitting,
                saveText = if (isEditMode) "保存修改" else "连接并浏览",
                onSave = {
                    if (isEditMode) viewModel.submitEdit()
                    else viewModel.submitAdd(onBrowse)
                },
                extraContent = {
                    // 目录（仅编辑模式，只读展示 + 浏览目录按钮）
                    if (isEditMode) {
                        // .source-webdav-page__path-row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            SourceFormInput(
                                label = "目录",
                                value = formState.path,
                                placeholder = "目录",
                                error = formState.pathError,
                                readOnly = true,
                                modifier = Modifier.weight(1f),
                                onValueChange = {},
                            )
                            MusesTextButton(
                                text = "浏览目录",
                                onClick = { viewModel.startEditBrowse(onBrowse) },
                            )
                        }
                    }
                },
            )

            // 编辑模式第二动作：连接并浏览（共用卡只有一个主按钮，编辑态副按钮放卡外）
            if (isEditMode) {
                Spacer(Modifier.height(12.dp))
                MusesTextButton(
                    text = "连接并浏览",
                    onClick = { viewModel.startEditBrowse(onBrowse) },
                    enabled = !formState.isVerifying && !formState.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 验证中指示器
            if (formState.isVerifying || formState.isSubmitting) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (formState.isVerifying) "正在验证连接…" else "正在保存…",
                        fontSize = 14.sp,
                        color = scheme.onBackgroundVariant,
                    )
                }
            }
        }
    }

    // 错误提示（m-toast center 观感用系统 Toast 承担行为层，样式待 SaltToast 组件落地统一；miuix MusesDialog）
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

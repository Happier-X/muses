package com.muses.player.feature.sources

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import com.muses.player.core.ui.icons.TablerIcons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.muses.player.core.ui.components.SaltNavbar
import com.muses.player.core.ui.components.SaltTextButton
import com.muses.player.core.ui.components.SourceFormCard
import com.muses.player.core.ui.components.SourceFormInput
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltSpacing
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
    val salt = LocalSaltColors.current
    val context = LocalContext.current
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
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            delay(800)
            viewModel.dismissSuccess()
            onBack()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(salt.surface),
    ) {
        // .source-webdav-page__navbar-wrap
        SaltNavbar(
            title = if (isEditMode) "编辑 WebDAV" else "添加 WebDAV",
            left = {
                // m-navbar-back-link：返回箭头按钮
                SaltIconButtonBack(onClick = onBack)
            },
        )

        // .source-webdav-page__content：表单可滚动
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SaltSpacing.spacingSub)
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
                            SaltTextButton(
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
                SaltTextButton(
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
                        color = salt.text2,
                    )
                }
            }
        }
    }

    // 错误提示（m-toast center 观感用系统 Toast 承担行为层，样式待 SaltToast 组件落地统一）
    formState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text("错误") },
            text = { Text(message, color = salt.text2) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) {
                    Text("确定")
                }
            },
        )
    }
}

/** navbar 返回箭头（对照 m-navbar-back-link） */
@Composable
private fun SaltIconButtonBack(onClick: () -> Unit) {
    com.muses.player.core.ui.components.SaltIconButton(
        onClick = onClick,
        contentDescription = "返回",
    ) {
        androidx.compose.material3.Icon(
            imageVector = TablerIcons.ArrowBack,
            contentDescription = null,
        )
    }
}

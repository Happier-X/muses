package com.muses.player.feature.sources

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.muses.player.core.ui.components.SaltNavbar
import com.muses.player.core.ui.components.SaltTextButton
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltRadius
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
            // .source-webdav-page__form-fields
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 显示名称（仅编辑模式）
                if (isEditMode) {
                    WebDavFormInput(
                        label = "显示名称",
                        value = formState.name,
                        placeholder = "显示名称",
                        error = formState.nameError,
                        onValueChange = { viewModel.updateName(it) },
                    )
                }

                // 服务器地址
                WebDavFormInput(
                    label = "服务器地址",
                    value = formState.serverUrl,
                    placeholder = "https://example.com/dav",
                    error = formState.serverUrlError,
                    keyboardType = KeyboardType.Uri,
                    onValueChange = { viewModel.updateServerUrl(it) },
                )

                // 用户名
                WebDavFormInput(
                    label = "用户名",
                    value = formState.username,
                    placeholder = "用户名",
                    error = formState.usernameError,
                    onValueChange = { viewModel.updateUsername(it) },
                )

                // 密码
                WebDavFormInput(
                    label = if (isEditMode) "新密码" else "密码",
                    value = formState.password,
                    placeholder = if (isEditMode) "新密码" else "密码",
                    info = if (isEditMode) "留空则保留原密码" else null,
                    error = formState.passwordError,
                    isPassword = true,
                    onValueChange = { viewModel.updatePassword(it) },
                )

                // 目录（仅编辑模式，只读展示 + 浏览目录按钮）
                if (isEditMode) {
                    // .source-webdav-page__path-row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        WebDavFormInput(
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
            }

            Spacer(Modifier.height(16.dp))

            // .source-webdav-page__actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SaltSpacing.spacingSub),
            ) {
                val busy = formState.isVerifying || formState.isSubmitting
                if (isEditMode) {
                    // 编辑模式：连接并浏览 + 保存修改
                    SaltTextButton(
                        text = "连接并浏览",
                        onClick = { viewModel.startEditBrowse(onBrowse) },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    )
                    SaltTextButton(
                        text = if (formState.isSubmitting) "正在保存…" else "保存修改",
                        onClick = { viewModel.submitEdit() },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    // 添加模式：连接并浏览（先验证连接再进浏览页）
                    SaltTextButton(
                        text = "连接并浏览",
                        onClick = { viewModel.submitAdd(onBrowse) },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    )
                }
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

/**
 * 表单输入行 —— 对照 m-list-input：label 上、输入框下（40px 高透明底 input）、
 * info/error 辅助行。
 */
@Composable
private fun WebDavFormInput(
    label: String,
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    info: String? = null,
    error: String? = null,
    isPassword: Boolean = false,
    readOnly: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    val salt = LocalSaltColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = salt.text,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        // .source-webdav-page__input：40px 高、无边框透明底
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                readOnly = readOnly,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = if (isPassword) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                textStyle = TextStyle(fontSize = 16.sp, color = salt.text),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 16.sp,
                                color = salt.text3,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }

        error?.let {
            Text(
                text = it,
                fontSize = 12.sp,
                color = salt.danger,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        info?.let {
            Text(
                text = it,
                fontSize = 12.sp,
                color = salt.text2,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

package com.muses.player.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltRadius
import com.muses.player.core.ui.theme.SaltSpacing

/**
 * 跨平台音源行数据（平台无关，只承载展示信息）。
 *
 * 安卓端由调用方从 [com.muses.player.core.model.Source] 映射而来
 * （subtitle =「本地文件夹」/「WebDAV · user@host」，detail = path/url）；
 * 桌面端由调用方从 [com.muses.player.core.data.db.SourceEntity] 映射而来。
 */
data class SharedSourceItem(
    val id: String,
    val name: String,
    val subtitle: String? = null,
    val detail: String? = null,
)

/**
 * 跨平台音源行（音源管理共用化）。
 *
 * 视觉契约（对照安卓 SourcesScreen.SourceCardList 卡片）：
 * - surface1 背景 + radius-card + hairline 边框，内缩 16dp；
 * - name 17sp/600 → subtitle 13sp text2 → detail 单行省略 13sp text2；
 * - actions 右对齐（编辑 / 删除 danger / 扫描可选）。
 *
 * 纯 UI 组件，零平台依赖，所有业务逻辑经回调注入。
 *
 * @param item 音源展示数据
 * @param onEdit 编辑回调
 * @param onDelete 删除回调
 * @param onScan 扫描回调；null = 不渲染扫描按钮（桌面端）
 */
@Composable
fun SourceListItem(
    item: SharedSourceItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onScan: (() -> Unit)? = null,
) {
    val salt = LocalSaltColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(salt.surface1, RoundedCornerShape(SaltRadius.card))
            .border(1.dp, salt.hairline, RoundedCornerShape(SaltRadius.card))
            .padding(16.dp),
    ) {
        Text(
            text = item.name,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = salt.text,
        )
        item.subtitle?.let {
            Text(
                text = it,
                fontSize = 13.sp,
                color = salt.text2,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        item.detail?.let {
            Text(
                text = it,
                fontSize = 13.sp,
                color = salt.text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = SaltSpacing.spacingSub),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SaltTextButton(text = "编辑", onClick = onEdit)
            Spacer(Modifier.width(SaltSpacing.spacingSub))
            SaltTextButton(
                text = "删除",
                onClick = onDelete,
                destructive = true,
            )
            if (onScan != null) {
                Spacer(Modifier.width(SaltSpacing.spacingSub))
                SaltTextButton(text = "扫描", onClick = onScan)
            }
        }
    }
}

/**
 * 跨平台音源表单输入行 —— 对照 m-list-input：label 上、输入框下（40px 高透明底 input）、
 * info/error 辅助行（与安卓 WebDavFormScreen.WebDavFormInput 同视觉）。
 *
 * 纯 UI，零平台依赖。
 */
@Composable
fun SourceFormInput(
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

/**
 * 跨平台音源表单卡（音源管理共用化）。
 *
 * 字段：名称（可选显示）/ 地址 / 用户名 / 密码 + 保存 / 取消按钮。
 * 受控组件：字段值与错误文案由调用方持有（安卓 ViewModel / 桌面 remember），
 * 所有业务逻辑经回调注入。额外行（如安卓编辑态目录 + 浏览目录）经 [extraContent] 插入。
 *
 * @param name 显示名称值；[showNameField] = false 时不渲染名称行（安卓添加态）
 * @param url 服务器地址值
 * @param username 用户名值
 * @param password 密码值
 * @param busy 验证/提交中；true 时输入与按钮禁用
 * @param saveText 保存按钮文案（如「保存」「连接并浏览」「保存修改」）
 * @param onSave 保存回调
 * @param onCancel 取消回调；null = 不渲染取消按钮
 * @param extraContent 额外行插槽（目录行等），渲染在密码行之后、按钮之前
 */
@Composable
fun SourceFormCard(
    url: String,
    onUrlChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    saveText: String,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    name: String = "",
    onNameChange: (String) -> Unit = {},
    showNameField: Boolean = true,
    nameError: String? = null,
    urlError: String? = null,
    usernameError: String? = null,
    passwordError: String? = null,
    passwordInfo: String? = null,
    passwordLabel: String = "密码",
    busy: Boolean = false,
    saveEnabled: Boolean = true,
    cancelText: String? = null,
    onCancel: (() -> Unit)? = null,
    extraContent: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showNameField) {
            SourceFormInput(
                label = "显示名称",
                value = name,
                placeholder = "显示名称",
                error = nameError,
                readOnly = busy,
                onValueChange = onNameChange,
            )
        }

        SourceFormInput(
            label = "服务器地址",
            value = url,
            placeholder = "https://example.com/dav",
            error = urlError,
            keyboardType = KeyboardType.Uri,
            readOnly = busy,
            onValueChange = onUrlChange,
        )

        SourceFormInput(
            label = "用户名",
            value = username,
            placeholder = "用户名",
            error = usernameError,
            readOnly = busy,
            onValueChange = onUsernameChange,
        )

        SourceFormInput(
            label = passwordLabel,
            value = password,
            placeholder = passwordLabel,
            info = passwordInfo,
            error = passwordError,
            isPassword = true,
            readOnly = busy,
            onValueChange = onPasswordChange,
        )

        extraContent()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SaltSpacing.spacingSub),
        ) {
            SaltTextButton(
                text = if (busy) "请稍候…" else saveText,
                onClick = onSave,
                enabled = !busy && saveEnabled,
                modifier = Modifier.weight(1f),
            )
            if (onCancel != null && cancelText != null) {
                SaltTextButton(
                    text = cancelText,
                    onClick = onCancel,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

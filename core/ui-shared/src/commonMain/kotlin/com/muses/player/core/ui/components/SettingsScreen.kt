package com.muses.player.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltRadius
import com.muses.player.core.ui.theme.SaltSpacing
import com.muses.player.core.uishared.platform.PlatformToast
import kotlinx.coroutines.launch

/**
 * 跨平台设置数据源（平台无关的音源描述）。
 *
 * Android 端由调用方从 [com.muses.player.core.data.db.SourceEntity] 映射而来；
 * Desktop 端由调用方从同样的 [com.muses.player.core.data.db.SourceEntity] 映射而来。
 */
data class SettingsSource(
    val id: String,
    val name: String,
    val url: String? = null,
    val username: String? = null,
)

/**
 * 跨平台设置页共用组件（U4 设置页共用化）。
 *
 * 纯 UI 展示 + 表单，业务逻辑由调用方通过回调注入：
 * - [onSave]：保存音源（新增/更新），成功返回 [Result.success]，失败返回 [Result.failure]；
 * - [onDelete]：删除音源；
 * - [sources]：当前音源列表（调用方管理生命周期）；
 * - [extraContent]：平台专属扩展区域（Android 放「关于/反馈」，Desktop 留空）。
 *
 * 约束：commonMain 零安卓 import，所有平台依赖经回调/expect 解决。
 */
@Composable
fun SettingsScreen(
    sources: List<SettingsSource>,
    onSave: suspend (name: String, url: String, username: String, password: String) -> Result<Unit>,
    onDelete: suspend (sourceId: String) -> Result<Unit>,
    modifier: Modifier = Modifier,
    extraContent: @Composable () -> Unit = {},
) {
    val salt = LocalSaltColors.current
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }

    // ---- navbar 顶部避让 ----
    // 与 SaltNavbar 同口径：CMP WindowInsets 跨平台取真实状态栏高度，桌面返回 0
    val statusBarTop = with(LocalDensity.current) {
        WindowInsets.statusBars.getTop(this).toDp()
    }
    val navbarPt = maxOf(SaltSpacing.navbarTopPaddingMin, statusBarTop) + 44.dp

    Box(modifier = modifier.fillMaxSize().background(salt.surface)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = navbarPt + 8.dp),
        ) {
            // ---- 音源管理 ----
            SettingsBlockTitle(text = "音源管理")
            Column(
                modifier = Modifier
                    .padding(horizontal = SaltSpacing.spacingSub)
                    .background(salt.surface1, RoundedCornerShape(SaltRadius.card))
                    .padding(vertical = 4.dp),
            ) {
                if (sources.isEmpty() && !showAdd) {
                    SaltEmpty(
                        title = "暂无音源",
                        description = "点击下方按钮添加 WebDAV 音源",
                        icon = TablerIcons.Folder,
                    )
                }

                sources.forEach { source ->
                    SourceListItem(
                        source = source,
                        onDelete = {
                            scope.launch {
                                onDelete(source.id)
                                    .onFailure { e -> PlatformToast.show("删除失败：${e.message}") }
                            }
                        },
                    )
                }

                if (showAdd) {
                    AddSourceForm(
                        onSave = { name, url, username, password ->
                            val result = onSave(name, url, username, password)
                            result.onSuccess { showAdd = false }
                            result.onFailure { e -> PlatformToast.show("保存失败：${e.message}") }
                        },
                        onCancel = { showAdd = false },
                    )
                }
            }

            // ---- 添加按钮 ----
            if (!showAdd) {
                SaltTextButton(
                    onClick = { showAdd = true },
                    text = "添加音源",
                    modifier = Modifier.padding(horizontal = SaltSpacing.spacingSub, vertical = 8.dp),
                )
            }

            // ---- 平台扩展区域 ----
            extraContent()

            // ---- 底部避让 MiniPlayer ----
            Spacer(Modifier.height(96.dp))
        }

        // ---- 吸顶导航栏 ----
        SaltNavbar(
            title = "设置",
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

// ---- 内部子组件 ----

/**
 * 音源列表行 —— SaltListItem + 右侧删除按钮。
 * 对照 SettingsPage.vue `.source-item`。
 */
@Composable
private fun SourceListItem(
    source: SettingsSource,
    onDelete: () -> Unit,
) {
    val salt = LocalSaltColors.current
    SaltListItem(
        title = source.name,
        subtitle = source.url,
        onClick = null,
        leading = {
            Box(
                modifier = Modifier
                    .padding(end = SaltSpacing.spacingSub)
                    .size(36.dp)
                    .background(salt.primary.copy(alpha = 0.12f), RoundedCornerShape(SaltRadius.sm)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = TablerIcons.Folder,
                    contentDescription = null,
                    tint = salt.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        after = {
            SaltTextButton(
                onClick = onDelete,
                text = "删除",
                size = SaltTextButtonSize.SMALL,
                destructive = true,
            )
        },
    )
}

/**
 * 添加音源表单 —— 对照 SettingsPage.vue `.add-source-card`。
 *
 * 字段：名称、地址、用户名（可空）、密码；保存/取消双按钮。
 */
@Composable
private fun AddSourceForm(
    onSave: suspend (name: String, url: String, username: String, password: String) -> Unit,
    onCancel: () -> Unit,
) {
    val salt = LocalSaltColors.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SaltSpacing.spacingSub, vertical = 8.dp)
            .background(salt.surface2, RoundedCornerShape(SaltRadius.sm))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "添加 WebDAV 音源",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = salt.text,
        )
        SaltTextField(label = "名称", value = name, onValueChange = { name = it })
        SaltTextField(label = "地址（http(s)://…）", value = url, onValueChange = { url = it })
        SaltTextField(label = "用户名（可空）", value = username, onValueChange = { username = it })
        SaltTextField(label = "密码", value = password, onValueChange = { password = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SaltTextButton(
                onClick = {
                    if (name.isBlank() || url.isBlank()) {
                        return@SaltTextButton
                    }
                    scope.launch {
                        onSave(name.trim(), url.trim().trimEnd('/'), username.trim(), password)
                    }
                },
                text = "保存",
            )
            SaltTextButton(
                onClick = onCancel,
                text = "取消",
                destructive = true,
            )
        }
    }
}

// ---- 公共辅助组件（供外部扩展区域复用） ----

/**
 * 设置分组标题 —— 对照 SettingsPage.vue `.m-block-title--default`。
 * （16px 顶距 / sm 字号 / 600 字重 / --m-text-2）。
 */
@Composable
fun SettingsBlockTitle(text: String) {
    val salt = LocalSaltColors.current
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = (13f * 1.4f).sp,
        letterSpacing = 0.02.sp,
        color = salt.text2,
        modifier = Modifier.padding(start = SaltSpacing.spacing, top = 16.dp, bottom = 8.dp),
    )
}

/**
 * 设置项左侧图标容器 —— 36dp 圆角方形 + primary 浅底。
 * （rgba(var(--m-primary-rgb), 0.12)，明暗主题自动跟随）。
 *
 * 注意 Web margin-right → Compose `padding(end)` 必须放链最外层（先留间距再画壳），
 * 放 size/background 之后会收缩背景本身（布局陷阱 #7）。
 */
@Composable
fun SettingsIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val salt = LocalSaltColors.current
    Box(
        modifier = Modifier
            .padding(end = SaltSpacing.spacingSub)
            .size(36.dp)
            .background(salt.primary.copy(alpha = 0.12f), RoundedCornerShape(SaltRadius.sm)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = salt.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ---- Salt 风格文本输入框 ----

/**
 * Salt 风格文本输入框 —— 对照 SettingsPage.vue `.m-input`。
 * 纯 Compose 实现，不依赖 Material OutlinedTextField（避免其默认边框观感）。
 */
@Composable
private fun SaltTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val salt = LocalSaltColors.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = salt.text3,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = salt.text, fontSize = 14.sp),
            cursorBrush = SolidColor(salt.primary),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp)
                .background(salt.surface3, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

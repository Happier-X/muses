package com.muses.player.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.data.db.SourceEntity
import com.muses.player.core.ui.components.SaltEmpty
import com.muses.player.core.ui.components.SaltTextButton
import com.muses.player.core.ui.components.SharedSourceItem
import com.muses.player.core.ui.components.SourceFormCard
import com.muses.player.core.ui.components.SourceListItem
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.uishared.platform.PlatformToast
import com.muses.player.desktop.di.DesktopContainer
import com.muses.player.desktop.di.DesktopCredentials
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 桌面音源管理页（从 SettingsScreen 拆分）：调用 ui-shared 共用
 * [SourceListItem] + [SourceFormCard]，业务逻辑（DAO / 凭据）经本地回调注入。
 *
 * - 列表行：名称 + URL + 用户名展示，编辑切换表单态、删除清库 + 凭据；
 * - 表单：名称/地址/用户名/密码 + 保存/取消（编辑态密码留空保留原密码）。
 */
@Composable
fun SourceManagerScreen() {
    val salt = LocalSaltColors.current
    val scope = rememberCoroutineScope()
    var sources by remember { mutableStateOf<List<SourceEntity>>(emptyList()) }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SourceEntity?>(null) }
    var errorText by remember { mutableStateOf("") }

    // 表单受控字段（新增与编辑共用；进入编辑时回填）
    var formName by remember { mutableStateOf("") }
    var formUrl by remember { mutableStateOf("") }
    var formUsername by remember { mutableStateOf("") }
    var formPassword by remember { mutableStateOf("") }
    var formBusy by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            runCatching {
                sources = DesktopContainer.database().sourceDao().observeAll().first()
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    fun openAdd() {
        editing = null
        formName = ""
        formUrl = ""
        formUsername = ""
        formPassword = ""
        errorText = ""
        showAdd = true
    }

    fun openEdit(entity: SourceEntity) {
        editing = entity
        formName = entity.name
        formUrl = entity.url.orEmpty()
        formUsername = entity.username.orEmpty()
        formPassword = ""
        errorText = ""
        showAdd = true
    }

    fun submitForm() {
        val targetEdit = editing
        if (formName.isBlank() || formUrl.isBlank()) {
            errorText = "请填写名称与地址"
            return
        }
        formBusy = true
        scope.launch {
            val result = runCatching {
                val now = System.currentTimeMillis()
                if (targetEdit == null) {
                    val id = UUID.randomUUID().toString()
                    DesktopContainer.database().sourceDao().upsert(
                        SourceEntity(
                            id = id,
                            name = formName.trim(),
                            type = "WEBDAV",
                            url = formUrl.trim().trimEnd('/'),
                            username = formUsername.trim().ifBlank { null },
                            createdAt = now,
                            updatedAt = now,
                        ),
                    )
                    if (formPassword.isNotBlank()) {
                        DesktopCredentials().savePassword(id, formPassword)
                    }
                } else {
                    DesktopContainer.database().sourceDao().upsert(
                        targetEdit.copy(
                            name = formName.trim(),
                            url = formUrl.trim().trimEnd('/'),
                            username = formUsername.trim().ifBlank { null },
                            updatedAt = now,
                        ),
                    )
                    if (formPassword.isNotBlank()) {
                        DesktopCredentials().savePassword(targetEdit.id, formPassword)
                    }
                }
                reload()
            }
            formBusy = false
            result
                .onSuccess {
                    showAdd = false
                    editing = null
                    errorText = ""
                }
                .onFailure { e ->
                    errorText = "保存失败：${e.message}"
                    PlatformToast.show("保存失败：${e.message}")
                }
        }
    }

    fun deleteSource(entity: SourceEntity) {
        scope.launch {
            runCatching {
                DesktopContainer.database().sourceDao().deleteById(entity.id)
                DesktopContainer.database().songDao().deleteBySource(entity.id)
                DesktopCredentials().clearPassword(entity.id)
                reload()
            }.onFailure { e ->
                errorText = "删除失败：${e.message}"
                PlatformToast.show("删除失败：${e.message}")
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "音源",
            color = salt.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        if (errorText.isNotBlank()) {
            Text(text = errorText, color = salt.danger, fontSize = 13.sp)
        }
        if (sources.isEmpty() && !showAdd) {
            Spacer(modifier = Modifier.height(24.dp))
            SaltEmpty(
                title = "暂无音源",
                description = "点击下方按钮添加 WebDAV 音源",
                icon = TablerIcons.Folder,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height((sources.size * 150).coerceAtMost(600).dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(sources, key = { it.id }) { entity ->
                    SourceListItem(
                        item = entity.toSharedSourceItem(),
                        onEdit = { openEdit(entity) },
                        onDelete = { deleteSource(entity) },
                    )
                }
            }
        }

        if (showAdd) {
            SourceFormCard(
                name = formName,
                onNameChange = { formName = it },
                showNameField = true,
                url = formUrl,
                onUrlChange = { formUrl = it },
                username = formUsername,
                onUsernameChange = { formUsername = it },
                password = formPassword,
                onPasswordChange = { formPassword = it },
                passwordLabel = if (editing != null) "新密码" else "密码",
                passwordInfo = if (editing != null) "留空则保留原密码" else null,
                busy = formBusy,
                saveText = "保存",
                onSave = ::submitForm,
                cancelText = "取消",
                onCancel = {
                    showAdd = false
                    editing = null
                    errorText = ""
                },
            )
        } else {
            SaltTextButton(
                onClick = ::openAdd,
                text = "添加音源",
            )
        }
        Spacer(modifier = Modifier.height(96.dp))
    }
}

/** Room SourceEntity → 共用 SharedSourceItem 映射 */
private fun SourceEntity.toSharedSourceItem() = SharedSourceItem(
    id = id,
    name = name,
    subtitle = username?.let { "WebDAV · $it@${url.orEmpty().removePrefix("https://").removePrefix("http://")}" }
        ?: ("WebDAV · " + url.orEmpty()),
    detail = url.orEmpty(),
)

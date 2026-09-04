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
 * - 浏览子页：表单保存后进入共用 [WebDavBrowseScreen]（BROWSE 子页，导航内切换）；
 *   multiple 多选批量建源（对照安卓添加流程），编辑态 single 单选回填 path。
 */
@Composable
fun SourceManagerScreen() {
    val salt = LocalSaltColors.current
    val scope = rememberCoroutineScope()
    var sources by remember { mutableStateOf<List<SourceEntity>>(emptyList()) }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SourceEntity?>(null) }
    var errorText by remember { mutableStateOf("") }
    var browseSession by remember { mutableStateOf<BrowseSession?>(null) }

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

    /** 浏览确认：multiple 批量建源 / single 回填编辑态 path（对照安卓 consumeBrowseResult）。 */
    fun confirmBrowse(paths: List<String>) {
        val session = browseSession ?: return
        if (paths.isEmpty()) {
            browseSession = null
            return
        }
        if (session.mode == "single") {
            val targetEdit = editing
            if (targetEdit != null) {
                scope.launch {
                    runCatching {
                        DesktopContainer.database().sourceDao().upsert(
                            targetEdit.copy(
                                path = paths[0],
                                updatedAt = System.currentTimeMillis(),
                            ),
                        )
                        reload()
                    }.onFailure { e ->
                        errorText = "回填目录失败：${e.message}"
                    }
                }
            }
            browseSession = null
            showAdd = false
            editing = null
            return
        }
        formBusy = true
        scope.launch {
            val result = runCatching {
                val now = System.currentTimeMillis()
                for (path in paths) {
                    val id = UUID.randomUUID().toString()
                    DesktopContainer.database().sourceDao().upsert(
                        SourceEntity(
                            id = id,
                            name = path.trimEnd('/').substringAfterLast('/').ifBlank { path },
                            type = "WEBDAV",
                            url = session.serverUrl,
                            path = path,
                            username = session.username.ifBlank { null },
                            createdAt = now,
                            updatedAt = now,
                        ),
                    )
                    if (session.password.isNotBlank()) {
                        DesktopCredentials().savePassword(id, session.password)
                    }
                }
                reload()
            }
            formBusy = false
            result
                .onSuccess {
                    browseSession = null
                    showAdd = false
                    editing = null
                    errorText = ""
                }
                .onFailure { e ->
                    errorText = "添加失败：${e.message}"
                    PlatformToast.show("添加失败：${e.message}")
                }
        }
    }

    /** 表单保存后进入浏览：先落库连接信息，再打开 BROWSE 子页（对照安卓 submitAdd）。 */
    fun submitThenBrowse() {
        val targetEdit = editing
        if (formUrl.isBlank() || formUsername.isBlank()) {
            errorText = "请填写地址与用户名"
            return
        }
        if (targetEdit == null && formName.isBlank()) {
            errorText = "请填写名称与地址"
            return
        }
        if (targetEdit == null && formPassword.isBlank()) {
            errorText = "请填写密码"
            return
        }
        formBusy = true
        scope.launch {
            val resolved = runCatching {
                if (targetEdit == null) {
                    // 新增：先建一个占位源拿到 id 存密码，浏览确认后再批量建子目录源
                    val id = UUID.randomUUID().toString()
                    val now = System.currentTimeMillis()
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
                    DesktopCredentials().savePassword(id, formPassword)
                    Triple(formUrl.trim().trimEnd('/'), formUsername.trim(), formPassword)
                } else {
                    val password = formPassword.ifBlank {
                        DesktopCredentials().getPassword(targetEdit.id).orEmpty()
                    }
                    if (password.isBlank()) throw IllegalStateException("WebDAV 密码不存在，请输入新密码。")
                    if (formPassword.isNotBlank()) {
                        DesktopCredentials().savePassword(targetEdit.id, formPassword)
                    }
                    Triple(
                        formUrl.trim().trimEnd('/'),
                        formUsername.trim(),
                        password,
                    )
                }
            }
            formBusy = false
            resolved
                .onSuccess { (url, user, pass) ->
                    browseSession = BrowseSession(
                        mode = if (targetEdit == null) "multiple" else "single",
                        serverUrl = url,
                        username = user,
                        password = pass,
                    )
                    errorText = ""
                }
                .onFailure { e ->
                    errorText = e.message ?: "保存失败。"
                }
        }
    }

    val session = browseSession
    if (session != null) {
        WebDavBrowseScreen(
            mode = session.mode,
            initialPath = "/",
            serverUrl = session.serverUrl,
            username = session.username,
            password = session.password,
            onBack = { browseSession = null },
            onConfirm = ::confirmBrowse,
        )
        return
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
                extraContent = {
                    SaltTextButton(
                        onClick = ::submitThenBrowse,
                        text = if (editing != null) "浏览并回填目录" else "保存并浏览目录",
                    )
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

/** 桌面浏览会话（BROWSE 子页参数；对照安卓导航参数 connection/initialPath/mode）。 */
private data class BrowseSession(
    val mode: String,
    val serverUrl: String,
    val username: String,
    val password: String,
)

/** Room SourceEntity → 共用 SharedSourceItem 映射 */
private fun SourceEntity.toSharedSourceItem() = SharedSourceItem(
    id = id,
    name = name,
    subtitle = username?.let { "WebDAV · $it@${url.orEmpty().removePrefix("https://").removePrefix("http://")}" }
        ?: ("WebDAV · " + url.orEmpty()),
    detail = url.orEmpty(),
)

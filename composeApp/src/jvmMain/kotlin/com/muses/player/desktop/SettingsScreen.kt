package com.muses.player.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.data.db.SourceEntity
import com.muses.player.desktop.di.DesktopContainer
import com.muses.player.desktop.di.DesktopCredentials
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 桌面设置页（S3b 最小版）：WebDAV 音源增删 + 扫描入口。
 * 凭据经 DesktopCredentials（DPAPI）保存，不落明文。
 */
@Composable
fun SettingsScreen() {
    val scope = rememberCoroutineScope()
    var sources by remember { mutableStateOf<List<SourceEntity>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            runCatching {
                sources = DesktopContainer.database().sourceDao().observeAll().first()
            }.onFailure { e -> status = "读取失败：${e.message}" }
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF11111B)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "设置",
                color = Color(0xFFCDD6F4),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            AddButton("添加音源") { showAdd = true }
        }
        if (status.isNotBlank()) {
            Text(text = status, color = Color(0xFFF38BA8), fontSize = 13.sp)
        }
        if (showAdd) {
            AddSourceCard(
                onDone = { showAdd = false; reload() },
                onCancel = { showAdd = false },
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sources, key = { it.id }) { source ->
                SourceCard(
                    source = source,
                    onDelete = {
                        scope.launch {
                            runCatching {
                                DesktopContainer.database().sourceDao().deleteById(source.id)
                                DesktopContainer.database().songDao().deleteBySource(source.id)
                                DesktopCredentials().clearPassword(source.id)
                                reload()
                                status = "已删除 ${source.name}"
                            }.onFailure { e -> status = "删除失败：${e.message}" }
                        }
                    },
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Muses Desktop · 首版最小可用",
            color = Color(0xFF585B70),
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun SourceCard(source: SourceEntity, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF181825))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = source.name, color = Color(0xFFCDD6F4), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = source.url ?: "", color = Color(0xFF7F849C), fontSize = 12.sp)
            if (!source.username.isNullOrBlank()) {
                Text(text = "用户：${source.username}", color = Color(0xFF7F849C), fontSize = 12.sp)
            }
        }
        DeleteButton("删除", onDelete)
    }
}

@Composable
private fun AddSourceCard(onDone: () -> Unit, onCancel: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E2E))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "添加 WebDAV 音源", color = Color(0xFFCDD6F4), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        SettingField("名称", name) { name = it }
        SettingField("地址（http(s)://…）", url) { url = it }
        SettingField("用户名（可空）", username) { username = it }
        SettingField("密码", password, isPassword = true) { password = it }
        if (error.isNotBlank()) {
            Text(text = error, color = Color(0xFFF38BA8), fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AddButton("保存") {
                if (name.isBlank() || url.isBlank()) {
                    error = "名称与地址不能为空"
                    return@AddButton
                }
                scope.launch {
                    runCatching {
                        val id = UUID.randomUUID().toString()
                        val now = System.currentTimeMillis()
                        DesktopContainer.database().sourceDao().upsert(
                            SourceEntity(
                                id = id,
                                name = name.trim(),
                                type = "WEBDAV",
                                url = url.trim().trimEnd('/'),
                                username = username.trim().ifBlank { null },
                                createdAt = now,
                                updatedAt = now,
                            ),
                        )
                        if (password.isNotBlank()) {
                            DesktopCredentials().savePassword(id, password)
                        }
                        onDone()
                    }.onFailure { e -> error = "保存失败：${e.message}" }
                }
            }
            AddButton("取消", danger = true, onClick = onCancel)
        }
    }
}

@Composable
private fun SettingField(
    label: String,
    value: String,
    isPassword: Boolean = false,
    onChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, color = Color(0xFF7F849C), fontSize = 12.sp)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(color = Color(0xFFCDD6F4), fontSize = 14.sp),
            cursorBrush = SolidColor(Color(0xFF89B4FA)),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF313244))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun AddButton(text: String, danger: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (danger) Color(0xFF45475A) else Color(0xFF89B4FA))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (danger) Color(0xFFCDD6F4) else Color(0xFF11111B),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DeleteButton(text: String, onClick: () -> Unit) {
    AddButton(text, danger = true, onClick = onClick)
}

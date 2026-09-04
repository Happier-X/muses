package com.muses.player.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.muses.player.core.data.db.SourceEntity
import com.muses.player.core.ui.components.SettingsScreen
import com.muses.player.core.ui.components.SettingsSource
import com.muses.player.desktop.di.DesktopContainer
import com.muses.player.desktop.di.DesktopCredentials
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 桌面设置页（共用化后）：调用 ui-shared 共用 [SettingsScreen]，
 * 业务逻辑（DAO / 凭据）经回调注入。
 */
@Composable
fun SettingsScreen() {
    val scope = rememberCoroutineScope()
    var sources by remember { mutableStateOf<List<SettingsSource>>(emptyList()) }

    fun reload() {
        scope.launch {
            runCatching {
                val entities = DesktopContainer.database().sourceDao().observeAll().first()
                sources = entities.map { it.toSettingsSource() }
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    SettingsScreen(
        sources = sources,
        onSave = { name, url, username, password ->
            runCatching {
                val id = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                DesktopContainer.database().sourceDao().upsert(
                    SourceEntity(
                        id = id,
                        name = name,
                        type = "WEBDAV",
                        url = url,
                        username = username.ifBlank { null },
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                if (password.isNotBlank()) {
                    DesktopCredentials().savePassword(id, password)
                }
                reload()
            }.fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { Result.failure(it) },
            )
        },
        onDelete = { sourceId ->
            runCatching {
                DesktopContainer.database().sourceDao().deleteById(sourceId)
                DesktopContainer.database().songDao().deleteBySource(sourceId)
                DesktopCredentials().clearPassword(sourceId)
                reload()
            }.fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { Result.failure(it) },
            )
        },
    )
}

/** Room SourceEntity → 跨平台 SettingsSource 映射 */
private fun SourceEntity.toSettingsSource() = SettingsSource(
    id = id,
    name = name,
    url = url,
    username = username,
)

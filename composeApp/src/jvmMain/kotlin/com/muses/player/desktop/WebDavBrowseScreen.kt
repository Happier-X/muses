package com.muses.player.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.muses.player.core.ui.components.SaltTextButton
import com.muses.player.core.ui.components.SaltTextButtonSize
import com.muses.player.core.ui.components.WebDavBrowseItem
import com.muses.player.core.ui.components.WebDavBrowseList
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.desktop.webdav.DesktopWebDavBrowseLoader
import kotlinx.coroutines.launch

/**
 * 桌面 WebDAV 目录浏览页（浏览页共用化的桌面消费端）。
 *
 * 调用 ui-shared 共用 [WebDavBrowseList]，业务经 [DesktopWebDavBrowseLoader] 回调注入：
 * - single：单选确认（编辑回填目录，由 [onConfirm] 带回单路径）；
 * - multiple：多选确认（添加流程，由 [onConfirm] 带回多路径）。
 * 加载/错误均为本地 state，不碰播放页、曲库主页、刮削页。
 */
@Composable
fun WebDavBrowseScreen(
    mode: String,
    initialPath: String,
    serverUrl: String,
    username: String,
    password: String,
    onBack: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val salt = LocalSaltColors.current
    val scope = rememberCoroutineScope()
    var currentPath by remember(mode, serverUrl, username) {
        mutableStateOf(DesktopWebDavBrowseLoader.normalizePath(initialPath))
    }
    var directories by remember { mutableStateOf<List<WebDavBrowseItem>>(emptyList()) }
    var selectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun load(path: String) {
        val target = DesktopWebDavBrowseLoader.normalizePath(path)
        isLoading = true
        errorText = null
        scope.launch {
            runCatching {
                DesktopWebDavBrowseLoader.listDirectories(serverUrl, target, username, password)
                    .map { it.toShared(serverUrl) }
            }.onSuccess {
                currentPath = target
                directories = it
                isLoading = false
            }.onFailure { e ->
                isLoading = false
                errorText = e.message ?: "读取 WebDAV 目录失败。"
            }
        }
    }

    LaunchedEffect(mode, serverUrl, username) { load(initialPath) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(salt.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (mode == "single") "选择目录" else "选择文件夹",
            color = salt.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        SaltTextButton(
            onClick = {
                selectedPaths = emptySet()
                onBack()
            },
            text = "返回",
            size = SaltTextButtonSize.SMALL,
        )
        Spacer(modifier = Modifier.height(4.dp))
        WebDavBrowseList(
            mode = mode,
            currentPath = currentPath,
            directories = directories,
            selectedPaths = selectedPaths,
            isLoading = isLoading,
            canGoParent = DesktopWebDavBrowseLoader.parentPath(currentPath) != null && !isLoading,
            onGoParent = {
                DesktopWebDavBrowseLoader.parentPath(currentPath)?.let { load(it) }
            },
            onToggleSelection = { path ->
                selectedPaths = if (selectedPaths.contains(path)) {
                    selectedPaths - path
                } else {
                    selectedPaths + path
                }
            },
            onOpenDirectory = { load(it) },
            onConfirmSingle = { onConfirm(listOf(it)) },
            onConfirmMultiple = { onConfirm(it) },
            modifier = Modifier.weight(1f),
            errorText = errorText,
            onDismissError = { errorText = null },
        )
    }
}

/** 桌面加载器条目 → 共用浏览条目映射（url 键取“目录路径”，与安卓 path 键同语义）。 */
private fun DesktopWebDavBrowseLoader.Entry.toShared(serverUrl: String): WebDavBrowseItem {
    return WebDavBrowseItem(
        name = name,
        url = extractBrowsePath(url, serverUrl, name),
    )
}

/**
 * 从 PROPFIND 完整 URL 还原“目录路径”键（对照安卓 `extractPathFromUrl`：
 * 剥离 server 地址自带的前缀路径，失败回退名称）。
 */
private fun extractBrowsePath(fullUrl: String, serverUrl: String, name: String): String {
    return runCatching {
        val basePath = DesktopWebDavBrowseLoader.normalizePath(java.net.URI(serverUrl).path)
        val fullPath = DesktopWebDavBrowseLoader.normalizePath(java.net.URI(fullUrl).path)
        if (fullPath.startsWith(basePath)) {
            val relative = fullPath.removePrefix(basePath)
            if (relative.isEmpty()) "/" else relative
        } else {
            fullPath
        }
    }.getOrNull() ?: name
}

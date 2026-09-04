package com.muses.player.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.muses.player.core.ui.icons.TablerIcons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.muses.player.core.ui.theme.LocalMusesHazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.log.ErrorLogStore
import com.muses.player.core.ui.components.SaltListItem
import com.muses.player.core.ui.components.SaltNavbar
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltRadius
import com.muses.player.core.ui.theme.SaltSpacing
import com.muses.player.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val errorLogStore: ErrorLogStore,
) : ViewModel() {

    /** 最近错误摘要 —— 供「复制报错日志」条目副标题 */
    val latestErrorSummary: StateFlow<String?> = errorLogStore.latestSummary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * 复制用日志全文：文件头（版本 + 导出时间）+ 缓冲正文（含上次会话崩溃段）。
     * 无任何日志时返回 null，由 UI 层提示「暂无可复制的日志」。
     */
    suspend fun dumpLogs(): String? {
        val body = errorLogStore.dump() ?: return null
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        return "[Muses 错误日志] v${BuildConfig.VERSION_NAME} @ $time\n$body"
    }
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val salt = LocalSaltColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val hazeState = rememberHazeState()
    CompositionLocalProvider(LocalMusesHazeState provides hazeState) {
        Box(modifier = modifier.fillMaxSize()) {
            val navbarTopPadding = with(LocalDensity.current) {
                WindowInsets.statusBars.getTop(this).toDp()
            }.coerceAtLeast(16.dp) + 44.dp
            Box(
                Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .background(salt.surface),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = navbarTopPadding + 8.dp),
                ) {
            // ---- 关于 ----
            SettingsBlockTitle(text = "关于")
            Column(
                modifier = Modifier
                    .padding(horizontal = SaltSpacing.spacingSub)
                    .background(salt.surface1, RoundedCornerShape(SaltRadius.card))
                    .padding(vertical = 4.dp),
            ) {
                // Muses 版本
                SaltListItem(
                    title = "Muses",
                    subtitle = "应用版本 ${BuildConfig.VERSION_NAME}",
                    onClick = null,
                    leading = {
                        SettingsIcon(icon = TablerIcons.Info)
                    },
                )
                // 检查更新
                SaltListItem(
                    title = "检查更新",
                    subtitle = if (checking) "正在检查更新…" else null,
                    onClick = {
                        if (checking) return@SaltListItem
                        checking = true
                        scope.launch {
                            val result = checkLatestRelease(BuildConfig.VERSION_NAME)
                            toastMessage = if (result == null) {
                                "检查更新失败，请稍后重试"
                            } else {
                                val (tag, url) = result
                                val latestVer = tag.removePrefix("v")
                                val currentVer = BuildConfig.VERSION_NAME
                                    .removeSuffix("-miui")
                                    .substringBefore("-")
                                if (compareVersions(latestVer, currentVer) <= 0) {
                                    "已是最新版本"
                                } else {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    "发现新版本 $tag"
                                }
                            }
                            checking = false
                        }
                    },
                    leading = {
                        SettingsIcon(icon = TablerIcons.Refresh)
                    },
                )
            }

            // ---- 反馈 ----（任务 08-26-settings-log-viewer）
            val latestSummary by viewModel.latestErrorSummary.collectAsState()
            SettingsBlockTitle(text = "反馈")
            Column(
                modifier = Modifier
                    .padding(horizontal = SaltSpacing.spacingSub)
                    .background(salt.surface1, RoundedCornerShape(SaltRadius.card))
                    .padding(vertical = 4.dp),
            ) {
                SaltListItem(
                    title = "复制报错日志",
                    subtitle = latestSummary ?: "暂无报错记录",
                    onClick = {
                        scope.launch {
                            val text = viewModel.dumpLogs()
                            if (text == null) {
                                toastMessage = "暂无可复制的日志"
                            } else {
                                val clipboard = context.getSystemService(
                                    Context.CLIPBOARD_SERVICE,
                                ) as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText("Muses 报错日志", text),
                                )
                                toastMessage = "已复制报错日志"
                            }
                        }
                    },
                    leading = {
                        SettingsIcon(icon = TablerIcons.BugReport)
                    },
                )
            }

            // m-content-pb：底部避让 MiniPlayer
            Spacer(Modifier.height(96.dp))
                }
            }
            SaltNavbar(
                title = "设置",
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }

    // Toast 显示（m-toast center 行为层；样式组件待后续统一落地）
    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            delay(2000)
            toastMessage = null
        }
    }
}

/**
 * 检查 GitHub 最新 release —— 对照 SettingsPage.vue checkUpdate。
 * 返回 (tag, html_url)；网络/格式异常返回 null 并由调用方区分提示不足——
 * 这里简化为统一失败文案，与 Web 层 403 分支的细分提示在行为层等价。
 */
private suspend fun checkLatestRelease(currentVersion: String): Pair<String, String>? {
    return withContext(Dispatchers.IO) {
        var connection: java.net.HttpURLConnection? = null
        try {
            connection = java.net.URL("https://api.github.com/repos/Happier-X/muses/releases/latest")
                .openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "Muses/${BuildConfig.VERSION_NAME} (Android)")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            val code = connection.responseCode
            if (code != 200) {
                return@withContext null
            }
            val body = connection.inputStream.bufferedReader().readText()
            val tag = Regex("\"tag_name\"\\s*:\\s*\"(v\\d+\\.\\d+\\.\\d+)\"").find(body)?.groupValues?.getOrNull(1)
            val htmlUrl = Regex("\"html_url\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.getOrNull(1)
            if (tag != null && htmlUrl != null) tag to htmlUrl else null
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}

/**
 * 设置分组标题 —— 对照 SettingsPage.vue 的 `.m-block-title--default`
 * （本页覆写：16px 顶距 / sm 字号 / 600 字重 / --m-text-2）。
 */
@Composable
private fun SettingsBlockTitle(text: String) {
    val salt = LocalSaltColors.current
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = (13f * 1.4f).sp,
        letterSpacing = 0.02.sp,
        color = salt.text2,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

/**
 * 设置项左侧图标容器 —— 36dp 圆角方形 + primary 浅底
 * （rgba(var(--m-primary-rgb), 0.12)，明暗主题自动跟随）。
 */
@Composable
private fun SettingsIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val salt = LocalSaltColors.current
    Box(
        modifier = Modifier
            // margin-right: var(--m-spacing-sub) —— 必须在链最外层（先留间距再画壳），
            // 放 size/background 之后会收缩背景本身
            .padding(end = SaltSpacing.spacingSub)
            .size(36.dp)
            .background(salt.primary.copy(alpha = 0.12f), RoundedCornerShape(SaltRadius.sm)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // aria-hidden
            tint = salt.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** 版本号比较（语义同 Web 层 compareVersions） */
private fun compareVersions(a: String, b: String): Int {
    val partsA = a.split('.').map { it.toIntOrNull() ?: 0 }
    val partsB = b.split('.').map { it.toIntOrNull() ?: 0 }
    val len = maxOf(partsA.size, partsB.size)
    for (i in 0 until len) {
        val va = partsA.getOrElse(i) { 0 }
        val vb = partsB.getOrElse(i) { 0 }
        if (va > vb) return 1
        if (va < vb) return -1
    }
    return 0
}

package com.muses.player.settings

import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.ViewModel
import com.muses.player.core.appupdate.checkLatestRelease
import com.muses.player.core.data.log.ErrorLogStore
import com.muses.player.core.data.repository.SettingsRepository
import com.muses.player.core.ui.components.SettingsAboutFeedbackContent
import com.muses.player.core.ui.components.SettingsBlockTitle
import com.muses.player.core.ui.components.SettingsScreen
import com.muses.player.feature.shell.platform.AppVersionProvider
import com.muses.player.feature.shell.platform.InAppUpdateSection
import com.muses.player.feature.shell.platform.XiaomiIslandSettingRow
import com.muses.player.feature.shell.platform.rememberShellPlatformActions
import com.muses.player.feature.shell.platform.supportsInAppUpdate

import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsViewModel constructor(
    private val errorLogStore: ErrorLogStore,
) : ViewModel() {

    /**
     * 复制用日志全文：文件头（版本 + 导出时间）+ 缓冲正文（含上次会话崩溃段）。
     * 无任何日志时返回 null，由 UI 层提示「暂无可复制的日志」。
     */
    suspend fun dumpLogs(): String? {
        val body = errorLogStore.dump() ?: return null
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        return "[Muses 错误日志] v${versionName()} @ $time\n$body"
    }

    /** 版本号经 Koin [AppVersionProvider] 注入（原 BuildConfig 仅安卓可读） */
    private fun versionName(): String =
        org.koin.core.context.GlobalContext.get().get<AppVersionProvider>().versionName
}

/**
 * 设置页（U22 双端共享）：ui-shared 共享容器 + 共享「关于/反馈」扩展区块；
 * 平台动作（浏览器/剪贴板）经 rememberShellPlatformActions 注入，版本号经
 * Koin AppVersionProvider，报错日志同源 ErrorLogStore（桌面绑定 RingBufferErrorLogStore）。
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
    // 窄屏底栏只保留「探索 / 曲库 / 设置」三项时，刮削/音源经此进入；
    // 宽屏 Rail 自带这两项，传 null 即隐藏本区块。
    onOpenSources: (() -> Unit)? = null,
    onOpenScrape: (() -> Unit)? = null,
    // AI 服务配置二级页入口（设置页「AI 推荐」→「AI 服务」箭头进入）。
    onOpenAiSettings: () -> Unit,
) {
    val actions = rememberShellPlatformActions()
    val versionProvider = koinInject<AppVersionProvider>()
    val settingsRepository = koinInject<SettingsRepository>()
    val coroutineScope = rememberCoroutineScope()
    val lyricsEnabled by settingsRepository.miniPlayerLyricsEnabled.collectAsState(initial = false)
    val notificationLyricsEnabled by settingsRepository.notificationLyricsEnabled.collectAsState(initial = false)


    // U15：设置页共享组件（音源区块已移除，独立音源页承载）；「关于/反馈」扩展区为
    // 双端共享实现（SettingsAboutFeedbackContent），平台动作经回调注入。
    // 视觉：官方 Settings 范式——每个分组一张 Card，开关/入口行用 miuix Preference 系列。
    SettingsScreen(
        modifier = modifier,
        extraContent = {
                // ---- 播放设置 ----
                SettingsBlockTitle("播放")
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    SwitchPreference(
                        title = "播放控件歌词",
                        checked = lyricsEnabled,
                        onCheckedChange = { coroutineScope.launch { settingsRepository.setMiniPlayerLyricsEnabled(it) } },
                    )
                    SwitchPreference(
                        title = "通知栏歌词",
                        checked = notificationLyricsEnabled,
                        onCheckedChange = { coroutineScope.launch { settingsRepository.setNotificationLyricsEnabled(it) } },
                    )
                    // 小米超级岛（仅 HyperOS 安卓渲染，桌面为空实现）
                    XiaomiIslandSettingRow()
                }

                // ---- AI 推荐（一级只留总开关；地址/模型/Key 收进二级页） ----
                AiRecommendSettingSection(onOpenAiSettings = onOpenAiSettings)

                // ---- 应用更新（Windows 应用内更新卡片；安卓空实现，走共享外链检查项） ----
                if (supportsInAppUpdate) {
                    InAppUpdateSection(
                        versionName = versionProvider.versionName,
                        onOpenUrl = actions.openUrl,
                    )
                }

                // ---- 工具（窄屏底栏未收纳项的入口；宽屏 Rail 自带时不传回调即隐藏） ----
                if (onOpenSources != null || onOpenScrape != null) {
                    SettingsBlockTitle("工具")
                    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                        onOpenSources?.let { open ->
                            ArrowPreference(
                                title = "音源",
                                onClick = open,
                            )
                        }
                        onOpenScrape?.let { open ->
                            ArrowPreference(
                                title = "刮削",
                                onClick = open,
                            )
                        }
                    }
                }

                // ---- 关于 ----
                SettingsAboutFeedbackContent(
                    versionName = versionProvider.versionName,
                    onOpenUrl = actions.openUrl,
                    onCopyToClipboard = actions.copyToClipboard,
                    onCheckUpdate = { current -> checkLatestRelease(current) },

                    onDumpLogs = { viewModel.dumpLogs() },
                    showCheckUpdate = !supportsInAppUpdate,
                )
            },
        )
}



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
import androidx.lifecycle.viewModelScope
import com.muses.player.core.appupdate.checkLatestRelease
import com.muses.player.core.data.log.ErrorLogStore
import com.muses.player.core.data.repository.SettingsRepository
import com.muses.player.core.ui.components.SettingsAboutFeedbackContent
import com.muses.player.core.ui.components.SettingsBlockTitle
import com.muses.player.core.ui.components.SettingsIcon
import com.muses.player.core.ui.components.SettingsScreen
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.feature.shell.platform.AppVersionProvider
import com.muses.player.feature.shell.platform.InAppUpdateSection
import com.muses.player.feature.shell.platform.XiaomiIslandSettingRow
import com.muses.player.feature.shell.platform.rememberShellPlatformActions
import com.muses.player.feature.shell.platform.supportsInAppUpdate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsViewModel constructor(
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
    // 窄屏底栏只摆 5 项（曲库/专辑/艺术家/歌单/设置）时，刮削/音源经此进入；
    // 宽屏 Rail 自带这两项，传 null 即隐藏本区块。
    onOpenSources: (() -> Unit)? = null,
    onOpenScrape: (() -> Unit)? = null,
) {
    val actions = rememberShellPlatformActions()
    val versionProvider = koinInject<AppVersionProvider>()
    val settingsRepository = koinInject<SettingsRepository>()
    val coroutineScope = rememberCoroutineScope()
    val lyricsEnabled by settingsRepository.miniPlayerLyricsEnabled.collectAsState(initial = false)
    val notificationLyricsEnabled by settingsRepository.notificationLyricsEnabled.collectAsState(initial = false)
    val latestSummary by viewModel.latestErrorSummary.collectAsState()

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
                        title = "播放控件显示歌词",
                        summary = "开启后播放控件将使用当前歌词替换艺术家，长歌词会随播放自动滚动",
                        checked = lyricsEnabled,
                        startAction = { SettingsIcon(TablerIcons.MusicNote) },
                        onCheckedChange = { coroutineScope.launch { settingsRepository.setMiniPlayerLyricsEnabled(it) } },
                    )
                    SwitchPreference(
                        title = "媒体通知显示歌词",
                        summary = "开启后通知卡片标题显示当前歌词，下方显示歌曲标题与艺术家",
                        checked = notificationLyricsEnabled,
                        startAction = { SettingsIcon(TablerIcons.QueueMusic) },
                        onCheckedChange = { coroutineScope.launch { settingsRepository.setNotificationLyricsEnabled(it) } },
                    )
                    // 小米超级岛（仅 HyperOS 安卓渲染，桌面为空实现）
                    XiaomiIslandSettingRow()
                }

                // ---- AI 推荐（首页「猜你喜欢」的配置面：服务商/模型/Key） ----
                AiRecommendSettingSection()

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
                                summary = "本地目录 / WebDAV 管理与扫描",
                                startAction = { SettingsIcon(TablerIcons.Folder) },
                                onClick = open,
                            )
                        }
                        onOpenScrape?.let { open ->
                            ArrowPreference(
                                title = "刮削",
                                summary = "封面 / 歌词 / 标签补全队列",
                                startAction = { SettingsIcon(TablerIcons.Checklist) },
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
                    errorLogSummary = latestSummary,
                    onDumpLogs = { viewModel.dumpLogs() },
                    showCheckUpdate = !supportsInAppUpdate,
                )
            },
        )
}



# 全项目切换 Compose Multiplatform（U18 起）

## 目标形态
所有 UI 代码收敛到 CMP（commonMain + expect/actual），双端共用一套导航壳：
- `:app` 瘦身为 Android 薄入口（MainActivity/MusesApplication/AppKoinModule/manifest/res），不再有任何 UI 代码
- 新建 `:feature:shell`（KMP 双 target android+jvm，四件套插件，同 feature:library 形态）：NavDestination/TabsLayout/MusesApp/MainViewModel/SettingsScreen + CMP Navigation
- feature 层 androidMain 残留全量上收：playlist 3 文件、sources 3 文件（SAF 选目录抽口）、player 5 文件（歌词链跨平台重写）
- `:composeApp` 切共享壳：DesktopTitleBar/托盘/SMTC 保留，DesktopSidebar+enum 切屏废弃，改用共享 MusesApp（CMP Navigation 返回栈；窗口宽 1280 ≥ 768 断点自动走 TabsLayout 的 260px 侧栏形态）
- 删除空壳 `:core:ui`；`:app` 清理 androidx UI 依赖

## 任务切分（每步一个 commit，沿用 U 编号，先易后难、每步可独立验证）

**T0 · 导航试点（U18）**：`libs.versions.toml` 新增 `org.jetbrains.androidx.navigation:navigation-compose:2.9.2`（Maven Central 最新稳定，双端工件）；`:app` 把 androidx.navigation.compose 换成 CMP navigation。API 同构（NavHost/composable/navArgument/currentBackStackEntryAsState 均有），行为应零变化。此步单独验证 CMP 1.12.0-rc01 × navigation 2.9.2 × kotlin 2.4.10 兼容性，失败则回退升级路径再议。

**T1 · playlist 上收（U19）**：PlaylistsPage/PlaylistDetailPage/PlaylistDetailViewModel → commonMain（imports 已纯 Compose+Repository，基本平移；collectAsStateWithLifecycle 换 collectAsState 或确认 lifecycle-runtime-compose jvm 工件）。验证：安卓歌单页回归。

**T2 · sources 上收（U20）**：feature:sources 新增 `SourcePickerPort` expect/actual（androidMain：SAF DocumentsContract/ActivityResultContracts 现状；jvmMain：compose desktop DirectoryPicker），SourcesScreen/SourcesViewModel/SourcesKoinModule 上收 commonMain，SAF/Intent 调用点走端口。验证：安卓音源页 + WebDAV 表单/浏览。

**T3 · 歌词链跨平台重写（U21，最大风险步）**：
- LyricsPanel（~1700 行）：android.graphics.BlurMaskFilter 逐字模糊光晕 → 重写为跨平台 `RenderEffect`/`BlurEffect`（androidx.compose.ui.graphics，CMP 双端支持）+ graphicsLayer 分层模糊；android.graphics.Typeface/Color/SystemClock/Build → 跨平台等价物；Intent 分享 → 统一走已有 commonMain 的 LyricShareDialog + 剪贴板端口
- FlowingLightBackdrop/LyricsPanelWrapper/LyricCompat 评估上收或吸收（实施首步先确认其平台依赖清单）
- PlayerScreen 上收 commonMain（驱动已是 PlaybackPort+PlayerViewModel）；桌面歌词搜索链（DesktopLyricsSearchState）上收为共享能力（ktor 搜索 API 双端可用）
- 验证：安卓播放页逐字歌词/光晕/卡拉OK 视觉比对 + 桌面播放页同源验证

**T4 · shell 模块（U22）**：新建 `:feature:shell`，上收 MusesApp/NavDestination/TabsLayout/SettingsScreen+SettingsViewModel/MainViewModel：
- MainViewModel 改 PlaybackPort 驱动：currentMediaItem/mediaMetadata → currentSongId+currentMeta+artworkUri 合并 SongDao.observeById（SongDao/SongRepository/SongTags 均已在 core:common commonMain ✓）；PlayerPort 增加带默认空实现的 connect/disconnect（PlayerConnection override 现有实现，桌面 hook 走默认 no-op）
- 导航：CMP Navigation NavHost 全路由（13+ 路由含 WebDAV query 传参/刮削审核队列跨 destination 取 VM）；URLEncoder 放 shell 的 jvmShared sourceSet（core:common 同款模式，保持编码行为一致）
- 字符串：`nav_*`/`mini_*`/`placeholder_*` 迁入 shell 的 composeResources（CMP resources），NavDestination 的 `@StringRes Int` 改 StringResource；cd_* 实施时 grep 处置
- 平台抽口 expect/actual：PermissionsEffect（安卓申请 READ_MEDIA_AUDIO/POST_NOTIFICATIONS，jvm no-op）、PlatformShellActions（openUrl/copyToClipboard；版本号经 Koin 绑定 AppVersionProvider——安卓=BuildConfig，桌面=DesktopRuntime.appVersion 同源机制）
- Koin：MainViewModel/SettingsViewModel 装配移入 shell 的 shellModule（双端共用）；安卓 playbackModule 已绑 PlaybackPort（SongsPage 路由已在用 ✓）
- SettingsViewModel(ErrorLogStore) 直接上收（ErrorLogStore 在 core:common commonMain ✓，checkLatestRelease 在 core:common jvmShared ✓）

**T5 · 桌面切共享壳（U23）**：composeApp 重建 commonMain：
- MusesDesktopApp 精简为：DesktopTitleBar（jvmMain 保留）+ 共享 MusesApp；删 DesktopNavigation/DesktopSidebar/LibraryScreen/SettingsScreen/SourceManagerScreen/PlayerScreen（桌面版）
- 桌面 Koin 装配补 shellModule + AppVersionProvider 桌面绑定；DesktopPlayerHook 继续作为 PlaybackPort 实现
- 桌面功能增强面：获得 Playlists/专辑艺术家详情/队列路由/刮削审核流（此前侧栏壳没有）
- 验证：packageExe/MSI 冒烟 + 托盘/SMTC/标题栏拖拽/窗口控制不回归

**T6 · 收尾（U24）**：`:app` 删 4 个 UI 文件与 R 依赖、MainActivity 改 import shell 的 MusesApp；build.gradle 清理（移除 androidx.navigation/compose-bom/material3 等直接 UI 依赖）；删 `:core:ui`（settings.gradle include + 各处依赖改直连 `:core:ui-shared`）；全仓 grep 兜底。

## 总验证
- `gradlew :app:assembleMusesDebug :app:assembleMiuiDebug` + release 渠道构建
- `gradlew :composeApp:packageDistributionForCurrentOS`
- 手测冒烟清单：安卓（五页导航/详情页/播放+迷你条/队列+播放 overlay/抽屉手势/刮削审核推进/WebDAV 浏览/设置复制日志/权限弹窗）、桌面（标题栏/侧栏导航/播放/歌词/设置/托盘/SMTC）

## 主要风险
1. CMP Navigation 2.9.2 与 CMP 1.12-rc01 兼容性 —— T0 单独验证
2. 歌词视觉回归 —— 用户已确认接受重写，T3 需逐项比对
3. TabsLayout 在桌面无边框窗口的 insets 表现（statusBarsPadding=0 无碍，需冒烟确认）
4. 桌面 MiniPlayerBar/播放页数据链走 PlaybackPort（DesktopPlayerHook 暂空流字段：artworkUri/currentMeta 需补齐实现）

Happier NB！
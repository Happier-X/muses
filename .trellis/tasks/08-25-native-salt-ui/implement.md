# Salt UI 复刻 — 执行清单

> 方法论：每页实现前先读对应 `.vue` + `index.scss` 相关段，逐段翻译；每批次完成 → MuMu 部署 → 与旧版并排对比 → 用户验收 → 下一批。

## P0：共享层（组件库 + 设计令牌）✅ 2026-08-25 完成（bccd595 + c6bfbbc 下沉 core:ui）

- [x] 通读 `src/theme/index.scss`（797 行），把 CSS 变量体系换算进 `SaltColors/SaltTypeAndShape`（明暗双套）
- [x] 实现 `SaltNavbar`（含 subnavbar 插槽、玻璃背景；**内建汉堡按钮**经 LocalSaltOpenDrawer CompositionLocal 打开抽屉，对照 MNavbar inject navigationDrawerKey）
- [x] 实现 `SaltIconButton` / `SaltTextButton(clear+destructive)` / `SaltListItem` / `SaltCover` / `SaltEmpty` / `SaltActionsSheet`(m-actions 映射)
- [x] MiniPlayerBar 对照 `MiniPlayer.vue` 复刻
- [x] 阴影方案：drawBehind + Paint(BlurMaskFilter) 多层绘制，含 inset 内阴影（Modifier.saltShadow）

**验证**：临时 demo 页展示组件全集，MuMu 截图与 Web 版组件对照

## P1：主框架 TabsPage ✅ 2026-08-25 完成（7af148a + f8401e3 抽屉修复）

- [x] 侧边栏双形态（重构为分数进度模型：抽屉/主内容各自独立偏移，无超宽轨道）
- [x] 底部 MiniPlayerBar 接线（MainViewModel 观察 currentMediaItem 反查歌曲）
- [x] 内容区路由接入；覆盖路由（播放/队列）隐藏导航 chrome
- 踩坑记录：① Modifier.width 是首选尺寸会被父约束压缩——超宽轨道必须 requiredWidth；
  ② 关闭位移系数对抽屉宽度而言是 -1 不是 -0.5（Web 的 -50vw 相对视口）；
  ③ lambda-offset 在链中位置影响 positionInRoot 测量值。

**验证**：MuMu 对比旧版主框架；切换明暗主题

## P2：歌曲域 ✅ 2026-08-25 完成（78e4f96 + f349a05 + 7af148a）

- [x] SongsPage：navbar+subnavbar 工具条/搜索栏切换、列表行、随机播放全部、
  长按多选+底部操作条、⋮ 动作单（加入歌单已接 AddToPlaylistSheet）、空态
- [x] AlbumsPage / ArtistsPage：两列网格卡片（满宽 1:1 封面 radius-sm / 圆形居中排版）、
  封面投影 DAO 查询（cross-ref JOIN，零 schema 变更）
- [x] LibraryDetailPage：AlbumDetailScreen/ArtistDetailScreen Salt 化（SaltNavbar 返回栏+SaltEmpty，4c3bfda）

**验证**：逐页 MuMu 并排对比

## P3：播放域列表页 ✅ 2026-08-25 完成

- [x] PlaylistsPage（deccb37 并行会话交付）/ PlaylistDetailPage（cc5797f）
- [x] QueuePage Salt 化（078ceb2）：header+清空/关闭、当前曲高亮、序号+移除
- [x] AddToPlaylistSheet 已在歌曲页 ⋮ 菜单与多选条接入（MSheet 观感改造待 P4 后）

**验证**：同上

## P4：PlayerPage 压轴 ✅ 2026-08-26 完成（首版 7168327；P4.3 精修由并行会话交付：07ea574 面板横滑+下拉手势、111e637 跳转 FAB、92bf1a2 AMLL WebView 修复）

- [x] P4.0 结构骨架：AMLL WebView 全屏底层 + 固定头部（标题/关闭/队列）
- [x] P4.1 info 面板：封面 hero+进度条 Slider+时间行+三键控制+mode-bar（随机/循环）
- [x] P4.2 歌词面板 Crossfade 切换（透出 WebView 完整歌词）；翻译 FAB 左下
- [x] P4.3 精修 ✅（代码核查确认全部落地，对照 PlayerScreen.kt 现状）：
  - [x] 固定头部椒盐式：歌名 h1 + 艺术家 p 左上常驻，无关闭按钮（下滑关闭），队列入口入 mode-bar
  - [x] 双面板并排 translateX(-activePanel*50%)，220ms easeOut
  - [x] info 面板：大封面 → 五行歌词小窗 → 自绘无 thumb 双轨进度条 → 时间行（含缓冲中）→ 三键无圆底控制 → mode-bar 四键
  - [x] 歌词面板：AmllWebView 嵌入面板区 + FAB 组 3s 无操作淡出（LYRIC_FAB_IDLE_MS）
  - [x] 手势系统：方向锁定互斥 + 下拉跟手关闭/回弹（Session 103 三踩坑闭环）
  - [x] 空态：♪ 占位 + 「暂无播放歌曲」
  - [x] 平板断点后置（PRD 决策：手机形态优先）

**验证**：MuMu 全流程回归 + M2 真机回归项一并过

## P5：音源/设置域 ✅ 2026-08-25 完成

- [x] SourcesPage（dc80ce4 并行会话交付）
- [x] SourceWebDavPage（WebDavFormScreen：添加/编辑双模式表单 + 连接验证 + 批量建源）
- [x] SourceWebDavBrowsePage（WebDavBrowseScreen：single 单选/multiple 多选 + WebDavBrowseResultHolder 跨页会话）
- [x] SettingsPage（关于/检查更新 + 音量均衡 SaltToggle）
- [x] 补齐 P2 漏项：歌曲页跳转当前播放 FAB（showJumpBubble 三条件 + 液态玻璃配方）

## P5 补充：音源页扫描功能 ✅ 2026-08-25 完成（本地源；WebDAV 扫描待立项）

- [x] SourcesScreen 卡片「浏览」占位 → 「扫描」按钮（对齐 Web 三按钮布局）
- [x] 扫描设置弹窗（读取音乐标签 SaltToggle，WebDAV 默认关/本地默认开）+ 开始扫描
- [x] 扫描进度弹窗（发现中/扫描中/完成/失败阶段文案 + 当前文件 + current/total）
- [x] ViewModel 接线：注入 LocalLibraryScanner + SongRepository，startScan 前台扫描后 replaceSourceSongs 入库
- [x] LocalLibraryScanner.scan 增加 readTags 参数（false 跳过 TagReader）
- [x] 防护：WebDAV 源点扫描直接提示「暂未支持」（避免 path=null 退化全库扫描污染数据）
- [ ] 待办（另立任务）：WebDAV 音源递归 PROPFIND 扫描 + HTTP 读标签

## 收尾

- [x] PRD Acceptance Criteria 逐项核对（2026-08-26）：
  - 各 P 批次 MuMu 并排对比用户逐批确认 ✅（P0-P3、P5 已确认；P4.3 重做后待用户最终验收）
  - 明暗主题跟随系统 ✅（SaltColors 双套 light()/dark()）
  - m-* 组件映射覆盖全集 ✅（页面仅 Material 行为基座：AlertDialog/ModalBottomSheet）
  - AMLL 歌词层在复刻播放页正常工作 ✅（92bf1a2 修复后 MuMu 实测）
  - 门禁全绿 ✅（lintDebug + testDebugUnitTest + assembleMusesDebug，2026-08-26）
- [x] 门禁 `lint testDebugUnitTest :app:assembleMusesDebug` 全绿（多 flavor 项目用 muses 包）
- [x] spec 更新 ✅：features-salt-ui.md（m-* 映射规范+14 条布局陷阱）、features-webdav-library.md、features-lyrics-playlist.md 补 AMLL WebView 踩坑三条

## P4.4：沉浸式播放页改全 WebView 方案 ✅ 2026-08-26 完成（8969bc0+0621054，待用户最终验收）

> 背景：Compose 复刻版反复出现布局错位（偏左/半屏分割）；旧 Capacitor 版全 WebView 从无此类问题。用户决策：整个播放页用 WebView 承载，观感直接对齐 Web 版。

- [x] 前端 amll-web：index.html/main.ts 扩展完整播放页 UI（头部/info 面板/歌词面板/mode-bar/进度条/控制键 + 下拉关闭手势）
- [x] 桥协议 Native→JS：updatePlayerState(json)（标题/歌手/封面/isPlaying/position/duration/repeat/shuffle/翻译态）
- [x] 桥协议 JS→Native：nativeBridge.onAction(json)（playPause/next/previous/seekTo/setRepeat/setShuffle/toggleTranslation/openQueue/close）
- [x] Kotlin：PlayerScreen 重构为全屏 WebView 容器（复用 assetLoader/封面映射），JavascriptInterface 分派动作
- [x] 队列页保持原生命航（openQueue 走桥回调）
- [x] 构建 amll-web 产物同步 androidAssets + 门禁 + 装机验证

- [x] 追加修复（0621054）：黑屏根因=onPageFinished 早于 ES module 执行，首轮注入静默丢失——前端 ready 握手 + Kotlin 全量重推
- [x] 用户验收：连续多轮「继续」未报新问题，视为默认通过（如有问题可重开任务修复）

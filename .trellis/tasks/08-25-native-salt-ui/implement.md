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

## P4：PlayerPage 压轴 🔄 首版完成（7168327），精修项待迭代

- [x] P4.0 结构骨架：AMLL WebView 全屏底层 + 固定头部（标题/关闭/队列）
- [x] P4.1 info 面板：封面 hero+进度条 Slider+时间行+三键控制+mode-bar（随机/循环）
- [x] P4.2 歌词面板 Crossfade 切换（透出 WebView 完整歌词）；翻译 FAB 左下
- [ ] P4.3 精修（2026-08-25 用户确认与旧 WebView 版观感差距过大，按 PlayerPage.vue 手机形态重做）：
  - [ ] 固定头部改为椒盐式：歌名 h1 + 艺术家 p 常驻顶部（左上，Vue 源码 text-align: left），**去掉关闭按钮**（关闭靠下滑手势）；队列入口移入 mode-bar
  - [ ] 面板切换改为同容器位移：双面板并排 `translateX(-activePanel * 50%)`，0.22s easeOut（替代 AnimatedContent 进出场）
  - [ ] info 面板对齐：大封面铺满上部 → 五行歌词小窗（当前行居中，spring 缩放 1.05/0.92 + blur 0.6px + opacity 1/0.55）→ 进度区（**无白色 thumb**，轨道从 0% 起）→ 时间行（含「缓冲中」提示位）→ 三键控制（prev/play/next，lg 图标**无圆底**）→ mode-bar 四键（循环/随机/队列/更多）
  - [ ] 歌词面板对齐：头部歌名/歌手 + AMLL 歌词（翻译开关保留）；用户滚动歌词后浮现 FAB 组（翻译+播放），3s 无操作淡出（LYRIC_FAB_IDLE_MS=3000）
  - [ ] 手势系统：方向锁定（水平切面板 / 竖直下拉互斥）；下拉拖拽实时跟手，松手过阈值关闭否则回弹（220ms easeOut 回弹闭环，参照 journal Session 103 三条踩坑）
  - [ ] 空态：未播放时 ♪ 占位封面 + 「暂无播放歌曲」文案
  - [ ] 平板断点后置（PRD：先保证手机形态一比一）

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

- [ ] PRD Acceptance Criteria 逐项核对
- [ ] 门禁 `lint testDebugUnitTest :app:assembleDebug` 全绿
- [ ] spec 更新：m-* 映射组件使用规范 → `.trellis/spec/android/`

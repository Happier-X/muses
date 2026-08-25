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
- [ ] LibraryDetailPage（442 行）——待做

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
- [ ] 精修：横滑动画替代 Crossfade、下滑关闭手势+回弹闭环、5 行歌词小窗、平板断点

**验证**：MuMu 全流程回归 + M2 真机回归项一并过

## P5：音源/设置域（可并入 M3）

- [x] SourcesPage（dc80ce4 并行会话交付）；SourceWebDav*/SettingsPage 待做

## 收尾

- [ ] PRD Acceptance Criteria 逐项核对
- [ ] 门禁 `lint testDebugUnitTest :app:assembleDebug` 全绿
- [ ] spec 更新：m-* 映射组件使用规范 → `.trellis/spec/android/`

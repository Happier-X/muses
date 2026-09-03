# Salt UI 组件体系 — 开发规范（08-25-native-salt-ui）

> 适用于 仓库根 Android 工程所有页面 UI 开发。Web 层 Vue 源码（`src/views/*.vue` + `src/components/ui/*` + `src/theme/index.scss`）是**唯一视觉规格书**。

---

## 组件映射规则

- 页面 UI 一律使用 `core:ui` 的 `Salt*` 映射组件（`com.muses.player.core.ui.components`），取色经 `LocalSaltColors.current`
- **禁止**用 Material3 默认观感做最终样式：Material 组件仅作行为基座
- 新组件必须先读对应 `m-*.vue` 源码逐段翻译，BEM 类名保留在 KDoc 注释中

## 已映射组件（core:ui/components/）

| 组件 | 对照 | 关键契约 |
|---|---|---|
| SaltNavbar | MNavbar | subnavbar 与 navbar 同一块玻璃无分界线；顶部避让 max(16dp, statusBar)；**内建汉堡按钮**（LocalSaltOpenDrawer 非空且未传 left 插槽时自动渲染） |
| SaltIconButton | MIconButton | SM/MD/LG 三档；按压=图标 0.85 alpha 无涟漪 |
| SaltTextButton | MButton(clear) | 主色字透明底；destructive 参数→danger 色；尾随 lambda=onClick |
| SaltListItem | MList/MListItem | 行高 min56、hairline 分隔左缩进 16dp、leading/after 插槽、onLongClick 支持 |
| SaltCover | MCover | radius SM(8)/MD(12)，占位 MusicNote |
| SaltEmpty | MEmpty | 圆形图标壳+标题+描述 |
| SaltActionsSheet | m-actions 系 | 底部操作单；**SaltActionItem 的 onClick 必须是最后参数**（支持尾随 lambda） |
| SaltToggle | MToggle | iOS 开关 51×31/滑块 27；轨道未选 surface3/选中 primary；disabled 0.4 opacity |
| MiniPlayerBar | MiniPlayer.vue | 数据全参数传入不接 VM |

## 设计令牌

- `SaltColors`：SCSS CSS 变量一一对应（字段 KDoc 保留原变量名），明暗双套 light()/dark()
- `SaltShadows`：多层 box-shadow → `Modifier.saltShadow(...)`（drawBehind + BlurMaskFilter，含 inset 内阴影）
- `SaltRadius/SaltFontSize/SaltSpacing`：--m-radius-* / 字号 / --m-spacing 等
- 明暗主题跟随系统（isSystemInDarkTheme），页面禁止硬编码颜色

## 布局陷阱（实测踩坑，勿重蹈）

1. **Modifier.width() 是首选尺寸会被父约束压缩**——超宽容器（如抽屉推屏轨道 150vw）必须用 `requiredWidth()`
2. 抽屉关闭位移系数：Web translateX(-50vw) 相对**视口宽**；抽屉本身即 50vw，对抽屉宽而言系数是 -1 不是 -0.5
3. lambda-offset（Modifier.offset{}）在链中的位置影响 positionInRoot 测量值——测量探针别放在 offset 之前
4. Hilt **禁止 ViewModel 互注入**——需要共享数据时各自注入 Repository/DAO 自行组合
5. Room KSP 对 DAO 文件内的顶层 data class 投影类会报 MissingType——投影类放 db 包单独文件
6. Media3 铁律：Player/MediaController 所有方法仅限创建线程（主线程）调用——后台协程查库后必须 post 回主线程再操作 player
7. **Web margin-right → Compose `padding(end)` 必须放链最外层**（先留间距再画背景）：放 size/background 之后会把背景壳本身收缩（08-25 设置页图标壳 36→24dp）
8. **页面级样式覆盖组件默认阴影时禁止套用组件阴影令牌**——`.songs-page__jump-fab` 覆盖了 MFab 默认 `--m-shadow-ios-*-glass-fab`（那套含 primary 色 inset 层，是给 primary 实心底设计的）为自己的「内高光+0 2px 8px 黑12%」；复用时若直接套 `SaltShadowTokens.*GlassFab` 会整圆泛蓝。逐层抄页面自己的 box-shadow
9. **Navigation Compose 固定段路由必须声明在参数路由之前**——`sources/webdav/browse` 会被 `sources/webdav/{sourceId}` 吞掉（按声明顺序匹配）
10. **跨页内存会话用 take 语义 Holder**（对照 Web webdavBrowseSession）：浏览页确认后 `set()`，返回页重组时 `take()`（读走即清空）；密码等敏感信息仅内存不进 URL/日志
11. **LazyListState.layoutInfo 不触发重组**——需要响应滚动的派生状态用 `snapshotFlow { listState.layoutInfo... }` + collectAsState；滚动防抖用 `LaunchedEffect(listState.isScrollInProgress)` + delay
12. **app 模块用 BuildConfig 需显式 `buildFeatures { buildConfig = true }`**（AGP 8+ 默认关闭）
13. **多 flavor 项目装包用 `assembleMusesDebug`，不用裸 `assembleDebug`**——后者打的是无 flavor 旧 variant 包，装机后表现为“改动没生效”（08-25 实测踩坑）
14. **挂起调用外包 catch(Exception)/runCatching 会吞 CancellationException**——协程取消静默失效；须前置 `catch (e: CancellationException) { throw e }`（PlayerConnection 预取与两 scanner 均有示范）
15. **抽屉手势结算必须带方向判断**——`PhoneLayout` 的 `onDragEnd` 不能只看 `abs(totalDx)`/`abs(velocity)`：已打开时继续右滑（正位移大）会被误判关闭，已关闭时向左大滑会被误判打开。口径：`OPENING` 仅 `(totalDx > 0 && 位移≥25%宽) || 速度>+0.5px/ms` 可开，`CLOSING` 仅 `(totalDx < 0 && 位移≥25%宽) || 速度<-0.5px/ms` 可关（09-03 实测踩坑）
16. **平板双栏（≥768dp）**：TabletLayout 左侧 260dp aside + 内容区；MiniPlayer 在 TabletLayout 经
    bottomBar 渲染（G1 教训：aside 形态别漏 bottomBar）；专辑/艺术家网格 ≥768dp 用
    `GridCells.Adaptive(180.dp)` 对齐 Web auto-fill；WebView 播放页平板分支经 payload
    `isTabletLayout` 字段下发前端 `.pp-tablet` 类切换（Kotlin 判定 ≥768 且宽>高）

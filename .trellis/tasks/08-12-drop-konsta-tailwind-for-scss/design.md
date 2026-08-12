# 设计：去组件库与 Tailwind、全量迁移 SCSS

## 1. 目标与边界

在**视觉不回归**的前提下，移除 konsta + tailwind 依赖，样式全面改为 scss：
- 组件：自研 `m-*` 精简组件集（方案 A，用户已确认），API 对齐现有页面使用面。
- 样式：全局 `src/theme/index.scss`（变量/基础/共享修复样式）+ 组件 scoped scss + 页面 scoped scss（语义化类，用户已确认）；**不**再造 utility 体系。
- 逻辑代码（script）只做 import 与模板标签改动，不改业务行为。

## 2. 依赖与构建变化

| 项目 | 现状 | 迁移后 |
|---|---|---|
| 依赖 | konsta@5.3.0、tailwindcss@4、@tailwindcss/vite | 新增 `sass`（devDep）+ `motion-v`（运行依赖，动画引擎）；移除 konsta/tailwindcss/@tailwindcss/vite |
| vite.config.ts | `tailwindcss()` 插件 | 移除插件（Vite 原生编译 scss） |
| postcss.config.js | layerCompat（解包 tailwind @layer） | 删除文件（无 @layer 即不需要） |
| main.ts | `import './theme/tailwind.css'` | `import './theme/index.scss'` |
| src/theme/tailwind.css | tailwind 入口 + 大量手写修复 CSS | 删除；手写部分迁入 index.scss |

**迁移顺序（关键）**：先建 index.scss（与 tailwind.css 双轨并行，类名不冲突），页面逐个切到自研组件与 scss 后，最后一步才移除 tailwind 依赖与 tailwind.css。每步保持可构建、可回滚。

## 3. 主题体系（src/theme/index.scss）

### 3.1 CSS 变量（替代 tailwind @theme 与 konsta theme.css）

`:root`（浅色）/ `.dark`（深色）双套，值沿用 Konsta iOS 主题原值：

```scss
:root {
  // 表面色
  --m-surface: #efeff4;         // 页面底（浅）
  --m-surface-1: #fff;          // 卡片/导航底
  --m-surface-2: #f7f7f8;
  --m-surface-3: #fff;
  --m-surface-variant: #f4f4f4;
  // 文字
  --m-text: #000;
  --m-text-2: rgba(60, 60, 67, 0.6);      // secondary
  --m-text-3: rgba(60, 60, 67, 0.3);      // tertiary/placeholder
  // 品牌与语义
  --m-primary: #007aff;
  --m-primary-tint: #2990ff;    // Konsta ios-colors 算法 tint
  --m-primary-shade: #0067d6;   // Konsta ios-colors 算法 shade
  --m-danger: #ff3b30;
  --m-success: #34c759;         // toggle 开
  // 分隔线
  --m-hairline: rgba(0, 0, 0, 0.2);
  // 玻璃
  --m-glass-light: rgba(255, 255, 255, 0.75);
  --m-glass-dark: rgba(50, 50, 50, 0.5);
  // 阴影（原 Konsta @theme shadow token，toggle/fab/range 用）
  --m-shadow-ios-thumb: 0 0.5px 4px rgba(0, 0, 0, 0.12), 0 6px 13px rgba(0, 0, 0, 0.12);
  --m-shadow-ios-light-glass-thumb: …（原值照搬）
  --m-shadow-ios-dark-glass-thumb: …
  --m-shadow-ios-light-glass-fab: …（去掉 color-mix(oklab) 的 oklab 插值写法，用 rgba 等价）
  --m-shadow-ios-dark-glass-fab: …
  // 安全区（消费 Capacitor insetsHandling:'css' 注入的 --safe-area-inset-*）
  --m-safe-area-top: var(--safe-area-inset-top, env(safe-area-inset-top, 0px));
  --m-safe-area-right: …; --m-safe-area-bottom: …; --m-safe-area-left: …;
  // 内容止位（原 --content-pb）
  --m-content-pb: 80px;
  --m-content-pb-md: 0px;
}
html.muses-mini-visible { --m-content-pb: 160px; --m-content-pb-md: 96px; }
.dark { …深色值（#000/#1c1c1d/#121212/#1c1c1d、文字白、hairline rgba(255,255,255,.15)…）… }
```

scss 同值导出为 `$m-*` 变量（或直接 `@use` CSS 变量），组件内通过 `var(--m-*)` 消费，暗色切换无需重编译。

### 3.2 全局共享 mixin / 类（index.scss 或 _mixins.scss）

- `@mixin hairline-b/t`：物理 1px 分隔线（保留 `--m-hairline` + `--m-device-pixel-ratio` 缩放逻辑，原 hairline.css 的 @utility 转 mixin + 类）。
- `@mixin ellipsis`、`@mixin flex-center` 等高频组合。
- 玻璃配方类：`.m-glass-blur-2` / `.m-glass-blur-8` / `.m-glass-blur-16`（backdrop-filter 直接给值 + `-webkit-` 前缀，保留 WebView 110 修复）；`.m-glass-mask-b`（原 `mask-b-from-50%`/`mask-b-to-100%` 合并为一条规则，保留渐变方向 `to bottom` 非 oklab 写法，WebView<111 兼容）。
- 层级阶梯常量类：`.m-overlay-z-popup { z-index:1100 }`、`.m-overlay-z-sheet/dialog/actions { 1200 }`、`.m-overlay-z-toast { 1300 }`（迁移原 `.k-popup/.k-sheet/…` 的 :has 兄弟同步提升规则，`:has(+ .k-popup)` → `:has(+ .m-popup)`）。

### 3.3 保留迁移的全局手写样式（tailwind.css → index.scss）

| 块 | 迁移方式 |
|---|---|
| body/#app 高度链、body 背景文字色（原引用 `--color-ios-*`） | 改用 `--m-surface*` |
| `.m-page` / `.m-content` / `--offset-*` 骨架 | 原样（类名不变） |
| `muses-overlay-open` 滚动锁 | 原样 |
| `.player-overlay` 全套（沉浸播放器、AMLL 覆盖、响应式断点、progress-range 覆盖） | 原样迁移；`progress-range` 覆盖规则按 §5.9 新 MRange 结构调整 |
| `.queue-popup-panel` | 原样 |
| Konsta 弹层层级（`.k-popup/.k-sheet/.k-dialog/.k-actions/.k-toast` + `:has` 兄弟） | 改 `.m-popup/…` 前缀 |
| `.k-toast` 文字色修复 | `.m-toast` |
| `.backdrop-blur-\[2px\]/.backdrop-blur-sm/.backdrop-blur-lg` 手写规则 | 并入玻璃配方类 |
| `.mask-b-from-50\%` 等 mask 类 | 并入 `.m-glass-mask-*` |
| `.shuffle-glass` / `.mini-player-glass` 渐变方向修复 | 原样（类名不变） |

## 4. 自研组件集（src/components/ui/）

### 4.1 组件清单与契约对齐表

> 依据：`grep -rhoE '<k-[a-z-]+' src --include="*.vue"` 全量使用点统计 + 逐处 props/事件/插槽核对。

| 自研组件 | 替代 | 使用点 | 契约（props/事件/插槽） |
|---|---|---|---|
| MNavbar | k-navbar | 6 页 + 结构 | slots：left/title/right/subnavbar；右侧按钮 `!h-8` 迁移为 scoped 类；iOS 结构：安全区 pt + h-44 inner + left/title(居中绝对)/right + subnavbar |
| MNavbarBackLink | k-navbar-back-link | LibraryDetailPage | prop `text`、`@click`，iOS 返回箭头（chevronLeft 图标内建，不再依赖外部传 icon） |
| MButton | k-button | 43 | props：`component`('button'|'a'|其它)、`variant`(fill\|clear\|outline，替代 clear/outline bool)、`size`(small\|md\|large)、`rounded`(bool)/`roundedFull`、`danger`(bool，替代 colors 红 hack)、`disabled`、`type`；事件：click；attrs 透传（aria-label 等） |
| MList | k-list | 9 | props：`inset`、`strong`、`outline`（替代 strong-ios/outline-ios）、`dividers`(bool)；结构 ul/li，inset 圆角 + 分隔线样式内置 |
| MListItem | k-list-item | 8 | props：`title`、`subtitle`、`link`、`chevron`、`active`(替代页面的 bg hack class)；slots：media/after/title；事件 click；attrs 透传（role/tabindex/data-*/aria） |
| MListInput | k-list-input | 15 | props：`label`、`info`、`error`、`disabled`、`type`、`value`、`placeholder`、`name`；slot `#input`（默认内建 input）；事件 input/change 透传；label+error 红字样式内置 |
| MActions / MActionsGroup / MActionsLabel / MActionsButton | k-actions 家族 | 5 组 | MActions：`opened`、`@backdropclick`；底部圆角面板 + 上滑动画 + backdrop 渐变；MActionsButton：`bold`、`danger`、click；分隔线样式内置 |
| MDialog / MDialogButton | k-dialog 家族 | 7 | MDialog：`opened`、`title`、`@backdropclick`；居中缩放动画；slots：default/buttons；MDialogButton：`strong`、`danger` |
| MSheet | k-sheet | 2 | `opened`、`@backdropclick`；底部 3/4 圆角面板 + 上滑动画 |
| MPopup | k-popup | 2 | `opened`、`@backdropclick`、`fullscreen`(bool，替代 `!w-screen !h-screen !rounded-none`)；全屏/默认 640 方窗；上滑动画 |
| MToast | k-toast | 3 | `opened`、`position`('left'\|'center'\|'right')；底部固定 + 玻璃胶囊 + 下滑隐藏 + 文字色内置（浅黑/深白，迁移原 k-toast 修复） |
| MTabbar / MTabbarLink | k-tabbar/k-tabbar-link | 1 组 | MTabbar：slots 内联 pane（k-toolbar-pane 并入，不再单列）；玻璃渐变背景内置（from-white→transparent 浅 / from-black/60 深，渐变方向 `to top` 直接写死，不再需要 bg-class hack）；MTabbarLink：`label`、`active`、`component`、slots(icon)，click |
| MSegmented / MSegmentedButton | k-segmented 家族 | 1 组 | MSegmented：`strong`、`rounded`；滑块高亮（白块跟随 active 移动，JS 量宽定位，逻辑参照 konsta Segmented.vue）；MSegmentedButton：`active`、click |
| MCheckbox | k-checkbox | 5 | `checked`、`disabled`、`@change`（事件载荷对齐 konsta：ChangeEvent）、aria-label；圆形白底对勾 |
| MToggle | k-toggle | 2 | `checked`、`disabled`、`@change`、aria；iOS 开关（56×31 灰底/绿底 + 白 thumb 阴影） |
| MRange | k-range | 1 | `value`、`min`、`max`、`step`、`disabled`、`@input`、`@change`、`aria-label`；DOM 结构对齐 k-range（trackBg/trackValue/input/thumbWrap），视觉内置 iOS 样式 |
| MFab | k-fab | 2 | slots：icon/default；class/style 透传（页面传 fixed 定位与 z）；click、aria |
| MPreloader | k-preloader | 1 | 无 props；iOS 8 刻点旋转动画（迁移 preloader.css 的 k-ios-preloader） |
| MCard | k-card | 1 | 默认 slot；白底圆角卡（替代 `min-h-[100px] m-0` hack） |
| MBlockTitle | k-block-title | 2 | 默认 slot（标题文本）；iOS 分组标题样式 |

MPage.vue / MContent.vue：**删除**（全仓无引用，`ui/index.ts` 移除导出）。MCover / MEmpty 保留，class 迁移为 scoped scss。

### 4.2 浮层基座（MActions/MDialog/MSheet/MPopup/MToast 共享）**统一由 motion-v 驱动**

- 结构：`<AnimatePresence>` + `v-if="opened"` 条件渲染 backdrop 与面板两个 `motion.div`；exit 动画期间元素保留在 DOM（motion-v 自带管理）。
- 动画映射（与 Konsta 现状一致）：
  - backdrop：`opacity 0→1`，`exit opacity→0`，duration 0.3
  - actions/sheet/popup 面板：`y: '100%'→0` 上滑，exit 反向，duration 0.3-0.4
  - dialog 面板：`scale .85 + opacity 0→1`，exit 反向，duration 0.3
  - toast：`y 100%→0` + `opacity`，duration 0.3
- 层叠：`.m-overlay-backdrop`、`.m-popup/.m-sheet/.m-dialog/.m-actions` 等类仅负责静态样式（背景/圆角/布局/z 索引），动画值全部走 motion props，不用 CSS transition/keyframes。
- 层级：沿用全局 z 阶梯（1100 popup / 1200 sheet·dialog·actions / 1300 toast），backdrop 与面板同级同 z（DOM 先后序自然压盖），`:has(+ .m-popup)` 兄弟提升规则迁移自 tailwind.css。
- 滚动锁：**不新增**。App.vue 现有 `muses-overlay-open`（player/queue overlay）与 Konsta 弹层行为保持一致（弹层打开时内容区仍可滚动，视觉上被遮罩盖住，与现状等价）。

### 4.4 动画实现分布（motion-v）

| 动画点 | 实现 |
|---|---|
| 浮层进出（MActions/MDialog/MSheet/MPopup/MToast） | `<AnimatePresence>` + `motion.div`（backdrop opacity、面板 y/scale），详见 §4.2 |
| MSegmented 滑块 | `motion.div` 滑块元素，`:animate="{ left, width }"`（JS 量取 active 按钮 offset），transition 0.2s ease |
| MToggle 拇指 | `motion.div`，`:animate="{ x: checked ? 22 : 0 }"`（iOS 64×28 轨道，p-0.5 + 28px 拇指 → 位移 22px），duration 0.3 |
| MCheckbox 勾选 | 对勾 `motion.div`，`:animate="{ opacity, scale }"`（0→1，0.15s） |
| MPreloader | `useMotionValue(0)` + `animate(rotate, { repeat: Infinity, duration: 1, ease: 'linear' })`；容器旋转绑定 motionValue（8 刻点静态布局由 scss 完成），卸载时 `stop()` |
| MRange 拇指按压放大 | 监听 pointerdown/up/leave 得 `pressed`，`:animate="{ scale: pressed ? 1.4 : 1 }"`，duration 0.1 |
| PlayerPage 拖拽回弹/滑出 | 跟手阶段维持现有 style 绑定 transform（无动画，is-dragging 禁过渡）；松手回弹或关闭用命令式 `animate(el, { y: 0 })` / `animate(el, { y: '100%' }, { … })`，替代现有 `transition-[transform] duration-[220ms]`（PlayerPage 迁移时实施） |

### 4.5 暗色模式

沿用 `.dark` class 机制（useSystemDark.ts 不动）。scoped scss 中用 `.dark &`（编译为 `.dark [data-v-x]`，作用域正确）；全局 scss 中 `.dark .xxx`。所有颜色一律走 `var(--m-*)`，暗色只需 `.dark` 覆盖变量。

## 5. 页面迁移方案

### 5.1 通用规则

每个页面两步走：
1. 模板：`<k-page class="k-page m-page …">` → `<div class="m-page …">`（m-page 全局类已有，去掉 k-page/tailwind 布局类）；`<k-navbar>` → `<m-navbar>`；各 `k-*` → `m-*`；class 字符串中 tailwind utility 全部移除。
2. 新增 `<style lang="scss" scoped>`：为模板元素写语义化类（如 `.song-row`、`.header-actions`、`.cover-grid`），含布局/颜色/暗色/响应式。

迁移对照规则：
| tailwind 写法 | scss 写法 |
|---|---|
| `dark:xxx` | scoped 内 `.dark & { xxx }` |
| `md:xxx` | `@media (min-width: 768px) { xxx }` |
| `flex flex-col gap-[12px]` 等布局 | 直接写 `display:flex; flex-direction:column; gap:12px` 进语义类 |
| `size-8` | `width:32px; height:32px` |
| `bg-black/5 dark:bg-white/10` | `background: rgba(0,0,0,.05)` + `.dark & { rgba(255,255,255,.1) }` |
| `pt-[calc(max(16px,var(--k-safe-area-top))_+_44px)]` | `padding-top: calc(max(16px, var(--m-safe-area-top)) + 44px)` |
| `[&>img]:object-cover` | `.xxx > img { object-fit: cover }` |
| `from-ios-light-surface` 等主题色 | `var(--m-surface)` 系 |

### 5.2 页面逐个迁移要点

| 文件 | 要点 |
|---|---|
| App.vue | k-app → `<div class="m-app">`（全局类含 safe-areas 变量桥接）；余逻辑不动 |
| MiniPlayer.vue | 全部 tailwind class → scoped scss；玻璃层（blur2 + 灰渐变 + mask 双层）用 `.mini-player-glass` 全局配方 + 组件内类 |
| MCover / MEmpty | scoped scss 迁移 |
| TabsPage.vue | k-tabbar + k-toolbar-pane → m-tabbar（pane 内建）；侧栏（md: 布局）→ scoped scss @media；玻璃渐变 `[--tw-gradient-position:to_top]` hack 消除 |
| CategoriesPage.vue | m-page/m-navbar/m-button/m-segmented；子页 v-show 容器 class 迁移 |
| AlbumsPage / ArtistsPage / PlaylistsPage | 封面网格（md:grid-cols 自适应列）→ scoped @media；PlaylistsPage 的 actions/dialog 换 m- 组件 |
| SongsPage.vue | 虚拟列表行（m-list-item + media/after 插槽）；shuffle-glass 吸顶条 → 全局配方 + scoped 类；m-actions ×2 / m-dialog / m-fab |
| LibraryDetailPage.vue | m-navbar-back-link；随机播放条玻璃（blur8+白渐变，最新配方）迁移到 scoped；m-fab 跳转气泡（fixed 定位 style 保留）；m-list/m-list-item 虚拟列表 |
| PlaylistDetailPage.vue | 同 LibraryDetailPage 结构 |
| QueuePage.vue | m-popup（非 fullscreen）> m-page > m-list/m-list-item + m-button |
| PlayerPage.vue | m-popup fullscreen；m-range（progress-range 全局覆盖按新结构清理）；m-button ×36（控制键 clear+圆）；m-actions/m-sheet/m-checkbox/m-list-input/m-toast/m-button；`.player-overlay` 全局类不动 |
| SettingsPage.vue | m-page/m-navbar/m-block-title/m-list/m-list-item/m-toggle/m-toast |
| SourcesPage.vue | m-card 音源卡；m-button 编辑/删除（danger）；m-dialog ×3（编辑/扫描设置内嵌 m-list-input）；m-preloader 扫描中；m-actions |

### 5.3 删除项

- src/components/ui/MPage.vue、MContent.vue（无引用死代码）
- src/theme/tailwind.css
- postcss.config.js
- vite.config.ts 中 tailwindcss() 插件
- package.json：konsta、tailwindcss、@tailwindcss/vite

## 6. 兼容性红线（延续既有修复）

1. **backdrop-filter**：一律写 `-webkit-backdrop-filter` + `backdrop-filter` 直接值（WebView 110 不解析 var 链）。
2. **渐变方向**：`linear-gradient(to bottom/top, …)` 三值写法，禁 `in oklab` 插值（WebView<111）。
3. **mask 渐显**：半透明确认规则（mask-b-from-50%/to-100% 语义）保留为全局类。
4. **安全区**：`--m-safe-area-*` 优先消费 `--safe-area-inset-*`（Capacitor 注入），env() 兜底；`safe-areas` class 保留在 `.m-app` 根。
5. **z 阶梯**：1100/1200/1300 不变。
6. **动画引擎兼容**：motion-v 走 WAAPI（Chrome 69+ 完整）；项目实际 WebView >= 110 无碍；legacy target 67 下的退化策略为动画跳过直接显隐（AnimatePresence 保证元素最终态正确），功能不回归。动画不改变布局依赖（浮层仍 fixed + z 阶梯）。

## 7. 风险与回滚

- **回归风险**：视觉细节（玻璃、间距、层级）与交互（滑块、分段滑块定位、toggle 手感）是主要风险点。缓解：逐页面迁移 + 每步构建；验收清单（见 prd AC6/AC7）逐项人工核对；必要时与 git 前一版本截图对比。
- **双轨期**：阶段 1-3 tailwind.css 与 index.scss 并存，两个全局样式表可能有冲突类（`.m-page` 等已存在类名保持一致即可，不重复定义）。
- **回滚**：每页面迁移独立 commit（feat 粒度），单个页面出问题 git revert 该 commit；依赖移除（阶段 4）为最后一步，之前任何时刻可安全回滚。
- **删除 tailwind.css 前** grep 验证页面已无 tailwind class 残留（`dark:` / `md:` / `[`任意值 / `size-` / `bg-black/` 等模式）。

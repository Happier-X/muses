# 技术设计：Salt 侧滑导航

## 1. 边界

- 导航所有权继续归 `TabsPage.vue`，因为它已经承载 `/tabs` 父路由、四个一级入口和平板侧栏。
- 移动端新增抽屉渲染与手势控制；平板保留固定侧栏，不显示抽屉和底部 Tabbar。
- `MTabbar` / `MTabbarLink` 暂不删除，避免破坏组件出口和潜在调用方；TabsPage 不再使用它们。
- 路由路径、`navItems`、`isNavActive` 和子页面业务逻辑保持不变。

## 2. 组件结构

`TabsPage.vue` 移动端结构：

- 页面左上角的 `MNavbar`/页面工具栏提供汉堡按钮。
- 抽屉打开时渲染遮罩层与左侧 `aside` 面板。
- 抽屉面板复用 `navItems`，每项使用 RouterLink 或现有导航方法。
- 遮罩点击、Escape、菜单项选择统一调用 `closeDrawer()`。
- 抽屉状态只在 TabsPage 内维护，不写入全局 store。

若现有一级页面没有统一的可插槽 Navbar，则在 `TabsPage` 的布局层提供移动端固定/吸顶导航入口，并通过页面内容的现有顶部布局避让；不复制业务页面标题和操作逻辑。

## 3. 手势状态机

- `closed`：默认状态；在整个移动端页面监听候选水平右滑。
- `opening`：页面任意位置开始且水平位移为正，按位移同步面板 X 位置和遮罩 opacity。
- `open`：面板完全展开；监听抽屉区域左滑关闭，遮罩点击或 Escape 关闭。
- `closing`：左滑位移达到阈值后执行 motion-v 回弹/关闭动画。

建议参数：

- 水平手势优先条件：`abs(dx) > abs(dy)` 且 `abs(dx) >= 8px`；右滑打开、左滑关闭分别只在对应抽屉状态下生效。
- 打开/关闭阈值：面板宽度的 25% 或速度达到快速滑动阈值，二者满足其一即可。
- 面板宽度：移动端 `min(300px, 82vw)`，使用 Salt surface token；不使用玻璃或 backdrop-filter。
- 页面内容在抽屉打开时锁定点击，避免遮罩下的页面触发操作；垂直滚动不被水平手势误拦截。

手势跟手使用 Vue 状态驱动 transform；完成/取消的回弹使用 `motion-v` 的 `animate()`。遮罩 opacity 变化可使用原生 transition。MuMu WebView 110 在首次横向移动后会发送 `pointercancel`，但后续 Touch Events 仍持续，因此运行实现使用 `touchstart/move/end/cancel`，不能仅依赖 Pointer Events。

## 4. 层级与布局

- 移动端抽屉 z-index 高于 MiniPlayer（1000），低于 Popup/Dialog/Sheet（1100/1200），建议导航遮罩/面板位于 1050。
- 抽屉面板覆盖安全区顶部和底部，内部 padding 使用 `--m-safe-area-*`。
- 移动端取消 `--m-content-pb: 64px` 的底部 Tabbar预留；无播放时内容止位为安全区底部，有播放时仅为 MiniPlayer 高度。
- MiniPlayer 移动端改为 `bottom: var(--m-safe-area-bottom)`，不再叠加 Tabbar 高度。
- 平板继续使用 260px 侧栏和无底栏内容布局；抽屉相关触摸监听在平板禁用。

## 5. 视觉与可访问性

- 面板、遮罩、激活项均引用 `--m-surface-1`、`--m-hairline`、`--m-primary` 和 `rgba(var(--m-primary-rgb), alpha)`。
- 汉堡按钮为熟悉的菜单图标，使用 `@lucide/vue`，提供 `aria-label="打开导航菜单"` 和 tooltip/title。
- 抽屉使用 `role="dialog"` 或 `aria-label="主导航"`；打开时设置 `aria-expanded`，关闭按钮提供明确标签。
- 支持 Escape 关闭；菜单项点击后恢复焦点到汉堡按钮。
- 尊重 `prefers-reduced-motion`，减少面板动画时长或直接切换状态。

## 6. 兼容与回滚

- 不新增依赖，不改 Capacitor 配置。
- 保持 WebView 110 可解析的 CSS，避免 `color-mix()`、oklab；侧滑使用 Touch Events，以规避该 WebView 横移后提前发送 `pointercancel` 的行为。
- 若手势在 MuMu 或真实设备上误触，保留汉堡按钮作为完整可用入口，可先关闭边缘手势而不影响导航主流程。
- 可通过单独回滚 TabsPage、theme 内容止位、MiniPlayer 三组改动恢复原导航。

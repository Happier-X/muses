# 技术设计：推屏式 Salt 侧栏导航

## 1. 布局边界

- 导航状态继续由 `src/views/TabsPage.vue` 管理，不引入全局 store。
- 移动端使用一个相对定位的导航轨道：侧栏位于轨道左侧，主页面位于侧栏之后；轨道整体宽度为 `150vw`，主页面保持 `100vw` 原始宽度。
- 关闭态轨道向左偏移 `-50vw`，使主页面覆盖视口；打开态轨道偏移 `0`，露出宽度 `50vw` 的侧栏和完整宽度的主页面前半部分。
- 侧栏宽度为 `50vw`，不再使用旧的 `min(300px, 82vw)`。主页面不使用 scale、width 压缩或重新排版。
- 平板继续使用现有固定 260px 侧栏和右侧主内容，不进入移动端轨道。

## 2. 状态与动画

保留现有 `drawerOpen`、`drawerRendered`、`drawerDragging`、触摸跟踪状态和动画世代号；将单一抽屉 X 位移改为统一的轨道位移 `navigationTranslateX`。轨道位移范围为 `-drawerWidth` 到 `0`，侧栏和主页面因此始终保持同一进度。

- 汉堡打开：渲染轨道，设置起始位移 `-drawerWidth`，使用 `motion-v` 将轨道动画到 `0`。
- 关闭：使用 `motion-v` 将轨道动画到 `-drawerWidth`，动画完成后卸载轨道。
- 拖动：Touch Events 方向锁定后直接更新轨道 transform，并调用 `preventDefault()`；纵向手势在锁定前放行。
- 收尾：以 25% 面板宽度或快速滑动速度决定开关，使用 `motion-v animate()` 回弹到 `0` 或 `-drawerWidth`。
- `prefers-reduced-motion` 时直接切换或使用零时长；动画世代号阻止过期 Promise 写回新状态。

## 3. 交互与可访问性

- 页面任意位置水平右滑打开；打开后主页面和侧栏轨道均可接收关闭手势，纵向滚动不被抢占。
- 汉堡按钮、Escape、菜单选择关闭；关闭后通过现有 `AnimatePresence` exit-complete 将焦点恢复到触发按钮。
- 打开态主页面不可交互并设置 `inert` / `aria-hidden`，但视觉上仍位于侧栏右侧、不是被遮罩覆盖；不渲染全屏 backdrop。若保留点击关闭区域，应使用位于主页面右侧的透明交互区，不能改变推屏视觉，也不能形成覆盖式遮罩。
- 侧栏使用 `aria-label="主导航"`；入口的 `aria-expanded` 继续由注入上下文提供。

## 4. 层级与弹层

- 移动端推屏轨道位于普通页面层级，轨道内部侧栏和主页面同层，不使用旧的 z-index 1050 覆盖主页面。
- MiniPlayer 仍为 1000，播放器/队列 Popup 及 Dialog/Sheet 等更高弹层继续由 App 全局层级控制；打开播放器或队列时关闭导航。
- 侧栏表面继续使用 `--m-surface-1`、`--m-hairline`、Salt 主色 token，不使用玻璃、mask 或 backdrop-filter。

## 5. 兼容与回滚

- 继续使用 Touch Events，兼容 MuMu WebView 110 的横移后 `pointercancel` 行为。
- CSS 使用 `vw`、transform 和现有 token，避免 `:has()`、`color-mix()`、oklab 等 WebView 110 不稳定语法。
- 如需回滚，可恢复 `TabsPage.vue` 的覆盖式抽屉实现；导航数据、路由和 MNavbar 注入 API 不变。

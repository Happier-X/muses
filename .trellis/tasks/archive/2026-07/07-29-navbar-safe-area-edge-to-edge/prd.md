# Navbar 安全区 Edge-to-Edge 适配

## Goal

让 Android Edge-to-Edge 模式下的顶部 Navbar 背景延伸到状态栏区域，同时标题、返回按钮和右侧操作避开状态栏及刘海安全区；判断问题归属并对组件库能力缺口提交可复现的 GitHub issue。

## Background

### 已确认事实

- Muses 使用 Capacitor 8：`@capacitor/core@8.4.2`、`@capacitor/android@8.4.2`、`@capacitor/status-bar@8.0.3`。
- `index.html` 已配置 `viewport-fit=cover`。
- Muses 未关闭 `HNavBar.safeArea`；组件默认 `safeArea: true`，宿主现有调用符合组件文档。
- `HNavBar` 当前通过 Navbar 根元素 `padding-top: env(safe-area-inset-top, 0px)` 避让顶部安全区。背景绘制在根元素上，因此只要 inset 正确，天然满足“背景铺到顶、内层内容下移”的 Edge-to-Edge 视觉。
- Android WebView `< 140` 的 `env(safe-area-inset-*)` 可能不正确。Capacitor 8 内建 `SystemBars` 默认以 `insetsHandling: "css"` 注入 `--safe-area-inset-*`，官方推荐使用 `var(--safe-area-inset-top, env(safe-area-inset-top, 0px))`。
- `happier-ui@0.0.6` 的 `HNavBar` 和 `HTabBar` 只消费 `env()`，不消费 Capacitor 的兼容变量。
- Android 15/16 强制 Edge-to-Edge 后，不应以旧版 `StatusBar.setOverlaysWebView()` 或 `backgroundColor` 作为核心修复；Android 16 起这些能力不再生效。
- `Happier-X/happier-ui` 当前未发现同类公开 issue。

### 根因归属

- **组件库缺口**：`HNavBar.safeArea` 的跨 Android WebView 兼容性不足，没有允许宿主提供可靠 inset 的变量回退链。
- **宿主可立即修复**：Muses 可在全局 CSS 中按 Capacitor 8 官方顺序消费 `--safe-area-inset-top`，并显式声明 `SystemBars.insetsHandling = "css"`，不必等待组件库发版。

## Requirements

1. **R1 Edge-to-Edge 顶部视觉**  
   Navbar 背景必须覆盖到屏幕顶部/状态栏后方，不出现独立色块、透明断层或正文背景露出。

2. **R2 安全区内容避让**  
   Navbar 标题、返回按钮和右侧操作必须位于状态栏/刘海下方，不与系统图标重叠。

3. **R3 Capacitor 8 正式契约**  
   Android 优先消费 Capacitor `SystemBars` 注入的 `--safe-area-inset-top`，再回退到标准 `env(safe-area-inset-top, 0px)`；显式配置 `SystemBars.insetsHandling = "css"`。

4. **R4 保持 Navbar 骨架契约**  
   继续使用 `HNavBar :fixed="false"` + `.m-page/.m-content` 布局；不能破坏上一任务修复的“顶栏不随内容滚动”。

5. **R5 状态栏图标可读性**  
   普通页面保持适合浅色 Navbar 的系统状态栏图标样式；沉浸播放器显示时保持现有深色沉浸背景上的图标样式切换。

6. **R6 组件库 issue**  
   向 `Happier-X/happier-ui` 提交 issue，包含环境、复现、实际/预期、根因、建议修复；建议库至少修复 `HNavBar`，并审视 `HTabBar` 的同类底部安全区问题。

7. **R7 跨环境回退**  
   普通浏览器、iOS 或未注入 `--safe-area-inset-top` 的环境仍通过标准 `env()` 工作；无安全区时结果为 `0px`。

## Out of Scope

- 在本任务内直接修改、发布或升级 `happier-ui`。
- 以 Android 15 的 opt-out 规避 Edge-to-Edge。
- 重做 Navbar 高度、标题排版或侧栏布局。
- 全面重构所有底部安全区调用；`HTabBar` 同类问题仅在 issue 中提示，除非验证发现 Muses 当前底栏也明显回归。
- iOS 原生工程专项改造。

## Acceptance Criteria

- [ ] **AC1** Android Edge-to-Edge 下 Navbar 背景连续覆盖状态栏区域，Navbar 内容不与系统状态栏图标/刘海重叠。
- [ ] **AC2** 宿主 CSS 使用 `var(--safe-area-inset-top, env(safe-area-inset-top, 0px))`，且 `capacitor.config.ts` 显式设置 `SystemBars.insetsHandling: "css"`。
- [ ] **AC3** 滚动歌曲/歌单/设置页时 Navbar 仍固定在页面顶部，`.m-content` 继续独立滚动。
- [ ] **AC4** 打开/关闭 Player、Queue 后普通页面 Navbar 的安全区与状态栏图标样式仍正确。
- [ ] **AC5** Web 构建、类型检查和 lint 通过；Android 配置可被 `npx cap sync android` 正确写入。
- [ ] **AC6** GitHub issue 已提交到 `Happier-X/happier-ui`，最终回复包含 issue 链接。
- [ ] **AC7** 不修改 `node_modules`、不使用 `file:../happier-ui`、不新增组件库本地 alias。

## Key Decisions

| 决策 | 选择 | 理由 |
|------|------|------|
| 根因归属 | happier-ui 兼容缺口，Muses 提供临时适配 | 宿主已正确开启 viewport-fit 和 safeArea；库只读不可靠 env |
| Android API | Capacitor 8 `SystemBars` | 面向 Android 15/16 Edge-to-Edge 的正式方案 |
| inset 顺序 | `var(--safe-area-inset-top, env(...))` | 与 Capacitor 8 官方文档一致 |
| 修复等待库发版 | 不等待 | 宿主 CSS 可安全回退，立即解决体验问题 |
| Navbar fixed | 保持 false | 与现有页面骨架和上一任务修复一致 |

## Risks / Deferred Items

- 需要真机或模拟器验证 WebView 注入时序；CSS 自定义属性在运行时写入后会自动重新计算，无需重挂载。
- Android 底部安全区同类问题可能影响 `HTabBar`，本任务默认只通过 issue 跟踪；若验证发现明显问题，再回到规划调整范围。
- `StatusBar` 与 `SystemBars` 同时用于样式控制可能存在重复职责；本任务仅让 `SystemBars` 负责 inset，保留现有 `StatusBar.setStyle`，避免扩大重构。

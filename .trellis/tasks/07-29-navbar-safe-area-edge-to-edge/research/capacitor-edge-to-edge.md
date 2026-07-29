# Capacitor 8 Android Edge-to-Edge 调研

## 版本与环境

- `@capacitor/core`: 8.4.2
- `@capacitor/android`: 8.4.2
- `@capacitor/status-bar`: 8.0.3
- `happier-ui`: 0.0.6
- `index.html` 已设置 `viewport-fit=cover`。

## 官方契约

来源：`node_modules/@capacitor/core/system-bars.md`

- Capacitor 8 内建 `SystemBars`，面向现代 edge-to-edge。
- Android WebView `< 140` 存在 `env(safe-area-inset-*)` 不可靠的问题。
- `SystemBars` 默认 `insetsHandling: "css"`，会向根元素注入：
  - `--safe-area-inset-top`
  - `--safe-area-inset-right`
  - `--safe-area-inset-bottom`
  - `--safe-area-inset-left`
- 官方推荐消费顺序：

```css
var(--safe-area-inset-top, env(safe-area-inset-top, 0px))
```

来源：`node_modules/@capacitor/status-bar/README.md`

- Android 15+ 强制 edge-to-edge；Android 16+ 不再允许 opt-out。
- `StatusBar.overlaysWebView` / `backgroundColor` 在 Android 15+/16+ 不再是可依赖方案。
- 因而不得以旧版 `StatusBar.setOverlaysWebView(true)` 作为本任务核心修复。

## Capacitor 实现核验

`@capacitor/android` 的 `SystemBars.java`：

- 默认 `insetsHandling = "css"`。
- 在 WindowInsets 回调中计算 system bars + display cutout。
- 将 dp 值写入 document root 的 `--safe-area-inset-*`。
- 配合 `viewport-fit=cover` 和 WebView 版本决定 passthrough / parent padding。

## happier-ui 核验

`HNavBar` 默认 `safeArea: true`，生成 `.h-nav-bar--safe-area`。

当前 CSS：

```css
.h-nav-bar--safe-area {
  padding-top: constant(safe-area-inset-top);
  padding-top: env(safe-area-inset-top, 0px);
}
```

问题：只读 `env()`，未消费 Capacitor 8 官方注入的 `--safe-area-inset-top`。Android WebView `< 140` 时可能得到 0，从而标题/按钮进入状态栏区域。

`HTabBar` 同样只读取底部 `env()`，属于相同类别，但本任务用户明确聚焦 navbar；issue 可提示库统一处理所有 safe-area 组件。

## 根因判定

- Muses 已正确设置 `viewport-fit=cover`，且 `HNavBar.safeArea` 保持默认 true。
- 缺口主要在 happier-ui 的跨 WebView safe-area 消费契约。
- 宿主可立即以全局 CSS 覆盖修复；组件库应正式支持 `var(--safe-area-inset-*, env(...))` 并发布新版本。

## 推荐方案

1. Muses 全局样式覆盖 `.h-nav-bar--safe-area`：

```css
.h-nav-bar--safe-area {
  padding-top: var(--safe-area-inset-top, env(safe-area-inset-top, 0px));
}
```

2. `capacitor.config.ts` 显式声明 `SystemBars.insetsHandling = "css"`，虽默认如此，但将应用契约固定下来。
3. 继续用 navbar 根元素背景铺满安全区；内层 `.h-nav-bar__inner` 位于 padding 下，实现背景 edge-to-edge、交互内容避让。
4. 向 `Happier-X/happier-ui` 提 issue，请库统一兼容 Capacitor 8 CSS 变量（至少 HNavBar；建议同时 HTabBar）。

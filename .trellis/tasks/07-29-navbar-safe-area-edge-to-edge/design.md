# Design：Navbar 安全区 Edge-to-Edge 适配

## 目标布局

```text
屏幕顶部
┌──────────────────────────┐
│ 系统状态栏图标            │ ← Navbar 根背景延伸到这里
├──────────────────────────┤ ← inset-top
│ 返回       标题       操作 │ ← .h-nav-bar__inner，安全区下方
└──────────────────────────┘
│ .m-content 独立滚动       │
```

## 根因链路

```text
Android WindowInsets
  → Capacitor 8 SystemBars（默认 insetsHandling=css）
  → documentElement --safe-area-inset-top
  → happier-ui HNavBar 只读 env(safe-area-inset-top)
  → WebView < 140 env 可能为 0
  → Navbar padding-top 为 0，交互内容进入状态栏区域
```

## 选定方案

### 1. 显式固定 Capacitor 契约

在 `capacitor.config.ts` 增加：

```ts
plugins: {
  SystemBars: {
    insetsHandling: 'css',
  },
  // 其他现有插件保持不变
}
```

虽然 Capacitor 8 默认值是 `css`，显式配置可以避免升级或环境差异，并让应用约束可审查。

### 2. 宿主样式兼容覆盖

在 `src/theme/tailwind.css` 的全局组件覆盖层加入：

```css
.h-nav-bar--safe-area {
  padding-top: var(--safe-area-inset-top, env(safe-area-inset-top, 0px));
}
```

语义：

- Capacitor Android：优先用 SystemBars 注入值。
- 标准浏览器/iOS：自定义变量不存在，回退到 `env()`。
- 无安全区：最终为 `0px`。

Navbar 根元素本身有背景，padding 也属于其背景绘制区域，因此实现 Edge-to-Edge 背景；`.h-nav-bar__inner` 被 padding 推到安全区下方。

### 3. 保持现有页面布局

- 不修改 `MPage.vue` 的 `:fixed="false"`。
- 不修改 `.m-page/.m-content` 滚动归属。
- 不给 Navbar 再加额外占位，避免安全区重复计算。

### 4. 状态栏图标样式

- 保留 `App.vue` 当前 `StatusBar.setStyle` 流程：普通页 `Style.Default`，播放器 `Style.Dark`。
- 不调用已在 Android 15/16 受限的 `setOverlaysWebView` / `setBackgroundColor`。
- 若真机显示图标样式与背景不匹配，优先校准现有 style 语义，不改变 inset 方案。

### 5. 组件库 issue

目标：`Happier-X/happier-ui`。

Issue 内容：

- 标题：`HNavBar safe-area should support Capacitor 8 --safe-area-inset-* fallbacks`
- 环境：happier-ui 0.0.6、Capacitor 8、Android Edge-to-Edge、WebView < 140。
- 复现：viewport-fit=cover + HNavBar 默认 safeArea；Capacitor 注入非零自定义变量，但 `env()` 为 0。
- 实际：Navbar 内容与状态栏重叠。
- 预期：背景延伸到状态栏后，inner 内容避让。
- 建议：
  ```css
  padding-top: var(--safe-area-inset-top, env(safe-area-inset-top, 0px));
  ```
- 同类审视：`HTabBar` 使用 bottom inset，也应采用相同回退链。

## 未选方案

### `StatusBar.setOverlaysWebView(true)`

不选。Android 15/16 Edge-to-Edge 已强制化，Capacitor 文档明确这些旧配置在新系统不可依赖。

### 修改 `node_modules/happier-ui`

不选。不可提交且升级会丢失；用宿主覆盖并让组件库正式修复。

### 给 `html/body` 统一加 safe-area padding

不选。会让 Player、Queue、TabBar、MiniPlayer 等所有 fixed/overlay 层重复避让，破坏真正的 Edge-to-Edge。

## 兼容性与回滚

- CSS 规则有三级回退，不依赖 Capacitor 时不会产生副作用。
- `SystemBars.insetsHandling` 是 Capacitor 8 已支持配置。
- 回滚只需删除一条插件配置和一条 CSS 覆盖；无数据迁移。

## 验证矩阵

| 环境 | 预期 |
|------|------|
| Android Edge-to-Edge / WebView < 140 | 使用 `--safe-area-inset-top`，内容避让 |
| Android 新 WebView | 自定义变量或标准 env 均可，结果不重复 |
| 桌面浏览器 | inset 0，Navbar 高度维持现状 |
| iOS / 支持 env 的 WebView | 回退标准 env |
| Player overlay | 保持沉浸式铺满和图标样式切换 |

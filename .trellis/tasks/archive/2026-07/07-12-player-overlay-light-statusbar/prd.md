# 沉浸式播放页状态栏改为白色

## Goal

沉浸式播放页为深色背景时，Android 顶部状态栏图标/文字使用白色；退出播放页后恢复应用默认状态栏样式。

## Issue

- GitHub #15：`沉浸式播放页面是深色的，打开沉浸式页面的时候顶部状态栏应该是白色的才对`

## Confirmed Facts

- 项目已依赖 `@capacitor/status-bar@8.0.2`，无需新增依赖。
- `App.vue` 持有 `playerOverlayVisible`，并负责全局 overlay 生命周期和 Android 返回键关闭。
- Capacitor StatusBar 的枚举命名容易误解：
  - `Style.Dark` = 深色背景使用**浅色（白色）状态栏内容**。
  - `Style.Light` = 浅色背景使用深色状态栏内容。
  - `Style.Default` = 恢复系统/设备外观默认。
- 队列 overlay 不是沉浸式深色播放页，本 issue 仅要求播放器 overlay。

## Requirements

1. **R1** `playerOverlayVisible = true` 时调用 `StatusBar.setStyle({ style: Style.Dark })`，使状态栏图标/文字为白色。
2. **R2** 播放器 overlay 关闭时调用 `StatusBar.setStyle({ style: Style.Default })` 恢复默认。
3. **R3** `App.vue` 卸载时恢复 `Style.Default`，避免状态泄漏。
4. **R4** 非 Android / 插件不可用 / 调用失败时静默忽略，不阻塞 overlay 打开关闭。
5. **R5** 快速开关 overlay 时不得因异步调用乱序最终留下错误样式；使用请求 token 或串行化校验。
6. **R6** 队列 overlay 单独打开时不强制白色状态栏。
7. **R7** 补充单测：打开、关闭、队列不影响、失败静默、快速切换最终状态。
8. **R8** 同步 component-guidelines 中播放器 overlay 的系统状态栏约定。

## Acceptance Criteria

- [ ] AC1：打开沉浸式播放页后状态栏内容为白色。
- [ ] AC2：关闭沉浸式播放页后恢复默认状态栏样式。
- [ ] AC3：快速开关后最终样式与 `playerOverlayVisible` 一致。
- [ ] AC4：队列 overlay 不触发播放器状态栏样式。
- [ ] AC5：插件失败不产生未处理 Promise 或阻断 UI。
- [ ] AC6：测试、lint、type-check 通过，spec 已同步。

## Implementation Notes

- 建议在 `App.vue` watch `playerOverlayVisible`，不要在 `PlayerPage.vue` 内设置：异步组件挂载/过渡动画可能让恢复时机不稳定。
- 使用 `void StatusBar.setStyle(...).catch(() => undefined)`；结合递增 token 防止先发出的调用晚完成覆盖新状态。

## Task Type

Lightweight

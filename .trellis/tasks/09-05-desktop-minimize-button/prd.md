# 桌面标题栏最小化按钮实现真最小化

## Goal

composeApp 自定义标题栏(undecorated 窗口)的窗口控制"─"按钮当前是空实现(S3a 首版留空,点击无反应),用 Compose Desktop `WindowState.isMinimized` 实现真最小化,并保证任务栏点击可恢复、恢复后窗口状态不变。

## 背景

`DesktopTitleBar.kt:92` 首版注释"Compose Desktop 暂无 minimize API,首版留空"。经反编译 ui-desktop 1.12.0-rc01 验证,`WindowState` 接口存在 `isMinimized` 属性(getter/setter),可直接使用。窗口为 `undecorated = true` 的 Compose Desktop `Window`(Main.kt),最小化行为需实测验证。

## Requirements

1. `DesktopTitleBar.kt` 最小化按钮 onClick 设 `windowState.isMinimized = true`,移除 TODO 注释。
2. 不改动最大化/还原按钮、拖拽、双击最大化等既有逻辑。
3. 最小化后任务栏点击可恢复窗口,恢复后尺寸/最大化状态不变。

## Acceptance Criteria

- [ ] `:composeApp:compileKotlinJvm` 编译通过。
- [ ] 实测:启动桌面应用,点击"─"按钮窗口最小化;任务栏点击恢复,内容与窗口状态正常。
- [ ] 改动仅涉及 `DesktopTitleBar.kt` 最小化按钮回调(不触碰 scrape 相关模块)。

## Notes

- 与并行任务 09-05-scrape-kmp 零交集(它动 core/common scrape 包与 ui-shared)。

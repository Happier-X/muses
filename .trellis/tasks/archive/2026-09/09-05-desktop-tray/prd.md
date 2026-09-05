# 桌面系统托盘（显示窗口+播放控制）

## Goal

落地 `JvmPlayerPort.kt` 二期预留的系统托盘（`setTrayVisible` 空 TODO 所指能力）:AWT SystemTray 常驻图标,菜单含显示主窗口/播放暂停(动态)/上一首/下一首/退出,左键单击唤起主窗口;Main.kt 统一组装单一 DesktopPlayerHook 供 screens 与托盘共享,避免多实例状态分裂。

## 背景

- 现状:`JvmPlayerPort.kt:734` 起的托盘/SMTC/音频焦点为"二期预留空实现",托盘无调用方。
- `DesktopContainer.playerPort()` 非单例(每次新建 `JvmPlayerPort`),而 LibraryScreen/PlayerScreen 在未传 hook 时各自 `remember { DesktopPlayerHook() }`,存在状态分裂隐患;托盘若再自建 hook 会加剧。
- 项目无 png/ico 图标资源,托盘图标用程序化 BufferedImage 占位(项目蓝圆底 + "M"),正式图标后续任务替换。

## Requirements

1. `desktop` 模块新增 `DesktopTray`(`playback` 或 `tray` 包,纯 AWT + kotlinx-coroutines,不依赖 Compose):
   - `install()`/`uninstall()` 生命周期方法;`SystemTray.isSupported()` 为 false 时静默降级(no-op),保证无头环境/测试安全;
   - AWT 创建与状态更新走 EDT(`EventQueue.invokeLater`);
   - 托盘菜单:显示主窗口 / 播放⇄暂停(`isPlaying: StateFlow<Boolean>` 驱动动态 label)/ 上一首 / 下一首 / 分隔线 / 退出;动作经构造注入的 lambda 提供;
   - 左键单击托盘图标 → 显示主窗口;
   - 图标:程序化 BufferedImage 占位(不引入二进制资源)。
2. `Main.kt` 组装:
   - 创建单一 `DesktopPlayerHook`,同时传给 `MusesDesktopApp(playerHook = ...)` 与托盘,screens 不再各自默认建实例;
   - `DisposableEffect` 内 `install()`/`uninstall()`,托盘生命周期与应用一致;
   - 显示主窗口实现为 `windowState.isMinimized = false`;
   - 退出复用 `exitApplication`,X 按钮行为不变。
3. 最小化按钮(上一任务)、双击最大化、拖拽等既有行为不变。
4. `JvmPlayerPort` 的 `setTrayVisible` 空实现保持不动(无调用方,本任务不扩其语义)。

## Acceptance Criteria

- [ ] `:composeApp:compileKotlinJvm` 编译通过。
- [ ] 运行实测:应用启动后托盘区出现图标;左键单击唤起主窗口;托盘菜单项齐全,播放/暂停 label 随 `isPlaying` 切换;「退出」结束应用且托盘图标移除。
- [ ] screens 与托盘共享同一 `DesktopPlayerHook`(单一播放状态源)。
- [ ] 改动仅涉及 `desktop` 模块新增托盘类与 `Main.kt` 组装(不触碰 scrape 相关模块)。

## Notes

- SMTC(系统媒体传输控制)与音频焦点仍为二期预留,不在本任务范围。
- 正式托盘/应用图标(png 资源 + jpackage icon 配置)留后续任务。

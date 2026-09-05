# 桌面端 SMTC 系统媒体控制

## Goal

Windows SMTC（System Media Transport Controls）：应用播放时出现在任务栏媒体浮层/音量面板，展示曲目信息并支持系统按键控制播放。属桌面端二期集成（托盘/图标/最小化已完成）的最后一项。

## 背景

- `JvmPlayerPort.updateSystemMediaTransport(info: String?)` 已预留空实现；
- `DesktopTray` 的 lambda 注入模式（组件不感知播放器实现）是本任务的接线范本；
- Muses 为 Win32 桌面应用（jpackage 打包的 Compose Desktop JVM 进程），注册媒体会话应走 `ISystemMediaTransportControlsInterop::GetForWindow(hwnd)`，而非 UWP `GetForCurrentView`，也不是 `GlobalSystemMediaTransportControlsSessionManager`（那是旁观他人会话）。

## Requirements

- FR1 媒体会话注册：向 Windows 注册媒体会话并绑定主窗口；重复调用安全。
- FR2 元数据更新：曲目切换时更新标题、艺术家、专辑；播放/暂停/停止状态实时同步。
- FR3 按键控制：Play/Pause/Toggle、Next、Previous 回调到 `DesktopPlayerHook`，语义与托盘菜单一致（previous 的 >3s 回曲首沿用 `JvmPlayerPort.previous()`）。
- FR4 时间轴：随 positionMs/durationMs 推进更新进度（允许 1s 级节流）。
- FR5 封面：本地文件路径可得时尝试设置缩略图；失败静默跳过。
- FR6 生命周期：应用退出清理会话；SMTC 任何失败不影响播放主链路（静默降级 + errorLog）。
- FR7 非 Windows / 系统不支持时无副作用、不报错。

## 约束

- 首选零新增第三方依赖：`desktop` 模块已有 JNA 5.17.0 / jna-platform，优先纯 JNA 手写 WinRT interop；仅当调研发现维护良好的现成 JVM 库才引入。
- SMTC 组件放 `:desktop` 模块，不依赖 Compose；动作以 lambda 注入（对齐 `DesktopTray` 模式）。
- WinRT/COM 调用注意线程模型（apartment），不得阻塞 UI 与播放线程。

## Acceptance Criteria

- [ ] AC1 播放歌曲后任务栏媒体浮层显示 Muses 与曲名/艺术家（用户目视确认）。
- [ ] AC2 浮层按键：暂停/播放、下一首/上一首均生效。
- [ ] AC3 暂停时浮层状态为暂停；切歌后元数据刷新。
- [ ] AC4 应用退出后浮层不再显示 Muses。
- [ ] AC5 `:desktop` 与 `:composeApp` 编译通过；状态映射/降级逻辑有单元测试。
- [ ] AC6 全流程 try-catch：非 Windows 环境（CI 测试）下无未捕获异常。

## Notes

- 系统浮层显示与按键无法自动化验收，AC1–AC4 需用户手动确认（同托盘任务的先例）。
- 技术方案（JNA interop 细节 / 是否用现成库）见 `design.md`，依据 `research/smtc-interop.md` 调研结论。

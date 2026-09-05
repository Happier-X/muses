# 技术设计：桌面端 SMTC 系统媒体控制

> 技术依据：`research/smtc-interop.md`（调研报告，IID/vtable 顺序经官方 winmd 生成物 windows-rs 与本地 SDK IDL 双重核对）。以下常量已核对。

## 方案选型

**纯 JNA 手写 WinRT interop**（零新增依赖）：

- JNA 5.17.0 的 `Ole32` 无 `RoInitialize`/`RoGetActivationFactory`（已 javap 验证），但这几个 API 都导出自 `combase.dll`，用 `Native.load("combase")` 自行声明即可，共 4 个函数；
- 无维护良好的现成 JVM 库（调研确认后写入结论）；
- 不引入原生 DLL 构建链（C++/Rust helper 超出 Kotlin 工程舒适区，放弃）。

SMTC 注册方式：Win32 桌面应用必须走 `ISystemMediaTransportControlsInterop::GetForWindow(hwnd)` 绑定主窗口，不能用 UWP `GetForCurrentView`；`GlobalSystemMediaTransportControlsSessionManager` 是旁观他人会话的 API，不采用。

## 组件与文件布局（:desktop 模块，smtc 包）

| 文件 | 职责 |
|---|---|
| `smtc/WinRtRuntime.kt` | `combase.dll` 声明（RoInitialize/RoGetActivationFactory/WindowsCreateString/WindowsDeleteString）+ HSTRING 创建/释放助手 |
| `smtc/SmtcInterop.kt` | IID 常量、WinRT vtable 调用助手（`Function.getFunction(Pointer)` 从函数指针构造调用）、ButtonPressed 事件的 COM 回调对象（IUnknown+IInspectable+Invoke 的 JNA Structure vtable 实现，仿 JNA `DispatchListener`） |
| `smtc/SmtcSession.kt` | 会话接口：`updateMetadata/updatePlaybackStatus/updateTimeline/close`，供测试替换 |
| `smtc/SmtcWinRtSession.kt` | 真实实现：GetForWindow → put_IsEnabled/各按钮启用 → PlaybackStatus/DisplayUpdater/TimelineProperties 更新 |
| `smtc/SmtcController.kt` | 门面（对齐 `DesktopTray` lambda 注入模式）： hwnd 查找（EnumWindows 过滤当前进程）、StateFlow 订阅与节流、单线程串行调度、静默降级 |
| `desktop/src/test/.../smtc/SmtcControllerTest.kt` | 状态映射/降级/节流单测（fake SmtcSession） |

## 数据流

```
Main.kt (composeApp jvmMain)
  DesktopPlayerHook StateFlows
    combine(songs, currentSongId, isPlaying) → SmtcMetadata(标题/艺术家/专辑/封面文件)
    positionMs/durationMs → SmtcTimeline（sample 节流 1s）
  ↓ install(hwnd, metadataFlow, timelineFlow, onTogglePlay, onNext, onPrevious)
SmtcController（专用单线程 daemon executor 串行执行全部 WinRT 调用）
  ↓ vtable 调用
Windows.Media.SystemMediaTransportControls（GetForWindow 绑定主窗口）
```

- **hwnd 获取**：JNA `User32.EnumWindows` 过滤 `GetWindowThreadProcessId == 当前进程 && IsWindowVisible && 标题 "Muses"`；窗口未显示时 install 内轮询重试（500ms × 20）直至拿到或放弃（放弃 = 静默无 SMTC）。
- **动作回流**：ButtonPressed 事件回调（Windows 线程池线程）→ 直接调用注入 lambda（lambda 内部自己 launch 协程，同托盘模式）；回调实现内只做 try-catch 包裹，不抛异常跨 COM 边界。
- **元数据来源**：`DesktopPlayerHook.songs: StateFlow<List<SongEntity>>` + `currentSongId` 组合，装配层在 Main.kt 完成 combine，SmtcController 不依赖 Room 类型。

## 状态与降级

- PlaybackStatus（`MediaPlaybackStatus`）：`Closed=0, Changing=1, Stopped=2, Playing=3, Paused=4`；映射：`isPlaying → Playing(3)`；`!isPlaying && 有曲目 → Paused(4)`；`无曲目 → Stopped(2)`。
- ButtonPressed（`SystemMediaTransportControlsButton`）：`Play=0, Pause=1, Stop=2, Record=3, FastForward=4, Rewind=5, Next=6, Previous=7`；分发：Play/Pause → onTogglePlay（面板按当前状态只暴露其一，toggle 语义等价），Next=6 → onNext，Previous=7 → onPrevious；Stop 等其余按钮不启用，不会到达。
- 时间轴：`SystemMediaTransportControlsTimelineProperties` 是 activatable 对象（非 POD struct），需 RoGetActivationFactory + ActivateInstance 激活后经其接口（put_EndTime/MinSeekTime/MaxSeekTime/Position，TimeSpan=Int64 100ns）赋值，再经 **ISystemMediaTransportControls2::UpdateTimelineProperties（槽 12，需 QI）** 提交；`MediaPlaybackType.Music=1`。
- 元数据路径：get_DisplayUpdater（槽 8）→ put_Type(Music)（槽 7）→ get_MusicProperties（槽 12）→ put_Title（槽 7）/put_AlbumArtist（槽 9）/put_Artist（槽 11，注意 AlbumArtist 在 Artist 前）→ QI IMusicDisplayProperties2 put_AlbumTitle（槽 7）→ DisplayUpdater.Update()（槽 17，**不调用则面板不刷新**）。
- 封面：调研确认 `RandomAccessStreamReference.CreateFromUri(file://)` 理论可行（Uri factory 同步创建），但 file:// 接受性需运行时验证 → **首版不做**，面板显示文字元数据；封面列为二期增强。
- 降级矩阵（全部静默，`errorLog` 记录）：
  - 非 Windows / combase 缺失 / RoGetActivationFactory 失败 → install no-op，不抛异常；
  - `RoInitialize` 返回 `S_FALSE(1)`（已初始化）继续；`RPC_E_CHANGED_MODE` 放弃 SMTC（专用 MTA 线程下不应发生）；
  - 单次 update 失败 → 记日志，不影响后续更新与播放主链路；
  - uninstall/close 在退出时调用：remove_ButtonPressed + put_IsEnabled(false) + Release，失败仅记日志。

## 线程模型

- 专用单线程 daemon executor：首次调用时 `RoInitialize(RO_INIT_MULTITHREADED)`，全部 WinRT/HSTRING 调用串行在该线程执行（规避 apartment 争议，对象 agile 与否不再依赖）；
- 禁止在 UI/播放线程直接调用 WinRT；
- executor 生命周期由 `uninstall()` 关闭。

## 与既有预留代码的关系

- `JvmPlayerPort.updateSystemMediaTransport(info: String?)` 空实现：注释改为指向 `SmtcController`（同 `setTrayVisible` 与 `DesktopTray` 的关系），方法保留不删（保持接口稳定，本次不接线）；
- `DesktopTray` 不受影响，SMTC 与托盘并存（两者状态源相同）。

## 接线（composeApp jvmMain Main.kt）

```
val smtc = remember { SmtcController(errorLog = ...) }
DisposableEffect(Unit) {
    smtc.install(
        windowTitle = "Muses",
        metadata = combined flow,
        timeline = position/duration sample(1s),
        onTogglePlay = playerHook::togglePlayPause,
        onNext = playerHook::next,
        onPrevious = playerHook::previous,
    )
    onDispose { smtc.uninstall() }
}
```

与 `tray.install()` 同一 DisposableEffect 或并列均可（并列更清晰）。

## 风险与回滚

- 风险 1：IID/vtable 顺序记错 → 以微软官方文档核对（调研报告），运行时失败有 HRESULT 检查与日志；
- 风险 2：事件回调对象实现缺陷（COM 引用计数）→ AddRef/Release 最小实现（引用计数自增到 1 固定，卸载时一次性 Release），事件 token remove；
- 风险 3：SMTC 显示异常 → 全链路 try-catch + uninstall 即完全回退；
- 回滚：删除 `smtc/` 包 + Main.kt 接线三行 + JvmPlayerPort 注释行，无持久化/DB 变更。

## 常量备忘（已按 research/smtc-interop.md 核对）

- activation：`Windows.Media.SystemMediaTransportControls`
- `ISystemMediaTransportControlsInterop` IID `DDB0472D-C911-4A1F-86D9-DC3D71A95F5A`；GetForWindow 在槽 6，签名 `(HWND, REFIID, void**)`
- `ISystemMediaTransportControls` IID `99FA3FF4-1742-42A6-902E-087D41F965EC`，槽 6 起：get/put_PlaybackStatus(6/7)、get_DisplayUpdater(8)、get_SoundLevel(9)、get/put_IsEnabled(10/11)、get/put_IsPlayEnabled(12/13)、get/put_IsStopEnabled(14/15)、get/put_IsPauseEnabled(16/17)、get/put_IsRecordEnabled(18/19)、get/put_IsFastForwardEnabled(20/21)、get/put_IsRewindEnabled(22/23)、get/put_IsPreviousEnabled(24/25)、get/put_IsNextEnabled(26/27)、get/put_IsChannelUpEnabled(28/29)、get/put_IsChannelDownEnabled(30/31)、add_ButtonPressed(32)/remove_ButtonPressed(33)、add/remove_PropertyChanged(34/35)
- `ISystemMediaTransportControls2` IID `EA98D2F6-7F3C-4AF2-A586-72889808EFB1`，槽 12 = UpdateTimelineProperties（QI 后调用）
- `ISystemMediaTransportControlsDisplayUpdater` IID `8ABBC53E-FA55-4ECF-AD8E-C984E5DD1550`，槽 6 起：get/put_Type(6/7)、get/put_AppMediaId(8/9)、get/put_Thumbnail(10/11)、get_MusicProperties(12)、get_VideoProperties(13)、get_ImageProperties(14)、CopyFromFileAsync(15)、ClearAll(16)、**Update(17)**
- `IMusicDisplayProperties` IID `6BBF0C59-D0A0-4D26-92A0-F978E1D18E7B`，槽 6 起：get/put_Title(6/7)、get/put_AlbumArtist(8/9)、get/put_Artist(10/11)；`IMusicDisplayProperties2` IID `00368462-97D3-44B9-B00F-008AFCEFAF18`，槽 6/7 = get/put_AlbumTitle（QI 后调用）
- `SystemMediaTransportControlsTimelineProperties` 接口 IID `5125316A-C3A2-475B-8507-93534DC88F15`，槽 6 起 get/put 交替：StartTime/EndTime/MinSeekTime/MaxSeekTime/Position；activation 字符串 `Windows.Media.SystemMediaTransportControlsTimelineProperties`，经 IActivationFactory.ActivateInstance 激活
- `ITypedEventHandler<SMTC, ButtonPressedEventArgs>` IID `0557E996-7B23-5BAE-AA81-EA0D671143A4`；EventArgs `ISystemMediaTransportControlsButtonPressedEventArgs` IID `B7F47116-A56F-4DC8-9E11-92031F4A87C2`，槽 6 = get_Button（out Int32）
- ButtonPressed 回调对象 QI 需响应：IUnknown、IInspectable、IAgileObject `{94EA2B94-E9CC-49E0-C0FF-EE64CA8F5B90}`、handler IID
- `MediaPlaybackStatus`：Closed=0/Changing=1/Stopped=2/Playing=3/Paused=4；`MediaPlaybackType`：Music=1
- HSTRING 经 `WindowsCreateString(UTF-16, 字符数)` 创建、`WindowsDeleteString` 释放；`Windows.Foundation.TimeSpan` ABI = Int64（100ns 单位）
- add_ButtonPressed ABI：`(this, handler*, EventRegistrationToken* out)`（token 为 Int64 出参）；remove_ButtonPressed：`(this, token: Int64)`

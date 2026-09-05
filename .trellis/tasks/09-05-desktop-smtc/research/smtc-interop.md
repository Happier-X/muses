# Windows SMTC 互操作调研报告（桌面端 JVM 方案）

日期：2026-09-05
背景：muses :desktop（Compose Desktop + 纯 JVM，已依赖 jna / jna-platform 5.17.0，播放器 VLCJ）需要在 Win32 桌面进程中注册 SMTC 媒体会话（任务栏/音量面板显示媒体信息 + 播放/暂停/上一首/下一首按键）。

## 0. 结论速览

1. **没有可直接复用的"纯 JNA/纯 Java 注册自己的 SMTC 会话"的成熟库**。最接近的是 JMTC（Maven Central 可用，但内部是 JNI + 预编译 C++/WinRT 原生 DLL，且通过创建 WinRT MediaPlayer 实例获取会话，不是 GetForWindow 绑定 HWND）。
2. **推荐纯 JNA 手写**：所需接口面很窄（1 个 interop 接口 + 3 个 WinRT 接口 + 1 个回调 COM 对象 + RoInitialize/RoGetActivationFactory/WindowsCreateString 三个 native 函数），全部关键 IID 与 vtable 顺序已确认（本文均有来源）。
3. `github.com/KtWinRT` 组织**不存在**（404）。实际存在的是 `compose-fluent/kotlin-winrt`（Kotlin/JVM WinRT 绑定，但要求 JDK 25 FFM API，且仅 0.1.0-SNAPSHOT，未发布正式版），不适合当前 JDK 栈。
4. PowerShell/外部进程注册 SMTC **不可行**（会话必须由拥有窗口的本进程创建，见第 5 节）。
5. ButtonPressed 枚举值注意：**Next=6、Previous=7**（不是常见臆测的 3/4，完整表见 3.f）。

---

## 1. 现成 JVM/Java/Kotlin 库调研

### 1.1 JMTC（JavaMediaTransportControls）— 唯一真正"注册自己会话"的 JVM 库

- GitHub：https://github.com/Selemba1000/JavaMediaTransportControls（MIT，22 stars，最后 push 2024-08-15，Maven Central 最后发布 0.0.3 于 2024-04-30，已约两年未更新）
- Maven 坐标（已验证 Maven Central metadata）：`io.github.selemba1000:JavaMediaTransportControls:0.0.3`
  （README 中 `JavaMediaTranportControls` 拼写有误，正确 artifactId 为 `JavaMediaTransportControls`）
- 功能：标题/艺术家/专辑/封面、TimelineProperties、Seek/Play/Pause/Stop/Shuffle/Rate/Loop 回调，Lambda 风格 API。
- 实现方式（已核实源码 `src/main/sources/SMTCAdapter.cpp`）：
  - **JNI + 预编译 C++/WinRT 原生 DLL**（README 提供预编译产物，自建需 Windows 10 SDK + MSBuild）；
  - 初始化用 `winrt::init_apartment()`，并**创建 WinRT MediaPlayer 实例后取 `mp.SystemMediaTransportControls()`**（而非 `ISystemMediaTransportControlsInterop::GetForWindow`）；
  - 封面用 `RandomAccessStreamReference::CreateFromFile(StorageFile)`（异步 GetFileFromPathAsync）。
- 风险：需分发 native DLL；会话不绑定 muses 窗口 HWND（Win32 下 `GetForCurrentView` 不可用，`MediaPlayer.SystemMediaTransportControls()` 是否在所有 Win10/11 稳定工作**未查到确切文档保证，需运行时验证**）；版本陈旧。
- 结论：**不推荐作为首选**，但可作为行为对照参考。

### 1.2 compose-fluent/kotlin-winrt（即"KtWinRT"类项目）

- GitHub：https://github.com/compose-fluent/kotlin-winrt （27 stars，提交活跃，最近更新 2026-09）
- 定位：Kotlin/JVM + Kotlin/Mingw 的 WinRT 绑定系统（含 ABI、COM interop、activation、WinMD 加载与代码生成器）。
- 坐标：`io.github.compose-fluent` 组，运行时 `winrt-runtime-jvm`（纯 JVM），预构建投影 `winrt-projections-windows-sdk` 等；**当前仅 0.1.0-SNAPSHOT（Sonatype 快照仓库），无正式版**。
- 关键限制：JVM 桥基于 **JDK 25 的 java.lang.foreign（FFM）API**；非 XAML 调用需 `RuntimeScope.initializeSingleThreaded().use { ... }`。
- windows.media / SystemMediaTransportControls：预构建投影官方页面未明确列出（windows-sdk 投影理论上覆盖全 SDK，**未逐一验证**，需实测）。
- 结论：**观察项**。若 muses 未来升级到 JDK 25+ 且需要大量 WinRT API，可复评；当前与 JNA 栈不匹配。

### 1.3 其他

- `DubyaDude/WindowsMediaController`（C#）：只读**其他应用**的会话（GlobalSystemMediaTransportControlsSessionManager 思路），不符合需求。
- `mpv`（datasone/MPVMediaControl，Rust）、`thewizrd/iTunes-SMTC`（C#）：原生实现参考，非 JVM。
- 搜索 `ISystemMediaTransportControlsInterop java/kotlin/jvm` 未发现任何直接绑定实现（**未查到现成案例**）。

---

## 2. ISystemMediaTransportControlsInterop（官方定义，本地 Windows SDK 10.0.26100.0 验证）

来源：本地 SDK `um/SystemMediaTransportControlsInterop.idl`（与 learn.microsoft.com 文档一致）。

- 接口 IID：**`DDB0472D-C911-4A1F-86D9-DC3D71A95F5A`**
- 继承 `IInspectable`，仅 1 个方法（vtable 槽位 6）：

```cpp
HRESULT GetForWindow(
    [in]  HWND   appWindow,      // 必须是调用进程的顶层窗口
    [in]  REFIID riid,           // 传 IID_ISystemMediaTransportControls
    [out, retval, iid_is(riid)] void **mediaTransportControl);
```

- 由 `Windows.Media.SystemMediaTransportControls` 的 activation factory 实现（即先 RoGetActivationFactory 取 factory，再 QI/直接取 interop 接口调 GetForWindow）。
- 要求 NTDDI_WINTHRESHOLD（Win10 1507+），DESKTOP 分区。
- `SystemMediaTransportControls` 运行时类同时实现 `ISystemMediaTransportControls` 与 `ISystemMediaTransportControls2`（时间轴等在 2 上，见 3.g）。

## 3. 纯 JNA 方案技术细节（首选路径）

### 3.a activation factory 与 RoInitialize（JNA 现有能力缺口）

- Factory 字符串：**`"Windows.Media.SystemMediaTransportControls"`**（WinRT runtime class 名）。
- `RoGetActivationFactory(HSTRING activatableClassId, REFIID iid, void** factory)`：
  - 位于 **combase.dll**，头 roapi.h，Win8+ 桌面应用可用（learn.microsoft.com/.../roapi/nf-roapi-rogetactivationfactory）；
  - 调用前线程必须已初始化 WinRT（RoInitialize 或 CoInitializeEx）。
- `RoInitialize(RO_INIT_TYPE)`：`RO_INIT_SINGLETHREADED=0`、`RO_INIT_MULTITHREADED=1`（Win8+ 桌面可用，ComBase.dll）。
  - **建议在专用后台线程 `RoInitialize(RO_INIT_MULTITHREADED)`（MTA）**，避免与 JVM 主线程上可能已存在的 STA（VLCJ/其他 JNI 组件）冲突；返回 `S_FALSE` 表示已初始化（仍需配对 RoUninitialize），`RPC_E_CHANGED_MODE` 表示 apartment 不一致。
- **JNA 5.17 的 `com.sun.jna.platform.win32.Ole32` 没有 RoInitialize/RoGetActivationFactory**（已核对 5.17 源码：仅有 CoInitializeEx/CoCreateInstance/CoTaskMemAlloc 等）。需自行映射：

```kotlin
interface Combase : StdCallLibrary {
    fun RoInitialize(initType: Int): Int            // 1 = RO_INIT_MULTITHREADED
    fun RoUninitialize()
    fun RoGetActivationFactory(classId: HSTRING, iid: GUID.ByReference, out: PointerByReference): Int
    fun WindowsCreateString(src: WString, len: Int, out: PointerByReference): Int
    fun WindowsDeleteString(hstr: HSTRING)
    companion object { val INSTANCE = Native.load("combase", Combase::class.java, W32APIOptions.DEFAULT_OPTIONS) }
}
```
- JNA 无 HSTRING 类型，须用 `WindowsCreateString`（combase.dll）创建（UTF-16 宽字符 + 长度）。**JNA 未封装 HSTRING，需运行时验证封装细节**（也可考虑传 `WString`——不保证正确，不建议）。
- JNA `com.sun.jna.platform.win32.COM` 包已有 COM 调用支持（Unknown/Dispatch/COMInvoker/ConnectionPoint），但均为 IDispatch 时代的 Automation 对象模型，对 WinRT IInspectable 无直接帮助；**可复用的是其 vtable 模拟模式**（见 3.f）。

### 3.b ISystemMediaTransportControls（IID 与 vtable 顺序）

IID：**`99FA3FF4-1742-42A6-902E-087D41F965EC`**
来源：microsoft/windows-rs `Windows/Media/mod.rs`（生成自官方 winmd）；方法顺序与 learn.microsoft.com UWP 接口文档一致。
槽位 0-2 = IUnknown(QueryInterface/AddRef/Release)，3-5 = IInspectable(GetIids/GetRuntimeClassName/GetTrustLevel)，**方法从槽位 6 开始**：

方法顺序严格 get/put 交替（完整顺序，与官方 winmd 一致）：

| 槽位 | 方法 | 槽位 | 方法 |
|---|---|---|---|
| 6 | get_PlaybackStatus | 21 | put_IsFastForwardEnabled |
| 7 | put_PlaybackStatus | 22 | get_IsRewindEnabled |
| 8 | get_DisplayUpdater | 23 | put_IsRewindEnabled |
| 9 | get_SoundLevel | 24 | get_IsPreviousEnabled |
| 10 | get_IsEnabled | 25 | put_IsPreviousEnabled |
| 11 | put_IsEnabled | 26 | get_IsNextEnabled |
| 12 | get_IsPlayEnabled | 27 | put_IsNextEnabled |
| 13 | put_IsPlayEnabled | 28 | get_IsChannelUpEnabled |
| 14 | get_IsStopEnabled | 29 | put_IsChannelUpEnabled |
| 15 | put_IsStopEnabled | 30 | get_IsChannelDownEnabled |
| 16 | get_IsPauseEnabled | 31 | put_IsChannelDownEnabled |
| 17 | put_IsPauseEnabled | 32 | add_ButtonPressed(handler) → EventRegistrationToken(Int64) |
| 18 | get_IsRecordEnabled | 33 | remove_ButtonPressed(token) |
| 19 | put_IsRecordEnabled | 34 | add_PropertyChanged(handler) |
| 20 | get_IsFastForwardEnabled | 35 | remove_PropertyChanged(token) |
| 33 | remove_ButtonPressed(token) |
| 34 | add_PropertyChanged(handler) |
| 35 | remove_PropertyChanged(token) |

相关枚举（windows-rs，与官方文档一致）：
- `MediaPlaybackStatus`：Closed=0, Changing=1, Stopped=2, **Playing=3, Paused=4**
- `SoundLevel`：Muted=0, Low=1, Full=2

### 3.c ISystemMediaTransportControls2（IID 与 vtable）

IID：**`EA98D2F6-7F3C-4AF2-A586-72889808EFB1`**。时间轴不在原接口，需对 GetForWindow 返回的指针 **QI 到此接口**后调用：

| 槽位 | 方法 |
|---|---|
| 6/7 | get/put_AutoRepeatMode |
| 8/9 | get/put_ShuffleEnabled |
| 10/11 | get/put_PlaybackRate |
| **12** | **UpdateTimelineProperties(SystemMediaTransportControlsTimelineProperties*)** ← put 时间轴 |
| 13/14 | add/remove_PlaybackPositionChangeRequested（用户拖进度条回调） |
| 15/16 | add/remove_PlaybackRateChangeRequested |
| 17/18 | add/remove_ShuffleEnabledChangeRequested |
| 19/20 | add/remove_AutoRepeatModeChangeRequested |

`MediaPlaybackAutoRepeatMode`：None=0, Track=1, List=2。

### 3.d ISystemMediaTransportControlsDisplayUpdater

IID：**`8ABBC53E-FA55-4ECF-AD8E-C984E5DD1550`**（由 get_DisplayUpdater 返回，无需 QI）：

| 槽位 | 方法 |
|---|---|
| 6/7 | get/put_Type（`MediaPlaybackType`：Unknown=0, **Music=1**, Video=2, Image=3） |
| 8/9 | get/put_AppMediaId（HSTRING，可选） |
| 10/11 | get/put_Thumbnail（IRandomAccessStreamReference，见 3.h） |
| 12 | get_MusicProperties → IMusicDisplayProperties |
| 13 | get_VideoProperties |
| 14 | get_ImageProperties |
| 15 | CopyFromFileAsync |
| 16 | ClearAll |
| **17** | **Update()** ← 元数据提交，**不调用则面板不刷新** |

### 3.e IMusicDisplayProperties（+2）

`IMusicDisplayProperties` IID：**`6BBF0C59-D0A0-4D26-92A0-F978E1D18E7B`**，槽位 6 起：
- 6 get_Title / 7 put_Title(HSTRING)
- 8 get_AlbumArtist / 9 put_AlbumArtist(HSTRING)  ← 注意 AlbumArtist 在 Artist **之前**
- 10 get_Artist / 11 put_Artist(HSTRING)

`IMusicDisplayProperties2` IID：**`00368462-97D3-44B9-B00F-008AFCEFAF18`**（需 QI），槽位 6 起：
- 6/7 get/put_AlbumTitle  ← 专辑名在此接口，需 QI
- 8/9 get/put_TrackNumber
- 10 get_Genres

### 3.f ButtonPressed 事件回调的 JNA 实现要点

- 事件接口：`ITypedEventHandler<SystemMediaTransportControls, SystemMediaTransportControlsButtonPressedEventArgs>`
  官方 IID（MIDL/winmd 生成，mingw-w64 windows.media.h 与 .NET CLR 投影双重验证）：**`0557E996-7B23-5BAE-AA81-EA0D671143A4`**
  （C++/WinRT 运行时按自身签名算法算出的值不同，说明服务器 add 事件不做该 IID 的 QI；稳妥做法是 QI 时同时响应它）
- EventArgs 接口 `ISystemMediaTransportControlsButtonPressedEventArgs` IID：`B7F47116-A56F-4DC8-9E11-92031F4A87C2`，槽位 6 = get_Button → 枚举。
- **SystemMediaTransportControlsButton 完整枚举**（windows-rs/官方 winmd）：
  Play=0, Pause=1, Stop=2, Record=3, FastForward=4, Rewind=5, **Next=6, Previous=7**, ChannelUp=8, ChannelDown=9
- JNA 实现 COM 回调对象的模式（JNA 自带公开案例：`com.sun.jna.platform.win32.COM.DispatchListener`/`DispatchVTable`——Structure 内嵌 vtable Structure，字段为 `Callback` 子接口，构造后 `write()` 固化内存；第三方参考 COM4JNA）：

```kotlin
// vtable: 3 个 IUnknown + 3 个 IInspectable + Invoke
@FieldOrder("QueryInterface", "AddRef", "Release", "GetIids", "GetRuntimeClassName",
            "GetTrustLevel", "Invoke")
class ButtonPressedHandler : Structure() {
    interface Fn : Callback { fun invoke(thisPtr: Pointer, vararg args: Any?): Int }

    var QueryInterface: Fn = qicallback(...);  var AddRef: Fn = ...;  var Release: Fn = ...
    var GetIids: Fn = ...; var GetRuntimeClassName: Fn = ...; var GetTrustLevel: Fn = ...
    var Invoke: Fn = { thisPtr, sender, args ->  // args[1] = ISystemMediaTransportControlsButtonPressedEventArgs*
        val button = readInt32(args, 6 * POINTER_SIZE, /* get_Button slot */ ...)
        onButtonPressed(button); 0 /* S_OK */
    }
}
// 把 Structure 指针作为 handler 传入 add_ButtonPressed（槽位 32）
// QI 须响应：IUnknown、IInspectable、IAgileObject(可选)、IID 0557E996-...
// 引用计数：Release 中保活 Java 对象，直到 remove_ButtonPressed/关闭会话
```

- 注意：JNA `Structure` 首字段应为 vtable 指针（对象指针 → [vtable]），参考 DispatchListener 的 `DispatchVTable.ByReference` 内嵌写法。

### 3.g 时间轴 / 进度条

- `SystemMediaTransportControlsTimelineProperties` 是 **activatable struct**：`RoGetActivationFactory("Windows.Media.SystemMediaTransportControlsTimelineProperties", IID_IActivationFactory)` 后 `ActivateInstance`；其接口 IID `5125316A-C3A2-475B-8507-93534DC88F15`，槽位 6 起严格交替：get/put_StartTime, get/put_EndTime, get/put_MinSeekTime, get/put_MaxSeekTime, get/put_Position。**PlaybackRate 不在其中**（在 Controls2 上）。
- `Windows.Foundation.TimeSpan` ABI = **Int64，单位 100ns**（与 FILETIME 一致）。JNA 侧可直接用 `long` 传参（按值）。
- 提交入口：`ISystemMediaTransportControls2::UpdateTimelineProperties(TimelineProperties*)`（槽位 12，见 3.c）。
- 进度条拖动支持（可选二期）：`ITypedEventHandler<SMTC, PlaybackPositionChangeRequestedEventArgs>` IID `44E34F15-BDC0-50A7-ACE4-39E91FB753F1`；EventArgs IID `B4493F88-EB28-4961-9C14-335E44F3E125`。

### 3.h 封面缩略图（建议二期）

- `IRandomAccessStreamReferenceStatics` IID `857309DC-3FBF-4E7D-986F-EF3B1A07A964`，activation factory 字符串 `"Windows.Storage.Streams.RandomAccessStreamReference"`，槽位 6 起：
  6 `CreateFromFile(IStorageFile*)` → 需要 StorageFile（`StorageFile.GetFileFromPathAsync` 为异步 IAsyncOperation，Win32 下可用但处理麻烦）
  7 `CreateFromUri(Uri*)` → **同步，推荐**：先用 `Windows.Foundation.Uri` factory（`IUriRuntimeClassFactory` IID `44A9796F-723E-4FDF-A218-033E75B0C084`，factory 字符串 `"Windows.Foundation.Uri"`，槽位 6 = CreateUri(HSTRING)）构造 `file:///C:/...` URI，再把结果传给 `put_Thumbnail`（DisplayUpdater 槽位 11）
  8 `CreateFromStream(IRandomAccessStream*)`
- 用户问的 `IReadOnlyRandomAccessStreamReferenceInterop`：**未查到该接口存在**（SDK 与 windows-rs 均无），从文件路径创建无需 interop，`CreateFromUri(file://)` 即可（是否接受本地 file:// **需运行时验证**）。
- **不做封面的影响**：音量面板/任务栏只显示文字（标题/艺术家），无专辑图，按钮功能不受影响。

### 3.i GetForWindow 之后的启用要求

注册后 SMTC 默认不可用，**必须**：
1. `put_IsEnabled(true)`（槽位 11）；
2. 逐个 `put_IsPlayEnabled(true)`（13）、`put_IsPauseEnabled(true)`（17）、`put_IsNextEnabled(true)`（27）、`put_IsPreviousEnabled(true)`（25）等（官方文档 integrate-with-systemmediatransportcontrols 要求设置按钮与 PlaybackStatus 后才有按键响应）；
3. 元数据设置后调 `DisplayUpdater.Update()`；播放状态变化时更新 `put_PlaybackStatus`。

### 3.j 已知坑（部分条目未逐一查证，标注）

1. 元数据/时间轴修改后**忘记 Update()/UpdateTimelineProperties() 则 UI 不刷新**（官方文档明确）。
2. **COM apartment 冲突**：JVM 主线程可能已被其他 JNI 组件初始化过 COM（VLCJ、AWT、jna-platform 某些调用），RoInitialize 返回 RPC_E_CHANGED_MODE/S_FALSE 需容忍；建议专用线程 + MTA。
3. **事件回调线程**：ButtonPressed 在系统 RPC 线程池线程触发，回调内不可长时间阻塞，需转投 Compose Main UIDispatcher 更新 UI（`Structure` 中 Callback 由 JNA 原生线程调用）。
4. **进程退出**：应先 `put_IsEnabled(false)`/释放引用让会话消失；jpackage 强杀可能残留灰色条目（**未查到官方说明，需实测**）。
5. **窗口生命周期**：Compose 全屏/最小化不影响会话；但 HWND 变化（重建窗口）需重新 GetForWindow（Compose Desktop 窗口重建时机**需实测**）。
6. SoundLevel=Muted（系统静音）时面板可能自动暂停显示（**未查证，需实测**）。
7. Handler 对象必须保活（Java 强引用），否则 native 回调悬挂 → JVM crash；remove_ButtonPressed 后方可释放。
8. HSTRING 均需 WindowsDeleteString 释放（传入参数字符串由调用方创建）。

## 4. 备选方案评估：PowerShell/外部进程注册 SMTC

**结论：不可行。**
- `GetForWindow` 官方要求 `appWindow` 必须是**调用进程自己的顶层窗口**（learn.microsoft.com GetForWindow Remarks）；
- SMTC 会话归属于注册它的进程；由 PowerShell 进程注册只会显示 PowerShell 自身的媒体条目（还要求 PowerShell 进程有可用 CoreWindow/窗口对象，PowerShell 无 GUI 窗口，GetForWindow 无法满足；GetForCurrentView 在无 CoreWindow 进程直接失败）；
- 借助外部进程劫持/注入 muses 窗口来注册不属于本方案的稳妥路径，且引入持久外部进程依赖与权限问题。

## 5. 推荐方案与 MVP 范围

**推荐顺序：纯 JNA 手写（首选）> JMTC（不推荐，仅参考）> kotlin-winrt（观察）**

理由：
- 需求面窄：1 次 RoGetActivationFactory + 1 次 GetForWindow + 3 个接口调用集 + 1 个回调 COM 对象；本文已提供全部 IID 与 vtable 顺序（主要来源为官方 winmd 生成物 windows-rs 与本地 SDK IDL），实现风险集中在"回调对象 vtable 布局"一处，且有 JNA DispatchListener 官方案例可依葫芦画瓢；
- 零新增二进制依赖（复用 :desktop 现有 jna/jna-platform 5.17.0），无需分发 DLL，与 jpackage 打包流程无冲突；
- JMTC 需带 native DLL、会话不绑窗口、两年未更新；kotlin-winrt 需 JDK 25 FFM 且无正式版。

**MVP（首版做）**：
1. `SmtcSession`：专用 MTA 线程 + RoInitialize + RoGetActivationFactory("Windows.Media.SystemMediaTransportControls", IID_Interop) + GetForWindow(hwnd)；
2. 启用：put_IsEnabled(true) + Play/Pause/Next/Previous 四键 put_IsXxxEnabled；
3. ButtonPressed 回调 → 回调线程安全地转投 Compose Main 线程（支持 Play=0/Pause=1/Stop=2/Next=6/Previous=7）；
4. put_PlaybackStatus(Playing/Paused/Stopped)；
5. DisplayUpdater：put_Type(Music)、put_Title/put_Artist/put_AlbumTitle(IMusicDisplayProperties2)/put_AlbumArtist + Update()；
6. 时间轴节流更新（如 ≥1s 或 ≥5% 进度变化）经 ISystemMediaTransportControls2::UpdateTimelineProperties；
7. 关闭钩子：put_IsEnabled(false) + remove 事件 + Release。

**降级（二期）**：
- 封面缩略图（CreateFromUri(file://) 或不做，仅文字）；
- Seek（PlaybackPositionChangeRequested）；
- Shuffle/AutoRepeat/PlaybackRate；
- PropertyChanged 监听。

**需运行时验证清单**：HSTRING 经 WindowsCreateString 的 JNA 封装；CreateFromUri 是否接受本地 file://；Compose Desktop 顶层 HWND 获取（ComposeWindow → awt Window peer HWND）；回调对象 QI 响应集合；进程强杀后 SMTC 残留行为。

## 6. 来源清单

官方文档（learn.microsoft.com）：
- ISystemMediaTransportControlsInterop 接口：https://learn.microsoft.com/en-us/windows/win32/api/systemmediatransportcontrolsinterop/nn-systemmediatransportcontrolsinterop-isystemmediatransportcontrolsinterop
- GetForWindow：https://learn.microsoft.com/en-us/windows/win32/api/systemmediatransportcontrolsinterop/nf-systemmediatransportcontrolsinterop-isystemmediatransportcontrolsinterop-getforwindow
- RoGetActivationFactory：https://learn.microsoft.com/en-us/windows/win32/api/roapi/nf-roapi-rogetactivationfactory
- RoInitialize：https://learn.microsoft.com/en-us/windows/win32/api/roapi/nf-roapi-roinitialize
- RO_INIT_TYPE：https://learn.microsoft.com/en-us/windows/win32/api/roapi/ne-roapi-ro_init_type
- SystemMediaTransportControls（UWP 参考）：https://learn.microsoft.com/en-us/uwp/api/windows.media.systemmediatransportcontrols
- SMTC 集成指南：https://learn.microsoft.com/en-us/windows/apps/develop/media-playback/integrate-with-systemmediatransportcontrols

本地验证（权威 ABI 数据）：
- Windows SDK 10.0.26100.0：`um/SystemMediaTransportControlsInterop.idl`（interop IID + 签名）、`cppwinrt/winrt/impl/windows.media.*.h`（接口层次：SystemMediaTransportControls 实现 ISystemMediaTransportControls + ISystemMediaTransportControls2）

第三方验证（由官方 winmd/IDL 生成）：
- microsoft/windows-rs `Windows/Media/mod.rs`、`Windows/Storage/Streams/mod.rs`、`Windows/Foundation/mod.rs`（所有 WinRT 接口 IID 与 vtable 顺序、枚举值、TimelineProperties）：https://github.com/microsoft/windows-rs
- mingw-w64 `mingw-w64-headers/include/windows.media.h`（ITypedEventHandler<SMTC,ButtonPressedEventArgs>=0557E996-7B23-5BAE-AA81-EA0D671143A4、ITypedEventHandler<SMTC,PlaybackPositionChangeRequestedEventArgs>=44E34F15-...）：https://github.com/mingw-w64/mingw-w64

库与案例：
- JMTC：https://github.com/Selemba1000/JavaMediaTransportControls ；Maven metadata：https://repo1.maven.org/maven2/io/github/selemba1000/JavaMediaTransportControls/maven-metadata.xml
- kotlin-winrt：https://github.com/compose-fluent/kotlin-winrt
- JNA DispatchListener（vtable 回调案例）：https://github.com/java-native-access/jna/blob/master/contrib/platform/src/com/sun/jna/platform/win32/COM/DispatchListener.java
- JNA Ole32（无 Ro* 封装）：https://github.com/java-native-access/jna/blob/master/contrib/platform/src/com/sun/jna/platform/win32/Ole32.java
- COM4JNA（JNA 直调 COM vtable 参考）：https://github.com/java-native-access/jna-users 讨论串 https://groups.google.com/g/jna-users/c/Ts6jY79hJ_c

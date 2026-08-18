# Design — 蓝牙/车机断开时暂停播放（音频输出设备移除检测）

## 1. 现状与根因

- 蓝牙耳机断开时能暂停/停止，靠的是**系统音频焦点机制**：capgo `NativeAudio` 实现 `OnAudioFocusChangeListener`，`AUDIOFOCUS_LOSS` → stop、`AUDIOFOCUS_LOSS_TRANSIENT` → pause。
- CarWith 车机断开时，系统**不发送音频焦点变化**（音频路由不强制切回、焦点不回收），capgo 焦点链路不触发 → 播放继续。
- 项目自有 `AudioPlayerPlugin.kt` 无任何音频设备监听。

**结论**：实现「断开即暂停」不能依赖系统焦点事件，须在 App 原生层主动监听**音频输出设备移除**（`AudioManager.registerAudioDeviceCallback`，API 23+，minSdk 24 ✓，无需新增权限/manifest）。

## 2. 总览

```
蓝牙耳机 / 蓝牙音箱 / 有线耳机 / USB 音频 / CarWith 车机（USB 音频）
        │ 拔出 / 断开
        ▼
AudioPlayerPlugin.kt  AudioDeviceCallback.onAudioDevicesRemoved（主线程注册）
        │ 过滤：isSink && type ∈ 输出设备集合 && jsExpectedPlaying == true
        ▼
500ms 去抖（合并瞬时多设备事件）→ executeDeviceRemovalPause()
        │ 1) jsExpectedPlaying = false（防预案 2.5s 后误触发自动播下一首）
        │ 2) jsCurrentAssetId 非空 → callNativeAudio("pause", assetId)
        ▼
capgo NativeAudio.pause ── playbackState(paused) 事件 ──► 前端 native.ts → controller
        （通知保留、状态同步、会话持久化全部走既有链路，无需新通道）
```

## 3. 原生改动（`AudioPlayerPlugin.kt`）

### 3.1 设备移除判定（纯函数，可单测）

提取为 companion object 纯逻辑，输入抽象数据（不依赖 Android 类，便于 JUnit）：

```kotlin
companion object {
    /** 输出设备中「拔出即应暂停」的类型集合 */
    val DISRUPTIVE_OUTPUT_TYPES = setOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,       // 蓝牙耳机/音响
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,        // 蓝牙通话耳麦
        AudioDeviceInfo.TYPE_WIRED_HEADSET,        // 带麦有线耳机
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,     // 有线耳机
        AudioDeviceInfo.TYPE_USB_DEVICE,           // USB DAC / 车机 USB 音频
        AudioDeviceInfo.TYPE_USB_HEADSET,          // USB 耳机
        AudioDeviceInfo.TYPE_USB_ACCESSORY,        // USB 外设音频
        AudioDeviceInfo.TYPE_DOCK,                 // API 26+ 底座/车机底座
    )

    /** 移除的设备中是否包含「应暂停」的输出设备 */
    fun isDisruptiveDeviceRemoved(removed: Iterable<RemovedOutputDevice>): Boolean =
        removed.any { it.isSink && it.type in DISRUPTIVE_OUTPUT_TYPES }
}
```

`RemovedOutputDevice` 为本插件私有数据类（`type: Int, isSink: Boolean`），由回调适配（Android 侧 `AudioDeviceInfo` → 数据类）；测试用数据类直测判定逻辑。`TYPE_DOCK` 常量 API 26+，minSdk 24——常量引用需 `if (Build.VERSION.SDK_INT >= 26)` 保护或直接引用（int 常量在较新编译下可用，运行时老系统不回调该类型）。为稳妥用 `Build.VERSION` 条件把 `TYPE_DOCK` 加入集合。

### 3.2 回调注册

```kotlin
private var deviceCallbackRegistered = false
private var pendingRemovalPauseTask: Runnable? = null

private val removalDebounceHandler = Handler(Looper.getMainLooper())

private val audioDeviceCallback = object : AudioDeviceCallback() {
    override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
        try {
            val simplified = removedDevices.map { RemovedOutputDevice(type = it.type, isSink = it.isSink) }
            if (!isDisruptiveDeviceRemoved(simplified)) return
            if (!jsExpectedPlaying) return          // 非播放中不动作
            pendingRemovalPauseTask?.let { removalDebounceHandler.removeCallbacks(it) }
            pendingRemovalPauseTask = Runnable {
                pendingRemovalPauseTask = null
                executeDeviceRemovalPause()
            }
            removalDebounceHandler.postDelayed(pendingRemovalPauseTask!!, DEVICE_REMOVAL_PAUSE_DEBOUNCE_MS)
        } catch (_: Exception) {
            // 回调异常静默
        }
    }
}
```

`load()` 中注册（进程存活期间常驻）：

```kotlin
private fun registerDeviceRemovalCallback() {
    if (deviceCallbackRegistered) return
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
    runCatching {
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, removalDebounceHandler)
        deviceCallbackRegistered = true
    }
}
```

### 3.3 执行暂停

```kotlin
private const val DEVICE_REMOVAL_PAUSE_DEBOUNCE_MS = 500L

private fun executeDeviceRemovalPause() {
    // 关键：先置 false 阻断预案。否则 pause 后 isMusicActive 转 false，
    // `tickAutoNext` 会在 2.5s 防抖后自动播放下一首，静音输出继续"播放"。
    jsExpectedPlaying = false
    pendingAutoNextAt = 0L

    val assetId = jsCurrentAssetId ?: autoNextPlan?.currentAssetId
    if (assetId.isNullOrBlank()) return
    callNativeAudio("pause", JSObject().put("assetId", assetId))
}
```

### 3.4 JS 上报扩展：`reportPlaybackStatus` 携带 assetId

原生层记录最近一次 JS 上报的当前 assetId（capgo `pause` 必须带 assetId 参数）：

```kotlin
private var jsCurrentAssetId: String? = null

@PluginMethod
fun reportPlaybackStatus(call: PluginCall) {
    val status = call.getString("status")
    jsExpectedPlaying = status == "playing"
    jsCurrentAssetId = call.getString("assetId") ?: jsCurrentAssetId
    if (!jsExpectedPlaying) pendingAutoNextAt = 0L
    call.resolve()
}
```

fallback 链：`jsCurrentAssetId` → `autoNextPlan.currentAssetId`（JS 冻结前最后一次上报/预案里的当前曲）。

## 4. 前端改动（`src/features/player/native.ts`）

`reportBridgePlaybackStatus` 上报时附带当前 assetId（仅一行），向后兼容（旧 APK 原生忽略）：

```ts
const reportBridgePlaybackStatus = (status: 'playing' | 'paused' | 'stopped'): void => {
  if (!AudioPlayerBridge.reportPlaybackStatus) return
  void AudioPlayerBridge.reportPlaybackStatus({ status, assetId: currentAssetId ?? undefined }).catch(() => undefined)
}
```

接口 `AudioPlayerPermissionBridge.reportPlaybackStatus` 增加 `assetId?: string`。

JS 状态同步**零改动**：capgo `pause` → `playbackState(paused)` → `native.ts` → `controller.applyNativeState` → paused（通知/会话持久化走既有链路）。恢复播放由用户手动（通知按钮/前台 UI），走既有 `resumePlayback`。

## 5. 行为对照与边界

| 场景 | 行为 |
|---|---|
| 播放中拔蓝牙 A2DP | 500ms 内去抖 → 暂停（通知保留） |
| 播放中断开 CarWith（USB 音频/车载蓝牙） | 同上（USB/蓝牙设备移除事件） |
| 播放中拔有线耳机 | 同上 |
| 已暂停时拔设备 | `jsExpectedPlaying=false` → 无动作 |
| 拔出-插入顺序颠倒/多设备同断 | 去抖合并为一次 pause |
| 播放中插设备/切到扬声器 | 不动作（onDevicesAdded 不处理） |
| 来电/铃声/语音助手（焦点打断不涉及设备移除） | 不动作（走 capgo focus 路径） |
| 设备移除后重连 | 不自动恢复（保持暂停，用户手动恢复） |
| 前端 JS 冻结 | 原生层独立执行 pause，JS 醒来经 playbackState 事件对账 |

风险：拔耳机瞬间 `AudioManager` 可能短暂把路由切到扬声器再暂停——pause 在 500ms 去抖后执行，输出已静音，无"功放漏音"窗口；若 500ms 窗口内用户切回扬声器想继续听（边缘场景），仍是暂停——符合"断开即暂停"语义，可接受。

## 6. 文件变更清单

| 文件 | 变更 |
|---|---|
| `android/app/src/main/java/com/muses/player/AudioPlayerPlugin.kt` | AudioDeviceCallback 注册 + 纯判定函数 + 去抖暂停 + reportPlaybackStatus 扩展 assetId |
| `android/app/src/test/java/com/muses/player/AudioDeviceRemovalPolicyTest.kt` | **新增**：判定逻辑 JUnit 单测（蓝牙/有线/USB/sink 过滤/非输出设备） |
| `src/features/player/native.ts` | reportBridgePlaybackStatus 携带 assetId（接口 + 实现） |
| `.trellis/spec/frontend/features-player.md` | 记录「音频输出设备移除→暂停」机制 |
| 无 manifest / 无权限 / 无 node_modules 改动 | — |

## 7. 验证计划

### 自动化
- `./gradlew :app:testDebugUnitTest`（新增判定单测绿）
- `./gradlew :app:assembleDebug`（编译绿，产出 app-debug.apk）
- `npm run lint` / `npm run build`（native.ts 改动无回归）

### 真机（小米 15）
| # | 步骤 | 预期 |
|---|---|---|
| D1 | 本地源播放中拔蓝牙耳机 | 暂停、通知保留、可恢复 |
| D2 | WebDAV 源播放中拔蓝牙耳机 | 暂停 |
| D3 | CarWith 连接播放中断开 | 暂停 |
| D4 | 已暂停时拔蓝牙 | 无状态变化（仍暂停） |
| D5 | 拔后重连 | 不自动恢复，手动播放正常 |
| D6 | 播放中手机扬声器/切换设备 | 不误暂停 |
| D7 | 通知上恢复播放 | 正常恢复 |

## 8. 回滚

- 新增逻辑集中在 AudioPlayerPlugin 单类 + native.ts 单函数；各自单文件 revert 即回滚。
- 与 08-18-carwith-bg-ctrl-fix 无共享代码路径（keepalive 无关），互不影响回滚。
# 蓝牙/车机断开时暂停播放——音频输出设备移除检测

## Goal

播放中拔出蓝牙耳机 / 断开 CarWith 车机后，当前播放应立即**暂停**（保留媒体通知与当前曲，可一键恢复），而不是继续在手机扬声器/无声输出上播放。

现状：蓝牙耳机断开时能暂停/停止，是因为系统焦点机制触发 `@capgo/capacitor-native-audio` 的 `OnAudioFocusChangeListener`；CarWith 断开时系统**不发**焦点变化，播放继续。本任务在 App 原生层主动监听**音频输出设备移除**，补齐这一行为，不依赖系统焦点事件。

## Requirements

- R1：播放中拔出蓝牙 A2DP 音频设备（耳机/车载蓝牙）→ 暂停播放，保留通知与当前曲。
- R2：播放中断开 CarWith 车机（含 USB 音频 / 车载蓝牙音频）→ 暂停播放。
- R3：拔出有线耳机（3.5mm / Type-C）→ 同样暂停。
- R4：暂停后手动播放/恢复正常；不因设备移除自动恢复。
- R5：非播放状态（已暂停/停止）时设备移除不产生副作用。
- R6：来电、通知铃声、语音助手等**短暂焦点打断**（不涉及设备移除）不得触发暂停（沿用现状 focus 路径 AUTO 处理）。

## 决策（默认值，review gate 确认）

- **D1 行为 = 暂停（非停止）**：保留通知、保留当前曲与进度，用户可一键恢复。对齐「断开就暂停播放」字面语义。
- **D2 范围 = 蓝牙 A2DP + 有线耳机 + USB 音频设备移除** 全量覆盖（`TYPE_BLUETOOTH_A2DP` / `TYPE_BLUETOOTH_SCO` / `TYPE_WIRED_HEADSET` / `TYPE_WIRED_HEADPHONES` / `TYPE_USB_DEVICE` / `TYPE_USB_HEADSET` / `TYPE_USB_ACCESSORY`）。
- **D3 真实现状（蓝牙耳机断开是暂停还是停止）未确认**：不阻塞实现——本功能独立于系统行为；真机验证时一并观察。

## Acceptance Criteria

- [x] 本地源播放中拔出蓝牙耳机 → 通知仍在、播放暂停、按钮可恢复。
- [x] WebDAV 源播放中拔出蓝牙耳机 → 暂停（同上）。
- [x] CarWith 连接播放中拔线/断开 → 暂停。
- [x] 暂停状态拔出设备 → 无状态抖动（仍暂停）。
- [x] 拔出后重新连接设备 → **不**自动恢复播放（保持暂停，用户手动恢复）。
- [x] 播放中插入设备、切换设备（蓝牙→扬声器切换）不误暂停。
- [x] 暂停/恢复/切歌/seek/自动切歌（含 CarWith 场景，配合任务 08-18-carwith-bg-ctrl-fix）不回归。
- [x] lint / `vue-tsc` build / `gradle assembleDebug` 全绿。

## 验证记录（2026-08-18 真机，小米 15 + 蓝牙耳机 + CarWith）

用户实测通过：播放中拔蓝牙耳机/**断开 CarWith**均立即暂停且通知保留、可恢复；已暂停拔出无抖动；重连不自动恢复；切换设备不误暂停；与 CarWith 自动切歌修复配合无回归。

## Constraints

- Android 原生层实现（`AudioManager.registerAudioDeviceCallback`，API 23+，无需新运行时权限、无需新增 manifest 权限）。
- 遵守 `.trellis/spec/frontend/features-player.md`：原生改动集中在项目自有 `AudioPlayerPlugin.kt`；JS 状态同步走既有事件链路；不改 `node_modules/@capgo/*`、不改其配置。
- 暂停执行走 capgo（callNativeAudio pause），JS 侧通过既有 playbackState 事件自动同步为 paused，不另造同步通道。
- 防误触发：设备移除后留短确认窗口（~500ms）合并瞬时事件（拔出-插入顺序颠倒/多设备同时断开）。

## Notes

- 验证需真机：小米 15 + 蓝牙耳机 + CarWith。
- 与 08-18-carwith-bg-ctrl-fix（保活 JS）为互补任务：若保活使 JS 活跃，媒体通知按钮正常；本任务保证断开即暂停在原生层兜底，不依赖 JS 存活。
# 设计：进度条播放时不随时间更新 (#47)

## 问题分析

两条独立故障面，叠加后符合 issue 描述：

### A. 原生进度事件可能未持续驱动 `playerState.position`

链路：Capgo `currentTime`（约 100ms）→ `native.ts` → `stateChange` → `controller.applyNativeState` → `playerState.position`。

风险点：

1. 仅依赖插件 timer；若 `isPlaying()` 瞬时为 false、timer 停掉、或事件丢包，前端 `position` 停更。
2. `setCurrentTime`（seek）不保证重启 timer；seek 后若 timer 已停，会停在 seek 目标（「能跳转但之后不动」）。
3. 规范禁止改 `node_modules/@capgo/*`，兜底必须在 `native.ts` / controller。

### B. `ion-range` 受控 value 触发伪 `ionInput`，冻结填充

Ionic `ion-range` 在 `value` 属性变化时 `valueChanged` 会 **emit `ionInput`**。

`PlayerPage` 的 `onSeekInput` 会：

- `lockSeekGesture()`
- 写入 `seekPreviewPosition`

而 `effectiveSeekPosition = seekPreviewPosition ?? playerState.position`。

一旦首帧 programmatic `ionInput` 写入 preview，后续 `playerState.position` 增长也不再驱动 `:value`，进度条视觉卡住。时间行若仍绑 `playerState.position`，可能「条不动、时间还在动」；若 A 同时发生则两者都停。

## 方案

### 1. `native.ts`：playing 期间位置轮询兜底

- `play` / `resume` 成功且 status=`playing` 后启动 interval（建议 250ms）。
- 轮询 `NativeAudio.getCurrentTime` + 可选 `isPlaying`；更新 `currentPosition` 并 `emitCurrentState`。
- `pause` / `stop` / `unload` / 切歌前停止轮询。
- 与 `currentTime` 事件并存：取新值写回即可；不要求严格单调（seek 允许回退）。
- **不改** capgo 源码。

### 2. `PlayerPage`：仅用户手势写 preview

- `seekPreviewPosition` 仅在进度条手势锁已开启（`seekGestureLocked` / progress pointerdown）时由 `onSeekInput` 写入。
- 忽略 `value` 绑定引起的 programmatic `ionInput`。
- `ionChange` / seek 完成仍清空 preview。

### 3. 归一化（次要）

- `normalizePlaybackTime` 改为 `>= 0` 且有限（避免语义含混）；行为与「非法→0」兼容。

## 边界

| 场景 | 行为 |
|------|------|
| 正常播放 | 事件或轮询驱动 position；条与时间前进 |
| seek 成功后仍 playing | 从目标继续前进 |
| pause | 停轮询；position 保持 |
| 无 currentAsset | 不轮询 |
| 缓冲拒绝 seek | 保持现语义 |

## 测试

- 单测：mock `getCurrentTime` 递增，断言 `playerState.position` 在 playing 下更新（若可注入 fake timers）。
- 单测：PlayerPage 在非手势下 value 变化不残留 `seekPreview`（若可测）。
- 回归：现有 seek / finished / ion-range 用例。

## 非目标

- MiniPlayer 进度条
- 改 capgo 插件
- #49 持久化

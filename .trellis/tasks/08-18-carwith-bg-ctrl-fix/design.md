# Design — CarWith 后台播放修复（方案 A）

## 1. 现状与目标

CarWith 连接后 WebView 页面 hidden，Chromium 冻结/深度节流 JS：

- **complete 事件**（原生 → `WebView.evaluateJavascript`）与**媒体按钮命令**（通知 PendingIntent → MediaButtonReceiver → MediaSessionCompat callback → `actionCallback` resolve 给 JS keepAlive handler）都依赖 WebView JS 执行；
- **播完不切**：JS 冻结收不到 complete；且 JS 晚处理 complete 时 `shouldIgnoreFinished` 拿冻结的旧 `state.position` 误判「未接近结尾」→ 置 `paused`；
- **按钮失效**：命令 resolve 到 JS 后无人处理。

目标（方案 A，用户确认）：**让 CarWith 场景下 WebView JS 不被冻结**（源头保活），并修复 finished 误判这一确定性缺陷；既有原生预案/心跳/对账保持为兜底。**不改 node_modules/@capgo/*、不改 manifest、不加原生直控。**

## 2. 总览

```
┌─ CarWith 连接（屏幕关/锁屏）───────────────┐
│  WebView JS ◄──── keepalive（静音 Web Audio）─ 阻止 Chromium 冻结
│    · complete 事件 → finished → 自然播完判定(修复后不再误判) → playSong(next)
│    · 媒体按钮命令 → playNextFromQueue / playPreviousFromQueue / resume / pause
│    · 心跳(1s)→getState 兜底（hidden 时被节流至约1次/分钟）
└───────────────────────────────────────────┘
原生兜底（保持现状）：
  · AudioPlayerPlugin.tickAutoNext：isMusicActive + jsExpectedPlaying + 2.5s 防抖
  · 回前台 reconcileAfterBackground 对账
```

Play 状态时 keepalive 持续运行 → JS 保持活跃 → 完整链路在 JS 内闭环，预案只充当保活失败的最后防线。

## 3. 改动一：WebView JS 保活模块（新增 `src/features/player/keepalive.ts`）

### 3.1 原理

Chromium 对 hidden 页面的冻结（Page Lifecycle freeze）在「页面有进行中的媒体活动」时不执行。播放一个 **gain=0 的常驻 Web Audio 轨**（`AudioContext` + `ConstantSourceNode` → `GainNode(0)` → `destination`）即可让页面携带「ongoing media」标签，阻止冻结；同时输出静音，不影响原生播放。

### 3.2 API（keepalive.ts 导出）

```ts
export const startKeepAlive(): void   // 幂等；已有轨则 resume
export const stopKeepAlive(): void    // 幂等；停轨、断开，保留 ctx 复用
```

内部：
- 懒创建 `AudioContext`（带 `webkitAudioContext` 兼容）；`ctx.state==='suspended'` 时尝试 `ctx.resume()`（失败静默，等下一次用户手势窗口内再试）。
- 轨结构：`ConstantSourceNode.start()`（0Hz/常量 0 → gain 0 → destination）。
- 运行标志 `running` 防重复；任何异常 catch 后静默降级（不抛、不打断播放）。
- 调试：`localStorage muses:debug-keepalive` 输出日志（对齐 `muses:debug-native-audio` 风格，默认静默）。

### 3.3 生命周期挂接（controller.ts）

| 事件 | 动作 | 理由 |
|---|---|---|
| `playSongInternal` 成功（status 置 playing） | `startKeepAlive()` | 播放中保活，切歌不断（下一首选完仍 playing） |
| `resumePlayback` 成功 | `startKeepAlive()` | 恢复播放 |
| `pausePlayback` | `stopKeepAlive()` | 暂停无音频输出，无需保活，省电 |
| `stopPlayback` | `stopKeepAlive()` | 停止清场 |
| 播放失败链终止 | `stopKeepAlive()` | 不再播放 |
| `initializePlayer` 冷启动恢复会话（restore） | 保持 paused，不启动 | 恢复为暂停展示，用户点播放时才启动 |

只在 **Android 原生平台**启用（`Capacitor.getPlatform()==='android'`）；web/iOS 不运行。

### 3.4 手势窗口（autoplay policy）

Android WebView 默认可能要求用户手势解锁 AudioContext。启动点在 `playSongInternal` 链路（由按钮/通知媒体键触发的用户手势栈内），若仍 suspended，则：
1. 立即尝试 `resume()`（捕获 `NotAllowedError`）；
2. 失败不重试，但每次后续媒体操作（play/pause 之外的用户交互）仍会调用 `startKeepAlive`/`ensureResume`，均静默重试；
3. 不引入延时或阻塞播放的等待。

### 3.5 风险与缓解

| 风险 | 缓解 |
|---|---|
| WebAudio 常驻改变 `AudioManager.isMusicActive`（恒 true） | 预案触发依赖 isMusicActive 的**静音跳变**：保活生效期间 JS 本就不冻结，预案不需要触发；仅「保活失败 + JS 已冻结」时 isMusicActive 才可能失真 → 预案失效 → 此时由心跳/回前台对账兜底。行为可接受，验证项记录 |
| 系统出现第二个「正在播放」声音 | gain=0 数字静音，无实际输出；系统媒体卡片由 media-session 单独管理，WebAudio 不注册 MediaSession，不会重复出卡片 |
| 功耗 | 仅播放中运行；ConstantSource 极低开销；暂停即停 |
| Chromium 仍因进程级冻结（MIUI 智能冻结）挂起 JS | 超出本任务边界：进程级冻结时任何 JS/预案/心跳全部无效，PRD 已声明 out of scope |
| fake 音频会话触发系统音量条/语音助手误判 | 低；验证项记录（AC：音量条/媒体卡片正常） |

## 4. 改动二：finished 判定修复（`controller.ts`）

### 4.1 现状缺陷

```ts
const shouldIgnoreFinished = (position, duration) =>
  isWithinSeekGuard() || !isNearNaturalEnd(position, duration)
```

`isNearNaturalEnd` 要求 `position >= duration - 1.25`。JS 冻结期间 `state.position` 停在旧值（远离结尾），complete 晚到后 `effectivePosition = max(nativePosition(=0), 冻结position)` 大概率不满足 near-end → 误判「伪 finished」→ 置 `paused`（播完暂停的直接原因之一）。

### 4.2 修改

complete/finished 的**唯一合法来源是播放器播完**（本地 SoundPool/ExoPlayer 的 complete/STATE_ENDED），不再需要 position 佐证自然结束：

```ts
// finished 分支内：
if (isWithinSeekGuard()) { /* 仅 seek 后 1.5s 内的 finished 视为伪结束：恢复 seek 目标进度 */ }
else { /* 无条件 treat as 自然播完 → handlePlaybackFinished() */ }
```

- 删除 `shouldIgnoreFinished` 的 near-end 部分（及不再使用的 `isNearNaturalEnd`）。
- **保留**：seek 保护窗（1.5s，`lastSeekAt`）；`resumeSeekGuard`（冷启动恢复 #53）逻辑不动；`duration=0` 保守分支删除（complete 本身即播完信号，与 duration 无关）。
- **保留**外层的 `seekPlayback` 源头限制（≤ bufferedPosition，R2/R3）+ seek 保护窗，双保险丢弃「seek 到未缓冲区的伪 complete」。

### 4.3 行为对照（回归面）

| 场景 | 旧行为 | 新行为 |
|---|---|---|
| 正常播完（前台/后台） | 若 position 新鲜且 near-end → 切歌 | 切歌（同） |
| 正常播完但 position 冻结（CarWith） | **误判伪 finished → 暂停** | **切歌（修复）** |
| seek 到未缓冲区触发的伪 complete（1.5s 内） | 忽略 | 忽略（同） |
| 暂停后 seek 恢复再播到尾 | 切歌 | 切歌（同，seek guard 已过） |
| duration 未知时播完 | 不切歌（保守） | 切歌（complete 即权威） |

capgo complete 语义确认：`AudioAsset`（SoundPool setOnCompletionListener）/ `RemoteAudioAsset`（ExoPlayer STATE_ENDED）均为真正播完；pause/unload 不触发 complete。assetId 过滤已存在，不会串曲。

## 5. 改动三：原生预案判定（调整：**不做**，注明技术边界）

原拟把预案「播完判定」从 `isMusicActive()` 换成 capgo `isPlaying`。**调研结论：在 A 边界内不可行**——capgo 插件方法返回值经 `PluginCall.resolve()` 异步发往 WebView，无插件间同步返回值通道（`CALLBACK_ID_DANGLING` 时直接丢弃）；反射读取其私有播放器状态属「侵入第三方内部」，升级即碎，与「不改第三方」边界冲突。

**决定**：预案 `AudioPlayerPlugin.tickAutoNext` **保持现状**，角色明确为「保活失效时的最后防线」；CarWith 下是否触发由真机验证判定（见 §7 验证项 V3）。若验证确认 isMusicActive 在 CarWith 下失真且保活失效，升级路径 = 方案 B（另开任务）。

## 6. 文件变更清单

| 文件 | 变更 |
|---|---|
| `src/features/player/keepalive.ts` | **新增**：WebAudio 保活模块 |
| `src/features/player/controller.ts` | finished 判定修复 + 生命周期挂接 startKeepAlive/stopKeepAlive |
| `.trellis/spec/frontend/features-player.md` | 记录 keepalive 机制与 finished 判定语义变更 |
| 无 Android 原生/无 manifest/无 node_modules 改动 | — |

## 7. 真机验证计划（小米 15 + CarWith）

| # | 步骤 | 预期 |
|---|---|---|
| V1 | 连接 CarWith，播放队列（本地源）2 首，屏幕关闭 | 第 1 首播完自动切第 2 首 |
| V2 | 同上，WebDAV 源 | 播完自动切（含弹缓冲/直链两路径） |
| V3 | CarWith 下展开手机媒体通知 | 上一曲/下一曲/播放/暂停均响应；若仍失效 → 判定保活是否生效（V5） |
| V4 | CarWith 下通知点下一曲 ×N 快速切歌 | 每一次都切换且无串曲/无崩 |
| V5 | 保活生效性观测：CarWith 下播放中手机无操作 2 分钟，日志显示 keepalive 轨 running（debug 开关） | running 且 JS 心跳（getState）每次间隔 <5s（不被节流到 1 分钟） |
| V6 | 普通锁屏后台（不连 CarWith）：播完自动切 | 预案/心跳兜底不回归 |
| V7 | 前台：切歌/暂停/seek/单曲/列表/随机 | 无回归 |
| V8 | 音量条/系统媒体卡片 | 无第二个媒体卡片；音量条正常 |

## 8. 验收与回滚

- 验收 = PRD Acceptance Criteria 全数通过 + V1-V8 无回归项。
- 回滚点：每个改动独立提交；keepalive 为独立模块，可单文件 revert；finished 判定改动单提交 revert，恢复原判定。
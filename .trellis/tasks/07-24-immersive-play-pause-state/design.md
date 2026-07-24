# 设计：沉浸式二次播放/暂停状态丢失（#52）

## 问题边界

| 层 | 职责 |
| --- | --- |
| UI | 只读 `playerState.currentSong` / `status`；调用 `pausePlayback` / `resumePlayback` |
| controller | 权威 UI 状态；决定 resume 走轻量 `resume` 还是 #49 的 `play+seek`；过滤陈旧 native 事件 |
| native.ts | asset 生命周期、`stateChange` 映射 |
| 原生插件 | 真实音频；可能异步上报 stop/unload |

## 根因假设（按优先级）

1. **主因**：`resumePlayback` 把「任意 paused」都当成 #49 无 asset，走 `playSongInternal` → unload/stop 异步事件 + 状态窗口竞态 → `applyNativeState` 清空 `currentSong`，音频已由新 play 播出。
2. **放大因素**：`applyNativeState` 对匹配 songId 的 `stopped`/`idle` 无条件清空当前曲（仅 `restoredSessionUiOnly` 例外），无法区分「用户 stop / 播完」与「切歌/重载过程中的瞬时 stop」。

## 目标行为

```
playing --pause--> paused --resume--> playing
         \ 不丢 currentSong / 不整曲 reload（有 asset 时）

#49 only-UI:
paused(restoredSessionUiOnly) --resume--> play(+seek) --> playing
```

## 方案

### A. 收紧 `resumePlayback`（必做）

仅在以下情况走 `playSongInternal`：

- `restoredSessionUiOnly === true`，或
- 原生侧确认无当前 asset（若可从 native 暴露/推断；否则以 flag 为准）

否则：

```ts
await AudioPlayerNative.resume()
state.status = 'playing'
// 失败时再回退 play+seek（保留现有 catch 回退）
```

普通 pause 后不得写 `pendingResumePosition` 仅为了触发整曲 play（除非走 #49 路径）。

### B. 收紧 `applyNativeState` 对 idle/stopped 的清空（必做或强防御）

在清空 `currentSong` 前增加守卫，例如：

- 仍处于 `loading`：已有，保持；
- `restoredSessionUiOnly`：已有，保持；
- **新增**：若 controller 仍持有 `currentSong`，且该次 `stopped`/`idle` **不是**来自明确的 `stopPlayback` / 播完收尾路径，则：
  - 优先忽略，或
  - 仅把 `status` 置为 `paused`/`stopped` 而不清空 `currentSong`（需与 media session / session 持久化一致）。

推荐最小语义：

- **显式 stop**（`stopPlayback`）与 **队列耗尽 finished→stop**：允许清空。
- **重载/unload 伴随的 stopped**：在 `playGeneration` 进行中或 `status === 'loading'|playing` 且 songId 仍是当前意图时忽略清空。

实现时可引入短生命周期 flag（如 `ignoreNativeStopToken` / 沿用 `playGeneration`）避免布尔泄漏；优先复用现有 generation，避免新全局状态泛滥。

### C. 测试

在 `tests/unit/player.spec.ts`：

1. `play → pause → resume → pause → resume`：`currentSong` 始终非 null；第二次 resume **不**再次调用 `play`（mock 计数），只 `resume`（在 mock 有 asset 语义下）。
2. #49：`initializePlayer` 恢复后 `resumePlayback` 仍 `play` + `seek`。
3. 可选：模拟 resume 重载路径下迟到的 `stopped` 不得清空 `currentSong`（若实现 B）。

## 兼容性

- 不改 storage key、不改 UI 文案契约（空态仍可「暂无播放歌曲」）。
- media-session 仍由 `syncMediaSessionState` 跟随 `currentSong`；避免误 clear 是本修复附带收益。

## 回滚

单点回退 `controller.ts`（及对应测试）即可；无迁移。

# 修复沉浸式播放页二次播放暂停状态丢失

## Goal

修复 GitHub #52：在沉浸式播放页反复点击播放/暂停时，UI 不应丢失当前曲；底部迷你条与沉浸页必须与实际音频保持一致。

## Background / Confirmed Facts

- Issue #52：沉浸式页点击播放/暂停，**第二次**操作后沉浸页变空、迷你条显示「暂无播放歌曲」，但音频仍在播。
- 沉浸页空态与迷你条文案均由 `playerState.currentSong == null` 驱动：
  - `src/views/PlayerPage.vue`（`v-if="!playerState.currentSong"` / 「暂无播放歌曲」）
  - `src/components/MiniPlayer.vue`（`titleText` 回退「暂无播放歌曲」）
- `App.vue` 用 `!!playerState.currentSong` 决定是否保活 `PlayerPage`；`currentSong` 被清空会卸载沉浸页。
- `applyNativeState` 在原生 `idle` / `stopped` 时会把 `state.currentSong = null`（仅 `restoredSessionUiOnly` 时跳过，见 #49）。
- `resumePlayback` 注释写的是「冷启动仅 UI 恢复、原生尚未 load 时 play+seek」，但条件实际覆盖**所有** `paused|stopped|idle` + 曲库能找到当前曲，会无条件走 `playSongInternal`。
- `playSongInternal` → `AudioPlayerNative.play` 会先 `unloadCurrentAsset`（`NativeAudio.stop` + `unload`），可能异步冒出 `stopped` / 陈旧 `stateChange`；若在 `status === 'loading'` 保护窗外落到 controller，会清空 `currentSong`，而新一轮音频可能已在播——与 #52 现象一致。
- 相关历史：#28/#29（陈旧 songId 事件）、#49（冷启动会话恢复，禁止 idle 冲掉仅 UI 恢复）。

## Requirements

1. **R1 当前曲不因暂停/继续丢失**  
   沉浸页或迷你条上对「同一首已加载曲」做暂停 → 继续 → 再暂停 → 再继续（至少两轮），`playerState.currentSong` 始终保持该曲，沉浸页不进入空态，迷你条不显示「暂无播放歌曲」。

2. **R2 正常暂停/继续走轻量路径**  
   原生已加载当前 asset 时，暂停后继续应优先 `AudioPlayerNative.resume()`（或等价轻量恢复），**不得**为普通 pause/resume 无条件整曲 `play` + unload。  
   仅在「仅 UI 会话 / 原生无 asset」（#49）时允许 `play` + 可选 `seek`。

3. **R3 陈旧 stopped/idle 不得洗掉有意当前曲**  
   非用户明确 `stopPlayback`、非队列播完自然收尾时，原生 `stopped`/`idle` 不得把仍应对用户展示的当前曲清空，尤其不能出现「UI 无曲 + 音频仍在播」。

4. **R4 回归不破坏 #49**  
   冷启动从 `muses:playback-session` 恢复为 paused 展示、不自动出声；用户首次点播放仍可从恢复进度 `play` + `seek`；`stopPlayback` 仍清除 session 与当前曲。

5. **R5 自动化覆盖**  
   增加/更新单元测试，锁定「播放 → 暂停 → 继续 → 暂停 → 继续」后 `currentSong` 仍在，且普通 resume 不强制二次 `play`（仅 #49 路径允许 play+seek）。

## Acceptance Criteria

- [x] AC1：沉浸页打开且正在播时，连续两轮「暂停 → 播放」，页面始终展示当前曲信息/控件，不出现空态「暂无播放歌曲」。（单测：两轮 pause/resume 保持 currentSong）
- [x] AC2：同一操作序列下，底部迷你条标题不为「暂无播放歌曲」，且仍对应当前曲。（同源 `playerState.currentSong`）
- [x] AC3：上述序列中音频可暂停与继续；不出现「UI 显示无曲但声音仍在」。（普通 resume 不整曲 play；陈旧 stopped 忽略）
- [x] AC4：冷启动会话恢复（#49）行为保持：恢复为 paused、不自动 play；点播放从恢复进度起播。（既有 #49 用例仍绿）
- [x] AC5：`stopPlayback` 后仍清空当前曲与 session，迷你条可显示「暂无播放歌曲」。
- [x] AC6：相关 `tests/unit/player.spec.ts` 通过，包含 R5 所述回归用例。（118 passed）

## Out of Scope

- 沉浸页视觉改版、歌词/封面匹配逻辑变更。
- 队列、随机/循环、seek 伪 finished（#47 等）的新功能。
- 非本 bug 的原生插件大重构（除非修 bug 必须的最小改动）。

## Risks / Notes

- 播放状态机与 native bridge 竞态敏感；修复需同时收紧 `resumePlayback` 入口与 `applyNativeState` 对 `stopped`/`idle` 的清空条件，并靠单测锁行为。
- 轻量任务倾向：单 issue、边界清晰；因触及 controller 状态机与 #49 兼容，附 `design.md` + `implement.md`。

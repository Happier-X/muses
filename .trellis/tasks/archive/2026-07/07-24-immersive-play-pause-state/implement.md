# 实现清单：#52 沉浸式二次播放/暂停

## 顺序

1. **单测先锁行为**（`tests/unit/player.spec.ts`）
   - 普通路径：两轮 pause/resume 后 `currentSong` 仍在；有 asset 时 resume 不强制 `play`。
   - #49 路径：恢复会话后 resume 仍 `play` + `seek`。
   - 若实现 stopped 守卫：迟到 `stopped` 不清空当前曲。

2. **改 `resumePlayback`**（`src/features/player/controller.ts`）
   - 仅 `restoredSessionUiOnly`（或确认无 asset）时 `playSongInternal`。
   - 默认 `AudioPlayerNative.resume()`；失败再回退 play+seek。

3. **改 `applyNativeState` 清空条件**（同文件）
   - 防止重载/unload 的 `stopped`/`idle` 在非显式 stop 时 `currentSong = null`。
   - 保持 #49 `restoredSessionUiOnly` 与 loading 窗口行为。

4. **必要时 native 最小配合**（仅当单测/逻辑证明 controller 不够）
   - 例如 `getState`/`resume` 对无 asset 的明确信号；避免大范围插件改动。

5. **验证**
   - `npx vitest run tests/unit/player.spec.ts`
   - 手动：沉浸页两轮暂停/播放 + 冷启动恢复续播 + stop 清空。

## 风险文件

- `src/features/player/controller.ts` — 主修改
- `tests/unit/player.spec.ts` — 回归
- （可选）`src/features/player/native.ts`

## 完成前检查

- [ ] PRD AC1–AC6
- [ ] #49 用例仍绿
- [ ] 不引入「paused 但永久无法 resume」
- [ ] spec 若有新不变量，收工时写入 `features-player.md`

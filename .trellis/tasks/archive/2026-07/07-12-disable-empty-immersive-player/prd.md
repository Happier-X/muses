# 无歌曲时禁用打开沉浸式播放页

## Goal

当迷你播放器没有当前播放歌曲时，用户不能通过点击（或键盘）打开沉浸式播放页，避免进入无意义的空状态页。

## Background

- `src/components/MiniPlayer.vue` 主体始终可点击，点击后调用 `openPlayerOverlay()`。
- 无歌曲时标题文案为「暂无播放歌曲」，但点击仍会打开 `PlayerPage` 空状态。
- 播放/暂停按钮已在无歌曲时禁用；队列按钮不受本需求影响。
- 沉浸式播放页本身已有空状态 UI；本需求只阻断「从迷你播放器打开」的入口。

## Confirmed Facts

- 打开路径：`MiniPlayer.openPlayerPage` → `openPlayerOverlay()`（`src/features/player/overlay.ts`）。
- 当前歌曲判定：`playerState.currentSong` 为 `null` 即「暂无播放歌曲」。
- 现有测试覆盖「有歌曲时点击主体进入播放页」；尚无「无歌曲时不可打开」断言。
- Spec 约定见 `.trellis/spec/frontend/component-guidelines.md`：点击底栏主体打开播放器 overlay；需补充「无当前歌曲时不可打开」。

## Requirements

1. **R1** 当 `playerState.currentSong` 为 `null` 时，点击迷你播放器主体不得打开沉浸式播放页（`playerOverlayVisible` 保持 `false`）。
2. **R2** 当 `playerState.currentSong` 为 `null` 时，键盘 Enter / Space 同样不得打开沉浸式播放页。
3. **R3** 当存在当前歌曲时，点击主体仍应打开沉浸式播放页（既有行为不变）。
4. **R4** 无歌曲时播放/暂停按钮继续禁用；队列按钮行为不变。
5. **R5** 补充单元测试：无当前歌曲时点击迷你播放器主体不打开 overlay。
6. **R6** 同步更新 frontend component / player 相关 spec 约定。

## Acceptance Criteria

- [ ] AC1：无当前歌曲时，点击 `MiniPlayer` 主体后 `playerOverlayVisible` 仍为 `false`。
- [ ] AC2：无当前歌曲时，对 `MiniPlayer` 触发 `keyup.enter` / `keyup.space` 后 overlay 仍关闭。
- [ ] AC3：有当前歌曲时，点击主体仍可打开 overlay（既有测试继续通过）。
- [ ] AC4：无歌曲时播放按钮仍禁用；队列按钮仍可打开队列 overlay。
- [ ] AC5：`tests/unit/player.spec.ts` 新增覆盖 AC1 的断言并通过。
- [ ] AC6：相关 `.trellis/spec/frontend` 文档写明「无当前歌曲时不可打开沉浸式播放页」。

## Out of Scope

- 不改 `PlayerPage` 空状态 UI 本身。
- 不在播放过程中因歌曲变空而自动关闭已打开的沉浸式页（本需求仅阻断打开入口）。
- 不改队列 overlay 的打开条件。
- 不引入路由 `/player` 回退。

## Technical Notes

- 轻量任务：优先在 `MiniPlayer.openPlayerPage` 内判断 `!playerState.currentSong` 后直接 return。
- 可选防御：`openPlayerOverlay` 内增加同样守卫，避免其他入口误开；若本轮仅有 MiniPlayer 入口，以组件守卫为主即可。
- 可访问性建议：无歌曲时将主体 `role="button"` 标为不可用（如 `aria-disabled`），并去掉 `cursor: pointer` 误导。

## Task Type

Lightweight（PRD-only 即可；实现点单一、验收清晰）。

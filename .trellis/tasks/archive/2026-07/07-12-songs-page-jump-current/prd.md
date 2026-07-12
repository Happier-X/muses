# 歌曲页跳转当前播放悬浮按钮

## Goal

在歌曲页右下侧增加一个悬浮按钮（FAB），点击后滚动/跳转到当前正在播放的歌曲行。

## Issue

- GitHub #7：`歌曲页右下侧增加一个悬浮按钮用于跳转到当前播放的歌曲`

## Confirmed Facts

- `SongsPage.vue` 使用 `<ion-list>` 渲染歌曲行；每行可高亮当前播放（已有 `is-playing` / highlight 约定）。
- 当前播放 id：`playerState.currentSong?.id`。
- `ion-content` 可滚动；列表行可用 `data-song-id` 属性定位。

## Requirements

1. **R1** 歌曲页右下侧放一个 FAB（`ion-fab` / `ion-fab-button`），图标用定位类（如 `navigate` / `musical-notes`）。
2. **R2** 点击后滚动到当前播放歌曲行，并可选轻微高亮。
3. **R3** 无当前播放歌曲或当前歌曲不在列表中时：按钮隐藏或禁用。
4. **R4** 不破坏现有列表交互；FAB 不遮挡关键内容（留安全区）。
5. **R5** 提供 aria-label（如「跳转到当前播放」）。
6. **R6** 补充单测；lint/type-check 通过。
7. **R7** 同步 component-guidelines。

## Acceptance Criteria

- [ ] AC1：右下侧有 FAB，带 aria-label。
- [ ] AC2：当前有播放歌曲且在列表中时，点击滚动到该行。
- [ ] AC3：无播放歌曲或不在列表时，FAB 不可见或不可点。
- [ ] AC4：相关单测通过。

## Technical Notes

实现建议：

- 给每行加 `data-song-id="song.id"` 或 `ref`。
- 点击 FAB：`ion-content.scrollToPoint` 或用 `document.querySelector('[data-song-id="..."]')` 的 `scrollIntoView`。
- 宽屏列表多列时，`scrollIntoView({ block: 'center' })` 即可。
- 按钮可见性：`v-if="currentPlayingInList"`。

## Task Type

Lightweight

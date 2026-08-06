# 修复歌曲页滚动到底部多余空白

## Goal

消除歌曲页（`/tabs/songs`）虚拟列表滚动到最底部时，最后一行歌曲下方多出的空白区域，同时保证最后一行不被悬浮的 MiniPlayer / tab-bar 遮挡。

## Background（已确认事实，代码证据）

- `TabsPage.vue:28`：`<main>` 在移动端已通过 `pb-[calc(var(--muses-tab-bar-height)+env(safe-area-inset-bottom,0px))]` 为底部 fixed tab-bar 预留空间（tab-bar 为 `position:fixed; bottom:0`，见 happier-ui `HTabBar` 的 `.h-tab-bar--fixed` / `--safe-area`）。
- `SongsPage.vue:32`：虚拟列表容器 `listParentRef` 的底部 padding 为 `pb-[calc(var(--muses-tab-bar-height)+var(--muses-mini-player-height)+env(safe-area-inset-bottom,0px))]`，**重复计算了 tab-bar 高度**（`--h-tab-bar-height: 64px`、`--h-mini-player-height: 64px`、`--h-song-row-height: 72px`，见 happier-ui tokens）。
- MiniPlayer（`src/components/MiniPlayer.vue`）为 `fixed`，`bottom: calc(tab-bar-height + safe-area)`，悬浮在 tab-bar 之上，常驻显示；实际高度 = `--h-mini-player-height`（64px，Tailwind preflight `box-sizing: border-box`，min-h 含 padding，内容 48px cover + 16px py 恰好 64px）。
- 因此移动端列表实际只需为 MiniPlayer 让出约 64px（+少量余量），当前 padding = 128px + safe-area，多出约 64px + safe-area 的空白。
- 平板端（≥768px）：main `pb-0`，MiniPlayer `bottom: env(safe-area-inset-bottom)` 贴视口底，列表 padding 需要 64px + safe-area + 余量。
- 该问题自 Tailwind v4 迁移（f58aa18）前即存在（原 `.song-list` 同样双算 tab-bar），非近期回归。

## Requirements

- R1（SongsPage 修复）：歌曲页虚拟列表底部 padding 不再重复计算 tab-bar 高度，滚动到底后最后一行与 MiniPlayer 之间无多余空白、也不被遮挡。
  - 移动端：`pb = mini-player-height + 少量余量`（无 safe-area，因 safe-area 已由 main 的 pb 消化）
  - 平板端：`pb = mini-player-height + 余量 + safe-area`（main pb-0，MiniPlayer 贴视口底）
- R2（已确认纳入）：修复同类遮挡问题——
  - `PlaylistDetailPage.vue` 虚拟列表无底部 padding，滚动到底最后一行被 MiniPlayer 遮挡；
  - `SourcesPage.vue` 列表仅 `pb-[24px]`，同样可能被 MiniPlayer 遮挡。
  - 两处均补上与 R1 一致的底部预留：移动端 `mini-player-height + 余量`，平板端再加 `safe-area`。

## Acceptance Criteria

- [ ] AC1：移动端歌曲页滚动到最底部，最后一行下方无多余空白（相对修复前明显减少，仅保留 MiniPlayer 上方少量余量）。
- [ ] AC2：滚动到底时最后一行不被 MiniPlayer / tab-bar 遮挡。
- [ ] AC3：平板端（≥768px）行为不回归（mini-player 贴底时最后一行不被遮挡）。
- [ ] AC4：PlaylistDetailPage / SourcesPage 滚动到底同样不被 MiniPlayer 遮挡。

## Out of Scope

- 虚拟列表 `estimateSize` / `measureElement` 的测量精度优化（无证据表明是本次空白来源）。
- AlbumsPage / ArtistsPage / PlaylistsPage 等其他页面的滚动与遮挡问题（用户未报告，不纳入本次范围）。
- 布局体系重构（如统一"底部悬浮层预留"机制）。

## Open Questions

- 无（Q1 已确认：R2 纳入）。

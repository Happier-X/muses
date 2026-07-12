# 平板歌曲页改为一列

## Goal

平板上歌曲页改为与窄屏相同的一列列表，取消两列 grid。

## Issue

- GitHub #11：`平板上歌曲页面不需要两列展示，跟窄屏上一样，一列展示即可`

## Confirmed Facts

- `SongsPage.vue` 宽屏使用 `.list-grid { grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)) }` 自动分列。
- 专辑/歌手页有相同模式，但本 issue 仅针对歌曲页。

## Requirements

1. **R1** 平板上 SongsPage 列表始终单列。
2. **R2** 窄屏行为不变。
3. **R3** 可保留 `tablet-content-limit` 内容最大宽度居中（若有）。
4. **R4** 不强制改 Albums/Artists 多列（超出 issue 范围）。
5. **R5** 同步 component-guidelines 中 SongsPage 列表布局约定。
6. **R6** lint / type-check 通过。

## Acceptance Criteria

- [ ] AC1：宽屏 SongsPage 仅一列。
- [ ] AC2：窄屏无回归。
- [ ] AC3：spec 已更新。

## Task Type

Lightweight

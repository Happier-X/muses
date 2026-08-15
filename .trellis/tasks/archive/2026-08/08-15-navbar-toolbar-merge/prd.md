# navbar 与工具条融为一体

## Goal

SongsPage 吸顶工具条与顶部 navbar 之间移除视觉界限（亮线），两者无缝融合为同一液态玻璃面。

## Background

- 上一轮液态玻璃化后，toolbar 自带 `inset 0 1px 0 rgba(255,255,255,0.65)` 顶部内高光——正好落在 navbar 底部边缘，形成一条白色亮线界限（navbar 自身 border-bottom 已为 none）。
- 修复：移除 toolbar 顶部内高光（含深色 inset 0.1），仅保留底部 hairline 区分列表。

## Requirements

- navbar 与工具条交界处无亮线/分割线，视觉连续（同为半透明白玻璃）。
- 工具条底部与列表之间的 hairline 保留。

## Acceptance Criteria

- [ ] MuMu 实测：交界区域逐行亮度平滑（无 ≥250 突变行）。
- [ ] 深色主题无回归。
- [ ] vue-tsc / ESLint / 构建通过。

## Notes

- 轻量任务，PRD-only。改动：`src/views/SongsPage.vue`（toolbar 移除 inset 高光，深浅两处）。

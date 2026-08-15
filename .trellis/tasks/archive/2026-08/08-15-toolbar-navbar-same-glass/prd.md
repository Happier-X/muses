# 工具条并入 navbar 同一块玻璃

## Goal

SongsPage 工具条/搜索栏从独立 sticky 元素移入 MNavbar 的 `#subnavbar` slot——与 navbar 成为**同一块玻璃**（同一背景/blur 层），彻底消除两块独立玻璃交界处的分界感。

## Background

- 用户反馈："用的是两块不同的玻璃，所以交界的地方必定会有分界感"——判断正确。
- 根因：navbar 与 toolbar 是两个独立元素，各自半透明背景 + backdrop-filter（MuMu WebView 110 上 blur 还失效），交界处内容透出/采样不一致 → 必然分界。
- 解法：MNavbar 已有 `#subnavbar` slot（渲染在根元素内部 `__subnavbar`，共享根背景/blur 层）——工具条/搜索栏移入后与 navbar 同一玻璃。
- 布局调整：navbar 总高 66→114（+48 subnavbar）；列表/空态 padding-top 同步 +48px；subnavbar 高度 48px（覆盖默认 56px）。

## Requirements

- navbar + 工具条/搜索栏为同一块玻璃，交界处无任何分界（线/色差/阴影）。
- 工具条功能（随机播放/歌曲数/多选计数）与搜索栏（输入/取消）行为不变。
- 手机/平板一致；深色模式随 navbar 玻璃；空态/搜索态布局无跳动。

## Acceptance Criteria

- [ ] MuMu 实测：navbar 与工具条交界逐行亮度平滑（无突变线/色差）。
- [ ] 搜索状态 navbar 高度不变（搜索栏也入 subnavbar），列表 padding 无跳动。
- [ ] 随机播放/多选计数/搜索功能正常。
- [ ] vue-tsc / ESLint / 构建通过。

## Notes

- 轻量任务，PRD-only。改动：`src/views/SongsPage.vue`（模板移入 subnavbar + 样式/避让调整）+ `src/components/ui/MNavbar.vue`（0.8 玻璃参数，上轮遗留未提交）。
- 已实测：navbar 高 114（66+48）、toolbar bg 透明 blur none（在 navbar 玻璃内）、交界 y 197-200 亮度平滑连续。

# 平板沉浸播放页左右布局

## Goal

平板（≥768px 逻辑宽度）下沉浸式播放页恢复**左右布局**（左侧播放控制页 + 右侧歌词页，panels flex row），且**左侧不再展示三行歌词窗口**（song-meta 隐藏）。

## Background

- 全局 `index.scss` 已有 md 断点规则（462-510 行）：`.player-overlay .panels` flex row 左右并排、右侧 lyric-player flex:1。
- **但规则内使用的旧类名（`.cover-slot`/`.cover`/`.placeholder-cover`/`.song-info`）在 08-14 椒盐复刻后已不存在**（现为 `.player-page__cover-hero`/`.cover-hero-img`/`.song-head` 等 scoped 类），且同特异性下组件 scoped 样式后注入会覆盖全局规则——平板布局实际失效。
- 手机竖排结构（scoped）：song-head → cover-hero → song-meta（三行歌词）→ progress → controls → mode-bar。
- 需求：md+ 左侧保留 song-head/cover-hero/progress/controls/mode-bar，**隐藏 song-meta**；右侧歌词面板保持。

## Requirements

- ≥768px：panels 左右布局（左信息面板 + 右歌词面板），各占 50%。
- 左侧：歌名/歌手、封面、进度、控制键、模式栏；**不展示三行歌词窗口**。
- 右侧：歌词页（隐藏顶部 lyric-header，与现 md 规则一致）。
- 手机（<768px）布局零回归；窄高屏（max-height:520px）逻辑不受影响。

## Acceptance Criteria

- [ ] MuMu 模拟平板（wm size/density 模拟 ≥768dp）：播放页左右分栏，左侧无三行歌词，右侧歌词完整。
- [ ] 左侧封面/控制/进度布局在平板宽度下协调（封面不溢出、控件不散）。
- [ ] 手机分辨率下布局无回归。
- [ ] `vue-tsc` / ESLint / 构建通过。

## Notes

- 轻量任务，PRD-only。改动集中在 `src/theme/index.scss` 的 md 断点块（旧类名 → 新类名 + 隐藏 song-meta），必要时补 PlayerPage scoped 的 md 覆盖。
- 需同步更新 `.trellis/spec/frontend/component-guidelines.md` 的 PlayerPage 平板布局说明（若存在）。

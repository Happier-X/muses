# 播放条液态玻璃效果对齐椒盐 Liquid Glass

## Goal

MiniPlayer 与跳转 FAB 恢复**液态玻璃**效果（纠正 08-15-glass-solid-salt 的实心误判）：高白底 + 强模糊，**底下列表内容模糊可见**，对齐椒盐音乐「实验室 → Liquid Glass」迷你播放条液态玻璃效果。

## Background

- **椒盐真相（GitHub Releases 更新日志）**：椒盐的「液态玻璃」= **实验室 Liquid Glass 开关**（仅 Android 13+），作用于迷你播放条。**默认关闭 = 实心胶囊**；开启后 = 半透明模糊（用户描述"播放条底下能看到歌曲列表，有模糊效果"）。
- **用户诉求**：muses 播放条玻璃"透明效果有点浅"，要椒盐液态玻璃质感。
- **08-15-glass-solid-salt 误判**：当时实测到椒盐默认状态（Liquid Glass 关闭）的纯白胶囊，误改为实心——方向错误，本任务纠正。
- **修正参数**（Android 13 Liquid Glass 风格）：
  - 浅色：`rgba(255,255,255,0.85)`（旧 0.72 太透 → 提高更实）+ `blur(30px)`（旧 20px → 增强模糊）+ `border: 1px solid rgba(255,255,255,0.45)`（边缘高光）+ 轻阴影
  - 深色：`rgba(30,30,30,0.85)` + blur + `border rgba(255,255,255,0.12)`
- 图标色保持主题变量（`var(--m-text)` / `var(--m-text-2)`，深色自动变浅——08-15-glass-solid-salt 的正确部分保留）。

## Requirements

- 播放条/FAB 为半透明液态玻璃：底下列表内容**模糊可见**，白色底更实（0.85）。
- 浅色/深色主题均有对应玻璃配方；图标/文字色随主题变量。
- 不改布局几何与交互；不影响其他页面。

## Acceptance Criteria

- [ ] MuMu：歌曲列表滚动后，播放条底下可见**模糊的列表内容**（modlens 实测确认背景透出模糊曲目标题）。
- [ ] 播放条白色底比旧版（0.72）更实，不刺眼不浑浊。
- [ ] `vue-tsc` / ESLint / 构建通过。

## Notes

- 轻量任务，PRD-only。改动：`src/components/MiniPlayer.vue` + `src/views/SongsPage.vue`。
- 已实测（modlens）：播放条背景为 "blurred, semi-transparent view of another track" ✓。
- 需修正 `.trellis/spec/frontend/component-guidelines.md` 中 08-15-glass-solid-salt 写入的「实心胶囊」契约（恢复液态玻璃契约 + Liquid Glass 背景事实）。

# 沉浸式播放页1:1复刻Capacitor

## Goal

将原生沉浸式播放页与 Capacitor 版 `PlayerPage.vue` 进行增量式 1:1 对齐，首轮聚焦手机端最易感知的视觉/布局差异，达到用户“一点点复刻就好”的效果，不做一次性大重构。

## 背景

- Capacitor 原版为 `src/views/PlayerPage.vue`（3430 行，`<template>` + `<style scoped lang="scss">` 888 行，见 `/tmp/cap_player.vue` / `/tmp/cap_style.scss`，git `de7e388f^`）。
- 原生当前为 `feature/player/PlayerScreen.kt`（约 1533 行）+ `IOSLyricsPanel.kt` + `FlowingLightBackdrop.kt`，已完成 HorizontalPager、拖动/底部条/平板分栏等骨架，但细节与 Capacitor 仍有偏差。
- 用户反馈“没有一比一复刻”，期望渐进式对齐，而非单次推翻。

## Requirements

### 功能（首轮增量，手机端优先）

- **R1 封面 Hero 尺寸**：对齐 Capacitor `player-page__cover-hero` 的 `width:auto height:auto max-width:100% max-height:100% aspect-ratio:1` + 容器 `max-height: min(50vh,420px)` 语义，当前原生固定 `272.dp/360.dp/150.dp` 需改为按可用高度比例 contain。
- **R2 五行小窗**：对齐 `player-page__song-meta` 79px 视口、`meta-window` -29.5px 居中、`meta-line` 13/1.5 rgba0.6/0.92、scale 1.05/0.92、blur 0/0.6、行距 10、矮屏单行 19.5 高度与 cover 34vw/150 限制。
- **R3 进度条与时间行**：对齐 `m-range` 的 `color: var(--m-primary)` + 隐藏 `thumbWrap`、时间行 `12px tabular-nums rgba0.68`、缓冲提示 `11px rgba0.55`，当前原生 Slider 白轨与 chip 样式需对齐。
- **R4 控制区**：对齐 `controls` gap `clamp(24,10vw,44)`、三键 `lg 28px fill+stroke`、 `mode-bar` max-w 320、无 `is-active` 仅图标、`bottom-bar` 渐变 `rgba(5,7,13,0)→0.55` 与 padding `6 24 calc(8+safe-bottom)`。
- **R5 歌词面板 Fab**：对齐 Capacitor `lyric-fabs` 的 `clear + text-white/80` 主控/`mode-bar`/`lyric FAB` 语义与 `is-active` 仅翻译开关的约束，当前原生 `FilledTonal/Filled` 需改为 `SaltIconButton` clear 风格。
- **R6 背景层**：对齐 `player-page__bg` opacity 0.75、fallback 纵向渐变、blur 28dp/scale1.08、高光径向 0.07，当前 `FlowingLightBackdrop` 为 blur 32dp/alpha0.68 + 额外 Canvas 高光 0.075 需对齐。
- **R7 手势与容器**：保持 `drag-layer` translateY 回弹 0.22s easeOut、panels 200%→`translateX(-active*50%)` 0.22s easeOut 的手势语义；移除手机端额外的小圆点指示器（Capacitor 原版无指示器，避免偏离 1:1）。

### 非功能

- 不引入新依赖，保持 `Hilt/Compose/Material3/Coil3` 栈。
- 不改变播放/歌词数据链路，仅视觉与交互层对齐。
- 增量交付，首轮不触平板双栏与编辑表单位（后续迭代）。

## 约束

- 严格对照 Capacitor 源码与旧版截图，不得臆造样式。
- `isTabletLayout` 定义保持 `>=768 && height<width` 横屏判定不变。
- 单轮改动控制在 5-7 文件内，避免大 diff 难审。

## Acceptance Criteria

- [ ] `PlayerScreen.kt` 中封面 Hero 容器改为 `maxHeight = min(50vh,420)` 语义，图片 `aspect 1 + maxW/H 100%` contain，矮屏自动收至 34vw/150 限制，视觉与 Capacitor 一致
- [ ] `MetaWindow` 高度/偏移/行样式与 Capacitor `song-meta/meta-window/meta-line` 一致（79/19.5、-29.5/-10、13/1.5、行距10、scale 1.05/0.92、opacity 1/0.55、blur 表现一致），窄屏单行正确
- [ ] 进度条 Slider 轨颜色与 thumb 隐藏对齐 Capacitor（primary 轨或白轨二选一以原版为准，thumb 不可见），时间行字号/颜色/缓冲文案一致
- [ ] 控制区与底部条间距/尺寸/颜色与 Capacitor 对齐，mode-bar 无 is-active 误用，图标 28/20 分两档
- [ ] 歌词 Fab 改为 clear 透明底 + 白字 `text-white/80`，仅翻译键有 is-active，3s idle 隐藏逻辑不变
- [ ] 背景层参数对齐 Capacitor（opacity 0.75、blur 28dp、scale 1.08、高光 alpha 一致），fallback 渐变正确
- [ ] 手机端移除额外指示器圆点，或证明保留不影响 1:1 判定（以 PRD 评审决定）；`grep -rn "isFirstLaunch"` 零命中保持
- [ ] `./gradlew :feature:player:assembleDebug` 与 `:app:assembleMusesDebug` 通过，`PlayerScreen` 无 lint 阻断

## Out of Scope

- 平板双栏/底部条/编辑 sheet 的像素级对齐（下轮）
- 歌词解析/翻译合并逻辑
- 播放队列页样式

## Notes

- 增量任务：首轮聚焦手机端 5-7 个视觉原子，后续可拆子任务逐轮逼近 1:1。
- 参考源：`git show de7e388f^:src/views/PlayerPage.vue`（模板 0-2543 行，样式 2543-3430 行，已导出 `/tmp/cap_player.vue` 与 `/tmp/cap_style.scss`）。

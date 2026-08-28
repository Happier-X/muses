# 执行计划 — 沉浸式播放页1:1复刻Capacitor

## 步骤

1. **研究**（已完成）：导出 `git show de7e388f^:src/views/PlayerPage.vue` → `/tmp/cap_player.vue` 与 `/tmp/cap_style.scss`，确认 888 行样式契约
2. **分支与上下文**：确认 `task.py current` 为 `08-28-player-capacitor-1to1`，curate `implement.jsonl/check.jsonl`（见下）
3. **R6 背景层**：改 `FlowingLightBackdrop.kt` blur 28 alpha0.75 高光 0.07，对齐 Capacitor
4. **R1 封面**：改 `PlayerScreen.kt CoverHero` 与 `InfoPanel`/`TabletImmersiveLayout` 封面 sizing 为 `min(50vh,420)` contain 逻辑，BoxWithConstraints 取 maxHeight
5. **R2 小窗**：微调 `MetaWindow` 79/-29.5/scale/opacity 与窄屏 19.5 逻辑，校验与 scss 一致
6. **R3 进度条**：调 `ProgressSection` Slider 颜色/隐藏与 `time-row` 字号 tabular-nums
7. **R4 控制区**：调 `ControlsRow/ModeBarRow` gap 与 maxW 320，图标 28/20 分档
8. **R5 歌词 Fab**：改 `IOSLyricsPanel.kt` Fab 为 clear 风格，仅翻译 is-active
9. **R7 指示器**：移除或隐藏 `PhoneImmersiveLayout` 中额外圆点指示器
10. **校验**：`./gradlew :feature:player:assembleDebug :app:assembleMusesDebug :feature:player:lint` + 真机目视对比

## 验证命令

```bash
./gradlew :feature:player:assembleDebug --rerun-tasks
./gradlew :app:assembleMusesDebug --rerun-tasks
./gradlew :feature:player:lintMusesDebug --rerun-tasks
```

## 回滚点

- 任一步失败可单独 revert 对应文件，不影响数据链路

## 评审门

- PRD/Design 已评审 → `task.py start` → 实现 → 自测 → `trellis-check`

# 修复加入待刮削提示被播放条遮挡及样式不一致

## Goal

入队提示不再被底部播放条遮挡，且样式与项目公共 Toast（`Toast.makeText` 居中 `m-toast`）一致。

## 背景

- 当前歌曲页使用 `SnackbarHost` 置于内容区底部 16dp，被 `MiniPlayerBar` 覆盖
- 项目其余轻提示（设置页、音源页）均使用系统 `Toast` 居中样式，待 SaltToast 统一前保持一致

## Requirements

### R1 不被遮挡（P0）
- 提示在系统窗口层展示，不受 `MiniPlayerBar`/`navigationBars` 遮挡

### R2 样式一致（P0）
- 使用 `android.widget.Toast` 短时居中样式，与设置页/音源页一致

## Constraints

- 仅改 `feature:library/SongsPage.kt`，移除局部 `SnackbarHost`，改为 `Toast`
- 不改入队幂等语义与文案

## Acceptance Criteria

- [ ] **AC1 可见**：歌曲页点“加入待刮削”后 Toast 在屏幕中部可见，不被播放条遮挡
- [ ] **AC2 一致**：文案“已加入待刮削队列 / 已加入 N 首”与公共 Toast 样式一致（系统短 Toast）

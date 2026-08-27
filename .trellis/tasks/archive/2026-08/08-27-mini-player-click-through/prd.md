# 迷你播放条点击穿透修复

## Goal

点击底部迷你播放条的播放/暂停、队列按钮时不再误触发打开沉浸式播放页；仅点击文案/封面区域才打开。

## 背景

- `MiniPlayerBar` 整条 `Row` 设 `clickable(onOpenPlayer)`，内层 `SaltIconButton` 点击时事件冒泡至父层，导致播放暂停同时打开播放页

## Requirements

### R1: 隔离点击区域
- 外层点击仅绑定到封面与标题/副标题区，控制区（播放/队列）独立消费事件
- 保持 64dp 胶囊整体视觉与阴影不变

### R2: 保持可访问性
- 播放/队列按钮独立 `contentDescription` 与 `enabled = hasSong` 逻辑不变
- 封面/文案区点击仍打开播放页

## Acceptance Criteria

- [ ] 点击播放/暂停按钮仅切换播放状态，不打开沉浸式页
- [ ] 点击队列按钮仅打开队列，不打开沉浸式页
- [ ] 点击标题/副标题或封面打开沉浸式页
- [ ] `hasSong=false` 时整条不可点
- [ ] MuMu 模拟器验证通过

## Notes

- 涉及 `core/ui` 的 `MiniPlayerBar.kt`，需避免整条 clickable 覆盖子按钮

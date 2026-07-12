# 去掉沉浸式封面阴影

## Goal

去掉沉浸式播放页封面后方的阴影效果。

## Issue

- GitHub #4：`沉浸式播放页面封面后面怎么多了一个阴影一样的东西，去掉它`

## Confirmed Facts

- `PlayerPage.vue` 中 `.cover { box-shadow: 0 20px 50px rgba(0, 0, 0, 0.45); }`
- `.controls .play-toggle` 另有 `--box-shadow`，属按钮样式，不在本 issue 范围（由 #3 处理）

## Requirements

1. **R1** 移除控制页封面（`.cover`）的 `box-shadow`
2. **R2** 占位封面与真封面一致，不出现额外阴影
3. **R3** 不改 AMLL 背景渲染本身

## Acceptance Criteria

- [ ] AC1：`.cover` 无 `box-shadow`
- [ ] AC2：宽屏/窄屏封面样式均无阴影

## Task Type

Lightweight

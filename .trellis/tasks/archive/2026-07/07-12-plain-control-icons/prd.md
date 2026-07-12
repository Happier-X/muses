# 播放控制按钮去背景圆圈

## Goal

沉浸式播放页中间主控制区（上一曲 / 播放暂停 / 下一曲）改为纯图标，不带背景圆圈。

## Issue

- GitHub #3：`沉浸式播放页面中间的按钮改成不带背景圆圈的，只要图标即可`

## Confirmed Facts

- 上一曲/下一曲已是 `fill="clear"` + `shape="round"`
- 播放/暂停是 `class="play-toggle" fill="solid" color="light" shape="round"`，有实心圆底与 `--box-shadow`
- `.controls ion-button` 固定 52×52（播放 68×68）

## Requirements

1. **R1** 播放/暂停改为与上一曲/下一曲一致的无背景图标按钮（`fill="clear"`）
2. **R2** 去掉播放按钮实心圆背景与按钮阴影
3. **R3** 保留可点热区与 `aria-label`
4. **R4** 次要控制（循环/随机/队列）保持现状，除非同样有实心圆需统一（默认不动）

## Acceptance Criteria

- [ ] AC1：中间三键均为 clear 图标按钮，无 solid 圆底
- [ ] AC2：视觉上只有图标，无背景圆/阴影
- [ ] AC3：播放/暂停功能与禁用态（loading）仍正确

## Task Type

Lightweight

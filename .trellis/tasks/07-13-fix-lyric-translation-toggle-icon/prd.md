# 修复歌词翻译开关与图标尺寸

## Goal

修复 Issue #26：歌词翻译开关不可用，且翻译图标过大突兀。

## Root Cause（推断）

- 现实现仅修改传入 `LyricPlayer` 的 `translatedLyric` / `romanLyric` 字段，但 AMLL 内部可能不会对同一播放器实例的行翻译变更完整刷新。
- 浮动按钮尺寸 `48px`、图标 `24px`，在歌词页左下角视觉权重过高。

## Requirements

1. 翻译开关点击后必须可靠生效：隐藏/恢复翻译时重建或刷新 `LyricPlayer`。
2. 翻译图标缩小并降低突兀感。
3. 手机右下播放按钮视觉尺寸同步收敛；平板仍不展示播放按钮。
4. 增加回归测试覆盖翻译内容隐藏/恢复。

## Acceptance Criteria

- [ ] 带 `translatedLyric` 的歌词行，点击翻译按钮后翻译消失，再点恢复。
- [ ] 翻译/播放浮动按钮尺寸小于上一版且不遮挡歌词主体。
- [ ] `vitest` / `vue-tsc` / lint 通过。

## Task Type

Lightweight

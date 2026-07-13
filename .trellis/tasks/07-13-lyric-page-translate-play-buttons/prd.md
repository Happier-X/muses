# 歌词页翻译与播放按钮

## Goal

修复 Issue #23/#24：沉浸式歌词页增加便捷浮动按钮。

## Issues

- #23：歌词页面左下角增加一个翻译图标，可以打开或关闭翻译功能。
- #24：歌词页面右下角增加一个播放暂停图标按钮，平板模式不展示该图标按钮。

## Confirmed Facts

- 歌词页在 `PlayerPage.vue` 的第二个 panel：`.lyric-panel`。
- AMLL `LyricLine` 支持 `translatedLyric` / `romanLyric` 字段；隐藏翻译可在传入 `LyricPlayer` 前清空翻译字段。
- 已有 `isPlaying` / `pausePlayback` / `resumePlayback` 和 `play` / `pause` 图标。
- 当前 `PlayerPage` 内已有手势逻辑，歌词区域上下滑不关闭 overlay。

## Requirements

1. **R1** 歌词页左下角展示翻译开关按钮；点击可隐藏/显示 `translatedLyric` 与 `romanLyric`。
2. **R2** 翻译开关状态在当前 PlayerPage 生命周期内保持；默认显示翻译。
3. **R3** 歌词页右下角在非平板模式展示播放/暂停按钮；点击切换播放状态。
4. **R4** 平板模式（宽度 >= 768px）不展示右下播放/暂停按钮。
5. **R5** 无当前歌曲时不展示这些浮动按钮；现有控制页按钮与手势不回归。

## Acceptance Criteria

- [ ] 翻译按钮点击后 AMLL 行翻译消失，再点恢复。
- [ ] 手机宽度歌词页右下角可播放/暂停。
- [ ] 平板宽度不显示歌词页右下角播放/暂停按钮。
- [ ] `vitest` / `vue-tsc` / lint 通过。

## Task Type

Lightweight

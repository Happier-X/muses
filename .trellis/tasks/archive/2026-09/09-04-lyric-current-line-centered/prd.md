# 沉浸式播放页歌词当前行居中

## Goal

将沉浸式播放页歌词当前行从视口约 25% 高度位置改为视口垂直居中（50%），手机与平板一致。

## Background

- 已确认现状（代码实测）：
  - `feature/player/.../lyric/LyricsPanel.kt:147` `UpstreamLyrics.FOCUS_POSITION = 0.25f`
  - `feature/player/.../lyric/Stubs.kt:57` `SettingsRuntime.lyricFocusPosition = 0.25f`
  - `AppleMusicLyricsPanel` 统一使用该值计算 `topPaddingPx / bottomPaddingPx / focusItemScrollOffset / focusAnchorY`（`LyricsPanel.kt:666-676,919`）。
- 沉浸式页手机（`PhoneImmersiveLayout`）与平板（`TabletImmersiveLayout`）共用 `LyricsPanel(document, positionMs, isPlaying, onSeek)`（`LyricsPanelWrapper.kt`），改一处即全生效。
- Eva / TextPV 本来就是 `Box Center` 居中构图，不受该值影响；Skyline 当前禁用（`skylineEnabled=false`），不纳入本次范围。

## Requirements

- R1：将歌词焦点锚点改为 `0.5f`，当前行在视口垂直居中。
  - 涉及 `UpstreamLyrics.FOCUS_POSITION` 与 `SettingsRuntime.lyricFocusPosition` 保持一致。
- R2：顶部与底部留白随锚点对称（各 50% 视口高），保证首行与末行也能滚到中央。
- R3：滚动定位公式保持 `desiredItemTop = viewportAnchor - estimatedHeight * focus`，`focus=0.5` 即行高一半偏移，实现真居中。
- R4：不改变级联动画、模糊/透明、逐词高亮等其他视觉行为。
- R5：歌词主行字号改为 `24sp`（现 `34sp` 偏大）。
  - 涉及 `UpstreamLyrics.FONT_SIZE_SP 34f → 24f`，`LINE_HEIGHT_SP 40.8f → 28.8f`（保持 1.2 倍行高）。
  - 翻译/音译按既有 `0.65` 缩放自动跟随，不单独改。
- R6：歌词主行字重改为 `600`（半粗）。
  - 涉及 `SettingsRuntime.lyricFontWeight Bold → SemiBold`（`SemiBold` 即 `FontWeight.SemiBold`，对应 600）。

## Out of Scope

- 不做用户可调的焦点位置设置项。
- 不改 Eva / TextPV / Skyline 样式。
- 不改翻译/音译缩放比例与行距比例（随主行自动缩放）。

## Acceptance Criteria

- [x] 播放有逐行时间的歌曲，当前行稳定停留在歌词视口垂直中央附近（上下留白大致对称）。
- [x] 切歌、拖进度 seek 后，新的当前行仍能回到中央。
- [x] 首行与末行播放时也能居中（靠对称 padding 实现）。
- [x] 手机竖屏与平板横屏双栏右侧歌词均居中。
- [x] 无回归：滚动跟随、级联动画、点击 seek 正常。
- [ ] 歌词主行字号为 24sp，视觉明显小于之前，行高协调不拥挤。
- [ ] 歌词主行字重为 600 半粗，视觉弱于之前粗体但仍突出。
- [ ] 手机与平板歌词字号字重一致。

## Open Questions

- 无阻塞问题。

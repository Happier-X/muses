# 歌词页浮动按钮样式与翻译可见性

## Goal

修好歌词页底部浮动 chrome 三件事：图标不再发黑、交互后（含横屏）能稳定看见、**无译文时不展示翻译按钮**。

## Background

- 入口：`PlayerPage.vue` 歌词面板 `.lyric-floating-actions`（左翻译、右播放/暂停）。
- **发黑**：FAB 仍写 Ionic 时代 `[--color:…]` / `[--background:…]`，而 `HButton variant="ghost"` 使用真实 `color` / `background`（默认 `var(--h-color-ink)` 黑字）。`tailwind.css` 里 `.lyric-fab` 也只写了无效的 `--color` / `--background`，深色歌词底上图标发黑或几乎不可见。
- **横屏「活动时不展示」**：规范要求点击/滑动歌词后 `.is-visible` 显示、3s 隐藏。宽屏（常含手机横屏 ≥768）按设计隐藏右下播放键，但翻译键应仍在。发黑 + 深底会表现为「没出来」；实现需保证有译时翻译键在 chrome 可见态可辨认，且 `revealLyricChrome` 路径在横屏/宽屏仍生效。
- **无译仍出翻译钮**：翻译 FAB 仅随 `currentSong` 渲染，未判断行内 `translatedLyric`/`romanLyric`（或已挂载的 tlyric）。无译文时开关无意义，应隐藏。

## Requirements

- **R1（颜色）**：歌词页浮动按钮（翻译 + 播放）在深色沉浸底上使用浅色图标与半透明底，**不得**依赖已失效的 Ionic `--color`/`--background`；激活态翻译键仍可区分（`.is-active`）。
- **R2（活动显隐）**：在歌词面板内点击/滑动后 chrome 显示（约 180ms fade），空闲 3s 隐藏；点 FAB 重置计时；切回控制页/关 overlay 立即隐藏。横屏与竖屏、宽屏与窄屏均如此（宽屏仍可不显示播放键）。
- **R3（无译隐藏）**：当前展示歌词行中**没有任何**可用译文/音译（`translatedLyric` / `romanLyric` 皆空，且无已并入的翻译数据）时，**不渲染**翻译按钮；有译时才出现。
- **R4**：无译仅剩播放键时，右下布局仍可用；宽屏无播放且无译时，浮动区可不渲染或空容器不可点。
- **R5**：不改 AMLL 解析、seek、手势隔离、主控区样式；翻译开关语义与双图标族（Captions / CaptionsOff）不变。

## Acceptance Criteria

- [ ] AC1：歌词页 chrome 可见时，翻译/播放图标为浅色（白/近白），非黑；激活翻译键有可辨高亮。
- [ ] AC2：竖屏与横屏（含宽屏双栏）在歌词区交互后，有译时翻译按钮出现并可点；3s 空闲再藏。
- [ ] AC3：纯原文、无 tlyric/双行译/roman 的歌词，不出现翻译按钮。
- [ ] AC4：有译歌词仍可开关翻译，关译后主行不丢、副行隐藏（既有 `applyLyricTranslationVisibility` 行为）。
- [ ] AC5：规范（`component-guidelines` / `features-player` 相关句）与实现一致。

## Out of Scope

- 重做歌词页整体布局 / AMLL 参数
- 在线匹配文案与空态大改
- 控制页主控按钮样式
- 平板强制显示右下播放键

## Technical Notes

- 颜色：模板改用真实 `color`/`background` utility 或全局 `.player-overlay .lyric-fab { color; background; }` 覆盖 ghost 默认；同步修 hover/active，去掉无效 `--color`/`--background`。
- 有译判定：基于 `lyricLines`（`prepareLyricLinesForDisplay` 之后）是否存在非空 `translatedLyric` 或 `romanLyric`；可选兼看 `playerState.lyricsTranslation`。
- 翻译按钮 `v-if="hasLyricTranslation"`（命名可再定）；无译时不要占位热区。
- 横屏：确认 `lyricChromeVisible` + 浅色后可辨；若仍有断点把整个 floating 藏掉则一并修掉。

## Risks

- HButton ghost 的 `@media (hover: hover)` 浅灰 hover 可能在桌面预览「闪白底」——需用更高优先级覆盖。
- 仅 roman、无 translated 的曲是否算「有译」：按现有开关会清 roman，**算有译** 更一致。

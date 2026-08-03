# 实现清单 — 歌词页 FAB 样式与翻译可见性

## 步骤

1. **`PlayerPage.vue` 翻译可见性**
   - 增加 `hasLyricTranslation`（`lyricLines` 任一行非空 `translatedLyric`/`romanLyric`，或 `lyricsTranslation` 非空）。
   - 翻译 `h-button`：`v-if="hasLyricTranslation"`。
   - 浮动容器：无翻译且（宽屏或无播放键）时避免空可点区；有播放或有译才需要 chrome 内容。
   - 清理 FAB class 上无效的 `[--color]`/`[--background]` Ionic 变量，改真实色或交给全局 CSS。

2. **`tailwind.css` 歌词 FAB 色**
   - `.player-overlay .lyric-fab` 设置 `color` + `background`（浅字、半透明底）。
   - `.is-active` / `:not(.is-active)` / hover / active 覆盖 HButton ghost 默认黑字与浅灰 hover。
   - 删除或停用仅写 `--color`/`--background` 的无效规则。

3. **横屏/活动显隐**
   - 复查：无规则在 landscape 隐藏 `.lyric-floating-actions`；宽屏仅 `display:none` 播放键。
   - 浅色修复后目视横屏交互应能看见翻译键；若 pointer 路径有问题再修 `onLyricPanelPointerUp`。

4. **Spec**
   - `component-guidelines`：FAB 用真实 color、有译才出翻译键、chrome 显隐契约。
   - `features-player`：同步「无译不展示翻译开关」。

5. **验证**
   - lint / build；逻辑上有译/无译分支；尽量不改无关播放路径。

## 回滚

- 还原 `PlayerPage.vue`、`tailwind.css`、相关 spec 段落。

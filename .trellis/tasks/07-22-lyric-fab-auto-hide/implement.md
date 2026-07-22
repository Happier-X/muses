# 实现清单：歌词页浮动按钮按需显示

## 1. 状态与 API（`PlayerPage.vue`）

- [x] 增加 `lyricChromeVisible`、`LYRIC_FAB_IDLE_MS = 3000`、idle timer 句柄
- [x] `revealLyricChrome()`：置 true + 清旧 timer + 新设 3s hide
- [x] `hideLyricChromeImmediate()`：置 false + 清 timer
- [x] `scheduleLyricChromeHide()` 抽取

## 2. 绑定触发

- [x] 歌词面板：在不破坏现有 root touch 链的前提下挂 reveal（优先复用 `isLyricPanelTarget` / touch 路径）
- [x] 纵向滑动浏览歌词时 reveal
- [x] 轻点（非横滑阈值、非 dismiss）reveal
- [x] fab 的 `@click` 内调用 reveal 重置计时
- [x] `watch(activePanel)` / `watch(playerOverlayVisible)` / `onUnmounted` 清状态

## 3. 样式

- [x] `.lyric-floating-actions` 增加可见 class（如 `.is-visible`）
- [x] 默认 opacity 0 + pointer-events none；`.is-visible` opacity 1
- [x] transition ~180ms；子按钮显示态可点

## 4. 测试

- [x] `tests/unit/player.spec.ts`：初始隐藏、交互显示、3s 隐藏、离面板隐藏
- [x] 既有翻译/平板/播放键用例不回归

## 5. 规范

- [x] 更新 `.trellis/spec/frontend/component-guidelines.md` 歌词浮动 chrome 约定

## 验证命令

```bash
npm run test:unit -- --run tests/unit/player.spec.ts
npm run lint
npm run build
```

## 回滚

还原 `PlayerPage.vue` 浮动区始终可见行为与相关测试/规范段落。

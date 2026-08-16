# PRD：navbar 与工具条改纯灰背景（对齐椒盐音乐）

## 背景

当前 Muses 顶部 navbar（`MNavbar`）和歌曲页工具条（subnavbar）使用"液态玻璃"配方：
半透明白 `rgba(255,255,255,0.6)` + `backdrop-filter: blur(20px)` + 内高光。
内容滚动时列表从其后透出糊影，视觉上是"白玻璃"，与椒盐音乐（Salt Player）不一致。

椒盐音乐顶栏是**与列表同色的纯色 subBackground**（浅色 `#F9F9F9` / 深色 `#262626`），
无模糊、无内高光。用户要求对齐。

## 需求

1. 顶部 navbar 背景改为与下方列表一致的纯灰色（`--m-surface-1`），去掉 blur 与内高光玻璃质感。
2. 歌曲页工具条 / 搜索栏（subnavbar 内的工具条）与 navbar 视觉一体，同色纯灰。
3. 深色主题同步：navbar 背景 `--m-surface-1`（dark = `#262626`）。

## 约束与边界（不做）

- 底部 MTabbar 已是 `--m-surface-1` 纯色，不改。
- FAB、MiniPlayer 胶囊仍用玻璃配方（`--m-glass-bg`），**不改**全局变量，避免影响它们。
- 不改 `--m-glass-bg` / `--m-surface-*` 等主题变量本身。
- 保留 `--transparent` 变体（PlayerPage 沉浸式）逻辑。

## 验收标准

- 浅色主题下 navbar、工具条背景与列表（`--m-surface-1` #f9f9f9）同色，无模糊透出。
- 深色主题下同色跟随（#262626），无浅色高光残留。
- 滚动列表时顶栏不再透出内容糊影。
- `--transparent` 页面（播放器沉浸式）无回归。
- `npm run lint` 通过。
# PRD：navbar 背景修正为列表底色（--m-surface）

## 背景（上轮验收失败）

08-16-navbar-toolbar-solid-surface 把 navbar/工具条背景改为 `--m-surface-1`（浅 #f9f9f9）。
MuMu 实机像素验证（screencap + 颜色采样）发现：顶部 navbar 区域为 (249,249,249)，
而下方歌曲列表区域为 (243,243,243) = `--m-surface`（#f3f3f3）——存在肉眼可见色差，
navbar 偏白。原因：SongsPage 自建虚拟列表（非 MList 组件）底色直接透出 body 的
`--m-surface`，用户期望"与下面列表同色"指的是 #f3f3f3，而非 MList 卡片色 #f9f9f9。

## 需求

1. MNavbar 默认背景：`--m-surface-1` → `--m-surface`（浅 #f3f3f3 / 深 #202020），
   与歌曲列表底色完全一致。
2. SongsPage navbar/工具条覆盖与深色规则同步改 `--m-surface`。
3. 深色主题随 `--m-surface` dark 值（#202020）。

## 约束与边界

- 底部 MTabbar 保持 `--m-surface-1`（椒盐底部导航为 subBackground，用户未提及，不动）。
- FAB / MiniPlayer 液态玻璃不动；`--transparent` 变体无回归。

## 验收标准

- MuMu 截图采样：navbar/工具条区域颜色与列表区域一致（均 #f3f3f3，差 ≤1 通道）。
- 深色主题同色跟随无高光；
- lint + vue-tsc 通过；重新 cap sync + assembleDebug + 安装验证。
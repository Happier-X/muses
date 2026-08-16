# PRD：navbar 灰底磨砂玻璃（半透明灰 + blur 恢复滚动磨砂）

## 背景（两轮迭代后的用户期望）

- 08-16-navbar-toolbar-solid-surface：玻璃 → 实心 surface-1（用户嫌不灰，且看的是旧产物）
- 08-16-navbar-bg-match-list-surface：实心 --m-surface（#f3f3f3），与列表一致 ✅
- 本轮用户反馈：**列表滚动经过 navbar/工具条下方时没有磨砂玻璃效果了**

用户期望的组合 = 椒盐 Liquid Glass：**基底灰（与列表同色）+ 半透明 + blur 磨砂**。
静止时顶栏看起来是与列表一致的灰；滚动时内容透出模糊磨砂。

## 需求

1. 新增主题变量 `--m-navbar-glass-bg`：浅色 `rgba(243, 243, 243, 0.8)`（--m-surface 灰的 0.8 alpha）、
   深色 `rgba(32, 32, 32, 0.8)`；**不动 `--m-glass-bg`**（MiniPlayer/FAB 白色玻璃继续用）。
2. MNavbar：`background: var(--m-navbar-glass-bg)` + `blur(20px)`（-webkit 前缀齐全）+ 顶部内高光
   （浅 `inset 0 1px 0 rgba(255,255,255,0.65)` / 深 `rgba(255,255,255,0.1)`），恢复液态玻璃质感。
3. SongsPage navbar 覆盖与深色规则同步（`--m-surface` → `--m-navbar-glass-bg` + blur）。
4. 保留 `--transparent` 变体；SongsPage border-bottom: none 保持。

## 约束与边界

- MiniPlayer / FAB 仍用 `--m-glass-bg` 白玻璃，**不改**。
- 底部 MTabbar 保持实心 `--m-surface-1`。
- 深色覆盖 scoped 写法遵守 spec（`:global(.dark ...)` 完整选择器，特异性 (0,3,0)）。

## 验收标准

- MuMu 截图：滚动列表至中部后，navbar/工具条区域内出现模糊后的内容色痕迹
  （非纯 #f3f3f3，且比实心时"柔和"）；静止顶部时顶栏仍为 #f3f3f3 灰。
- 深色主题同色跟随、无白雾（基色灰而非白）。
- lint + vue-tsc 通过；cap sync + assembleDebug + 安装验证。
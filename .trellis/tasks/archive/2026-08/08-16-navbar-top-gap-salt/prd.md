# navbar 顶部间距对齐椒盐（去掉状态栏呼吸间距）

## 背景

用户真机对比：muses 的 MNavbar 顶部距状态栏比椒盐音乐大，要求保持一致。

现状（08-15-navbar-status-bar-spacing 引入）：

```scss
--m-navbar-pt: max(16px, calc(var(--m-safe-area-top, 0px) + 8px));
```

- 真机间距 = 状态栏外 8px（其他页）；歌曲页再 +6px = 14px（SongsPage `+6px` 差值，自称"22px 椒盐基准"）。

## 实测依据（MuMu 1080x1920，density 3，与 muses 截图同环境交叉验证）

- 椒盐状态栏文字带 y16-53 与 muses 截图完全一致 → 同一模拟器/缩放，可直接对比。
- 椒盐：状态栏 24dp（y0-72px）；navbar 行 56dp **紧贴状态栏下沿（间距 0）**，行中心 52dp（汉堡图标中心 y≈155px=51.8dp ✓）。
- muses：行顶 = 状态栏 + pt；歌曲页 pt=22px 时行中心 y≈204px=68dp ✓ 与公式吻合。
- 结论：**椒盐间距 = 0**。此前 `+8px` 呼吸间距与 SongsPage `+6px`（22px）均源于把"图标在行内的位置"（56dp 行内 24dp 图标垂直居中，图标顶距行顶 ~16dp）误判为行间距。

## 改动

| 文件 | 内容 |
|---|---|
| `src/theme/index.scss:245` | `--m-navbar-pt: max(16px, calc(var(--m-safe-area-top, 0px) + 8px))` → `max(16px, var(--m-safe-area-top, 0px))`（去掉 +8px；浏览器 safe-area=0 保留 16px 兜底） |
| `src/views/SongsPage.vue` | 4 处去掉 `+6px`：`__navbar-wrap :deep(.m-navbar)` padding-top、`__empty`/`__list` padding-top、`__index-bar` top（`calc(var(--m-navbar-pt) + 44px + 48px)`） |
| `.trellis/spec/frontend/component-guidelines.md` | 安全区契约更新：token 新值 + 实测数据 + 禁止再写 `+8px`/`+6px` 差值；SongsPage 工具条合并条目总高 114→108、公式同步 |

## 验收标准

- [ ] 真机/模拟器：所有页面 navbar 行顶紧贴状态栏（0 间距），与椒盐一致；歌曲页工具条/列表/索引条无错位。
- [ ] 浏览器（safe-area=0）：navbar 顶部 16px 不变，其余页面布局不变。
- [ ] `npm run build` 通过；`npm run lint` 无新增报错。

## 非目标

- 不改 MNavbar 44px 内容行高度（椒盐为 56dp，属既有设计，用户未要求）。
- 不改 SourcesPage 内容区 `+8px` 页面留白（语义不同）。
- 不改底部安全区、MiniPlayer 等无关几何。

# 差距清单 — Web 宽屏形态 vs native 现状

> 盘点口径：13 个文件的 `@media(min-width: 768px)` 段 + PlayerPage.vue isTabletLayout 分支，逐项对照 native Compose / amll-web。
> 注：Web token `--m-content-pb` 与 `--m-content-pb-md` 同值（均 = 72px + safe-bottom），故各页 padding-bottom 覆盖段无视觉差异，native 无需改动。

## 需要修的差距

| # | 差距 | Web 规格 | native/amll-web 现状 | 处理 |
|---|------|----------|---------------------|------|
| G1 | **平板 aside 形态下 MiniPlayer 缺失** | MiniPlayer.vue 无平板覆盖段 → 平板仍显示（fixed left/right 18px 全宽胶囊） | `TabsLayout.TabletLayout` 未接收/渲染 `bottomBar`，平板下 MiniPlayer 消失 | TabletLayout 增加 bottomBar 参数并对齐 BottomCenter 渲染 |
| G2 | **专辑/艺术家页网格列数** | `grid-template-columns: repeat(auto-fill, minmax(180px, 1fr))` | `GridCells.Fixed(2)` 恒两列 | screenWidthDp≥768 时改 `GridCells.Adaptive(180.dp)` |
| G3 | **播放页 isTabletLayout 下行缺失** | PlayerPage.vue：`viewportWidth>=768 && viewportHeight<viewportWidth` 启用平板分支 | updatePlayerState payload 无该字段 | Kotlin 判定后 payload 加 `isTabletLayout`；判定口径对齐 Web（含横屏条件） |
| G4 | **amll-web 平板分支 UI** | 对照 player-page--tablet：①固定头部隐藏+面板内头部显示 ②封面垂直居中 ③五行歌词小窗隐藏 ④歌词 FAB 组**不显示播放键** ⑤进度/三键/mode 移到底部全宽控制条（渐变底） | amll-web 只有手机形态结构 | main.ts 加 applyTabletLayout（类切换 + DOM 迁移）；style.css 加 .pp-tablet 规则组 |

## 核对一致、无需改动的项

- **TabsLayout aside 细节**：260dp/surface-1/hairline/statusBarsPadding/纵向滚动 ✓；nav-link min-height 64/icon 壳 60/文字 16px ✓；激活态与普通项一致（08-16 定案）✓；次组 9+9dp 间距 + border-top hairline ✓
- **覆盖路由全屏**：navVisible=false 时全屏 content，不受 aside 影响 ✓（MiniPlayer 在覆盖路由隐藏与 Web popup 盖过 z 序一致）
- **SaltNavbar 汉堡按钮**：Web MNavbar ≥768px 隐藏 `__left--drawer`；native TabletLayout 不提供 LocalSaltOpenDrawer → 内建汉堡自动不渲染 ✓
- **各列表页 content-pb 覆盖段**（Songs/Playlists/PlaylistDetail/LibraryDetail/Settings/Sources/WebDav×2）：pb 与 pb-md 同值，native 96dp 留白维持手机冻结值 ✓
- **MPopup 平板 640×640 居中**：仅非 fullscreen 弹层生效；native 侧弹层均为覆盖路由（fullscreen 保持全屏）或 M3 ModalBottomSheet/actions（对应 m-sheet/m-actions，无平板覆盖段）✓

## MuMu 平板分辨率实测

1280×800（横屏）→ aside + 自适应网格 + 播放页平板分支应全部生效；待用户装机验收。

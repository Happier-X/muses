# Salt UI 复刻 — 技术设计

> 前置：父任务 design.md §4（Salt 视觉体系）；M2 已交付 AMLL WebView 层。本设计的核心是「翻译方法论 + 组件映射表」，像素细节以 Vue 源码为准，不在本文重复。

## 1. 翻译流水线

```
.vue 模板结构        →  Composable 函数分解（一 BEM 块 = 一私有 @Composable）
index.scss 令牌      →  SaltTheme 设计令牌（Color/Type/Spacing，明暗双套）
m-* 组件 props/slot  →  映射组件参数 + content lambda
交互态 (:class 绑定)  →  Compose state 驱动
```

规则：
- SCSS 数值直接换算（px→dp 不换算系数；颜色/透明度原样抄）
- `.vue` 里 `v-if/v-for` → Kotlin 条件/list 映射；`:class` 条件态 → 参数化样式
- **禁止**用 Material3 默认观感做最终样式：Material 组件仅当行为基座（如 Scaffold 结构），视觉全走自绘或映射组件

## 2. 设计令牌层（P0）

`theme/index.scss` 的 CSS 变量体系 → `SaltTheme` 扩展：

| SCSS 变量族 | Compose 归宿 |
|---|---|
| `--m-glass-bg / --m-navbar-glass-bg / --m-glass-light/dark` | `SaltColors.glass*`（明暗双套） |
| `--m-shadow-ios-*-glass(-thumb/-fab)` | Modifier 画阴影的参数对象（Compose 无多层 box-shadow，需绘制方案） |
| 字号/字重/圆角 | `SaltTypeAndShape` 扩展 |

- 明暗主题：SCSS 的暗色变量块 → `isSystemInDarkTheme()` 切换两套 `SaltColors`

## 3. m-* 组件映射表（P0 交付物）

按页面需求分期实现，全部放 `native/app/.../theme/` 或新 `core:ui`/`feature:common` 模块（决策：先放 app 模块 `ui/` 包，避免过早抽模块；跨 feature 复用时再下沉）：

| Web 组件 | Compose 映射 | 首个使用页 |
|---|---|---|
| MNavbar（含 subnavbar 插槽） | `SaltNavbar(title, right, subnavbar)` | SongsPage |
| MIconButton | `SaltIconButton` | 全局 |
| MButton(variant=clear) | `SaltTextButton` | SongsPage 搜索取消 |
| MList/MListItem | `SaltListItem`（虚拟列表行） | SongsPage |
| MCover | `SaltCover(uri, size, fallback)` | 歌曲行 |
| MEmpty | `SaltEmpty(title, description)` | 各空态 |
| MFab | `SaltFab` | 播放页 FAB 组 |
| MSheet | `SaltBottomSheet` | AddToPlaylistSheet 改造 |
| MDialog/MDialogButton | `SaltDialog` | 播放列表重命名等 |
| MPopup | 播放页容器（M1 已有导航形态，对照复刻） | PlayerPage |
| MiniPlayer | `MiniPlayerBar` | TabsPage 底部 |
| 其余（MToggle/MRange/MSegmented…） | 用到时按同法映射 | — |

## 4. 页面级要点（各批次设计输入）

- **TabsPage**：宽屏 aside 固定侧边栏 / 窄屏 drawer（M1 已有骨架，对齐 nav-link 图标壳+激活态样式）；底部 MiniPlayerBar 叠加
- **SongsPage**：navbar+subnavbar 同一块玻璃（无分界线，注释明确）；工具条=随机播放按钮+歌曲总数；搜索栏替换工具条；虚拟滚动列表（Compose LazyColumn 天然对应）；多选模式计数条
- **PlayerPage（P4 压轴）**：AMLL WebView 已是最底层背景——复刻的是其上的信息层/控制层/歌词面板切换、固定头部、下滑手势关闭、FAB 组；3430 行逐段翻译，拆成多个子任务执行
- **Playlists/Queue/Albums/Artists/LibraryDetail**：同 navbar+list 范式，量小

## 5. 与既有代码的关系

- M1/M2 的 ViewModel/Repository/数据链路**不动**；只换 UI 树
- M2 的 AmllWebView 在 P4 时嵌入复刻后的播放页（当前装配位置可能调整）
- feature:playlist/library 现有页面在各自批次被覆盖改造

## 6. 回滚

每批次独立 commit；某页复刻不满意可单独 revert 该批次，不影响其他页面与数据层。

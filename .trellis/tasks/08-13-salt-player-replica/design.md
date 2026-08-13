# Design: 复刻 Salt Player UI（视觉风格层）

## 1. 目标与边界

视觉风格层改造：改 `--m-*` token 值与组件/页面 scoped scss，**零逻辑改动、零 DOM 结构改动、零组件 API 改动**。保持 28 个 m-* 组件与 14 个页面骨架不变。

## 2. SaltUI 精确设计规范（调研取证）

来源：SaltUI 开源仓库 `ui2/`（已 clone 取证，关键文件 `SaltColors.kt` / `SaltDimens.kt` / `SaltShapes.android.kt` / `SaltTheme.kt`）。

### 2.1 颜色 token（SaltColors.kt 默认值）

| 语义 | Light | Dark |
|------|-------|------|
| highlight（主色） | `#0470E6` | `#0088FF` |
| text（主文字） | `#1E1715` | `#EBEEF1` |
| subText（次要文字） | `#8C8C8C` | `#BFE1E6EB`（75% 白） |
| background（页面底） | `#F3F3F3`（Android）/ iOS 平台 `#F2F2F7` | `#202020`（Android）/ iOS 平台 `#000000` |
| subBackground（卡片/导航底） | `#80FFFFFF`（50% 白） | `#08FFFFFF`（3% 白） |
| stroke（分割线/描边） | subText @ 15% 透明度 | subText @ 10% 透明度 |
| onHighlight（主色上文字/图标） | White | White |

注意：SaltUI 区分 OS 平台（Android vs iOS）。Muses 目标是 Android 为主（MuMu/电容），但保留 light/dark 双轨即可，不按平台切换（取 Android 默认值，视觉一致性好）。

### 2.2 圆角（SaltShapes.android.kt + SaltDimens.kt）

| token | 值 |
|-------|-----|
| small | 8dp |
| medium | 16dp |
| large | 24dp |
| 卡片 corner | 12dp |
| dialogCorner | 20dp |
| 列表行高 item | 56dp（Android） |
| 行图标 itemIcon | 24dp |

### 2.3 间距与排版

- padding = 16dp、subPadding = 12dp
- 字号：12sp（小）、16sp（正文）、24sp（大标题）
- 列表 RoundedColumnInListItemPadding = 3dp、EdgePadding = 5dp

## 3. Muses 现有 token 对照（当前 iOS 值 → Salt 值）

在 `src/theme/index.scss` 的 `:root` 与 `.dark` 中做映射替换（**只改值，变量名不变**，保护所有调用面）：

| 现有变量 | 当前 iOS 值 | 目标 Salt 值（light） | 目标 Salt 值（dark） |
|----------|------------|----------------------|---------------------|
| `--m-primary` | `#007aff` | `#0470E6` | `#0088FF` |
| `--m-primary-tint/shade` | iOS 算法 | 沿用 Salt highlight 的 tint/shade 推导（或同色微调） | 同左 |
| `--m-surface` | `#efeff4` | `#F3F3F3` | `#202020` |
| `--m-surface-1` | `#fff` | `#80FFFFFF over #F3F3F3`（≈`#F9F9F9` 实算） | `#08FFFFFF over #202020`（≈`#262626` 实算） |
| `--m-surface-2` | `#f7f7f8` | 略深于 surface-1（`#ECECEC`） | 略深（`#2B2B2B`） |
| `--m-text` | `#000` | `#1E1715` | `#EBEEF1` |
| `--m-text-2` | `rgba(60,60,67,.6)` | `#8C8C8C` | `#BFE1E6EB` |
| `--m-text-secondary` | `rgba(0,0,0,.55)` | `#8C8C8C` | `#BFE1E6EB` |
| `--m-hairline` | iOS 分割线 | `subText@15%`（light）/`subText@10%`（dark） | 同左 |
| `--m-danger/success` | iOS 原值 | 保留（SaltUI 有对应 palette，可微调） | 同左 |

新增 token（不动现有调用面，供新样式使用）：
- `--m-radius-sm: 8px; --m-radius-md: 16px; --m-radius-lg: 24px; --m-radius-card: 12px; --m-radius-dialog: 20px;`
- `--m-list-row-h: 56px; --m-list-icon: 24px; --m-spacing: 16px; --m-spacing-sub: 12px;`

## 4. 组件改造点（28 个 m-*，scoped scss 内改样式）

| 组件 | 改什么 |
|------|--------|
| MButton | 圆角 → 16dp(medium)，primary 用 `--m-primary`，text 色 `#1E1715`/`#EBEEF1`，disabled 用 stroke |
| MCard | 圆角 → 12dp，背景 subBackground，描边 stroke 可选 |
| MList/MListItem | 行高 56px，分割线 hairline→stroke，inset 圆角 12dp |
| MListInput | 圆角 12dp，背景 surface-1，边框 stroke |
| MNavbar | 去玻璃（surface 渐变+blur+mask）→ 干净 subBackground/表面色 + 底部 hairline；圆角无 |
| MTabbar/MTabbarLink | 玻璃胶囊 → 干净表面 + 高亮用 highlight 色（激活 item 高亮/图标色） |
| MDialog/MDialogButton | 圆角 20dp，背景 popup 色，按钮 tonal（highlight 15% 底） |
| MSheet | 圆角 large(24dp) 或 dialog(20dp)，背景 subBackground |
| MActions | 圆角 20dp，列表项 56px，danger 保留 |
| MPopup | 全屏（无圆角）或 md 640×640 圆角 24dp |
| MToggle | 开态 highlight 色（替代成功绿或保持），thumb 白 |
| MCheckbox | 勾选 highlight |
| MRange | 填充 highlight，thumb highlight |
| MSegmented | 激活段 highlight 底 + onHighlight 字 |
| MToast | 背景 subBackground/popup 深色，文字 text |
| MPreloader | 主色 highlight |
| MFab | 圆角 large(24dp) 或圆形，highlight 底白图标 |
| MNavbarBackLink | 颜色 text |

## 5. 页面改造点（14 个，scoped scss）

- **统一原则**：去 iOS 玻璃（`.m-glass-blur-*`/mask 渐显在导航/吸顶条的使用）→ 干净表面色 + hairline；配色 token 随 theme 自动变
- **TabsPage**：底部导航玻璃胶囊 → 干净表面 + 激活 highlight
- **Albums/Artists 卡片网格**：卡片圆角 12dp、行高对齐、封面圆角微调
- **SongsPage 吸顶随机条**：玻璃条 → 干净表面（或保留轻 blur，先做干净版）
- **PlayerPage**：深底白字保留（播放器本身深色沉浸是 Salt 风格的一部分——Salt 播放页也是暗色背景+动态流光），但进度条/主控色改 highlight；编辑弹窗（MSheet/MDialog）圆角对齐
- **QueuePage/SourcesPage 等虚拟列表**：行高 56px、分割线 stroke
- **SettingsPage 列表**：行高 56px、toggle highlight
- **MiniPlayer**：玻璃胶囊 → 干净 surface 底 + hairline 顶线（或保留轻玻璃，需对比 Salt 迷你条样式——Salt 用干净深色/表面色小条）

## 6. 兼容与风险

- **变量名不变**：全部页面/组件引用 `var(--m-*)` 的调用面零改动，只改 `:root`/`.dark` 赋值——低风险、可整体回滚（git revert 单文件）
- **玻璃 CSS 类保留**：`.m-glass-*` 等类保留不删（某些浮层/临时 UI 可能仍用），但导航/吸顶等常驻条迁移到干净表面。若页面仍有依赖玻璃的 DOM，先视觉检查再决定
- **回滚点**：阶段 1（token）独立提交；阶段 2（组件）独立提交；阶段 3（页面）分页提交——任一阶段可单独 revert
- **WebView<111 兼容**：继续避免 oklab 渐变插值、保留 `-webkit-` 前缀；Salt 风格玻璃更少，反而降低此风险
- **深色模式**：保留 `.dark` class 驱动机制不变

## 7. 不做的事（明确）

- 不动 script（逻辑）、不动模板 DOM 结构、不动组件 props/events
- 不做动态取色引擎（Material You）——token 结构预留 `--m-primary` 可变即可，后续可接
- 不做流光/新页面/新功能

# 复刻 Salt Player 椒盐音乐 UI（视觉风格层）

## Goal

将 Muses（Vue 3 + Capacitor 本地音乐播放器）的**视觉语言**从当前 iOS（Konsta 衍生）风格切换为 **Salt Player（椒盐音乐）** 风格——基于其开源组件库 SaltUI 的精确设计 token（颜色/圆角/间距/排版），在不改变任何业务逻辑与交互行为的前提下，重塑所有页面与组件的观感。目标是「一眼识别为椒盐音乐的审美」，而非像素级 1:1 克隆（后者因源 App 闭源无法做到）。

**用户价值**：Muses 功能已完备（本地音乐扫描/管理/播放/歌词/队列），缺的是符合用户审美的视觉呈现。复刻 Salt 的极简、清爽、低干扰、动态取色美学，让自研应用拥有专业播放器的气质。

## Background（已确认事实）

- Muses 技术栈：Vue 3.5 + Vite + Capacitor 8；28 个自研 `m-*` 组件 + scoped SCSS（`--m-*` 变量体系，刚完成 Konsta→自研迁移）
- 当前视觉：iOS 风格——系统蓝 `#007aff`、表面 `#efeff4`/`#000`、玻璃毛玻璃、安全区、圆角偏 iOS
- SaltUI（开源，Apache/LGPL）：Compose Multiplatform 组件库，源自 Salt Player
- **SaltUI 精确设计 token（调研获得，见 design.md）**：
  - 圆角：small=8dp、medium=16dp、large=24dp、卡片=12dp、dialog=20dp
  - 列表行 56dp、图标 24dp、间距 16dp/12dp
  - 颜色 Light：highlight `#0470E6`、text `#1E1715`、subText `#8C8C8C`、bg `#F3F3F3`
  - 颜色 Dark：highlight `#0088FF`、text `#EBEEF1`、subText `#BFE1E6EB`、bg `#202020`
  - 支持动态取色（Material You）
- Salt Player 产品特征（影响视觉布局）：极简无首页推荐、记忆式简易首页、主页/播放界面分控主题、动态流光、媒体库分类（专辑/艺术家）、层次/平铺文件夹浏览

## Scope（In Scope）

方案 A：**视觉风格层**。改造对象为：

1. **主题 token**：`src/theme/index.scss` 的 `--m-*` 变量改 Salt 色值（含 light/dark 双套），新增 Salt 圆角/间距/字体 token
2. **28 个 m-* 组件的 scoped 样式**：圆角、颜色、尺寸对齐 Salt 规范（按钮/列表/卡片/弹窗/分段/开关/滑块/导航/播放器控件）
3. **页面布局观感**：14 个页面调整配色与圆角、列表行高、卡片圆角、导航栏样式（去 iOS 玻璃感，改 Salt 的干净表面风格）
4. **播放器页**：进度条/控制键/编辑弹窗等改 Salt 视觉（含分控主题色概念——播放器可用独立 highlight）
5. **图标保持 @lucide/vue**（只改尺寸/颜色语义）

## Out of Scope（明确不做）

- ❌ 不改任何业务逻辑、数据流、路由、组件 API、事件契约
- ❌ 不做像素级克隆；不做动态壁纸取色（Material You 实时取色引擎）——本期仅固定 Salt 默认色值 + 预留 token 结构
- ❌ 不做 Salt 特有新功能/新页面（桌面端、小部件、文件夹浏览树、统计页、沉浸模式长按、DLNA 等）
- ❌ 不引入 Compose/Kotlin/SaltUI 依赖
- ❌ 不重做布局结构（不改 DOM 骨架、不改页面分区），只改视觉样式
- ❌ 不做流光（SALT FX 动态背景效果）——超出视觉风格层范围，且 Pixi 已有类似能力可后续评估

## Requirements

- **R1 主题 token 迁移**：`:root`/`.dark` 的 `--m-surface/--m-surface-1/--m-text/--m-text-2/--m-primary/...` 改 Salt 精确色值；新增 `--m-radius-*`、`--m-spacing-*`、`--m-font-size-*`、`--m-stroke` 等 token；保留 `--m-safe-area-*`/`--m-danger` 等既有语义变量名（只改值，不破坏调用面）
- **R2 组件视觉对齐**：MButton/MCard/MList/MListItem/MDialog/MSheet/MActions/MTabbar/MToggle/MCheckbox/MRange/MSegmented/MPopup/MToast/MNavbar 等样式改 Salt 圆角+色值（scoped scss 内改）
- **R3 页面观感对齐**：14 页面的 scoped scss 配色/圆角/行高/分割线改 Salt 风格；导航栏从玻璃感改为 Salt 干净表面
- **R4 播放器分控主题**：PlayerPage 进度条/主控/编辑弹窗适配 Salt 视觉；可独立于主页设置 highlight 色（本期先统一用默认 highlight，结构上预留）
- **R5 图标语义**：@lucide/vue 图标尺寸/颜色沿用 Salt 规范（如列表行图标 24px）

## Acceptance Criteria

- [ ] AC1：`npm run build` 通过，`npm run lint` 无 error
- [ ] AC2：`:root`/`.dark` 变量全部为 Salt 色值（spot-check `--m-primary=#0470E6`（light）/`#0088FF`（dark）、`--m-surface=#F3F3F3`（light）/`#202020`（dark）、text `#1E1715`/`#EBEEF1`）
- [ ] AC3：28 个组件 scoped scss 圆角/色值对齐 Salt（spot-check 按钮圆角 16dp、卡片 12dp、dialog 20dp、列表行高 56px）
- [ ] AC4：14 个页面观感检查通过——在 MuMu 模拟器上逐页浏览，无「iOS 蓝/玻璃」残留观感，配色为 Salt 风格
- [ ] AC5：业务功能零回归——播放/队列/歌词/编辑/扫描/虚拟列表全部可用（手动验证 MuMu 全流程）
- [ ] AC6：播放器页进度条/主控/编辑弹窗为 Salt 视觉
- [ ] AC7：无 `<k-`/tailwind 类/konsta 残留（保持零组件库依赖）

## Notes

- 视觉对照依据：SaltUI 开源源码（已调研取证）+ Salt Player 公开界面信息；允许迭代微调（先上 token，再逐页打磨）
- 复杂度判定：中等偏复杂（跨全部组件+页面但模式统一、无新逻辑）——需要 design.md + implement.md

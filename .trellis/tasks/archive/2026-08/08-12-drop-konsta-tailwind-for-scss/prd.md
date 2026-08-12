# 去组件库与 Tailwind、全量迁移 SCSS

## Goal

移除项目对 Konsta UI 组件库与 Tailwind CSS 的全部依赖，样式体系整体迁移为 SCSS（组件 scoped / 全局共享变量与工具），保留既有视觉还原度（iOS 风格、玻璃效果、暗色模式、安全区）与全部已修复的行为细节（层级、滚动、过渡）。

用户价值：消除对第三方 UI 框架的耦合，样式完全自持（可维护、可定制、打包更小）；不再依赖 tailwind 编译管道（现有 WebView 兼容性 hack 收敛为自研 CSS）。

## 已确认事实（repository evidence）

- 框架：Vue 3.5 + Vite 8 + Capacitor 8（Android WebView 为主，兼容 Chrome >= 67 / WebView 110 级旧引擎）。
- 依赖：`konsta@5.3.0`（`konsta/vue`）、`tailwindcss@4.3.3` + `@tailwindcss/vite`、`motion-v`（新增，用户指定动画引擎）、`postcss.config.js`（命名 layerCompat 的 PostCSS 插件，仅为解包 Tailwind `@layer` 而存在）。
- 当前无 sass 依赖。
- Konsta 模板组件实际使用 23 种、约 150 个标签（`grep -rhoE '<k-[a-z-]+' src --include="*.vue"`）：
  - 结构：`k-app`×1、`k-page`×11（含 MPage）、`k-navbar`×7（含 MPage）、`k-navbar-back-link`×2、`k-toolbar-pane`×1、`k-block-title`×2、`k-card`×1、`k-popup`×2、`k-sheet`×2
  - 列表：`k-list`×9、`k-list-item`×8、`k-list-input`×15
  - 交互：`k-button`×43、`k-checkbox`×5、`k-toggle`×2、`k-range`×1、`k-segmented`×1、`k-segmented-button`×1、`k-tabbar`×1、`k-tabbar-link`×1、`k-fab`×2、`k-preloader`×1
  - 浮层：`k-actions`×5、`k-actions-group`×5、`k-actions-label`×5、`k-actions-button`×14、`k-dialog`×7、`k-dialog-button`×8、`k-toast`×3
- 入口：`src/theme/tailwind.css`（`@import 'tailwindcss'` + `@import 'konsta/vue/theme.css'`），其中约 2/3 是沉淀的手写修复 CSS（`.player-overlay` 全套、`.m-page/.m-content`、弹层层级阶梯 z-index、玻璃修复、安全区桥接、`.k-toast` 文字色），这些必须原样保留。
- Konsta 主题机制：`plugin-colors.js`（Tailwind 插件）从 `--color-brand-primary`(#007aff) 生成 `--k-color-*` 变量；Tailwind `@theme` 生成 `--color-ios-*` 变量；暗色由 `document.documentElement` 的 `.dark` class 驱动（`src/composables/useSystemDark.ts` 已实现，行为保留）。
- 项目实际引用的主题 token：`--color-ios-light-surface`、`--color-ios-light-surface-1`、`--color-ios-dark-surface`、`--color-ios-dark-surface-1`（body 背景/文字，tailwind.css:25-33）、`from-ios-light-surface` / `dark:from-ios-dark-surface/50`（MiniPlayer 渐变）；`ring-primary`（PlayerPage 云封面选中，`--color-primary` 链）、`--k-safe-area-*` + `safe-areas`/`pt-safe-*`/`pb-safe-*`/`bottom-safe-*`（安全区）。
- Tailwind class 约 300 个唯一 token，分布在 18 个 vue 文件；包含大量任意值（`[12px]`）、`dark:` 变体、`md:` 断点变体、`size-*`、`bg-black/5` 透明度、`[&>img]:` 子选择器变体。
- `@lucide/vue` 为图标库（`src/icons/index.ts` 语义表），非 UI 组件库，不在本次移除范围内。

## Requirements

### R1 依赖移除
- R1.1 移除 `konsta`、`tailwindcss`、`@tailwindcss/vite` 依赖与代码引用；卸载 `postcss.config.js` 的 layerCompat 插件（无 `@layer` 后不再需要）。
- R1.2 引入 `sass`，Vite 通过原生 scss 管道编译（`<style lang="scss">` 与全局 scss 入口）。
- R1.3 入口样式改为 scss（如 `src/theme/index.scss`），保留 tailwind.css 中全部手写修复样式，仅去除 tailwind/konsta 编译产物依赖。

### R2 组件自研替换
- R2.1 对 konsta 23 种使用组件逐一定义自研替代（按页面实际使用的 prop/插槽/事件契约对齐），建议 `m-` 前缀与 `src/components/ui/` 收编；替换后页面模板中不再出现 `k-` 组件。
- R2.2 关键行为契约必须对齐（基于现有页面代码）：`:opened` 控制浮层显隐、`@backdropclick`、遮罩背景、进入/退出过渡动画（统一 motion-v）、浮层层级（沿用 tailwind.css 现有 z-index 阶梯）、body 滚动锁定（沿用现有 `muses-overlay-open` 机制）、Toast 位置与文案。
- R2.3 MPage/MContent 重构为纯自研（当前 MPage 基于 k-page/k-navbar），保持插槽协议（title/start/end/subnavbar/fullscreen）与页面使用方式不变。
- R2.4 全部交互动画（浮层进出、Segmented 滑块、Toggle 拇指、Checkbox 勾选、Preloader 旋转、播放器拖拽回弹/滑出）由 `motion-v` 实现，禁用 CSS keyframes/transition 动画（纯颜色过渡除外）。

### R3 样式体系迁移
- R3.1 全部 Tailwind class 迁移为 scss：页面/组件内部 `<style lang="scss">` 语义化类；高频基础布局与主题变量入全局 scss。
- R3.2 `dark:` 变体 → `.dark` 前缀嵌套（`.dark &` / `:global(.dark)`）；`md:` 断点 → `@media (min-width: 768px)`。
- R3.3 任意值、透明度、子选择器变体按语义转义为具名 scss 类/变量。
- R3.4 主题 token 自持：定义 scss 变量/CSS 变量替代 `--color-ios-*` 与 `--color-primary` 链，亮/暗两套，值沿用 Konsta iOS 主题原值（surface: #efeff4/#000、surface-1: #fff/#1c1c1d、primary: #007aff 等）。
- R3.5 安全区：保留 `--safe-area-inset-*` 桥接与 `--k-safe-area-*` 语义（自研变量，如 `--m-safe-area-top`），保留 env() 兜底，替代组件继续消费。

### R4 行为与视觉不回归
- R4.1 视觉回归校验点：iOS 分组列表（inset/strong/outline）、按钮四种形态（默认/clear/outline/small/rounded/红色）、Actions 弹层、Dialog、Sheet、Popup（全屏播放页/半屏队列页）、Toast（居中）、Tabbar（玻璃+渐变）、Segmented、FAB（跟随跳转气泡定位）、Preloader、Range（播放进度）、Checkbox/Toggle、ListInput（含 error 态）、Navbar 吸顶（玻璃 blur）。
- R4.2 保留既有修复：玻璃效果（blur 2px/8px/16px、渐变方向 `to bottom` 无 oklab 插值、mask 渐显）、弹层层级（1100/1200/1300）、MiniPlayer/随机播放条玻璃、`--content-pb` 滚动止位、`muses-overlay-open` 锁滚动、k-toast 文字色修复。
- R4.3 暗色模式跟随系统行为不变（`.dark` 机制，`useSystemDark` 不动）。
- R4.4 旧 WebView 兼容约束沿用（WebView 110：backdrop-filter 需 -webkit- 前缀与直接值；WebView<111：渐变不解析 oklab 插值）。

### R5 构建与清理
- R5.1 `vite build`（vue-tsc + vite）与 `npm run lint` 通过；产物 CSS 无 tailwind 残留（grep 验证无 `@layer`/tailwind 变量）。
- R5.2 移除后无死代码：konsta 相关 re-export、theme/tailwind.css、postcss.config.js 移除或清空。
- R5.3 包体收益记录到 changelog（konsta+tailwind 依赖树移除）。

## 验收标准（Acceptance Criteria）

- [ ] AC1 依赖清单不再包含 konsta/tailwindcss/@tailwindcss/vite；package.json 含 sass；vite 配置无 tailwind 插件。
- [ ] AC2 `grep -rn "konsta\|k-" src`（组件标签/import）无残留（`k-page` 等类名内自研样式除外）；`src/theme/tailwind.css` 移除。
- [ ] AC3 18 个 vue 文件全部使用 `<style lang="scss">`；无 Tailwind class 残留（`dark:`/`md:`/`[...]` 任意值语法不再出现在模板 class 中）。
- [ ] AC4 自研组件全部就位：按钮/列表/列表项/列表输入/浮层（actions/dialog/sheet/popup/toast）/navbar/tabbar/segmented/checkbox/toggle/range/fab/preloader/card/block-title/toolbar-pane；行为契约（opened、backdropclick、过渡、滚动锁、层级）经交互验证一致。
- [ ] AC5 `vite build`（vue-tsc 类型检查通过）与 `npm run lint` 通过。
- [ ] AC6 在模拟器/真机（或 dev server 旧 Chrome 模式）验证视觉清单 R4.1 全部通过，玻璃/层级/暗色/安全区不回归。
- [ ] AC7 浅色/深色各过一遍主要流程：歌曲列表滚动止位（--content-pb）、播放器全屏进出、队列半屏、编辑歌曲 Sheet、ActionSheet、新建/删除歌单 Dialog、音源管理（Sheet+FAB）、分类分段、设置开关、Toast 展示。
- [ ] AC8 changelog 记录本次重构与包体收益。

## Out of Scope

- 不更换 `@lucide/vue` 图标库（图标非组件库，语义表保持）。
- 不重构页面信息架构与交互逻辑（仅样式体系迁移，[功能] 逻辑代码不动）。
- 不重写 AMLL/歌词渲染、Pixi 相关样式体系。
- 不做视觉重新设计（不改变现有 iOS 风格外观）。
- 不引入 CSS-in-JS、不引入新的 UI 框架。

## Key Decisions

- **组件替代形态（用户已确认，方案 A）**：自研同 API 精简组件集 `m-*` 前缀（MButton/MNavbar/MList/MListItem/MListInput/MActions 家族/MDialog 家族/MSheet/MPopup/MToast/MTabbar 家族/MSegmented 家族/MCheckbox/MToggle/MRange/MFab/MPreloader/MCard/MBlockTitle/MNavbarBackLink 约 20 个），契约对齐现有页面使用面（`:opened`、`@backdropclick`、插槽、事件载荷），页面只改标签与 import。
- **样式迁移形态（用户已确认）**：语义化 scoped scss（页面/组件内写语义类）+ 全局主题变量与 mixin；不再造工具类体系。`dark:`→`.dark` 前缀嵌套、`md:`→`@media(min-width:768px)`、任意值→具名值/变量。
- **动画引擎（用户已确认）**：全部动画统一用 Motion for Vue（`motion-v`，motion.dev/docs/vue）实现：浮层进出（`<AnimatePresence>` + `motion.div`）、Segmented 滑块、Toggle 拇指、Checkbox 勾选、Preloader 无限旋转（`animate(..., { repeat: Infinity })`）、PlayerPage 面板拖拽回弹/滑出。仅纯颜色/背景渐变过渡（hover/active 色变）保留 CSS transition（官方亦推荐轻量场景用 CSS）。
- **技术细节决策（设计文档落地）**：主题 token 自持为 `--m-*` CSS 变量（:root/.dark 双套）；安全区 `--m-safe-area-*` 保留 Capacitor 注入 + env 兜底；z 阶梯 1100/1200/1300 沿用；`.dark` class 暗色机制与 useSystemDark 不动；浮层不新增滚动锁（与现状一致）；MPage/MContent 为死代码直接删除；@lucide/vue 保留。

## Deferred / Risk

- 玻璃/间距/层级等视觉细节回归风险：逐页面迁移 + 每步构建 + 验收清单（AC6/AC7）人工核对；每页面独立 commit 可单独回滚。
- 双轨期（阶段 1-3）index.scss 与 tailwind.css 并存，需保证类名不冲突（既有 .m-page/.player-overlay 等保持原样迁移）。
- 依赖移除（阶段 4）为最后一步，此前任意时刻可回滚。
- motion-v 基于 Web Animations API（WAAPI）：Chrome 69+ 完整支持，项目 legacy 目标 67 与项目实际 WebView 110 均可用；极端旧环境退化策略＝动画跳过、直接显隐（渐进增强，功能不回归）。
- 技术无未知项：konsta 各组件 class/behavior 源码已逐项核对（node_modules/konsta 源码 + 页面全量使用点）；motion-v API（motion.div / AnimatePresence / animate）已查证官方文档。
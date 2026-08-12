# 实施：去组件库与 Tailwind、全量迁移 SCSS

## 阶段总览

```
阶段 1  主题基建（index.scss + sass 引入，双轨并行）
阶段 2  自研组件集（m-* 组件，konsta 并行）
阶段 3  页面逐个迁移（k-* → m-*，class → scoped scss）
阶段 4  依赖移除与清理
阶段 5  全量验证与收尾
```

每阶段结束跑一次构建确认无回归；阶段 3 每个页面一个 commit。

## 阶段 1：主题基建

- [ ] 1.1 `npm i -D sass` 与 `npm i motion-v`
- [ ] 1.2 新建 `src/theme/index.scss`：
  - CSS 变量（:root/.dark 双套，见 design §3.1：--m-* 全量，含 safe-area 桥接、content-pb、阴影 token）
  - mixin 与玻璃配方类（hairline、ellipsis、m-glass-blur-*、m-glass-mask-*）
  - 迁移 tailwind.css 全部手写块：body/#app 高度链、.m-page/.m-content、muses-overlay-open、.player-overlay 全套、.queue-popup-panel、层级阶梯（k-* → m-* 前缀）、.k-toast 文字色（→ .m-toast）、backdrop-blur 手写规则、mask 类、shuffle-glass/mini-player-glass 渐变方向
  - 注意：`.player-overlay` 内的 `progress-range` 覆盖规则暂留（阶段 3 PlayerPage 迁移时按新 MRange 结构清理）
- [ ] 1.3 main.ts 追加 `import './theme/index.scss'`（tailwind.css 暂保留）
- [ ] 1.4 `npm run dev` + `npm run build` 确认：视觉无变化（双轨不冲突）、构建通过

**验证**：`npm run build` 通过；浏览器检查 body 背景/字体不变。

## 阶段 2：自研组件集

按依赖序实现（先原子组件，后浮层）。每个组件：`<template>` 纯结构 + `<style lang="scss" scoped>` + props/emits 对齐表（design §4.1）+ motion-v 动画（design §4.4）。全部放 `src/components/ui/`，最后统一更新 `ui/index.ts` 导出。

- [ ] 2.1 基础：MButton（variant/size/danger）、MBlockTitle、MCard、MFab
- [ ] 2.2 表单：MCheckbox（勾选 motion scale/fade）、MToggle（拇指 motion x: 0↔22px）、MRange（DOM 对齐 k-range；拇指按压 scale motion）、MListInput（#input 插槽 + error 态）
- [ ] 2.3 列表：MList（inset/strong/outline/dividers）、MListItem（media/after 插槽 + active 高亮）
- [ ] 2.4 导航：MNavbar（left/title/right/subnavbar 插槽 + 安全区 pt）、MNavbarBackLink
- [ ] 2.5 分段：MSegmented（strong/rounded + 滑块高亮：JS 量 active 按钮 offset，motion :animate left/width）、MSegmentedButton（active）
- [ ] 2.6 Tabbar：MTabbar（玻璃渐变内置、pane 内建）、MTabbarLink（label/active/icon slot）
- [ ] 2.7 浮层：MActions 家族（Actions/Group/Label/Button）、MDialog 家族（Dialog/DialogButton）、MSheet、MPopup（fullscreen prop）、MToast（position）——全部 `<AnimatePresence>` + `motion.div`（design §4.2）；MPreloader（useMotionValue + animate repeat Infinity）
  - 浮层共享全局类：`.m-overlay-backdrop`、z 阶梯类（阶段 1 已建）
- [ ] 2.8 清理死代码：删除 MPage.vue / MContent.vue；`ui/index.ts` 重写导出（m-* 全部 + MCover/MEmpty）
- [ ] 2.9 构建验证：`npm run build`（此时无页面引用新组件，仅编译验证）

**验证**：`vue-tsc` 无错；每个组件在 `npm run dev` 下可由页面（阶段 3 引入后）即时代验。

## 阶段 3：页面逐个迁移（每页一个 commit）

迁移顺序建议按依赖与风险递增：壳与简单页 → 列表页 → 交互复杂页（PlayerPage 最后）。

- [ ] 3.1 App.vue：k-app → div.m-app（safe-areas 变量桥接在全局类）
- [ ] 3.2 小部件：MiniPlayer.vue、MCover.vue、MEmpty.vue（tailwind class → scoped scss，玻璃配方复用全局类）
- [ ] 3.3 TabsPage.vue（tabbar 玻璃 + 侧栏 md 布局）
- [ ] 3.4 CategoriesPage.vue（navbar + segmented）
- [ ] 3.5 AlbumsPage.vue / ArtistsPage.vue / PlaylistsPage.vue（封面网格 + actions/dialog）
- [ ] 3.6 SettingsPage.vue（block-title/list/toggle/toast）
- [ ] 3.7 SongsPage.vue（虚拟列表 + shuffle-glass + actions×2/dialog/fab）
- [ ] 3.8 LibraryDetailPage.vue / PlaylistDetailPage.vue（back-link + 随机播放玻璃条 + fab 气泡 + 虚拟列表）
- [ ] 3.9 SourcesPage.vue（card + dialog×3 + preloader + actions）
- [ ] 3.10 QueuePage.vue（popup 半屏 + 列表）
- [ ] 3.11 PlayerPage.vue（最重：popup fullscreen、m-range 沉浸样式、36 个 m-button、sheet/checkbox/list-input/toast/actions；清理 `.player-overlay .progress-range` 全局覆盖规则——新 MRange 自带 iOS 视觉，删除 nth-child hack 与 `[&_[style*='inset-inline-start']]:hidden`；拖拽关页回弹/滑出动画改用 motion 命令式 `animate()`，去掉 `transition-[transform] duration-[220ms]` 类与 `!transition-none` 条件）

每页完成后：
- [ ] 3.x.1 `npm run build` 通过
- [ ] 3.x.2 dev server 目测该页：浅色/深色、玻璃、层级、交互
- [ ] 3.x.3 commit（`feat(ui): …迁移scss` 粒度）

**验证（阶段 3 终点）**：`grep -rn "<k-\|konsta" src` 无残留；`grep -rnE 'dark:|md:|"size-[0-9]|\[[0-9a-z-]+px\]' src --include="*.vue"` 无 tailwind 模式残留。

## 阶段 4：依赖移除

- [ ] 4.1 main.ts 移除 tailwind.css import
- [ ] 4.2 删除 src/theme/tailwind.css、postcss.config.js
- [ ] 4.3 vite.config.ts 移除 tailwindcss() 插件与 import
- [ ] 4.4 package.json 移除 konsta、tailwindcss、@tailwindcss/vite；`npm i` 更新 lock
- [ ] 4.5 `npm run build` + `npm run lint` 通过；产物 CSS grep 验证无 `@layer`/`--color-ios`/tailwind 标记

## 阶段 5：全量验证与收尾

- [ ] 5.1 验收清单逐项（prd AC1-AC8）：
  - AC1 依赖清单（grep package.json；含 motion-v）
  - AC2 无 k- 组件/konsta 引用残留
  - AC3 18 个 vue 全 scss、无 tailwind class 模式
  - AC4 组件行为契约（opened/backdropclick/过渡/滚动/层级）真机或模拟器验证；动画均由 motion-v 驱动（grep 组件内无 CSS transition 动画声明，纯颜色过渡除外）
  - AC5 build + lint
  - AC6/AC7 视觉回归清单：浅/深色各过一遍主要流程（列表止位、播放器进出、队列、Sheet、ActionSheet、Dialog、音源管理、分段、开关、Toast）；动画观感逐项核对（浮层进出、分段滑块、开关拇指、预加载旋转、拖拽回弹）
  - AC8 changelog 记录重构与包体收益（对比 dist 体积 / 依赖树）
- [ ] 5.2 更新 spec（.trellis/spec/）记录新样式体系约定（如适用）
- [ ] 5.3 最终 commit

## 风险文件与回滚点

| 文件 | 风险 | 回滚 |
|---|---|---|
| src/views/PlayerPage.vue | 最大（36 按钮 + range 结构 hack + 浮层多） | 独立 commit，revert 即可 |
| src/views/SongsPage.vue / LibraryDetailPage.vue / PlaylistDetailPage.vue | 虚拟列表 + 吸顶玻璃 | 独立 commit |
| src/theme/index.scss | 全局样式迁移遗漏 | 与 tailwind.css 双轨期可对照；阶段 4 前删除 tailwind.css 必须已过 AC2/AC3 grep 验证 |
| vite.config.ts / package.json | 仅阶段 4 触碰 | 最后一步，此前全部可回滚 |

## 提交前检查

- [ ] `npm run lint` 无新错误
- [ ] `npm run build`（含 vue-tsc）通过
- [ ] git status 无意外文件（node_modules 不提交）

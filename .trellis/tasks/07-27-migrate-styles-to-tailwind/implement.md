# 执行计划：组件样式迁移到 Tailwind CSS v4

## 验证命令

- 构建 + 类型检查：`npm run build`（= `vue-tsc && vite build`）
- Lint：`npm run lint`
- 视觉验证：`npm run dev` 手动核对每批组件（无自动视觉回归）

## 执行原则

- 每批完成后跑 `npm run build` + `npm run lint`，作为 review gate。
- 每个文件迁移后，template class 逐条对照原 CSS，确保无遗漏、无语义漂移。
- 保留 `:style` 动态绑定（JS 计算值），不算 scoped CSS。
- 每批为一个阶段回滚点（git 未提交前可 `git checkout` 单文件回退）。

## 第一批：小型 UI 组件（建立范式）

- [ ] `MContent.vue` — flex/overflow 基础规则
- [ ] `MPage.vue` — flex 纵向布局
- [ ] `ExploreContainer.vue` — 居中容器
- [ ] `MCover.vue` — 保留 `:style` 的 `--m-cover-size`；img 子选择器用 `[&>img]:...`；radius 变体用 class 表达
- [ ] Gate：`npm run build` + `npm run lint` + 视觉核对

## 第二批：中等页面和布局

- [ ] `AlbumsPage.vue` — grid `grid-cols-2` + `md:grid-cols-[repeat(auto-fill,minmax(180px,1fr))]`；MCover 覆盖改 `!w-full h-auto aspect-square`；`line-clamp-2`
- [ ] `ArtistsPage.vue` — 同 Albums，头像加 `rounded-full`
- [ ] `PlaylistsPage.vue`
- [ ] `SettingsPage.vue`
- [ ] `QueuePage.vue` — 虚拟列表容器；overlay；Transition class 若有则用 prop
- [ ] `MiniPlayer.vue` — `fixed`；`bottom-[calc(...)]` 安全区；`z-[var(...)]`；`dark:` 边框/背景；`md:` 断点
- [ ] `App.vue` — 双 style 块：scoped 部分转 utility + Transition prop；全局 `<style>` 移至 `src/theme/tailwind.css`
- [ ] `TabsPage.vue` — 平板 sidebar + 移动 tab bar 响应式
- [ ] Gate：`npm run build` + `npm run lint` + 视觉核对（重点：暗色、平板断点、overlay 锁滚动）

## 第三批：大型页面

- [ ] `SongsPage.vue` — 虚拟列表 + shuffle bar + FAB
- [ ] `PlaylistDetailPage.vue` — 虚拟列表
- [ ] `SourcesPage.vue` — 扫描/虚拟列表行高/pixi；逐段迁移
- [ ] `PlayerPage.vue` — AMLL 背景/渐变（任意值语法验证）/overlay/手势/panels；最复杂，逐段迁移
- [ ] Gate：`npm run build` + `npm run lint` + 视觉核对（重点：播放器沉浸页、背景渐变、手势拖动、音源扫描页虚拟列表滚动）

## 收尾

- [ ] 全局 grep 确认无残留 `<style scoped>` / `<style>` 页块（App.vue 全局块已迁走）
- [ ] 更新 `.trellis/spec/frontend/component-guidelines.md`：移除"允许 scoped style"表述，改为"禁止组件 scoped style，全部 Tailwind utility"
- [ ] 最终 `npm run build` + `npm run lint` 全绿

## 风险与回滚点

- **最高风险**：`PlayerPage.vue`（1455 行）的 AMLL 背景渐变、overlay 手势、panels 横滑；`SourcesPage.vue`（1003 行）虚拟列表行高测量。这两个放最后单独成批，出问题只回退单文件。
- **视觉回归风险**：暗色模式、平板断点、安全区适配无自动测试，依赖手动核对。
- **Transition 风险**：Vue Transition class 改 prop 后需确认动画仍生效。
- **全局样式风险**：App.vue 全局 body 锁滚动移到 CSS 入口后，需确认 overlay 打开时滚动锁定仍工作。
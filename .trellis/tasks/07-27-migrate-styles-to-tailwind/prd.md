# 组件样式迁移到 Tailwind CSS v4

## Goal

把 16 个 `.vue` 组件里手写的 scoped `<style>` CSS 迁移为 Tailwind CSS v4 的 utility class 写法，统一样式表达方式，减少分散的手写 CSS。用户诉求：彻底迁移。

## 背景 / 已确认事实（来自代码探查）

- Tailwind v4 管道已就绪：`vite.config.ts` 挂载 `@tailwindcss/vite`；`src/theme/tailwind.css` 用 `@import 'tailwindcss'` + `@import 'happier-ui/styles'`；`main.ts` 已引入。
- 设计 token 由 `happier-ui` 提供（`--h-*` + `--muses-*` 别名），通过 `@import 'happier-ui/tokens.css'` 注入。
- 现规范 `component-guidelines.md`：
  - 明确 Tailwind v4 为必需样式管道（第 40 行）。
  - 同时允许 `<style scoped>`（第 25、306 行："only when the component needs local styles" / "can use when needed"）。
  - 记录了多条依赖手写 CSS 的既有约定（见风险）。
- 全部 16 个 `.vue` 均含 `<style>` 块，共约 4657 行。其中大文件：
  - `PlayerPage.vue` 1455 行、`SourcesPage.vue` 1003 行、`SongsPage.vue` 444 行、`PlaylistDetailPage.vue` 246、`PlaylistsPage.vue` 234、`QueuePage.vue` 193、`App.vue` 176、`MiniPlayer.vue` 173、`TabsPage.vue` 163、`SettingsPage.vue` 146、`ArtistsPage.vue` 128、`AlbumsPage.vue` 119、`MCover.vue` 67、`MPage.vue` 42、`ExploreContainer.vue` 39、`MContent.vue` 29。
- 无 JSS 实际使用（`jss` 依赖存在但组件层未用）。

## Requirements

- R1：移除全部 16 个 `.vue` 组件内的 `<style scoped>` / `<style>` 页块，样式改用 Tailwind v4 utility class（含任意值 `[...]` 语法与 `dark:` / `md:` 等变体）表达。
- R2：迁移须保持视觉与行为等价——布局、间距、颜色、字号、暗色模式、平板断点、安全区适配、overlay 锁滚动、Transition 动画、虚拟列表滚动均无回归。
- R3：保留 JS 运行时计算的 `:style` 动态绑定（如 `MCover` 的 `--m-cover-size`），此类不属于 scoped CSS。
- R4：Vue `<Transition>` 的过渡样式改用 `enter-active-class` 等 prop 传 Tailwind utility，不依赖自动注入的 `xxx-enter-active` 具名 class。
- R5：`App.vue` 作用于 `html` / `body` / 全局 `.m-content` 的非 scoped 全局块迁移到全局入口 `src/theme/tailwind.css`（document 根元素样式，不在组件模板范围内）。
- R6：按三批推进（小型 UI 组件 → 中等页面 → 大型页面），每批以 `npm run build` + `npm run lint` + 视觉核对为 review gate。
- R7：完工后同步收敛 `.trellis/spec/frontend/component-guidelines.md`——将"允许 scoped style"表述改为"禁止组件 scoped style，全部 Tailwind utility"，并更新相关组件结构示例。

## Acceptance Criteria

- [ ] AC1：全局 grep 确认 `src/**/*.vue` 无残留 `<style scoped>` / `<style>` 页块。
- [ ] AC2：`npm run build`（`vue-tsc && vite build`）全绿，无类型或构建错误。
- [ ] AC3：`npm run lint` 全绿。
- [ ] AC4：手动视觉核对通过——重点覆盖暗色模式、平板断点、安全区、overlay 打开时锁滚动、Transition 动画、播放器沉浸页背景渐变与手势、音源页/队列/歌曲页虚拟列表滚动。
- [ ] AC5：`App.vue` 全局 body 锁滚动逻辑迁移后仍生效（overlay 打开时底层路由页无法滚动）。
- [ ] AC6：`component-guidelines.md` 已更新为禁止组件 scoped style。

## 关键决策

- **验收边界**：零 scoped CSS。所有组件的 `<style scoped>` / `<style>` 块必须移除，样式全部用 Tailwind v4 utility class（含任意值语法 `[...]` 和 `dark:` 等变体）表达。允许保留 `:style` 动态绑定用于 JS 计算出的运行时值（如 `MCover` 的 `--m-cover-size` 尺寸注入），这不属于 scoped CSS。

## 技术可行性证据

- **暗色模式对齐**：项目未使用 `.dark` class 切换（`grep` 无结果），现有代码走 `@media (prefers-color-scheme: dark)`，与 Tailwind v4 `dark:` 变体默认策略一致，可直接用 `dark:xxx` 表达。
- **CSS 变量与 calc/env 组合**：Tailwind v4 任意值语法可表达，例如 `bottom-[calc(var(--muses-tab-bar-height)+env(safe-area-inset-bottom,0px))]`、`z-[var(--muses-z-mini-player)]`。
- **响应式断点**：`@media (min-width: 768px)` 用 `md:` 变体；其他断点用任意值 `min-[900px]:`。
- **深度覆盖子组件样式**：`MCover` 等自建组件是单根节点，Vue 会把父组件传入的 class 透传到根元素。例如 album-card 覆盖封面尺寸/圆形，可写 `<m-cover class="!w-full h-auto aspect-square rounded-full">`，比现有 `--m-cover-size: 100% !important` 更干净，无需保留 CSS。
- **动态 `:style` 保留**：JS 运行时计算的 CSS 变量注入（`MCover` 的 `coverStyle`）不属于 scoped CSS，予以保留。

## 迁移中可能触及的既有约定（需评估影响）

- overlay/虚拟列表/pixi 相关样式集中在 `PlayerPage` / `SourcesPage` / `QueuePage`，是最大工作量来源。
- `component-guidelines.md` 中"`<style scoped>` only when the component needs local styles"这条规则需要在完工时同步收敛为"禁止 scoped style"。

## 范围与拆分决策

- 采用**单任务内分批**（不拆 Trellis 子任务）。三批按复杂度递增：
  - 第一批（建立范式）：`MContent` / `MPage` / `ExploreContainer` / `MCover`。
  - 第二批（中等页面/布局）：`AlbumsPage` / `ArtistsPage` / `PlaylistsPage` / `SettingsPage` / `QueuePage` / `MiniPlayer` / `App` / `TabsPage`。
  - 第三批（大型页面）：`SongsPage` / `PlaylistDetailPage` / `SourcesPage` / `PlayerPage`。
- 每批为一个阶段回滚点；最高风险的 `PlayerPage`（1455 行）与 `SourcesPage`（1003 行）放最后单独成批，出问题只回退单文件。
- 详细执行 checklist 见 `implement.md`，技术映射范式与特殊问题处理见 `design.md`。

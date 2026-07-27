# 设计文档：组件样式迁移到 Tailwind CSS v4

## 目标

零 scoped `<style>` 页块——全部用 Tailwind v4 utility class 表达。动态 `:style` 绑定保留。

## 映射范式

| 手写 CSS | Tailwind v4 等价 |
|---|---|
| `display: flex; flex-direction: column` | `flex flex-col` |
| `gap: var(--muses-space-lg)` | `gap-[var(--muses-space-lg)]`（或若能映射到 happier-ui 层的 Tailwind theme 值则用标准 key） |
| `padding: var(--muses-space-lg)` | `p-[var(--muses-space-lg)]` |
| `height: 100%` | `h-full` |
| `overflow: hidden` | `overflow-hidden` |
| `position: fixed; inset: 0` | `fixed inset-0` |
| `z-index: var(--muses-z-xxx)` | `z-[var(--muses-z-mini-player)]`（任意值） |
| `font-size: var(--muses-font-title)` | `text-[var(--muses-font-title)]` |
| `color: var(--muses-color-ink)` | `text-[var(--muses-color-ink)]` |
| `white-space: nowrap; overflow: hidden; text-overflow: ellipsis` | `truncate`（或分行时 `line-clamp-*`） |
| `media (min-width: 768px)` | `md:` 前缀 |
| `media (prefers-color-scheme: dark)` | `dark:` 前缀 |
| Grid 布局 | `grid grid-cols-2` / `grid-cols-[repeat(auto-fill,minmax(180px,1fr))]` |
| `min-width: 0`（Flex/grid 收缩） | `min-w-0` |
| `flex: 1` | `flex-1` |
| `flex-shrink: 0` | `shrink-0` |
| `width: 100%` | `w-full` |
| 深度覆盖 `--m-cover-size: 100%` | 改用任意值覆盖尺寸：`!w-full h-auto aspect-square`（MCover 根节点透传 class） |

## 范围拆分（三批）

按复杂度和文件大小分组：

### 第一批：小型 UI 组件（建立迁移范式）

| 文件 | 行数 | 复杂度 |
|---|---|---|
| `MContent.vue` | 29 | 极简，4 条规则 |
| `MPage.vue` | 42 | 极简 |
| `ExploreContainer.vue` | 39 | 极简 |
| `MCover.vue` | 67 | 中等：含 `:style` 保留，img 子选择器需处理 |

### 第二批：中等页面和布局组件

| 文件 | 行数 | 关注点 |
|---|---|---|
| `AlbumsPage.vue` | 119 | Grid 响应式列+深度 MCover 覆盖 |
| `ArtistsPage.vue` | 128 | 同上，加 50% border-radius |
| `PlaylistsPage.vue` | 234 | 列表+响应式 |
| `SettingsPage.vue` | 146 | 表单/列表 |
| `QueuePage.vue` | 193 | 虚拟列表+overlay |
| `MiniPlayer.vue` | 173 | calc/安全区/暗色模式 |
| `App.vue` | 176 | **双 style 块+全局 CSS** |
| `TabsPage.vue` | 163 | 平板 sidebar+路由 |

### 第三批：大型页面

| 文件 | 行数 | 关注点 |
|---|---|---|
| `SongsPage.vue` | 444 | 虚拟列表+FAB |
| `PlaylistDetailPage.vue` | 246 | 虚拟列表 |
| `SourcesPage.vue` | 1003 | 扫描/虚拟列表/pixi |
| `PlayerPage.vue` | 1455 | AMLL 背景/overlay/手势 |

## 特殊问题处理

### 1. MCover `img` 子选择器

现有：
```css
.m-cover img {
  width: 100%; height: 100%; object-fit: cover;
}
```

Tailwind v4 支持 **child selector** 变体：`[&>img]:w-full [&>img]:h-full [&>img]:object-cover`

### 2. `-webkit-line-clamp` 多行省略

Tailwind v4 内置 `line-clamp-2`。无需手写。

### 3. Vue `<Transition>` class

`App.vue` 和 `QueuePage.vue` 用 `name="queue-overlay"`，Vue 自动注入 `queue-overlay-enter-active` 等 class。迁移方案：改用 `enter-active-class` 等 prop 直接传 Tailwind utility：

```html
<Transition
  enter-active-class="transition-transform duration-[220ms] ease"
  enter-from-class="translate-y-full"
  leave-active-class="transition-transform duration-[220ms] ease"
  leave-to-class="translate-y-full"
>
```

### 4. App.vue 全局非 scoped `<style>`（影响 html/body/远程 m-content）

涉及：
```css
html.muses-overlay-open, body.muses-overlay-open { overflow: hidden ... }
body.muses-overlay-open .m-content { --overflow: hidden; ... }
```

迁移策略：这些是全局基础层样式，不是组件级样式。移到 `src/theme/tailwind.css` 作为全局层声明（该文件已有 `body` 基础样式），保留为 CSS 而非 Tailwind utility（它们作用于 document 根元素，不在组件模板范围内）。

### 5. `PlayerPage` 深层 fallback 描画（`radial-gradient`/`linear-gradient`）

复杂渐变不能用 utility 表达。方案：拆为 `:style` 绑定，或保留一个极小的 non-scoped style。但验收 A 是零 scoped——实际上 Tailwind v4 任意值可以：`bg-[radial-gradient(circle_at_50%_18%,var(--muses-immersive-placeholder),transparent_42%)]`——语法上可行，验证时需确认。

## 全局样式重定位

App.vue 第二个 `<style>`（非 scoped）移至 `src/theme/tailwind.css` 末尾，附录到已有 `body` 规则之后。

## 规范同步

完工后，`.trellis/spec/frontend/component-guidelines.md` 需要：
- 移除第 25、306 行"style scoped 允许"的表述
- 更新组件结构示例为纯 Tailwind template
- 更新第 71-72 行 MPage/MContent 的样式描述
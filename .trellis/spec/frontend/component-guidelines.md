# Component Guidelines

> How components are built in this project.

---

## Overview

Components in this repository are Vue single-file components using the Composition API via `<script setup lang="ts">`. The current codebase is simple and mostly follows the default Ionic Vue starter patterns.

Reference files:

- `src/App.vue`
- `src/components/ExploreContainer.vue`
- `src/views/Tab1Page.vue`
- `src/views/TabsPage.vue`

---

## Component Structure

Use the standard Vue SFC layout already present in the repo:

1. `<template>` first
2. `<script setup lang="ts">` second
3. `<style scoped>` only when the component needs local styles

Examples:

- `src/App.vue` shows a minimal shell component with only Ionic wrapper markup.
- `src/components/ExploreContainer.vue` shows template + script + scoped style.
- `src/views/Tab1Page.vue` shows a route page with Ionic layout components and no local style block.

---

## Muses 语义组件层 → `happier-ui`

Muses 默认从 npm 使用固定版本 **`happier-ui@0.0.1`**，不得提交 `file:../happier-ui`，也不得配置指向相邻仓库源码的 Vite/TypeScript alias。应用通过 `src/components/ui` re-export 库真实导出与 app-only 组件。

- **权威 token**：包内 `happier-ui/tokens.css` 的 **`--h-*`**；`--muses-*` 为兼容别名。
- **边界**：真实库导出为 `HButton`、`HSwitch`、`HBottomSheet`、`HDialog`、`HInput`、`HCheckbox`、`HEmpty`、`HImage`、`HIcon`、`HTabBar`、`HNavBar`；app-only 为 `MCover`（音乐封面）与 `MPage`（Ionic 宿主页壳）。
- **首版视觉**：HeroUI Native 角色与触控节奏；**不**引入 `heroui-native` / RN。
- **禁止**：`MIon*`、整库复刻 Ionic、Material elevation、包内 `@/features` / 业务。

### 组件契约

`src/components/ui/index.ts` 只转出 happier-ui@0.0.1 的真实导出，并附带 `MCover`、`MPage`。已消失的 `HEmptyState`、`HIconButton`、`HListRow`、`HSettingRow` 及 `MEmptyState`、`MIconButton`、`MListRow`、`MSettingRow` 均不得恢复。Muses 不新造这些通用平行组件；库缺口保留 Ionic/业务实现并登记 `.trellis/tasks/07-23-happier-ui-full-migrate/gaps.md`，未来在 happier-ui 仓库开发后再回迁。

### 使用规则

- 通过 `@/components/ui` 具名导入。组件只表达语义，**不**读取播放、曲库等业务状态。
- 新样式优先 `--h-*` 或已有 `--muses-*` 别名；不得新硬编码主色 / elevation。
- `HEmpty`、`HButton`、`HSwitch`、`HInput`、`HCheckbox` 等已有能力优先使用；`MCover` 仅用于音乐封面业务。
- 通用列表行、设置行、icon-only 按钮等 0.0.1 缺口不在 Muses 造替代组件，具体落点以 `gaps.md` 为准。
- **可提交表单**统一用 `@tanstack/vue-form` + `HInput` 字段绑定；约定见 [forms.md](./forms.md)。

### 何时直连 `ion-*`（白名单）

业务页可直接使用下列 Ionic 能力；**新增** UI 仍不得写死色/圆角，应使用 token 或 happier-ui：

| 允许直连 | 原因 |
|----------|------|
| `ion-page` / `ion-content` | Ionic 路由页壳与滚动容器；简单页优先 `MPage` |
| `ion-list` | 结构容器，暂不封装 |
| `ion-button`（带文字或纯图标缺口） | 带文字操作优先 `HButton`；icon-only 等库缺口保留 Ionic 壳并登记 |
| `ion-toggle` / `ion-input` / `ion-checkbox` / `ion-range` | 优先 `HSwitch`/`HInput`/`HCheckbox`；`ion-range` 及未覆盖能力按缺口保留 |
| `ion-action-sheet` / `ion-alert` / `ion-modal` / `ion-fab` | 叠层/FAB 宿主；仅替换内部图标为 `HIcon` |
| `ion-note` / `ion-item` / `ion-label` / `ion-card*` / `ion-text` / `ion-progress-bar` | 库缺口：列表行、设置行、卡片、提示、进度等，保留并登记 `gaps.md` |
| `ion-router-outlet` / Tab 壳相关 | 应用壳，非语义组件 |

**禁止**：业务代码使用 `ion-icon` / `@/icons/ion-lucide` / `ionicons/icons`；为迁移率新建 `MListRow`/`MSettingRow`/`MIconButton` 或复制空态结构。库没有对应组件时，保留现有 Ionic/业务结构并登记 `gaps.md`。

## Ionic Page Pattern

路由页面保留 Ionic 宿主页壳，但用户可见的顶部导航栏统一使用 `HNavBar`：

- 页面根节点使用 `<ion-page>`。
- `HNavBar` 作为 `ion-page` 的直接子级，放在 `ion-content` 前，页面级统一传 `:fixed="false"`，让它参与 flex 布局并避免遮挡正文。
- 页面内容使用 `<ion-content :fullscreen="false">`；不再依赖 Ionic 折叠大标题或 header 叠加。
- 简单页面优先使用 `MPage`；其 `title`、`start`、`end` 插槽分别映射到 `HNavBar` 的 `title`、`left`、`right` 插槽。
- modal 内的 `HNavBar` 同时传 `:fixed="false" :safe-area="false"`，避免重复状态栏留白。
- overlay 自管 flex 布局时，`HNavBar :fixed="false"` 作为首个 flex 项。
- 页面 navbar 禁止使用 `ion-header`、`ion-toolbar`、`ion-title`、`ion-buttons`、`ion-back-button` 拼装；返回行为使用 `HNavBar show-back` 与 `handleLeftClick` 显式处理。

参考文件：

- `src/components/ui/MPage.vue`
- `src/views/SongsPage.vue`
- `src/views/PlaylistDetailPage.vue`
- `src/views/QueuePage.vue`
- `src/views/SourcesPage.vue`

不要构建缺少 `ion-page` 的路由内容页，否则会破坏 Ionic 布局预期。例外：`src/views/TabsPage.vue` 这类仅负责导航 chrome 和 `<RouterView />` 的父路由壳可以使用普通 Vue 容器，避免嵌套路由重复堆叠 Ionic page。

---

## Props Conventions

Current code shows a lightweight runtime prop declaration:

```ts
defineProps({
  name: String,
})
```

Reference file:

- `src/components/ExploreContainer.vue`

For consistency with current code, simple presentational components may use compact `defineProps(...)` declarations. When adding more than trivial props, prefer explicit TypeScript prop typing so the component remains aligned with the repository’s `strict: true` TypeScript mode.

Good fit:

- Small display-only props on reusable components
- Explicit imports for all used Ionic components

Avoid:

- Implicit global component assumptions
- Large untyped prop bags
- Passing unrelated page state through many component layers in this small app

---

## Import Conventions

Import all Ionic components explicitly inside each SFC.

Examples:

- `src/App.vue` imports `IonApp` and `IonRouterOutlet`
- `src/views/Tab1Page.vue` imports `IonPage`, `IonHeader`, `IonToolbar`, `IonTitle`, `IonContent`
- `src/views/TabsPage.vue` 从 `@/icons` 导入 Lucide Vue 语义组件，并使用 `HIcon` 渲染

Also prefer the `@/` alias for application imports from `src/`:

- `import ExploreContainer from '@/components/ExploreContainer.vue'`
- lazy route import `import('@/views/Tab1Page.vue')`

---

## 图标约定（`@lucide/vue` + `HIcon`）

业务侧图标统一使用 `@lucide/vue` 的 Vue 组件，并交给 happier-ui 的 `HIcon` 渲染：

- 语义导出：`src/icons/index.ts`；导入示例：`import { play, pause, shuffle } from '@/icons'`。
- 渲染：使用 `<HIcon :icon="play" />`；业务代码禁止 `ion-icon`、`@/icons/ion-lucide` 与 `ionicons/icons` 直引。
- `package.json` 可因 Ionic 框架运行时保留 `ionicons`，但业务图标不得消费它。
- 播放模式状态图标必须可区分：`repeatOutline` vs `repeat`、`listOutline` vs `shuffle`，不得两状态共用同一图标。
- 尺寸、颜色与 fill/outline 由 `HIcon` 属性及现有 CSS 控制，保持 `currentColor`。
- **播放主控 fill**：`play` / `pause` / `playSkipBack` / `playSkipForward` 使用 `HIcon` 的 fill 变体，供 `MiniPlayer`、`PlayerPage` 主三键及歌词页浮动播放控件使用。
- **次级仍 outline**：列表「播放全部」用 `playOutline`（与 `play` 解耦，保持线框）；模式键（`shuffle` / `repeat*` / 顺序用 `listOutline`）、队列入口（`list`）、返回、翻译等继续 outline
- **禁止**歌词页浮动播放键再使用圆形 `PlayCircle` / `PauseCircle`；必须与主控同一对 `play` / `pause`（fill）
- **歌词翻译开关必须可区分且同族**：开 = `languageOutline`（Lucide `Captions`），关 = `languageOffOutline`（Lucide `CaptionsOff`）；同一字幕图标族，只差开/关标记。禁止两态共用同一图标只靠透明度，也禁止开态用 `Languages`、关态用 `CaptionsOff` 这种跨族搭配。`aria-label` 仍为「隐藏翻译」/「显示翻译」，并保留 `.is-active` 高亮作为辅助
- 主控按钮仍为 `fill="clear"` 纯图标，不得为 fill 图标另加 solid 圆底阴影

### 同语义同图标（list vs listOutline）

`src/icons` 用不同 Lucide Vue 组件区分历史 ionicons 语义名；业务侧必须按语义选用，**不得混用**：

| 语义 | 导出符号 | Lucide | 调用点示例 |
|------|----------|--------|------------|
| 打开队列 | `list` | ListMusic | `MiniPlayer` 队列键、`PlayerPage` 队列键 |
| 歌单导航 / 歌单列表占位 | `list` | ListMusic | `TabsPage` 歌单 Tab、`PlaylistsPage` 行图标 |
| 顺序播放模式（shuffle off） | `listOutline` | **ListOrdered** | `PlayerPage` 的 `shuffleIcon` 非随机态 |
| 随机播放模式 | `shuffle` | Shuffle | `PlayerPage` 的 `shuffleIcon` 随机态 |
| 列表循环 / 单曲循环 | `repeatOutline` / `repeat` | Repeat / Repeat1 | 播放模式循环键 |
| 播放主控 / 歌词浮动播放 | `play` / `pause` / `playSkip*`（fill） | Play / Pause / Skip* | MiniPlayer、PlayerPage 主三键、歌词页右下角浮动键 |
| 列表次级播放 | `playOutline` | Play outline | 歌单详情「播放全部」等 |
| 专辑 | `albums` | **Disc3** | `TabsPage` 专辑 Tab 等 |
| 艺术家 | `person` | **MicVocal** | `TabsPage` 艺术家 Tab 等 |
| 音源 | `radio` | **Folder** | `TabsPage` 音源 Tab 等 |
| 歌曲占位 / 歌曲 Tab | `musicalNotes` / `musicalNotesOutline` | Music | 歌曲 Tab、列表无封面占位 |
| 歌词翻译开 | `languageOutline` | **Captions** | 歌词页左下翻译键（显示译文） |
| 歌词翻译关 | `languageOffOutline` | **CaptionsOff** | 歌词页左下翻译键（隐藏译文） |

规则：

- **`list`（ListMusic）** 专用于「打开队列」与「歌单」相关入口；`MiniPlayer` 与 `PlayerPage` 打开队列必须同一导出 `list`。
- **`listOutline`（ListOrdered）** **仅**用于顺序播放模式（shuffle off），不得用于队列按钮或歌单行占位；**不得**再映射为无序号的 Lucide `List`。
- 歌词页右下角播放/暂停与主控同图标（`play` / `pause` fill），**不得**使用圆形 outline 变体。
- 歌词页翻译键：`showLyricTranslation` 为 true 用 `languageOutline`，为 false 用 `languageOffOutline`；**不得**两态共用 `languageOutline`。
- **导航 Tab 图标**：`albums` → Lucide `Disc3`（禁止 `DiscAlbum`）；`person` → Lucide `MicVocal`（禁止通用 `User`）；`radio` → Lucide `Folder`（音源是文件夹/目录语义，**禁止** `Radio` 电台图标，尽管导出符号历史名仍为 `radio`）。
- 业务侧继续使用既有导出名 `albums` / `person` / `radio`，**不要**为改 Lucide 几何而重命名调用点；新代码优先与 `TabsPage` 保持一致。
- `musicalNotes` 与 `musicalNotesOutline`、`add` 与 `addOutline` 在 `@/icons` 中已是同一 Lucide 几何的别名，视觉已一致；可不强制改名，但新代码优先与现有调用点保持一致。

---

## Responsive Breakpoint Convention

项目使用以下全局 CSS 变量在宽屏下适配平板布局：

- `--muses-breakpoint-tablet: 768px` — 平板断点宽度（定义于 `src/theme/tokens.css`）
- `--muses-content-max-width: 720px` — 内容最大宽度限位居中

### 断点约定的规则

1. **Muses 视觉变量统一在 `src/theme/tokens.css` 的 `:root` 中定义**，单一来源，所有页面引用 `var(--muses-*)`；`variables.css` 只桥接 Ionic。
2. **`@media (min-width: XXX)` 条件中不可使用 `var()`** — CSS 标准不允许。在 `@media` 中直接使用硬编码的 `768px`；`var()` 只用于属性值部分（如 `max-width: var(--muses-content-max-width)`）。
3. **宽屏下隐藏元素**：对窄屏专属元素（如 `ion-tab-bar`）使用 `@media (min-width: 768px) { … display: none }`。
4. **窄屏零回归**：所有平板改造限定在 `@media (min-width: 768px)` 内；窄屏下不加任何额外样式。

### 当前平板组件模式

- **导航 Shell**：`src/views/TabsPage.vue` 使用普通 Vue 布局容器作为父级 shell，不使用 `ion-page` 包裹父路由布局；子页面继续保留自己的 Ionic page 结构。
- **侧栏**：宽屏下由固定定位的普通 `<aside>` 提供左侧导航，右侧 `<main>` 渲染 `<RouterView />`；窄屏回落为普通 `<nav>` + `RouterLink` 底部导航。
- **避免 Split Pane**：当前 MuMu / Android WebView 环境中，`ion-split-pane` + `ion-menu` 曾触发白屏；不要在 `TabsPage.vue` 中恢复该结构，除非完成真机与 MuMu 回归验证。
- **专辑卡片网格（Albums）**：`src/views/AlbumsPage.vue` 使用 `<div class="album-grid">` 直接渲染 `<article class="album-card">` 卡片（封面 + 专辑名 + 歌曲数 + 艺术家摘要），不再使用 `ion-list` / `ion-item`。窄屏固定 `grid-template-columns: repeat(2, minmax(0, 1fr))`；宽屏在内容宽度上限内 `repeat(auto-fill, minmax(180px, 1fr))` 自动增列。卡片封面复用 `MCover`，通过 `.album-card > .album-card__cover { --m-cover-size: 100% !important; height: auto; aspect-ratio: 1; flex: 0 0 auto }` 覆盖 MCover 内联默认尺寸；`height: auto` 必须保留，使 `aspect-ratio` 能按卡片宽度计算正方形高度（不改 MCover 全局契约）。
- **艺术家卡片网格（Artists）**：`src/views/ArtistsPage.vue` 使用 `<div class="artist-grid">` 直接渲染 `<article class="artist-card">` 卡片（圆形头像 + 艺术家名 + 歌曲数 + 专辑数），不再使用 `ion-list` / `ion-item`。窄屏固定 `grid-template-columns: repeat(2, minmax(0, 1fr))`；宽屏在内容宽度上限内 `repeat(auto-fill, minmax(180px, 1fr))` 自动增列。头像复用 `MCover`，从艺术家歌曲中选择首张有效封面，无封面时保留占位；通过 `.artist-card > .artist-card__avatar { --m-cover-size: 100% !important; height: auto; aspect-ratio: 1; border-radius: 50% }` 保持正圆。
- **SongsPage 宽屏单列**：`src/views/SongsPage.vue` 宽屏不使用多列 grid，列表始终竖排单列；外层 `.list-grid` / `.tablet-content-limit` 仅做 `max-width: var(--muses-content-max-width); margin-inline: auto` 限位居中（与窄屏一致的一列体验）。
- **内容限位居中**：各列表页 `.tablet-content-limit`、`.artist-grid`（Artists）和 `.album-grid`（Albums）在宽屏下 `max-width: var(--muses-content-max-width); margin-inline: auto`。

### SongsPage Navbar 下方固定随机播放全部

`src/views/SongsPage.vue` 在顶部 Navbar 正下方固定显示随机播放全部入口，歌曲列表在其下方滚动：

- 位置：作为 `HNavBar` 与 `ion-content` 之间的独立 `.shuffle-bar` flex 项，使入口与 Navbar 一起固定，不随歌曲列表滚动。
- 布局：按钮容器在窄屏左对齐；宽屏使用 `max-width: var(--muses-content-max-width)` 与 `margin-inline: auto` 限宽居中，按钮仍位于内容左侧。
- 样式：优先 `HButton` + `HIcon` 展示 `@/icons` 的 `shuffle` 与「随机播放全部」文案；若仍用 Ionic 文字按钮，内部图标也必须是 `HIcon`，不得使用 `expand="block"` 或整行描边操作条。
- 禁止恢复为 `ion-content slot="fixed"` 的底部 `.bottom-actions`，也不要把入口放入会随列表滚走的普通内容流。
- 无歌曲时按钮仍出现且 `:disabled`，点击不产生副作用；保留 `aria-label="随机播放全部"`。
- 点击语义：`clearQueue()` → `enqueueSongs(allSongs)` → 若 `!shuffleEnabled()` 则 `toggleShuffle()` → `selectSongAtIndex(0)` → `playSong(first)`。
- `toggleShuffle` 会生成 `shuffleOrder`；`selectSongAtIndex(0)` 取乱序首曲。
- `ion-content` 的 `--padding-bottom` 只需避让 MiniPlayer 和移动端 Tab Bar，不再为随机播放操作条额外留位；仍须确保最后一首歌曲滚动到底后完整可见。

参考结构：

```vue
<HNavBar title="歌曲" :fixed="false"><!-- 搜索操作 --></HNavBar>
<div class="shuffle-bar">
  <div class="shuffle-actions">
    <HButton variant="ghost" aria-label="随机播放全部">
      <template #leading>
        <HIcon :icon="shuffle" aria-hidden="true" />
      </template>
      随机播放全部
    </HButton>
  </div>
</div>
<ion-content :fullscreen="false"><!-- 歌曲列表 --></ion-content>
```

### SongsPage 跳转到当前播放 FAB

`src/views/SongsPage.vue` 在 `ion-content` 内右下侧放置 `ion-fab` / `ion-fab-button`，用于滚动到当前播放歌曲行：

- 图标：`@/icons` 的 `locateOutline` 经 `HIcon` 渲染；`aria-label="跳转到当前播放"`。
- 可见性：`v-if="currentPlayingInList"` —— 仅当 `playerState.currentSong?.id` 存在且该 id 出现在当前歌曲列表中时展示；无当前播放或不在列表则隐藏。
- 行定位：每行 `ion-item` 带 `data-song-id="song.id"`；点击 FAB 用页面内 `[data-song-id]` 找到匹配行后 `scrollIntoView({ behavior: 'smooth', block: 'start', inline: 'nearest' })`。
- **避开固定顶栏**：`block: 'start'` 只对齐滚动端口顶部，不扣除外层 `HNavBar + .shuffle-bar`。必须在 `.song-item`（或带 `data-song-id` 的行）设置足够的 `scroll-margin-top`（当前约 108px，覆盖 navbar、48px 随机播放条与缓冲），使目标行完整出现在列表可视区内、标题主信息不被顶栏挡住。仅写 `block: 'start'` 而不设 margin/等价偏移会回归遮挡。
- 列表末尾无法再滚时停在容器允许的最大位置（不必强行置顶）。宽屏单列同样适用。
- 可选轻高亮：滚动后给目标行加 `jump-highlight` 约 1.2s，再移除；卸载时清理 timer。
- 安全区：`.jump-current-fab` 的 `bottom` 需避开底部导航与 MiniPlayer（窄屏约 `calc(144px + safe-area)` = Tab Bar ~64 + MiniPlayer ~64 + 间距；宽屏无 Tab Bar、MiniPlayer 贴底，约 `calc(80px + safe-area)` = MiniPlayer ~64 + 间距），`right: 12px`，不遮挡列表关键操作。勿按「平板 MiniPlayer 抬高 64px」再额外加偏移。
- 不破坏现有列表点击播放与更多按钮交互。

## Styling Gotchas

### navbar 标题统一使用 HNavBar 居中契约

页面与 modal 的顶部导航栏统一使用 `HNavBar`。其 `left / title / right` 三栏布局负责标题相对完整 navbar 居中，业务页面不再维护 Ionic 标题绝对定位补丁。

统一约定：

- 标题优先使用 `title` prop；需要自定义内容时使用 `title` slot。
- 左右操作分别使用 `left` / `right` slot；返回使用 `show-back`、`back-aria-label` 与 `handleLeftClick`。
- 动态长标题依赖 `HNavBar` 内建单行省略，不添加页面级居中 class 或覆盖内部 grid。
- `src/theme/variables.css` 只保留 Ionic token 桥接，不得恢复 `ion-header` / `ion-title` / `ion-buttons` navbar 专属全局样式。
- 页面级 `safeArea` 保持默认开启；modal 内显式关闭；固定与否按页面壳规则设置。

### ion-list 为 Web Component，CSS Grid 在外层无法布局子 ion-item

`ion-list` 是 Ionic Web Component，有 Shadow DOM 隔离。在外层 div 套 CSS Grid 后，`ion-list` 只是 grid 容器的第一个子项，不会将 `ion-item` 暴露为 grid item。

**适用范围**：仅仍使用 `ion-list` + 宽屏多列的页面。`ArtistsPage` 与 `AlbumsPage` 已改为直接渲染 `article` 卡片网格（不含 `ion-list`），不适用本 gotcha；`SongsPage` 宽屏已改为单列，也不再使用 grid + `display: contents`。

**修复（多列页）**：在宽屏下给 `ion-list` 加 `display: contents;`，使 `ion-item` 成为 grid 容器的直接子元素：

```css
@media (min-width: 768px) {
  .list-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 1px;
    max-width: var(--muses-content-max-width);
    margin-inline: auto;
  }
  .list-grid > ion-list {
    display: contents;
  }
}
```

**SongsPage 宽屏单列**：

```css
@media (min-width: 768px) {
  .list-grid {
    max-width: var(--muses-content-max-width);
    margin-inline: auto;
  }
}
```

### CSS var() 不可用于 @media 断点条件

CSS 变量只能在属性值中解析，不能在 `@media (min-width: …)` 中生效。错误写法：

```css
/* ✗ 错误——CSS 变量在 @media 条件中无法解析 */
@media (min-width: var(--muses-breakpoint-tablet)) { … }

/* ✓ 正确——@media 条件用硬编码，属性值用 var() */
@media (min-width: 768px) {
  .list-grid {
    max-width: var(--muses-content-max-width);
  }
}
```

Current styling is split by scope:

- Global framework/theme CSS is loaded once in `src/main.ts`
- Project theme overrides belong in `src/theme/variables.css`
- Component-local styling can use `<style scoped>` when needed

Reference files:

- `src/main.ts`
- `src/theme/variables.css`
- `src/components/ExploreContainer.vue`

Do not duplicate Ionic core or utility CSS imports inside page components.

---

## MiniPlayer 与播放器 Overlay 约定

`src/components/MiniPlayer.vue` 是应用级固定底栏，由 `src/App.vue` 始终挂载；播放器和队列使用全局 overlay 状态显示，不再通过 `/player` 或 `/queue` 路由打开。

### 样式约定

- 底栏占满屏幕宽度，固定在移动端底部导航栏上方。
- **窄屏**（`<768px`）：`bottom: calc(64px + var(--ion-safe-area-bottom, 0px))`，为底部 Tab Bar 留位。
- **宽屏**（`@media (min-width: 768px)`）：平板侧栏布局已隐藏 `.mobile-tab-bar`，底栏贴底，仅保留安全区：`bottom: var(--ion-safe-area-bottom, 0px)`；禁止继续抬高 64px，否则会悬空。
- 底栏本身不使用圆角和阴影，仅保留顶部边线分隔内容。
- 封面容器圆角与歌曲列表一致，使用 `border-radius: 10px`。
- 无当前歌曲或无封面时展示稳定占位封面与占位文案，避免播放状态为空时底栏跳动或消失。
- `MiniPlayer` 不要因 overlay 打开而用 `v-if` 卸载；overlay 打开时只禁用交互（例如 `pointer-events: none`），避免下滑关闭时底栏闪烁。

### 交互约定

- 点击底栏主体调用 `openPlayerOverlay()`，不能改变当前路由 URL。
- **无当前歌曲时不可打开沉浸式播放页**：当 `playerState.currentSong` 为 `null` 时，点击主体或键盘 Enter / Space 都不得调用 `openPlayerOverlay()`；主体应标记 `aria-disabled`，并去掉 `cursor: pointer` 误导。
- 点击播放/暂停按钮只控制播放状态，不能触发打开播放器 overlay。
- 播放/暂停图标使用 `@/icons` 的 `play` / `pause` 组件并由 `HIcon` 以 fill 变体渲染，与沉浸页主控一致；不得用 outline、`ion-icon` 或 ionicons 直引。
- 无歌曲时播放/暂停按钮继续禁用；队列按钮行为不受影响，仍可打开队列 overlay。
- 点击队列按钮调用 `openQueueOverlay()`，不能改变当前路由 URL，也不能触发打开播放器 overlay。
- 对 Ionic `ion-button` 不要只依赖 `@click.stop`；按钮内部事件可能穿过 Web Component 边界。父级主体点击处理需要检查 `event.composedPath()`，如果事件路径包含 `.player-actions` 就直接忽略。

```ts
const openPlayerPage = (event: MouseEvent | KeyboardEvent) => {
  if (event.composedPath().some((target) => target instanceof Element && target.classList.contains('player-actions'))) {
    return
  }
  if (!playerState.currentSong) {
    return
  }
  openPlayerOverlay()
}
```

### Overlay 页面约定

- `PlayerPage.vue` 和 `QueuePage.vue` 是全局 overlay 内容组件，由 `App.vue` 渲染在 `ion-router-outlet` 之后；不要在 `src/router/index.ts` 中新增 `/player` 或 `/queue` 路由。
- 打开播放器/队列 overlay 时底层 tabs 路由页面必须保持存在，以支持下滑收起露出真实底层页面。
- 播放器 overlay 的系统状态栏样式由 `App.vue` 监听 `playerOverlayVisible` 统一管理：打开时调用 `StatusBar.setStyle({ style: Style.Dark })` 显示白色内容，关闭及 `App.vue` 卸载时用 `Style.Default` 恢复默认；插件失败必须静默忽略，并通过串行化或请求 token 防止快速开关导致异步乱序。不得监听 `hasGlobalOverlay`，队列 overlay 单独打开不修改状态栏，也不要在 `PlayerPage.vue` 内管理状态栏。
- 播放器 overlay 顶部不显示返回/收起按钮，也不展示「正在播放」标题；顶部仅保留安全区留白。关闭通过下滑手势、Android back 键或显式 overlay 状态完成。
- 下滑收起播放器时移动 overlay 内容层，不要移动 Ionic 路由页或依赖透明路由页露出缓存层，否则容易出现黑屏或重复页面。
- 沉浸式控制页布局自上而下：大封面 → 歌名/歌手 → 进度条 → 主控制（上一曲/播放暂停/下一曲）→ 次要控制（循环/随机/队列）。
- **不展示元信息补充过程文案**（#48）：沉浸式页不得渲染「正在补充歌曲信息…」「歌曲信息补充失败…」等 `metadataStatus` 提示；后台扫描/在线补全逻辑可继续运行。
- 沉浸式控制页封面（`.cover` / 占位封面）不加 `box-shadow`；宽屏与窄屏保持一致，避免封面后方出现额外阴影。
- **封面必须保持正方形**：`.cover` / `.placeholder-cover`（与 `.cover` 共用尺寸类）使用 `aspect-ratio: 1; height: auto; object-fit: cover`。正方形边长 = `min(水平上限, 垂直上限)`。
  - **窄屏** `.cover` 的 `width` 也必须同时受 vw 与 cover-slot 的 dvh/`max-height` 约束（默认 `min(72vw, 100%, 340px, 52dvh)`；`max-height: 720px` 时 `min(72vw, 100%, 260px, 42dvh)`；更矮 `max-height: 520px` 时 `min(72vw, 100%, 200px, 38dvh)`）。
  - **宽屏** 同理：`min(40vw, 48dvh, 320px)`；矮屏 `min(40vw, 42dvh, 260px)`；更矮 `min(40vw, 38dvh, 200px)`。
  - 禁止只写 `width: min(72vw, 100%, 340px)` 或 `min(40vw, 320px)` 而仅靠 `max-height: 100%` clamp 高度——当 cover-slot 可用高度小于目标宽度时，高度被夹、宽度仍按 vw → 封面被压成长方形（车机矮屏/手机横屏的典型回归）。
- **矮屏/横屏控制区收紧**（仅控制页 `.info-panel`，不改歌词页）：保持竖排与全部控件可见，不改为左右分栏、不隐藏模式栏/进度。用 `max-height` 分层收紧：
  - 默认（正常竖屏高度，如 `>720px`）：较大 padding / gap / 按钮与进度热区，观感不变。
  - `max-height: 720px`：减小 panel 上下 padding（保留 `safe-area`）、`info-panel-inner` gap、进度 slider 热区（约 20px）、主控与模式栏按钮尺寸，封面槽位拿到更多垂直空间。
  - `max-height: 520px`：再收一档 gap/字号/按钮/热区（约 18px），仍显示全部控件。
  - 不引入 landscape 专用 DOM；横屏通常命中 `max-height` 断点即可。padding 只减固定 px 部分，用 `calc(... + safe-area)`，不得抹掉安全区。
- 主控制三键（上一曲/播放暂停/下一曲）均为 `fill="clear"` 纯图标按钮，无 solid 圆底与按钮阴影；图标从 `@/icons` 导入并由 `HIcon` 以 fill 变体渲染（`play` / `pause` / `playSkipBack` / `playSkipForward`），不得回退 outline 主控或 ionicons 直引；可保留略大热区（如播放键 68×68），必须提供 `aria-label`，loading 禁用态保留。
- 循环/随机/队列使用纯图标按钮，必须提供 `aria-label`；激活态用高亮或更高不透明度表达，不要依赖可见文字标签。播放器模式图标必须与当前状态同步，且一律从 `@/icons` 导入并由 `HIcon` 渲染：列表循环使用 `repeatOutline`（Lucide `Repeat`）、单曲循环使用 `repeat`（Lucide `Repeat1`），顺序播放使用 `listOutline`（Lucide `ListOrdered`）、随机播放使用 `shuffle`（Lucide `Shuffle`）；状态切换后图标和标签应立即更新，禁止两个状态共用同一图标。**打开队列**按钮使用 `list`（Lucide `ListMusic`），与 `MiniPlayer` 队列键一致；不得用 `listOutline` 表示队列（`listOutline` 仅顺序播放）。
- **歌词页浮动播放键**：窄屏歌词页右下角播放/暂停必须使用与主控相同的 `play` / `pause`（fill），禁止圆形 `PlayCircle` / `PauseCircle`。
- 控制页必须一屏适配：`immersive-shell` / panels 固定 `height: 100dvh`，`overflow: hidden`；封面用弹性槽位（`.cover-slot`：`flex: 1 1 auto; min-height: 0`）缩放，控制区块 `flex: 0 0 auto`，禁止页面纵向滚动。
- 歌词页（AMLL）视觉约定：
  - **窄屏** `.lyric-panel`：顶部 `.lyric-header` 展示歌名（主标题）+ 歌手（副标题，空则不渲染；不拼接专辑、不回退「未知歌手」）；其下为 `flex:1` 的 AMLL `LyricPlayer`；底部仅安全区。
  - **歌词页浮动 chrome 按需显示**：左下翻译、右下播放/暂停（仅非平板）默认 **隐藏**（`opacity: 0` + 容器/按钮 `pointer-events: none`），约 180ms fade。用户在 **歌词面板内** 点击或滑动歌词后显示（`.is-visible`），空闲 **3 秒** 再隐藏；点浮动按钮重置计时。**切回控制页**（`activePanel !== 1`）或 **关闭 overlay** 立即隐藏并清 timer。隐藏态禁止可点热区。
  - **宽屏**（`@media (min-width: 768px)`）：隐藏 `.lyric-header`，右侧只保留歌词；AMLL 视觉参数与窄屏一致。
  - AMLL 参数：`alignAnchor="center"`、`alignPosition=0.5`（当前行位于歌词可视区中心）、`enableBlur` / `enableScale` 开启；字号用 `--amll-lp-font-size`（约 `clamp(22px, 6.5vw, 32px)`）；用 `:deep()` 去掉行左右 padding，使歌词左缘与顶部信息对齐。
  - 翻译副行样式必须使用 AMLL 实际类名：`.FmKaba_lyricLine.FmKaba_active`、`.FmKaba_lyricMainLine.FmKaba_active` 和 `.FmKaba_lyricSubLine`；不要依赖不存在的自定义 active 类。歌词 timed 翻译需支持点号、冒号、逗号毫秒时间戳，匹配容差应保持较小并有超界测试，避免翻译错位。同时间戳双语主行合并时主行须为原文（非 Han 优先于 Han），关翻译后不得只剩中文译文当主行。
  - 继续使用 `@applemusic-like-lyrics` 的 `LyricPlayer`，不自研滚动引擎；主词解析用库内 `parseLrc` / `parseYrc` / `parseQrc` / `parseTTML`，翻译适配仅走 `prepareLyricLinesForDisplay`（tlyric 挂载 + 双行 plain LRC 主译 + 开关），不修改 `node_modules`，不新增平行歌词解析器。
  - 在线歌词匹配期间：若有本地歌词先展示本地；若无本地歌词显示「正在匹配在线歌词…」。匹配无结果、网络失败或解析失败且无本地歌词时，空态需说明「未匹配到在线歌词，且无本地歌词」，不得一直空白或弹错误打断播放。
  - **歌词行点击 seek**：`LyricPlayer` 绑定 `@line-click`（AMLL emit `lineClick` / core `line-click`）。事件类型为 `LyricLineMouseEvent`，其中 `line` 是 `LyricLineBase`，通过 `line.getLine().startTime` 取起始时间（**毫秒**），再调用 `seekPlayback(startTime / 1000)`（秒）。`startTime` 非 number / 非有限数 / `< 0` 时不 seek。处理时 `stopPropagation` + 复用 `seekGestureLocked`，避免点击误触发 overlay 下滑关闭或横向切面板。无歌词空状态不绑定该行为。
  - **歌词区上下滑动手势隔离**：AMLL `LyricPlayer` 内部滚动基于 transform，**非原生 scroll**，`canStartVerticalDismiss` 的原生 `scrollHeight > clientHeight && scrollTop > 0` 检测无法识别。因此 `canStartVerticalDismiss` 必须额外用 `composedPath` 检测触点是否位于 `.lyric-panel` / `.lyric-player` 内，是则返回 `false`，使歌词区上下滑动不更新 `dragOffsetY`、不触发 overlay 下滑关闭。控制页（`.info-panel`）下滑关闭语义不变；`onTouchEnd` 中基于 `startX / endX` 的横向切换面板逻辑保留，歌词页左滑仍可切回控制页。
- **SongsPage 大列表必须虚拟化**（#50）：使用 `@tanstack/vue-virtual` 只渲染可视行（固定约 72px，适量 overscan），自建滚动容器；「跳转当前播放」先 `scrollToIndex` 再高亮挂载行，禁止恢复全量 `v-for="song in songs"`。
- 打开播放器/队列 overlay 时必须锁定底层路由页交互与滚动：`ion-router-outlet` 设 `pointer-events: none`，`body.muses-overlay-open ion-router-outlet ion-content` 禁用滚动；不要锁住队列 overlay 自己的 `ion-content`。
- 播放器 overlay 自身使用 `touch-action: none`，并在非原生可交互控件（含 `input` / `ion-range` / `ion-button` 等）上对 `touchmove` 调用 `preventDefault`，防止滑动穿透到底层歌曲列表；进度条保留可拖动。
- **进度条手势隔离**：`.progress-area` 必须 `@touchstart.stop` / `@pointerdown.stop`，并配合短 debounce 的 `seekGestureLocked`；seek 期间/刚结束后禁止 `playPreviousFromQueue` / `playNextFromQueue`，也禁止横向切换 `activePanel`，避免松手点穿到上一曲/下一曲或误切歌词面板。`isNativeInteractiveEvent` 必须识别 `ion-range` / `.progress-range`（不仅是原生 `input`）。
- **进度条使用 `ion-range`（无可见圆点）**：
  - 控件：`<ion-range class="progress-range">`，`min=0`，`max=duration`（duration 为 0 时 max 兜底为 1 并禁用），`step` 细粒度（如 `0.1`），`value` 绑定 `effectiveSeekPosition`（拖动 preview 优先，否则 `playerState.position`）。
  - **`onSeekInput` 仅在 `seekGestureLocked` 为 true 时写 preview**：`ion-range` 在 `value` 属性变化时会 emit `ionInput`；无手势锁时必须忽略，否则 preview 冻住填充、播放进度看似不走（#47）。
  - 隐藏 knob：`--knob-size: 0`、`--knob-box-shadow: none`，必要时透明 `--knob-background`；桌面与窄屏均不可见圆点，但轨道仍可点击/拖动 seek。
  - 轨道视觉用 ion-range 自带 bar：`--bar-background`（未播放）、`--bar-background-active`（已播放）、`--bar-height`；**不再维护** `.progress-track-buffered` / 自绘三层缓冲 DOM，也不再注入 UI 用的 `--buffered` CSS 变量。
  - 事件：`ionInput` → 更新 preview + `seekGestureLocked`；`ionChange` → `seekPlayback` + 解锁调度。缓冲已知时 UI 侧仍将目标 clamp 到 `bufferedPosition`，越界轻提示「缓冲中」；`seekPlayback` 业务 clamp/拒绝语义不变。
  - **缓冲未知**（`playerState.bufferedPosition == null`）时不画假缓冲条；WebDAV 远程直链固定属于此状态，seek 退化为 duration clamp。
  - **歌词行点击**：目标 > `bufferedPosition` 时不 seek（与进度条共用 `seekPlayback` 拒绝语义）。

### 隐藏播放器渲染降载约定

`App.vue` 在有当前曲时保活 `PlayerPage`，关闭态不得恢复销毁重建；关闭态可用 `visibility: hidden` 与 `contain: paint` 跳过不可见绘制。`PlayerPage.vue` 的 AMLL `current-time` 在可见时跟随 `playerState.position`，隐藏时冻结；重新打开时必须以最新 position 同步，避免白闪和旧歌词位置。后台音频、MediaSession 和播放进度状态不受影响。

### Overlay 组件必须异步加载（首屏性能约定）

**What**: `App.vue` 中 `PlayerPage` / `QueuePage` 必须用 `defineAsyncComponent(() => import(...))` 异步加载，不能用静态 `import`。

**Why**: `PlayerPage` 静态 `import @applemusic-like-lyrics/*` + `@pixi/*` 整套 WebGL 库（gzip 后上百 KB，原体积 ~400KB+）。一旦静态 import 被打进 `App.vue`，这些库就进入首屏必须同步下载/执行的主 bundle，直接导致打开应用白屏几秒。改为 `defineAsyncComponent` 后，Vite/Rollup 把 PlayerPage 及其依赖切成独立异步 chunk，仅在 `v-if="playerOverlayVisible"` 首次为 true（即用户点开播放器全屏页）时才加载。实测主入口 JS 从 1.5MB 降到 38KB。

**Example**:
```ts
// ✓ 正确——异步加载，重量级库进独立 chunk
import { defineAsyncComponent, onMounted } from 'vue'
const PlayerPage = defineAsyncComponent(() => import('@/views/PlayerPage.vue'))
const QueuePage = defineAsyncComponent(() => import('@/views/QueuePage.vue'))
```
```ts
// ✗ 错误——静态 import，drag AMLL/Pixi 进首屏主 bundle，导致白屏
import PlayerPage from '@/views/PlayerPage.vue'
import QueuePage from '@/views/QueuePage.vue'
```

**Related**: `vite.config.ts` 的 `build.rollupOptions.output.manualChunks` 已将 `@applemusic-like-lyrics` + `@pixi` 锁定到 `amll-pixi` chunk、`@ionic/vue`+`ionicons` 到 `ionic` chunk、`vue`+`vue-router` 到 `vue-vendor` chunk，利于长期缓存。新增任何重量级（>50KB）第三方库到 overlay 页面时，同样适用此约定；不要再用静态 import 把它拽进主 bundle。

**Gotcha**: 异步组件首次解析有极短延迟；如果 `<Transition>` 动画出现时序问题，给 `defineAsyncComponent` 传 `loadingComponent` / `delay` 选项，不要回退到静态 import。MiniPlayer 必须保持静态 import（它依赖很轻且首屏底栏需始终可见，不能等异步加载）。

## SourcesPage 扫描默认值约定

`src/views/SourcesPage.vue` 的扫描设置弹窗在 `openScanSettings(source)` 中按音源类型设置 `scanOptions.readTags` 默认值；顶层 `scanOptions = ref<ScanOptions>({ readTags: true })` 仅作初始占位，实际使用前会被 `openScanSettings` 覆写。

### 规则

1. **WebDAV 默认关闭 `readTags`**：WebDAV 读标签需逐文件网络请求读取原生元数据，开启会明显变慢、易卡顿。`src/features/library/scanner.ts` 中 `options.readTags` 决定是否调用 `readWebDavAudioTags` / `WebDavNative.readMetadata`，关闭时回退为文件名标题。
2. **本地音源默认开启 `readTags`**：本地元数据读取无网络开销，无需回归。
3. **用户仍可手动切换**：弹窗中的 `HSwitch v-model="scanOptions.readTags"` 不受默认值影响，用户可随时打开/关闭。

### 正确实现

```ts
const openScanSettings = (source: SourceItem): void => {
  selectedScanSource.value = source
  // WebDAV 默认关闭读标签（避免网络逐文件读取导致慢/卡）；本地默认开启
  scanOptions.value = { readTags: source.type !== 'webdav' }
  resetScanProgress()
  isScanSettingsOpen.value = true
}
```

### 避免

- 在 `openScanSettings` 中对所有音源统一写死 `{ readTags: true }`——会让 WebDAV 扫描默认逐文件读标签。
- 把 WebDAV 默认 `false` 提到 scanner 层（`scanSourceLibrary`/`readTagsSafely`）——读标签与否是扫描期用户可调的偏好，应由调用方在 `ScanOptions` 中明确传入，scanner 只负责如实执行 `options.readTags`。
- 删除顶层 `scanOptions` 初始值或改为函数式默认——弹窗打开前必经 `openScanSettings` 覆写，初始占位值保持 `{ readTags: true }` 即可，无需引入额外抽象。

参考文件：`src/views/SourcesPage.vue`、`src/features/library/scanner.ts`、`src/features/library/types.ts`（`ScanOptions`）。

## QueuePage / PlaylistDetailPage 虚拟列表约定

`QueuePage.vue` 和 `PlaylistDetailPage.vue` 的长队列/歌单列表使用 `@tanstack/vue-virtual`，避免一次挂载全量 Ionic 行。

### 规则

1. 虚拟器必须使用真实原生滚动容器，容器设置 `overflow: auto`、`min-height: 0` 和 `box-sizing: border-box`。
2. 保留原生 HTML 包装行，行带 `data-index`，通过 `measureElement` 测量；不要直接把 Ionic Web Component 实例作为测量目标。
3. 不要为虚拟器未就绪状态回退渲染完整数组，否则大列表首帧仍会创建全量 DOM。
4. `ion-item-sliding` 不与虚拟行复用混用；需要删除时使用明确的行尾按钮，并用 `event.composedPath()` 防止按钮事件触发整行播放。
5. 队列必须保留当前项 `aria-current`、当前项定位、播放、删除、清空和空态；歌单必须保留播放全部、单曲播放、移除、封面、空态和数据更新刷新。

参考文件：`src/views/QueuePage.vue`、`src/views/PlaylistDetailPage.vue`、`@tanstack/vue-virtual`。

## SourcesPage 虚拟列表行高测量约定

`src/views/SourcesPage.vue` 使用 `@tanstack/vue-virtual` 渲染音源卡片。窄屏/竖屏下 Ionic 卡片实际高度会因副标题、内边距等超过固定估算值；若只依赖 `estimateSize` 而不实测行高，后续行的 `translateY` 会偏小，导致卡片互相覆盖并遮挡扫描按钮。

### 规则

1. **保留 `estimateSize` 仅作首屏占位**，挂载后必须用 `measureElement` 用真实高度重算。
2. **测量目标必须是原生 HTML 元素**，不要直接测 `ion-card` 等 Web Component（Vue 组件实例/`$el` 与库的 `HTMLElement` 期望不一致）。正确做法是外包一层原生 `div` 作为虚拟行。
3. **虚拟行必须带 `data-index`**（与库默认 `indexAttribute` 一致），并通过 ref 回调调用 `rowVirtualizer.value.measureElement(...)`。
4. **行间距必须进入实测高度**：不要用 `margin` 做绝对定位行的垂直间距（`offsetHeight` 通常不计入 margin）。应把间距放进测量容器的 `padding` / `border-box`。
5. **ref 回调只向 `measureElement` 传入 `HTMLElement | null`**：卸载时传入 `null`，由库清理断开节点。

### 正确实现

```vue
<div
  v-for="virtualRow in virtualRows"
  :key="sources[virtualRow.index].id"
  :ref="measureVirtualRow"
  class="source-card-row"
  :data-index="virtualRow.index"
  :style="{ transform: `translateY(${virtualRow.start}px)` }"
>
  <ion-card class="source-card">...</ion-card>
</div>
```

```ts
const measureVirtualRow = (element: Element | ComponentPublicInstance | null): void => {
  rowVirtualizer.value.measureElement(element instanceof HTMLElement ? element : null)
}
```

```css
.source-card-row {
  position: absolute;
  top: 0;
  left: 12px;
  right: 12px;
  box-sizing: border-box;
  padding-block: 8px; /* 间距计入实测高度 */
}

.source-card {
  min-height: 100px;
  margin: 0; /* 不要用 margin 承担行间距 */
}
```

### 避免

- 只写 `estimateSize: () => 148`、不接 `measureElement` / `data-index`。
- 把 `position: absolute` + `translateY` 直接绑在 `ion-card` 上并指望固定估算值在窄屏仍准确。
- 用 `margin: 8px 0` 做绝对定位行间距，导致测量高度小于实际占位。
- 在 ref 回调里直接访问组件实例的 `$el` 而不做 `HTMLElement` 收窄（类型不安全，且测量目标应优先选原生包装节点）。

参考文件：`src/views/SourcesPage.vue`、`@tanstack/vue-virtual`。

—

## Accessibility

The current codebase includes a few baseline patterns that should be preserved:

- Decorative icons use `aria-hidden="true"` in `src/views/TabsPage.vue`
- External links in `src/components/ExploreContainer.vue` include `target="_blank"` with `rel="noopener noreferrer"`
- Tab buttons use visible `IonLabel` text

Preserve these practices when extending the UI.

Avoid:

- Icon-only controls without accessible labels
- External links opened in new tabs without `rel="noopener noreferrer"`
- Replacing visible labels with only decorative icons in navigation

---

## Common Mistakes

Given the current app shape, common mistakes to avoid are:

- Putting route logic inside page components instead of `src/router/index.ts`
- Adding `/player` or `/queue` routes for immersive playback; these surfaces are global overlays, not route pages
- Forgetting to import Ionic components explicitly in the SFC script block
- Mixing global theme concerns into component-local styles
- Introducing new architectural layers (store, services, composables) without an actual need in the task
- Wrapping a nested parent route shell in `ion-page` when child route pages already provide Ionic page containers; in Android WebView this can cause duplicate navigation chrome or stacked layouts
- Reintroducing `ion-split-pane` / `ion-menu` for the main tabs shell without MuMu regression testing; this previously caused a white screen in the Android emulator
- Using `ion-tab-bar` / `ion-tab-button` outside an `ion-tabs` shell in `TabsPage.vue`; in the custom parent route shell, use a plain `<nav>` with `RouterLink` to avoid missing or duplicated mobile bottom navigation
- Relying only on `@click.stop` for nested `ion-button` controls inside a clickable parent; guard the parent handler with `event.composedPath()` so button clicks do not trigger parent navigation
- Hiding `MiniPlayer` with `v-if` while a player overlay is open; keep it mounted behind the overlay and disable interaction to avoid close-animation flicker
- Setting immersive `.cover` width without a height-based cap（窄屏只写 `min(72vw, 100%, 340px)` 或宽屏只写 `min(40vw, 320px)`）while `.cover-slot` clamps height via `max-height: min(…dvh, …)`；矮高/横屏时正方形高度被 clamp、宽度不变 → 封面被压成长方形。窄屏与宽屏 `.cover` width 都必须同步含 dvh/`max-height` 对齐的上限
- 矮屏控制页只缩按钮却不收 panel padding / `info-panel-inner` gap / 进度热区，导致控制区仍占过多垂直空间、封面槽位被挤；或为腾空间隐藏模式栏/进度——应分层 `max-height` 收紧尺寸，保留全部控件

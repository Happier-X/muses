# Navbar 固定在顶部

## Goal

列表/设置等业务页滚动时，顶部 `HNavBar` 必须始终留在可视区域顶部，不随内容一起滚走。恢复「顶栏固定 + 内容区独立滚动」的页面骨架体验。

## Background

### 已确认事实

- `happier-ui` 的 `HNavBar` 默认 `fixed: true`，`fixed` 时样式为 `position: fixed; top/left/right: 0; z-index: var(--h-z-nav)`（`node_modules/happier-ui/dist/styles.css` / 源码 `nav-bar.css`）。
- Muses 页面骨架**有意**关闭组件级 fixed：`MPage.vue` 与 Songs/Playlists/Sources/Settings/PlaylistDetail/Queue 等页均写 `:fixed="false"`，依赖 flex 文档流顶栏 + `.m-content` 内部滚动（见 `.trellis/spec/frontend/component-guidelines.md`「页面骨架 Pattern」）。
- `.m-page`：`display:flex; flex-direction:column; height:100%; overflow:hidden`（`src/theme/tailwind.css`）。
- `.m-content`：`flex:1; overflow-y:auto; overscroll-behavior:contain`。
- `TabsPage.vue` 在 `md+` 将 `<main>` 设为 `md:fixed md:top-0 … md:overflow-auto`，使 **整页（含 navbar）** 成为 main 的滚动内容，顶栏随 main 滚动——这是桌面/平板侧最直接的回归点。
- 脱离 Ionic 后，高度链依赖宿主；`index.html` 的 `#app` 与 `html/body` 未显式保证 `height: 100%` / `100dvh` 时，`.m-page { height:100% }` 可能无法形成约束高度，导致 overflow 落在更外层滚动容器上（迁移设计曾假设 preflight 覆盖 html/body 高度，需在本任务中核验并补齐）。
- **结论：这是 Muses 使用与布局问题，不是 happier-ui 组件库缺陷；不向组件库提 issue。**

### 用户价值

滚动浏览歌曲/歌单/设置时，标题与右侧操作按钮始终可触达，符合常见移动端/桌面壳层导航预期。

## Requirements

1. **R1 顶栏不随内容滚走**  
   在 Tabs 内各业务页（歌曲、专辑、艺术家、歌单、音源、设置、歌单详情）滚动内容时，顶部 `HNavBar` 保持在该页可视区域顶部。

2. **R2 滚动归属在内容区**  
   垂直滚动应发生在 `.m-content`（或虚拟列表页内部列表容器），而不是把整页（navbar + 内容）当作同一滚动体。

3. **R3 保持现有骨架契约**  
   继续采用「`HNavBar :fixed="false"` + `.m-page` / `.m-content` flex 骨架」；不改为全局 `position: fixed` 顶栏方案（避免与侧栏、`safe-area`、overlay 页重复占位纠缠）。

4. **R4 断点一致**  
   窄屏（底部 Tab）与 `md+`（侧栏 + main）行为一致：顶栏钉住、内容滚。

5. **R5 Overlay 页不回归**  
   `QueuePage` / `PlayerPage` 全屏 overlay 行为与现有一致；Queue 顶栏仍为 overlay 内文档流头部即可。

6. **R6 不误伤组件库**  
   不修改 `happier-ui` 发布包；不提交组件库 issue（根因在宿主布局）。

## Out of Scope

- 将业务页顶栏改为 `HNavBar fixed=true` + 占位/padding 方案。
- 重做侧栏信息架构或 Tab 路由。
- 修改 MiniPlayer / 沉浸播放器布局。
- happier-ui 源码改动或发版。
- 为所有手写 `m-page` 页强制迁移到 `<m-page>` 组件（可顺手统一，非必须验收项）。

## Acceptance Criteria

- [ ] **AC1** 歌曲页（及至少歌单/设置各一页）在内容足够长时向下滚动，顶栏标题与右侧操作仍完整可见，不移出视口顶部。
- [ ] **AC2** `md+`（≥768px）侧栏布局下同样满足 AC1；滚动条/滚动手势作用在内容区，而非整页把 navbar 卷走。
- [ ] **AC3** 窄屏底部 Tab 布局下同样满足 AC1；TabBar / MiniPlayer 既有固定层不因本次改动消失或错位。
- [ ] **AC4** 打开 Queue / Player overlay 后关闭，回到列表页，顶栏仍符合 AC1。
- [ ] **AC5** 源码中 Tabs 主内容区不再用「整页 `overflow-auto`」承载带 navbar 的业务页滚动（实现以 design/implement 为准）。
- [ ] **AC6** 不新增对 `happier-ui` 的 file: 链接或本地 alias；不向 happier-ui 仓库提 issue。

## Key Decisions

| 决策 | 选择 | 理由 |
|------|------|------|
| 根因归属 | Muses 布局/使用 | 库默认 fixed 可用；宿主刻意 false + 错误滚动父级 |
| 修复策略 | 修滚动归属 + 高度链 | 与现有 spec 骨架一致，改动面小 |
| 是否 `fixed=true` | 否 | 与 component-guidelines 及 modal/overlay 约定冲突成本高 |
| 组件库 issue | 否 | 非库 bug |

## Risks / Notes

- 补 `html/body/#app` 高度时避免 `position:fixed` 浮层（MiniPlayer、TabBar、Player）错位；优先 `height:100%` 链或受限的 `100dvh`，并做 overlay 回归。
- `TabsPage` main 从 `md:overflow-auto` 改为不滚动后，原先依赖 main 滚动的边缘页需确认都走 `.m-content`。

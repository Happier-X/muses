# 设计文档：删除测试并改用 Vue 3 声明式 DOM 操作

## 目标

删除 `tests/unit/`（外加 vitest 与 cypress 全套脚手架）；把 5 处命令式 DOM 操作改为 Vue 3 推荐方式；清理因此无用的标记类。

## 技术映射

### 1. MiniPlayer：`composedPath` + `player-actions` 父代理解析

**现状**：按钮已有 `@click.stop`（`togglePlayback`/`openQueuePage`），父级 `@click` 的 `isPlayerActionEvent` 是**冗余防御层**，用 `composedPath` 再走一遍 class 过滤。

**改法**：删掉 `isPlayerActionEvent` 函数 + 删掉 `openPlayerPage` 里的 `composedPath` 调用，直接信任 `@click.stop`。

**连带去除的类**：`player-actions`

### 2. PlaylistDetailPage：`composedPath` + `more-button`

**现状**：`onPlaySong` 用 `composedPath().some(... 'more-button')` 忽略移除按钮区域的点击。移除按钮已有 `@click.stop`。同类冗余防御。

**改法**：删掉 `onPlaySong` 内的 `composedPath` 检查，信任 `@click.stop`。

**连带去除的类**：`more-button`

### 3. QueuePage：`composedPath` + `remove-button`

**现状**：`onSelectSong` 用 `composedPath().some(... 'remove-button')` 判断。移除按钮已有 `@click.stop`。

**改法**：删 `onSelectSong` 内的 `composedPath` 检查。

**连带去除的类**：`remove-button`

### 4. SongsPage FAB 跳转：`querySelectorAll('[data-song-id]')` → 行 ref

**现状**：`findSongRow` 用 `root.querySelectorAll('[data-song-id]')` 获取目标行，再用 `scrollIntoView`。

**改法**：利用虚拟列表已有的 `v-for` 和 `data-song-id`，给每行挂 `ref`；维护一个 `songRowRefs: Map<string, HTMLElement>`，通过 `@tanstack/vue-virtual` 的 `measureElement`ref 收集行 DOM。`scrollToCurrentSong` 改用 `rowVirtualizer.scrollToIndex` 直接定位索引（已有此调用），后面 `scrollIntoView` 可用 `songRowRefs.get(currentId)?.scrollIntoView()` 兜底。

> **注意**：`findSongRow` 也不只是为 FAB 调用——以后如果需要也可被外部 used。改造只影响本组件内部调用 `scrollToCurrentSong`。

**`data-song-id` 属性保留**（不依赖它查询，仅用于 `songItemClass` 的行身份标识；`data-index` 更好但改会涉及多处）。

### 5. PlayerPage 手势：`closest` / `classList.contains` / `.progress-range` 选择器

**约束**：`lyric-panel`、`lyric-player`、`progress-range` 都是 `tailwind.css` 全局级联锚点——**class 语义必须留在模板上**。改造只改 JS 端的落点判断表达式。

**改法**：

| 原 JS 表达式 | 改成 |
|---|---|
| `event.target instanceof Element && event.target.closest('.lyric-panel, .lyric-player')` | `lyricPanelRef.value?.contains(event.target as Node) \|\| lyricPlayerRef.value?.contains(event.target as Node)` |
| `return target.classList.contains('lyric-panel') \|\| target.classList.contains('lyric-player')` (composedPath fallback) | 删除（ref 子节点 contains 已覆盖） |
| `el.closest('.progress-range')` (INTERACTIVE_SELECTOR 内) | `progressRangeRef.value?.contains(el)` |
| `INTERACTIVE_SELECTOR` 剩余项（`input, textarea, select, button, a, [role="button"], [contenteditable="true"]`） | **保留原生选择器**（不是 class 标记，是标准元素/属性选择器，完全合法） |
| `canStartVerticalDismiss` 里的 `composedPath` + `scrollTop` 检查 | **保留**（需读 DOM 属性，非 class 查询，无可声明式替代） |

**新增 ref**：`lyricPanelRef`、`lyricPlayerRef`、`progressRangeRef`（template ref 字符串绑定）。

## 标记类：保留 vs 删除

**保留**（tailwind.css 全局锚点；删了会布局回归）：
- `lyric-panel`, `lyric-player`, `lyric-fab`, `lyric-floating-actions`, `lyric-play-toggle`, `lyric-header`, `lyric-title`, `lyric-artist`
- `progress-range`, `progress-area`
- `panel`, `panels`, `info-panel`, `info-panel-inner`
- `cover`, `cover-slot`, `placeholder-cover`
- `controls`, `mode-bar`, `play-toggle`
- `empty-state`, `fallback-background`, `song-info`, `time-row`
- `m-page`, `m-content`, `m-content--fullscreen`
- `player-overlay`
- 动态状态类：`is-playing`, `is-empty`, `is-overlay-active`, `is-player-visible`, `is-dragging`, `is-active`, `is-visible`

**删除**（零 CSS 锚点、零 JS 引用——仅测试/冗余防御）：
- `player-actions`
- `more-button`
- `remove-button`
- `amll-background`
- `amll-background-render`
- `immersive-shell`
- `mini-player`
- `app-mini-player`
- `app-player-page`
- `m-cover`
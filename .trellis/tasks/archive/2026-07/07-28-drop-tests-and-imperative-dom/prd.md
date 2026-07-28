# 删除测试并改用 Vue3 推荐方式消除 DOM 命令式操作与标记类

## Goal

去掉对单元测试的依赖；把命令式 DOM 操作（`event.composedPath` / `classList.contains` / `querySelector`）改成 Vue 3 声明式写法；从而把仅被测试或命令式 DOM 依赖的标记类一并清掉。用户会自己做手动测试。

## 背景 / 已确认事实（来自代码探查）

### 单元测试现状
- `tests/unit/` 下共 17 个 spec：`cover` / `example` / `icons` / `library` / `library-views` / `loudness` / `lyrics*` / `metadata` / `navbar-title` / `player` / `playlist` / `sources` 等。
- `package.json` 有 `test:unit`（vitest）与 `test:e2e`（cypress）。
- 多处标记类仅因测试断言保留：`.amll-background`、`.immersive-shell`、`.mini-player`、`.app-mini-player`、`.app-player-page`、`.m-cover`。

### 命令式 DOM 操作点（src）
| 位置 | 当前写法 | 用途 |
|---|---|---|
| `MiniPlayer.vue` | `event.composedPath()` + `classList.contains('player-actions')` | 父级点击打开播放器时，忽略动作按钮区域 |
| `PlaylistDetailPage.vue` | `composedPath` + `more-button` | 行点击播放时忽略「更多」按钮 |
| `QueuePage.vue` | `composedPath` + `remove-button` | 行点击选中时忽略「移除」按钮 |
| `SongsPage.vue` | `querySelectorAll('[data-song-id]')` + `scrollIntoView` | FAB 跳到当前播放行 |
| `PlayerPage.vue` | `composedPath` / `closest('.lyric-panel, .lyric-player')` | 手势：歌词区内不触发下滑关闭；交互控件不吞手势 |

### 与标记类的关系
- 一旦去掉测试依赖 + 用 Vue 3 声明式替代 `classList.contains`，下列类可再评估删除：`player-actions`、`more-button`、`remove-button`、`amll-background*`、`immersive-shell`、`mini-player`、`app-mini-player`、`app-player-page`、`m-cover`（若仅测试用）、部分仅手势判断的 `lyric-panel`/`lyric-player`（若 CSS 全局锚点仍需要则保留）。
- `src/theme/tailwind.css` 里仍有大量 `.player-overlay .xxx` 全局级联锚点（`.cover`、`.controls`、`.mode-bar` 等）——**这些不是测试依赖，删了会破坏布局，必须保留**。

## Requirements（待 brainstorm 收敛）

- R1：删除整个 `tests/unit/` 目录。
- R2：命令式 DOM 改为 Vue 3 推荐声明式写法（具体范围见未决决策 3）。
- R3：在 R1/R2 完成后清理因此变为无用的标记类。
- R4：保留 tailwind.css 全局级联必需的锚点类；保留 JS 运行时动态状态类（`is-playing` 等）若仍有 CSS 依赖。
- R5：不引入新的单元测试；用户自行手动验收。
- R6：`npm run build` + `npm run lint` 全绿。
- R7：同步移除 vitest（`test:unit` 脚本、`vitest` / `@vue/test-utils` / `jsdom` 依赖、`vite.config.ts` 的 `test:` 块与 `<reference types="vitest" />`）。
- R8：一并删 cypress（`tests/e2e/` 整目录、`cypress.config.ts`、`cypress` 依赖、`test:e2e` 脚本）—— 属死样板（断言 `ion-content` 早已被删除，必挂）。

## Acceptance Criteria（草稿，待确认）

- [ ] AC1：`tests/unit/` 整目录已删除。
- [ ] AC1b：vitest 脚本 / 依赖 / 配置已移除。
- [ ] AC1c：cypress 脚本 / 依赖 / 配置 / `tests/e2e/` 已删除。
- [ ] AC2：src 中无 `composedPath` + 标记 class 的事件代理模式；剩 `canStartVerticalDismiss` 中的 DOM scrollTop 检查（无声明式替代，详见 design.md）。
- [ ] AC3：仅因测试/命令式 DOM 存在的标记类已移除（.player-actions / .more-button / .remove-button / .amll-background{,-render} / .immersive-shell / .mini-player / .app-mini-player / .app-player-page / .m-cover）。
- [ ] AC4：`npm run build` + `npm run lint` 全绿。
- [ ] AC5：用户手动验证关键交互（MiniPlayer 按钮不误开播放器、队列移除、更多菜单、歌词手势、FAB 跳转）。

## 已确认决策

1. **单元测试删除范围 = 乙**：删除整个 `tests/unit/`。
2. **测试基础设施 = 同步移除 vitest + cypress**：两者皆删（cypress 属死样板，断言 `ion-content` 必挂）。
3. **命令式 DOM 改造范围 = 乙**：5 处全改声明式（含 PlayerPage 手势）。
   - MiniPlayer / PlaylistDetailPage / QueuePage：按钮加 `@click.stop`，删除 `composedPath` + `classList.contains` 父代理解析。
   - SongsPage FAB：`querySelectorAll('[data-song-id]')` + `scrollIntoView` 改为虚拟行 ref 数组（或 `data-index` + 行 ref map），不再查询 DOM。
   - PlayerPage 手势：`closest('.lyric-panel, .lyric-player')` / `classList.contains('.lyric-panel')` 改为根容器模板 ref + `event.target` 落点判断（`ref.value?.contains(event.target as Node)`）；`INTERACTIVE_SELECTOR` 中的 `.progress-range` 改为进度条 ref。

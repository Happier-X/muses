# 清理冗余的标识类和单元测试

## Goal
移除在完成 Tailwind CSS 迁移后残留在 Vue 组件 template 中的纯无用类名（Marker Classes），让代码更加简洁；同时移除对这些类名有强依赖的非必要单元测试，从而减轻维护负担。

## Requirements
- **R1: 清理残留类名**：遍历项目中所有的 Vue 组件，移除所有不再用于样式的无用 class（例如 `.album-card`、`.artist-card__avatar`、`.album-grid`、`.song-item` 等等）。
- **R2: 保留必要类名**：
  - 保留那些供 `src/theme/tailwind.css` 做全局级联样式锚定的类名（特别是 `PlayerPage` 相关的那些如 `.player-overlay`、`.lyric-panel`、`.immersive-shell` 等）。
  - 保留 JS 事件代理或逻辑查询需要的类名（例如 `MiniPlayer` 中的 `player-actions`，或者其他 `event.composedPath`、`querySelector` 会用到的类）。
  - Vue 的动态状态 class (如 `is-playing`) 若仅由 Tailwind 替代，可视情况处理，但须保证 JS 逻辑不受损。
- **R3: 删减单元测试**：
  - 移除 `tests/unit/example.spec.ts` 中针对这些视觉类名是否存在的强耦合测试。
  - 由于用户指出“单元测试没有必要”，如果某些测试已经完全丧失意义或严重依赖于被删掉的类，可以直接移除该用例。
- **R4: 保持零回归**：移除这些类名和测试不应破坏应用的实际运行表现和现有的 Tailwind 布局。

## Acceptance Criteria
- [ ] AC1：项目构建与 Lint 全绿 (`npm run build` / `npm run lint`)。
- [ ] AC2：大部分组件中的无用标识类已被清理干净。
- [ ] AC3：现存的单元测试（如果有保留的）依然能通过（`npm run test:unit`）。
- [ ] AC4：应用的基本运行时交互（如点击 MiniPlayer 控制区、歌词 FAB 跳转等）仍正常工作。

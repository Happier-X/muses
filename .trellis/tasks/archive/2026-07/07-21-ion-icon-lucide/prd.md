# ion-icon 图标库换为 Lucide

## Goal

关闭 GitHub #42：业务侧所有 `ion-icon` 的图标数据不再来自 `ionicons/icons`，改为 **Lucide**；**继续使用 `ion-icon` 组件**（含 `slot="icon-only"` 等现有写法）。

## Background

- 现状：多页面 `import { ... } from 'ionicons/icons'`，经 `:icon="..."` 传给 `ion-icon`。
- `ionicons/icons` 导出的是 `data:image/svg+xml;utf8,<svg...>` 字符串，`ion-icon` 的 `:icon` 可直接吃这种格式。
- 用户决策：只换图标库，不换组件；不引入 `lucide-vue-next` 组件树替代 `ion-icon`。
- `package.json` 仍可能因 Ionic 间接依赖保留 `ionicons` 包，但业务代码禁止再从 `ionicons/icons` 导入。

## Requirements

### R1. 依赖与适配
- 新增 Lucide 依赖（优先 `lucide` 可 tree-shake 的 icon 模块；若实现更简单可用 `lucide-static`）。
- 提供薄适配层（建议 `src/icons/` 或 `src/features/ui/icons.ts`）：
  - 将 Lucide 图标转为 `ion-icon` 可用的 `icon` 值（与现网一致的 data-URI SVG 或等价 `src`）。
  - 统一 stroke 风格（Lucide 默认 outline，viewBox 24），与当前 UI 尺寸/颜色通过 `ion-icon` CSS 继承即可。
- **禁止**业务页面直接 `import ... from 'ionicons/icons'`。

### R2. 全量替换业务图标
以下文件中的 ionicons 导入与图标引用全部改为 Lucide 映射（名称可集中导出，如 `playIcon`、`pauseIcon`）：

| 文件 | 当前 ionicons 示例 |
|------|-------------------|
| `MiniPlayer.vue` | list, musicalNotes, pause, play |
| `PlayerPage.vue` | play/pause/skip/repeat/shuffle/list/language/… |
| `QueuePage.vue` | chevronBack, close, musicalNotes, trash |
| `TabsPage.vue` | albums, list, musicalNotes, person, radio, settings |
| `SongsPage.vue` | search, shuffle, musicalNotes, ellipsis, locate |
| `PlaylistsPage.vue` | add, ellipsis, list |
| `PlaylistDetailPage.vue` | play, musicalNotes, remove |
| `SourcesPage.vue` | add |
| `tests/unit/player.spec.ts` 等 | 图标相等断言改为 Lucide 映射后的值 |

语义映射原则（实现时定表，PRD 层要求「功能语义等价」）：

- play / pause / skip back·forward / shuffle / repeat / list / search / add / close / trash / settings / person / radio(源) / albums / music note / language / locate / more-vertical 等对应 Lucide 最接近图标。
- 播放模式状态切换（列表循环 vs 单曲、顺序 vs 随机）仍须靠**不同** Lucide 图标区分，不得两个状态共用同一图标。

### R3. 视觉与交互不变（除字形风格）
- 布局、slot、`aria-label`、按钮行为不变。
- 允许 Lucide 线性风格与 ionicons 填充/线框混用风格有差异；不要求像素级复刻。
- `ion-icon` 尺寸与颜色仍由现有 CSS / `color` 控制。

### R4. 测试与规范
- 更新依赖图标引用的单测（如播放模式图标相等断言）。
- 同步 `.trellis/spec` 中「使用 ionicons/icons」的表述为 Lucide + `ion-icon` 约定。
- lint / type-check / 相关 unit 通过。

### Out of Scope
- 不把 `ion-icon` 换成 `<LucideIcon />` 或其它 Vue 图标组件。
- 不强行从 `package.json` 删除 `ionicons`（若 Ionic 仍需要）；仅业务侧停用。
- 不发版（除非你另行要求）。
- 不改图标业务逻辑（仅资源与导入路径）。

## Acceptance Criteria

- [ ] 业务代码无 `from 'ionicons/icons'`（可用 ripgrep 验收）
- [ ] 所有原 `ion-icon` 使用点仍渲染可见图标（Tabs / MiniPlayer / 播放页 / 队列 / 歌�� / 歌单 / 音源）
- [ ] 播放模式、循环模式等状态切换仍显示不同图标
- [ ] 相关单测通过；lint / vue-tsc 通过
- [ ] 关闭 GitHub issue #42

## Notes

- 中等复杂度、以迁移为主：PRD-only 可启动；实现前在适配层定一张「ionicons → Lucide」对照表即可。
- 关联 issue：#42。

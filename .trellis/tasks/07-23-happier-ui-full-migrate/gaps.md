# happier-ui 组件缺口清单（Muses → 未来库开发）

> 策略：`happier-ui@0.0.1` **没有的先不替换**；本文件记录真实落点，供以后在 **happier-ui 仓库** 开发与发版后回迁。
> 本轮 **不** 在 Muses 新建通用平行组件，**不** 跨仓实现库代码。

## 图例

| 字段 | 含义 |
|------|------|
| 候选名 | 建议在 happier-ui 的导出名 |
| 优先级 | P0 阻塞多页 / P1 明显重复 / P2 可后置 |
| 状态 | `open` 未做 · `partial` 仅图标等已迁 · `done` 库已提供且 Muses 已换（本文件最终应无 done 或移出） |

## 缺口表（初稿，实现中持续补全）

| 候选名 | 优先级 | 状态 | 当前落点（文件/场景） | 所需能力摘要 | 阻塞的替换范围 |
|--------|--------|------|------------------------|--------------|----------------|
| `HListRow` | P0 | open | `SongsPage`、`QueuePage`、`PlaylistDetailPage` 已回退 `ion-item`；Albums/Artists/Playlists 同样保留 `ion-item` 双行 | title/subtitle、leading/trailing slot、playing/selected、可点击 a11y | 曲目/队列/歌单行统一语义 |
| `HSettingRow` | P0 | open | `SettingsPage` 已回退 `ion-item` + `HLabel`/`HSwitch` 组合 | label、description、end 槽（switch/button） | 设置页行壳 |
| `HIconButton` | P0 | partial | MiniPlayer 控键、列表 more/移除、Queue 顶栏、Player 主控等 **icon-only** 已保留 `ion-button` 壳并将图标迁为 `HIcon` | ≥48 热区、aria-label、variant（ghost/on-media/danger）、slot/`HIcon` | 无法用 `HButton` 干净表达的纯图标触控 |
| `HListSection` | P1 | open | 多页 `ion-list` / inset | section header、inset/flat 容器 | 列表分组视觉 |
| `HCard` / `HSurface` | P1 | open | `SourcesPage` `ion-card*` | 轻 surface、无 elevation | 音源卡片 |
| `HNotice` | P1 | open | `SourcesPage` `ion-text` 成功/错误 | tone：info/success/danger | 表单反馈 |
| `HRange` | P1 | open | `PlayerPage` `ion-range` | 无 knob 轨、拖动/缓冲、a11y | 沉浸进度条 |
| `HProgress` | P2 | open | `SourcesPage` 扫描 `ion-progress-bar` | determinate/indeterminate | 扫描进度 |
| `HDialogSurface` / Sheet 内容壳 | P2 | open | Sources/Songs/Playlists 的 modal/alert/sheet **内容区视觉**（引擎可仍宿主） | 标题/操作槽、safe-area | 叠层去 Ionic 观感（非本迭代） |

## 非缺口（本迭代明确保留，不必进库）

| 能力 | 原因 |
|------|------|
| `ion-page` / router outlet / 手势返回 | 应用宿主 |
| `ion-modal` / `ion-alert` / `ion-action-sheet` **引擎** | 保守宿主边界 |
| `MCover` | 音乐封面业务 |
| MiniPlayer / AMLL / WebDAV 逻辑 | 业务 |

## 0.0.1 已有、应对齐替换（非缺口）

`HButton` · `HSwitch` · `HInput` · `HCheckbox` · `HEmpty` · `HImage` · `HIcon` · `HTabBar` · `HNavBar` · `HBottomSheet` · `HDialog`（后两者本迭代不替换宿主引擎）。当前已落地：全仓 `HIcon`/`HEmpty`、移动端 `HTabBar`、设置 `HSwitch`/`HButton`、音源表单 `HInput`/`HCheckbox`/`HSwitch`/`HButton`；`HImage` 不替换 app-only 音乐封面 `MCover`，`HNavBar` 不强行替换复杂 Ionic header。

## 依赖与本地联调约定

- 默认与 CI 一律从 npm registry 安装固定版本 `happier-ui@0.0.1`，不得提交 `file:` 依赖或指向相邻仓库源码的 Vite/TypeScript alias。
- 库侧联调请在本机临时使用 npm link/本地包覆盖；联调完成后恢复 registry 依赖并重新生成锁文件，保证 `npm ci` 可复现。

## 变更日志

| 日期 | 说明 |
|------|------|
| 2026-07-23 | 规划阶段初稿，基于代码扫描与 prd 决策 |
| 2026-07-23 | 实施收口：删除旧 M* 包装；列表/设置行回退 Ionic；图标按钮仅保留 Ionic 壳，图标已迁 `HIcon`；Sources 表单和 Tabs 底栏已使用库组件 |

# happier-ui 组件清单与逐个实现路线

## Goal

梳理 **`happier-ui` 独立库** 应提供的组件边界、已有能力、待实现优先级，以及 Muses 中逐个替换的落点。输出一份可供后续开发执行的路线图；本任务 **只做规划文档**，不要求写组件代码。

## 背景

- 独立库路径：`C:\code\happier-ui`
- 当前已有：`HEmptyState`、`HIconButton`、`HListRow`、`HSettingRow`、`tokens.css`
- Muses 当前仍通过 `@/components/ui` re-export / 包装逐步消费
- 目标：用户将在 `happier-ui` 中 **逐个实现**，再在 Muses 中按页面/组件替换

## 原则

1. **happier-ui 提供通用移动 UI 原语**，不提供 Muses 音乐业务。
2. **首版样式参照 HeroUI Native 移动端**，token 为 `--h-*`。
3. **不复刻 Ionic 全家桶**：导航栈、Modal 生命周期、ActionSheet/Alert 引擎可先留宿主。
4. **优先做能替换 ≥2 处的通用组件**，单点业务先留 Muses。
5. **组件命名用 H***；Muses 中 `M*` 可以作为过渡包装。

## 当前已有组件

| 组件 | 状态 | Muses 使用现状 | 备注 |
|------|------|----------------|------|
| `HEmptyState` | 已有 | `MEmptyState` re-export | 空列表/空数据 |
| `HIconButton` | 已有 | `MIconButton` re-export | 图标按钮；slot SVG 优先 |
| `HListRow` | 已有 | `MListRow` 包装 + `MCover` | 包内不含封面 |
| `HSettingRow` | 已有 | `MSettingRow` re-export | 设置行壳 |
| `tokens.css` | 已有 | `happier-ui/tokens.css` | `--h-*` + `--muses-*` alias |

## 推荐实现优先级

### P0 — 已有能力继续打磨（先完善而不是扩太快）

| 组件 | 要补的能力 | Muses 替换收益 |
|------|------------|----------------|
| `HIconButton` | `variant`: default / ghost / subtle / danger / on-media；loading 可选；禁用态；slot icon 文档 | MiniPlayer、Queue、Songs、PlaylistDetail |
| `HListRow` | `density`、`selected/active`、`leading/trailing` slot 规范、键盘事件文档 | Songs、Queue、PlaylistDetail、Albums/Artists 列表 |
| `HSettingRow` | `interactive` 模式、`description` 多行、`lines`、`as` 或 button 形态 | Settings、Sources 表单 |
| `HEmptyState` | action slot、icon/illustration slot、compact/fullscreen | Artists/Albums/Sources/空列表 |
| tokens | surface / separator / focus / field / danger / success / warning / radius / motion | 全局一致性 |

### P1 — 下一批最值得新增

| 组件 | 解决什么 | Muses 首批替换点 | 备注 |
|------|----------|------------------|------|
| `HPageShell` / `HScreen` | 纯 Vue 页面内壳：标题区、content padding、安全区 | 替代一部分 `MPage` 视觉层；不替代 ion-page 路由壳 | 不做导航栈，只做布局 |
| `HTextButton` / `HButton` | 带文字按钮、主操作、危险操作 | Settings「检查更新」、Sources 操作、播放全部文字按钮 | 当前 ion-button 28 次，文字按钮仍多 |
| `HListSection` | list 容器、section header、inset/flat | Settings、Sources、Playlists/Artists/Albums | 替代重复 ion-list/inset 视觉 |
| `HFormField` | label + description + error + input slot | Sources 的 URL/用户名/密码输入 | 不封装 input 本体，先做 field 壳 |
| `HNotice` / `HInlineMessage` | success/error/info 文案 | Sources 错误/成功提示、设置状态 | 替代散落 ion-text |
| `HCard` / `HSurface` | 轻 surface 容器 | Sources WebDAV 配置卡、后续设置组 | flat，无 elevation |

### P2 — 后续可做

| 组件 | 说明 | 何时做 |
|------|------|--------|
| `HDialogSurface` | Modal 内部内容壳，不接管 modal 引擎 | Sources modal 重复后 |
| `HActionList` | ActionSheet/菜单内选项视觉 | 如果不想用 Ionic ActionSheet 外观 |
| `HProgress` | 进度条 | Sources 扫描进度、下载进度复用时 |
| `HToggle` | 纯 Vue switch | 想完全摆脱 ion-toggle 时 |
| `HCheckbox` | 纯 Vue checkbox | Sources 勾选类增多时 |
| `HRange` | 纯 Vue range | Player 进度若从 ion-range 迁出 |
| `HTabs` | 纯视觉 tab，不管 router | 新非 Ionic 子导航时 |

## 不建议放进 happier-ui 的组件

| 组件/能力 | 留在 Muses 原因 |
|----------|------------------|
| `MCover` | 音乐封面领域语义，含占位图标与尺寸约定 |
| MiniPlayer | 播放器业务强绑定 |
| PlayerPage controls | 手势、AMLL、播放状态强绑定；可只复用 IconButton/Range |
| Queue 业务壳 | 队列操作业务语义 |
| WebDAV 连接逻辑 | 数据源业务 |
| `MPage` 当前 `ion-page` 壳 | Ionic 路由/手势宿主能力，不进通用包 |
| ActionSheet/Alert/Modal 引擎 | 先用宿主，happier-ui 最多提供内部 surface |

## Muses 中 ion 使用现状（本次扫到）

| Ionic 标签 | 次数 | 结论 |
|------------|------|------|
| `ion-button` | 28 | 优先由 `HIconButton` + 新 `HButton/HTextButton` 吃掉一部分 |
| `ion-item` | 18 | `HListRow` / `HSettingRow` / `HFormField` / `HListSection` 分摊 |
| `ion-icon` | 18 | 图标触控走 `HIconButton`；展示图标可留宿主 |
| `ion-toolbar/header/title/content/page/buttons` | 多 | 先留 Ionic app shell；`HPageShell` 只管视觉布局 |
| `ion-list` | 10 | `HListSection` 值得做 |
| `ion-input` | 8 | `HFormField` 包 slot，暂不自研 input |
| `ion-action-sheet/alert/modal` | 各 4 | 暂留宿主 |
| `ion-toggle` | 2 | 后续 `HToggle`，非优先 |
| `ion-range` | 1 | 后续 `HRange`，非优先 |

## 建议实施顺序

1. **先打磨 P0**：把已有 4 组件 API、样式、文档完善。
2. **新增 `HButton` / `HTextButton`**：替换文字按钮与主操作。
3. **新增 `HListSection`**：统一列表分组、inset、section header。
4. **新增 `HFormField` + `HNotice`**：集中处理 Sources 表单/错误提示。
5. **新增 `HSurface/HCard`**：统一设置/来源配置块。
6. 最后再看 `HToggle`、`HRange`、`HDialogSurface`。

## Acceptance Criteria

- [x] 列出已有组件与状态
- [x] 列出 P0/P1/P2 候选
- [x] 明确不进库的 Muses 业务组件
- [x] 给出 Muses 替换优先级
- [x] 记录 ion 使用扫描依据

## Notes

- 本任务为路线规划；**后续实现已迁到独立库 Trellis**：`C:\code\happier-ui\.trellis\tasks\07-22-component-roadmap`。
- Muses 侧本任务可归档；在 happier-ui 仓库内继续组件开发。

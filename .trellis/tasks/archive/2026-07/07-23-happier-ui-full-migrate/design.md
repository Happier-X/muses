# 设计：全量改用 happier-ui（Muses 侧）

## 目标与边界

在 **不破坏 Ionic 路由/手势宿主** 的前提下，把 Muses 接到已发布的 `happier-ui@0.0.1`，并完成：

1. 依赖与 token/样式接入（纯 npm）
2. 拆除对旧导出名的编译依赖
3. 用库内**已有**组件替换对应业务控件
4. 图标全面改为 `@lucide/vue` + `HIcon`
5. 库缺口只记录、不替换、不在 Muses 补造通用组件

**非目标**

- 不在本任务开发/发布 happier-ui 新组件
- 不移除 `IonicVue` / `ion-page` / `ion-router-outlet`
- 不替换 `ion-modal` / `ion-alert` / `ion-action-sheet` 引擎
- 不替换 `PlayerPage` 的 `ion-range`
- 不把 `MCover`、播放业务、WebDAV 逻辑抽进库

## 依赖与构建契约

| 项 | 决策 |
|----|------|
| 包版本 | `happier-ui@0.0.1`（npm registry，非 `file:`） |
| peer | 安装 `@lucide/vue`（与库一致）；保留 `vue` |
| 样式入口 | `main.ts`：`import 'happier-ui/style.css'`；token 继续经 `src/theme/tokens.css` → `happier-ui/tokens.css` |
| alias | 删除 `vite.config.ts` / `tsconfig.json` 对 `../happier-ui/src` 的路径映射 |
| 解析目标 | 以包 `exports` → `dist/*` 为准 |

`lucide`（非 Vue）包：若仅服务于 `ion-lucide` 适配层，图标迁移完成后可移除；若仍有非 UI 用途再评估。

## 组件能力矩阵（0.0.1）

### 可直接替换

| happier-ui | 替换对象 | 说明 |
|------------|----------|------|
| `HButton` | 带文字的 `ion-button` | variants/sizes；leading/trailing 可放 `HIcon` |
| `HSwitch` | `ion-toggle` | `v-model` boolean |
| `HInput` | `ion-input`（表单文本） | stacked label 改 `label` prop |
| `HCheckbox` | `ion-checkbox` | 目录多选等 |
| `HEmpty` | `MEmptyState` / 空态结构 | 无 `HEmptyState` 别名 |
| `HImage` | 普通图片展示（非音乐封面业务） | 封面业务仍 `MCover` |
| `HIcon` | `ion-icon` + `ion-lucide` 数据 | 全面替换图标渲染 |
| `HTabBar` | Tabs 底栏视觉 | `v-model` key；宿主做路由 |
| `HNavBar` | 简单页顶栏视觉 | 无内置路由；`showBack` 只 emit |

### 本迭代保留（宿主 / 业务）

| 保留 | 原因 |
|------|------|
| `ion-page` / `ion-header` / `ion-content` / `ion-buttons` / `ion-back-button` | 路由壳与 Ionic 生命周期 |
| `ion-modal` / `ion-alert` / `ion-action-sheet` | 叠层引擎 |
| `ion-range` | 无 `HRange` |
| `ion-fab` / `ion-fab-button` | 无等价组件；仅内部图标改 `HIcon` |
| `ion-list` / `ion-item` / `ion-label` / `ion-note` / `ion-card*` / `ion-text` / `ion-progress-bar` | 无列表行/设置行/卡片/提示原语 |
| `MCover` | 音乐封面 app-only |
| `MPage` | HOST-IONIC 页壳；内部图标/按钮按矩阵替换 |
| MiniPlayer / Player 业务手势与 AMLL | 业务，不进库 |

### 明确缺口（写入 `gaps.md`，不替换）

| 候选组件 | 当前落点摘要 |
|----------|----------------|
| `HListRow` | Songs/Queue/PlaylistDetail/`MListRow`、部分列表 `ion-item` |
| `HSettingRow` | Settings/`MSettingRow` |
| `HIconButton` | MiniPlayer/队列/列表 more、纯 `icon-only` 按钮 |
| `HListSection` | 多处 `ion-list` inset |
| `HCard` / `HSurface` | Sources 卡片 |
| `HNotice` | Sources `ion-text` 错误/成功 |
| `HRange` | Player 进度 |
| `HProgress` | Sources 扫描进度 |
| Modal/Sheet 内容壳 | 若未来要脱离 Ionic 引擎视觉 |

**规则**：缺口处继续用现有 Ionic/业务实现；允许把其中的 **图标** 换成 `HIcon`（图标决策 B），但不新建 Muses 通用 `MListRow`/`MIconButton` 等平行库。

对已损坏的兼容层：

- `src/components/ui/index.ts` 不得再 re-export 不存在的 `HEmptyState`/`HIconButton`/`HListRow`/`HSettingRow`/`MEmptyState`/`MIconButton`/`MSettingRow`
- `MListRow.vue` 依赖已消失的 `HListRow`：应 **删除该包装**，调用点临时回到 Ionic `ion-item` 行（或既有页面结构），并在 `gaps.md` 登记 `HListRow`
- `MEmptyState` → 直接使用 `HEmpty`（可 re-export 别名仅当等价；优先改调用点为 `HEmpty`）

## 接入层（`src/components/ui`）

目标形态：

```text
src/components/ui/
  index.ts      # re-export happier-ui 真实导出 + app-only
  MCover.vue    # app-only；占位图标改 HIcon
  MPage.vue     # HOST-IONIC；可选 HNavBar 或保留 ion-toolbar
```

`index.ts` 仅导出库真实符号 + `MCover`/`MPage`。业务可 `from 'happier-ui'` 或 `from '@/components/ui'`。

## 图标体系

| 项 | 决策 |
|----|------|
| 渲染 | 全面：`HIcon` + `@lucide/vue` 组件 |
| 禁止 | 业务 `ion-icon`；`@/icons/ion-lucide` 适配层（迁移完成后删除） |
| 语义映射保留 | 队列/歌单 `ListMusic`；顺序播放 `ListOrdered`；专辑 `Disc3`；艺术家 `MicVocal`；音源 `Folder`；翻译 `Captions`/`CaptionsOff`；主控 fill 用 `HIcon variant="fill"` |
| 存放 | 可用 `src/icons/` 导出 **Lucide Vue 组件别名**（非 data-uri），避免散落魔法字符串 |

`HButton` 无专用 icon-only 方形 API：纯图标触控若无法用 `HButton` 合理表达，**整控件保留 `ion-button`**，仅把内部图标换 `HIcon`，并登记 `HIconButton` 缺口。

## 页面替换策略

### 1. Bootstrap

- 改 `package.json` 依赖；`npm install`
- 清 alias；确认 `tokens.css` import 与 `style.css` 入口
- 重建 `ui/index`；修编译断点

### 2. 高置信替换

- **Settings**：`ion-toggle` → `HSwitch`；检查更新等文字按钮 → `HButton`；设置行结构保留并登记 `HSettingRow`
- **Sources**：表单 `HInput`/`HCheckbox`/`HButton`；空态 `HEmpty`；modal/alert/sheet 保留；卡片/列表/进度登记缺口
- **TabsPage**：底栏/侧栏导航图标与可选 `HTabBar`（注意现有平板侧栏布局：可用 `HTabBar` 仅窄屏，或手工复用 `HIcon` 保持侧栏结构——以不破坏 768 断点为准）
- **列表页空态**：`HEmpty`
- **带文字按钮**：`HButton`（Songs「随机播放全部」、Playlists 等）

### 3. 页头

- 简单单 toolbar 页：可评估 `HNavBar`（`fixed=false` 放在 `ion-header` 内，或替换 toolbar 视觉但保留 `ion-header` 粘性语义）
- 复杂双 toolbar（Songs 随机条）、modal 内 header：优先保留 `ion-toolbar`，仅替换按钮/图标

### 4. Player / MiniPlayer

- 进度 `ion-range` 保留
- 主控等：若为 icon-only，保留按钮壳 + `HIcon`；能改成带文案的改 `HButton`
- 不改 AMLL/手势/媒体会话逻辑

### 5. 缺口清单

实现过程中持续更新 `gaps.md`：候选名、文件/调用点、所需 API、优先级、阻塞范围。

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| `HNavBar`/`HTabBar` 默认 `fixed` 与 Ionic 壳叠层冲突 | 在 ion 壳内使用时设 `fixed=false`；真机看安全区 |
| 去 `file:` 后 dist 与本地源码不一致 | 以 npm 0.0.1 为准；本地联调另开 link 不进默认配置 |
| 图标全面替换触达 Player 样式 | 保持 `currentColor`；主控 fill/outline 对照旧语义表回归 |
| 删除 `MListRow` 导致列表回退 | 回 `ion-item` 保证可编译可听；登记 `HListRow` |
| 旧 unit/e2e 仍断言 Tab1 等 | 按现网页面修测试或跳过无关用例 |

## 验证

- `npm run lint`
- `npm run build`（`vue-tsc` + vite）
- `npm run test:unit -- --run`（若有相关用例）
- 手测：Tab 导航、列表播放、队列、设置开关、音源表单关键路径、沉浸页主控与进度

## 与现有 spec 的关系

当前 `.trellis/spec/frontend/*` 仍描述 `file:../happier-ui`、`MEmptyState`/`MIconButton`/`ion-lucide`。本任务实现完成后应通过 `trellis-update-spec` 回写；实现期以 **本 design + prd** 为准，冲突处不沿用过期 M* 契约。

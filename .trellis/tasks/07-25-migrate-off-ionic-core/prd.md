# 脱离 Ionic 框架迁移至 vue-router 与自建页面骨架

## Goal

一次性清干净应用对 `@ionic/vue` / `@ionic/vue-router` 的依赖：升级 happier-ui、把残留 `ion-*` 组件全部迁到 happier-ui 等价物、替换路由与页面骨架、移除生命周期钩子与全局 CSS，最终从 `package.json` 删除 `@ionic/vue`、`@ionic/vue-router`、`ionicons`。UI 完全建立在 happier-ui + Vue 原生能力之上。Capacitor 原生壳保持不变。

## Background / 已确认事实

### 路由现状（大部分已脱离 Ionic）
- `src/router/index.ts`：用 `createRouter`（来自 `@ionic/vue-router`）+ `createWebHistory`（vue-router 原生 history）。
- `src/App.vue`：顶层 `<ion-router-outlet>`，其下唯一路由 `/tabs`（`/` redirect 到 `/tabs/songs`）。
- `src/views/TabsPage.vue`：页面主体用 `<RouterView>` + `useRoute`/`useRouter`（均来自 `vue-router`）。**tab 间切换与详情页（`/tabs/playlists/:id`）都是 TabsPage 的 RouterView 子路由，这一层已无 Ionic 转场。**
- 推论：当前应用页面间切换实际已无 Ionic 页面转场动画；detail 页手势返回依赖 `ion-router-outlet` 的 page stack，而子路由不入栈，手势返回大概率已不生效。

### happier-ui 0.0.6 已解除浮层阻塞 + 独立暗色态（关键前置）
- npm 已发布 `0.0.6`；当前 muses 仍 pin `0.0.3`。本任务直接 pin `0.0.6`。
- `HToast` / `HBottomSheet` / `HDialog` 默认 `Teleport to="body"`（0.0.4 起），可选 `teleport` prop（`false` 就地渲染）。
- **破坏性变更**（0.0.4 起）：`HIconButton` 已移除，合并进 `HButton`（`isIconOnly` + `shape: 'square' | 'circle'`）。muses 多处引用 `HIconButton`，升级时必须一并迁移。
- **暗色态阻塞已在 0.0.5 解除、0.0.6 补齐**（issue #11）：0.0.4 及之前的 tokens.css 反向依赖 Ionic 变量（`--h-color-ink: var(--ion-text-color)` 等），且无独立暗色态——拔掉 Ionic 全局 CSS 后暗色必然黑底黑字。0.0.5 已：①彻底去除全部 `var(--ion-*)` 反向依赖，改为自持有硬编码值（明色值保留原 Ionic fallback，观感与迁移前 100% 一致）；②新增双触发独立暗色态：`@media(prefers-color-scheme: dark) :root:not(.light)`（系统跟随 + `.light` 逃生舱）与 `:root.dark, .dark`（手动强制）。因此本任务 R4.1 **无需** muses 侧任何暗色 workaround，拔根后暗色自动由 happier-ui 承接。
- 遗漏已补（0.0.6）：`--h-color-bg-muted` / `--h-color-bg-hover` 已补进 0.0.6 tokens.css（明 `#f4f4f5`/`#f0f0f0`，暗 `#2a2a2a`/`#333333`），HTag/HBadge/HTable/HSelect/HPagination 暗色背景也已正确。issue #11 全部结清。

### Ionic「根」依赖清单
| 位置 | Ionic 用法 | 迁移目标 |
|------|-----------|---------|
| `main.ts` | `IonicVue` 插件 + 12 个 `@ionic/vue/css/*` + `dark.system.css` | 移除插件；用自建/标准 reset 替代必要的 normalize/typography |
| `router/index.ts` | `createRouter`（`@ionic/vue-router`） | `createRouter`（`vue-router`） |
| `App.vue` | `IonApp`、`IonRouterOutlet` | 自建 app shell + `<RouterView>` |
| `components/ui/MPage.vue` | `IonPage`、`IonContent` | 自建 `MPage`/`MContent`（div + 滚动容器 + safe-area） |
| `PlaylistDetailPage.vue` | `useIonRouter`（`canGoBack`/`back`/`navigate(...,'back','pop')`） | `vue-router` 的 `router.back()` / `router.replace()` |
| AlbumsPage、ArtistsPage、PlaylistDetailPage、PlaylistsPage、SongsPage | `onIonViewWillEnter`（×5，全为「进入刷新数据」） | `onActivated` / `onMounted` / `watch(route)` 等 |

### 残留 ion-* 组件清单（本任务一并迁移）
| 组件 | 文件 | 替换为 |
|------|------|--------|
| `ion-action-sheet` ×3 | PlaylistsPage、SongsPage(×2) | `HBottomSheet` |
| `ion-alert` ×4 | PlaylistsPage(×2)、SongsPage、SourcesPage | `HDialog` |
| `ion-modal` ×4 | SourcesPage | `HDialog` / `HBottomSheet` |
| `ion-fab` + `ion-fab-button` | SongsPage | `HFloatingBubble` |
| `ion-button` ×12 | MiniPlayer(×2)、PlayerPage(×8)、QueuePage(×2) | `HButton`（含 icon-only） |
| `ion-item` + `ion-label` + `ion-list` | PlaylistDetailPage、PlaylistsPage、QueuePage、SettingsPage、SongsPage、SourcesPage、TabsPage | `HCellGroup` / `HCell` |
| `ion-note` ×5 | QueuePage、SourcesPage(×4) | 文本 / `HTag` |
| `ion-range` ×1 | PlayerPage | `HRange` |

### App.vue 与 Ionic 结构的耦合
- MiniPlayer、PlayerPage、QueuePage 是 `ion-router-outlet` 的兄弟节点，位于 `ion-app` 层级。
- overlay 锁滚动依赖选择器 `body.muses-overlay-open ion-router-outlet ion-content`。
- 状态栏同步、返回键监听（Capacitor `App.addListener('backButton')`）与结构耦合。

### safe-area 变量
- 多处使用 `--ion-safe-area-top` / `--ion-safe-area-bottom`（Ionic 注入），需替换为标准 `env(safe-area-inset-*)`。

### IonicVue 插件行为（代码验证）
- `IonicVue.install` 只调用 `initialize(config)` 并给 `<html>` 加 `ion-ce` 类，**不**全局注册所有组件。
- 各 `ion-*` 是按文件导入的 Vue 包装组件，导入时自行 `defineCustomElement`。
- `IonPage` 实际就是 `div.ion-page`。

## Decisions（已拍板）

1. **交付边界 = 完全清干净（选项 B）**
   - 本任务最终态：`package.json` 无 `@ionic/vue` / `@ionic/vue-router` / `ionicons`；源码无 `ion-*` 标签与 `@ionic/*` 导入；无 Ionic 全局 CSS。
   - 前置：升级 `happier-ui` 到精确版本 `0.0.6`，并迁移 `HIconButton` → `HButton` icon-only。
   - 因范围大，采用单任务一把梭执行（见决策 3），不分 child。

2. **转场动画 / 手势返回 = 接受现状（选项 A）**
   - 迁移后用 vue-router 原生切换，不重建页面转场，不重建 swipe-to-go-back。
   - 依据：现状本就无 Ionic 页面转场（切换全在 RouterView 子路由层），手势返回大概率已失效；迁移目标是「不产生可感知退化」，而非补功能。转场增强如需要，后续单独立项。

3. **执行策略 = 一把梭（选项 B），单任务不拆 child**
   - 一个任务内完成全部改动，最后统一验证，不分阶段提交中间态。
   - 风险缓解（写入 design/implement）：即便一次性完成，编辑顺序仍遵循「先迁完所有 `ion-*` 组件 → 再拔根（删 IonicVue 插件、全局 CSS、`@ionic/*` 依赖）」，避免中途残留 `ion-*` 因失去全局 CSS 而崩。全部改完后一次性 `vue-tsc` + `build` + 测试验证。

4. **浮层迁移映射（选项 A）**
   - 确认/输入类 alert（PlaylistsPage 删除确认、新建/重命名歌单；SongsPage 新建歌单；SourcesPage 删除确认）→ `HDialog`（输入用 `HInput` 承载）。
   - action-sheet（PlaylistsPage 歌单操作、SongsPage 歌曲操作/选歌单）→ `HBottomSheet`。
   - SourcesPage 4 个**全屏表单** modal（编辑音源、扫描设置、扫描进度、WebDAV 目录浏览）→ 统一 `HBottomSheet`（`max-height 88vh` + 内部滚动）。理由：移动端底部面板是多字段表单主流模式，复用 happier-ui、不新增自建组件；观感从「整页覆盖」变为「底部 88vh 面板」，符合移动端习惯。
   - SongsPage FAB → `HFloatingBubble`。

## Requirements

### R1 依赖升级与破坏性变更
- R1.1 `happier-ui` 升级到精确版本 `0.0.6`（`package.json` 不用 `^`，不用 `file:`/源码 alias）。0.0.5 起已彻底去除 token 层 `var(--ion-*)` 反向依赖，并新增独立暗色态（`@media(prefers-color-scheme: dark) :root:not(.light)` 系统跟随 + `:root.dark`/`.dark` 手动强制双触发），明色值保留原 Ionic fallback 值（观感与迁移前一致）；0.0.6 补齐 `--h-color-bg-muted`/`--h-color-bg-hover` 明暗值。这是拔除 Ionic 全局 CSS 后暗色不退化的关键前置。
- R1.2 `src/components/ui/index.ts` 移除 `HIconButton` 导出；所有 `HIconButton` 用法改为 `HButton`（`isIconOnly` + `shape='square'|'circle'` + `variant='ghost'` 按原语义）。

### R2 残留 ion-* 组件迁移
- R2.1 `ion-action-sheet`（PlaylistsPage、SongsPage×2）→ `HBottomSheet`。
- R2.2 `ion-alert`（PlaylistsPage×2、SongsPage、SourcesPage）→ `HDialog`（含输入型 alert 用 `HInput` 承载）。
- R2.3 `ion-modal`（SourcesPage×4 全屏表单）→ 统一 `HBottomSheet`。
- R2.4 `ion-fab`+`ion-fab-button`（SongsPage）→ `HFloatingBubble`。
- R2.5 `ion-button`（MiniPlayer×2、PlayerPage×8、QueuePage×2）→ `HButton`（图标按钮用 icon-only）。
- R2.6 `ion-item`/`ion-label`/`ion-list`（PlaylistDetailPage、PlaylistsPage、QueuePage、SettingsPage、SongsPage、SourcesPage、TabsPage）→ `HCellGroup`/`HCell` 或原生结构。
- R2.7 `ion-note`（QueuePage、SourcesPage×4）→ 文本 / `HTag`。
- R2.8 `ion-range`（PlayerPage）→ `HRange`。

### R3 路由与页面骨架
- R3.1 `router/index.ts` 的 `createRouter` 由 `@ionic/vue-router` 改为 `vue-router`（保持 `createWebHistory` 与既有路由表不变）。
- R3.2 `App.vue` 的 `IonApp`/`IonRouterOutlet` 改为自建 app shell + `<RouterView>`；保留 MiniPlayer/PlayerPage/QueuePage 的兄弟层级与 overlay 行为。
- R3.3 `MPage.vue` 的 `IonPage`/`IonContent` 改为自建骨架（滚动容器 + safe-area 内边距）；对外 slot/props 契约保持不变，避免各页面改动。
- R3.4 `PlaylistDetailPage.vue` 的 `useIonRouter` 改为 `vue-router`（`router.back()` / 有历史则 back、否则 `router.replace('/tabs/playlists')`）。
- R3.5 5 处 `onIonViewWillEnter`（AlbumsPage、ArtistsPage、PlaylistDetailPage、PlaylistsPage、SongsPage）改为 Vue/vue-router 等价物，保证「进入页面刷新数据」语义不变（含从 detail 返回、tab 重新激活场景）。

### R4 全局样式与 safe-area
- R4.1 移除 `main.ts` 中 `IonicVue` 插件与 12 个 `@ionic/vue/css/*` + `dark.system.css` 导入；补齐必要的 reset/normalize/typography（自建或标准方案）与暗色模式。
- R4.2 `--ion-safe-area-top/bottom` 全部替换为标准 `env(safe-area-inset-*)`（MiniPlayer、PlayerPage、QueuePage、SongsPage 等）。
- R4.3 `theme/variables.css` 中 `--ion-color-*` 桥接：残留 Ionic 组件全部移除后可删除；确认无遗留引用。
- R4.4 `App.vue` overlay 锁滚动选择器 `body.muses-overlay-open ion-router-outlet ion-content` 改为适配自建骨架的等价选择器。

### R5 依赖清理
- R5.1 `package.json` 删除 `@ionic/vue`、`@ionic/vue-router`、`ionicons`（及相关 `ionic:*` scripts 视情况保留/清理）。
- R5.2 源码全局搜索确认零 `@ionic/*` 导入、零 `ion-*` 标签、零 `ionicons` 引用。

## Acceptance Criteria

- [ ] AC1 `rg "@ionic/" src` 与 `rg "from 'ionicons'" src`、`rg "<ion-" src` 全部零命中。
- [ ] AC2 `package.json` 的 dependencies 无 `@ionic/vue`、`@ionic/vue-router`、`ionicons`；`happier-ui` 精确版本 `0.0.6`。
- [ ] AC3 `npm run build`（含 `vue-tsc`）通过，无类型错误。
- [ ] AC4 单元测试与 e2e（`npm run test:unit`、`npm run test:e2e`）通过。
- [ ] AC5 应用可启动：tab 切换、进入/返回歌单详情、各页面数据刷新正常。
- [ ] AC6 浮层功能对齐原行为：action-sheet/alert/modal/fab 对应的操作（增删歌单、确认弹窗、WebDAV 弹窗、SongsPage FAB）可正常打开、交互、关闭，且不被播放条/tab-bar 遮挡。
- [ ] AC7 safe-area 在刘海屏/手势条设备下不被裁切（MiniPlayer、PlayerPage 底部、SongsPage FAB 定位）。
- [ ] AC8 暗色模式、主色 token 显示与迁移前一致。
- [ ] AC9 Capacitor 原生行为回归：状态栏样式同步、Android 返回键（含 overlay 优先关闭）、键盘不异常。

## Open Questions（阻塞规划）

- 无（三项决策已全部拍板）。

## Notes

- Capacitor 原生能力（状态栏、返回键、键盘、启动屏）不属本次迁移，但页面骨架替换后需回归验证。
- 浮层 Teleport 阻塞已由 happier-ui 0.0.4 解除（issue #7）。

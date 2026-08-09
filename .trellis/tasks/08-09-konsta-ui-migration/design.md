# 组件库迁移 Konsta UI — 技术设计

## 1. 架构与边界

### 目标形态
```
src/theme/tailwind.css  → @import 'tailwindcss'; @import 'konsta/vue/theme.css';
src/theme/tokens.css    → 删除（--h-* / --muses-* 全部移除，方案 A）
src/components/ui/index.ts → k* 组件 re-export + 自建组件（MCover 等）收编
src/icons/index.ts      → 保留语义表（@lucide/vue 组件），HIcon 移除
页面                   → k* 组件 + @lucide/vue 图标 + 自绘（Konsta 变量/class）
```

### Konsta 集成方式：k-app 独立模式（官方最佳实践）
- 按 Konsta 官方推荐：纯 Konsta 应用用 `<k-app theme="ios">` 包裹整个应用（官方文档："if you use only Konsta UI then we need to wrap all our components with Konsta UI's App component"）
- `k-app` 内部即 `konsta-provider` + 根 div 自动挂 `k-ios` class：提供全局 KonstaContext（theme/dark 配置注入，所有 k 组件读取）+ CSS 层 class
- App.vue 现有根 div 替换为 `<k-app theme="ios" class="flex flex-col h-full overflow-hidden">`（k-app 支持 class 透传，component prop 可指定渲染元素）；现有高度链（html/body/#app 100%）保留
- 注意：k-app 的 theme 默认值是 `material`，必须显式 `theme="ios"`
- 弹层类组件（k-popup/k-sheet/k-dialog/k-toast）组件实例化，由 Konsta 自管层级与动画
- 组件按需 import（`import { kButton } from 'konsta/vue'`），不全局注册

### 数据流
- 无数据流变更。迁移仅限 UI 层（组件/样式），业务逻辑、store、音频、歌词引擎零改动

## 2. 组件映射表（H* → k*）

| happier-ui（现状） | Konsta（目标） | 备注 |
|---|---|---|
| HNavBar | `k-navbar` + `k-navbar-back-link` | 标题、返回按钮语义 |
| HCell / HCellGroup | `k-list` + `k-list-item`（inset 分组） | 分组卡片样式用 `k-list inset` |
| HSwitch | `k-toggle` | |
| HButton | `k-button` | 尺寸/颜色用 k 主题 prop |
| HPopup | `k-popup` | 位置 prop（bottom）对齐现有 bottom 弹层 |
| HBottomSheet | `k-sheet` / `k-actions` | 操作菜单用 Actions（iOS action sheet），内容面板用 Sheet |
| HDialog | `k-dialog` | |
| HInput | `k-list-input` | 输入在列表内（iOS 风格） |
| HCard | `k-card` | |
| HCheckbox | `k-checkbox` | |
| HProgress | `k-progressbar` | |
| HTabBar | `k-tabbar` + `k-tabbar-link` | TabsPage |
| HToast | `k-toast` | |
| HEmpty | 自建（k-block + 图标 + 文案） | Konsta 无空状态组件 |
| HFloatingBubble | 自建（k-fab 或自绘悬浮球） | Konsta 无悬浮球；若迁移成本高可先移除，功能降级为入口按钮（待定，见风险 R3） |
| HIcon | `@lucide/vue` 组件直接渲染 | 项目已有依赖；icons/index.ts 语义表保留 |
| MPage / MContent | `k-page`（或保留自建壳） | 评估 k-page 的高度链兼容性 |
| MCover | 保留自建（音乐封面，含占位/模糊） | |
| MiniPlayer | 保留自绘，样式改用 Konsta 变量 | |
| PlayerPage 自绘区 | 控制按钮 → `k-button`（圆形 fill 变体）、进度条 → `k-range`/自绘、action sheet → `k-actions` | 保留歌词引擎与 PIXI 背景不动 |

### 自建组件收编规则
- Konsta 缺失且视觉强相关 → 用 k 组件 + k 变量组装（空状态、悬浮球）
- 业务强相关 → 保留自建（MCover、MiniPlayer、PlayerPage 歌词区）
- 收编统一放 `src/components/ui/`，k* 组件 re-export 保持页面 import 路径不变（减少改动面）

## 3. 主题与暗色模式

### Konsta iOS 主题机制（已核实源码）
- 品牌色 `--color-brand-primary: #007aff`（系统蓝），可被 `k-color-brand-*` class 覆盖
- 表面色：`--color-ios-light-surface-*`（浅）/ `--color-ios-dark-surface-*`（深），组件内通过 `dark:` variant 选择
- 字体：iOS 主题系统字体栈 `--font-ios`（-apple-system, SF Pro Text, ...），与项目现状一致，无需引字体
- 暗色模式：**`.dark` class 驱动**（`@custom-variant dark (&:where(.dark, .dark *))`），非 prefers-color-scheme 媒体查询

### 项目适配方案
1. 新增 `src/composables/useSystemDark.ts`：`matchMedia('(prefers-color-scheme: dark)')` 监听 + 在 `document.documentElement` 切换 `.dark` class（初始化立即同步），保持现状"跟随系统"行为
2. `tailwind.css` 移除 `happier-ui/styles` import，改为 `@import 'konsta/vue/theme.css'`
3. `tokens.css` 删除（方案 A：不再保留任何 --h-* / --muses-* 语义层）
4. 页面自绘中的旧变量逐处替换：
   - `var(--h-color-ink)` → `text-ios-light-surface-1 dark:text-ios-dark-surface-1` 对应色或 `text-[color:var(--color-ios-*)]` 类
   - 主题色映射表（旧 → 新）：
     - ink（文字）→ 浅 #000 / 深 #fff（iOS 系统文字色）
     - ink-muted → iOS secondary label 灰
     - primary → `--color-brand-primary` #007aff
     - surface / surface-secondary → `--color-ios-light-surface*` / `--color-ios-dark-surface*`
     - border → hairline `--k-hairline-color` 或 `--color-ios-*` 系统分隔线
     - danger → iOS 红 #ff3b30
     - 触控目标 48px 等间距类 → 保留原数值（Konsta 默认符合 HIG 触控规范）

## 4. 兼容性与迁移注意事项

### 高度链（重点风险）
- 现有 `html, body, #app { height: 100% }` + `.m-page` 内部滚动体系与 k-page 可能冲突
- k-page 自带滚动容器；需在迁移首批（TabsPage/壳）验证 k-page 高度链，必要时保留 MPage 壳（内部布局换 k 组件）
- PlayerPage 的 `player-overlay`、MiniPlayer 常驻布局必须保持现有高度链，否则歌词区/常驻条回归

### 弹层与全局遮罩
- 项目现有 `hasGlobalOverlay`（App.vue 控制 content pointer-events）与 H* 弹层联动
- Konsta 弹层（popup/sheet/dialog/toast）自管遮罩与层级，需核对 hasGlobalOverlay 联动逻辑是否仍生效，不生效则改造为 Konsta 事件/状态

### 虚拟列表与性能
- SongsPage 等使用 @tanstack/vue-virtual 的虚拟列表，迁移仅替换行内组件（k-list-item），不动虚拟逻辑

### 图标
- `src/icons/index.ts` 语义表保留（值仍是 @lucide/vue 组件），页面 `h-icon` 标签替换为直接渲染组件（`<component :is="...">` 或改模板标签）

## 5. 关键取舍

| 决策 | 选择 | 理由 |
|---|---|---|
| 主题策略 | 方案 A：Konsta 默认 iOS 主题 | 用户拍板；最原生、最省维护 |
| 集成模式 | k-app 独立模式（官方最佳实践） | 用户拍板；纯 Konsta 应用，官方推荐；k-app = provider + 根 class，无额外侵入 |
| 图标 | @lucide/vue 直用 | 已有依赖，HIcon 包装无必要 |
| 暗色 | .dark class + JS 同步系统 | 对齐 Konsta 机制，行为不变 |
| 迁移节奏 | 基础设施 → 壳 → 逐页（由简到难）→ 清理 | 每阶段可验证，风险可控 |
| 悬浮球 | 优先 k-fab 替代方案，成本高则降级为列表页入口按钮 | 避免阻塞主线迁移（待实施时定夺） |

## 6. 回滚与发布

- 迁移按 commit 分批推进，每批独立可回退（git revert 单批）
- 关键回滚点：阶段 0 完成后（基础/主题切换）先整体构建验证；壳与首个页面迁移后真机冒烟
- 全部完成后统一卸载 happier-ui（package.json 变更作为最后一步，回滚只需恢复依赖 + 重装）
- 真机验证清单：Android 模拟器（亮/暗色、滚动、弹层、MiniPlayer、播放队列、悬浮球）

## 7. 风险登记

| # | 风险 | 影响 | 缓解 |
|---|---|---|---|
| R1 | k-page 高度链与现有布局冲突 | 页面滚动/常驻条错位 | 首批验证，保留 MPage 壳兜底 |
| R2 | 弹层遮罩与 hasGlobalOverlay 联动失效 | 下层页面可误触 | 迁移弹层时逐处核对 |
| R3 | 悬浮球（HFloatingBubble）Konsta 缺失 | 功能降级 | k-fab 替代 / 降级入口按钮 |
| R4 | PlayerPage 自绘改造成本高 | 工期 | 仅替换可复用 k 组件部分，歌词/背景不动 |
| R5 | 虚拟列表内 k-list-item 性能 | 滚动卡顿 | 保持 virtual 结构，最小化行内组件 |
| R6 | --h-*/--muses-* 残留遗漏 | 样式丢失 | 全仓 grep 旧变量 + 构建检查 + 视觉走查 |

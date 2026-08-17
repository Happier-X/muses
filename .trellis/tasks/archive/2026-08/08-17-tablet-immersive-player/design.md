# 复刻椒盐沉浸式播放页平板模式 — 技术设计

## 架构与边界

改动集中在 `src/views/PlayerPage.vue`（模板 + scoped 样式）与 `src/theme/index.scss`（全局 `.player-overlay` 平板规则）。不改动播放内核（controller/native/queue）、歌词（AMLL）、背景（BackgroundRender）。

### 断点重构：media query → class 驱动（核心决策）

现状：`isTabletLayout = viewportWidth >= 768`，平板 CSS 用 `@media (min-width: 768px)`。

目标：仅横屏平板（≥768px 宽且宽>高）双栏；竖屏平板（如 800x1280）走手机式。

**问题**：若仅给现有 media query 加 `and (orientation: landscape)`，竖屏平板（800px 宽）既命中不了 `min-width: 768px` 平板断点（加了 orientation），也命中不了 `max-width: 767.98px` 手机断点 → 落到基础样式，panels 保持 `height: 100%`（手机断点里是 `flex:1 1 auto`），布局缝隙。

**方案**：断点逻辑统一收敛到 JS computed，容器挂 class：

- `viewportHeight` 新增 ref + resize 监听（现有只监听 innerWidth）。
- `isTabletLayout = computed(() => viewportWidth >= 768 && viewportHeight < viewportWidth)`。
- drag-layer（或 overlay 根）挂 `:class="{ 'player-page--tablet': isTabletLayout }"`。
- 所有平板样式从 `@media (min-width: 768px)` 改为 `.player-page--tablet & { … }`（scoped 自动限定后代）；全局 index.scss 的 `.player-overlay` 平板规则改为 `.player-overlay--tablet` 后代选择器（overlay 根也挂 class）。
- 现有 `@media (max-width: 767.98px)` 手机断点**保留**（仅微调，竖屏平板也命中基础手机式样式 + 该断点，行为一致）。
- `@media (max-height: 720px)` / `@media (max-height: 520px)` 矮屏断点保留（与 tablet 判定正交）。

## 布局结构（横屏平板 ≥768px 且宽>高）

```
drag-layer (flex column, .player-page--tablet)
├── bg（AMLL 全屏，不变）
├── song-head--fixed（手机头部）→ 平板隐藏（现有 CSS 改 class 驱动）
├── panels（flex:1 1 auto; min-height:0; 左右分栏 flex row）
│   ├── info-panel（50%）
│   │   └── info-inner（justify-content: center）
│   │       ├── song-head--in-panel（平板显示）
│   │       ├── cover-hero（大封面，弹性区）
│   │       └── .player-page__info-controls（手机控件区：progress+controls+mode-bar）
│   │           → 平板 display:none
│   └── lyric-panel（50%）
│       ├── lyric-header（平板显示）
│       └── LyricPlayer（flex:1）
└── bottom-bar（新增，仅平板，flex:none）★
    ├── progress-area（全宽进度条 + 时间行）
    └── bottom-row（三段式 space-between）
        ├── 左组：repeat / shuffle（复用 mode-bar 模板与 class）
        ├── 中组：prev / play / next（复用 controls 模板与 class）
        └── 右组：queue / more（复用 mode-bar 模板与 class）
```

手机（<768px 或竖屏平板）：bottom-bar 不渲染（`v-if="isTabletLayout"`），info-controls 显示，行为与现在完全一致。

## 关键实现点

### 1. isTabletLayout 与 viewportHeight

- `viewportHeight = ref(window.innerHeight)`；`updateViewportWidth` 改名为 `updateViewportSize`（同时更新两者），resize 监听同步改。
- 依赖 isTabletLayout 的现有逻辑自动生效：`lyric-play-fab v-if="!isTabletLayout"`、`showLyricFloatingActions`（平板只挂翻译 FAB）、`onLyricPanelPointerUp` 等。

### 2. 底部控制条模板（v-if="isTabletLayout"）

- **复制**现有 progress-area / controls / mode-bar 模板到 bottom-bar 内（事件 handler 复用 script 现有函数，零新逻辑）。
- progress-range 的 scoped 样式（隐藏 Konsta thumb）与时间行 class 复用；底部条内加 padding 对齐 panels（左右 24px）。
- 底部条背景：`linear-gradient(transparent → rgba(0,0,0,.45))` + `backdrop-filter: blur(8px)` 保沉浸与可读；z-index 高于 bg。
- 三段式按钮行：左组 repeat/shuffle、中组 prev/play/next（gap 与现有 controls 一致）、右组 queue/more；`justify-content: space-between`。

### 3. info-panel 平板样式

- `.player-page--tablet .player-page__info-controls { display: none }`（包裹 progress+controls+mode-bar 的新容器，手机不动）。
- info-inner 平板 `justify-content: center`（现状 flex-end，控制移走后封面居中更佳）；cover-hero 保持 `flex:1 1 auto; max-height: min(50vh, 420px)`。
- song-meta（五行歌词窗口）平板 `display:none`（现有 08-15 契约保留，改 class 驱动）。

### 4. panels / lyric-panel 平板样式（class 驱动）

- panels：`flex-direction: row; width: 100%; flex: 1 1 auto; min-height: 0; transform: none !important`（原全局 md 规则迁移 + flex 占剩余高度）。
- panel：`flex: 1; min-width: 0`。
- lyric-panel：`flex: 1; min-height: 0`；LyricPlayer `flex:1` 与 `--amll-lp-font-size: clamp(20px, 2.4vw, 30px)` 保留。
- lyric-header 平板 `display: block`（保留裸类 `.lyric-header` 契约）。

### 5. 全局 index.scss 迁移

`@media (min-width: 768px)` 块整体改为 `.player-overlay--tablet` 后代选择器（overlay 根 class 同步挂）。死代码类（.cover-slot/.cover/.song-info/.lyric-play-toggle）随迁移删除或保留注释。

### 6. 背景

AMLL BackgroundRender 不变（全屏 absolute inset 0）。底部条半透明渐变叠加其上，无独立背景层。

## 兼容性与风险

- **竖屏平板 800x1280**：isTabletLayout=false → 手机式全屏。验证：fixed head 显示、panels 200% 滑动、五行歌词、内嵌控制、歌词 FAB 播放键出现。
- **横屏手机 <768px**：不进双栏（宽度不足）；`@media (max-height: 520px)` 单行歌词等矮屏规则不受影响。
- **motion 内联 transform**：panels 平板 `transform: none !important` 由 class 选择器 + !important 压过 motion 内联（与现状一致）。
- **iOS WKWebView**：无 orientation media query 依赖（class 驱动），无兼容性隐患。
- **回滚**：git revert；改动集中在两文件，回滚面小。

## 验收路径

AC1/AC2：Edge headless 或模拟器 1280x800 横屏实测双栏 + 底部条；AC3：800x1280 竖屏手机式；AC4：640x360 横屏窄高；AC5：滑动/拖拽/歌词 chrome 回归；AC7：lint/vue-tsc/build + 模拟器截图。

## 决策记录

| 项 | 决策 |
|---|---|
| 断点机制 | class 驱动（JS isTabletLayout + `.player-page--tablet`），替代 media query |
| 控件模板 | 底部条复制模板（v-if），info-panel 内 CSS 隐藏；事件复用现有 handler |
| 底部条布局 | 进度条+时间一行；按钮三段式（左 repeat/shuffle 中 prev/play/next 右 queue/more） |
| 底部条背景 | 透明→深色渐变 + blur，叠加 AMLL 背景 |

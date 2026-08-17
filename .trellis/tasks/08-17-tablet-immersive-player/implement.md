# 复刻椒盐沉浸式播放页平板模式 — 执行计划

## 前置检查

- [ ] 读 `src/views/PlayerPage.vue`（3211 行）模板/script/样式全貌；读 `src/theme/index.scss` 平板断点块（470-600 行）
- [ ] 读 spec：`.trellis/spec/frontend/features-player.md`（沉浸播放页）、`component-guidelines.md`（平板布局契约）
- [ ] 确认改前基线：手机布局 + 横屏平板双栏现状截图（模拟器）

## 实施清单（顺序执行）

### Step 1: 断点 class 化（script + 模板根 class）
- [ ] `viewportHeight` ref + `updateViewportSize`（替换 `updateViewportWidth`），resize 监听更新
- [ ] `isTabletLayout = viewportWidth >= 768 && viewportHeight < viewportWidth`
- [ ] overlay/drag-layer 根挂 `:class="{ 'player-page--tablet': isTabletLayout }"`；全局侧 overlay 根加 `--tablet` class
- [ ] 验证：竖屏平板 800x1280 → isTabletLayout false

### Step 2: 模板结构调整
- [ ] info-panel 内 progress-area/controls/mode-bar 包进 `.player-page__info-controls`
- [ ] drag-layer 内 panels 后新增 `bottom-bar`（`v-if="isTabletLayout"`）：
  - progress-area（复制现有模板）
  - bottom-row：左组 mode-bar（repeat/shuffle）+ 中组 controls（prev/play/next）+ 右组 mode-bar（queue/more）
- [ ] 事件全部复用现有 handler（onPrevious/onTogglePlayback/onNext/onToggleRepeat/onToggleShuffle/goToQueue/openPlayerActions/onRangeInput/onRangeChange）

### Step 3: scoped 样式（PlayerPage.vue）
- [ ] `@media (min-width: 768px)` 平板规则 → `.player-page--tablet &`：
  - panels width 100% / flex 1 1 auto / min-height 0
  - song-head--fixed display none、song-head--in-panel display block
  - song-meta display none
  - lyric-header display block
- [ ] info-controls 平板 `display:none`
- [ ] bottom-bar 样式：flex none、渐变背景、padding、progress-range（隐藏 thumb）、时间行、三段式按钮行
- [ ] info-inner 平板 `justify-content: center`

### Step 4: 全局 index.scss
- [ ] `@media (min-width: 768px)` 块 → `.player-overlay--tablet` 后代选择器（.panels/.panel/.info-panel/.info-panel-inner/.lyric-header/.lyric-player）
- [ ] 死代码类（.cover-slot/.cover/.song-info/.lyric-play-toggle）迁移时清理

### Step 5: 验证
- [ ] `npm run lint`、`npx vue-tsc --noEmit`、`npm run build`
- [ ] Edge headless CDP：1280x800（横屏平板双栏+底部条）、800x1280（竖屏手机式）、640x360（横屏窄高）、411x731（手机）四视口截图
- [ ] 模拟器装 debug APK 实测横屏平板 + 竖屏平板 + 真机交互（进度拖动/切歌/下滑关闭/歌词浮动 chrome）

### Step 6: spec 更新 + 收尾
- [ ] 更新 `features-player.md` / `component-guidelines.md` 平板契约（断点机制、底部控制条、class 驱动）
- [ ] changelog 记录

## 风险文件与回滚点

- `src/views/PlayerPage.vue`（模板+script+scoped）——主要改动，Step 2/3
- `src/theme/index.scss`（全局平板规则）——Step 4
- 回滚点：Step 1 后（断点 class 化）、Step 3 后（scoped 样式）、Step 4 后（全局）。任一步异常即 git checkout 该文件恢复。

## 检查清单（check.jsonl 对应）

- 平板契约 spec 2 份：features-player.md、component-guidelines.md
- 实施前读取：PlayerPage.vue、index.scss（代码路径不入 jsonl，仅 spec/research）

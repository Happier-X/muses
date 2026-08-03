# 设计：PlayerPage 迁移 HPopup fullscreen

## 架构决策：用 HPopup 完整外壳（选项 A）

PlayerPage 组件根替换为 `<HPopup position="fullscreen">`，App.vue 常驻挂载。PlayerPage 内部 200+ 行自建手势原样保留，仅外壳由库接管。

### 组件结构变化

**PlayerPage.vue 模板顶层**（当前）：
```
<div class="player-overlay fixed inset-0 z-[var(--muses-z-player)] overflow-hidden overscroll-behavior-none touch-action-none ..." :aria-hidden="!playerOverlayVisible" @touchstart @touchmove @touchend @touchcancel>
  <div class="relative h-dvh max-h-dvh overflow-hidden [background:...] transition-transform ..." :style="{transform:translateY(dragOffsetY)}" :class="{is-dragging...}">
    ...全部内容（背景/面板/歌词）...
  </div>
</div>
```

**迁移后**：
```
<h-popup
  v-model="playerOverlayVisible"
  position="fullscreen"
  :keep-alive="true"
  :swipe-close="false"
  :close-on-overlay="false"
  :close-on-esc="false"
  style="{ '--h-popup-z': 'var(--muses-z-player)' }"
>
  <div class="player-overlay h-full overflow-hidden overscroll-behavior-none touch-action-none ..." @touchstart=... @touchmove=... @touchend=... @touchcancel=...>
    <div class="relative h-full overflow-hidden [background:...]">  <!-- 原 h-dvh 改 h-full，因 HPopup panel 已 inset:0 占满 -->
      ...原内容...
    </div>
  </div>
</h-popup>
```

### 关键替换点

1. **remove fixed/z-index/aria-hidden**：原 `.player-overlay` 根从 `fixed inset-0 z-[var(--muses-z-player)]` 改为**普通内容根**（HPopup fullscreen panel 已 `position:fixed; inset:0`）。保留 `overflow-hidden overscroll-behavior-none touch-action-none`（PlayerPage 全屏禁滚 + 自建手势需要 touch-action-none）。
2. **`:aria-hidden="!playerOverlayVisible"` 移除**：HPopup 关闭时 rootEl 自动 `visibility:hidden; pointer-events:none`，无需手动 aria-hidden。但 `aria-hidden` 可保留（双保险，不影响）。
3. **`dragOffsetY` transform / is-dragging**：PlayerPage 自建纵向关闭用 **translateY(dragOffsetY) 跟手**。此处 HPopup 外壳 `swipeClose=false` 不接管，但 HPopup fullscreen panel 自带 swipe 拖动时 transform 只作用于自身——**PlayerPage 内部拖动逻辑保留且互不干扰**（外层 HPopup 不 preventDefault，内部 PlayerPage 自建 touchmove preventDefault）。两者 transform 作用于不同元素（HPopup panel vs PlayerPage 内部 inner div），无冲突。↴
4. **保活**：`keep-alive` 使 HPopup slot `v-show="visible"`；关闭时 PlayerPage 内容保留 DOM（AMLL BackgroundRender 不卸载，满足 #22）。App.vue 移除 `keepPlayerPageMounted` v-if 条件挂载——改为常驻 `<PlayerPage />`（无需额外判断，HPopup 关闭态不渲染内容显示问题）。
   - 无当前曲时：`playerState.currentSong` 为空，PlayerPage 显示 empty-state；此时 HPopup 若关闭则 `visibility:hidden`，不弹浮层。AC7 满足。
5. **关闭手势**：PlayerPage 自建 `onTouchEnd` 判断 `shouldDismiss` → `goBack()` → `closePlayerOverlay()` → `playerOverlayVisible=false` → HPopup v-model false → 收起。等价替代。

### App.vue 变更

```
- <PlayerPage v-if="keepPlayerPageMounted" class="..." :class="[translate-y..., contain...]" />
+ <PlayerPage />
```
- 移除 `keepPlayerPageMounted` computed；移除 `<Transition>`（若存在，Player 无 Transition）。
- **保留**：`hasGlobalOverlay`、`syncBodyOverlayLock`、`syncPlayerStatusBar`、`backButton` 顺序（queue→player→minimize）。
- `hasGlobalOverlay = playerOverlayVisible || queueOverlayVisible` 供 MiniPlayer pointer-events-none + `muses-overlay-open` class 锁。不变。
- `playerOverlayVisible` 仍在 `overlay.ts`，由 `openPlayerOverlay()/closePlayerOverlay()` 控制。

### z-index（R6）

- HPopup fullscreen 默认 `--h-popup-z: 1200`（`var(--h-popup-z, 1200)`）。
- PlayerPage 需 1100（`--muses-z-player`），QueuePage 1200。→ 通过 `wrap` 传 `'--h-popup-z': 'var(--mus-z_player)'` 或 HPopup 上的 `class` 覆盖 `style="--h-popup-z: var(--muses-z-player)"`。
- 确认 HPopup 是否支持 style 覆盖 CSS 变量——是，`--h-popup-z` 从 host 层继承，直接在 h-popup 自定义样式覆盖即可。

### 滚动锁（R7）

两套并存，均幂等：
- **HPopup useScrollLock**（lockScroll 默认 true）：锁 `document.documentElement` inline `overflow:hidden` + padding-right 补偿，引用计数。
- **宿主 class 锁**（App.vue `syncBodyOverlayLock`）：`html/body.muses-overlay-open { overflow:hidden !important; overscroll:none }` + `body.muses-overlay-open .m-content { pointer-events:none }`。
- 打开时：两者都生效（双锁）。关闭时：HPopup unlock（计数归零还原 inline）+ 宿主 class 移除（`hasGlobalOverlay` false）。`syncBodyOverlayLock(false)` 由 `watch(hasGlobalOverlay)` 处理。无双重解锁——两套分别归零。✓

### 升级 happier-ui 0.0.8（R5）

```
npm install happier-ui@0.0.8 --save-exact
```
- 精确版本，无 `^`，无 file:
- 验证 `node -e "console.log(require('happier-ui/package.json').version)"` → 0.0.8
- 组件库 dist 是否含 keepAlive/swipeClose（`grep -l "keepAlive\\|swipeClose" dist/*.js`）

### PlayerPage 内部手势不回归（R4）

- 自建 touchstart/move/end 绑在 `.player-overlay` 根 div（迁移后仍在内容根 div）。行为不变。
- 进度条 `seekGestureLocked` + `@touchstart.stop` 不变。
- `canStartVerticalDismiss`、`isLyricPanelTarget`、横向切面板 `activePanel` 不变。
- 仅确认：迁移后 PlayerPage 根 div 仍自定义 touch-action-none，HPopup fullscreen `swipeClose=false` → `.h-popup--swipe-disabled .h-popup__panel { touch-action:auto }`，但 PlayerPage 内容根兜底 `touch-action-none`，重叠区域由 PlayerPage 控制。✓

## 兼容性 / 迁移注意

- HPopup fullscreen `overflow:auto`：slot 内容 `h-full`（body height 100% 补丁已有）→ panel 不滚动，无双滚动条。
- HPopup 入场动画 `h-popup-fullscreen-in` 330ms 覆盖 overlay + panel；leave `h-popup-fullscreen-out` 330ms。原 220ms translate-y 移除 → 观感变化（已获批准）。
- `h-popup__body` 高度 100% 补丁（Muses tailwind.css line ~80）通用生效。

## 回滚

`git checkout src/App.vue src/views/PlayerPage.vue` + `npm install happier-ui@0.0.7 --save-exact` 即可回滚到当前基线。
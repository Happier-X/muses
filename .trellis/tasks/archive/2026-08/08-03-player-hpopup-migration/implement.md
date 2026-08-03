# 执行计划：PlayerPage 迁移 HPopup fullscreen

## 前置

1. **升级依赖**（先做，保证类型定义可用）：
   ```bash
   npm install happier-ui@0.0.8 --save-exact
   node -e "console.log(require('happier-ui/package.json').version)"   # → 0.0.8
   grep -l "keepAlive\|swipeClose" node_modules/happier-ui/dist/*.js   # 库含新契约
   ```

## 实现顺序（按依赖）

2. **PlayerPage.vue 模板**：
   - 顶层 `<div class="player-overlay fixed inset-0 z-[var(--muses-z-player)]...">` 替换为 `<h-popup v-model="playerOverlayVisible" position="fullscreen" :keep-alive="true" :swipe-close="false" :close-on-overlay="false" :close-on-esc="false" style="--h-popup-z: var(--muses-z-player)">`，包裹原内容根。
   - 原内容根 div 从 `fixed inset-0 z-...` 改为 `class="player-overlay h-full overflow-hidden overscroll-behavior-none touch-action-none ..."`，`:aria-hidden` 可移除。
   - 内部 `relative h-dvh max-h-dvh` → `relative h-full`（HPopup panel 已 inset:0 占满）。
   - 确保 HPopup 闭合标签。
   - script：`import { HPopup } from '@/components/ui'`（若未导出需在 ui/index.ts 加）。

2. **App.vue**：
   - `<PlayerPage v-if="keepPlayerPageMounted" class="..." :class="[...translate-y/contain...]" />` → `<PlayerPage />`。
   - 移除 `keepPlayerPageMounted` computed。
   - 保留 `hasGlobalOverlay`、`syncBodyOverlayLock`、`syncPlayerStatusBar`、`backButton`。

3. **ui/index.ts**：确保 `HPopup` 已导出（adopt 任务已加，验证）。

## 验证命令

```bash
npm run lint
npm run build
# 语义验证（静态）：
rg -n "keepPlayerPageMounted" src/App.vue        # 应为空
rg -n "fixed inset-0 z-\[var\(--muses-z-player\)\]" src/views/PlayerPage.vue   # 应为空
rg -n "translate-y" src/App.vue                  # Player 相关应为空
```

## 回归抽查（build 后手动/浏览器）

1. Player 打开（从 MiniPlayer/任一歌曲）→ 全屏显示、背景动画、进度条、封面。
2. 纵向关闭（下拉）→ translateY 跟手、超阈值关闭、`closePlayerOverlay()` 生效。
3. 横向切面板（info↔lyric）→ activePanel 切换正常。
4. 进度条 seek → 拖拽/点击可 seek，`seekGestureLocked` 防误触。
5. 关闭再打开 → 背景不闪默认底（keep-alive 生效）。
6. 打开 Player、关闭 → 检查 `html/body` 无 `muses-overlay-open` class、`documentElement.style.overflow` 为空（双锁归零）。
7. Player→Queue 层叠：Queue（1200）叠在 Player（1100）上。
8. 系统返回：queue→player→minimize 顺序正常。
9. 空态：无 currentSong 打开 → 空态显示；关闭不弹浮层。
10. 无当前曲常驻 → HPopup 隐藏（AC7）。

## 风险点 / 回滚

- **风险**：HPopup fullscreen `overflow:auto` 若 slot 内容未 h-full 可能双滚动条 → 依赖 body height:100% 补丁；已含。if render → 加 `.h-popup--position-fullscreen .h-popup__body` 已有。
- **回滚**：`git checkout src/App.vue src/views/PlayerPage.vue` + `npm install happier-ui@0.0.7 --save-exact`。
- **转场观感**：330ms vs 220ms（已批准）。如不满意可在 PlayerPage 的 HPopup 上覆盖转场 class（本期不做）。

## 完成后

- 更新 `component-guidelines.md`：PlayerPage 迁移记录、双锁说明、keepAlive/swipeClose 用法。
- trellis-check 验证 AC1–AC7。
- 提交 + 归档 + journal。
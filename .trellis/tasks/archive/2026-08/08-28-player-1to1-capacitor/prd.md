# 沉浸式播放页一比一复刻 Capacitor

## Goal

一比一复刻 Capacitor 版本 `PlayerPage.vue` 的沉浸式布局，修正当前原生版与旧版的细节差异，确保视觉、交互、响应式与旧版一致，同时保留已接入的 MeloX 流体背景与逐词歌词能力。

## 背景

- 旧版 Capacitor `PlayerPage.vue`（`de7e388f^`）为沉浸式页唯一真源，已于 `de7e388f` 删除；现原生 `PlayerScreen.kt` 已完成主体重构但 PRD 仍为 TBD，需要以旧版为基准做 1:1 验收
- 刚完成 `melox-copy` 已将背景与歌词替换为 MeloX 原生实现，布局层已对齐 Capacitor BEM 结构，剩余工作为查漏补缺与细节对齐验收，而非推倒重做

## Requirements

### R1: 结构与布局 1:1
- `player-page__drag-layer`：`translateY(dragOffsetY)` + `is-dragging` 无过渡跟手，松手 0.22s `easeOut` 回弹；下滑阈值 `clamp(0.18*h, 96, 160)` 触发关闭
- `player-page__bg / BackgroundRender`：`album / flowSpeed=2 / hasLyric` + `fallback-background`（无封面纵向渐变，深色占位），已由 `MeloXFlowingLightBackdrop` 承担
- `player-page__song-head--fixed` 常驻（手机面板外），`--in-panel` 仅平板显示，避免重复
- `panels`：手机 `width 200% → translateX(-activePanel*50%) 0.22s easeOut`，`info-panel / lyric-panel` 各 50%；平板收缩为 `100% + 左右双栏` + 底部全宽控制条
- `player-page__cover-hero`：`aspect-ratio 1` 正方形，`max-height min(50vh,420px)`，圆角 12，窄屏 `≤520` 时 `150dp` 限制
- `player-page__meta-window`：79px 视口 + `translateY -29.5` 居中，五行 `displayedWindow`（-2..+2），当前行 `scale 1.05 / opacity 1 / blur 0`，非当前 `0.92 / 0.55 / blur 0.6`，窄屏降为单行 19.5px
- `player-page__info-controls`：手机显示、平板 `display:none` 由 `player-page__bottom-bar` 承担

### R2: 控件与交互 1:1
- `progress-range + time-row`：`m-range` 隐藏 thumbWrap（`inset-inline-start: hidden`）、`step 0.1`、拖动本地 preview 抬起才 `seekTo`，时间行 `position / 缓冲中 / duration`（`--:--` 占位）
- `controls`：三键 `lg`（48/28）、`gap clamp(24,10vw,44)`；`mode-bar` 四键 `md`（40/20）、`max-width 320`、`space-between`，无 `is-active` 状态，仅图标与 `aria-label` 区分
- `LyricPlayer`：`lyric-lines / current-time / align center 0.5 / enableBlur / enableScale / wordFadeWidth 0.5`，`line-click → seekTo`、`wheel/touchmove → revealChrome`，空态标题+描述
- `lyric-fabs`：`motion 180ms fade`、`3s idle` 隐藏、`hasTranslation ? split : end` 布局，平板无播放键，翻译键 `is-active` 态
- `player-page__bottom-bar`：仅平板（`≥768 && 宽>高`）显示，`flex none / z 10 / 6px 24px` + 渐变背景，进度全宽 + 三段式按钮 `spaceBetween`

### R3: 响应式与空态
- 平板断点 `768dp` 且横屏才启用双栏与底部条，竖屏保持手机式；窄屏 `≤520` 单行歌词
- 空态：`!currentSong` 时显示占位（图标 + 标题“暂无播放歌曲” + 描述），背景渐变保留，避免黑屏误判
- 粘性封面：`stickyCover` 三段语义（有封面更新 / 无封面沿用 / 无当前曲清空），切歌不闪黑

### R4: 非目标（Out of Scope）
- 编辑歌曲信息表单（`editMetaTab basic/lyrics`、`cloud-card` 等）与歌曲操作 `m-actions` 弹窗不纳入本次 1:1，仅保留入口 `onOpenEditMeta` 回调
- 播放列表页、队列页独立路由不重做，仅保证 `onOpenQueue` 可跳转

## Acceptance Criteria

- [x] 手机竖屏：固定头部常驻，封面正方形居中，五行小窗当前行居中高亮，进度/三键控制/四键 mode-bar 完整可交互，左右滑切面板 0.22s easeOut，下滑关闭阈值与回弹符合旧版 — MuMu 1080x1920 截图 mumu_melox.png / final_check.png 验证
- [x] 歌词面板：MeloX 逐词高亮/翻译切换/和声标记/点击跳转/自动居中滚动均生效，Fab 组 3s 隐藏逻辑与旧版一致，平板不显示播放 Fab — mumu_melox2.png 逐词高亮验证
- [x] 平板横屏（≥768 且宽>高）：左 `info-panel` 居中封面 + 面板内头部，右 `lyric-panel` 填充，无五行小窗与手机控件区，底部全宽控制条三段式布局与旧版一致；窄屏与竖屏平板回退为手机布局 — 代码 TabletImmersiveLayout 断点 768 + 宽>高已对齐
- [x] 空态与粘性：无播放歌曲时占位正确，背景不卸载；有词/无词切换不闪默认底，切歌无封面时沿用旧封面 — final_check.png / mumu_1to1_check.png 空态验证，stickyCover 三段语义已实现
- [x] MuMu 真机验证通过（1080x1920）：手机与平板布局截图与旧版对比无显著差异 — 补漏后 27d84d0d 已装机验证，下滑守卫/canSeek/安全区均通过

## Notes

- 真源：`git show de7e388f^:src/views/PlayerPage.vue`（3430 行，含 template + script + scoped style，关键类见 R1/R2）
- 现状：`feature/player/PlayerScreen.kt` 已按上述映射实现，`backdrop/MeloXFlowingLightBackdrop.kt` 与 `lyric/MeloXIOSLyricsPanel.kt` 已替换旧 `BackgroundRender/LyricPlayer`，本次为验收型任务，重点在查漏补缺而非重写
- 依赖：`PlayerViewModel.parsedLines / lyricPosition / stickyCover / translationEnabled / hasTranslation` 数据链路保持不变

## Open Questions

- 无阻塞问题；编辑表单与 actions 弹窗已明确移出范围

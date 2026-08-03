# 沉浸式播放页下拉露白底

## Goal

修复沉浸式播放页（`PlayerPage`）下滑关闭手势过程中，内容层 `translateY` 后顶部露出纯白底的问题，使下拉时能透过遮罩看到底层页面内容。

## 背景 / 根因（已确认）

- 下滑关闭只移动 `.player-overlay` **内层**（带 `--muses-immersive-void` 与 AMLL 背景），外层与 `HPopup` 面板不动。
- `happier-ui` 的 `.h-popup__panel` 默认 `background: var(--h-color-surface, #ffffff)`；fullscreen 变体未覆盖该底色。
- Player 使用 `position="fullscreen"` + `:swipe-close="false"`，宿主侧已有 `.h-popup--swipe-disabled` 区分（用于 z-index），可安全只改播放弹层。
- 队列页 `QueuePage` 同样 fullscreen，但**未**设 `:swipe-close="false"`，不带 `.h-popup--swipe-disabled`，应继续使用默认 surface 底，不受本修复影响。

## Requirements

- 沉浸式播放页在**下拉过程中**与**未达关闭阈值回弹时**，顶部不得再出现纯白（`--h-color-surface`）露底。
- **下拉露顶时期望（已确认）**：panel 透明，透过 `.h-popup__overlay`（约 36% 黑）看到底层列表/页面内容，接近系统 sheet 下滑预览。
- 修复范围仅限播放页弹层视觉；不改队列页、不改关闭阈值/手势逻辑、不改 AMLL 背景生命周期。
- 静止全屏时沉浸背景、封面 mesh、fallback 渐变行为保持不变（无额外闪白、无裁切回归）。
- 样式落在宿主 CSS（`src/theme/tailwind.css`），**禁止**改 `node_modules/happier-ui`。
- `.player-overlay` **不要**铺死 `--muses-immersive-void` 外层底，否则会挡住「透出底层内容」的目标。

## Acceptance Criteria

- [ ] 播放页可下滑关闭手势过程中，顶部不再出现纯白底。
- [ ] 下拉露顶区域可见半透明遮罩下的底层页面内容（非死白、非纯 void 填色）。
- [ ] 未达关闭阈值松手回弹后，画面恢复全屏沉浸，无白闪残留。
- [ ] 队列页 fullscreen 弹层背景/列表观感无回归。
- [ ] 播放页打开静止态：封面背景 / fallback / 无封面路径无新增闪白。
- [ ] 不修改 `node_modules`；手势关闭阈值与 `dragOffsetY` 语义保持原状。

## Out of Scope

- 重新设计下滑跟手动画曲线或关闭阈值。
- 改造 `HPopup` 组件库通用 API。
- 状态栏、safe-area、歌词面板手势隔离。
- 下拉时额外自定义遮罩透明度动画（沿用现有 `.h-popup__overlay` 即可）。

## Implementation sketch（轻量，非独立 design）

在现有 `.h-popup--swipe-disabled` 块中为 panel 去白底：

```css
.h-popup--swipe-disabled .h-popup__panel {
  background: transparent;
}
```

可选配套：`overflow: hidden`（fullscreen 已全屏，避免透明 panel 上多余滚动条）。  
验证：Player 下拉透底；Queue 白/浅底不变；静止态 AMLL 背景无闪。

## Notes

- 轻量样式修复，PRD-only 即可；实现约 1 处 CSS。
- 选择器复用已有 Player 宿主区分类，避免误伤 Queue。

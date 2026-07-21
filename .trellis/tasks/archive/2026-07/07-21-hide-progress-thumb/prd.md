# 进度条改用 ion-range（去掉圆点）

## Goal

关闭 GitHub #41：播放页进度条改用 Ionic 的 `ion-range`，默认不展示 knob/圆点，只保留可拖动轨道与已播放填充；seek 能力与手势隔离保持可用。

## Background

- 现状：`PlayerPage.vue` 使用原生 `input[type=range]` + 自绘三层轨道（底轨 / 缓冲 / 已播放）+ 自定义 thumb 样式。
- 用户决策：不要再维护原生 range + 圆点样式，**直接改用 `ion-range`**。
- Ionic 已依赖 `@ionic/vue` 8.x，组件可用：`IonRange` / `<ion-range>`。
- `ion-range` 支持 CSS 变量：`--knob-size`、`--knob-background`、`--bar-background`、`--bar-background-active`、`--bar-height` 等，便于隐藏圆点并定制轨道。

## Requirements

### R1. 控件替换
- 播放页进度控制由原生 `<input type="range" class="progress-slider">` 替换为 `<ion-range class="progress-range">`（类名可微调，但测试需同步）。
- `min=0`，`max=duration`（duration 为 0 时禁用或 max 兜底为 1），`step` 保持细粒度（如 `0.1`）。
- `value` 绑定当前播放位置；拖动中可用本地 preview，松手后提交 `seekPlayback`。

### R2. 无可见圆点
- 隐藏 knob：通过 `--knob-size: 0` 和/或透明 `--knob-background` / `--knob-box-shadow: none`，桌面与窄屏均不可见圆点。
- 仍须可拖动与点击轨道 seek（依赖 ion-range 自带交互；必要时保留加大 hit 区域的容器高度）。

### R3. 视觉与缓冲层简化
- 轨道视觉以 `ion-range` 自带 bar 为主：`--bar-background`（未播放）、`--bar-background-active`（已播放）。
- **不再维护**独立的 `.progress-track-buffered` 视觉缓冲层 DOM（本任务范围内去掉中间缓冲色条）。
- 控制器层 `bufferedPosition` / seek 上限逻辑可暂时保留（Out of Scope 不删业务算法），但 UI 不再画缓冲条。
- 「缓冲中」轻提示文案若仍有入口可保留；无缓冲条时不强依赖 `--buffered` CSS 变量。

### R4. 事件与手势
- 拖动中：`ionInput` → 更新 preview + `seekGestureLocked`。
- 松手/确认：`ionChange` → `seekPlayback` + 解锁调度。
- `.progress-area` 继续 `@touchstart.stop` / `@pointerdown.stop` 手势隔离，避免误触上一曲/下一曲或切面板。
- `isNativeInteractiveEvent` / 手势锁需把 `ion-range` 识别为原生可交互控件（若当前只认 `input/range`，需扩展）。

### R5. 测试与规范
- 更新 `tests/unit/player.spec.ts` 中依赖 `.progress-slider` / 原生 input 的选择器与交互模拟。
- 任务完成后同步 `.trellis/spec/frontend/component-guidelines.md` 中「三层进度条 + 原生 range」约定为 `ion-range` 方案（实现收尾时做，或 Phase 3）。

### Out of Scope
- 不改 `seekPlayback` 缓冲 clamp / 歌词点击 seek 业务语义（除 UI 事件接线）。
- 不改 MiniPlayer / 媒体通知进度。
- 不发版。
- 不引入 dual-knobs / pin 气泡。

## Acceptance Criteria

- [ ] 播放页进度控件为 `ion-range`，无可见 knob/圆点
- [ ] 拖动与点击可正常 seek；手势不误触上一曲/下一曲/切面板
- [ ] 不再展示独立缓冲色条层
- [ ] 相关 player 单测通过
- [ ] 关闭 GitHub issue #41

## Technical Notes（非设计全文，实现提示）

```vue
<ion-range
  class="progress-range"
  :min="0"
  :max="durationForSlider"
  :step="0.1"
  :value="effectiveSeekPosition"
  :disabled="!canSeek"
  aria-label="播放进度"
  @ionInput="onSeekInput"
  @ionChange="onSeek"
/>
```

样式示例：

```css
.progress-range {
  --knob-size: 0px;
  --knob-box-shadow: none;
  --bar-height: 4px;
  --bar-background: rgba(255, 255, 255, 0.2);
  --bar-background-active: #fff;
  padding: 0;
}
```

## Notes

- 轻量偏中：仍以 PRD-only 推进；若实现中发现事件/手势耦合过深，可补一页短 `implement.md`。
- 关联：#41；历史 #37 修过原生 range 填充与点击，换 `ion-range` 后需回归这两点。

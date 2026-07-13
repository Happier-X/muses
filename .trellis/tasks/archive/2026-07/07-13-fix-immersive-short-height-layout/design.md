# 设计：沉浸式矮屏/横屏控制页收紧

## 边界

| 层 | 职责 |
|----|------|
| `PlayerPage.vue` 样式 | 封面正方形约束 + 矮屏控制区收紧 |
| `component-guidelines.md` | 记录窄屏也须 height 约束 width 的约定 |
| 不改 | `controller` / seek / 歌词页 DOM 与 AMLL 参数 |

## 根因

1. **窄屏封面 width 无高度上限**  
   `width: min(72vw, 100%, 340px)` + `max-height: 100%` + `aspect-ratio: 1`  
   当 cover-slot 高度 < 目标宽度时，高度被夹、宽度仍按 vw → 矩形。

2. **控制区固定占高过大**  
   panel padding + gap + 进度 24px 热区 + 52/68 主控 + 44 模式栏在横屏/车机矮高下挤掉封面。

## 方案

### 1. 封面：全断点“宽高互限”

原则：正方形边长 = `min(水平上限, 垂直上限)`。

**窄屏默认**（示意）：

```css
.cover,
.placeholder-cover {
  width: min(72vw, 100%, 340px, 52dvh); /* 或与 cover-slot max-height 对齐的 dvh/px */
  max-width: 100%;
  max-height: 100%;
  aspect-ratio: 1;
  height: auto;
  object-fit: cover;
}
```

**窄屏矮高**（`max-height: 720px`）：

```css
.cover-slot { max-height: min(42dvh, 260px); }
.cover,
.placeholder-cover {
  width: min(72vw, 100%, 260px, 42dvh);
}
```

**宽屏**保持现有 `min(40vw, 48dvh, 320px)` / 矮高 `42dvh/260px`，必要时把 placeholder 与 cover 规则对齐。

占位 `.placeholder-cover` 与 `.cover` 共用尺寸规则，避免无封面时仍矩形。

### 2. 控制区：分层 media 收紧（不隐藏）

建议断点（实现可微调数值，语义固定）：

| 视口 | 动作 |
|------|------|
| 默认（正常竖屏） | 保持现有较大控件与 padding |
| `max-height: 720px` | 收紧 panel 上下 padding、inner gap；进度 track 热区略减；主控/模式栏略缩（已有按钮缩小，补 padding/gap） |
| 可选更矮 `max-height: 520px` 或 `max-height: 480px` | 再收一档 gap/字号/按钮，仍显示全部控件 |

不引入 landscape 专用结构；横屏通常命中 `max-height` 断点即可。

### 3. Flex 分配

- `.cover-slot` 继续 `flex: 1 1 auto; min-height: 0`，吃掉剩余高度。
- `.song-info` / `.progress-area` / `.controls` / `.mode-bar` 保持 `flex: 0 0 auto`。
- `info-panel-inner` 矮屏减小 `gap`，必要时 `justify-content: space-between` 保留，避免控件重叠。

### 4. 歌词页

本任务 **零改动** lyric-panel / lyric-header / lyric-player 样式。

## 风险

| 风险 | 缓解 |
|------|------|
| width 含 52dvh 在正常高度过小 | 与 cover-slot max-height 同档；正常高度 52dvh 通常 ≥ 72vw 上限，取 vw 侧 |
| 进度热区过小难点 | 矮屏可减到 ~18–20px，不低于可点下限 |
| 安全区 | padding 用 `calc(... + safe-area)`，只减固定 16px 部分，不抹掉 safe-area |
| 宽屏回归 | 不改 panels 分栏；仅对齐 placeholder 与 cover |

## 回滚

还原 `PlayerPage.vue` 中 cover width 与矮屏 media 规则及 spec 增补即可。

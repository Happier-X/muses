# 实施清单：沉浸式矮屏/横屏布局

## 有序步骤

1. **封面正方形（窄屏）**
   - `.cover` / `.placeholder-cover` width 增加 dvh（及与 cover-slot 对齐的 px）上限
   - 矮屏 media 同步收紧 width 与 cover-slot max-height
   - 宽屏保持并确认 placeholder 一致

2. **控制区收紧**
   - `max-height: 720px`：减小 panel 上下 padding、`info-panel-inner` gap、进度 slider 高度/thumb 相关间距、controls gap、mode-bar 尺寸（在现有按钮缩小基础上补全）
   - 可选第二档更矮断点，仍不隐藏控件
   - 保留 safe-area 计算

3. **回归检查**
   - 正常竖屏：控件与封面尺寸不明显缩水
   - 横屏/矮高：封面正方形、控制区更紧、一屏无滚动
   - 宽屏分栏未破坏

4. **spec**
   - `component-guidelines.md`：窄屏封面 width 也必须含高度约束；矮屏收紧策略；禁止只靠 max-height clamp 高度

5. **验证**
   - `npx vitest run`（至少 player 相关）
   - `npm run lint`
   - `npx vue-tsc --noEmit`
   - 人工/CSS 审查关键 media 块

## 回滚点

- 仅 `PlayerPage.vue` 样式 + `component-guidelines.md`

## 启动前

- [x] 决策收敛（A/A/A）
- [x] prd / design / implement
- [ ] 用户审阅并确认开始
- [ ] curate jsonl
- [ ] `task.py start`

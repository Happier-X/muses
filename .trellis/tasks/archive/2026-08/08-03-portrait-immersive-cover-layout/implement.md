# 实现清单 — 竖屏沉浸式封面排版

## 范围

轻量 UI：仅竖屏控制页垂直节奏（方案 3 整体居中收紧）。

## 步骤

1. **窄屏 `.info-panel-inner`**
   - `PlayerPage.vue`：`justify-between` → `justify-center`。
   - 视需要微调 `gap`（保持可被矮屏 CSS 覆盖）。

2. **窄屏 `.cover-slot` 停止 flex-grow**
   - 由 `flex-[1_1_auto]` 改为不扩张、可收缩（如 `flex-[0_1_auto]` 或 CSS `flex: 0 1 auto`），保留 `min-h-0`。
   - 保留 `max-height` 与封面正方形 width 的 dvh 对齐规则。

3. **`tailwind.css` 对齐**
   - 确认默认 / `max-height:720` / `520` 与 `min-width:768` 规则不互相打架。
   - 平板分支已有 `justify-content: center`；若模板 utility 改全局，确认宽屏仍正确。

4. **自检**
   - 窄屏高机：内容组居中、不松散、不贴顶。
   - 矮屏/横屏：无溢出、无纵向滚动、封面仍正方形。
   - 平板 + 歌词页 + 下滑/横滑：无回归。

5. **Spec**
   - 实现通过后更新 `component-guidelines.md`：竖屏整体居中 + cover-slot 不 grow。

## 验证

- 目视 / 真机或模拟器竖屏控制页。
- 必要时 `npm run build` 或项目既有 lint/typecheck（若有）。

## 回滚

- 还原 `PlayerPage.vue` 与 `tailwind.css` 中 info-panel / cover-slot 相关 diff。

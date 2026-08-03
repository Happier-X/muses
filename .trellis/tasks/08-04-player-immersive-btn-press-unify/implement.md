# 实现清单 — 沉浸页图标按钮按下态统一

## 步骤

1. **`tailwind.css` 统一沉浸 ghost 交互**
   - 新增 `.player-overlay .h-button--ghost`（或等价）基类：`color` 浅色、`background` 透明/微透明、`:hover` / `:active` 半透明白底。
   - 覆盖 `.is-active` 及 active+hover，避免掉回库浅灰。
   - 主控 / mode-bar 保留尺寸与默认透明度差异（mode-bar 可更淡字色），删除与基类冲突的半截规则。
   - 将 `.lyric-fab` 收敛到同一套，去掉重复 hover/active 若已由基类覆盖。

2. **`PlayerPage.vue`（仅必要时）**
   - 一般无需改模板；若需统一 class 标记可加 `player-icon-btn`，优先 CSS 选择器。

3. **Spec**
   - `component-guidelines`：沉浸页所有 ghost 图标键必须覆盖 color/background/hover/active；禁止只设 color。
   - `features-player`：一句指向统一按下态（可选）。

4. **验证**
   - lint / build；逻辑不改播放路径。

## 回滚

- 还原 `tailwind.css` 与相关 spec；歌词 FAB 可回退到上一版独立规则。

# 播放条/FAB 液态玻璃效果不明显

## Goal

用户反馈底部播放条与悬浮按钮（jump-fab）「还是没有液态玻璃效果」——实测当前渲染，确认 backdrop-filter 是否生效、参数（alpha/blur）是否合适，调整为肉眼可辨的液态玻璃质感。

## Background

- v0.3.3 已将 MiniPlayer/FAB 改为 `rgba(255,255,255,0.85) + blur(30px) + 高光边`（对齐椒盐 Liquid Glass）。
- 用户仍觉得没效果。可能原因：
  1. **参数过实**：0.85 白底 + 30px 强模糊 → 底下内容几乎不可辨，视觉接近实心白；
  2. **backdrop-filter 失效**：fixed 元素 + WebView 的合成层限制（需实测确认）；
  3. FAB 悬浮在列表上方时底下内容滚动经过，若 blur 失效则无玻璃感。

## Requirements

- 播放条/FAB 底下滚动内容经过时**肉眼可见模糊透出**（液态玻璃质感）。
- 若 backdrop-filter 失效需定位原因（fixed/合成层/WebView 版本）。
- 手机与平板模式一致；深浅主题不回归。

## Acceptance Criteria

- [ ] MuMu 实测：滚动歌曲列表，播放条/FAB 底下内容模糊可见（截图对比确认）。
- [ ] 参数调整后视觉自然（不透不实）。
- [ ] `vue-tsc` / ESLint / 构建通过。

## Notes

- 轻量任务，PRD-only。

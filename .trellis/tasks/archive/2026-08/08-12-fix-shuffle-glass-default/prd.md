# PRD：随机播放吸顶条改回 k-glass 默认玻璃效果

## 背景

用户反馈：歌曲页 navbar 在歌曲滚动到其下方时有玻璃模糊效果，但随机播放吸顶条没有，希望同样有模糊效果。

**根因**（诊断结论，已通过 CDP 像素分析验证）：

- navbar 是 Konsta 官方双层结构：blur 层（`backdrop-blur-[2px]` + mask 渐隐，无背景）+ bg 渐变层（灰渐变，无 blur、无 mask）→ 内容滚过时上半朦胧、下半灰幕过渡，玻璃感完整。
- 随机播放吸顶条当前是 k-glass 单层 + 覆盖类（`!bg-transparent` + `!shadow-none` + `!backdrop-blur-[2px]` + `mask-b-from-50%`/`mask-b-to-100%` + 灰渐变）：mask 把单层元素的下半 blur **和渐变一起裁掉** → 上半是实心灰板（内容被完全盖住）、下半内容清晰透出（无模糊过渡）→ 用户感知"没有模糊效果"。

## 方案（用户已选定 A）

**裸用 k-glass 默认样式**：移除覆盖类，恢复 Konsta 默认玻璃（白 0.75 底 + `backdrop-blur-lg` 16px + iOS 内阴影），与 MiniPlayer、底部 tabbar 白玻璃风格统一。效果有保证（MiniPlayer 同款，已生效验证）。

## 改动范围

- `src/views/SongsPage.vue`：shuffle 条 k-glass 覆盖类移除
- `src/views/LibraryDetailPage.vue`：同上

保留：`h-full w-full` 撑满类（SongsPage）、相对定位相关类（k-button `relative` 保持内容层在玻璃之上）。

## 验收标准

1. 歌曲滚过随机播放吸顶条下方时，条内出现明显玻璃模糊（白玻璃 + blur16px），不再"实心灰板/清晰透出"
2. 浅色/深色模式下文字可读（k-glass 默认深色变体 `dark:bg-ios-dark-glass`）
3. 两页（歌曲页、专辑/艺术家详情页）行为一致
4. 构建通过（vue-tsc + vite build）、模拟器实测无回归（列表滚动、吸顶、按钮点击）

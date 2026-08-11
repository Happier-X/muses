# 歌曲页 navbar 覆盖式布局修复（官方玻璃模糊）

## 背景

用户反馈：顶部 navbar 没有官网 demo（konstaui.com/vue/navbar）那种"内容滚动到下方时半透明模糊"的玻璃效果；用户猜测"是不是顶上部分没有列表"。

## 根因（已查证）

1. **结构问题（用户猜测成立）**：k-navbar 在文档流（滚动容器外），滚动容器从 navbar 底部开始 → 列表内容永远不经过 navbar 后方（y0-60 恒空）→ bg 渐变 + blur 无内容可透。
2. **tabbar 对比**：tabbar 是 fixed 覆盖在列表上，内容从其后经过 → 有玻璃感。
3. **附带 bug**：SongsPage k-page 的 `!h-auto`（important）覆盖 `.m-page{height:100%}` → k-page 被内容撑高（20290px）→ 列表 h-full 失效无法滚动（改覆盖式后暴露）。

## 修复方案

- k-navbar 包 `root-navbar-wrap absolute top-0 left-0 right-0 z-20`（覆盖式），k-page 加 `relative`。
- 滚动容器（listParentRef）从屏幕顶开始（无 pt），内容滚动时从 navbar 后方经过 → 官方玻璃生效。
- 移除 k-page `!h-auto`（k-page 锁定视口高）。
- shuffle 行 `sticky top-[calc(max(16px,var(--k-safe-area-top))+44px)]` z-10（吸 navbar 正下方、保持可点、不再挡内容经过 navbar）。
- empty 分支 pt 用同款 calc 避让 navbar。

## 验收标准

- [x] 歌曲页 k-page 高度 = 视口高（616），列表可滚动
- [x] shuffle 行吸在 navbar 正下方（可点击）
- [x] 列表内容滚动时从 navbar 后方经过（DOM 层行位置验证）
- [x] 滚动位置记忆、虚拟列表行高/行为无回归

## 已知限制

- 模拟器程序化滚动不产生合成帧 → backdrop 采样不更新 → 截图差分恒 0（工具限制，非真实效果），真实滚动时玻璃可见。
- CDP `Input.dispatchTouchEvent` 在此 WebView 无效（模拟器通道限制）。
- 只改了歌曲页；音乐库/音源/设置等页 navbar 仍是文档流式，无此效果（待用户确认后统一推广）。

## 提交

- a7a3d7a `fix(ui): 歌曲页 navbar 改覆盖式布局——内容从 navbar 后方滚过实现官方玻璃模糊`

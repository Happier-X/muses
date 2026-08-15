# 修复切歌时三行歌词第一行被裁切

## Goal

修复沉浸式播放页三行歌词窗口在**歌曲切换时**出现的异常：窗口整体下跳再滚回，视觉上第一行歌词被裁切/错位。

## Background

- 三行歌词窗口（`player-page__song-meta` 79px 视口 + `player-page__meta-window` 五行窗口）由两套 watch 驱动（`src/views/PlayerPage.vue`）：
  - `watch(lyricWindow)`（默认 pre flush）：换歌/换词/翻译显隐等**非切行同步**——直接更新 `displayedWindow`，无动画。
  - `watch(lyricContext.current)`（post flush）：**仅应在播放中相邻切行时**触发单段滚动动画（translateY 0 → -29.5px，0.4s），完成后换窗口数据 + 清内联 transform 回落 CSS 变量。
- **根因**：切歌时（新歌有歌词）`lyricContext.current` 从旧歌当前行变为新歌第 1 行，被第二个 watch 误判为"切行"而触发滚动动画：
  1. pre flush 的 `watch(lyricWindow)` 已把 `displayedWindow` 换成新歌五行 `[空, 空, 新行1, 新行2, 新行3]`；
  2. post flush 动画启动先设 `translateY(0px)` → 视口显示变成 `[空, 空, 新行1]`，第 2 行歌词掉出视口底部被裁；
  3. 0.4s 动画再滚回 `-29.5px`，整体表现为"第一行被裁切/窗口下跳再上滑"。
- 判断依据：**相邻切行**时新窗口的 prev 行文本 === 旧 current（prev 参数）；**窗口整体重置**（切歌/翻译切换等）时新窗口 prev 行是空或其他行，不相等。

## Requirements

- 切歌（含在线歌词异步加载完成后落词）必须走"非切行同步"路径：直接换窗口，不播滚动动画，不产生下跳/裁切。
- 播放中正常相邻切行保留现有单段连续上移动画（视觉连续、无跳动）。
- 翻译显隐切换、歌词整体变化（seek 大跳等）同样不得触发切行动画。
- 窄高屏单行模式行为不变。

## Acceptance Criteria

- [ ] 切歌瞬间三行歌词直接显示新歌三行（prev/current/next 结构正确），无下跳、无裁切、无滚动动画。
- [ ] 播放中相邻切行仍为单段 0.4s 连续上移动画，动画结束无跳变。
- [ ] 在线歌词匹配完成落词（空词 → 有词）不触发切行动画。
- [ ] 翻译显隐切换直接刷新窗口，不播切行动画。
- [ ] `npm run build`（vue-tsc + vite build）通过，无新增 lint 错误。

## Notes

- 轻量任务，PRD-only，无需 design.md / implement.md。
- 修复点：`src/views/PlayerPage.vue` `watch(lyricContext.current)` 内、窄高屏分支之后动画启动之前，增加"新窗口 prev 行 !== prev → 直接换窗口"的判断。

# 列表底部内容被 MiniPlayer 遮挡——内容止位漏算 8px 悬浮空隙

## Goal

修复真机上列表滚动到底时最后一项底部被底部悬浮 MiniPlayer 条遮挡的问题。遮挡发生在歌曲页、专辑页、艺术家页（以及歌单、音源、详情等所有消费内容止位 token 的页面）。

## 根因

- MiniPlayer 为悬浮胶囊：`bottom: calc(var(--m-safe-area-bottom) + 8px)`，高度 64px，实际占用纵向范围 = **8px 悬浮空隙 + 64px + 底部安全区**（约 72px + safe-area）。
- 全局内容止位 token `--m-content-pb` / `--m-content-pb-md` 目前为 `calc(64px + var(--m-safe-area-bottom, 0px))`，只预留了 MiniPlayer 高度 64px，漏算底部 8px 悬浮空隙 → 列表滚到底时最后一项底部约 8px 被盖住。

## Requirements

- 内容止位 token 修正为 `calc(72px + var(--m-safe-area-bottom, 0px))`（64px 高度 + 8px 悬浮空隙 + 安全区），一处改动覆盖全部消费页面。
- 保持 token 语义与注释一致；MiniPlayer 定位样式本身不动（悬浮观感不变）。
- 不引入新的魔法数字散落页面；统一走 `--m-content-pb` token。

## Acceptance Criteria

- [ ] `src/theme/index.scss` 中 `--m-content-pb` 与 `--m-content-pb-md`（默认块 + `html.muses-mini-visible` 块，共 4 处）均更新为 72px 基准，公式不含重复/冗余写法。
- [ ] 页面级（SongsPage/AlbumsPage/ArtistsPage 等）无新增 padding 改动，仍只消费 token。
- [ ] 构建通过（lint / tsc / vite build 无新错误）。
- [ ] 真机验证：歌曲页/专辑页/艺术家页列表滚动到底，最后一项完整可见，不被 MiniPlayer 遮挡；MiniPlayer 悬浮 8px 观感不变。

## Notes

- 轻量修复任务，PRD-only，不需要 design.md / implement.md。
- 浏览器调试时列表项若非贴底滚动不易察觉，需在真机或模拟底部遮挡场景验证。

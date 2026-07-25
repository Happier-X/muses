# PRD: 音源页添加按钮改为 ghost 变体

## 问题

音源页面右上角的添加图标按钮使用了 `variant="primary"`，而其他页面（如歌曲页）的图标按钮统一使用 `variant="ghost"`，风格不一致。

## 目标

将 SourcesPage 导航栏的添加图标按钮的 variant 从 `primary` 改为 `ghost`，与歌曲页保持一致。

## 验收标准

1. SourcesPage 右上角添加图标按钮视觉上为 ghost 样式（无填充背景）
2. 与 SongsPage 的搜索图标按钮风格一致

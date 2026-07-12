# 歌曲页随机播放与跳转当前播放

## Goal

为歌曲页（SongsPage）增加两个便捷播放操作，对应 Issue #6 与 #7。

## Issue Map

| Issue | 子任务 | 内容 |
|-------|--------|------|
| #6 | `07-12-songs-page-shuffle-all` | 顶部左侧加随机播放按钮，点击随机播放全部歌曲 |
| #7 | `07-12-songs-page-jump-current` | 右下侧悬浮按钮，跳转到当前播放歌曲 |

## Parent Acceptance

- [ ] 两个子任务实现并通过各自验收
- [ ] 相关 issue 关闭
- [ ] 单测与 lint/type-check 通过

## Scope

- 主要改动面：`src/views/SongsPage.vue`、相关单测、frontend spec
- 父任务本身不直接改代码，只做编排

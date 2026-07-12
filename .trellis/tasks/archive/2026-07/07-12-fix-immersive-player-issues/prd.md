# 修复沉浸式播放页 open issues

## Goal

逐个修复 GitHub 上与沉浸式播放页相关的 open issues，提升拖动进度、视觉与歌词交互体验。

## Issue Map

| Issue | 子任务 | 优先级 |
|-------|--------|--------|
| #5 调节进度时触发上一曲/下一曲 | `07-12-fix-seek-prev-next` | 1（先修） |
| #4 封面后阴影去掉 | `07-12-remove-cover-shadow` | 2 |
| #3 中间按钮只要图标不要背景圆 | `07-12-plain-control-icons` | 3 |
| #2 歌词点击跳转到对应行 | `07-12-lyric-click-seek` | 4 |

## Parent Acceptance

- [ ] 四个子任务均实现并通过各自验收
- [ ] 相关 issue 关闭
- [ ] 播放器单测与 lint/type-check 通过

## Scope

- 主要改动面：`src/views/PlayerPage.vue`、相关单测、frontend spec
- 父任务本身不直接改代码，只做编排与最终整合验收

# 沉浸式页隐藏信息补充提示 (#48)

## Goal

沉浸式播放页不再展示「正在补充歌曲信息…」「歌曲信息补充失败…」等元信息补充过程提示，界面更干净。

## Background

`PlayerPage.vue` 曲名/副标题下方当前有：

```vue
<small v-if="playerState.metadataStatus === 'scanning'">正在补充歌曲信息…</small>
<small v-else-if="playerState.metadataStatus === 'failed'">歌曲信息补充失败，已使用当前信息播放。</small>
```

后台 `scanSongMetadata` / 在线补全逻辑保留；仅去掉沉浸式页上的过程/失败文案。

## Requirements

### R1. 沉浸式页不展示补充提示
- 移除（或永久不渲染）上述 `metadataStatus` 为 `scanning` / `failed` 的 UI 文案。
- 不新增替代 toast / banner（除非产品后续另开需求）。

### R2. 后台逻辑不变
- `metadataStatus` 状态机、扫描、在线补全、写库、媒体会话同步保持可用。
- 其他页面若无同类提示则无需改动。

### Out of Scope
- 设置页开关「是否显示补充状态」
- 修改歌词 matching / 封面 matching 的错误提示策略（本 issue 仅点名歌曲信息补充文案）

## Acceptance Criteria

- [x] 沉浸式播放页不可见「正在补充歌曲信息…」「歌曲信息补充失败…」
- [x] 播放与元信息补全行为不因去掉文案而中断
- [x] lint / 相关测试通过
- [ ] 关闭 GitHub #48

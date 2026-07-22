# 队列与长列表虚拟化

## Goal

避免 QueuePage、PlaylistDetailPage、AlbumsPage 和 ArtistsPage 因一次挂载大量 Ionic 行节点而卡顿。

## Requirements

- QueuePage 和 PlaylistDetailPage 使用 `@tanstack/vue-virtual`。
- QueuePage 保留选择、删除、清空和当前曲视觉状态；打开时能定位当前曲。
- 若 `ion-item-sliding` 与虚拟行复用冲突，允许改为明确的行尾删除按钮，不能损失删除能力。
- AlbumsPage / ArtistsPage 仅在数据量足以产生收益时虚拟化；宽屏现有双列语义不得回归。
- 空态、播放、更多菜单、封面和可访问标签保持不变。

## Acceptance Criteria

- [x] 1000 项队列/歌单不再创建 1000 个行 DOM
- [x] 队列当前项定位、选择和删除正确
- [x] 歌单播放和菜单行为正确
- [x] 专辑/艺术家宽屏布局不回归（本任务未改动其双列实现）
- [x] 组件单测、lint、build 通过

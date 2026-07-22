# 队列与长列表虚拟化实现清单

1. [x] QueuePage 使用 `@tanstack/vue-virtual`，只渲染可视队列行
2. [x] 保留当前项定位、播放、删除、清空、空态与无障碍语义
3. [x] 用明确行尾删除按钮替代 `ion-item-sliding`，避免虚拟行复用冲突
4. [x] PlaylistDetailPage 使用 `@tanstack/vue-virtual`，保留播放全部、单曲播放、移除与封面
5. [x] 使用原生滚动容器、`data-index`、`measureElement` 和实际行高测量
6. [x] AlbumsPage / ArtistsPage 保持现有宽屏双列，不引入高风险多列虚拟布局
7. [x] 完整单测、lint、build、`git diff --check`
8. [ ] 提交并归档任务

## 验证结果

- 完整单测：17 个测试文件，302 passed
- lint：通过
- build：通过
- `git diff --check`：通过

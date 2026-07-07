# 展示入库音乐数据实施计划

## 实施步骤

1. 新增 `src/features/library/views.ts`，实现歌曲排序、时长格式化、专辑聚合、艺术家聚合。
2. 将 `Tab1Page.vue` 重命名并改造为 `SongsPage.vue`：读取 `loadSongs()`，展示歌曲列表和空状态。
3. 将 `Tab2Page.vue` 重命名并改造为 `AlbumsPage.vue`：读取歌曲并展示专辑聚合列表和空状态。
4. 将 `Tab3Page.vue` 重命名并改造为 `ArtistsPage.vue`：读取歌曲并展示艺术家聚合列表和空状态。
5. 更新或新增单元测试，覆盖聚合 helper、歌曲页渲染、空状态。
6. 运行 `npm run test:unit -- --run`、`npm run lint`、`npm run build`。

## 验证命令

- `npm run test:unit -- --run`
- `npm run lint`
- `npm run build`

## 风险点

- Ionic 生命周期在单元测试中可能不触发，页面刷新逻辑需要同时兼容 Vue `onMounted`。
- 旧 starter 测试断言 `Tab 1 page` 会失效，需要更新为新用户可见内容。
- 聚合字段可能缺失或为空字符串，需要统一归入未知项。

## 启动前检查

- 用户确认是否需要专辑/艺术家点击后展开或进入歌曲列表；默认本任务只做聚合列表展示。
- `implement.jsonl` 和 `check.jsonl` 已加入前端规范上下文。

# 设计：编辑 sheet 云端 UI

## 依赖

`@/features/editMeta` 的 `searchEditCloudMeta`（API 子任务已归档）。

## 交互

- 表单顶部「从云端获取」；打开编辑不自动搜。
- loading / 分维状态 / 全失败文案。
- 三维当前候选摘要 +「更换」展开列表；切换只改 index。
- `HCheckbox` × title/artist/album/cover/lyrics；有值默认勾。
- 「应用到表单」：勾选字段写入 form / 封面；封面先 `cacheRemoteCover`。
- 歌词应用主词 text；`editLyricsFormat` 记 format（保存 dirty 时写入 patch，不再写死 lrc）。
- 关 sheet / 切歌：`AbortController.abort` + 清空云端 UI 状态。

## 文件

主要改 `src/views/PlayerPage.vue`；逻辑尽量内聚于编辑区块，不抽独立大组件（MVP）。

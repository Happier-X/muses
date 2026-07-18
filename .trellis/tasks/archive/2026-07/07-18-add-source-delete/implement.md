# 执行计划

1. 更新 `implement.jsonl` / `check.jsonl` 指向 frontend state/component/quality specs。
2. 在 sources storage 增加 `deleteSource`，并为 WebDAV 凭据删除写单测。
3. 在 SourcesPage 增加删除确认状态、`ion-alert`、删除按钮与处理函数。
4. 删除成功后调用 `reconcileSourceSongs(source.id, [])` 清理该音源歌曲。
5. 运行 `npm run lint`、`npm run build`、`npm run test:unit -- --run`。
6. 检查 diff，确认只实现 #33。

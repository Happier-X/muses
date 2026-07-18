# 执行计划

1. 扩展 `ScanSummary`：增加 `removed`，同步 `createEmptySummary` 与 SourcesPage 进度初始值/展示/完成文案。
2. 在 `storage.ts` 实现 `reconcileSourceSongs`。
3. 在 `scanner.ts` 发现成功后收集路径，循环结束后对账并写入 `summary.removed`。
4. 在 `tests/unit/library.spec.ts` 增加空扫描、部分删除、跨音源隔离用例。
5. 运行 `npm run lint`、`npm run build`、`npm run test:unit`。

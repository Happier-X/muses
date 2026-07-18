# 执行计划

1. 完善 PRD、设计和上下文清单，明确 #36 的三类现象。
2. 检查并修复 `mergeTranslation.ts` 的时间戳解析、容差匹配和相邻双行合并边界。
3. 修复 `PlayerPage.vue` 中翻译副行 active 样式选择器，确保开关和 TTML/LRC/YRC/QRC 管线一致。
4. 补充 `lyrics-merge-translation.spec.ts` 与 `player.spec.ts` 回归测试。
5. 运行 `npm run lint`、`npm run build`、`npm run test:unit -- --run` 和 `git diff --check`。
6. 运行独立检查，更新前端规范，提交、关闭 GitHub #36 并归档任务。

## 风险

- 译文时间戳容差过大可能导致错配；必须有边界测试。
- AMLL CSS module 类名是运行时实际类名，不能凭猜测新增选择器。
- 不修改 node_modules；样式修复只在页面 scoped CSS 中完成。

# 执行计划

1. 在 `queue.ts` 增加有界的失败恢复候选选择 helper，使用 active order、忽略单曲循环并跳过 attempted ids。
2. 在 `controller.ts` 增加仅内部使用的恢复上下文和播放入口参数；将 play catch 接入自动恢复。
3. 确保 generation 过期不推进、继续恢复时不清空下一首媒体会话、终止时保留安全错误。
4. 在 `tests/unit/player.spec.ts` 增加单失败、连续失败、全失败、单曲循环、随机顺序和过期 generation 测试；增加 queue helper 有界行为测试。
5. 运行 `npm run lint`、`npm run build`、`npm run test:unit -- --run`、`git diff --check`。
6. 独立检查并修复问题，更新播放器规范。
7. 提交、关闭 GitHub #40、归档任务。

## 回滚点

- queue helper 独立于普通 `advanceToNext`，回滚失败恢复不会改变正常队列推进。
- 若恢复链影响媒体会话，可回滚 controller 接入，保留 helper 不会产生运行时副作用。

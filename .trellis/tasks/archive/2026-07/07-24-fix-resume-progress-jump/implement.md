# 实施计划

## 步骤

1. 阅读 `src/features/player/controller.ts` 的冷启动恢复、`playSongInternal()`、`applyNativeState()`、`seekPlayback()` 相关逻辑。
2. 在 controller 中增加冷启动恢复 seek 保护机制，确保恢复 seek 完成前同曲原生初始 position 不覆盖 UI 恢复位置。
3. 确保保护在以下路径清理：恢复 seek 成功、恢复 seek 失败、播放失败、用户主动点播新曲、普通 seek 或停止播放。
4. 更新 `tests/unit/player.spec.ts`：覆盖冷启动续播期间原生 position 0 不回退，以及最终仍 play + seek 到恢复进度。
5. 运行相关测试后再跑全量验证。

## 验证命令

- `npm run test:unit -- tests/unit/player.spec.ts --run`
- `npm run lint`
- `npm run build`
- `npm run test:unit -- --run`
- `git diff --check`
- `python ./.trellis/scripts/task.py validate 07-24-fix-resume-progress-jump`

## 回滚点

- 若 controller 状态保护导致普通播放进度冻结，回滚 controller 保护逻辑，保留测试作为复现用例重新设计。
- 若测试需要过度耦合私有实现，优先通过原生 listener 模拟真实事件，不导出内部状态。

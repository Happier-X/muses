# 执行计划

1. 在 `PlayerPage.vue` 增加 preview position 和统一 seek 提交函数。
2. 将进度条视觉改为自绘三层轨道，使填充层由 preview/播放器位置响应式控制。
3. 为轨道增加点击/指针位置换算，复用缓冲 clamp 和 seek 锁。
4. 补充 `tests/unit/player.spec.ts`：拖动填充同步、轨道点击、缓冲 clamp、提交失败恢复。
5. 运行 lint、build、完整 unit test、git diff --check。
6. 独立检查、更新播放器规范、提交并关闭 #37、归档任务。

## 风险

- range 原生 click 与外层点击可能重复提交，事件需在单一层处理并阻止重复。
- preview 必须在切歌、overlay 关闭和 seek 失败时清理，避免串曲。
- 不修改 controller 的缓冲与自然结束逻辑。

# 技术设计

## 边界

只修改 `src/views/PlayerPage.vue` 的模式图标映射和 `tests/unit/player.spec.ts` 的展示契约，不修改 `src/features/player/queue.ts` 的状态、随机顺序、循环推进或持久化。

## 映射

- `repeatMode === 'all'`：`repeatOutline`，标签“列表循环”。
- `repeatMode === 'one'`：`repeat`，标签“单曲循环”。
- `shuffleEnabled === false`：`listOutline`，标签“顺序播放”。
- `shuffleEnabled === true`：`shuffle`，标签“随机播放”。

使用现有 ionicons 导出，避免新增依赖。Vue computed 直接读取响应式 `queueState`，按钮点击继续调用现有 `setRepeatMode` / `toggleShuffle`，状态改变后图标、标签和激活样式一起更新。

## 兼容性与回滚

不改变配置格式和队列行为；若视觉反馈不符合预期，可单独回滚本次 PlayerPage 与测试提交，不影响播放器状态机。

# 队列线性解析

## Goal

将播放队列到歌曲对象的解析从 Q×N 查找降为 O(N+Q)，降低全库入队后的切歌和刷新卡顿。

## Requirements

- 单次解析只加载一次曲库。
- 使用 `songId` 索引 Map 解析队列。
- 保持队列顺序、随机顺序、缺失歌曲跳过、currentIndex 和恢复链语义。
- 不在本子任务引入跨窗口曲库缓存，控制一致性风险。

## Acceptance Criteria

- [ ] 解析过程中不再为每个队列项调用 `Array.find`
- [ ] 全库入队、缺失歌曲、随机模式和 next/previous 行为不回归
- [ ] 新增大队列正确性测试
- [ ] 单测、lint、build 通过

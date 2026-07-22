# 全应用性能审计与卡顿治理

## Goal

系统治理 #50 第一轮修复后仍可能导致卡顿、假死或长会话内存增长的热点，并通过独立子任务完成实现和验证。

## Scope

- 曲库扫描与本地持久化
- 播放队列解析
- 隐藏沉浸式播放器 / AMLL 渲染
- 在线歌词索引和匹配
- 队列、歌单、专辑、艺术家大列表
- 监听器、缓存和定时器生命周期

## Requirements

- 按子任务顺序实施：低风险数据路径 → UI 热点 → 在线索引和生命周期。
- 保持播放恢复、后台播放、ReplayGain、歌词匹配和队列语义。
- 不修改 `node_modules/@capgo/*` 或 AMLL 第三方源码。
- 不用删除产品功能换取性能；隐藏播放器仍须避免重开白闪。
- 每个子任务独立测试、检查、提交和归档；父任务最后做全量集成验证。

## Child Deliverables

1. `07-22-scan-batch-persist`：扫描批量一次持久化。
2. `07-22-queue-linear-resolve`：队列 O(N+Q) 解析。
3. `07-22-virtualize-long-lists`：队列和其它长列表虚拟化。
4. `07-22-hidden-player-render-budget`：隐藏 AMLL 渲染降载。
5. `07-22-optimize-amll-index`：AMLL 索引解析和匹配降载。
6. `07-22-runtime-cache-lifecycle`：缓存容量与 listener 生命周期。

## Acceptance Criteria

- [x] 审计覆盖主线程同步工作、响应式更新、列表、播放器、网络匹配和生命周期
- [x] 每个发现有证据、影响、优先级和建议
- [x] 用户确认全部实施
- [ ] 六个子任务全部通过各自验收并归档
- [ ] 父任务执行全量 lint、unit、build
- [ ] 无法在当前环境自动验证的真机 CPU/GPU 项明确交付人工验收步骤

## Out of Scope

- 无真机 Perfetto / Android Studio CPU Profile 时，不虚构精确帧率或耗时
- 不重写存储层为 SQLite/IndexedDB
- 不改变在线歌词产品优先级
- 不发版

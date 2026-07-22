# 实施计划

## 执行顺序

父任务只维护集成要求；代码在子任务中实现并独立提交。

### 批次 A：主线程同步热点

1. [x] `07-22-scan-batch-persist`
   - 扫描纯内存 upsert，成功后单次 saveSongs
   - 进度回调节流，最终态立即
   - 验证失败原子性和更新广播次数
2. [x] `07-22-queue-linear-resolve`
   - 单次曲库加载 + songId Map
   - 验证缺失歌曲、随机、恢复链和 next/previous

**回滚点 A**：两个子任务各自独立提交；数据语义异常时单独回滚，不影响 #47/#49 播放状态机。

### 批次 B：渲染热点

3. [x] `07-22-virtualize-long-lists`
   - QueuePage、PlaylistDetailPage 优先
   - Albums/Artists 按宽屏布局兼容方案处理
4. [x] `07-22-hidden-player-render-budget`
   - 保活前提下隐藏绘制
   - 冻结 AMLL 时间输入
   - 真机观察重开闪烁和 CPU/GPU

**Review gate B**：虚拟列表交互与播放器重开视觉必须分别检查；不允许用卸载 PlayerPage 规避问题。

### 批次 C：尖峰和长会话

5. [x] `07-22-optimize-amll-index`
   - 标题候选索引
   - JSONL 分片解析并让出事件循环
6. [x] `07-22-runtime-cache-lifecycle`
   - bounded cache / LRU
   - 根 listener handle remove

**回滚点 C**：候选索引必须与全量评分结果做对照测试；不一致时回滚候选缩小，仅保留分片解析。

## 每个子任务验证

```bash
npm run lint
npm run test:unit
npm run build
```

可按修改范围先跑目标 Vitest，完成前再跑全量。

## 父任务最终验证

- 全量 lint / unit / build
- 1000+ 项扫描写入次数自动测试
- 1000 项队列/歌单 DOM 数量或 virtualizer range 测试
- AMLL exact/contains 匹配与旧实现对照测试
- 真机人工项：关闭沉浸页播放 5 分钟，观察 CPU/GPU；重开无白闪

## 最终集成验收结果

- 全量单元测试：17 个测试文件，309 项通过
- `npm run lint`：通过
- `npm run build`：通过
- `git diff --check`：通过
- 真机 CPU/GPU：当前环境无法执行，需人工验收
  1. 播放歌曲并打开沉浸式播放器，记录可见态 CPU/GPU
  2. 关闭沉浸式播放器但保持播放至少 5 分钟，观察是否仍有持续高占用
  3. 重新打开，确认无白闪、半屏、背景重建异常和歌词错位
  4. 验证 Android 返回键、队列 overlay、后台播放和媒体会话无回归

## 完成流程

1. 六个子任务逐个检查、更新 spec、提交、归档。
2. 父任务汇总验证。
3. 更新性能相关规范和 journal。
4. 提交父任务规划/归档记录并归档父任务。

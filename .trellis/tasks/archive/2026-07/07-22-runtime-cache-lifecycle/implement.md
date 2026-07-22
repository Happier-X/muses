# 运行时缓存与监听器治理实现清单

1. [x] 增加共享有界 Map 轻量 LRU helper
2. [x] AMLL TTML 命中缓存与负缓存限制为 256 条
3. [x] 在线封面、在线文本负缓存限制为 256 条
4. [x] 保留 TTL、命中刷新顺序与 reset 语义
5. [x] App 根级 Android back listener 保存 handle 并在卸载时 remove
6. [x] 处理 listener handle 在卸载后才 resolve 的竞态
7. [x] 增加容量淘汰与 App listener 卸载测试
8. [x] 相关单测、lint、build 通过
9. [ ] 提交并归档任务

## 验证结果

- 相关单测：181 passed
- lint：通过
- build：通过

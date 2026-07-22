# 队列线性解析实现清单

1. [x] 建立 `songId -> SongItem` Map 解析器，单次解析只调用一次 `loadSongs()`
2. [x] 让 active queue、peek、next、previous、失败恢复链复用线性解析结果
3. [x] 保持重复 songId 首条命中、缺失歌曲跳过及 currentIndex 语义
4. [x] 新增 1200 首大队列顺序/缺失/重复项正确性测试
5. [x] 运行 `npm run test:unit -- --run tests/unit/player.spec.ts`
6. [x] 运行 `npm run lint`
7. [x] 运行 `npm run build`
8. [ ] 提交并归档任务

## 验证结果

- player 单测：113 passed
- lint：通过
- build：通过
- `git diff --check`：通过

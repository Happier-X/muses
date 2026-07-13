# 实施清单：在线 artist/album 补缺

## 步骤

1. 新建 `src/features/metadata/`：types + match 编排 + 负缓存
2. 实现 providers：至少 **kw / tx** 优先，再补 wy / kg / mg（与 cover 解析字段对齐）
3. `controller.ts`：`onlineTextToken` + `matchOnlineTextMetaForSong`；扫描后触发；合并写回 + sync
4. 单测：`metadata.spec.ts`（补空、不覆盖、全 miss 负缓存、源顺序）
5. spec：features-player / state-management 增加文本补缺约定
6. vitest + lint + vue-tsc

## 启动前

- [x] 决策收敛（A 系列）
- [x] prd / design / implement
- [ ] 用户审阅确认
- [ ] curate jsonl
- [ ] `task.py start`

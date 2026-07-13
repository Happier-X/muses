# 实施清单：弱 title

## 步骤

1. `util.ts`：`isWeakTitle` / `titlesRelated`；扩展 needs / hitFills / merge（merge 签名含 title+path）
2. `types.ts`：`OnlineTextQuery.path?: string`
3. `controller.ts`：query 传 path；upsert title
4. `metadata.spec.ts`：弱可改、强不改、无关 title 不写、artist 回归
5. spec 更新「可改弱 title」
6. vitest + lint + vue-tsc

## 启动前

- [x] 决策 A + B
- [x] prd / design / implement
- [ ] 用户审阅
- [ ] curate jsonl
- [ ] `task.py start`

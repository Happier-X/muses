# 实施清单：+kg 封面回退

## 步骤

1. `types.ts`：`OnlineCoverSource` 增加 `'kg'`
2. 新建 `providers/kg.ts`：song_search_v2 → Image URL（`{size}`→480，http→https，打分）
3. `match.ts`：defaultProviders = [itunes, kw, mg, kg]；注释更新
4. `cover.spec.ts`：kg 命中、前三 miss 后 kg、四源全 miss、顺序
5. spec：features-player / state-management 源顺序含 kg
6. vitest + lint + vue-tsc

## 启动前

- [x] 决策：+kg；iTunes→kw→mg→kg；全链串行
- [x] prd / design / implement
- [ ] 用户审阅确认
- [ ] curate jsonl
- [ ] `task.py start`

# 实施清单：+mg 封面回退

## 步骤

1. `types.ts`：`OnlineCoverSource` 增加 `'mg'`
2. 新建 `providers/mg.ts`：scr_search_tag → cover URL
3. `match.ts`：defaultProviders = [itunes, kw, mg]；注释更新
4. `cover.spec.ts`：mg 命中、双 miss 后 mg、三 miss、顺序
5. spec：features-player / state-management 源顺序含 mg
6. vitest + lint + vue-tsc

## 启动前

- [x] 决策：+mg；iTunes→kw→mg；全链串行
- [x] prd / design / implement
- [ ] 用户审阅确认
- [ ] curate jsonl
- [ ] `task.py start`

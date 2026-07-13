# 实施清单：+tx +wy 封面回退

## 步骤

1. `types.ts`：`OnlineCoverSource` 增加 `'tx' | 'wy'`
2. 新建 `providers/tx.ts`：search_for_qq_cp → albummid → gtimg URL
3. 新建 `providers/wy.ts`：search/get/web → song/detail → picUrl（最多 3 次详情）
4. `match.ts`：defaultProviders 追加 tx、wy；注释更新
5. `cover.spec.ts`：tx/wy 命中、回退、六源 miss、顺序
6. spec + controller 注释：源顺序含 tx/wy
7. vitest + lint + vue-tsc

## 启动前

- [x] 决策：+tx +wy；链尾；全链串行
- [x] prd / design / implement
- [ ] 用户审阅确认
- [ ] curate jsonl
- [ ] `task.py start`

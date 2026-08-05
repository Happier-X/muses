# 实现计划：编辑歌曲信息接入云端元信息

## 顺序

1. **规划收口（本父任务）**
   - [x] PRD D1–D6 + AC
   - [x] design.md
   - [x] 子任务 prd
   - [x] 用户批准最终规划
   - [x] 子任务 design/implement + 分别 `task.py start` 子任务（**不要**用父任务做实现主体）

2. **Child: `08-05-edit-cloud-meta-api`**
   - [x] 扩展/新增 search 多候选 + 强制搜（`src/features/editMeta`）
   - [x] 可调用导出 `searchEditCloudMeta`
   - [x] lint/build + check
   - [x] commit → archive 子任务

3. **Child: `08-05-edit-cloud-meta-ui`**
   - [x] PlayerPage 云端区块
   - [x] 接 API、勾选应用、cache 封面
   - [x] lint/build + AC
   - [ ] commit → archive 子任务

4. **父任务集成**
   - [ ] 对照父 AC1–AC12
   - [ ] spec 更新
   - [ ] archive 父任务 + journal

## 验证

```bash
npm run lint
npm run build
```

真机：获取 → 换候选 → 分字段应用 → 保存 → 再播确认保护字段不被静默覆盖。

## 回滚点

- API 落地后、UI 前：可只保留 API 无入口
- UI 落地后：隐藏按钮或整段回退

## 禁止

- 父任务 in_progress 后直接大改代码而不走子任务（除非用户明确要求单任务实现）
- 改播放 match 补空语义「顺手」变强制覆盖

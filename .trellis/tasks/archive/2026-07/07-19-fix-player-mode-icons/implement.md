# 执行计划

1. 按已确认映射调整 `shuffleIcon`，确认循环图标映射和响应式状态。
2. 增加播放页测试，断言四种状态下按钮 aria-label、图标绑定值和点击后的立即更新。
3. 运行 `npm run lint`、`npm run build`、`npm run test:unit -- --run`、`git diff --check`。
4. 进行独立质量检查，更新必要的前端组件规范。
5. 提交、关闭 GitHub #38 并归档任务。

## 风险点

- 测试不能只断言 aria-label，必须验证图标绑定或渲染结果确实随状态变化。
- 不要把播放模式图标变化误写成队列逻辑变更。

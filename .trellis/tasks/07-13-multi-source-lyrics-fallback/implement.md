# 实施总览（父）

## 顺序

1. 审阅父 prd/design；创建/启动 **lyrics-orchestrator**  
2. 完成编排 + controller + 单测（amll + mock 平台/LRC）  
3. **lyrics-platform-providers**（五源）  
4. **lyrics-lrclib**  
5. 父级全链验收 + spec + archive 子再 archive 父  

## 父级不直接写业务代码

实现落在子任务；父任务跟踪 AC1–AC6。

## 启动前

- [x] 决策收敛  
- [x] 父 prd/design/implement  
- [ ] 用户审阅  
- [ ] 创建子任务并写子 prd  
- [ ] 子任务分别 start  

# LRCLIB 公开 LRC 回退

## Goal

LRCLIB synced LRC 作为在线链第三段（平台之后）。

## 依赖

- orchestrator 契约  
- 建议平台子任务可并行，但挂链需编排已支持多 provider  

## Requirements

1. 合规 User-Agent  
2. 优先 synced lyrics；无则 null（MVP 不用 plain）  
3. 挂入默认链 amll 与平台之后  
4. 单测  

## Out of Scope

- 写库、指纹

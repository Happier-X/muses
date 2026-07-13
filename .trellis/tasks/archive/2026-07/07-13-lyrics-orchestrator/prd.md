# 歌词编排与 controller 接入

## Goal

统一 `matchOnlineLyrics`：第一段 amll；可插拔后续 LyricsProvider；controller 只调编排层。

## 依赖

- 无（最先做）
- 父：`07-13-multi-source-lyrics-fallback`

## Requirements

1. 编排顺序：amll → providers[]（平台+lrclib 由后续子任务注册/注入）  
2. amll 成功短路  
3. controller 用编排结果写 state；**不写库**  
4. 单测：mock 后源验证顺序与短路  

## Out of Scope

- 真实平台/LRCLIB 实现（后两子任务）

# 平台歌词五源

## Goal

实现 kw → tx → wy → kg → mg 搜歌+取词，挂入编排层第二段。

## 依赖

- **须先有** orchestrator 的 LyricsProvider 契约  
- 父任务决策：不写库；参考 any-listen 算法自研/移植（Apache），不引入扩展宿主  

## Requirements

1. 五源均可返回歌词或 null；**逐字优先**（yrc/qrc 等），否则行级 LRC  
2. 单源失败不中断链  
3. 顺序固定 kw→tx→wy→kg→mg  
4. 挂入 `matchOnlineLyrics` 默认 fallback  
5. PlayerPage 解析 yrc/qrc（AMLL lyric 包）  
6. 单测 + 尽量不依赖实网 flaky  
7. 不写库  

## Out of Scope

- amll、LRCLIB、写库

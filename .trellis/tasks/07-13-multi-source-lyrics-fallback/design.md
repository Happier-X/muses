# 设计：多源歌词回退（父）

## 架构

```
controller.matchOnlineLyricsForSong
  → matchOnlineLyrics(query)   // features/lyrics 新编排
       1. amllTtmlDb.matchAmllTtmlLyrics
       2. for platform in [kw,tx,wy,kg,mg]: search → getLyric
       3. lrclib.searchSyncedLrc
  → 写 playerState only
```

## 统一结果类型（示意）

```ts
type OnlineLyricsHit = {
  ok: true
  text: string
  format: 'ttml' | 'lrc'
  source: 'amll' | 'kw' | 'tx' | 'wy' | 'kg' | 'mg' | 'lrclib'
}
type OnlineLyricsMiss = { ok: false; reason: 'no-match' | 'network' | 'parse' }
```

## Provider 契约

```ts
type LyricsProvider = {
  id: string
  searchLyrics(query: {
    songId: string
    title: string
    artist?: string
    album?: string
    duration?: number
  }): Promise<{ text: string; format: 'lrc' | 'ttml' } | null>
}
```

- amll 适配为 format=`ttml`  
- 平台/LRCLIB 为 `lrc`（逐字若取到可降级为带时间轴 LRC 或行级 LRC，子任务定）  
- 空文本 / 纯空行视为 null  

## 平台实现要点

- **不可** `npm` 直用 any-listen 扩展；Apache 可参考算法，**替换 hostApi**  
- 复用现有 `httpGetJson` / 搜索解析经验（cover/metadata）  
- wy 等 eapi：子任务内评估最小加密移植，失败则 catch 下一源  
- 负缓存：按 songId+queryKey，段级或整链级（子任务 orchestrator 定）  

## LRCLIB

- 公开 API + 明确 User-Agent  
- 优先 synced lyrics；无则 plain 是否采用：子任务默认 **仅 synced**（避免无时间轴）  

## Controller

- 替换直接 `matchAmllTtmlLyrics` 为 `matchOnlineLyrics`  
- 成功：`state.lyrics` / `lyricsFormat` / `onlineLyricsStatus=ready`  
- 失败：回退本地 LRC 逻辑保持  

## 与封面/文本

- 并行、独立 token；歌词不写库 vs 封面/文本可写库 — 文档写清  

## 子任务边界

| 子 | 可测交付 |
|----|----------|
| orchestrator | 注入 mock provider 测顺序/短路/回退 |
| platform | 各源 unit + 串行 |
| lrclib | unit + 挂链 |

## 回滚

- controller 改回仅 amll；删除新 provider

# 设计：歌词编排

## 模块

```
src/features/lyrics/
  match.ts          # matchOnlineLyrics 编排
  providers/types.ts # LyricsProvider 契约
  amllTtmlDb.ts     # 已有，第一段
  index.ts          # 导出
```

## 流程

1. 校验 title/songId  
2. amll → ok 则 `{ text: ttml, format: 'ttml', source: 'amll' }`  
3. 否则 for provider in fallbackProviders：searchLyrics，首个非空返回  
4. 全失败：汇总 reason（network 优先于 no-match）

## controller

`matchOnlineLyricsForSong` 改调 `matchOnlineLyrics`；成功写 state；失败回退本地。

# 设计：平台歌词五源 + 逐字优先

## 优先级（源内）

1. 逐字原文（wy `yrc` / tx `qrc` / kg `krc` 等），若 AMLL `@applemusic-like-lyrics/lyric` 可解析  
2. 普通 LRC  
3. null  

## 展示

扩展 `lyricsFormat`：`ttml | lrc | yrc | qrc`  
`PlayerPage` 用 `parseTTML` / `parseLrc` / `parseYrc` / `parseQrc`。

## 源顺序

kw → tx → wy → kg → mg（挂到 `matchOnlineLyrics` fallback）

## 实现策略

- 复用现有搜索字段（与 cover/metadata 同源接口）  
- 取词用较稳公开接口；加密逐字（eapi yrc、qrc 解密、krc）能拿则拿，失败降级 LRC  
- 不写库  

## 与 any-listen

参考算法，独立实现；不引入扩展宿主。

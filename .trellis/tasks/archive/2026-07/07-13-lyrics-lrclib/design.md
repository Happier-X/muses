# 设计：LRCLIB 公开 LRC

## 位置

在线链第三段：amll → 平台五源 → **LRCLIB** → 本地 LRC。

## API

- 优先：`GET https://lrclib.net/api/get?track_name=&artist_name=&album_name=&duration=`  
  （有 duration 秒时精确匹配）
- 回退：`GET https://lrclib.net/api/search?q=` 或带 `track_name`/`artist_name` 参数
- 仅使用 **`syncedLyrics`**（带时间轴 LRC）；无则 null（MVP 不用 plainLyrics）

## 合规

- 固定 `User-Agent: Muses/<version> (https://github.com/happier/muses)`（或项目真实仓库 URL）
- 不写库

## 挂载

`defaultFallbackProviders = [...platformLyricsProviders, lrclibLyricsProvider]`

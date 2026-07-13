# 设计：YRC / QRC 逐字

## tx

1. 搜索保留 `search_for_qq_cp`，同时取 `songid` + `songmid`。  
2. 有 `songid` → POST `https://u.y.qq.com/cgi-bin/musicu.fcg`  
   `GetPlayLyricInfo`（`qrc:1`, `crypt:1`）。  
3. `data.lyric` 为 hex → `decryptQrcHex` → XML → 提取 `LyricContent="..."` → 明文 QRC。  
4. 成功：`{ text, format: 'qrc' }`。  
5. 失败：songmid + `fcg_query_lyric_new` LRC（现状）。

## wy

1. 搜索不变。  
2. 优先 eapi：`POST interface3.music.163.com/eapi/song/lyric/v1`  
   body 加密：`eapiKey = e82ckenh8dichen8`，AES-128-ECB + MD5 拼参（与 NeteaseCloudMusicApi / any-listen 同思路，自研小工具，不引宿主）。  
3. 响应有 `yrc.lyric` 且像 yrc → `format: 'yrc'`。  
4. 否则 `lrc.lyric` → `format: 'lrc'`；再失败则公开 GET `/api/song/lyric` 兜底。

## HTTP

- 新增 `httpPostJson` / `httpPostText`（CapacitorHttp.post，业务 4xx 不回退 fetch 双请求，与 get 一致）。

## 展示

已有 PlayerPage `parseYrc` / `parseQrc`，无需改 UI。

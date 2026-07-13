# 网易 / QQ 平台逐字歌词

## Goal

在已有多源歌词链上，让 **网易云 YRC** 与 **QQ 音乐 QRC** 真正以逐字格式进入播放器；展示层沿用 AMLL `parseYrc` / `parseQrc` / `decryptQrcHex`。  
krc / mrc / 酷我 lyricx **不做**（AMLL 不支持，保持行级 LRC）。

## 背景

- AMLL 文档：仅原生支持 yrc / qrc 解析；QQ 分发加密串可用 `decryptQrcHex`。  
- 现状：PlayerPage 已能解析 yrc/qrc；tx 多为行级 LRC；wy 公开接口常无 yrc。

## Requirements

1. **R1（tx）** 有 songid 时优先 `GetPlayLyricInfo` → 加密 lyric → `decryptQrcHex` → 提取 `LyricContent` → `format: 'qrc'`；失败降级现有 `fcg_query_lyric_new` LRC。  
2. **R2（wy）** 优先拿到明文 yrc（可用 eapi `/api/song/lyric/v1`），`format: 'yrc'`；无则公开 API LRC。  
3. **R3** 不写库；不改 kw/kg/mg 逐字策略。  
4. **R4** 单测：mock 解密/取词路径；不依赖实网 flaky。  
5. **R5** lint / tsc / 相关单测通过；spec 补充 yrc/qrc 取词说明。

## Acceptance Criteria

- [ ] AC1：tx 命中 qrc 时 `format === 'qrc'` 且文本可被 `parseQrc` 解析出逐字行。  
- [ ] AC2：tx 解密失败时仍可返回 LRC。  
- [ ] AC3：wy 有 yrc 时 `format === 'yrc'`；仅有 lrc 时 `format === 'lrc'`。  
- [ ] AC4：在线结果不写 `muses:songs`。  
- [ ] AC5：测试与类型检查通过。

## Out of Scope

- 酷狗 krc / 咪咕 mrc / 酷我 lyricx 解密  
- 翻译 / 罗马音合并进 UI  
- 写库、扩展宿主

## Task Type

Complex（轻量 design + implement）

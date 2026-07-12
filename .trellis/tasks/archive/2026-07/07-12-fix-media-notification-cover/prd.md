# 修复切歌时媒体通知封面不更新

## Goal

切换歌曲时，Android 媒体通知栏的封面应同步切换为当前歌曲封面；无封面时应清除旧封面，避免残留上一首图片。

## Issue

- GitHub Issue #1：`歌曲切换的时候，媒体通知栏的封面没有更新`
- 正文为空；按标题定位为「切歌后通知栏封面不刷新」。

## Background / Confirmed Facts

封面链路：

1. `playSong` → `syncMediaSessionSong` → `updateMediaSessionMetadata`
2. 两段式 metadata：先 `setMetadata({ title/artist/album, artwork: [] })`，再 `prepareArtworkDataUrl(file://)` → 二次 `setMetadata` 带 `data:` artwork
3. 原生桥接：`AudioPlayerPlugin.prepareArtworkDataUrl` 用 `ContentResolver.openInputStream` 读封面并转 JPEG base64
4. 懒扫描：`scanSongMetadata` 可能在开播后补全 `coverUri`，只调用 `syncDisplayStateFromSong`，**不会**再调 `syncMediaSessionSong`

### 根因（代码已证实）

**R-A 空 artwork 不清除旧图（高）**  
`@capgo/capacitor-media-session` Android `MediaSessionPlugin.setMetadata`：仅当 `artwork[].src` 非空时才 `artwork = urlToBitmap(src)`。传入 `artwork: []` 时循环不执行，**插件字段 `artwork` 保留上一首 Bitmap**。  
因此：

- 切到无封面歌曲 → 标题更新，封面仍是上一首
- 切到有封面但 `prepareArtworkDataUrl` 失败 → 同样残留上一首

**R-B 懒扫描补封面后未同步媒体会话（高）**  
`scanSongMetadata` 成功后只 `syncDisplayStateFromSong` 更新 UI 的 `coverUri`，不调用 `syncMediaSessionSong`。  
若开播时尚无封面、稍后扫描得到封面，通知栏永远拿不到新封面；快速切歌后若依赖扫描结果，也会表现为「封面不更新」。

**R-C `file://` 封面读取脆弱（中）**  
`prepareArtworkDataUrl` 统一走 `ContentResolver.openInputStream`。缓存封面实际是 `Uri.fromFile` 写出的 `file://.../cache/covers/*.jpg`。部分机型/版本上对 `file://` 打开可能失败并静默返回 `null`，触发 R-A 残留。

## Requirements

1. **R1** 从歌曲 A（有封面）切到歌曲 B（有不同封面）时，通知 metadata 的 artwork 必须更新为 B 的封面（二次 `setMetadata` 成功路径）。
2. **R2** 从有封面歌曲切到无封面歌曲时，通知栏不得继续显示上一首封面（必须主动清除或替换为占位清空）。
3. **R3** 开播后懒扫描补全 `coverUri` 时，必须再次同步媒体会话 metadata（含封面）。
4. **R4** `prepareArtworkDataUrl` 对项目内 `file://` 缓存封面应能可靠读出；失败时不得留下上一首封面。
5. **R5** 保留现有两段式更新与 `metadataToken` 防串封面；快速连续切歌不得把旧封面写回新歌。
6. **R6** 补充/强化单元测试覆盖切歌与懒扫描后的 `MediaSession.setMetadata` 调用序列。
7. **R7** 同步 `.trellis/spec/frontend/features-player.md` 中关于封面更新/清空的约定。

## Acceptance Criteria

- [ ] AC1：连续 `playSong(A)` → `playSong(B)`（均有不同 `coverUri`）时，最终一次带 artwork 的 `setMetadata` 使用 B 的封面 data URL（或等价断言 prepare 入参为 B 的 uri）。
- [ ] AC2：`playSong(有封面)` → `playSong(无封面)` 后，媒体会话不得保留上一首 artwork（清空调用可验证：显式空图/透明占位或插件可识别的清除约定）。
- [ ] AC3：`playSong` 后懒扫描写入新 `coverUri` 会触发再次 `updateMediaSessionMetadata` / `setMetadata`。
- [ ] AC4：快速切歌时过期 token 仍丢弃旧封面回调，不回写。
- [ ] AC5：相关单测通过；lint / type-check 通过。
- [ ] AC6：`features-player.md` 写明「空 artwork 数组不能清封面，必须显式替换；懒扫描后需 re-sync」。

## Out of Scope

- 不替换 `@capgo/capacitor-media-session` 为其他通知方案。
- 不修改 `node_modules` 插件源码（用调用侧规避）。
- 不改 UI 内封面展示逻辑（MiniPlayer / PlayerPage）。
- 不处理锁屏以外的第三方车机/蓝牙设备特殊协议。

## Technical Notes

推荐修复方向（实现阶段可微调）：

1. **`updateMediaSessionMetadata`**  
   - 无 `coverUri` 或 prepare 失败：用 1×1 透明/中性 JPEG 的 `data:` 强制覆盖旧 Bitmap，而不是 `artwork: []`。  
   - 有 `coverUri`：可先强制清空再写入新图，或直接二次写入新图；失败时仍走强制清空。  
2. **`scanSongMetadata` / `syncDisplayStateFromSong`**  
   - 当当前歌曲 `coverUri`（或 title/artist/album）变化且仍是当前曲时，调用 `syncMediaSessionSong`。  
3. **`prepareArtworkDataUrl`（Kotlin）**  
   - `file://` 优先 `FileInputStream`；`content://` 继续 `ContentResolver`。  
4. **测试**  
   - mock `MediaSession.setMetadata` + `AudioPlayerBridge.prepareArtworkDataUrl`，断言切歌/无封面/懒扫描序列。

## Task Type

轻量到中等 bugfix：以 `prd.md` 为主；若实现涉及多文件联动可在 start 前补极简 `implement.md`，否则 PRD-only 可进入实现。

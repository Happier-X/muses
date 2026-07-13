# 在线匹配歌曲元信息（封面 MVP）

## Goal

为本地/WebDAV 曲库中**缺少封面**的歌曲，在播放时自动在线匹配封面：下载到 app 私有缓存并以安全 `coverUri` 写回曲库，提升列表、沉浸式页与媒体通知的展示质量。

参考 any-listen「在线匹配歌曲信息」的产品能力与「搜索 → 取图 URL」流程；**不**引入扩展商店、music-tag-web 服务或 any-listen 扩展宿主。

## Issue

- GitHub #18：`增加匹配歌曲封面、歌手、艺术家、歌词等歌曲元信息的功能，可以参考 any-listen 的在线匹配歌曲信息功能`

## Confirmed Facts

### 本仓库

- `SongItem.coverUri`：仅安全 URI（禁 `data:`/base64）；内嵌封面已由 `AudioMetadataReader.writeCover` 落到 `cache/covers/{sha}.jpg`。
- 播放后 `scanSongMetadata` 懒扫本地标签并可 `upsertSong`。
- 在线歌词：amll-ttml-db，运行时不写回 `SongItem`；本任务不改歌词链路。
- 媒体通知：`prepareArtworkDataUrl` 将 `file://` 封面转 `data:`。

### 外部方案结论

- any-listen 扩展 / music-tag-web：**不可**直接当 SDK 装入 Capacitor。
- MVP 采用 **iTunes 主源 + 酷我 (kw) 回退**；kw 逻辑移植自 any-listen-extension-online-metadata（Apache-2.0），替换宿主 `request` 为我们的 HTTP。

## Decisions

| 决策 | 结论 |
|------|------|
| MVP 字段 | **仅封面**（`coverUri`） |
| 数据源 | **混合 C**：iTunes Search → 未命中则 kw 酷我；结构预留多源 |
| 触发 | **播放时自动**：本地标签扫描后若仍无安全 `coverUri`，异步匹配当前曲 |
| 优先级 / 写回 | **仅补缺 + 写回曲库**；已有封面不请求、不覆盖；命中则缓存文件 URI + `upsertSong` |
| 歌词 | 不动 amll；不在线匹配歌词 |
| 扩展/服务 | 不引入 any-listen 运行时、不部署 music-tag-web |

## Requirements

1. **R1** 播放成功后，若当前曲无安全 `coverUri`，在本地标签扫描结束后（或已扫描仍无封面时）异步发起在线封面匹配。
2. **R2** 匹配查询使用 `title` + `artist`（可选 `album`）；不得阻塞音频播放。
3. **R3** 源顺序：iTunes Search 先；未得到可用 HTTP(S) 封面 URL 时再尝试 kw；任一源成功即停止。
4. **R4** 下载封面到 app 私有缓存（与现有 `cache/covers` 一致或等价），得到 `file://`（或平台安全 URI），**禁止**把 `data:`/base64/远程 URL 写入 `muses:songs`。
5. **R5** 成功：`upsertSong` 更新 `coverUri`，并 `syncDisplayStateFromSong` + 媒体会话封面刷新；仅当仍是当前曲时应用。
6. **R6** 已有安全 `coverUri` 时不发起在线匹配；不覆盖本地/内嵌封面。
7. **R7** 失败/无命中/超时/切歌：静默；负缓存避免同曲短时间反复请求。
8. **R8** 切歌用 token 丢弃过期结果，禁止上一首封面串到当前曲。
9. **R9** 网络错误不弹阻塞播放错误；不影响 `playSong` 状态机。
10. **R10** 单测：补缺触发、已有封面跳过、iTunes 命中、iTunes miss 回退 kw、双 miss、token 防串、不写 base64。
11. **R11** 同步 frontend spec（state-management / features-player 或 library 相关）。

## Acceptance Criteria

- [ ] AC1：无封面歌曲播放后，能自动匹配并展示封面（iTunes 或 kw 至少一路可达时）。
- [ ] AC2：已有 `coverUri` 的歌曲不发起在线匹配、封面不变。
- [ ] AC3：写回 `muses:songs` 的 `coverUri` 为本地缓存安全 URI，非 `data:`/base64/裸远程 URL。
- [ ] AC4：iTunes 失败/无结果时尝试 kw；kw 成功则同样写回与展示。
- [ ] AC5：快速切歌时过期匹配不覆盖当前曲封面。
- [ ] AC6：匹配失败静默，播放与歌词链路不受影响。
- [ ] AC7：测试 / lint / type-check 通过；spec 已同步。

## Out of Scope

- 歌手/专辑/标题文本在线改写
- 在线歌词（已有 amll）
- 批量整库刮削
- 手动「匹配封面」按钮（可后续）
- 五源全上 / 扩展商店 / music-tag-web 旁路服务
- 覆盖已有本地封面
- 修改 capgo 插件源码

## Task Type

Complex

# PRD：修复刮削回写 WebDAV 目标地址错误

## 目标

刮削写回 WebDAV 歌曲时使用正确的文件地址，修复「永远报下载失败」的根因。

## 根因（代码勘察确认）

`src/features/scrape/writeback.ts` `writeWebDavFile`：

```ts
const webdavSource = sources.find((s) => s.type === 'webdav')  // ← 第一个 WebDAV 音源
return writeWebDavAudioMetadata({ url: webdavSource.serverUrl, ... })  // ← 服务器根地址，无文件路径
```

- **Bug 1（致命）**：`url` 只有 `serverUrl` 根地址，缺文件路径。原生层对目录地址做 GET 下载必然失败 → 「下载 WebDAV 音频失败」，与网络无关，重试无效
- **Bug 2**：取的是列表里第一个 WebDAV 音源而非歌曲所属音源（`song.sourceId`），多 WebDAV 音源时读写目标不一致

对照正确实现：读取链路 `readWebDavAudioTags` 用 `buildWebDavUrl(source.serverUrl, file.path)`（src/features/sources/webdav.ts:131，含 encodePath）。

## 需求

1. `writeWebDavFile` 改为按 `song.sourceId` 在 sources 中查找歌曲自己的 WebDAV 音源；找不到或该音源非 WebDAV 时返回 `{ ok:false, code:'no_password', message:'未找到歌曲所属的 WebDAV 音源，请重新扫描后重试。' }`（沿用既有 auth 类 code，前端文案映射已覆盖）
2. URL 构造改为 `buildWebDavUrl(webdavSource.serverUrl, song.path)`
3. 密码获取按该音源的 credentialKey（现有逻辑保留）

## 验收标准

1. 单测：writeWebDavFile 抽出可测的地址构造逻辑（或集成测 mock）断言 url = serverUrl + encoded song.path、按 sourceId 选源、找不到源返回明确错误
2. MuMu 真机：对之前失败的 WebDAV 歌曲重试刮削回写成功（fileOk），行状态 ✓
3. lint / test:unit / build 全过（禁止管道吞退出码）
4. 回归：本地文件回写路径不受影响

## 范围外

- 不改原生插件、不改读链路

## 约束

- 遵循 features-scrape.md 写回契约（journal/入库语义不变）

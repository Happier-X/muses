# PRD：修复 WebDAV 写回工作副本扩展名误判

## 目标

消除 WebDAV 回写时「当前格式暂不支持写入标签」的必现失败：工作副本文件名保留真实扩展名。

## 根因（代码勘察确认）

- `WebDavPlugin.kt` writeMetadata：`workFile = File(parent, "${cachedFile.name}.write-tmp")`，缓存名 `<sha256>.mp3` → 工作副本名 `<sha256>.mp3.write-tmp`
- `AudioMetadataWriter.isLikelySupportedExtension` 按最后一个点取扩展名 = `write-tmp` ∉ SUPPORTED_EXTENSIONS → 必抛 `unsupported_format`「当前格式暂不支持写入标签」
- 影响所有 WebDAV 回写，与真实格式无关，100% 必现

## 需求

1. **R1（主修）**：工作副本命名为 `<nameWithoutExtension>.write-tmp.<extension>`（如 `<sha256>.write-tmp.mp3`），扩展名为空时回退 `.audio`（isLikelySupportedExtension 对空/audio 放行尝试）
2. **R2（兜底）**：`isLikelySupportedExtension` 在判断前剥离已知临时后缀（`.write-tmp`/`.tmp`/`.partial`），防止其他调用点再踩同类坑
3. PUT 阶段读取 workFile 字节上传的逻辑不变（内容不受命名影响）

## 验收标准

1. gradle assembleDebug 通过；前端 lint/test/build 不回归（本次无前端改动则跑一次确认即可）
2. MuMu 真机：对之前失败的 WebDAV 歌曲重试刮削回写成功（行状态 ✓）
3. 回归：本地文件写标签路径不受影响（guessExtension 逻辑未动）

## 范围外

- 不改 AudioMetadataWriter 其他逻辑、不改缓存命名 cacheFile()

## 约束

- Kotlin 改动仅限 WebDavPlugin.kt / AudioMetadataWriter.kt（isLikelySupportedExtension）

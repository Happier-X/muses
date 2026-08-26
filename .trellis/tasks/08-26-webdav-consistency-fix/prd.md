# WebDAV 一致性修复（轻量任务）

## 缺陷 1：缓存 LRU 淘汰顺序退化
`DiskWebDavAudioCache.trimToLimit` 与删除路径经 `metaFile(urlFromCacheFile(file))`
对 sha256 前缀**二次哈希**，meta 文件名永远错配 → lastAccess 恒 0 → LRU 退化为任意序。
修复：同名前缀 + `.meta` 后缀直接关联；移除无法反推 URL 的 `urlFromCacheFile`。

## 缺陷 2：Basic 编码双轨不一致
`OkHttpWebDavClient.authenticate` 用 OkHttp 默认 ISO-8859-1，`WebDavAuthRegistry` 用 UTF-8，
非 ASCII 用户名两条认证链产生不同 Authorization 头。
修复：authenticate 显式 `Charsets.UTF_8`。

## Acceptance Criteria
- [ ] `:core:webdav:testDebugUnitTest` 全绿（既有 meta 关联测试通过）
- [ ] 编译无 urlFromCacheFile 残留引用

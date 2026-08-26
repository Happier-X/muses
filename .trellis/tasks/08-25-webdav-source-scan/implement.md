# 执行清单 — WebDAV 音源扫描

> 验证命令统一：`cd native && JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew <targets>`

- [x] 1. core:media 依赖 core:webdav（build.gradle.kts）
- [x] 2. 抽 `CoverCacheWriter`（core:media scanner 包）：writeCoverCache + sha256 从 LocalLibraryScanner 迁出共用，LocalLibraryScanner 改引用，行为不变
- [x] 3. 新增 `WebDavLibraryScanner`：BFS 发现 → 串行处理（缓存命中/下载+预热/读标签/sidecar lrc）→ 产出 List<Song>；进度写自有 StateFlow<ScanProgress>；密码缺失抛 IllegalStateException(「WebDAV 密码不存在，请重新添加该音源。」)；附带把 WebDavAudioCache 抽成接口 + DiskWebDavAudioCache 实现（测试注入 fake 需要，Hilt @Binds 已接）
- [x] 4. SourcesViewModel：删 WebDAV 拦截；startScan 按 type 分支取流/调扫描（两扫描器无公共接口，直接分支最简）
- [x] 5. 单测：WebDavLibraryScannerTest 5 例（扩展名过滤+递归 / readTags=false 零下载 / 读标签失败降级不中断 / 缓存命中零下载 / 密码缺失抛错且进度置终态）
- [x] 6. 门禁：`:core:media:testDebugUnitTest :core:webdav:testDebugUnitTest :feature:sources:assembleDebug :app:assembleDebug` 全绿（34 tests），`:core:media:lintDebug :feature:sources:lintDebug` 通过
- [ ] 7. MuMu 手工验收：WebDAV 源 readTags 关→快速建库文件名歌；开→标签/封面正确；断网中途扫→失败态文案

### 增量：WebDAV 歌曲播放接线 🔄 进行中（2026-08-25 用户反馈无法播放）

- [x] `WebDavAuthRegistry`（core:webdav）：内存凭据表 baseUrl→(user,pass)，全量加载 SourceRepository+CredentialsRepository，最长前缀匹配出 authHeader；core:webdav 新增依赖 core:data（方向合法无环）
- [x] PlaybackService 改用注入的 OkHttpClient + 认证 Interceptor（interceptor 不覆盖请求已带的 Authorization，避免与扫描器 client.authenticate 手动 header 冲突）；顺带删除死代码 buildWebDavMediaItem
- [x] PlayerConnection.play()：WEBDAV 曲目先查 WebDavAudioCache 命中→file://，未命中走 HTTP URL（由 interceptor 认证）；顺带修正引用不存在 setWebDavAuthorization 的 KDoc
- [x] 源增删改后调 registry.refresh()（SourcesViewModel save/update/delete + OnboardingViewModel 保存）
- [x] 单测 WebDavAuthRegistryTest 5 例（最长前缀 / 无匹配 null / refresh 生效 / null user Basic 编码 / '/' 边界不误命中），门禁 :app:assembleMusesDebug + :core:media:test + :core:webdav:test 全绿（共 62 tests, 0 失败）
- [x] 引导页 saveWebdavSource 补齐 username 入库 + 密码 Keystore 存储（对齐音源页流程，消除引导创建源必然认证失败的缺口）
- [ ] MuMu 验证：WebDAV 歌曲可播放（需用户重建含用户名的源）

### 主会话复查修正（子代理产出后的四处修复）

1. `DiskWebDavAudioCache.putToCache` override 带默认参数值 → 编译错；默认值收口到接口声明
2. `WebDavLibraryScanner` 引用不存在的 `isSupportedAudioFile` → 改为 `isSupportedAudio`
3. `getPassword` 在 try 块外，密码缺失抛错时进度未置终态（UI 卡「正在查找文件」）→ 移入 try
4. 测试三处：fake 子目录文件 URL 拼错父路径、JUnit 三参 assertEquals 语序、URL 编码期望过度指定

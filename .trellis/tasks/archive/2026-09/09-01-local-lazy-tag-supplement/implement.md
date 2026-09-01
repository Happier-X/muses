# 实施计划 — 本地扫描未勾选标签时像 WebDAV 一样懒补充

## 前置检查

- [ ] `prd.md` / `design.md` 已评审
- [ ] 分支基于 `main` 最新，无未提交脏改

## 步骤

### 1. 代码修改

- [ ] 1.1 `LocalLibraryScanner.kt`
  - 新增 `companion object { const val FILENAME_TAGS_VERSION = 0 }`（与 WebDav 侧同值）
  - `scan()` 组装 Song 处：`tagsVersion = if (readTags) TAGS_VERSION else FILENAME_TAGS_VERSION`
  - 确保 `coverUri` 等 lazy 字段在 false 分支为 null（已是）

- [ ] 1.2 `AudioTagReader.kt`
  - `resolveFile(source)` 扩展分支：`content://` / `file://` / `http(s)` / 其它
  - 新增 `private fun copyContentUriToCache(uriString: String): File`：`contentResolver.openInputStream(Uri.parse(...))` → `cacheDir/audio_tags/content_${hash}.tmp`
  - `parseTags` / `readTagForUpdate` / `extractCover` 复用新 `resolveFile`
  - 异常路径保持返回 null，不抛上层

- [ ] 1.3 （可选/校验）`PlaybackService.kt` 确认 `tagsVersion < LocalLibraryScanner.TAGS_VERSION` 分支对本地 `content://` 生效，日志 tag 保持 `PlaybackLazyScan`

### 2. 本地验证

- [ ] 2.1 编译：`JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug`
- [ ] 2.2 单测：`./gradlew testDebugUnitTest`（关注 `WebDavLibraryScannerTest`、`TagReaderTest` 回归）
- [ ] 2.3 lint：`./gradlew :app:lintMusesDebug`
- [ ] 2.4 真机/模拟器手工：
  - 添加本地文件夹 → 扫描设置关闭“读取音乐标签” → 扫描完成查 DB/lib 列表为文件名占位且 `tagsVersion=0`
  - 播放其中一首 → 观察列表即时刷新为真实标题/歌手/封面，DB `tagsVersion=1`
  - 再次扫描关闭态的同一源，仍为 0，播放再补；开启态扫描立即为 1
  - WebDAV 源回归：文件名建库 → 播放补齐仍正常

### 3. 文档与守卫

- [ ] 3.1 更新 `.trellis/spec/android/features-webdav-library.md`：在“标签读取只在播放时懒扫描”段落补充本地 `readTags=false` 时同为 `tagsVersion=0` 入懒链路的说明
- [ ] 3.2 自检 `core:media` → `core:data:tag` 依赖合法（feature 不触 Room/OkHttp）

## 验证命令

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug :app:lintMusesDebug testDebugUnitTest
```

## 回滚点

- 若手工验证本地 `content://` 懒读失败率偏高，回退 `AudioTagReader` 新增分支，保留 `LocalLibraryScanner` 置 0 逻辑可独立回退为恒 1

## 产出

- 代码：`LocalLibraryScanner.kt`、`AudioTagReader.kt`
- 文档：spec 增量说明
- 产物：`app-muses-debug.apk` 可装机验证

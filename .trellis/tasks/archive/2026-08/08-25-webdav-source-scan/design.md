# 技术设计 — WebDAV 音源扫描

## 数据流

```
SourcesScreen「扫描」→ SourcesViewModel.startScan
  → WebDavLibraryScanner.scan(source, readTags)   // core:media，挂起函数，进度写 StateFlow
      1. credentialsRepository.getPassword(source.id)  // 缺失 → throw（UI 显示失败态）
      2. BFS list(baseUrl + path)：WebDavItem(name,url,isDirectory,eTag)
         音频扩展名过滤复用 LocalLibraryScanner.isSupportedAudioFile
         发现阶段进度：currentFile=当前目录, total=0（UI 已映射「正在查找文件」）
      3. processing：逐文件
         - cached = webDavAudioCache.getCachedFile(url)
         - 未命中且 readTags=true → client.get(url, tempFile) → putToCache(url, file, eTag)（预热）
         - readTags=true：TagReader.read(file) + sidecar .lrc（client.getString(同名.lrc URL)，仅当内嵌歌词为空）
           失败 → 降级文件名，继续
         - readTags=false：零下载，标题=displayName 去扩展名
         - Song(id=stableSongId(sourceId,url), path=url, sourceType=WEBDAV, ...)
      4. songRepository.replaceSourceSongs(sourceId, songs)  // ViewModel 调，scanner 不碰库
```

## 模块与依赖变更

| 模块 | 变更 |
|---|---|
| `core/media/build.gradle.kts` | + `implementation(project(":core:webdav"))` |
| `core/media/.../scanner/WebDavLibraryScanner.kt` | 新增 @Singleton，构造注入 WebDavClient/WebDavAudioCache/CredentialsRepository(@Lazy? 经 Provider 避免循环——见下) |
| `LocalLibraryScanner.kt` | 抽 `CoverCacheWriter` 小组件（writeCoverCache+sha256）供两个 scanner 共用；`isSupportedAudioFile`/`stableSongId` 已是 companion public 直接复用 |
| `feature/sources/.../SourcesViewModel.kt` | 移除 WebDAV「暂未支持」拦截；按 source.type 分派 scanner；scanReadTags 默认值已对齐 |

**循环依赖风险**：core:data 的 CredentialsRepository 被 core:media 引用——core/media 已依赖 core:data（现状），无环。

## 进度模型

复用既有 `ScanProgress(current,total,currentFile,finished)` 不扩字段：
- discovering：total=0, currentFile=正在列的目录（UI 现有映射 total==0&&!finished→「正在查找文件」天然成立）
- processing：current/total/currentFile=文件名
- 终态：finished=true

## Song 字段映射（对齐 Web SongItem）

- id：`LocalLibraryScanner.stableSongId(sourceId, url)`
- path：完整文件 HTTP URL（播放预留）；title 回退链：tag→文件名去扩展名
- durationSec/durationMs：readTags=false 时 0（Web 同样缺 duration）；后续懒扫补
- lyrics+lyricsSource=EMBEDDED/sidecar→LyricsSource 枚举值核对后写入；coverUri 经 CoverCacheWriter 落盘
- tagsVersion：沿用 TAGS_VERSION

## 权衡

- **串行下载**：对齐 Web；大库首次 readTags 扫描慢是已知取舍（默认关读标签缓解）
- **整文件下载 vs Range 读头**：旧 Capacitor 插件即整文件下载进缓存，复用该策略换播放预热收益；Range 解析 ID3/Vorbis 复杂度过高不做
- **sidecar .lrc 仅在无内嵌歌词时请求**：省一次网络往返（Web 同语义）

## 回滚

单 commit 交付；回滚 revert 即可，无 schema/数据迁移。

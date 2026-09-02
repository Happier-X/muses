# 修复 FLAC 刮削回传 ImageIO 崩溃

## Goal

修复刮削回写封面时 `StandardArtwork` 触发 `javax.imageio.ImageIO` 导致 `NoClassDefFoundError` 崩溃，FLAC 及其他容器在 Android 端可稳定写入封面，且任何写入异常均折叠为 `write_failed` 不崩溃。

## 背景

- 崩溃栈：`StandardArtwork.getImage() -> setImageFromData() -> FlacTag.createField() -> TagWriter.applyRequest()`，`javax.imageio.ImageIO` 在 Android 运行时不存在
- 根因1：`ArtworkFactory.getNew()` 依赖 `TagOptionSingleton.isAndroid`，未显式设置为 true 时返回 `StandardArtwork`
- 根因2：即便返回 `AndroidArtwork`，其 `setImageFromData()` 直接抛 `UnsupportedOperationException`，`FlacTag.createField()` 对 FLAC 仍会失败
- 根因3：`TagWriter.write()` 仅 `catch (Exception)`，`NoClassDefFoundError`（`Error`）未被捕获

## Requirements

### R1 - Android 标志初始化
- 在 `MusesApplication.onCreate()`（`super.onCreate()` 之后、`CrashHandler.install` 之前或之后均可）显式 `TagOptionSingleton.getInstance().setAndroid(true)`，确保全进程 `ArtworkFactory` 走 Android 分支
- 不依赖系统属性探测

### R2 - TagWriter 封面写入去 ImageIO 化
- `TagWriter.createArtwork(bytes)` 不再使用 `ArtworkFactory.getNew()`
- 提供 Android 安全的 `Artwork` 实现：
  - 持有 `binaryData / mimeType / description / pictureType / width / height / isLinked / imageUrl`
  - `setImageFromData()` 不触碰 `javax.imageio`/`java.awt`，优先用 `android.graphics.BitmapFactory.decodeByteArray` 解析宽高，失败则置 0×0 并返回 `true`
  - `getImage()` 抛 `UnsupportedOperationException`（与 `AndroidArtwork` 一致，但不再被 `FlacTag` 调用路径触发）
  - `setFromFile` 等按需实现或抛 `UnsupportedOperationException` 保持接口完整
- FLAC `FlacTag.createField(artwork)` 依赖 `setImageFromData()==true` 分支可正常构造 `MetadataBlockDataPicture`，不再抛异常

### R3 - 写入异常兜底不崩溃
- `TagWriter.write()` 的 `catch` 扩大为 `Throwable`，并对 `CancellationException` 原样重抛（协程取消语义）
- 任何 `Error`（含 `NoClassDefFoundError`、`ExceptionInInitializerError`）均折叠为 `WriteResult.failure("write_failed", message)`
- `SongFileWriters` 的 WebDAV 路径已有 `withContext(Dispatchers.IO) { TagWriter.write }`，无需额外改，但需验证错误码透传

### R4 - 回归与兼容
- 现有 `TagWriterTest` 5 项用例保持通过（MP3 基本标签、歌词+封面、clearLyrics、null 不覆盖、缺失文件 write_failed）
- 新增或验证：含封面的 FLAC 写入不崩溃（可用 Robolectric 或临时文件 + `AudioFileIO` 写入冒烟）
- WebDAV 本地两条路径的刮削回传 `FileWriteResult` 均不抛异常

## Acceptance Criteria

- [ ] 真机刮削回传含封面到 FLAC 不再崩溃，`TagWriter.write()` 返回 `ok=true` 且 `TagReader.read().coverBytes` 与写入一致
- [ ] 真机刮削回传含封面到 MP3/OGG/FLAC 均不触发 `NoClassDefFoundError`，异常统一为 `write_failed`
- [ ] `MusesApplication.onCreate()` 中已设置 `TagOptionSingleton.isAndroid=true`
- [ ] `TagWriter.write()` `catch (Throwable)` 且 `CancellationException` 重抛
- [ ] `./gradlew :core:media:testDebugUnitTest :core:scrape:testDebugUnitTest :app:lintMusesDebug` 通过

## Notes

- jaudiotagger 3.0.1 源码事实：`StandardArtwork.getImage()` 用 `ImageIO.createImageInputStream` / `ImageIO.read`，`AndroidArtwork.setImageFromData()` 直接抛异常，`FlacTag.createField()` 强依赖 `setImageFromData()==true`
- 参考实现：`TagWriter.createArtwork` 手写 `Artwork` 匿名实现，`BitmapFactory.Options.inJustDecodeBounds=true` 取宽高
- 非目标：不升级 jaudiotagger 版本，不改其他 tag 逻辑

## Out of Scope

- 刮削 UI、队列、限流逻辑
- jaudiotagger 版本升级或替换

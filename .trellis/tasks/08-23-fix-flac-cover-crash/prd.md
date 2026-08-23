# PRD：修复 FLAC 封面写入崩溃（ImageIO 不存在于 Android）

## 目标

刮削写回带封面的 FLAC/OGG 等格式时不再闪退，封面正常写入。

## 根因（真机崩溃日志确认）

```
FATAL EXCEPTION: CapacitorPlugins
java.lang.NoClassDefFoundError: Ljavax/imageio/ImageIO;
  at org.jaudiotagger.tag.images.StandardArtwork.getImage
  at FlacTag.setField → AudioMetadataWriter.applyFields(:119)
```

- `applyFields` 封面分支用 `ArtworkFactory.createArtworkFromFile(coverFile)`；`TagOptionSingleton` 默认 `isAndroid=false` → 返回 `StandardArtwork`
- FLAC 的 `FlacTag.setField(artwork)` 内部调 `artwork.getImage()` → StandardArtwork 走 `javax.imageio.ImageIO`（Android 不存在）→ `NoClassDefFoundError`
- 该错误是 Error 不是 Exception，现有 catch 全部漏接 → 插件线程闪退

## 需求

1. **R1 主修**：在 AudioMetadataWriter 写入入口（writeToFile 开头或类初始化）设置 `TagOptionSingleton.getInstance().setAndroid(true)`，使 ArtworkFactory/ImageHandlingFactory 走 AndroidArtwork/AndroidImageHandler（BitmapFactory 实现，无 ImageIO 依赖）
2. **R2 兜底**：applyFields 封面写入分支（deleteArtworkField/setField）用 try/catch Throwable 包裹，非预期错误转成 `AudioMetadataException("write_failed", ...)`——插件内任何异常不得击穿成闪退
3. 本地写标签与 WebDAV 写标签共用 applyFields，两路同时受益

## 验收标准

1. gradle assembleDebug 通过
2. MuMu 真机：对带封面的 WebDAV FLAC 歌曲重试刮削回写成功（含封面字段），App 不闪退
3. 回归：mp3 等其他格式回写不受影响（setAndroid 只影响 artwork/handler 工厂选择）

## 范围外

- 不改 ScrapePage/writeback 前端逻辑
- 不改读取链路（AudioMetadataReader）

## 约束

- Kotlin 改动仅限 AudioMetadataWriter.kt（如需 TagOptionSingleton 导入）
- setAndroid 是全局静态配置，确认对读链路无副作用（读封面走 metadata 二进制解析，不经 ImageIO）

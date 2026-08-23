# PRD：FLAC 封面写入绕过 setImageFromData

## 目标

刮削回写 FLAC/OGG 歌曲带封面时成功写入 Picture 块，不再报「写入封面失败」。

## 根因（字节码勘察确认）

- `FlacTag.createField(artwork)`（含 setField 路径）内部调 `artwork.setImageFromData()` 判断解码成败
- jaudiotagger 3.0.1 的 `AndroidArtwork.setImageFromData()` / `getImage()` **无条件抛 UnsupportedOperationException**；`StandardArtwork` 则依赖 Android 不存在的 ImageIO
- 结论：Vorbis 系（FLAC/OGG）经 Artwork 接口写封面在 Android 上无解，必须直接构造字段
- 对照：ID3（mp3）`createField(artwork)` 只消费 isLinked/getBinaryData/getPictureType/getMimeType/getDescription/getImageUrl，AndroidArtwork 均可用 ✓

## 需求

1. `applyFields` 封面分支重构：
   - 读封面字节 `coverFile.readBytes()`（非空校验已有）
   - 用 `BitmapFactory.Options(inJustDecodeBounds = true)` 解码出 width/height/outMimeType（不分配像素内存）；mime 兜底按扩展名 jpg→image/jpeg、png→image/png，最终兜底 image/jpeg
   - `tag is VorbisCommentTag`（FlacTag 是其子类）：`tag.setField(tag.createArtworkField(bytes, 3 /*FRONT_COVER*/, mime, "", w, h, 0, 0))`
   - 其他格式：构造 `AndroidArtwork`（setBinaryData/setMimeType/setPictureType(3)/setDescription）后 `tag.setField(artwork)`
2. R2 的 catch Throwable 兜底保留包裹整个封面分支
3. 移除对 `ArtworkFactory.createArtworkFromFile` 的依赖（或仅留非 Vorbis 路径使用直接 new AndroidArtwork）

## 验收标准

1. gradle assembleDebug 通过
2. MuMu 真机：之前失败的 WebDAV FLAC 歌曲重试刮削回写成功（行状态 ✓），播放验证封面/标签生效
3. 回归：mp3 回写不受影响

## 范围外

- 不改前端、不改读取链路

## 约束

- Kotlin 改动仅限 AudioMetadataWriter.kt

# 音频标签读取与缓存优化

## Goal

集成 jaudiotagger 实现音频标签读取（标题/歌手/专辑/歌词/封面），实现 WebDAV 本地缓存策略，修复歌曲列表信息为空的占位问题

## 背景

当前歌曲列表从 WebDAV 索引后，数据库中只有文件名，缺少实际的元数据（标题、歌手、专辑、封面）。导致 UI 展示的都是空的占位信息。

## Requirements

### R1: 集成 jaudiotagger 库
- 添加 `org.jthink:jaudiotagger:2.2.5` 依赖
- 创建 `AudioTagReader` 工具类，封装标签读取逻辑

### R2: 支持读取的标签
- 标题 (Title)
- 艺术家 (Artist)
- 专辑 (Album)
- 封面 (Artwork)
- 歌词 (Lyrics) - USLT 字段
- 时长 (Duration)

### R3: WebDAV 文件缓存策略
- 首次读取标签时下载文件头部（64KB）解析
- 播放时优先使用本地缓存
- 支持 Range 请求降级（不支持时下载全文件）
- LRU 缓存清理机制

### R4: 修复歌曲列表信息展示
- 索引 WebDAV 歌曲时自动读取标签并更新数据库
- 已有歌曲支持手动刷新标签
- UI 正确显示标题、歌手、封面

### R5: 兼容性
- 支持 MP3、FLAC、OGG、M4A 格式
- 兼容各种标准 WebDAV 服务器（Nextcloud、Synology、坚果云等）
- 本地文件和 WebDAV 文件都能读取

## Acceptance Criteria

- [ ] jaudiotagger 依赖成功添加，编译通过
- [ ] 能从 MP3/FLAC 文件中读取标题、歌手、专辑、封面
- [ ] 能从音频文件中读取内嵌歌词（如有）
- [ ] WebDAV 文件能通过 Range 请求读取头部标签
- [ ] 本地缓存文件能正确存储和清理
- [ ] 歌曲列表正确显示标题和歌手（不再是空占位）
- [ ] 封面能正确加载和显示

## Technical Notes

- 数据库 `SongEntity` 已有 `title`、`artist`、`albumTitle`、`coverUri`、`lyrics` 等字段，无需迁移
- jaudiotagger 需要本地文件路径，WebDAV 文件需先下载到缓存目录
- 封面数据为 ByteArray，存储为本地文件后更新 `coverUri`

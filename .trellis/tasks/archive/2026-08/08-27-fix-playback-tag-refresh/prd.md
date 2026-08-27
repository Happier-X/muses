# 修复播放后列表仍显示占位

## Goal

播放 WebDAV/本地歌曲后，列表中该歌曲的标题/歌手/专辑/封面仍为“未知”，需在播放时通过 AudioTagReader 读取真实标签并回写数据库，列表自动刷新。

## 背景

- 当前 `AudioTagReader` 已实现但未接入播放链路，原有 ExoPlayer 容器解析不一定覆盖所有格式
- 用户验证：MuMu 模拟器播放后，列表仍显示占位

## Requirements

### R1: 播放时懒扫描接入 AudioTagReader
- 在 `PlaybackService` 或 `PlayerConnection` 当前曲目切换时，对 `tagsVersion < 1` 的歌曲调用 `AudioTagReader.readTagForUpdate`
- 支持 `http(s)` 与本地路径，复用现有 Range 逻辑与 OkHttp 鉴权拦截器
- 在 IO 调度器执行，不阻塞主线程/播放

### R2: 回写数据库并触发刷新
- 读取成功后 `songDao.upsert` 更新 `title/artist/albumTitle/coverUri/durationMs/durationSec/tagsVersion=1`
- 保持 `title` 空白回退原标题，避免空标题
- Room Flow 自动刷新列表，无需手动通知

### R3: 兼容性与幂等
- 仅对 `tagsVersion < 1` 的歌曲执行，已补齐的跳过
- 读取失败保持原值，下次播放重试，不抛出异常
- 本地与 WebDAV 路径均兼容

## Acceptance Criteria

- [ ] 播放一首 tagsVersion=0 的 WebDAV 歌曲后，切回列表该行显示真实标题/歌手/专辑
- [ ] 封面如有则显示，不再为默认音符
- [ ] 再次播放已补齐的歌曲不重复触发读取
- [ ] 本地文件同样生效
- [ ] 播放过程无卡顿/ANR

## Notes

- 复用 `AudioTagReader.readTagForUpdate` 与 `CoverCacheWriter` 逻辑（如有封面落盘）
- 需在 `core:media` 模块可访问 `AudioTagReader`（已在 `core:data` 提供 Singleton，可跨模块注入）

# 本地扫描未勾选标签时像 WebDAV 一样懒补充

## 目标

本地音源扫描时，若用户未勾选“读取音乐标签”，扫描产物应像 WebDAV 一样采用懒补充策略：快速入库文件名/ MediaStore 占位数据（tagsVersion=0），在播放时再按需读取真实音频标签并回写，避免扫描期逐文件 jaudiotagger 的耗时与潜在卡顿，同时保持后续播放链路自动补齐。

## 背景

- WebDAV 链路已实现「文件名建库 + 播放时懒扫描」：`WebDavLibraryScanner` 产出 `tagsVersion=0` 的占位 Song，`PlaybackService` 在切歌时对 `tagsVersion < 1` 的歌曲调用 `AudioTagReader.readTagForUpdate` 并 `songRepository.upsert(tagsVersion=1)`。
- 本地方案现为 MediaStore 枚举 + 可选 jaudiotagger：`LocalLibraryScanner.scan(readTags: Boolean)` 控制是否逐文件读标签。但 `readTags=false` 时仍写入 `tagsVersion=1`，导致后续播放懒扫描分支（`tagsVersion < TAGS_VERSION`）永远不命中，列表长期为文件名/MediaStore 回退值且无法自动补齐。
- 另：本地 Song.path 存的是 `content://` URI，`AudioTagReader` 现仅支持 `File(path)` 与 `http(s)`，对 `content://` 会静默失败，致使即便 tagsVersion=0 也无法完成懒读。

## 需求

### 功能需求

1. **本地未勾选时置零**：`LocalLibraryScanner.scan(source, readTags=false)` 产出的 `Song.tagsVersion` 必须为 `FILENAME_TAGS_VERSION (0)`，与 WebDAV 一致，作为“待懒补充”标记。
2. **占位数据可用**：未勾选时仍可用 MediaStore 列值/文件名作为标题等占位（`title = displayName 去扩展名 / MediaStore TITLE 回退`），`coverUri/lyrics` 留空，`durationMs` 取 MediaStore 时长，等待懒补充覆盖。
3. **播放时懒补充**：复用既有 `PlaybackService` 懒扫描分支（`tagsVersion < TAGS_VERSION`），对本地 `content://` 歌曲亦生效；成功读取后通过 `songRepository.upsert` 回写真实 `title/artist/album/lyrics/coverUri/durationMs/durationSec/tagsVersion=1` 并重建派生索引（专辑/艺术家）。
4. **AudioTagReader 支持本地 URI**：`AudioTagReader.readTags/readTagForUpdate/extractCover` 需支持 `content://`（经 `ContentResolver.openInputStream` 拷贝到缓存再解析）、`file://` 以及普通文件路径；`content://` 缓存文件命名稳定可控，失败不崩溃返回 null。
5. **幂等与容错**：无有效标签（字段全空/封面无）亦视为已处理，可将 `tagsVersion` 置 1 避免对同一无标签文件重复 Range/IO；读取异常静默保持 `tagsVersion=0` 下次重试，不阻塞播放。

### 非功能 / 约束

- 不改变 WebDAV 现有流程与限流契约。
- 本地勾选 `readTags=true` 时保持现有立即读标签并置 `tagsVersion=1` 的行为不变（回归兼容）。
- `PlaybackService` 既有 `catch (CancellationException) throw` 与 `ErrorLogStore` 留痕不变。
- `Song.path` 仍存 `content://` URI，不新增 DB 字段；新增常量与逻辑需在 `LocalLibraryScanner` 内聚。

## 验收标准

- [ ] 未勾选“读取音乐标签”扫描本地音源后，落库 songs 的 `tagsVersion=0`，列表显示文件名或 MediaStore 标题，`coverUri` 为空。
- [ ] 勾选后扫描，落库 `tagsVersion=1` 且标题/封面等立即完整（回归）。
- [ ] 播放一首 `tagsVersion=0` 的本地歌曲后，DB 中该行 `tagsVersion` 变为 1，标题/歌手/专辑/封面（如音频内嵌）被真实标签覆盖，列表自动刷新；多次切换同一首不再重复触发懒读。
- [ ] 对无标签的纯文件名本地音频，播放后 `tagsVersion` 亦置 1（避免重复尝试），标题保持文件名。
- [ ] `AudioTagReader` 对 `content://` 本地路径懒读成功，无崩溃；对无效/不可读路径静默失败（返回 null），`tagsVersion` 保持 0 下次重试。
- [ ] WebDAV 懒补充链路不受影响（仍为文件名建库 tagsVersion=0 → 播放补齐）。
- [ ] 单元门禁与现有 relevant 单测通过。

## 边界与例外

- `content://` 权限缺失或文件已被删除：懒读返回 null，不抛异常上冒，`tagsVersion` 保持 0。
- `file://` 前缀路径兼容（部分机型/历史数据）：同样支持。
- 扫描期 `READ_MEDIA_AUDIO` 未授权：保持现有“返回空列表”行为。

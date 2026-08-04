# 实现清单 — 沉浸页更多菜单与编辑歌曲信息

## 建议顺序（可单 PR，逻辑分步）

### 1. 曲库模型与保护 helper

- [ ] `SongItem` + `UserEditedField` 类型  
- [ ] `isSongItem` / `sanitize` / `hasSongChanged` 支持 `userEditedFields`  
- [ ] `applyTagsRespectingUserEdits`  
- [ ] `updateSongUserEdit`（或等价 patch API）  
- [ ] 所有自动 upsert 路径接入保护（扫描、controller 在线、prefetch）

### 2. 原生写元数据（Android）

- [ ] `AudioMetadataWriter`（或 Reader 旁类）：jaudiotagger 写 title/artist/album/lyrics/artwork/RG  
- [ ] `LocalLibraryPlugin.writeMetadata`：SAF 读临时 → 写 → outputStream 回写  
- [ ] WebDAV：缓存文件写 tag + PUT（插件或前端 http + 原生写文件）  
- [ ] TS 桥：`library/native` 或 `player` 侧封装 `writeAudioMetadata`  
- [ ] 错误码：`not_writable` / `unsupported_format` / `put_failed` / …

### 3. 封面选图落盘

- [ ] 选图 → app covers 缓存安全 URI（可扩 `AudioPlayerPlugin` 或 LocalLibrary 拷贝）  
- [ ] 禁止 data:/http 入 `muses:songs`

### 4. Player UI

- [ ] mode-bar 第四键更多 + 操作 `HBottomSheet`（仅编辑 + 取消）  
- [ ] 编辑 `HBottomSheet` + `useForm` 字段  
- [ ] 保存：update 库 → sync 展示/音量/会话 → writeMetadata 尽力 → Toast  
- [ ] mode-bar 宽度/矮屏微调  

### 5. Spec

- [ ] `component-guidelines`：沉浸更多 + 编辑入口  
- [ ] `features-player` 或 library 规范：userEditedFields、保存顺序、D4 提示  
- [ ] forms 若有特例（textarea/封面）补一句  

### 6. 验证

- [ ] `npm run lint`  
- [ ] `npm run build`  
- [ ] 手工：AC1–AC6  

## 回滚点

- 仅 UI：还原 PlayerPage  
- 模型：去掉 userEditedFields 需迁移忽略未知字段（向前兼容读）  
- 原生：删除 write 方法不影响读  

## 风险实施备注

- 播放中写文件失败可接受（D4）  
- WebDAV PUT 与 local 可分 commit，但同一任务验收「尽力」  
- 若原生工作量爆炸：可先交 **库+保护+UI**，writeMetadata 打桩返回 not_implemented 仍满足 D4 文案——**仅当实现中阻塞时再与用户确认降级**；规划默认仍要做真写  

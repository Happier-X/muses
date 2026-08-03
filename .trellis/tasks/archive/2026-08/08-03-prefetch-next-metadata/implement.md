# 实现计划：WebDAV 下一首元信息预取

## 清单

1. [ ] 在 `controller.ts`（或抽 `prefetchMetadata.ts` 由 controller 调用）增加 `metadataPrefetchToken` 与 `prefetchNextMetadata(song, token)`：
   - 并行：歌词 / 封面 / 文本在线路径
   - 仅 upsert；token 校验；try/catch 静默
2. [ ] 扩展 `prefetchNextTrack`：在现有 WebDAV 音频预取旁 `void prefetchNextMetadata(next, token)`（先 `++metadataPrefetchToken` 捕获）；非 webdav / 自身仍整段 return 前不调元信息
3. [ ] 确认 `reschedulePrefetchAfterQueueChange` 仍只调 `prefetchNextTrack` 即可带动元信息重调度
4. [ ] 复用：`matchOnlineLyrics`、`matchOnlineCoverRemote`、`cacheRemoteCover`、`matchOnlineTextMeta`、`mergeTextMetaFillEmpty`、`needsOnlineTextMeta`、`shouldPersistOnlineLyrics`、`toSafeCoverUri`、`getLatestSongSnapshot` / `upsertSong`
5. [ ] **禁止**：预取路径写 `state.*` 展示字段；禁止动 `lyricsMatchToken` / `onlineCoverToken` / `onlineTextToken`
6. [ ] 单测：非 webdav 跳过；token 过期不写库；成功路径 upsert 字段（mock match API）
7. [ ] 更新 `.trellis/spec/frontend/features-player.md`「下一首预取」节：音频 + 元信息、仅 WebDAV、只写库
8. [ ] `npm run lint` / 相关 `test:unit` / 必要时 `build`

## 验证命令

```bash
npm run lint
npm run test:unit
# 若有针对性文件：
# npx vitest run <prefetch-related-spec>
```

## 回滚点

- 删除 `prefetchNextMetadata` 调用与 helpers；spec 回退该段。

## Review gates

- [ ] PRD AC1–AC7
- [ ] design：独立 token、不写 playerState
- [ ] Queue 本地下一首无额外网络预取元信息

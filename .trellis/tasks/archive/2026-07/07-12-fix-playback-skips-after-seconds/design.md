# 设计：WebDAV 恢复远程直链播放

## 根因

增长中的普通本地文件不是稳定的流媒体数据源。NativeAudio/底层播放器可能固定文件长度或读到当前 EOF，产生 `complete`；短时 duration 又使前端自然结束判定成立。因此不能靠 finished 过滤彻底修复，必须停止播放未完成文件。

## 数据流

```
playSong(webdav)
  → resolve WebDAV password
  → native.ts resolveAssetPath
  → assetPath = remote URL
  → headers.Authorization = Basic ...
  → currentBufferedPosition = null
  → NativeAudio.preload({ isUrl: true, headers })
  → NativeAudio.play
```

本地音源继续：content URI 复制完成 / file URI → full buffer。

## 改动边界

- `src/features/player/native.ts`
  - 删除 WebDAV 优先调用 `prepareWebDavAudioFile` 的分支。
  - WebDAV 始终返回远程 URL + Authorization。
  - 明确缓冲未知。
- 测试
  - WebDAV 不调用 bridge prepare。
  - preload 参数为远程 URL + headers。
  - bufferedPosition 不伪造。
- spec
  - `features-player.md`、`component-guidelines.md`、`state-management.md` 更新 WebDAV 缓冲语义。

## 保留项

- `AudioPlayerPlugin.prepareWebDavAudioFile` / `WebDavAudioCache` 暂不删除；它们可供元数据缓存或后续原生流式方案研究，但当前播放链路不调用。
- `cancelBufferSession` 继续调用，防止升级/竞态遗留下载。
- controller 的 seek guard + near-end 规则继续作为异常事件第二道防线。

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| 某些 WebDAV 服务不支持 NativeAudio 远程 headers | 这是 v0.0.7 已验证链路；保留安全错误文案 |
| 缓冲条消失 | 使用 `null` 明确表达未知，禁止假进度 |
| seek 到未加载区 | NativeAudio 远程流自行处理；前端仅 duration clamp |
| 旧渐进下载仍运行 | play/stop 前继续 cancelBufferSession |

## 回滚

单文件恢复 `resolveAssetPath` 的 prepare 分支即可，但除非换成真正流式数据源，不应回滚。

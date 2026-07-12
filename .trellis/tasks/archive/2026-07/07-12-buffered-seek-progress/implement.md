# 实现计划：已缓冲进度条与 seek 限制

## 顺序

1. **类型与状态**
   - `PlayerState` 增加 `bufferedPosition: number | null`
   - `playSong`/`stop`/`applyNativeState` 读写与重置
2. **native 缓冲上报（Android）**
   - 扩展 `AudioPlayerPlugin`：WebDAV 渐进下载到缓存文件 + `bufferProgress` 事件
   - 本地 prepare 完成后上报 full buffer
   - `native.ts`：订阅 buffer 事件并入 `stateChange` 或独立合并进 controller
3. **播放路径改造**
   - WebDAV：渐进文件可播后 `file://` 交给现有 `NativeAudio.play`（仍不改 capgo 源码）
   - 保证首包可播即播（非整首下完）
4. **seekPlayback clamp**
   - 上限 = `min(duration, bufferedPosition)`（buffered 已知时）
   - 歌词点击走同一 API；越界不 seek
5. **PlayerPage UI**
   - `--buffered` 缓冲层 + 现有 `--progress`
   - input/change clamp
6. **测试**
   - controller / PlayerPage 单测
7. **spec**
   - features-player / state-management / component-guidelines

## 验证

```bash
npx vitest run tests/unit/player.spec.ts
npm run lint
npx vue-tsc --noEmit
```

真机：WebDAV 曲目开播后观察缓冲条增长，拖到未缓冲区应被限制且不切歌。

## 风险点

- 增长中的缓存文件与播放器兼容性 → 可播阈值 + 真机验证
- 与 `WebDavAudioCache` 路径复用，避免重复下载

## 回滚点

- 可先合「仅状态 + UI + clamp」，原生下载失败时 buffered=null 降级
- 出问题可关掉 clamp，只保留视觉（不推荐，但可应急）

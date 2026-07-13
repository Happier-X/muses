# 实施清单：预取下一首

## 有序步骤

1. **queue**
   - 新增 `peekNext()`
   - 导出到 controller
   - 单测：顺序/随机/单曲循环/空队列；确认不改 `currentIndex`

2. **原生 bridge**
   - `AudioPlayerPlugin.getCachedWebDavAudioFile`
   - `AudioPlayerPlugin.prefetchWebDavAudioFile` → `WebDavAudioCache.getCachedFile` / `downloadInBackground`
   - 同 URL 已有完整缓存时直接返回 cached
   - partial 不得当作缓存命中

3. **native.ts**
   - WebDAV resolve：先查完整缓存；命中 file:// + fullBuffer；否则远程直链
   - 暴露/内部调用 prefetch API
   - 保留 cancelBufferSession 兼容逻辑，但不依赖它取消完整预取

4. **controller.ts**
   - `playSong` 成功进入 playing 后调度 `prefetchNextTrack()`
   - 解析下一首、跳过本地/自身/空
   - 取 WebDAV 密码后调用 bridge
   - 失败静默

5. **测试**
   - peekNext
   - 预取触发与跳过条件
   - 缓存命中播放路径
   - 未缓存远程回退
   - 密码不泄漏
   - 不调用 progressive prepare 作为播放路径

6. **spec**
   - features-player / state-management：下一首完整预取与完整缓存优先
   - 明确 partial 禁止播放

7. **验证**
   - vitest / lint / vue-tsc / diff-check

## 回滚点

- controller 去掉 prefetch 调度
- native WebDAV resolve 去掉 getCached 优先

## 启动前

- [x] 决策收敛
- [x] prd / design / implement
- [ ] 用户审阅并确认开始
- [ ] curate jsonl
- [ ] `task.py start`

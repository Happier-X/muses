# 实施清单：修复 WebDAV 数秒跳歌

1. 修改 `src/features/player/native.ts`
   - WebDAV 始终远程 URL + Basic Auth headers
   - 设置 `currentBufferedPosition = null`
   - 不调用 `prepareWebDavAudioFile`
   - 保留 cancel buffer session

2. 补强 `tests/unit/player.spec.ts`
   - WebDAV preload 使用远程 URL + Authorization
   - bridge `prepareWebDavAudioFile` 未调用
   - WebDAV 缓冲未知
   - 本地 full buffer 不回归
   - 自然结束仍自动下一曲

3. 更新规范
   - `features-player.md`
   - `component-guidelines.md`
   - `state-management.md`
   - 删除/改写“WebDAV 渐进 file:// 起播”约定

4. 验证
   - `npx vitest run`
   - `npm run lint`
   - `npx vue-tsc --noEmit`
   - `git diff --check`

## 回滚点

- 代码核心只在 `resolveAssetPath`；出现远程认证兼容问题时可单独回滚，但不得恢复增长文件播放。

## 启动前

- [x] PRD / design / implement 齐备
- [x] 用户选择远程直链策略
- [ ] 用户确认开始实现
- [ ] curate implement/check context
- [ ] `task.py start`

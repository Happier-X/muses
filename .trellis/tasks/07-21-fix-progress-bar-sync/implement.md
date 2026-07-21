# 实现清单：#47 进度条同步

## 顺序

1. [x] `native.ts`：playing 位置轮询 start/stop，挂到 play/resume/pause/stop/unload/seek
2. [x] `PlayerPage.vue`：`onSeekInput` 仅在用户 seek 手势锁下更新 preview
3. [x] `normalizePlaybackTime` 使用 `>= 0`
4. [x] 单测补充 / 回归 `tests/unit/player.spec.ts`
5. [x] `npm run lint` / `npm run test:unit` / `npm run build`
6. [ ] 关闭 GitHub #47

## 验证命令

```bash
npm run lint
npm run test:unit
npm run build
```

## 回滚

还原 `native.ts` 轮询与 `PlayerPage` 手势判断即可。

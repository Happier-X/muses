# 实现清单

1. [x] `PlayerPage.vue`：队列 `listIcon = list`；import `list`；顺序模式仍 `listOutline`
2. [x] `PlaylistsPage.vue`：歌单行图标改为 `list`
3. [x] `ion-lucide.ts` 文件头语义表对齐（如需）
4. [x] `component-guidelines.md`：同语义表 + 队列/歌单约定
5. [x] lint / unit
6. [x] 关闭 #44；commit / archive

## 验证

```bash
npm run lint
npm run test:unit -- --run
```

结果：`eslint` 通过；`vitest` 16 files / 271 tests 全部通过。

## 审查门

- `rg` 确认打开队列与歌单占位无再误用 `listOutline`：业务侧 `listOutline` 仅出现在 `PlayerPage.vue` 的顺序播放 `shuffleIcon` 与单测断言。
- `listOutline` 仅出现在顺序播放（及规范说明）

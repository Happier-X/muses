# 歌单 CRUD 实现清单

## 顺序

1. [x] `src/features/playlist/types.ts` + `storage.ts` + `index.ts`
2. [x] `tests/unit/playlist.spec.ts` 绿
3. [x] 路由 `playlists/:id` + `PlaylistDetailPage.vue` 骨架
4. [x] `PlaylistsPage.vue` 列表 CRUD UI
5. [x] 详情：解析曲目、移曲、播放全部
6. [x] `SongsPage` 更多 ActionSheet + 加入歌单
7. [x] 全量 `vitest` + `vue-tsc` + lint
8. [x] 更新 `.trellis/spec/frontend`（directory / state 如有必要）

## 验证命令

```bash
npx vitest run tests/unit/playlist.spec.ts
npx vitest run
npx vue-tsc --noEmit
npm run lint
```

## 回滚点

- 仅 feature + 空页：可整目录删 `playlist` 回退
- 路由/UI 接入后：还原 `PlaylistsPage` 与 `SongsPage` 更多按钮

## 审查门

- 无 `data:` 进 playlist 存储
- 空名不可创建/重命名
- 删歌单有确认
- 重复加歌幂等

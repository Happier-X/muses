# 执行计划：Token 化与 Muses UI 组件库

## 前置门禁

1. **归档** `07-22-impeccable-init`（含 PRODUCT/DESIGN/AGENTS/主题 primary/sidecar 的 commit + `task.py archive`）
2. 用户审阅通过本任务的 `prd.md` + `design.md` + `implement.md`
3. `task.py start` 后才写业务代码

## Checklist

### 0. 前置

- [x] impeccable-init 已 commit 并 archive
- [x] 当前任务 `task.py start` → `in_progress`

### 1. Token 层

- [x] 新增 `src/theme/tokens.css`（色/间距/圆角/字号/动效/z-index/断点/immersive）
- [x] 重构 `src/theme/variables.css`：primary 与布局引用 token；保留 header 无阴影规则
- [x] `src/main.ts` 在 variables 之前 import tokens.css
- [x] 校验深色模式 primary 仍为 `#006FEE`

### 2. 组件层

- [x] `src/components/ui/MEmptyState.vue`
- [x] `src/components/ui/MCover.vue`
- [x] `src/components/ui/MPage.vue`
- [x] `index.ts` 具名导出
- [x] 列表 playing/行高：token 化

### 3. 迁移

- [x] AlbumsPage / ArtistsPage：empty + cover（若适用）+ MPage
- [x] PlaylistsPage / PlaylistDetailPage：empty + cover
- [x] SongsPage：empty + cover + playing 背景 token
- [x] MiniPlayer：封面改 MCover
- [x] SourcesPage empty
- [x] PlayerPage：仅 immersive 硬编码色/时长 → token（不拆大组件）

### 4. 文档与规范

- [x] 更新 `.trellis/spec/frontend/directory-structure.md`（theme + components/ui）
- [x] 更新 `component-guidelines.md`（ui 层约定、禁止 ion 1:1 封装）
- [x] DESIGN.md 组件节补充路径（轻量）

### 5. 验证

- [x] `npm run lint`
- [x] `npm run build`（含 vue-tsc）
- [x] `npm run test:unit -- --run`（17 文件 / 311 测试通过）
- [ ] 手动：歌曲列表播放高亮、空状态、MiniPlayer 封面、平板 720 宽、深浅色 primary（需 Android 真机/浏览器目视）

## 建议 commit 切分

1. `style(theme): 引入 muses tokens 并桥接 Ionic primary`
2. `feat(ui): 新增 MEmptyState / MCover / MPage`
3. `refactor(views): 列表页迁移空状态与封面组件`
4. `docs(spec): 记录 token 与 ui 组件约定`

## 回滚点

- 每步可独立 revert；优先保证 tokens 不破坏 ion-color-primary

## 实现时注意

- 读 `.trellis/spec/frontend` Pre-Development Checklist
- 图标继续走 `src/icons/ion-lucide.ts`
- 不引入 Material elevation

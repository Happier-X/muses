# 技术设计：Token 化与 Muses UI 组件库

## 1. 边界

| 层 | 职责 | 不负责 |
|----|------|--------|
| `src/theme/tokens.css` | 原始 + 语义 CSS 变量（唯一视觉数值源） | 组件行为 |
| `src/theme/variables.css` | Ionic 变量桥接、header 全局修正 | 业务布局细节 |
| `src/components/ui/*` | Muses 语义 UI（可内用 Ionic） | 播放/曲库业务状态 |
| `src/views/*` | 页面编排、数据、路由 | 重复定义色/圆角/空状态结构 |
| `src/features/*` | 业务逻辑 | UI token 定义 |

**依赖顺序**：先归档 impeccable-init 文档与 primary 主题 → 再落地 tokens → 组件 → 页面迁移 → spec。

## 2. Token 架构

### 2.1 文件与加载

```text
src/main.ts
  → @ionic/... css
  → ./theme/tokens.css      # 新增，先于 variables
  → ./theme/variables.css   # 桥接 ion-* + 全局修正
```

### 2.2 命名

- 前缀：`--muses-`
- 原始色：`--muses-primary-50` … `--muses-primary-900`（HeroUI `common.blue`）
- 语义色：`--muses-color-primary`（= 500）、`--muses-color-ink-muted`、`--muses-color-playing-bg` 等
- 间距：`--muses-space-xs|sm|md|lg|xl`
- 圆角：`--muses-radius-cover`（10px）、`--muses-radius-cover-sm`（8px）、`--muses-radius-pill`
- 字号角色：`--muses-font-title`、`--muses-font-body-sm`、`--muses-font-label`（固定 rem/px，产品 UI 不用 fluid 列表字）
- 动效：`--muses-duration-overlay: 220ms`、`--muses-duration-fab: 180ms`、`--muses-ease-standard: ease`
- 布局：`--muses-breakpoint-tablet`、`--muses-content-max-width`、`--muses-tab-bar-height`、`--muses-mini-player-height`
- z-index：`--muses-z-tab`、`--muses-z-mini-player`、`--muses-z-player`（对齐现网 30 / 1000 / 1100）

### 2.3 Ionic 桥接（variables.css）

```css
:root {
  --ion-color-primary: var(--muses-color-primary);
  --ion-color-primary-rgb: var(--muses-color-primary-rgb);
  --ion-color-primary-shade: var(--muses-primary-600);
  --ion-color-primary-tint: var(--muses-primary-400);
  /* contrast 白 */
}
@media (prefers-color-scheme: dark) {
  :root { /* 同样指向 Muses primary，禁止 #4d8dff */ }
}
```

布局断点 token 可从现有 `:root` 迁入 `tokens.css`，`variables.css` 只 `var()` 引用或 re-export 注释说明。

### 2.4 Immersive 专用

沉浸页 token 单独分组（如 `--muses-immersive-void`），**不**强制 PlayerPage 组件化；迁移时把硬编码 hex 换成 token 即可。

## 3. 组件契约

目录：`src/components/ui/`  
导出：可选 `src/components/ui/index.ts` 具名导出（便于页面 import）。

### 3.1 `MEmptyState.vue`

- **Props**：`title: string`；`description?: string`
- **Slots**：`default` 可选操作区（按钮）
- **样式**：居中、padding 用 space token、标题 ink、说明 ink-muted
- **替换**：Songs/Albums/Artists/Playlists/Sources 等同构 empty 块

### 3.2 `MCover.vue`

- **Props**：`src?: string | null`；`size?: 'sm' | 'md' | number`（默认 md≈48–52）；`radius?: 'sm' | 'md'`；`alt?: string`（装饰性默认空 alt + aria-hidden）
- **行为**：有 src 显示 img；无则 medium 占位底 + 音乐图标（沿用项目 ion-lucide）
- **替换**：`.song-cover`、`.cover-wrap`、歌单封面方块

### 3.3 `MPage.vue`

- **职责**：薄壳，不吞掉业务 toolbar 按钮
- **Slots**：`title` | `start` | `end`（header 区）；`default`（content）
- **实现要点**：
  - 外层 `ion-page`
  - header/toolbar 依赖全局无阴影（已有 variables 规则）
  - 不强制 large title；由页面决定
  - content 默认 `fullscreen` 可 prop 控制
- **迁移策略**：优先 1–2 个简单页（Albums/Artists）验证，再推 Songs；若某页 header 过于定制可暂不套 MPage，但 empty/cover 仍迁移

### 3.4 列表行

- **优先**：`.m-song-item` / 共享 CSS 类（playing 背景用 `--muses-color-playing-bg`）放在 `ui/list.css` 或组件 scoped + :deep
- **可选**：`MSongRow` 仅当 Songs + PlaylistDetail + Queue 三处 API 能统一时再抽；否则本任务停在 token + 类名

## 4. 数据流 / 依赖

```text
DESIGN.md / PRODUCT.md
       ↓ 约束
tokens.css → variables.css (ion bridge)
       ↓
ui/M* components
       ↓
views/* 迁移
       ↓
.trellis/spec/frontend 更新
```

无运行时主题切换 API；跟随 `prefers-color-scheme` + Ionic dark.system（primary 固定 HeroUI）。

## 5. 兼容与风险

| 风险 | 缓解 |
|------|------|
| MPage 与复杂 header 冲突 | 可选采用；不阻塞 empty/cover |
| 虚拟列表 Songs 行高变化 | 保持 `--min-height: 72px` token 化后测量 |
| MiniPlayer 封面尺寸 | MCover size 映射现网 48 |
| 全局 CSS 顺序错误 | main.ts 明确 tokens → variables |
| 过度抽象 | 3+ 处复用才组件化；禁止 ion-* 全量包装 |

## 6. 回滚

- 删除 `tokens.css` 引用并恢复 variables 内联 primary 可退
- 组件迁移按文件 git 还原；token 与组件分 commit 便于只回滚迁移

## 7. 与 Impeccable

- 实现后必要时轻量更新 `DESIGN.md` Components 节指向 `src/components/ui`
- 不在本任务重新跑完整 document，除非 token 名与 DESIGN 严重漂移

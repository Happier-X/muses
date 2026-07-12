# 沉浸式播放页 UI 复刻 — 技术设计

## Scope Boundary

| 在范围内 | 不在范围内 |
|----------|------------|
| `PlayerPage.vue` 模板结构、样式、控制页布局 | 播放 controller / queue / native / media-session |
| 次要控制纯图标化与状态样式 | 新增收藏、音量、分享等能力 |
| 空状态与单测断言适配 | 歌词页独立视觉重设计 |
| 宽屏双栏兼容性保持 | 路由、MiniPlayer 业务逻辑 |

## Component Contract

`PlayerPage.vue` 继续作为全局 overlay 内容组件：

- 由 `App.vue` 以 `defineAsyncComponent` + `v-if="playerOverlayVisible"` 挂载
- 关闭：`closePlayerOverlay()`（下滑手势 / Android back 由 App 层处理）
- 不引入 `/player` 路由

## Layout Design

### 窄屏控制页

```
┌────────────────────────────┐
│  safe-area padding         │  无标题
│         [ cover ]          │  大圆角封面
│      Title / Artist        │  左对齐或居中，主副标题层级清晰
│   ──●────────  0:12 3:45   │  自定义/增强 range 样式
│      ⏮  ▶/❚❚  ⏭         │  主控制，播放键放大
│      🔁  🔀  ☰            │  纯图标次要控制
└────────────────────────────┘
```

### 视觉要点

- 去掉 `.player-header`「正在播放」；保留顶部 `padding-top: safe-area`
- 封面：`width: min(78vw, 340px)` 左右，圆角约 `24–32px`，强阴影
- 信息区：标题更大、副标题更淡；元数据扫描提示保留为小号文案
- 进度条：加高可点区域，轨道半透明，已播放段与滑块更亮
- 主控制：三键居中；播放键实心圆形强调
- 次要控制：三图标均分或居中间距；激活态提高不透明度/主色，未激活约 0.55–0.7
- 背景：继续 AMLL `BackgroundRender` + fallback；控制内容相对背景 `z-index` 更高

### 歌词页

- 保持现有 AMLL `LyricPlayer` 与空状态
- 仅必要时调整 panel padding / min-height，避免与新控制页安全区冲突
- 左右滑 `translateX(-50%)` 不变

### 宽屏

- 维持 `@media (min-width: 768px)` 双栏：左控制右歌词
- `transform: none !important` 覆盖窄屏滑动
- 控制页在左栏内垂直居中/靠上紧凑排布

## Interaction

| 手势/操作 | 行为 | 备注 |
|-----------|------|------|
| 下滑 | 关闭 overlay | 现有 vertical drag 逻辑保留 |
| 左右滑 | 控制页 ↔ 歌词页 | 仅窄屏；vertical 判定优先 |
| 循环图标 | `setRepeatMode(one/all)` | 图标 `repeat` / `repeatOutline` 区分 |
| 随机图标 | `toggleShuffle()` | 启用时高亮 |
| 队列图标 | `openQueueOverlay()` | 不改路由 |
| 进度 change | `seekPlayback(seconds)` | 保持 |

## Accessibility

- 图标按钮必须有 `aria-label`（上一曲、播放或暂停、下一曲、循环模式文案、随机模式文案、播放队列）
- 进度条保留 `aria-label="播放进度"`
- 不引入无标签的纯装饰可点击区域

## Test Impact

现有 `tests/unit/player.spec.ts` 需调整：

1. 空状态不再期望文案「返回」
2. 不再依赖可见文字「列表循环」「顺序播放」；改为断言 `aria-label` 或按钮存在
3. 封面 / seek / AMLL / 左右滑断言尽量保留

## Compatibility & Risk

| 风险 | 缓解 |
|------|------|
| 去掉顶部标题后用户找不到关闭方式 | 保留下滑 + 系统返回；spec 已约定无顶部返回按钮 |
| 纯图标导致测试/可读性回归 | `aria-label` + 更新单测 |
| 宽屏布局被窄屏 flex 改动破坏 | 保留并手测/单测宽屏 CSS 关键选择器 |
| AMLL 背景被新层遮挡 | 不改 `.amll-background` 全尺寸包裹约定 |

## Rollout / Rollback

- 单文件视觉改动为主；回滚即还原 `PlayerPage.vue` + 对应测试
- 无需数据迁移、无需原生发版特判

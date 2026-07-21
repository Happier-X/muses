# 修复跳转当前遮挡与播放控件 fill 图标

## Goal

修复两个 UI 问题：

1. **#43**：歌曲页「跳转到当前播放」后，当前歌曲行不再被顶部固定区域挡住。
2. **#45**：底部 MiniPlayer 与沉浸式播放页的播放主控图标改为实心（fill）样式，观感更重、更易识别。

## Background

### #43 跳转当前被挡住

- 先前已将 `scrollIntoView` 的 `block` 从 `center` 改为 `start`（任务 `07-21-jump-current-scroll-start`）。
- 现状：`src/views/SongsPage.vue` 使用  
  `row.scrollIntoView({ behavior: 'smooth', block: 'start', inline: 'nearest' })`。
- 问题：`block: 'start'` 对齐的是滚动容器内容区顶部；SongsPage 顶部有**粘性** `ion-header`（标题栏 +「随机播放全部」工具栏），以及 content 内 `collapse="condense"` 大标题区域。目标行滚到 start 后仍会被这些上方固定/粘性内容盖住。
- 用户感知：点 FAB 后「当前歌曲被上面的内容挡住了」。

### #45 播放控件应用 fill 图标

- 业务图标已统一走 `src/icons/ion-lucide.ts`（Lucide → ion-icon data-URI）。
- 适配层默认 Lucide outline：`fill: none` + `stroke: currentColor`。
- 底部播放条（`MiniPlayer.vue`）播放/暂停、沉浸式播放页（`PlayerPage.vue`）上一曲/播放暂停/下一曲目前均用 outline 版 `play` / `pause` / `playSkipBack` / `playSkipForward`。
- 用户要求这些**主控键**使用 **fill（实心）** 版图标。

## Related Issues

- https://github.com/Happier-X/muses/issues/43
- https://github.com/Happier-X/muses/issues/45

## Requirements

### R1. 跳转当前：目标行完整可见（#43）

- 点击 SongsPage「跳转到当前播放」FAB 后，当前播放歌曲行应完整出现在**列表可视区内**，不被顶部标题栏、「随机播放全部」工具栏或其他粘性/固定头部挡住。
- 优先保证行的**标题/主信息**可见；允许底部仍受 MiniPlayer / Tab Bar 既有 padding 约束（本任务不重做底部避让，沿用现有 `--padding-bottom` / FAB 定位）。
- 列表末尾几项：仍依赖滚动边界，无法再滚时停在可到达位置，不强行伪造置顶。
- 保留：`data-song-id` 定位、`jump-highlight` 轻高亮、`behavior: 'smooth'`（或等价平滑滚动）、FAB 显示条件不变。
- 实现可选用（任选其一生效即可，以验收为准）：
  - 对目标行设置足够的 `scroll-margin-top`（覆盖粘性头高度）；或
  - 在 `scrollIntoView` 之后/改为对 `IonContent` 滚动容器按测量偏移滚动。

### R2. 播放主控 fill 图标（#45）

- **范围（必须 fill）**：
  - `MiniPlayer`：播放 / 暂停
  - `PlayerPage` 主控制区：上一曲 / 播放暂停 / 下一曲
- **不在本任务强制 fill**：
  - 播放模式（shuffle / repeat / list）
  - 队列入口、返回、翻译、歌词页圆形 play/pause（`playCircle` / `pauseCircle`）等次级控件
  - 列表「播放全部」等非沉浸/底栏主控
- 图标仍须经 `ion-lucide` 适配层导出，禁止业务侧直接从 `ionicons/icons` 或裸 SVG 散落实现。
- fill 与 outline 若需并存：适配层可导出独立符号（如 `playFill`）或覆盖主控导出为 fill，并保证现有 import 语义清晰、无误用 outline 到主控。
- 尺寸、颜色继续由现有 CSS / `color` / `currentColor` 控制；不得引入 solid 圆底按钮阴影（主控仍为 `fill="clear"` 纯图标按钮）。

### R3. 规范与测试

- 同步 `.trellis/spec/frontend/component-guidelines.md` 中：
  - FAB 跳转滚动约定（说明需避开粘性顶栏，不能只写 `block: 'start'` 而不提遮挡）
  - 播放主控图标使用 fill 版的约定
- 更新/补充相关单测：
  - 跳转当前：若实现从纯 `scrollIntoView` 变为带偏移，单测断言需匹配新 API/调用方式
  - 图标：若有导出层单测则覆盖 fill 根属性；无则至少保证现有单测不因 import 变更失败

### Out of Scope

- **#44** 全局图标语义统一（本任务不扫全库同语义不一致）
- MiniPlayer 增加上一曲/下一曲按钮
- 虚拟列表、其它列表页（歌单详情等）的「跳转当前」能力
- 发版 / 改 versionCode

## Acceptance Criteria

- [x] #43：SongsPage 点「跳转到当前播放」后，当前歌曲行不被顶部粘性/固定区域挡住，主信息可读
- [x] #43：列表末尾目标行不报错，可滚到尽头；不强行置顶
- [x] #43：FAB 可见性、高亮、安全区定位行为与现网一致
- [x] #45：MiniPlayer 播放/暂停为 fill 图标
- [x] #45：PlayerPage 上一曲/播放暂停/下一曲为 fill 图标
- [x] 图标仍走 `src/icons/ion-lucide.ts`，无业务侧 ionicons 回退
- [x] component-guidelines 已同步上述约定
- [x] lint / 相关 unit 通过
- [ ] 关闭或评论关联 issue #43、#45（实现合并后）

## Notes

- 轻量偏中等：两个可独立验收的 UI 修复，合在同一任务交付。
- 需要 `design.md`（滚动偏移策略 + fill 导出策略）与简短 `implement.md` 后再 `task.py start`。
- 不创建 parent/child：改动面小、同一轮交付更合适。

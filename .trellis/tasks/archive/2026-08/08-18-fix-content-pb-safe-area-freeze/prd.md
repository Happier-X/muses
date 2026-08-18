# 手势导航时列表底部被 MiniPlayer 遮挡——`--m-content-pb` 在 `:root` 被 Chrome 提前冻结

## Goal

修复真机上启用手势导航（底部安全区 > 0）时，歌曲/专辑/艺术家/歌单等所有列表页面滚动到底时最后一项底部被迷你播放条（MiniPlayer 胶囊）遮挡的问题。

## 现象

- 三键导航（底部安全区 = 0）时正常。
- 启用手势提示线后出现：列表滚动到底，最后一项约一个手势条高度（真机约 16–24px）的一小块被 MiniPlayer 盖住。
- 用户观察：MiniPlayer 正确避让了底部安全区（上移），但列表内容区域没有同步避让。

## 根因（已用 headless Edge + CDP 模拟手势导航实测复现）

- 全局内容止位 token 定义在 `:root`：

  ```css
  :root {
    --m-safe-area-bottom: 0px;   /* :root 默认值 */
    --m-content-pb: calc(72px + var(--m-safe-area-bottom, 0px));  /* 引用 --m-safe-area-bottom */
  }
  html.muses-mini-visible {
    --m-content-pb: calc(72px + var(--m-safe-area-bottom, 0px));  /* 同款重复定义 */
  }
  .m-app {
    --m-safe-area-bottom: var(--safe-area-inset-bottom, env(safe-area-inset-bottom, 0px)); /* 真实桥接 */
  }
  ```

- Chromium 在计算 custom property 时，若其 `var()` 依赖在**同一元素（:root）作用域内可解析**，会立即替换并冻结为字面值：
  `--m-content-pb` 的计算值被冻结为 `calc(72px + 0px)`，作为常量向所有后代继承。
- `.m-app` 上桥接出的真实 `--m-safe-area-bottom`（手势导航时 = 手势条高度）**再也无法影响** `--m-content-pb` → 列表 padding-bottom 恒等于 72px。
- 而 MiniPlayer 直接消费 `var(--m-safe-area-bottom, 0px)`（不经过中间 token，在自身作用域解析，能拿到 `.m-app` 桥接的真实值）→ 正确上移。
- 两处一边上移、一边冻结不动，列表底部漏出与手势条高度等高的内容被胶囊盖住。

## 实测数据（headless Edge + CDP 注入 `--safe-area-inset-bottom: 24px`）

| 场景 | 列表 padding-bottom | MiniPlayer bottom | 最后一行 bottom vs MiniPlayer top |
|---|---|---|---|
| 修复前（手势导航） | 72px（漏安全区） | 32px（正确上移） | overlap = +24px（被盖） |
| `--m-content-pb` 重定义到 `.m-app` | 96px | 32px | overlap = 0（修复） |
| 三键导航（safe=0）修复前对照 | 72px | 8px | overlap = 0（正常） |

## Requirements

- 内容止位 token（`--m-content-pb` / `--m-content-pb-md`）的**定义作用域移到 `.m-app`**（与 `--m-safe-area-*` 桥接、`--m-navbar-pt` 同处），使 `var(--m-safe-area-bottom, 0px)` 在该作用域立即解析为真实安全区值，覆盖全部消费页面。
- 移除/简化 `:root` 与 `html.muses-mini-visible` 中的冻结定义；`:root` 保留兜底默认并加注释说明 `.m-app` 覆盖。
- 不引入新魔法数字；MiniPlayer 定位样式不动；其他直接消费 `--m-safe-area-bottom` 的位置（多选条/索引条/抽屉/QueuePage 等）不动——它们不经中间 token，不在本 bug 影响范围。
- 同步更新 spec 底部几何契约（.trellis/spec/frontend/component-guidelines.md 中「安全区处理」「底部几何契约」条目）说明该冻结陷阱与定义位置约定。

## Acceptance Criteria

- [ ] `src/theme/index.scss`：`.m-app` 块内新增 `--m-content-pb` / `--m-content-pb-md` 定义（`calc(72px + var(--m-safe-area-bottom, 0px))`），`:root` 与 `html.muses-mini-visible` 中的同款定义移除或加注释澄清。
- [ ] 全项目仍只有 7 个 view 消费 `--m-content-pb`，无页面级新增 padding 改动。
- [ ] 构建通过（`vue-tsc` / `vite build` / `eslint` 无新错误，`vitest` 相关用例通过）。
- [ ] headless 复现实验回归：注入 `--safe-area-inset-bottom: 24px` 后歌曲页列表 padding-bottom = 96px、最后一项与 MiniPlayer 重叠 = 0；注入 0px 时保持 72px / 重叠 0 不变。
- [ ] 真机验证（手势导航）：歌曲/专辑/艺术家页列表滚动到底最后一项完整可见。

## Notes

- 轻量修复任务，PRD-only，不需要 design.md / implement.md。
- 08-16 曾修过一次同类问题（列表漏算 MiniPlayer 8px 悬浮空隙，commit 7448833）：当时 safe-area=0 场景两处一致，未暴露本冻结缺陷；本次是安全区非零场景暴露的更深一层问题。
- Chromium 冻结行为适用于「var() 依赖在变量定义同元素作用域内可解析」的情况；`.m-app` 中 `--m-navbar-pt` 一直用「定义在 .m-app」的模式所以从未有此问题，本次修复只是把内容止位 token 归入同一模式。
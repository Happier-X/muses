# PRD：MiniPlayer 改用 k-glass 组件

## 背景

用户指出："miniplayer 可以用 konsta ui 的 glass 组件呀"。MiniPlayer 外层原本手写 6 个玻璃类（bg-ios-light-glass / shadow-ios-light-glass / backdrop-blur-lg / dark:bg-ios-dark-glass / dark:shadow-ios-dark-glass），正是 k-glass 组件的默认配方，改由官方组件承载。修复已完成于 commit 0ba585f，本任务为留档补记。

## 改动内容（src/components/MiniPlayer.vue）

- 外层 `<div>` → `<k-glass component="div" class="...">`，删除手写玻璃/阴影/模糊类（k-glass 默认提供：半透明白玻璃 + 玻璃内阴影 + blur(16px) + dark 反色）
- 保留：`rounded-full`（k-glass 无圆角）、fixed 定位、flex 布局、`:class` 动态类（cursor/is-empty）、role/tabindex/aria-label/aria-disabled、@click/@keyup 事件
- import 增加 kGlass（@/components/ui 已导出）

## 验收（全部通过，模拟器 CDP 实测）

- [x] k-glass 渲染类含官方玻璃配方（bg-ios-light-glass + shadow-ios-light-glass + backdrop-blur-lg + dark 反色）
- [x] computed：背景 rgba(255,255,255,0.75)、blur(16px)、fixed、role=button、胶囊圆角
- [x] 点击 MiniPlayer 正常打开沉浸播放页（touch-action:none 不影响点击）
- [x] npm run build 通过

## 备注

- k-glass 默认 `touch-none`（touch-action:none）——官方玻璃组件行为，不影响 click
- 排查注意：`document.querySelector('.k-glass')` 会先匹配到 k-actions 菜单内的玻璃，定位组件需用 aria-label 或业务类
- tabbar 保持渐变透出（Konsta iOS 默认设计），未随本次统一为玻璃卡片
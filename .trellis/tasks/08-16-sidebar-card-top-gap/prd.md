# PRD：侧边栏卡片顶部留间距

## 需求

用户反馈：侧边栏卡片距离上面应该有一个距离，就像 navbar 一样。

- drawer 顶部 padding 从 `var(--m-safe-area-top, 0px)`（卡片紧贴状态栏下沿）
- 改为 `calc(var(--m-navbar-pt, 16px) + var(--m-spacing, 16px))`：
  - 浏览器（safe-area=0）：`--m-navbar-pt` = 16px → 顶部 32px
  - 真机：`--m-navbar-pt` = 状态栏高 → 状态栏下方再留 16px 空隙（navbar 同样的避让 token 口径 + 悬浮间距）
- 底部/左右空隙不变；其余契约不变

## 验收

- [ ] MuMu 真机：卡片顶部与状态栏之间有可见空隙（约 16px）
- [ ] 浏览器：顶部 32px 空隙
- [ ] 深色/推屏/手势无回归
- [ ] lint / type / build 通过

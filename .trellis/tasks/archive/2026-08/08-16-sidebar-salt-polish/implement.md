# Implement：侧边栏对齐椒盐音乐并美化

## 改动文件

| 文件 | 改动 |
|---|---|
| `src/views/TabsPage.vue` | 侧边栏（drawer + aside）样式对齐椒盐：header 区、行高/间距/文字、分组、激活态 |
| `src/composables/useSystemDark.ts` | 扩展为三态主题模式（跟随系统/亮/暗），持久化 + 返回 cycle 能力 |
| `src/icons/index.ts` | 新增 Sun / Moon / Monitor 导出（外观按钮用） |

## 实施步骤（有序）

1. **useSystemDark 三态改造**
   - 读 `localStorage['muses-theme']`（缺省 'system'）
   - system：监听 prefers-color-scheme 同步 .dark（现状逻辑）
   - light：移除 .dark；dark：添加 .dark（均不监听）
   - 导出 `themeMode`（Ref）/ `cycleThemeMode()`（system→light→dark→system，写存储并应用）
   - 兼容 SSR/无 window 兜底；`matchMedia` 不存在退化为 light

2. **TabsPage 布局改造**（drawer 与 aside 共用样式）
   - 新增 div header：`✕`（closeDrawer）、外观按钮（图标+label 由 themeMode 决定）、`⚙️`（router.push('/tabs/settings')）
   - nav 菜单项：行高 64dp；图标容器固定 60dp 宽（24dp 图标居中）；文字 77dp 起；主区 4 项 + 次区 2 项（组间留白 18dp）
   - 激活态：去蓝底，改图标 `--m-primary` + 文字加粗；保留 :active 按压微反馈
   - aside（平板）同构复用；height/overflow 维持现状

3. **验证**
   - `npm run lint`、`npx vue-tsc --noEmit`、`npm run build`
   - 桌面浏览器：开抽屉看 header/分组/间距；平板宽度切换断点；暗色切换三态
   - MuMu 真机流程：build → cap sync → assembleDebug → adb 安装 → 开抽屉截图对照椒盐

## 回滚点

- 单文件为主：`TabsPage.vue` 样式回滚即还原
- 主题三态：useSystemDark 若出问题可整体 git revert（main.ts 调用点不变，旧签名兼容）

## 检查清单

- [ ] header 三按钮可用（关闭/外观/设置跳转）
- [ ] 菜单 64dp 行高 + 文字 77dp 左距 + 主次分组
- [ ] 激活态无蓝底
- [ ] 深色模式无回归
- [ ] 平板 aside 一致
- [ ] 三态主题循环 + 持久化
- [ ] lint / type / build 通过
# Implement: 歌曲页行内按钮对齐椒盐 + 全局边距修复

## 执行顺序（每步独立验证，可单独回滚）

### 阶段 1：全局 body margin 修复（D1）
- [ ] 1.1 `src/theme/index.scss` body 规则追加 `margin: 0`
- [ ] 1.2 构建 + 安装模拟器，CDP 验证 `bodyMargin=0`、`#app x=0 w=360`
- [ ] 1.3 走查各页面（专辑/艺术家/歌单/播放器/设置/音源）+ MiniPlayer 布局无异常
- 提交：fix(theme): 重置 body 默认 margin，页面满屏对齐椒盐

### 阶段 2：圆形按钮交互区 + 视觉（D2/D5）
- [ ] 2.1 round-btn 44x48px 交互区（透明）+ `::before` 14px 视觉圆（#ECECEC）
- [ ] 2.2 图标 stroke-width 3 + 颜色 #949fab（浅色）/ rgba(225,230,235,.55)（深色）
- [ ] 2.3 is-playing 状态保持（主色 12% 圆底 + 主色图标）
- [ ] 2.4 CDP 验证 round 44x48、视觉圆 14 居中
- 提交：feat(songs): 圆形按钮交互区扩大至椒盐 44x48dp

### 阶段 3：更多按钮实心三点（D3）
- [ ] 3.1 more-btn 36x48px 交互区，模板改实心三点（3.5px 点 + 2px gap）
- [ ] 3.2 移除 lucide ellipsis-vertical 图标依赖（若不再使用）
- [ ] 3.3 CDP 验证 more 36x48、三点总高 ~14px
- 提交：feat(songs): 更多按钮对齐椒盐实心三点

### 阶段 4：行内横布局对齐（D4）
- [ ] 4.1 __row-actions gap 0（圆/⋮ 紧挨），文字区 flex:1
- [ ] 4.2 截图/CDP 验证行 1：封面 x16 → 圆交互区 x264-308 → ⋮ x308-344（物理 ×3 对照椒盐）
- [ ] 4.3 标题长行截断表现（ellipsis）正常
- 提交：feat(songs): 行内按钮横布局对齐椒盐

### 阶段 5：全量验收
- [ ] 5.1 `npm run build` / `npm run lint` 全绿
- [ ] 5.2 CDP 全项复测（AC2-AC4）
- [ ] 5.3 模拟器截图对比椒盐（行 1 同屏）
- [ ] 5.4 交互回归：加队列 toast、更多菜单、多选、正在播放指示、搜索/排序切换
- 提交：chore(songs): 行内按钮对齐收尾

## 验证命令

```bash
npm run build            # vue-tsc + vite build
npm run lint             # eslint .
npx cap sync android
adb -s emulator-5556 install -r android/app/build/outputs/apk/debug/app-debug.apk
# CDP 实测：
#   adb forward tcp:9333 localabstract:webview_devtools_remote_<pid>
#   node .tmp/cdp_probe.cjs   （改探针表达式读 bodyMargin/按钮 rect）
adb -s emulator-5556 exec-out screencap -p > .tmp/check.png   # 截图对比椒盐
```

## 风险与回滚

- body margin 修复影响全局 → 若某页面出现依赖旧边距的布局问题，回滚为仅 SongsPage 内补偿（最坏方案），但预期不需
- round-btn 用 `::before` 画视觉圆需注意 z-index 与 flex 居中；若复杂，改为内层 span 容器
- ⋮ 实心点 i 元素无内容，注意 aria-hidden 与无障碍（按钮已有 aria-label）
- 每阶段独立提交可回滚；核心文件 SongsPage.vue + theme/index.scss

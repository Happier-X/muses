# Implement: 沉浸式播放页 1:1 复刻椒盐

## 执行顺序（每步独立验证）

### 阶段 1：信息面板布局重构（D1）
- [ ] 1.1 PlayerPage.vue 信息面板模板重构：新增 topbar（返回+⋮）+ song-head（歌手+歌名大字）移到顶部，cover 区改为 cover-rotator，进度/控制/模式栏保持底部
- [ ] 1.2 调整样式：song-head 歌名 44px 暖白、artist 14px；cover-rotator flex:1 弹性区
- [ ] 1.3 验证：`npm run build` + 模拟器查看布局无溢出、歌词面板滑动正常
- 提交：feat(player): 信息面板布局重构——歌名顶部大字 + 顶部导航

### 阶段 2：封面旋转方形（D2）
- [ ] 2.1 cover-img 加 `transform: rotate(-45deg)` + 圆角 18px + 阴影
- [ ] 2.2 CDP 截图对比椒盐，微调角度（30-45°）与尺寸（min(58vw,260px)）
- [ ] 2.3 验证：封面倾斜显示，无溢出
- 提交：feat(player): 封面旋转方形对齐椒盐

### 阶段 3：顶部导航完善（D4）
- [ ] 3.1 返回按钮触发 closePlayerOverlay()；右侧 ⋮ 触发现有 actions
- [ ] 3.2 用 MIconButton（半透明涟漪）+ ChevronLeft 图标
- [ ] 3.3 验证：返回关闭播放页、⋮ 打开菜单
- 提交：feat(player): 顶部导航返回+更多按钮

### 阶段 4：控制行对齐（D5）
- [ ] 4.1 播放按钮 64px → 72px
- [ ] 4.2 CDP 对比椒盐控制行位置
- [ ] 4.3 验证：播放/暂停/上下一曲无回归
- 提交：feat(player): 播放按钮加大对齐椒盐

### 阶段 5：全量验收（对照 AC1-AC9）
- [ ] 5.1 `npm run build` / `npm run lint` 全绿
- [ ] 5.2 CDP 实测布局（歌名位置/字号、封面角度、控制行）
- [ ] 5.3 截图与椒盐对比（整体视觉）
- [ ] 5.4 交互回归：播放/暂停、进度拖动、歌词滑动、下滑关闭、编辑歌曲
- [ ] 5.5 其他页面无回归
- 提交：chore(player): 椒盐沉浸式播放页收尾

## 验证命令

```bash
npm run build
npm run lint
npx cap sync android
adb -s emulator-5556 install -r android/app/build/outputs/apk/debug/app-debug.apk
node .tmp/cdp_probe.cjs  # CDP 验证布局坐标
```

## 风险与回滚

- 布局重构影响歌词面板滑动 → panels 容器不变，验证 1.3
- 旋转封面溢出 → overflow: visible + 尺寸微调
- 歌名长文本换行 → nowrap + ellipsis
- 每阶段独立提交可回滚

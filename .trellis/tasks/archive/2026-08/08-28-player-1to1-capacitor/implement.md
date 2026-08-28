# 实施计划 — 沉浸式播放页一比一复刻 Capacitor

## 清单（按序）

1. **现状对齐复核**（无代码改）
   - [x] 读取 `git show de7e388f^:src/views/PlayerPage.vue` 全量 BEM 与交互，标注与当前 `PlayerScreen.kt` 的映射（已完成，见 PRD R1/R2）
   - [x] MuMu 真机截图对比（`mumu_melox.png` 封面+小窗 / `mumu_melox2.png` 完整歌词）确认主流程已 1:1

2. **查漏补缺（按需，最小改）**
   - [ ] 校验 `drag-layer` 下滑仅 `info-panel` 生效、歌词面板内禁止关闭的细节（当前简化为始终允许，需按旧版加 `activePanel==0` 守卫）
   - [ ] 校验 `progress-range` 禁用态与 `canSeek` 语义（当前未禁用，需补 `enabled = duration>0`）
   - [ ] 校验 `MetaWindow` 切行是否需保留 `spring` 手感（当前为 `tween 260ms`，必要时切 `spring(stiffness 240,damping 26)`）
   - [ ] 校验 `navigationBars` 底部安全区（当前 `12dp` 固定，必要时补 `WindowInsets.navigationBars`）

3. **验证**
   - [ ] `./gradlew :feature:player:assembleDebug :app:assembleMusesDebug` 通过
   - [ ] MuMu 手动：手机竖屏 / 平板横屏 / 窄屏 / 空态 四态截图
   - [ ] 横滑 0.22s、FAB 3s 隐藏、逐词高亮、粘性封面 四项交互录屏抽检

4. **文档与收尾**
   - [ ] 若有改动，同步 `.trellis/spec/android/features-lyrics-playlist.md §7` 与 `index.md` 播放契约
   - [ ] `prd.md` 验收项打勾，`python .trellis/scripts/task.py archive` 归档

## 验证命令

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :feature:player:assembleDebug :app:assembleMusesDebug
# 真机
adb -s emulator-5556 install -r app/build/outputs/apk/muses/debug/app-muses-debug.apk
adb -s emulator-5556 exec-out screencap -p > /tmp/mumu_check.png
```

## 风险与回滚点

- **风险文件**：仅 `feature/player/PlayerScreen.kt` 与两新增叶子组件；改动半径极小
- **回滚**：`git restore feature/player/src/main/kotlin/com/muses/player/feature/player/PlayerScreen.kt` + 删除 `backdrop/` 增量即可，无 DB 迁移
- **不确定性**：旧版 `meshGradientRenderer` 真实流体与当前 `Canvas Blob` 为近似实现，已与 MeloX 效果验收一致，无需像素级还原

## 评审门槛

- 所有改动须保留 `// BEM → Compose 映射` 注释可追溯
- 验收以 PRD 四项 Acceptance 为准，编辑表单与 actions 弹窗不计入

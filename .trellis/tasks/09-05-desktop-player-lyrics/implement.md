# 桌面播放页歌词执行计划

## 有序清单

- [ ] Y1 SimpleLyricsPanel 上收：迁 core:common jvmShared 同包名；安卓 feature:player 透传；验证 `:app:assembleMusesDebug + :core:common:assemble`。
- [ ] Y2 桌面数据链：DesktopPlayerHook 读库歌词→解析→StateFlow；播放页歌词面板渲染+进度联动。
- [ ] Y3 在线搜索：无库歌词时 LyricsMatcher 补充链（复用 DesktopScrapeGraph 网络单例）；展示命中结果。
- [ ] Y4 收尾：AC 全勾 + 三端回归 + 归档。

## 验证命令

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :core:common:assemble :core:common:allTests :app:assembleMusesDebug testDebugUnitTest :composeApp:compileKotlinJvm
```

## 风险文件

- `SimpleLyricsPanel.kt`（上收移动，安卓行为冻结）。
- `composeApp PlayerScreen.kt`（布局升级，控制区行为不变）。

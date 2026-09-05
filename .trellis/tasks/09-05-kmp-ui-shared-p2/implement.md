# 界面共用二期执行计划

## 有序清单

- [ ] V0 规范清单：implement.jsonl/check.jsonl 补 spec 条目；task.py start。
- [x] V1 T2 抽象：SaltShadowTokens 上收 + saltShadow expect/actual（安卓 BlurMaskFilter 原实现/桌面简化单层）；GlassSurface 删 import 上收；MusesHaze 删除切 LocalHazeBlurState；7 处调用方切换；三端编译通过。
- [x] V2 播放页非歌词部分上收（PlayerProgress 含手势铁律/PlayerControls+ModeBar/PlayerCoverHero）；歌词面板留待 Haze 降级验收（用户验收后定，见 prd AC2）。
- [x] V3 刮削页共用化（决策=上）：ScrapeComponents 共用组件 + 安卓两屏接入 + 桌面 ScrapeScreen（队列真实可用；匹配/写回链经 :core:scrape 安卓库边界，回调注入点预留）。
- [x] V4 收尾：AC 全勾 + 三端全量回归通过 + 归档。

## 验证命令

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :core:ui-shared:assemble :app:assembleMusesDebug :composeApp:compileKotlinJvm
```

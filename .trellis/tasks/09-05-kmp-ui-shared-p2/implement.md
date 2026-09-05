# 界面共用二期执行计划

## 有序清单

- [ ] V0 规范清单：implement.jsonl/check.jsonl 补 spec 条目；task.py start。
- [ ] V1 T2 抽象：SaltShadowTokens 上收 + saltShadow expect/actual；GlassSurface 删 import 上收；MusesHaze 删除切桥接；验证双端编译。
- [ ] V2 播放页非歌词部分上收（封面/进度/控制/模式栏）；歌词面板等 Haze 降级验收。
- [ ] V3 刮削页产品决策（上/不上）→ 执行或书面结论归档。
- [ ] V4 收尾：AC 全勾 + 全量回归 + 归档。

## 验证命令

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :core:ui-shared:assemble :app:assembleMusesDebug :composeApp:compileKotlinJvm
```

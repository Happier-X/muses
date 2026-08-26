# 执行清单 — 平板双栏完善

> 验证：`cd native && JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew <targets>`；MuMu 需切平板分辨率或用 1280×800 设备定义

- [x] 1. 盘点：grep 全部 .vue 的 min-width:768px 覆盖段 + TabsPage isTablet 分支，产出差距清单
- [x] 2. TabsLayout 宽屏细节核对修正
- [x] 3. 各列表页宽屏覆盖翻译（content-pb-md 等）
- [x] 4. PlayerPage 平板分支：payload 加 isTabletLayout + 前端适配
- [x] 5. MiniPlayer 宽屏语义
- [x] 6. 门禁 + MuMu 平板分辨率装机（lint 失败项仅红线文件 core/media/PlaybackModule，非本任务引入；装机待用户）
- [ ] 7. 用户逐页验收

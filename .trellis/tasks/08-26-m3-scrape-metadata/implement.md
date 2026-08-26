# 执行清单 — M3 UI 接线

> 验证：`cd native && JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew <targets>`

- [ ] 1. feature:scrape 模块骨架（build.gradle.kts + Hilt）+ NavDestination.Scrape 接 ScrapeScreen
- [ ] 2. queue 态：ScrapeQueueStore 列表 + 全部开始/单曲移除；歌曲页 ⋮ 菜单与多选条「加入刮削队列」
- [ ] 3. matching→preview→result 三态：匹配进度流、候选预览确认、结果列表 + journal 撤销
- [ ] 4. EditMetaSheet（editmeta 三维编排）+ PlayerWebView `openEditMeta` 桥动作接线
- [ ] 5. 自动补缺调度：扫描完成后按条件入队（DataStore 开关默认关）
- [ ] 6. 门禁全绿 + 装机
- [x] 7. MuMu 实测 ✅ 2026-08-26 用户验收通过（标记/入队/四态流转/写回/编辑云搜弹窗全部正常）

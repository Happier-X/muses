# M3 UI 接线 — 刮削页/云编辑/自动补缺调度

> 前置：`08-25-native-m3-scrape-engine` 已交付 core:scrape 数据层全量（五源文本链/六源封面链/写回五步+回滚 journal/队列历史/可疑检测/editmeta 三维编排），spec 见 `features-scrape-engine.md`。本任务 = **UI 接线 + 触发调度 + 链路实测**。Web 规格书 `src/views/ScrapePage.vue`（1390 行）+ 歌曲页刮削入口 + PlayerPage 编辑信息入口。

## Goal

把 core:scrape 能力接到 UI：刮削页四态流程（队列→匹配→预览→结果）、歌曲页标记入口、播放页编辑信息云搜弹窗、扫描后自动补缺调度；并实测 WebDAV 写回全链路（username 持久化已补）。

## 范围

1. **ScrapePage 刮削页**（替换导航占位）：pageState 四态机
   - queue：待刮削列表（来源=歌曲页标记）+ 全部开始/移除按钮
   - matching：进度条 + 当前匹配项
   - preview：候选预览确认（文本元数据+封面网格）
   - result：逐行 success/file-failed/failed 结果 + 撤销入口
2. **歌曲页标记入口**：⋮ 菜单加「加入刮削队列」；多选操作条批量加入
3. **编辑信息云搜弹窗**（editMeta 三维编排）：入口=播放页 WebView「更多」键（新增桥动作 openEditMeta 打开原生弹窗）
4. **自动补缺调度**：扫描入库完成后，对 title=文件名回退（tagsVersion<1 或 metaSources 缺失）的歌曲自动入队（可配置开关，默认关）
5. **写回实测**：WebDAV 文件标签写回 + 回滚验证（MuMu 真实源）

## Out of Scope（后续任务）

- 平板双栏布局完善、设置页增项（如与 Web 有差异另立小任务）
- 歌词 provider 聚合接线（LyricsSearchPort 注入方仍为空时 editmeta 歌词维度降级跳过）

## Acceptance Criteria

- [ ] 刮削页四态流转与 Web 版一致；队列/历史持久化重启不丢
- [ ] 歌曲页标记 → 刮削页可见 → 开始 → 匹配进度 → 预览确认 → 写回成功
- [ ] WebDAV 源写回后文件标签真实变更（重新扫描读出新标签）；撤销恢复旧值
- [ ] 播放页「更多」→ 编辑信息弹窗三维云搜可用
- [ ] `lintDebug testDebugUnitTest :app:assembleMusesDebug` 全绿

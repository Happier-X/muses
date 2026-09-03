# 实施计划：刮削预览可见匹配结果并支持编辑

## 任务分解

- [ ] **T1 模型扩展** — `ScrapeViewModel.PreviewCandidate` 增加 `currentTitle/currentAlbum` 与 `editTitle/editArtist/editAlbum`，补充 `resolved*()` helper，更新三处构造点
- [ ] **T2 编辑能力** — `updatePreviewItem` / 回退逻辑，单测
- [ ] **T3 预览行重构** — `ScrapeScreen.PreviewStateContent` 改为原/新分段 + 封面缩略 + 编辑按钮
- [ ] **T4 编辑弹层** — `PreviewEditSheet` BottomSheet 及接线
- [ ] **T5 写回对接与开关** — `confirmWriteback` 改用 `resolved*`，验证 checked 守卫不变
- [ ] **T6 回归与验收** — AC1-5 手测与 lint/tests 回归

## 依赖顺序

T1 → T2 → T3 → T4 → T5 → T6。T3/T4 可并行于 T2。

## T1 模型扩展

**文件**：`feature/scrape/ScrapeViewModel.kt`

**步骤**：
1. 扩展 `PreviewCandidate` 字段与默认值
2. `startMatching`/`retrySingle`/`retryThrottled` 构造时填充 `currentTitle = song.title`、`currentAlbum = song.album`
3. 提供 `resolvedTitle()` 等扩展或成员函数
4. 验证：编译通过 `assembleMusesDebug`

**回滚点**：移除新增字段即回滚

## T2 编辑能力

**文件**：`feature/scrape/ScrapeViewModel.kt`

**步骤**：
1. 新增 `fun updatePreviewItem(songId: String, title: String?, artist: String?, album: String?)`：
   `copy(editTitle = title, editArtist = artist, editAlbum = album)`（空串已在调用方转 null）
2. 单测：构造 preview → update → 断言 resolved 为新值；取消路径（不调）保持原值
3. 验证：`testDebugUnitTest`

**回滚点**：删除方法与调用点

## T3 预览行重构

**文件**：`feature/scrape/ScrapeScreen.kt`

**步骤**：
1. 行内由单匹配行改为两段：`原：` + `新：` 各一行，`新` 行带 `[confidence]` badge
2. 右侧增加封面 `AsyncImage` 40dp + `编辑` 按钮
3. 复用 `Salt` 颜色与圆角，保持勾选/全选交互不变
4. 验证：MuMu 手测可见对比

**回滚点**：恢复旧行布局

## T4 编辑弹层

**文件**：`feature/scrape/ScrapeScreen.kt`（新增 `PreviewEditSheet`）或 `feature/scrape/PreviewEditSheet.kt`

**步骤**：
1. `ModalBottomSheet` + 三 `OutlinedTextField` + 取消/确认
2. `onConfirm` 回调 `updatePreviewItem`
3. 接线到 `PreviewStateContent` 的编辑按钮
4. 验证：编辑确认后行内新值同步变更

**回滚点**：移除弹层与按钮

## T5 写回对接

**文件**：`feature/scrape/ScrapeViewModel.kt`

**步骤**：
1. `confirmWriteback` 改 `ScrapeChanges(title = item.resolvedTitle(), ...)`
2. 保持 Writing/Result 流程与 `any{checked}` 守卫
3. 验证：编辑后勾选写回，库内为编辑值

**回滚点**：恢复 `matched*`

## T6 回归与验收

**验证命令**：
```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug :app:lintMusesDebug :feature:scrape:testDebugUnitTest
```

**手测清单**：AC1 可见、AC2 编辑、AC3 写回、AC4 取消、AC5 回归

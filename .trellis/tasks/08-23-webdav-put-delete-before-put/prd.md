# PRD：WebDAV 写回先删后传避免网盘重名新文件

## 目标

刮削回写 WebDAV 后，网盘上是原文件名的更新版本，而不是「xxx (1).flac」之类的重名新文件。

## 根因分析

- 客户端已向同一文件地址发标准 HTTP PUT（WebDAV 语义=覆盖），地址正确
- 但用户链路为 openlist 网关 → 夸克网盘：夸克 API 不支持覆盖上传，openlist 转传时网盘侧自动重命名 → 产生「哀人 (i) (1).flac」等新文件
- 这是网盘后端行为；客户端唯一可靠解法是先删后传

## 需求

1. **R1 先删后传（原生 WebDavPlugin.kt writeMetadata PUT 阶段）**：
   - PUT 前对同一 url 发 `DELETE` 请求（Basic Auth 同现有）
   - DELETE 返回 404 视为正常继续；其他非 2xx 也仅记录、不阻断（部分网关对目录型资源或权限差异可能拒绝删除，此时退回纯 PUT 行为）
   - DELETE 后再执行现有 PUT
2. **R2 失败可恢复性说明（不改代码，验证点）**：DELETE 成功但 PUT 失败时，本地缓存仍持有写好标签的完整副本，重试写回即可恢复——在任务 notes 记录该边界

## 验收标准

1. gradle assembleDebug 通过
2. MuMu 真机：对 WebDAV 歌曲刮削回写成功后，网盘目录中原文件名更新、无「(1)」重名新文件
3. 回归：本地文件写标签不受影响

## 范围外

- 不改前端与 writeback.ts
- 不改下载/渐进缓存逻辑

## 约束

- Kotlin 改动仅限 WebDavPlugin.kt
- 密码不进日志/异常信息

# PRD：写回改为临时名上传加 MOVE 原子覆盖

## 目标

消除先删后传的失败窗口（DELETE 后 PUT 失败 + 本地缓存丢失 = 文件永久丢失）：改为「临时名上传 → MOVE 原子覆盖」，文件消失窗口缩到服务端单次改名操作。

## 背景

- 现状（08-23-webdav-put-delete-before-put）：PUT 前先 DELETE 原地址。若 PUT 失败且本地缓存丢失，云端旧版已删、无副本可恢复
- WebDAV 标准事务化做法：上传到临时资源（不碰原文件）→ `MOVE` 临时资源到目标地址并带 `Overwrite: T` 头（服务端原子改名/替换）
- openlist 网关对夸克驱动支持同目录重命名，MOVE 大概率可用；不支持时回退现有先删后传

## 需求

1. **R1 主流程改造（WebDavPlugin.kt writeMetadata 上传阶段）**：
   - PUT 到临时地址 `${url}.muses-tmp`（Basic Auth 同现有）
   - PUT 成功后发 `MOVE`：请求头 `Destination: <原 url>`、`Overwrite: T`
     - MOVE 2xx → 成功
     - MOVE 非 2xx（网关不支持等）→ 回退执行「DELETE 原 url（404 正常）+ PUT 原 url」的现有逻辑
2. **R2 清理**：MOVE 失败回退路径结束后，确保临时资源不残留——回退 DELETE 阶段顺带 DELETE `.muses-tmp` 地址（404 忽略）
3. **R3 日志**：各阶段结果（tmp put/move/fallback delete/put）用 Log 记录状态码，密码不进日志

## 验收标准

1. gradle assembleDebug 通过；前端 lint/test/build 不回归
2. MuMu 真机：WebDAV 歌曲刮削回写成功后，网盘上原文件名内容更新、无 `.muses-tmp` 残留、无重名新文件
3. 边界验证：回退路径仍能完成写回（可用不支持 MOVE 的源或模拟）

## 范围外

- 不改前端、writeback.ts、缓存逻辑

## 约束

- Kotlin 改动仅限 WebDavPlugin.kt
- 密码不进日志/异常信息

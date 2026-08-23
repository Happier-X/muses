# 任务 Notes：WebDAV 写回先删后传

## 实现摘要（08-23）

- 仅改 `android/app/src/main/java/com/muses/player/WebDavPlugin.kt` 的 `writeMetadata`：
  在现有 PUT 之前对同一 url 先发 `DELETE`（Basic Auth 同现有）。
  - DELETE 404 视为正常继续；其他非 2xx 或网络异常仅 `Log.w` 记录、不抛异常、不阻断，退回纯 PUT 行为。
  - 最终成败仍以 PUT 结果判定（`put_failed` 等原有错误码不变）。
  - 日志只含 HTTP code / 异常类型，密码不进任何日志或异常信息。

## R2 失败可恢复性边界（验证点，不改代码）

DELETE 成功但后续 PUT 失败时，网盘上原文件已被删除、远端暂时无文件；
此时本地音频缓存仍持有「已写好标签」的完整副本（PUT 成功前不会覆盖缓存，
工作副本 `*.write-tmp.*` 在 finally 中清理）。用户重试刮削写回即可恢复：
缓存命中 → 免下载 → 直接重打标签并 PUT。前端 file-failed 重试链路
（failure-copy + 幂等覆盖写）天然支持此恢复路径。

## 验证结果

| 检查 | 退出码 |
|------|--------|
| `gradlew assembleDebug` | 0 |
| `npm run lint` | 0 |
| `npm run test:unit`（131 用例） | 0 |

真机验收（MuMu 网盘无「(1)」重名新文件、本地文件回归）待主会话执行。

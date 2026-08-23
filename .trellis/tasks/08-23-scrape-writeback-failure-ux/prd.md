# PRD：完善刮削回写失败提示与重试

## 目标

刮削写回（尤其 WebDAV 音源）失败时，让用户看到**具体可行动的失败原因**，并且所有失败都能一键重试。

## 现状事实（代码勘察）

### 失败码已有细分，但 UI 没用上

- 原生层（`android/.../WebDavPlugin.kt` writeMetadata）已返回细粒度 code：`missingUrl` / `missingCredentials` / `download_failed`（下载缓存失败）/ `empty_file` / `put_failed`（含 HTTP 码文案）/ `AudioMetadataException.diagnosticCode` / `write_failed`
- 前端 writeFile 层另有：`no_password`（配置/密码缺失）、`not_implemented`
- **问题 1**：ScrapePage.vue:613 对所有 file-failed 一律显示笼统的「文件写入失败，值已入库（来源：云端）」，具体 message/code 被吞掉，用户无法区分密码错/网络断/上传失败
- **问题 2**：「重试失败项」（ScrapePage.vue:643-651）只收集 `status === 'failed'` 的行；file-failed（最常见=网络抖动）不能重试
- **问题 3**：失败汇总只有数量（⚠ N 文件失败 / ✗ N 失败），无原因归类

### 相关既有语义（不可破坏）

- file-failed 时值仍入库、来源标 `'scrape'`（features-scrape.md 写回编排契约）
- 回滚 journal、WebDAV 串行/本地并行的写回顺序
- 撤销功能与 journal 结构

## 需求

1. **R1 失败原因透传**：按 fileResult.code 映射人话文案（如 `download_failed`→「下载 WebDAV 音频失败，请检查网络」、`put_failed`→透传含 HTTP 码的上传失败信息、`no_password`/`missingCredentials`→「密码缺失，请到音源设置补全」），行详情展示具体原因；未知 code 兜底显示原始 message
2. **R2 全量可重试**：「重试失败项」同时覆盖 `failed` 与 `file-failed` 的行
3. **R3 汇总归类**：结果汇总按主要原因分组计数（如「网络问题 N 首」「认证问题 N 首」「其他 N 首」），点击可过滤/滚动定位到对应行（MVP 可只做分组计数展示）

## 验收标准

1. 单测：code → 文案映射函数覆盖全部已知 code + 未知兜底；重试集合包含 file-failed + failed
2. 真机/CDP：模拟 WebDAV 写回失败时行详情显示具体原因而非笼统文案
3. 既有语义回归：file-failed 仍入库且标 scrape、journal/撤销行为不变
4. lint / test:unit / build 全过（禁止管道吞退出码）

## 范围外

- 不改原生插件错误码定义（已够细）
- 不改匹配聚合、可疑判定逻辑

## 约束

- 不改回滚 journal 结构与撤销语义
- 遵循 Salt token 与现有 ScrapePage 样式体系

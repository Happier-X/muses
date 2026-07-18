# 执行计划

1. 在 `src/features/sources/storage.ts` 增加本地/WebDAV 编辑输入类型与 `updateSource`，保留身份字段和列表顺序，并实现 WebDAV 新密码写入失败恢复策略。
2. 在 `tests/unit/sources.spec.ts` 增加存储层测试：
   - 本地名称与目录更新，身份字段和其他音源不变。
   - WebDAV 非敏感字段更新且密码留空时不写 SecureStorage。
   - WebDAV 新密码写入原 `credentialKey`，密码不进入 `muses:sources`。
   - 不存在 id 安全返回。
   - 元数据保存失败时尝试恢复旧密码。
3. 在 `src/views/SourcesPage.vue` 为卡片增加编辑按钮和编辑 modal：
   - 本地表单支持改名和 `FilePicker.pickDirectory()` 重选目录。
   - WebDAV 表单预填非敏感字段，密码始终为空。
4. 实现保存流程：
   - 只改 WebDAV 名称时直接保存。
   - 连接字段或密码变化时，使用新密码或局部读取原密码，对目标目录执行在线验证；验证失败不持久化。
   - 成功后更新 `sources` ref、关闭弹窗并提示成功；不调用歌曲对账。
5. 检查编辑、删除、扫描按钮在虚拟列表卡片中均可见可用，且实测行高逻辑不回归。
6. 运行 `npm run lint`、`npm run build`、`npm run test:unit -- --run`。
7. 检查 diff 与安全边界，确认密码未进入 `localStorage`、响应式回填状态或日志，并仅实现 #34。

## 风险与回滚点

- 风险：WebDAV 验证需要临时读取原密码。必须限制在保存函数局部变量，禁止赋给表单状态。
- 风险：新密码写入后元数据持久化失败。存储层应保存旧密码快照并尽力恢复。
- 风险：增加第三个操作按钮可能改变卡片高度。虚拟列表已有实测行高，仍需构建和页面结构检查。
- 回滚：删除编辑 UI 与 `updateSource`；持久化 schema 未变化，无数据迁移回滚。

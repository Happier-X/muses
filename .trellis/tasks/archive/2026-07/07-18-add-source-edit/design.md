# 技术设计

## 方案概览

在 sources 存储层新增类型安全的编辑 API，在 `SourcesPage.vue` 增加按音源类型切换的编辑弹窗。编辑始终保留音源身份字段；WebDAV 连接字段变化时先在线验证，再执行凭据与元数据持久化。

## 存储层契约

### 编辑输入

- 本地音源：`{ name, path }`。
- WebDAV 音源：`{ name, serverUrl, username, path, password? }`。
- `password` 只作为调用期输入，不进入返回值、音源对象或 `localStorage`。

### `updateSource`

在 `src/features/sources/storage.ts` 增加 `updateSource(sourceId, changes, existingSources?)`：

- 找不到 id 时返回 `updated: null`，不写库。
- 根据现有音源类型校验更新输入，禁止改变 `id`、`type`、`createdAt`、`credentialKey`。
- 只替换目标音源，保持其他音源及顺序不变。
- 本地编辑只写 `muses:sources`。
- WebDAV 密码为空时不调用 SecureStorage 写入。
- WebDAV 提供新密码时，使用原 `credentialKey` 更新 SecureStorage；若后续元数据保存失败，尝试恢复旧密码，避免元数据与凭据不一致。
- 返回 `{ updated, sources }` 供页面更新。

WebDAV 在线验证不放入纯存储 helper，由页面在调用 `updateSource` 前完成，因为验证依赖 WebDAV 网络边界，而存储层只负责原子化持久化。

## 页面交互

### 编辑入口

音源卡片操作区增加“编辑”按钮，保留“删除”和“扫描”。点击后记录目标音源并打开编辑 modal。

### 本地编辑

- 预填名称和当前目录。
- “重新选择目录”调用 `FilePicker.pickDirectory()`，成功后更新表单路径；用户取消时保留原路径并保持弹窗可继续编辑。
- 保存时要求名称、路径非空，调用 `updateSource`。

### WebDAV 编辑

- 预填名称、服务地址、用户名、目录。
- 密码字段始终为空，提示“留空则保留原密码”，不读取已有密码用于回填。
- 仅名称变化：直接保存，无需联网。
- 服务地址、用户名、目录或密码变化：
  1. 密码非空时使用新密码；密码为空时临时通过 `getWebDavPassword(credentialKey)` 获取原密码。
  2. 对规范化后的目标目录调用 `listWebDavDirectories(connection, path)`；即使目录没有子目录，只要 PROPFIND 成功即视为验证通过。
  3. 验证成功后调用 `updateSource`。
- 原密码只存在于函数局部变量和调用边界，不写入响应式表单、日志或错误消息。
- 验证失败时不调用 `updateSource`，从而保证元数据和凭据均不变。

## 歌曲处理

编辑不调用 `reconcileSourceSongs`。无论本地目录还是 WebDAV 连接字段变化，已有歌曲暂时保留；用户下次成功扫描时由扫描器按 #32 对账清理旧记录。这样避免离线、误改配置或验证后的暂时异常导致即时数据删除。

## 错误与恢复

- 表单缺失必填项：页面显示固定业务提示。
- 原 WebDAV 密码缺失：显示“WebDAV 密码不存在，请输入新密码。”。
- WebDAV 验证失败：显示安全的编辑验证失败提示，不持久化任何变更。
- SecureStorage 或 `localStorage` 写入失败：不显示成功；新密码写入后元数据失败时尽力恢复原密码。
- 用户取消目录选择不视为编辑失败，不覆盖原目录。

## 兼容与回滚

- 不改变现有 `SourceItem` 持久化结构，无需迁移。
- 不改变扫描默认值：本地仍默认读标签，WebDAV 仍默认不读标签。
- 回滚时可移除编辑 modal、按钮及 `updateSource`，已有音源数据保持兼容。

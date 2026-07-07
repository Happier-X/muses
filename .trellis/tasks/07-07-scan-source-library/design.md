# 添加音源扫描入库设计

## 架构边界

本任务在现有 Ionic Vue 前端内实现，不新增全局状态库。

拟新增/扩展模块：

- `src/features/library/types.ts`：歌曲库记录、扫描选项、扫描进度、扫描摘要等类型。
- `src/features/library/storage.ts`：歌曲库 localStorage 持久化、校验、按来源路径 upsert 去重。
- `src/features/library/scanner.ts`：面向 UI 的扫描编排，接收 `SourceItem` 和扫描选项，持续回调进度并返回摘要。
- `src/features/library/tags.ts`：真实读取音频标签，封装第三方解析库或原生能力，对解析失败进行文件级降级。
- `src/features/library/native.ts`：声明项目自有 Android 原生能力接口，用于本地目录递归枚举和本地音频元数据读取。
- `src/features/sources/webdav.ts`：扩展 WebDAV `PROPFIND` 解析能力，支持读取目录和文件，供递归扫描使用；补充可安全读取 WebDAV 音频元数据或下载必要数据的能力，供标签解析使用。
- `android/app/src/main/java/ionic/muses/*`：新增或扩展 Capacitor 插件，支持本地目录扫描、音频元数据读取，以及必要的 WebDAV 文件元数据读取。
- `src/views/SourcesPage.vue`：每张音源卡片增加扫描按钮、扫描设置弹窗、进度弹窗和用户反馈。

## 数据模型草案

歌曲记录最小字段：

- `id`：稳定 ID，可由 `sourceId + path` 生成或首次创建后持久保存。
- `sourceId`：来源音源 ID。
- `sourceType`：`local` 或 `webdav`。
- `path`：音源内的文件路径。
- `uri`：后续播放可用的本地路径或 WebDAV URL/路径标识。
- `title`：展示标题，默认由文件名去扩展名得到。
- `artist?`、`album?`、`duration?`：标签读取成功后写入。
- `createdAt`、`updatedAt`：本地库记录时间。

歌曲库 localStorage key 暂定 `muses:songs`。写入前必须验证结构，忽略无效旧数据。

## 扫描流程

1. 用户点击音源卡片“扫描”。
2. 打开设置弹窗，默认显示“读取音乐标签”开关。
3. 用户确认后关闭设置弹窗，打开进度弹窗。
4. 扫描器根据音源类型遍历音频文件：
   - WebDAV：使用 SecureStorage 取密码后，通过原生 `WebDav` 插件递归 `PROPFIND` 目录，过滤音频扩展名，并在需要时读取文件标签。
   - 本地文件夹：基于 `FilePicker.pickDirectory()` 保存的 Android `content://` 树 URI，通过新增项目自有原生能力递归枚举音频文件，并读取文件标签。
5. 每发现/处理一批文件，回调进度：阶段、已发现、已处理、已入库/更新/跳过/失败、当前项。
6. 对每个音频文件生成或读取标签信息，并 upsert 到歌曲库。
7. 完成后展示摘要。

## 音乐标签读取策略

本任务要求真实读取音乐标签，不能只保留占位开关。当前仓库没有标签读取依赖，实施时需要选择并接入解析能力。

推荐优先方案：

- 本地音源优先使用 Android 原生 `MediaMetadataRetriever` 或等价能力读取标题、歌手、专辑、时长等字段，因为当前 `FilePicker.pickDirectory()` 只返回 `content://` 树 URI，不提供递归文件列表或文件内容读取。
- WebDAV 音源优先扩展项目原生 `WebDav` 插件：递归 `PROPFIND` 列文件，并在开启标签读取时通过受控请求读取远程音频元数据；认证密码仍只从 SecureStorage 取出并传入底层请求，不写入 UI 状态、歌曲库或日志。
- 如果原生远程元数据读取不可行，可引入浏览器端音频元数据解析库，但必须由 WebDAV 安全下载入口提供解析所需数据，且避免把大文件无上限加载到内存。
- 当用户关闭“读取音乐标签”时，不下载/解析标签，直接使用文件名生成标题。
- 当用户开启“读取音乐标签”时，逐文件真实解析标题、歌手、专辑、时长等可得字段；单个文件解析失败不终止整个扫描，使用文件名回退并在摘要中计数。

## 兼容性与安全

- WebDAV 密码只能通过 `getWebDavPassword(credentialKey)` 获取，不能存入歌曲记录或日志。
- WebDAV 递归扫描和标签下载要避免把认证信息暴露给 UI 状态。
- 扫描失败应隔离到当前音源，不影响其他音源和音源列表持久化。
- 重复扫描以 `(sourceId, path)` 作为唯一键进行 upsert。

## 回滚形态

- UI 变更集中在 `SourcesPage.vue`，可回滚扫描按钮和弹窗。
- 歌曲库新增 localStorage key 独立于现有 `muses:sources`，回滚不影响音源数据。
- WebDAV 扩展应保持现有 `listWebDavDirectories` 行为和测试通过。

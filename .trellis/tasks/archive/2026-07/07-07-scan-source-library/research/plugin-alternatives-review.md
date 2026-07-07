# 音源扫描入库插件替代方案技术评审

## 结论摘要

建议**保留当前实现路线**：`@capawesome/capacitor-file-picker` 负责用户选择目录，项目自定义 `LocalLibraryPlugin` 负责 Android SAF `content://` 树 URI 递归枚举与本地标签读取，自定义扩展 `WebDavPlugin` 负责 WebDAV `PROPFIND` 与远程文件标签读取，底层统一使用 Android `MediaMetadataRetriever`。

不建议在当前任务中整体替换为现成 JS 库或第三方 Capacitor 插件。原因是：

1. 现有成熟 Capacitor 文件选择插件更多解决“选择文件/目录”，不解决“在已授权的 Android SAF 树 URI 下递归枚举文件并读取元数据”。
2. 常见 JS 标签库（如 `music-metadata`、`jsmediatags`）能解析音频标签，但需要可读的文件流/Blob/Buffer；对 Android `content://` 目录、WebDAV 带认证远程文件、CORS、内存上限都不能单独闭环。
3. 当前实现把 WebDAV 密码只作为一次性参数传给原生 OkHttp/`MediaMetadataRetriever`，不写入歌曲库或 localStorage，更贴合任务安全要求。
4. Android 原生 `MediaMetadataRetriever` 正好覆盖本地 `content://` URI 和远程 URL + Header 场景，减少大文件完整下载到 JS 内存的风险。

建议只做**边界加固与未来替换点预留**，不做替换：

- 为 `LocalLibraryPlugin` 补充注释：说明它是 SAF 树 URI 的适配层，外部只暴露“递归枚举音频文件”和“读取元数据”。
- 为 `WebDavPlugin.readMetadata` 补充注释：说明选择原生读取是为了避开 CORS、避免密码进入 JS 可观测下载链路，并减少大文件内存风险。
- 在前端保持 `src/features/library/native.ts`、`src/features/library/tags.ts` 这样的窄接口；未来若出现成熟插件，只替换这两个边界，不影响扫描器与歌曲库。
- 后续可以补充 Android 端单元/仪器测试，或至少为 Kotlin 插件补充小范围手工验证清单。

## 证据来源

本次评审使用了本地仓库、已安装依赖文档与 npm registry 元数据。网络可访问 npm registry；未做更广泛网页检索，因此对“生态成熟度”的判断结合了 npm 元数据、本地包文档和 Capacitor/Android 通用生态经验。

已查证内容：

- `package.json` 当前依赖包含：
  - `@capawesome/capacitor-file-picker@^8.0.3`
  - `@capacitor/android@8.4.1`
  - `@capacitor/core`
  - `@aparajita/capacitor-secure-storage@^8.0.0`
  - 未包含 `music-metadata`、`jsmediatags`、`@capacitor/filesystem` 等标签或文件系统依赖。
- 本地 `node_modules/@capawesome/capacitor-file-picker/README.md` 与类型定义显示：
  - `pickDirectory()` 只承诺打开目录选择器并返回 `PickDirectoryResult.path`。
  - 文档描述“Directory picking: Allows users to select a directory”，但 API 没有递归枚举目录内容、过滤音频文件、读取元数据的方法。
  - `readData` 选项用于文件数据读取，并明确提示读取大文件可能导致应用崩溃，不推荐用于大文件。
- npm registry 元数据：
  - `@capawesome/capacitor-file-picker@8.0.3`：描述为“allows the user to select a file”，关键词包含 file picker。
  - `@capacitor/filesystem@8.1.2`：描述为“NodeJS-like API for working with files on the device”，但当前项目未安装；它面向应用沙盒/文件系统 API，不等价于 SAF 树 URI 递归访问。
  - `music-metadata@11.13.0`：描述为 Node.js 音乐元数据解析器，支持大量格式。
  - `jsmediatags@3.9.7`：描述为 Media Tags Reader，关键词集中在 ID3、mp3、mp4。
  - `@capacitor-community/media@9.1.0`：描述为保存/获取照片和视频、管理相册，不面向音乐库扫描或音频标签。
  - `capacitor-file-selector@0.0.5`：描述为选择文件，版本非常早，不显示成熟的目录递归与标签能力。
  - 查询 `capacitor-native-file-picker` 返回 npm 404，不存在该包。
- 当前实现证据：
  - `android/app/src/main/java/ionic/muses/LocalLibraryPlugin.kt` 使用 `DocumentFile.fromTreeUri(context, treeUri)` 和 `directory.listFiles()` 递归收集音频文件；使用 `MediaMetadataRetriever.setDataSource(context, Uri.parse(uriValue))` 读取 `content://` 文件元数据。
  - `android/app/src/main/java/ionic/muses/WebDavPlugin.kt` 使用 OkHttp 执行 `PROPFIND`，并使用 `MediaMetadataRetriever.setDataSource(url, mapOf("Authorization" to "Basic ..."))` 读取远程 WebDAV 文件元数据。
  - `src/features/sources/webdav.ts` 保留目录浏览 API `listWebDavDirectories`，新增 `listWebDavAudioFiles` 递归返回文件。
  - `src/features/library/tags.ts` 将本地与 WebDAV 标签读取封装在 `readLocalAudioTags`、`readWebDavAudioTags`。
  - `tests/unit/library.spec.ts` 已覆盖标签读取失败降级、WebDAV 密码从 SecureStorage 获取、歌曲库不包含密码等摘要逻辑。

## 1. Android SAF `content://` 目录递归枚举插件评审

### 现成插件能力

#### `@capawesome/capacitor-file-picker`

适合继续保留，用于让用户选择本地目录。

优点：

- 已在项目中使用，版本为 `^8.0.3`。
- 支持 `pickDirectory()`，Android/iOS 可用。
- 与 Capacitor 8 兼容，维护活跃度看起来较好。

不足：

- API 返回目录路径/URI，但不提供递归枚举树 URI 下文件的能力。
- 不提供音频扩展名过滤。
- 不提供音频元数据读取。
- `readData` 是针对选中文件读 Base64；文档提示大文件可能导致崩溃，不适合作为音乐库扫描基础。

结论：继续用于“选择目录”，不能替代 `LocalLibraryPlugin.scanDirectory`。

#### `@capacitor/filesystem`

不建议用于替代 SAF 树 URI 递归枚举。

原因：

- 它是 Capacitor 官方文件系统 API，适合应用沙盒目录、缓存、文档目录等 NodeJS-like 文件操作。
- Android SAF `content://tree/...` 需要通过 `ContentResolver`/`DocumentFile` 和持久化 URI 权限访问，不是普通路径读写。
- 即便安装 `@capacitor/filesystem`，也不能直接解决用户通过系统目录选择器授权后，对树 URI 递归枚举所有子文件的问题。

#### 其他文件选择类插件

`capacitor-file-selector@0.0.5` 等包从 npm 元数据看仅是“选择文件”，版本低且能力范围窄；没有证据显示能成熟支持 Android SAF 树 URI 递归枚举与元数据读取。

### 当前自定义实现是否造轮子

这里不是无意义造轮子，而是在补齐 Capacitor 生态的 Android 平台缺口。

当前 `LocalLibraryPlugin.scanDirectory` 使用 Android 官方 `DocumentFile.fromTreeUri` 递归 `listFiles()`，这是 SAF 树 URI 的直接适配方式。它只做两件事：

1. 根据已授权 tree URI 枚举音频文件。
2. 返回稳定的 `path`、`uri`、`name` 给前端扫描器。

这层逻辑和业务强相关，但边界窄，维护成本可控。

### 风险

- **Android 兼容性**：不同文件管理器/云盘 Provider 的 `DocumentFile.listFiles()` 性能和可访问性不同；大目录可能慢。
- **权限持久性**：需要确认 `pickDirectory()` 后是否持久化 URI 权限，或应用重启后是否仍可访问。若插件未自动持久化，后续可能需要在原生端调用 `takePersistableUriPermission`。
- **测试性**：JS 单元测试只能 mock 插件；真实 SAF 行为需要 Android 仪器测试或手工设备验证。

## 2. 音频元数据读取插件评审，尤其是 Android 本地 `content://` URI

### JS 标签库

#### `music-metadata`

能力强，但不建议当前替换本地原生读取。

优点：

- npm 元数据表明支持大量音频和标签格式：MP3、MP4/M4A、Vorbis、FLAC、WAV、WMA、APE、AIFF、Opus 等。
- 适合 Node.js 或有良好流/Buffer 支持的环境。

不足：

- npm 描述明确是 Node.js parser。浏览器/Capacitor WebView 使用时通常需要额外打包适配、流/Buffer polyfill 或改用其 Web API 入口。
- 对 Android `content://` 文件并不能直接读取；仍然需要一个原生桥把 `content://` 转成 Blob/ArrayBuffer/临时文件/可读流。
- 如果为了给 JS 库解析而把完整音频读成 Base64/ArrayBuffer，会引入大文件内存风险，特别是 FLAC/APE/WAV。
- 即使只读头部，也需要实现 Range/分片读取桥，复杂度不低。

#### `jsmediatags`

不建议作为当前任务的主方案。

优点：

- 体积和使用复杂度相对低。
- 适合 ID3、MP3、MP4/M4A 这类常见标签场景。

不足：

- npm 元数据关键词集中于 ID3、mp3、mp4；格式覆盖明显弱于 `music-metadata` 和 Android 原生能力。
- 同样不能直接访问 Android `content://` 树 URI。
- 仍需要读取文件内容到 JS，存在大文件内存问题。

### Capacitor 音频/媒体插件

未发现成熟的、直接满足“给定 Android `content://` URI 读取音频标题/歌手/专辑/时长”的通用插件。`@capacitor-community/media` 从 npm 描述看面向照片、视频和相册管理，不是音乐库标签读取。

### 当前原生读取是否合理

合理。`MediaMetadataRetriever.setDataSource(context, Uri)` 是 Android 原生读取本地 URI 音频元数据的标准能力，天然支持 `content://`，无需把大文件搬到 JS 层。

当前自定义 `LocalLibraryPlugin.readMetadata` 返回字段也很克制：`title`、`artist`、`album`、`duration`，刚好满足任务验收，不把解析库复杂度暴露给前端。

### 风险

- **格式支持差异**：`MediaMetadataRetriever` 对部分格式或编码的标签支持可能不如 `music-metadata` 全面，尤其是 APE、部分 Vorbis/FLAC 非标准标签。
- **Provider 行为差异**：部分 SAF Provider 可能无法被 `MediaMetadataRetriever` 正常 seek 或读取。
- **错误信息不细**：原生异常 message 可能不够友好；前端目前有降级摘要，可以接受。

## 3. WebDAV 带认证远程文件标签读取替代方案评审

### JS 库 + 下载/Range 请求方案

可行但不建议当前替换。

可能方案：

- 前端使用 `fetch` 携带 Basic Auth 或 Bearer Header 下载 WebDAV 文件头部/完整文件。
- 使用 `music-metadata` 或 `jsmediatags` 解析 Blob/ArrayBuffer。

主要问题：

1. **CORS**：很多 WebDAV 服务不会配置允许移动 WebView origin 的跨域响应头，浏览器层 `fetch` 可能失败。当前原生 OkHttp 不受浏览器 CORS 限制。
2. **密码安全**：JS 层发起下载时，认证 Header、URL、错误对象、调试日志更容易被 UI 状态或日志误带出；当前原生插件只在调用参数中短暂使用密码，歌曲库测试已验证不写入 localStorage。
3. **大文件内存**：标签解析库若需要 Blob/ArrayBuffer，容易把大文件读入 WebView 内存。Capawesome FilePicker 文档也明确提示读取大文件可能导致崩溃。
4. **Range 支持不稳定**：更优方案是 Range 读取头部和尾部，但不是所有 WebDAV 服务器都稳定支持 Range；同时 JS 标签库与 Range Reader 适配复杂。
5. **认证兼容性**：当前实现仅 Basic Auth；若以后支持 Digest/OAuth/自签证书，原生 OkHttp 层更容易集中处理。

### Capacitor 插件方案

未发现成熟插件能同时覆盖：

- WebDAV `PROPFIND` 目录递归。
- 带认证读取远程音频标签。
- 不把密码暴露到持久化存储或日志。
- 避免 CORS 和大文件 JS 内存问题。

### 当前 `WebDavPlugin.readMetadata` 是否合理

合理。它使用 `MediaMetadataRetriever.setDataSource(url, headers)` 直接把 Basic Auth Header 传给 Android 原生解析器，避免了 JS 下载链路。

但需要注意：

- `MediaMetadataRetriever` 对远程 URL 的协议行为依赖 Android 系统实现；部分 WebDAV 服务如果不支持 Range、响应慢、证书异常或要求特殊 Header，可能失败。
- 当前 OkHttp 超时只用于 `PROPFIND`；`MediaMetadataRetriever` 远程读取没有显式超时控制，可能在部分网络环境下表现不可控。
- 若未来需要更强可靠性，可在原生端改成 OkHttp Range 读取临时缓存片段，再交给更强解析器；但这会显著增加实现复杂度。

## 4. 当前实现对比

| 方案 | SAF 递归枚举 | 本地 `content://` 标签 | WebDAV 认证标签 | CORS | 大文件内存 | 密码安全 | 维护成本 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 当前：FilePicker + LocalLibraryPlugin + WebDavPlugin + MediaMetadataRetriever | 支持，原生 `DocumentFile` | 支持，原生 URI | 支持，URL + Header | 不受浏览器 CORS 影响 | 低，不进 JS 大对象 | 较好，密码不入库 | 中，需要维护少量 Kotlin |
| 仅 `@capawesome/capacitor-file-picker` | 不支持递归 | 不支持 | 不支持 | 无关 | 无关 | 无关 | 低但能力不足 |
| `@capacitor/filesystem` | 不适配 SAF 树 URI | 不支持标签 | 不支持 | 无关 | 可能需要读文件 | 无关 | 中且仍需原生桥 |
| `music-metadata` + JS 下载 | 仍需原生桥 | 需先把 `content://` 转 Blob/Buffer | 理论支持解析，但需下载 | 受 CORS 影响 | 高，需精细 Range | 较弱，认证进入 JS 下载链路 | 高 |
| `jsmediatags` + JS 下载 | 仍需原生桥 | 需先把 `content://` 转 Blob/Buffer | 部分格式可行 | 受 CORS 影响 | 高 | 较弱 | 中高，格式覆盖不足 |
| 其他媒体插件 | 未发现成熟支持 | 未发现成熟支持 | 未发现成熟支持 | 不确定 | 不确定 | 不确定 | 不确定 |

## 5. 建议

### 保留当前实现

建议本任务保留当前组合：

- `@capawesome/capacitor-file-picker`：继续只承担目录选择。
- `LocalLibraryPlugin`：继续承担 SAF 树 URI 递归枚举与本地标签读取。
- `WebDavPlugin`：继续承担 `PROPFIND` 和 WebDAV 远程标签读取。
- `MediaMetadataRetriever`：继续作为 Android 标签读取后端。

这是当前可执行性、安全性、内存风险和交付范围之间的最佳平衡。

### 需要补的边界和注释

建议在后续实现收尾时补充以下非功能性改进：

1. 在 `src/features/library/native.ts` 注释说明：这是本地音源平台能力边界，未来可替换为第三方插件，但前端扫描器不依赖具体实现。
2. 在 `LocalLibraryPlugin.kt` 注释说明：使用 `DocumentFile` 是为了访问 Android SAF `content://tree`，不是普通文件系统路径。
3. 在 `WebDavPlugin.kt` 注释说明：远程标签读取放在原生层是为了绕开 CORS、减少 JS 内存占用并降低认证信息泄漏面。
4. 在设计文档或代码注释中明确：如果未来替换为 `music-metadata`，必须先实现受控 Range Reader/Blob Provider，不能直接完整下载大文件到 JS。
5. 增加手工测试清单：
   - Android 版本覆盖至少 Android 10/11/13+。
   - 本地 SAF 目录含多层子目录、大量文件、中文路径。
   - WebDAV 服务覆盖 401/403、中文路径、带空格路径、大文件、标签读取失败降级。

### 暂不替换为 JS 标签库

不建议现在引入 `music-metadata` 或 `jsmediatags` 作为主链路。只有在以下条件满足时才考虑部分替换：

- 需要比 `MediaMetadataRetriever` 更完整的标签格式支持，例如复杂 FLAC/Vorbis/APE 标签、封面、曲号、年份、流派等。
- 已实现安全、可测试、限流的文件读取抽象：
  - 本地 `content://` 支持分片读取或临时文件流。
  - WebDAV 支持认证 Range 请求。
  - 有最大读取字节数限制和超时控制。
- 能接受额外包体积、打包 polyfill 和解析兼容性成本。

若未来迁移到 `music-metadata`，建议步骤为：

1. 新增 `AudioMetadataProvider` 抽象，定义 `readLocal(uri)`、`readWebDav(url, credentials)`。
2. 原生 `MediaMetadataRetriever` 作为默认 provider。
3. 先只在 WebDAV 或特定格式失败后启用 JS provider 作为 fallback，而不是一次性替换主链路。
4. JS provider 必须通过原生受控 reader 获取有限字节或临时缓存，禁止无上限完整读取。
5. 增加大文件、慢网络、CORS、认证失败和内存压力测试。

## 6. 风险清单

### 维护性

- 当前自定义 Kotlin 代码不多，边界清晰，维护成本可控。
- 风险在于 Capacitor 版本升级、Android API 行为差异、WebDAV 服务兼容性。
- 通过保持 TS 侧窄接口和注释，可降低未来替换成本。

### 密码安全

- 当前 WebDAV 密码从 SecureStorage 读取，传给原生插件，不进入歌曲库，已有单元测试覆盖 localStorage 不包含密码。
- 风险在于异常日志、调试输出或未来改造时误记录请求参数。
- 建议禁止打印 `password`、`Authorization` Header 和完整认证上下文。

### CORS

- 当前原生 OkHttp 和 `MediaMetadataRetriever` 不受浏览器 CORS 限制。
- 若改为 JS `fetch` + 标签库，会立即受到 WebDAV 服务 CORS 配置影响，兼容性显著下降。

### 大文件内存

- 当前方案不把音频完整搬到 JS 层，风险较低。
- JS 标签库方案如果读 Blob/ArrayBuffer/Base64，容易导致 WebView 内存压力甚至崩溃。
- 未来如做 JS fallback，必须限制读取字节数、支持 Range、设置超时。

### Android 兼容性

- SAF Provider 差异是主要风险；`DocumentFile.listFiles()` 在部分云盘型 Provider 上可能慢或失败。
- `MediaMetadataRetriever` 对远程 URL 和部分格式标签的支持存在系统差异。
- 建议用真实设备和至少一个 WebDAV 服务做手工验证。

### 测试性

- 当前 JS 单元测试能覆盖扫描摘要、密码不入库、标签失败降级。
- 原生插件行为难以通过 Vitest 覆盖，需要 Android 仪器测试或手工测试清单。
- WebDAV 兼容性最好补一个可控测试服务器或 mock server，但这可能超出当前任务范围。

## 最终可执行建议

本任务不替换当前自定义原生实现。继续交付当前方案，并补齐以下轻量改进：

1. 保持 `FilePicker.pickDirectory()` 只作为目录授权入口。
2. 保持 `LocalLibraryPlugin` 作为 Android SAF 递归枚举与本地 `content://` 标签读取边界。
3. 保持 `WebDavPlugin` 作为 WebDAV 认证访问与远程标签读取边界。
4. 不引入 `music-metadata`、`jsmediatags` 作为主链路。
5. 为上述原生边界补注释，明确未来可替换点。
6. 增补或记录 Android 手工验证清单，覆盖权限持久性、深层目录、大文件、中文路径、WebDAV 认证和标签失败降级。

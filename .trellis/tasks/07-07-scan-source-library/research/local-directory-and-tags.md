# 本地目录扫描与音乐标签读取调研

## 结论

- `@capawesome/capacitor-file-picker` 的 `pickDirectory()` 只返回所选目录的 `path`，在 Android 上该值来自 `Intent.ACTION_OPEN_DOCUMENT_TREE` 的 `content://` URI。
- 插件类型 `PickDirectoryResult` 只有 `path: string`，不包含目录内文件列表。
- 插件 Android 实现 `createPickDirectoryResult()` 只执行 `callResult.put("path", implementation.getPathFromUri(uri))`。
- 插件已有单文件 `pickFiles()` 的 `readData` 能力，但不提供对已选目录树的递归枚举或读取文件内容能力。

## 对本任务的影响

- 若本任务必须支持本地文件夹扫描入库，需要新增项目自有 Android Capacitor 插件能力：基于 `DocumentFile` 或 `DocumentsContract` 递归枚举目录树，过滤音频文件，并读取元数据。
- 如果使用浏览器端标签解析库读取本地文件，需要先把 `content://` 下的文件内容通过原生层暴露给前端；当前依赖不能直接满足。
- WebDAV 扫描已有项目自有 `WebDav` 插件，可扩展 `PROPFIND` 文件解析和文件下载/元数据读取，但必须继续避免密码进入 localStorage 或日志。

## 推荐方案

- 对本地音源：新增项目自有 Android 原生能力用于递归枚举目录和读取音频元数据，避免把大文件完整 base64 传回前端。
- 对 WebDAV 音源：扩展现有 WebDAV 能力递归列文件；标签读取可选择原生层用带 Basic Auth 的请求/元数据能力，或下载必要数据给前端解析。优先选择不暴露密码、不大量占用内存的实现。
- 前端仍保留统一扫描器和歌曲库模型，原生/远程读取细节封装在 feature helper 内。

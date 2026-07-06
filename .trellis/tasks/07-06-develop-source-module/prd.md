# 开发音源模块

## 目标

开发“音源”tab 的初版模块，让用户进入音源页面后能看到已添加的音源，并通过右上角添加入口添加本地文件夹或 WebDAV 文件夹；音源列表使用 TanStack 虚拟列表渲染，为后续大量音源目录做准备。

## 已确认事实

- 当前项目是 Ionic Vue 3 + TypeScript + Vite 应用。
- 当前音源 tab 路由为 `/tabs/sources`，仍复用 `src/views/Tab2Page.vue` 占位页。
- 当前项目没有全局状态管理、持久化层、API client、WebDAV client 或虚拟列表依赖。
- `@tanstack/vue-virtual` 当前未安装，可用于 Vue 虚拟列表。
- `webdav` npm 包当前未安装，可作为 WebDAV 客户端候选；目标平台为 Android WebView，不以 Web 浏览器 CORS 能力作为目标边界。
- 本地文件夹选择在 Web 浏览器中通常依赖 File System Access API，移动端或非 Chromium 浏览器需要 Capacitor/原生插件方案；当前 `src/` 中尚未使用 Capacitor 文件系统 API。
- 用户已确认项目只需要支持安卓端，Web 浏览器不是目标运行平台。
- 用户已确认本次 MVP 要做真实安卓目录选择，优先引入 `@capawesome/capacitor-file-picker` 并使用 `pickDirectory()` 获取目录路径。
- 用户已确认音源列表需要持久化，且 WebDAV 密码也需要保存，但必须使用安全存储方案。
- `@aparajita/capacitor-secure-storage` 是 Capacitor 8+ 安全存储候选；其 Android 实现使用 Android Keystore 生成密钥，以 AES-GCM 加密数据并存入应用私有 SharedPreferences。

## 需求

- 音源 tab 应使用独立的音源页面，不再复用模板占位页。
- 打开音源页面时，展示已添加的音源。
- 顶部标题栏居中展示“音源”。
- 顶部标题栏右侧展示添加音源按钮，使用加号图标即可。
- 点击添加后，用户可选择：
  - 添加本地文件夹
  - 添加 WebDAV 文件夹
- 选择 WebDAV 文件夹时，需要输入连接信息，并能浏览/多选对应的文件夹。
- 选中的文件夹应添加到音源页面，以列表形式展示。
- 音源列表必须使用 TanStack 的虚拟列表能力渲染。

## 待定决策

- MVP 运行平台与能力边界：仅支持安卓端。
- 本地文件夹选择应使用真实安卓目录选择能力：引入 `@capawesome/capacitor-file-picker`，调用 `pickDirectory()`，将选中的目录作为本地音源加入列表。
- WebDAV 添加应做真实连接和目录浏览：引入 `webdav` npm 包，输入服务器地址、用户名、密码后列出目录，支持进入子目录、多选文件夹并添加为音源。
- 音源列表需要本地持久化，重启 App 后仍应展示。
- WebDAV 密码需要保存，并必须使用 Android 安全存储方案；不得以明文写入 `localStorage`。
- 用户已确认接受分离存储方案：音源列表元数据保存到 `localStorage`，WebDAV 密码保存到安全存储；WebDAV 音源记录只保存 `credentialKey` 引用密码。

## 初步非目标

- 不扫描音乐元数据、不解析歌曲列表。
- 不实现播放能力。
- 不实现跨设备同步。
- 不建设完整全局状态管理，除非持久化或跨页面访问明确需要。

## 验收标准草案

- [ ] `/tabs/sources` 渲染独立音源页面。
- [ ] 页面标题栏居中显示“音源”，右侧有可点击的加号按钮。
- [ ] 点击加号后可以选择添加本地文件夹或 WebDAV 文件夹。
- [ ] WebDAV 添加流程包含连接信息输入界面。
- [ ] WebDAV 添加流程支持多选文件夹并添加到音源列表。
- [ ] 添加后的音源在音源页面以列表形式展示。
- [ ] 音源列表渲染基于 TanStack 虚拟列表。
- [ ] lint、type-check 和相关测试通过。

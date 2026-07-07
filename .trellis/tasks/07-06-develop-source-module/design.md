# 开发音源模块设计

## 范围

本任务实现安卓端音源模块 MVP：独立音源页、添加本地目录、添加 WebDAV 目录、音源持久化、WebDAV 密码安全保存、TanStack 虚拟列表展示。

## 页面与路由

- `/tabs/sources` 改为加载独立页面 `src/views/SourcesPage.vue`。
- `SourcesPage.vue` 使用 Ionic 页面结构：`IonPage`、`IonHeader`、`IonToolbar`、`IonTitle`、`IonButtons`、`IonButton`、`IonContent`。
- 标题栏：居中显示“音源”，右侧显示加号按钮。
- 点击加号后弹出添加方式选择：本地文件夹、WebDAV 文件夹。

## 数据模型

音源元数据保存到 `localStorage`：

```ts
type SourceItem =
  | {
      id: string
      type: 'local'
      name: string
      path: string
      createdAt: string
    }
  | {
      id: string
      type: 'webdav'
      name: string
      serverUrl: string
      username: string
      path: string
      credentialKey: string
      createdAt: string
    }
```

- `id` 由前端生成，用于列表 key 与安全存储 key 关联。
- `credentialKey` 只存在于 WebDAV 音源，用于从安全存储读取密码。
- WebDAV 密码不得进入 `localStorage`。

## 持久化策略

- `localStorage` 保存音源列表元数据，键名建议：`muses:sources`。
- `@aparajita/capacitor-secure-storage` 保存 WebDAV 密码，键名建议：`muses:webdav-password:<sourceId>` 或添加前的临时 credential key。
- 删除 WebDAV 音源时同步删除对应安全存储 key。
- 如果元数据存在但安全存储读取失败，保留音源记录，但后续 WebDAV 操作应提示凭据不可用。

## 本地文件夹流程

- 引入 `@capawesome/capacitor-file-picker`。
- 调用 `FilePicker.pickDirectory()` 打开安卓目录选择器。
- 使用返回的 `path` 作为本地音源路径。
- 添加记录后立即保存到 `localStorage` 并刷新列表。

## WebDAV 流程

- 用户输入服务器地址、用户名、密码。
- 使用项目内 Android 原生 `WebDav` Capacitor 插件发送 `PROPFIND` 请求，插件底层使用 OkHttp 支持 WebDAV 非标准方法，避免 Android WebView 浏览器网络栈的 CORS 限制，同时绕开 `CapacitorHttp` 和 `HttpURLConnection` 的标准 HTTP 方法白名单限制。
- 用户可输入任意 WebDAV 地址。为支持用户自建的 `http://` WebDAV 服务，Android `network_security_config` 使用 `base-config cleartextTrafficPermitted="true"` 允许明文 HTTP；`https://` 地址仍按系统 TLS 校验处理。
- 初始列出根目录或用户输入的起始路径。
- 目录浏览只展示目录项；支持进入子目录、返回上级目录、多选目录。
- 用户确认后，为每个选中目录创建 WebDAV 音源记录。
- 密码写入安全存储；元数据写入 `localStorage`。
- 连接失败、认证失败、列目录失败时在界面上展示错误信息。

## 虚拟列表

- 引入 `@tanstack/vue-virtual`。
- 音源列表使用虚拟列表渲染，不直接 `v-for` 渲染完整数组。
- 空列表显示空状态，引导用户点击右上角添加音源。

## 依赖与 Android 同步

新增依赖：

- `@tanstack/vue-virtual`
- `@capawesome/capacitor-file-picker`
- `@aparajita/capacitor-secure-storage`

实现后需要运行 Capacitor sync，使 Android 工程识别新插件。

## 取舍

- 不引入 Pinia/Vuex；本任务用轻量模块函数封装存储与 WebDAV 操作。
- 本任务不扫描目录中的歌曲，不解析音乐元数据。
- 本任务以 Android WebView 为目标，不保证 Web 浏览器生产可用。
- 允许明文 HTTP 是为支持用户自定义 WebDAV 地址的产品取舍；后续可在 UI 中对 `http://` 地址提示安全风险。

# 开发音源模块实现计划

## 实现步骤

1. 安装依赖：`@tanstack/vue-virtual`、`@capawesome/capacitor-file-picker`、`@aparajita/capacitor-secure-storage`。
2. 新增音源类型与存储工具：定义音源模型、localStorage 读写、安全存储密码读写/删除。
3. 新增 WebDAV 工具：规范化路径、通过项目内 Android 原生 `WebDav` Capacitor 插件发起 `PROPFIND`、解析 XML 目录响应、过滤目录项。
4. 新增 `SourcesPage.vue`：实现标题栏、添加按钮、空状态、虚拟列表、添加方式弹窗。
5. 实现本地文件夹添加：调用 `pickDirectory()`，生成本地音源并持久化。
6. 实现 WebDAV 添加流程：连接信息表单、目录浏览、返回上级、多选、确认添加、错误展示。
7. 更新路由：`/tabs/sources` 指向 `SourcesPage.vue`。
8. 同步 Android 插件：运行 Capacitor sync。
9. 验证：运行 lint、build/type-check、单元测试；如可行运行 e2e。

## 验证命令

- `npm run lint`
- `npm run build`
- `npm run test:unit -- --run`
- `npx cap sync android`

## 风险点

- Android 目录选择依赖原生插件，Web 预览环境可能不可用，需要界面错误提示。
- WebDAV 服务可能因地址、认证、证书或网络策略失败，需要清晰错误信息；Android 端必须使用项目内原生 `WebDav` 插件并由 OkHttp 发起 `PROPFIND`，避免 WebView fetch/XHR CORS 限制，并避开 `CapacitorHttp` 与 `HttpURLConnection` 的标准方法白名单。
- 用户可添加任意 WebDAV 地址；为支持 `http://` WebDAV 服务，Android 需要通过 `network_security_config` 允许明文 HTTP。
- WebDAV 密码必须只进入安全存储，不得写入 localStorage 或日志。
- TanStack 虚拟列表需要可滚动容器高度，否则列表可能不显示。

## 回滚点

- 若原生插件安装或 Android sync 失败，回滚依赖与相关调用。
- 若项目内 Android 原生 `WebDav` 插件的 `PROPFIND` 在特定服务上兼容性不足，优先补齐请求头或 XML 解析；仍失败则回到规划阶段评估更完整的专用原生插件或代理方案。

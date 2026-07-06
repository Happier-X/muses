# 开发音源模块实现计划

## 实现步骤

1. 安装依赖：`@tanstack/vue-virtual`、`webdav`、`@capawesome/capacitor-file-picker`、`@aparajita/capacitor-secure-storage`。
2. 新增音源类型与存储工具：定义音源模型、localStorage 读写、安全存储密码读写/删除。
3. 新增 WebDAV 工具：创建 client、规范化路径、列目录、过滤目录项。
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
- WebDAV 服务可能因地址、认证、证书或网络策略失败，需要清晰错误信息。
- WebDAV 密码必须只进入安全存储，不得写入 localStorage 或日志。
- TanStack 虚拟列表需要可滚动容器高度，否则列表可能不显示。

## 回滚点

- 若原生插件安装或 Android sync 失败，回滚依赖与相关调用。
- 若 WebDAV 依赖在 Vite/Android WebView 中构建失败，优先调整导入方式；仍失败则回到规划阶段重新评估客户端库。

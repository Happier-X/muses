# 实施计划：WebDAV 新增/编辑独立页面

## 前置

- [ ] 确认 SourcesPage 列表加载时机（onMounted / keep-alive），确定保存返回后的刷新方案
- [ ] 确认 MNavbar 返回按钮组件用法（MNavbarBackLink）

## 步骤

### S1 新页面与路由
- [ ] 新建 `src/views/SourceWebDavPage.vue`：按 design.md 结构搭建表单区（平移 webDavForm 双模式逻辑，模式取自路由）
- [ ] 注册路由 `/tabs/sources/webdav` 与 `/tabs/sources/webdav/:id`
- 验证：`npx vue-tsc --noEmit` 通过；两路由可达

### S2 目录浏览区接入
- [ ] 连接成功后展开 `<WebDavDirectoryBrowser>`（add=multiple / edit=single）
- [ ] confirm 分流：批量建源 / 回填 path；error → 页面 toast
- [ ] 浏览区高度占满剩余空间（flex 布局），路径导航与目录列表完整可见
- 验证：vue-tsc 通过

### S3 SourcesPage 迁移清理
- [ ] 「添加 WebDAV 文件夹」→ `router.push('/tabs/sources/webdav')`；WebDAV 卡片「编辑」→ `router.push(/tabs/sources/webdav/${id})`
- [ ] 删除 sheet 双模式表单、webDavMode/webDavForm、内嵌浏览器、submitWebDavEdit 等迁出代码及样式
- [ ] 保存/添加成功后返回列表的刷新验证
- 验证：vue-tsc + grep 无残留引用

### S4 质量收尾
- [ ] `npm run lint`、`npx vue-tsc --noEmit`、`npm run test:unit` 全过（禁止管道吞退出码）
- [ ] MuMu 实测（主会话执行）：新增全流程、编辑预填+回填+保存、返回不脏、本地流程回归
- [ ] 清理临时调试产物

## 验证命令

```bash
npm run lint
npx vue-tsc --noEmit; echo $?
npm run test:unit
npm run build; echo $?
npx cap sync android && cd android && ./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 回滚点

S1–S3 每步可独立编译；任一步失败 `git checkout -- <file>` 回退。

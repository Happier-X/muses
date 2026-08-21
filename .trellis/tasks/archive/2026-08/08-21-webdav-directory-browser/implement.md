# 实施计划：WebDAV 音源目录浏览器

## 前置

- [ ] 阅读 `.trellis/spec/frontend/index.md` 及 component-guidelines / directory-structure / forms 相关条目
- [ ] 确认 `listWebDavDirectories`、`getWebDavPassword` 签名与行为（src/features/sources/webdav.ts、storage.ts）

## 步骤

### S1 新建 `WebDavDirectoryBrowser` 组件
- [ ] 按 design.md 契约实现 props/emits/expose，内部浏览状态自管
- [ ] UI：路径导航条 + 目录行列表 + loading 态；single 模式行尾「选择」按钮，multiple 模式 checkbox + 底部确认按钮
- [ ] 错误经 `emit('error')` 上报，组件内不 toast（toast 由页面统一处理）
- 验证：`npx vue-tsc --noEmit` 通过

### S2 编辑流程接入
- [ ] 编辑对话框 WebDAV 分支「目录」字段增加「浏览」按钮
- [ ] 凭据组装：表单值 + 密码空时 `getWebDavPassword(source)` 兜底；读取失败 toast 并中止
- [ ] confirm 回调回填 `editSourceForm.setFieldValue('path', paths[0])` 并关闭浏览器
- 验证：MuMu 实测编辑 → 浏览 → 选目录 → 字段回填 → 保存成功

### S3 添加流程迁移
- [ ] 添加 sheet 内联浏览区替换为 `<WebDavDirectoryBrowser mode="multiple">`
- [ ] 「连接并浏览」成功后调用 `open()`；confirm 复用批量建源逻辑
- [ ] 删除 SourcesPage 中被替代的内联状态/函数/模板段
- 验证：MuMu 实测添加 WebDAV 全流程不回归（多选、批量添加）

### S4 质量收尾
- [ ] `npm run lint`、`npx vue-tsc --noEmit`、`npm run test:unit` 全过
- [ ] 为浏览器组件补单元测试（列表渲染/模式化操作/error 冒泡，mock listWebDavDirectories）
- [ ] 清理临时调试产物（cdp-eval.mjs、muses-01.png 不入库）

## 验证命令

```bash
npm run lint
npx vue-tsc --noEmit
npm run test:unit
# 真机验证
npm run build && npx cap sync android && cd android && ./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 回滚点

- S1/S2/S3 各自独立可编译提交进度；任一步失败可 `git checkout -- <file>` 回退该步。

# 实施计划：WebDAV 目录浏览独立页面

## 步骤

### S1 会话服务
- [ ] 新建 `src/features/sources/webdavBrowseSession.ts`（按 design.md 契约）
- 验证：vue-tsc 通过

### S2 浏览页
- [ ] 新建 `src/views/SourceWebDavBrowsePage.vue` + 注册路由 `/tabs/sources/webdav/browse`
- [ ] TabsPage 音源项 childPrefixes 增加前缀；无会话兜底返回逻辑
- 验证：vue-tsc 通过

### S3 来源页改造
- [ ] SourceWebDavPage 移除内嵌浏览器与相关状态，接入 openBrowseSession（add=multiple 批量建源 / edit=single 回填）
- [ ] 清理死代码（状态/样式/导入）
- 验证：vue-tsc + grep 无残留

### S4 质量收尾
- [ ] lint / vue-tsc / 单测全过（禁管道吞退出码）；build 通过
- [ ] MuMu 实测：新增多选批量建源、编辑单选回填保存、浏览页直接深链兜底、返回无副作用
- [ ] 清理临时调试产物

## 验证命令

```bash
npm run lint; npx vue-tsc --noEmit; echo $?; npm run test:unit
npm run build; echo $?
npx cap sync android && cd android && ./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 回滚点

S1–S3 各步独立可编译；失败可 checkout 回退。

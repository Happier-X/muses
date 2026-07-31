# Implement：升级 happier-ui 0.0.7 完成 edge-to-edge

## 执行清单

1. **加载规范与调研**
   - 阅读 `.trellis/spec/frontend/component-guidelines.md`（happier-ui 接入契约：精确版本、无 file: 链接）。
   - 阅读 `research/`（本任务无独立 research 文件，以 design.md 兼容性分析为准）。

2. **升级依赖**
   ```bash
   npm install happier-ui@0.0.7 --save-exact
   ```
   或手动改 `package.json` 后 `npm install`。确认 lock 同步。

3. **移除宿主临时覆盖**
   - 编辑 `src/theme/tailwind.css`，删除 `.h-nav-bar--safe-area` 覆盖块（含注释）。
   - 确认无其他代码引用该选择器（grep）。

4. **构建验证**
   ```bash
   npm run lint
   npm run build
   ```

5. **同步 Android 资产**
   ```bash
   npx cap sync android
   rg -n "SystemBars|insetsHandling" android/app/src/main/assets/capacitor.config.json
   ```

6. **重构组件兼容抽查**
   - 检查 SourcesPage（HBottomSheet ×4 + HDialog ×1）、SongsPage（HDialog ×1 + HBottomSheet）、PlaylistsPage（HDialog ×2）模板无编译错误。
   - `npm run build` 已含 vue-tsc 类型检查，类型层面自动把关。

7. **edge-to-edge 真机验证**（尽力执行，无法真机则列待验项）
   - Navbar 背景铺到状态栏后、内容避让、图标可读。
   - TabBar 底部避让。
   - 播放器/Queue 开闭后回归。

8. **更新 spec**
   - component-guidelines.md 记录：happier-ui 已升级 0.0.7，safe-area 由库正式接管，宿主不得再持有 `.h-nav-bar--safe-area` 覆盖。

## 验证命令

```bash
npm run lint
npm run build
npx cap sync android
rg -n "happier-ui" package.json                    # 期望 0.0.7 精确版本
rg -n "h-nav-bar--safe-area" src/theme/tailwind.css # 期望无输出
rg -n "SystemBars|insetsHandling" android/app/src/main/assets/capacitor.config.json
```

## 风险文件

| 文件 | 风险 |
|------|------|
| `package.json` / lock | 版本漂移或锁不同步 |
| `src/theme/tailwind.css` | 删除覆盖后依赖它的隐藏逻辑失效（本任务该选择器仅 safe-area 用） |
| HBottomSheet/HDialog 使用页 | 重构后行为/样式回归 |
| `android/` 资产 | sync 未更新 |

## 回滚点

- `git checkout package.json package-lock.json src/theme/tailwind.css` 即可恢复 0.0.6 + workaround。
- 若重构组件有回归，回滚依赖升级并记录 issue。

## Review Gates

- 升级后构建/类型/lint 全绿。
- 移除覆盖后 grep 确认无残留。
- 真机验证（或明确列出待验项）。

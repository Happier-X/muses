# Implement：Navbar 安全区 Edge-to-Edge 适配

## 执行清单

1. **加载规范与调研**
   - 阅读 `.trellis/spec/frontend/component-guidelines.md`。
   - 阅读 `research/capacitor-edge-to-edge.md`。

2. **固定 Capacitor SystemBars 配置**
   - 编辑 `capacitor.config.ts`。
   - 在现有 `plugins` 中增加：
     ```ts
     SystemBars: {
       insetsHandling: 'css',
     },
     ```
   - 不新增 `overlaysWebView` 或 `backgroundColor`。

3. **增加 Navbar 宿主兼容样式**
   - 编辑 `src/theme/tailwind.css`。
   - 在全局组件覆盖区域增加 `.h-nav-bar--safe-area`：
     ```css
     padding-top: var(--safe-area-inset-top, env(safe-area-inset-top, 0px));
     ```
   - 保持 Navbar 根背景、MPage 和滚动结构不变。

4. **核验状态栏样式流程**
   - 检查 `src/App.vue` 的 `StatusBar.setStyle`。
   - 除非验证发现实际图标不可读，否则不改现有切换逻辑。

5. **提交 happier-ui issue**
   - 提交前再次搜索重复 issue。
   - 用 `gh issue create -R Happier-X/happier-ui` 提交。
   - 内容覆盖版本、复现、根因、预期、建议修复，以及 HTabBar 同类检查。
   - 保存 issue URL 到任务 Notes/最终回复。

6. **验证**
   - `npm run lint`
   - `npm run build`
   - `npx cap sync android`
   - 检查生成的 `android/app/src/main/assets/capacitor.config.json` 含 `SystemBars.insetsHandling = "css"`。
   - 真机/模拟器：普通页面、长列表滚动、Queue/Player 开闭、状态栏图标可读性。

7. **更新 spec**
   - 在 frontend component guidelines 的安全区/页面骨架约定中记录：Capacitor 8 应优先消费 `--safe-area-inset-*`，不得只依赖 `env()`。

## 验证命令

```bash
npm run lint
npm run build
npx cap sync android
```

配置核验：

```bash
rg -n "SystemBars|insetsHandling" android/app/src/main/assets/capacitor.config.json
```

## 风险文件

| 文件 | 风险 |
|------|------|
| `src/theme/tailwind.css` | 选择器优先级不足或造成重复 inset |
| `capacitor.config.ts` | 配置键拼写或同步未写入 Android 资产 |
| `src/App.vue` | 仅验证；不应无依据扩大修改 |

## 回滚点

- 删除 `SystemBars.insetsHandling` 显式配置。
- 删除 `.h-nav-bar--safe-area` 宿主覆盖。
- GitHub issue 不删除；若结论变化，在 issue 追加更正说明。

## Review Gates

- CSS 值必须严格为 Capacitor 官方推荐回退顺序。
- 不得修改 `node_modules` 或 happier-ui 本地仓库源码。
- issue 必须在代码验证后提交，避免报告未经证实的方案。

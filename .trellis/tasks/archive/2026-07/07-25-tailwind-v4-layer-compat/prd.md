# PRD: 修复 Tailwind v4 @layer 在旧版 Android WebView 不兼容

## 问题描述

Tailwind CSS v4 将所有样式输出到 `@layer` 块内：
- `@layer base`（Preflight 重置）
- `@layer components`（happier-ui 组件样式）
- `@layer theme`（主题变量）
- `@layer utilities`（工具类）

`@layer` CSS at-rule 需要 **Chrome 99+（2022年3月）** 才支持。设备上若 Android System WebView 版本较旧，整个 `@layer` 块内的样式会被浏览器完全忽略，导致：

- Tailwind 工具类不生效
- happier-ui 组件样式不生效
- 原生按钮变回 Android 默认方形外观

## 目标

在构建阶段移除 CSS 中的 `@layer` 包装，使 Tailwind v4 样式兼容旧版 Android WebView，同时保持功能不变。

## 验收标准

1. 构建产物（`dist/assets/*.css`）中不包含 `@layer` 规则
2. Preflight 重置、happier-ui 组件样式、Tailwind 工具类等所有样式均正常出现在 CSS 输出中
3. 功能与之前一致，无样式退化
4. 不影响开发服务器（Vite dev server）

## 约束

- 继续使用 `@tailwindcss/vite`（不切换回 PostCSS 管道）
- 开发阶段（桌面浏览器）保留 `@layer` 行为（桌面浏览器均支持）

## 方案

添加 PostCSS 插件，在 PostCSS 阶段解包 `@layer` 块，释放其子节点到顶层。

```css
/* 构建前 */
@layer base { button { ... } }

/* 构建后 */
button { ... }
```

由于 Vite 的 CSS 管道在 `@tailwindcss/vite` 处理完 CSS 后自动运行 PostCSS 插件，只需创建 `postcss.config.js` 注册该插件即可。

# 升级 happier-ui 组件库到 0.0.2

## Goal

将 `happier-ui` 组件库从 `0.0.1` 升级到 `0.0.2`，并让 `src/components/ui/index.ts` 的转出与组件库最新真实导出保持镜像一致。

## Background

- 0.0.2 相对 0.0.1 **完全向后兼容**：原有 11 个组件（HButton、HSwitch、HBottomSheet、HDialog、HInput、HCheckbox、HEmpty、HImage、HIcon、HTabBar、HNavBar）与 `HTabBarItem` 类型全部保留。
- 0.0.2 新增 9 个组件：HIconButton、HToast、HRange、HProgress、HCard、HCell、HCellGroup、HFloatingBubble、HSidebar。
- 0.0.2 新增 4 组类型：HSidebarItem、HFloatingBubbleOffset、HFloatingBubbleAxis、HFloatingBubbleMagnetic、HFloatingBubbleGap。
- 项目实际直接引用：`HIcon`（MCover.vue）、`HNavBar`（MPage.vue），其余组件通过 `src/components/ui/index.ts` 统一转出。

## Requirements

- `package.json` 中 `happier-ui` 依赖升级到 `0.0.2` 并完成安装（`package-lock.json` 同步）。
- 按 0.0.2 breaking 要求接入 **Tailwind CSS v4**：安装 `tailwindcss@^4` 与 `@tailwindcss/vite`，在 `vite.config.ts` 启用官方插件。
- 全局样式改为 CSS-first 管道：`@import "tailwindcss"` + `@import "happier-ui/styles"`（旧 `happier-ui/style.css` 已移除；不得再只引预编译 style 入口）。
- `src/components/ui/index.ts` 补齐新增组件与类型的 re-export，保持"只转出组件库真实导出"的语义；更新文件头注释中的版本号到 `0.0.2`。
- 现有对 `HIcon`、`HNavBar` 等组件的用法保持可用；Ionic 桥接 `variables.css` 继续有效。

## Acceptance Criteria

- [x] `package.json` 与 `package-lock.json` 中 `happier-ui` 为 `0.0.2`。
- [x] 已声明并安装 `tailwindcss@^4`、`@tailwindcss/vite`，Vite 配置启用 `tailwindcss()` 插件。
- [x] 全局 CSS 通过 Tailwind 管道加载 `happier-ui/styles`（经 `src/theme/tailwind.css`）。
- [x] `src/components/ui/index.ts` 转出 0.0.2 全部组件与类型，头注释版本为 `0.0.2`。
- [x] `npm run build`（`vue-tsc && vite build`）通过，无类型错误。
- [x] unit test 通过（337 passed）；现有页面对 happier-ui 组件的引用无回归。

## Notes

- 轻量级任务，PRD-only。
- 新增组件为 re-export，未使用部分由 tree-shaking 处理，不影响打包体积。

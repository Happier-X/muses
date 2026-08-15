# 复刻椒盐音乐 Switch 开关样式

## Goal

将 `MToggle` 开关组件从当前 iOS 风格（64×28 轨道 + 纯白拇指滑块）改为椒盐音乐（Salt Player 12.2.0 实测）的 Switch 风格：46×26dp 胶囊轨道 + 白色圆环拇指 + 中心露底色圆点 + 300ms 轨道色渐变。保留组件事件契约不变。

## Background

- **椒盐开关实测规格**（MuMu 模拟器 12.2.0 实机像素测量 + modlens 识图 + SaltUI 开源 `Switcher.kt` 源码对照，三者一致）：
  - 轨道：46×26dp 胶囊（全圆角），开启 = highlight 蓝 `#0470E6`（深色 `#0088FF`），关闭 = `#E9E9E9`（= subText `#8C8C8C` @ 10% alpha 叠加表面）
  - 拇指：16dp 白色圆环（= 16dp 白圆 + 4dp 白 border，中心 8dp 露轨道色），开启位于右侧（位移 20dp），关闭位于左侧
  - 动画：轨道色 `tween(300)` 渐变；拇指位移平滑（源码 tween 300 / 当前 muses 用 spring 亦可，视觉差异可接受）
- **当前 MToggle**：iOS 风格 64×28 轨道 + 24px 白拇指位移 22px，选中主色底。
- 事件契约：`modelValue` / `checked`（兼容）/ `disabled` / `ariaLabel` / `change`（原生 change 事件）/ `update:modelValue`——**保持不变**，各页面调用点（SettingsPage 音量均衡等）零改动。

## Requirements

- 轨道几何改为 46×26dp 胶囊；开启态蓝色（`--m-primary`）、关闭态浅灰（需适配浅/深主题，参考 `--m-text-2` @ 10% 叠表面 ≈ 浅 `#E9E9E9` / 深约 `#303336`）。
- 拇指为 16dp 白色圆环（4dp 白边 + 中心 8dp 露轨道色圆点），开启右移 20dp、关闭左端。
- 轨道色切换约 300ms 渐变；拇指位移平滑动画（沿用 motion spring 或改 tween 300）。
- 事件契约与组件 API 不变；禁用态保留（opacity 0.5 或椒盐同款降透明度）。
- 不改动其他组件与页面布局；`vue-tsc` / ESLint / 构建通过。

## Acceptance Criteria

- [ ] 设置页「音量均衡」开关在 MuMu 模拟器上呈现椒盐样式：46×26 胶囊、白环拇指、开=蓝底+拇指右侧、关=灰底+拇指左侧。
- [ ] 切换动画平滑（轨道色渐变 ~300ms + 拇指滑动），无跳变。
- [ ] 开启/关闭状态与 `checked` 值同步正确；点击切换后事件照常触发（音量均衡即时生效）。
- [ ] 浅色/深色主题下关闭态轨道色均可见且协调。
- [ ] `npm run type-check` 与 ESLint 通过，无新增错误。

## Notes

- 轻量任务，PRD-only。改动集中在 `src/components/ui/MToggle.vue`（模板 + scoped SCSS），如需关闭态轨道色 token 可在组件内用 scoped 变量（浅/深两值），不动全局 token 面。
- 中心圆点实现建议：拇指 16dp 圆 + `border: 4px solid #fff` + 背景透明 → 中心自然露轨道色（与 SaltUI 源码 border 方案一致）。
- 需同步更新 `.trellis/spec/frontend/component-guidelines.md` 的 MToggle 描述（若存在）。

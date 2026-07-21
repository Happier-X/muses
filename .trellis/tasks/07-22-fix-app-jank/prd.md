# 应用卡顿与页面卡死 (#50)

## Goal

缓解「整体很卡、页面经常卡死」；优先修可证实的主线程/过度渲染热点，避免无依据大改。

## Background（初步嫌疑，待实现期验证）

1. **进度轮询**（#47）：playing 时 250ms `getCurrentTime` + bridge，并 `emit` → Vue 响应式 + media session 同步；可能放大 WebView 压力。
2. **诊断日志**：`native.ts` `console.info('[MusesNativeAudio]', ...)` 在 play 路径较频。
3. **沉浸式 AMLL**：`BackgroundRender` + `LyricPlayer`（blur/scale）GPU/主线程重；保活 PlayerPage。
4. **列表**：`SongsPage` 等可能全量 `v-for`（Sources 已 virtual）；大曲库滚动卡顿。
5. **会话/队列**：localStorage 读写、切歌多 token 异步匹配（歌词/封面/元信息）并发。

## Requirements

### R1. 诊断与可复现
- 记录主要卡顿场景（启动、进播放页、大列表滚动、播放中切换 tab）。
- 优先用代码审查 + 低成本计时/减少无效更新验证，不强依赖用户真机 profile（有则更好）。

### R2. 修复原则
- 先做 **高收益、低风险** 改动：降频轮询、合并 emit、静默/门控日志、避免无意义 reactive 写、列表虚拟化（若曲库页确认为热点）。
- 不删功能换流畅（如直接卸 AMLL），除非产品确认。
- 不改 capgo 源码。

### R3. 验收
- 至少 1–2 个已识别热点有可测改动（单测或前后对比说明）。
- 不回归 #47 进度可见更新、#49 恢复、播放稳定。

### Out of Scope

- 完整重写 UI 框架
- 原生侧 Media3 播放栈替换
- 发版

## Acceptance Criteria

- [ ] 至少一个主因得到处理并写入 design/实现说明
- [ ] 播放中进度仍更新；列表/播放页基本可交互
- [ ] lint / unit / build 通过
- [ ] 关闭 #50（或注明剩余已知限制）

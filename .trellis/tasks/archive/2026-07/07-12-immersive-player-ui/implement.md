# 沉浸式播放页 UI 复刻 — 执行计划

## Checklist

1. [x] 改造 `src/views/PlayerPage.vue` 控制页结构
   - 移除顶部「正在播放」header
   - 重组 info-panel：封面 → 信息 → 进度 → 主控制 → 纯图标 mode-bar
   - 循环/随机按钮改为 icon-only，保留 `aria-label`
   - 随机/单曲循环激活态样式
2. [x] 重写/调整 scoped CSS
   - 安全区留白、封面尺寸圆角阴影、标题层级
   - 进度条视觉增强
   - 主控制与次要控制间距
   - 宽屏双栏兼容
   - 歌词 panel 仅做必要 padding 适配
3. [x] 更新 `tests/unit/player.spec.ts`
   - 空状态去掉「返回」断言
   - 模式按钮改为 `aria-label` 断言
   - 保留封面/seek/歌词滑动关键断言
   - 顺手修正 overlay 相关过时断言与 media-session mock
4. [x] 质量验证
   - `npm run lint`
   - `npx vitest run tests/unit/player.spec.ts`
   - `npm run build`
5. [x] 回写 `.trellis/spec/frontend/component-guidelines.md`：控制页布局、无顶部标题、纯图标次要控制、一屏适配、overlay 底层滚动锁定
6. [x] 后续修复：一屏无滚动；打开 overlay 时底层歌曲列表不可滚动/交互
7. [x] 最终质量验证：lint / vitest player.spec / build 通过

## Validation Commands

```bash
npm run lint
npm run test:unit -- tests/unit/player.spec.ts
npm run build
```

## Review Gates

- 窄屏控制页结构符合 PRD Target Layout
- 无新增功能控件
- 下滑关闭、左右滑歌词、宽屏双栏未回归
- 图标按钮均有 `aria-label`

## Rollback Points

- 完成步骤 1–2 后若视觉不可接受：仅回滚 `PlayerPage.vue`
- 完成步骤 3 后测试失败：回滚页面 + 测试，重新对齐断言

# 歌词页 AMLL 视觉复刻 — 执行计划

## 检查清单

1. [x] 改 `src/views/PlayerPage.vue` 歌词 panel 结构
   - 增加 `.lyric-header`（歌名 `h2` + 可选歌手 `p`）
   - `LyricPlayer` 增加 `alignAnchor` / `alignPosition` / blur / scale / wordFadeWidth
   - 空状态仍居中可读；有当前歌曲时 header 仍显示
2. [x] 样式
   - `.lyric-panel` 改为纵向 flex：header 固定 + player `flex:1; min-height:0`
   - 左对齐大字、边距与安全区
   - `@media (min-width: 768px)` 隐藏 `.lyric-header`
   - 用 `:deep()` 覆盖 AMLL 行样式（若需要）
3. [x] 更新 `tests/unit/player.spec.ts`
   - 断言窄屏歌词区含歌名（及有歌手时的歌手）
   - 保留左滑 `-50%` + AMLL 歌词可见
   - mock 兼容新 props
4. [x] 验证
   - `npm run test:unit -- tests/unit/player.spec.ts`
   - `npm run lint`
   - `npm run build`
5. [x] 可沉淀约定写入 `.trellis/spec/frontend/`（完成阶段）

## 验证命令

```bash
npm run test:unit -- tests/unit/player.spec.ts
npm run lint
npm run build
```

## 审查门

- 对照 PRD 验收：窄屏 header、无底栏、宽屏无 header、AMLL 仍在
- 不破坏控制页布局与下滑/左右滑
- 无 `node_modules` 修改、无 git commit（除非用户要求）

## 回滚点

- 仅涉及 `PlayerPage.vue` + 单测 + 可选 spec；整文件还原即可

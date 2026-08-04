# 沉浸页 mode-bar 去掉随机/单曲循环选中态

## Goal

沉浸式播放页 mode-bar 的「随机播放」与「单曲循环」不再显示视觉选中态（半透明白底 / 提亮高亮），仅用图标与 `aria-label` 表达当前模式。

## Background

当前 `PlayerPage` mode-bar 对两键绑定：

- 单曲循环：`:class="{ 'is-active': queueState.repeatMode === 'one' }"`
- 随机：`:class="{ 'is-active': queueState.shuffleEnabled }"`

沉浸页 CSS `.player-overlay .h-button--ghost.is-active` 会给激活键半透明白底 + 更亮字色，用户不希望这种「选中」观感。状态本身已由图标区分（`repeat`/`repeatOutline`、`shuffle`/`listOutline`），无需再叠 `.is-active` 高亮。

## Requirements

1. **去掉 mode-bar 随机键的 `.is-active` 绑定**；随机开/关仍切换图标与 `aria-label`，功能不变。
2. **去掉 mode-bar 单曲循环键的 `.is-active` 绑定**；列表循环 / 单曲循环仍切换图标与 `aria-label`，功能不变。
3. **保留**歌词页翻译 FAB 的 `.is-active`（与本任务无关，仍为开/关辅助高亮）。
4. **不改**播放逻辑、`queueState`、按钮尺寸、mode-bar 布局、按下/hover 反馈基类。
5. Spec 同步：mode-bar 随机/循环**不得**用 `.is-active` 表达模式；模式仅图标 + 无障碍文案。

## Out of Scope

- 改 happier-ui 库默认
- 改 MiniPlayer 或其它页面的模式控件
- 去掉全局 `.is-active` CSS 规则（翻译 FAB 仍依赖）
- 改 repeat/shuffle 语义或队列行为

## Acceptance Criteria

- [ ] AC1：单曲循环开启时，mode-bar 循环键**无**半透明白底选中高亮，仅图标为单曲样式
- [ ] AC2：随机开启时，mode-bar 随机键**无**半透明白底选中高亮，仅图标为随机样式
- [ ] AC3：列表循环 / 顺序播放时行为与图标正确；切换模式仍可用
- [ ] AC4：歌词翻译 FAB 开态仍可有 `.is-active` 高亮（回归）
- [ ] AC5：`npm run lint` 与 `npm run build` 通过
- [ ] AC6：相关 spec（component-guidelines / features-player 若有「mode-bar is-active」表述）已改为仅图标区分

## Notes

- 轻量任务：PRD-only，无需 design.md / implement.md。
- 改动面预计：`PlayerPage.vue` 去掉两处 `:class is-active`；必要时微调 spec 一句。

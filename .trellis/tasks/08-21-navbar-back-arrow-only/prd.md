# PRD：导航栏返回按钮改为纯箭头

## 需求

所有页面的导航栏返回按钮不再显示「返回」文字，仅保留箭头图标。

## 现状

`MNavbarBackLink` 组件已支持纯箭头（不传 `text` 时默认 'Back' 且不显示文字）。以下 3 个页面显式传了 `text="返回"` 导致文字显示：

- `src/views/SourceWebDavPage.vue`
- `src/views/LibraryDetailPage.vue`
- `src/views/PlaylistDetailPage.vue`

## 改动

上述 3 处删除 `text="返回"` 属性。无障碍语义保留：组件 `aria-label` 回退到 `text`（'Back'），如需中文读屏文案可传 `aria-label="返回"`。

## 验收标准

1. 三个页面返回按钮仅显示箭头。
2. 点击行为不变；读屏标签仍为「返回」。
3. lint / vue-tsc 通过。

# 跳转当前播放置顶

## Goal

SongsPage 点击「跳转到当前播放」FAB 时，将当前播放歌曲行尽量滚动到**列表可视区顶部**；若目标接近列表末尾、无法再滚到顶部，则停在浏览器/容器允许的最大滚动位置（不必强行置顶）。

## Background

- 现状：`src/views/SongsPage.vue` 使用  
  `row.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' })`，当前曲出现在视口**中间**。
- 规范：`.trellis/spec/frontend/component-guidelines.md` 写明 `block: 'center'`，需同步修改。
- 用户反馈：希望置顶；最后几项滑不动时保持可及位置即可。

## Requirements

### R1. 滚动对齐
- FAB 跳转将 `scrollIntoView` 的 `block` 从 `center` 改为 **`start`**（`inline` 保持 `nearest` 或等价）。
- `behavior` 保持 `smooth`（与现网一致）。

### R2. 末尾自然停靠
- 不额外写死「强制 offset 伪造置顶」；依赖原生滚动边界：列表底部无法继续滚动时，目标行停在视口内可到达的位置（可能不在顶部），可接受。

### R3. 其它行为不变
- FAB 可见性、`data-song-id` 定位、`jump-highlight` 轻高亮、安全区定位、宽窄屏布局均不变。
- 无当前曲或不在列表时仍不显示 FAB / 不滚动。

### Out of Scope
- 不改 MiniPlayer / 其它页面的滚动逻辑。
- 不改虚拟列表（SongsPage 当前非虚拟列表）。
- 不发版。

## Acceptance Criteria

- [ ] 中间位置的当前曲，点击 FAB 后尽量出现在列表可视区顶部
- [ ] 列表最后几项点击 FAB 不报错、可滚动到尽头；不强行置顶
- [ ] 相关单测（若有）与 component-guidelines 中 FAB 滚动约定已同步
- [ ] lint / 相关 unit 通过

## Notes

- 轻量任务：PRD-only。
- 主要改动：`SongsPage.vue` 一行 + spec 一句。

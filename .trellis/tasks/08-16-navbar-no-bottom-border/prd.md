# PRD：navbar 去底部横线（全部页面统一）

## 背景

MNavbar 默认样式带 `border-bottom: 1px solid var(--m-hairline)`（顶部横线）。
歌曲页（SongsPage）通过覆盖为 `border-bottom: none` 去掉了。
用户要求专辑/艺术家/歌单/音源/设置等页面也去掉横线，跟歌曲页一致。

## 需求

1. MNavbar 默认样式去掉 `border-bottom`（全部页面统一无横线），
   覆盖含返回键的详情页（LibraryDetail / PlaylistDetail）随之统一。
2. SongsPage 中冗余的 `border-bottom: none` 覆盖清理（默认已无横线）。
3. `--transparent` 变体（PlayerPage 沉浸式）不受影响。

## 验收标准

- 专辑/艺术家/歌单/音源/设置/详情页 navbar 底部无 hairline。
- 歌曲页无回归（本就无横线）。
- lint + vue-tsc 通过；cap sync + assembleDebug + 安装 + 截图采样验证无横线。

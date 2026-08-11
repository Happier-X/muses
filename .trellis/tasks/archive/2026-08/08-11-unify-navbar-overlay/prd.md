# navbar 覆盖式布局统一推广（4 页）

## 背景

歌曲页（08-11-fix-navbar-overlay）已实现 navbar 覆盖式布局：内容从 navbar 后方滚动经过 → 官方 Konsta 玻璃（bg 渐变 + blur + mask）可见。用户确认效果后选择"统一推广到其他页面"。

## 范围

改 4 页（同 SongsPage 模式）：
- **LibraryDetailPage**（返回 navbar + 列表 + empty）
- **PlaylistDetailPage**（返回 navbar + 列表 + 两个 empty 分支）
- **SettingsPage**（navbar + m-content 直接滚动列表）
- **SourcesPage**（navbar + 列表 + empty，滚动容器原 pt-[8px]）

**不改**：CategoriesPage（navbar 含 subnavbar 分段条，需常驻吸顶；覆盖式收益低风险高）；SongsPage（已完成）。

## 改造模式（每页统一）

1. k-page：加 `relative`，移除 `!h-auto`（否则 k-page 被内容撑高，列表无法滚动）
2. k-navbar 包 `<div class="root-navbar-wrap absolute top-0 left-0 right-0 z-20">`
3. 滚动容器从屏幕顶开始 + 顶部 padding = navbar 高（无 sticky 行时内容初始在 navbar 下方、滚动经过其后方）
   - 通用：`pt-[calc(max(16px,var(--k-safe-area-top))_+_44px)]`
   - SourcesPage 原 pt-[8px] 保留：`pt-[calc(max(16px,var(--k-safe-area-top))_+_44px_+_8px)]`
4. empty 分支同步加 pt

## 验收标准

- [x] 每页 k-page 高度 = 视口（列表可滚动）
- [x] navbar absolute 覆盖，内容滚动经过其后方
- [x] 列表/empty 顶部不被 navbar 遮挡
- [x] 返回按钮、分段、列表行为无回归

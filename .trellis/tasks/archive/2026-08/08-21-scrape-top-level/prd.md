# 刮削页改为顶级页面

## Goal
将 ScrapePage 从独立路由 `/scrape` 改为 Tabs 体系内顶级页面 `/tabs/scrape`，与歌曲页一致，统一侧边抽屉选中态、返回键与手势逻辑。

## Scope
- 路由：`/scrape` → `/tabs/scrape`（保留重定向兼容旧链接）
- 侧边抽屉：刮削入口指向 `/tabs/scrape`
- 顶级页面判定：`topLevelPaths` 加入 `/tabs/scrape`
- 歌曲页内跳转：`router.push('/scrape')` 改为 `/tabs/scrape`

## Acceptance
- 访问 `/scrape` 自动重定向到 `/tabs/scrape`
- 抽屉中刮削选中态正常
- 在刮削页按返回键退到桌面（顶级页面行为）
- 歌曲页刮削相关跳转均到新路由

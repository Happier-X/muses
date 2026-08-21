# 执行计划

1. 读取 `SongsPage.vue` 现有工具条模板，确认三按钮与计数位置
2. 模板改造：
   - 删除 `crosshair` 与单独 `listChecks` 按钮
   - 保留 `shuffle` + `songs.length`
   - 新增「刮削」触发按钮：`<button @click="isScrapeMenuOpen=true"><listChecks/><span>刮削</span><span v-if="scrapeQueueCount>0" class="songs-page__scrape-badge">...</span></button>`
   - 新增 `<m-actions :opened="isScrapeMenuOpen">` 含两项：筛选可疑（` :disabled="suspiciousCount===0" @click="onOpenSuspiciousBatch"` ）与刮削队列（`@click="goScrapeQueue"`）
3. 脚本：新增 `isScrapeMenuOpen` ref，新增 `goScrapeQueue(){ isScrapeMenuOpen=false; router.push('/scrape') }`，复用现有 `suspiciousCount/scrapeQueueCount/onOpenSuspiciousBatch`
4. 样式：调整 `toolbar-left` gap，新增刮削触发按钮样式，徽标复用现有
5. 验证：`npm run build`、`npm run lint`、`npm run test:unit`，真机检查工具条仅随机播放+数字、刮削菜单可弹出且两项功能正常

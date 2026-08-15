# 真机歌曲页 navbar 与随机播放工具条之间出现空隙

## 根因（已定位，2026-08-15）

**真机安装的是 v0.3.2 release（566d0ec）旧包，该版本在 f5ca3cf 修复之前。**

旧代码 SongsPage.vue：
- navbar `:deep(.m-navbar) { padding-top: 22px }` 写死，navbar 高度固定 66px 不随安全区变化；
- 工具条 `top: calc(max(22px, var(--m-safe-area-top, 0px)) + 44px)` 随安全区变大；
- 真机（状态栏 s≈42dp）→ 空隙 = s − 22 ≈ 20dp；滚动时歌曲行从缝隙透出（截图显示被裁切行）。

### 证据链

1. 真机截图（1200x2670）像素级行扫描 + modlens 视觉分析交叉确认：
   - navbar 底边 y≈221px、工具条顶 y≈288px，中间透出被裁切的歌曲行（歌名 6px 残条 + "未知艺术家 - 未知专辑"）；
   - 5 项独立测量（navbar 66 CSS px、标题内边距、工具条 s+44、状态栏文字居中、行距 72）在 k≈3.35、s≈42dp 下全部吻合旧公式。
2. 浏览器复现（playwright-core + Edge，注入 `--safe-area-inset-top` = 0/47/90px 模拟 Capacitor）：main 当前代码 navbar 底 = 工具条顶 = 列表 padding-top，零缝隙。
3. git 时间线：v0.3.2（566d0ec）→ f5ca3cf（修复）→ main。

## 解决方案

无需代码改动：从 main 重新构建 APK 安装即可。

## 验收标准

- [ ] 从 main 构建新 APK 安装真机，歌曲页 navbar 与工具条零空隙贴合（椒盐基准）。
- [ ] 浏览器（safe-area=0）不回归（已验证 ✓）。
- [ ] 其余 7 个使用 `--m-navbar-pt` 的页面真机不回归。

## 复现/验证工具（临时，不入库）

- `vite preview --port 4173` + playwright-core（`npm i --no-save`，未污染 package.json）
- 注入脚本已删除；需要时可用本 PRD 记录的方法重建

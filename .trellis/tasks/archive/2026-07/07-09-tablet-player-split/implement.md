# 执行计划：Player 宽屏 50/50 双栏

## 实现步骤

### 唯一文件：src/views/PlayerPage.vue

改动范围：新增 `@media (min-width: 768px) { ... }` 块到 `<style scoped>` 末尾，模板/script 不动。

### 检查项

| # | 检查点 |
|---|--------|
| 1 | `.panels` 覆盖 `width:auto; display:flex; transform:none !important` |
| 2 | `.panel` 改为 `flex:1; width:auto` |
| 3 | `.info-panel` 宽屏居中 |
| 4 | `.lyric-panel` 填满右栏 |
| 5 | `.cover` 宽屏 `min(40%, 320px)` |
| 6 | `.lyric-player` `flex:1; height:auto; min-height:200px` |
| 7 | `.empty-state` 不受影响（不在 panels 内） |
| 8 | 背景未分割 |
| 9 | 窄屏无回归（所有规则在 `@media` 内） |

## 验证

```bash
npm run lint          # 零错误
npm run build         # vue-tsc + vite build 通过
npm run test:unit     # 不引入新失败
```

手动视觉：
- 浏览器 375px 宽：单栏滑动正常
- 浏览器 1024px 宽：左右双栏，封面左，歌词右，LyricPlayer 运行

## 回滚

revert `src/views/PlayerPage.vue` 即可。
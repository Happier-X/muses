# 技术设计：Player 宽屏 50/50 双栏

## 架构

纯 CSS 改造，不新增 JS 逻辑，只改 `src/views/PlayerPage.vue`。

改动层：

```
窄屏 (当前不变)          宽屏 (新增 @media 规则)
───────────────────     ─────────────────────────
.panels { width:200% }  .panels { width:auto; display:flex; flex-direction:row }
.panel { width:50% }    .panel { flex:1; width:auto }  ← 两栏各 50%
translateX 滑动          translateX 不生效 (overflow 隐藏)
touchstart/end 保留      保留（代码不动，CSS 不拦截）
```

## 宽屏 CSS 规则（新增在 `<style scoped>` 末尾）

```css
@media (min-width: 768px) {
  /* 双栏容器：正常 flex 并排 */
  .panels {
    width: auto;
    display: flex;
    flex-direction: row;
    min-height: 0;
    overflow: hidden;
    transform: none !important;
  }

  .panel {
    flex: 1;
    width: auto;
    min-width: 0;
    min-height: 0;
  }

  /* 左栏（封面控制）：居中排列 */
  .info-panel {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 24px 16px;
  }

  /* 右栏（歌词）：填满 */
  .lyric-panel {
    display: flex;
    flex-direction: column;
    justify-content: center;
  }

  /* 封面自适应 */
  .cover {
    width: min(40%, 320px);
  }

  /* 歌词高度：填充右栏可用，不锁死 70vh */
  .lyric-player {
    flex: 1;
    min-height: 200px;
    height: auto;
  }
}
```

注意 `.panels` 用 `transform: none !important` 覆盖 inline `:style` 的 `translateX`。

## 关键保护

1. `.lyric-player height: auto; flex: 1` 替代原有 `height: 70vh; min-height: 420px`——宽屏下右栏高度可变，歌词自填充。
2. 空状态 `.empty-state` 不受影响（v-if 条件，不属于 `.panels` 内）。
3. 背景 `.amll-background` 仍 `inset:0` 铺满，不受分栏影响（它在 `.immersive-shell` 里，未切分）。
4. `.lyric-panel` 原来的 `overflow: hidden` 保留（不在宽屏覆盖中覆盖）。

## 兼容性

- iOS/Android 一致：`@media (min-width: 768px)` 在 WebView 相同运作
- 暗色模式：无颜色改动，共存

## 回滚

revert `src/views/PlayerPage.vue` 即可。
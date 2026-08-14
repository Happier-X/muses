# Design: 歌曲页一比一复刻椒盐（源码参数 + 实测坐标）

## 1. 依据来源（已获取）

| 来源 | 内容 | 用途 |
|------|------|------|
| SaltUI 源码（`.tmp/saltui/`，Apache-2.0） | SaltDimens/SaltColors/SaltTextStyles/Padding/Item | 组件级权威参数 |
| SaltPlayerSource 翻译（strings.xml） | 排序菜单/工具条/多选文案 | UI 文案 |
| 模拟器实测（uiautomator + PIL + CDP） | 行高 72dp、圆 44x48dp、⋮ 36x48dp、坐标 | 应用层精确布局 |

交叉验证：SaltUI 的 padding 16dp / subPadding 12dp / item 56dp / 文字 16sp+12sp 与实测行高 72dp、封面 50dp 无冲突；歌曲页应用层自定义了行高 72dp（> 56dp 最小）与更大封面，符合 SaltUI 可定制模型。

## 2. 修改清单

### D1 全局 body margin（theme/index.scss）

```scss
body {
  margin: 0;              // ← 新增，修复默认 8px
  font-family: ...;
  color: var(--m-text);
  background: var(--m-surface);
}
```

影响面：#app 从 x=8/w=344 → x=0/w=360 满屏。MiniPlayer（bottom 定位）、浮层 backdrop（inset:0 基于视口）、safe-area 变量均不受 body margin 影响，预期零回归（AC7 验证）。

### D2 圆形按钮（SongsPage.vue）

模板不变（round-btn + round-icon），仅样式：

```scss
&__round-btn {
  position: relative;
  width: 44px; height: 48px;            /* 交互区 44x48dp（椒盐实测） */
  display: flex; align-items: center; justify-content: center;
  background: transparent;              /* 交互区透明 */
  border: none; border-radius: 50%;
  flex: none;

  &::before {                           /* 视觉圆 14dp 居中 */
    content: '';
    position: absolute;
    width: 14px; height: 14px;
    border-radius: 50%;
    background: var(--m-surface-2);     /* #ECECEC ≈ 椒盐 #EDEDED */
  }

  &:active::before { background: var(--m-surface-3); }

  &.is-playing::before {
    background: rgba(var(--m-primary-rgb), 0.12);
  }
}

&__round-icon {
  position: relative;                   /* 叠在视觉圆上 */
  width: 10px; height: 10px;
  stroke-width: 3;                      /* 加粗对齐椒盐图标视觉重量 */
  color: #949fab;                       /* 椒盐加号蓝灰 (148,159,171) */
}

/* 深色主题图标色 */
:global(.dark) .songs-page__round-icon { color: rgba(225, 230, 235, 0.55); }
```

is-playing 图标色：沿用现有逻辑（`color: var(--m-primary)` 覆盖），可加 `&.is-playing { color: var(--m-primary) }` 于 icon。

### D3 更多按钮实心三点（SongsPage.vue）

模板：`ellipsisVertical` 图标 → 实心三点 span 结构：

```html
<button class="songs-page__more-btn" aria-label="更多歌曲操作" @click.stop="openSongActions(...)">
  <span class="songs-page__more-dots" aria-hidden="true">
    <i></i><i></i><i></i>
  </span>
</button>
```

```scss
&__more-btn {
  width: 36px; height: 48px;            /* 交互区 36x48dp（椒盐实测） */
  display: flex; align-items: center; justify-content: center;
  background: transparent;
  border: none; border-radius: 50%;
  flex: none;
  color: var(--m-text);
}

&__more-dots {
  display: flex; flex-direction: column; align-items: center;
  gap: 2px;                             /* 椒盐点中心距 5.3dp - 点径 3.3dp */
  i {
    width: 3.5px; height: 3.5px;        /* 椒盐实心点 ~3.3dp */
    border-radius: 50%;
    background: currentColor;
  }
}
```

注意：more-btn 当前用 `m-button`（variant=clear, rounded），改造后直接用原生 button 或保留 m-button 但内部替换图标节点；确保 `--md` 高度 34px 不干扰 48px（m-button 有 min-height，需覆盖）。

### D4 行内横布局（SongsPage.vue）

```scss
&__row-actions {
  display: flex;
  align-items: center;
  gap: 0;                               /* 圆/⋮ 紧挨（椒盐实测 x264-308/x308-344） */
  flex: none;
  margin-left: auto;
}
```

修 D1 后：封面 x16（li padding 16px）✓；圆 x264-308；⋮ x308-344；文字区 ~198dp 与椒盐一致。标题长行 ellipsis 截断（`__row-title` 已有 min-width:0 + ellipsis）。

### D5 文字间距（SongsPage.vue 覆盖）

```scss
.m-list-item__subtitle {
  margin-top: 2px;                      /* 1px → 2px（SaltUI Item 源码 2dp） */
}
```

## 3. 验证

- CDP 探针（`.tmp/cdp_probe.cjs` 扩展）：读 bodyMargin、round/more rect、⋮ 点元素 rect
- 截图对比：`adb exec-out screencap` + PIL 测行 1 横布局坐标（物理 px ÷3 = dp）
- 对照表：封面 x16 / 圆 x264-308 / ⋮ x308-344（±2dp 容差）

## 4. 风险与回滚

- body margin 全局影响：若个别页面依赖旧边距 → 回滚为 SongsPage 内补偿（最坏方案）
- m-button 尺寸覆盖：more-btn 若保留 m-button，注意其 `--md` min-height 34px 与 padding 0 的冲突，用 `!important` 或直接换原生 button
- ::before 视觉圆 z-index：icon 需 `position: relative` 保证叠在上层
- 每阶段独立提交可回滚

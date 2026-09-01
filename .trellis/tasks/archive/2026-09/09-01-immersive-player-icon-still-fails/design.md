# 设计 — 沉浸式图标点击仍无切换排障（二次修复）

## 1. 背景与现状

- `aad86018` 已新增 `svgRepeatOne/svgOrder` 与 `setRepeatIcon/setShuffleIcon`，并在 `bindClick` 乐观与 `updateProgress` 回写两处替换 `innerHTML+active`，编译通过。
- MuMu 实测“有颜色变但是图标没换”：`active` 高亮切换证明 `bindClick` 的 `classList.toggle` 与 `Android.onAction -> Kotlin toggle*` 链路已通，落入分支“有 -> toggle* 但图标形态未变”。
- 锚点 `full-player.js:25-29` 的 `svgRepeatOne` 采用小 path `M13 9h-1L10.5 10...`（约 2×6px）在 20px 视口内过小，视觉上与 `svgRepeat` 几乎无差；`svgOrder` 的三圆点 `r=1.5` 亦偏小，且未加版本日志导致无法确认 APK 是否已更新至新资源。

## 2. 目标与非目标

- 目标：使 `Repeat ↔ RepeatOne` 与 `Shuffle ↔ Order` 的形状切换在 20px 下肉眼可辨，且通过日志可验证资源已更新与替换链路已执行。
- 非目标：不改手势分流与 Media3，仅前端资源可视性与可观测性增强。

## 3. 方案总览

```
[资源可视性] 放大 RepeatOne 的“1”与 Order 的圆点+线段
[可观测性] initDom 与 set*Icon 内加 Android.log 版本与切换日志
[链路保持] 复用既有 setRepeatIcon/setShuffleIcon 幂等逻辑
```

## 4. 详细设计

### 4.1 SVG 可视性放大

- `svgRepeatOne`：将第二 path 由小 path 改为居中 `<text x="12" y="15" text-anchor="middle" font-size="8" font-weight="800" fill="currentColor">1</text>`，或等效大号 path `M12.5 9v6h2v1.5h-3.8V9h1.8z`，保证 20px 下“1”占宽度 ~30%，与 Repeat 明显区分。
- `svgOrder`：圆点 `r=1.5 -> 2.0`，线段仍 `M8 5h12v2H8z` 等三行，点与线间距保持 4px，确保在深色背景下圆点可见；或采用更粗 `stroke` 但保持 `currentColor`。
- 体积增量 <0.3KB，保持 `20px` 与 `currentColor`。

### 4.2 可观测性

- `initDom` 首行 `Android.log('full-player.js v6 iconFix visible 1+Order','info')`。
- `setRepeatIcon` 内 `Android.log('setRepeatIcon mode='+mode+' isOne='+isOne,'info')`；`setShuffleIcon` 内 `Android.log('setShuffleIcon enabled='+enabled,'info')`。
- `bindClick` 已有 `btn click` 日志保留，便于 `adb logcat -s FullPlayer` 三段对照：`btn click -> set*Icon -> -> toggle* -> updateProgress 回写 set*Icon`。

### 4.3 兼容与回退

- 保持 `innerHTML` 替换与 `active` 切换原子性；若新 SVG 仍不清晰，可回退至仅文本“1”与“≡”的极简符号，但当前放大方案已满足对比度。
- 不新增 Bridge 字段，无需改 Kotlin。

## 5. 涉及文件

- `app/src/main/assets/amll/full-player.js`（唯一必改）
- `app/src/main/assets/amll/full-player.css`（无需改）
- `.trellis/spec/android/features-lyrics-playlist.md §7`（补充可视性与日志契约，按需）

## 6. 验证策略

- 编译 `assembleMusesDebug`、门禁 `lintMusesDebug`。
- 手工：MuMu 上卸载重装最新 APK，手机/平板各点循环/随机 3 次，观察形状是否在 `Repeat(循环箭头) ↔ RepeatOne(箭头+1)` 与 `Order(三线圆点) ↔ Shuffle(交叉箭头)` 间清晰切换；`adb logcat -s FullPlayer` 应见 `v6`、`setRepeatIcon`、`setShuffleIcon` 与 `-> toggle*`。

## 7. 回滚

- 回退 `full-player.js` 的 SVG 放大与日志至 `aad86018` 版，仅保留小 path，图标退化为高亮区分但功能可用。


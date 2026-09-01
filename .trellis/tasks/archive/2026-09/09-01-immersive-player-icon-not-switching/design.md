# 设计 — 沉浸式播放页底部图标未随模式切换

## 1. 背景与现状

- `full-player.js:26-27` 仅 `svgRepeat/svgShuffle/svgQueue/svgMore`，无 `RepeatOne` 与 `FormatListBulleted`；`bindClick:190-210` 与 `updateProgress:360-375` 仅 `classList.toggle('active')`。
- `feature/player/PlayerScreen.kt:1119/1125` Compose 侧已按 `repeatMode==ONE -> RepeatOne else Repeat`、`shuffleEnabled ? Shuffle : FormatListBulleted` 切图标，WebView 与之不一致。
- 点击链路已通：`FullPlayerWebView.kt` `onAction toggleRepeat/toggleShuffle -> rememberUpdatedState -> viewModel.toggle* -> PlayerConnection -> MediaController`，32ms 回写 `updateProgress` 已回传真值，但前端未据真值换图标。

## 2. 目标与非目标

- 目标：补齐 WebView 图标资源与替换链路，使四枚按钮图标与真值同步且乐观/回写一致。
- 非目标：不改手势分流（已由前序任务修复）、不改 Media3 契约、不引入新依赖。

## 3. 方案总览

```
[点击] bindClick toggleRepeat/toggleShuffle
  -> 乐观 setRepeatIcon/setShuffleIcon (换 innerHTML + active)
  -> Android.onAction -> Kotlin toggle* -> PlayerConnection
  -> 32ms updateProgress({repeatMode, shuffleEnabled}) -> set*Icon(真值) 覆验
```

复用现有 `currentColor` 与 `20px` 尺寸，仅新增两段 SVG 字符串与两个 setter。

## 4. 详细设计

### 4.1 前端资源

- 新增常量（`full-player.js` 顶部与 `svgRepeat/svgShuffle` 并列）：
  - `svgRepeatOne`：Material Icons `RepeatOne` 路径，20px，`fill="currentColor"`，参考 Compose `Icons.Filled.RepeatOne` PathData；简化可用 `Repeat` 基础上叠“1”文字 path。
  - `svgOrder`（顺序播放）：Material Icons `FormatListBulleted` 路径，三线+圆点，20px，命名 `svgOrdered`/`svgList`。
- 体积：各 <600B，共 <1.2KB。

### 4.2 抽取复用

```js
function setRepeatIcon(mode){
  const isOne = mode===1;
  ['btn-repeat','bottom-repeat'].forEach(id=>{
    const el=document.getElementById(id);
    if(el){ el.innerHTML=isOne?svgRepeatOne:svgRepeat; el.classList.toggle('active', isOne); }
  });
}
function setShuffleIcon(enabled){
  ['btn-shuffle','bottom-shuffle'].forEach(id=>{
    const el=document.getElementById(id);
    if(el){ el.innerHTML=enabled?svgShuffle:svgOrder; el.classList.toggle('active', !!enabled); }
  });
}
```

- 初始化 `initDom` 后调用一次 `setRepeatIcon(state.repeatMode); setShuffleIcon(state.shuffleEnabled);` 保证首帧一致。
- `bindClick` 乐观分支改为调用 `set*Icon`（先改 `state` 再调 setter），保持 0ms 反馈。
- `updateProgress` 真值分支：若 `p.repeatMode!==undefined` 则 `state.repeatMode=mode; setRepeatIcon(mode)`；若 `p.shuffleEnabled!==undefined` 则 `state.shuffleEnabled=!!p.shuffleEnabled; setShuffleIcon(state.shuffleEnabled)`；彻底替换原 `toggle('active')` 单点。

### 4.3 样式

- `full-player.css` 无需新增，仅 `.mode-bar .btn.active` 已有 `color:white`；图标 `currentColor` 自动跟随。必要时为 `RepeatOne` 的“1”做 `font-size` 微调，但 SVG 已内置。
- 保持 `pointer-events:none` 于 `svg` 不变。

### 4.4 数据流与兼容

- 不新增 Bridge 字段，沿用 `repeatMode:0/1` 与 `shuffleEnabled:bool`；Kotlin 侧 `FullPlayerWebView` 已在 32ms 轮询中下发，无需改 Kotlin。
- 回写幂等：乐观与回写均经同一 setter，快速连点以最后一次回写为准，不闪回。
- 降级：若 `updateProgress` 未携带字段，则沿用 `state` 现值，不触发重绘。

## 5. 涉及文件

- `app/src/main/assets/amll/full-player.js`（唯一必改）
- `app/src/main/assets/amll/full-player.css`（仅按需微调，预期不改）
- `feature/player/lyric/FullPlayerWebView.kt` / `PlayerScreen.kt` / `PlayerViewModel.kt`（本任务不改，仅验证链路）
- `spec/android/features-lyrics-playlist.md §7`（补充前端图标切换契约）

## 6. 异常与风险

- SVG 路径截断导致图标不显示：通过本机构建后 `lint` + 真机目视校验；路径取自 Material Icons 官方导出，非手写。
- 乐观与回写短暂不一致（32ms 内）：setter 幂等，视觉仅一次切换，不闪回。
- 平板/手机两套按钮不同步：setter 遍历四枚 id，确保同步。

## 7. 验证策略

- 编译：`assembleMusesDebug`
- 门禁：`lintMusesDebug`
- 手工：手机竖屏与平板横屏各切 3 次循环/随机，观察图标形状与 active 一致；`adb logcat -s FullPlayer` 核对 `btn click` 与 `updateProgress repeatMode/shuffleEnabled`；回归横滑/下滑/进度/歌词 seek。

## 8. 回滚

- 回退 `full-player.js` 两段 SVG 与 setter 调用至仅 `toggle('active')`，图标退化为单形态高亮，功能仍可用但视觉回归原缺陷，风险低。

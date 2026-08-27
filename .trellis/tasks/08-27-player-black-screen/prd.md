# 沉浸式播放页黑屏修复

> 背景：0.4.4 沉浸式播放页（全 WebView 方案 `AmllWebView` + `frontend/amll-web`）打开即黑屏。已尝试两次修复：`d27b9564` 移除 `LAYER_TYPE_SOFTWARE`、`af4c2940` 声称用 `100vh` 修复高度塌陷，但实查 `af4c2940` 仅改注释未加 `height:100vh`，`#player-ui/#background-layer/#lyric-layer` 仍为 `inset:0` 在 WebView 初始高度为 0 时塌陷为 0。

## Goal
沉浸式播放页在任何机型/模拟器上打开即有内容（非纯黑），背景/歌词/控制均可见。

## 范围
1. 补全 `frontend/amll-web/src/style.css` 中 `#player-ui`、`#background-layer`、`#lyric-layer` 的 `height: 100vh`（相对视口直接撑满，规避 `html/body height:100%` 在 WebView 初次布局为 0 的问题）
2. 执行 `frontend/amll-web` 的 `vite build` 重建 `feature/player/src/main/androidAssets/amll` 产物，更新 `index.html` 与 hashed `assets/*`
3. 保持 `AmllWebView` 硬件加速（不回退 `SOFTWARE` 层）与现有 `ready` 握手、`ResizeObserver` 背景自适应逻辑不变

## 非范围
- 播放功能/歌词解析逻辑改动
- 自动下载更新

## 验收标准
- [ ] 真机/模拟器打开播放页：非黑屏，封面/进度/控制/歌词面板可见
- [ ] `html/body height:100%` 初次为 0 时仍撑满视口（`100vh` 生效）
- [ ] `frontend/amll-web/src/style.css` 含 `height: 100vh` 三处
- [ ] `feature/player/src/main/androidAssets/amll` 产物为最新构建（`index.html` 引用新 hash CSS）
- [ ] ` :app:compileMusesDebugKotlin` 通过，无新增错误

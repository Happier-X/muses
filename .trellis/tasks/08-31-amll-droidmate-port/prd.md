# 沉浸式播放页 AMLL WebView 1:1 复刻 DroidMate — PRD

## 背景

沉浸式播放页当前歌词采用自绘 Compose 与半抄 WebView 混合方案，多轮调试（`ea323204` 手动挂载、`0d4e8575` 容器尺寸、`131346f8` --amll-lp-color、`8f684ca7` mask/mix-blend 清除等）仍不可见或效果与 AMLL 网页版差距大。用户明确要求直接照抄 `Zeehan2005/AMLL-DroidMate` 的 WebView 嵌入方案，本地已拉取对照源码 `.workbuddy/tmp/amll-droidmate`。

## 目标

将沉浸式播放页的歌词与背景渲染完整复刻 DroidMate 的 WebView 方案，使视觉、跟手、逐词、翻译/音译、点击跳转等体验与 AMLL 网页版 100% 一致，结束长期调试。

## 非目标

- 不改播放/队列/恢复/限流/刮削链路
- 不新增在线歌词/封面自动匹配
- 不引入新的播放失败恢复策略
- 不重写为纯 Compose（本次以 WebView 为准，与现行 `features-lyrics-playlist.md` 的“禁止 WebView”冲突由本任务更新 spec 解决）

## 用户需求

1. 作为听歌用户，我在沉浸式播放页看到的歌词与 DroidMate/网页 AMLL 完全一致：流体背景透出、逐词发光扫过、行弹簧波浪、缩放与模糊随滚动自然衰减。
2. 作为听歌用户，我点击任意歌词行立即跳转到该行起始时间，无闪回或延迟导致的“歌词乱跑”。
3. 作为听歌用户，我可开关翻译/音译，开关立即生效且不重建页面或闪黑。
4. 作为平板用户，横屏双栏下歌词面板与手机体验一致，字体不超大，控制条在底部全宽。
5. 作为开发者，WebView 加载不受 `file://` CORS、CSS 隔离、高度 0、`mix-blend` 兼容性等历史坑影响。

## 约束

- 必须使用 `WebViewAssetLoader` 映射 `https://appassets.androidplatform.net/assets/amll/`（规避 `file://` CORS），CSS 内联进 `amll.bundle.js`（`cssInliner`），无外部 CSS 文件。
- Kotlin 侧与前端 API 严格对齐 DroidMate：`window.updateLyrics({lines})/updateTime(ms)/setPaused(bool)/updateAlbumArt(uri)/configureLyricMotion/configureBackgroundEffect/configureLyricBackground/applyFontSettings + window.Android.onPageReady/onLineClick/log`。
- 透明 WebView：`setBackgroundColor(TRANSPARENT)` + `LAYER_TYPE_HARDWARE`，底层 `FlowingLightBackdrop` 或 DroidMate 的 `BackgroundRender` 二选一保持透底一致。
- Chromium 110 兼容：避免高级 `mask-image` 语法导致卡拉OK 不可见，已验证 `mask:none + opacity:1` 探针仍需兜底策略。

## 验收标准

- [ ] 真机（MuMu / 实体 Android 12+）打开含 TTML/LRC 逐词歌词的歌曲，歌词可见、可滚动、逐词发光扫过与网页版一致，非当前行模糊/缩放生效
- [ ] 点击歌词行立即 seek 到该行 `startTime`，不出现旧时间回滚或两帧闪烁
- [ ] 播放/暂停切换时 `setPaused` 去抖，暂停态 `updateTime` 不再高频刷，恢复播放后时间连续
- [ ] 翻译开关仅控制 `translatedLyric/romanLyric` 显隐，切换后当前行与滚动位置不变
- [ ] 专辑封面为 `file://` 时能正常显示为背景（经 `fetch->dataURL` 或 `WebViewAssetLoader /cache` 映射，无混合内容拦截）
- [ ] 持续播放 5 分钟无内存泄漏，页面 `onRelease` 正确 `destroy()`，旋转/切后台/熄屏后恢复正常
- [ ] 横屏平板（`>=768dp 且宽>高`）双栏 + 底部控制条，字体 `clamp(22px,6.5vw,32px)` 不溢出；竖屏平板保持手机式全屏
- [ ] `lintMusesDebug` / `assembleMusesDebug` / 单元测试通过；`features-lyrics-playlist.md` 已更新 WebView 复活契约

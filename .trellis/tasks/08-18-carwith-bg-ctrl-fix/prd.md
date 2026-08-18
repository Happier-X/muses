# 修复 CarWith 车机互联下后台播放不自动切歌与媒体按钮失效

## Goal

小米手机连接 CarWith（车机互联）时，后台播放出现两个问题：
1. **播完一首不自动切下一曲**，直接停在暂停状态；
2. **媒体通知卡片的上一曲/下一曲/播放/暂停按钮全部无效**。

本任务采用**方案 A**（用户已确认）：源头保活 WebView JS（常驻静音 Web Audio 轨阻止 Chromium 冻结隐藏页面），并对既有原生预案与 finished 判定做两处确定性修复。**不 patch 第三方插件、不新增 manifest 组件、不做原生媒体命令直控。**

## 根因（已确认）

- CarWith 连接后手机 WebView 页面不可见，Chromium 对隐藏页面冻结/深度节流 JS：
  - 原生 complete 事件与媒体按钮命令（MediaSession callback → JS keepAlive handler）都链到 WebView JS，JS 冻结后无人处理 → 播完不切歌、按钮无效；
  - JS 冻结期间 `state.position` 停在旧值，complete 事件晚到后 `shouldIgnoreFinished` 依据过期 position 误判「未接近结尾」→ 状态置 `paused`，不切歌。
- 原生预案（`AudioPlayerPlugin.tickAutoNext`）依赖 `AudioManager.isMusicActive()` 判定「播完」；CarWith 音频经 AOA/USB/蓝牙重定向后该值可能失真（一直 true），预案永不触发。
- 普通锁屏后台场景此前已由预案 + 对账 + 心跳（方案 C）修复；CarWith 场景暴露上述盲区。

## Scope（方案 A 包含）

1. **WebView JS 保活**：前端启动常驻静音 Web Audio 轨（gain=0），使隐藏页面被视为「有媒体活动」而不被冻结；仅在**有播放会话**时运行。
2. **finished 判定修复**（controller.ts）：complete/finished 不再依赖 position 的「接近结尾」判定，仅保留 1.5s seek 保护窗丢弃 seek 到未缓冲区的伪 complete。
3. **原生预案判定修复**（AudioPlayerPlugin.kt）：预案「播完」判定从 `isMusicActive()` 扩展为**优先用 capgo `NativeAudio.isPlaying`**（播放器状态，与音频输出路径无关），`isMusicActive` 降为兜底；起播验证窗口同步改用 isPlaying。

## 非目标 / Out of Scope（用户明确排除）

- 不改 `node_modules/@capgo/*`（spec 铁律）。
- 不新增/替换 AndroidManifest 组件，不做 MediaButtonReceiver 原生直控。
- 不处理**车机屏幕按钮**（CarWith 车机端按钮走 transportControls → capgo callback → JS，绕不开第三方插件；用户主要使用手机媒体通知卡片，已确认排除）。
- 不处理系统级进程冻结/杀进程（前台服务保活之外，App 被系统整体冻结时任何 JS/原生兜底均无效）。

## Acceptance Criteria

- [ ] CarWith 连接、手机屏幕关闭/锁屏时：当前曲播完**自动切下一首**，不卡在暂停（本地 + WebDAV 各验证一首链）。
- [ ] CarWith 连接时：展开发送的**手机媒体通知卡片**，上一曲/下一曲/播放/暂停均可用且行为正确。
- [ ] 失活后回到前台：UI/队列/媒体会话与真实播放一致（既有对账链路不回归）。
- [ ] 普通锁屏后台（不连 CarWith）：自动切歌不回归（预案 + 心跳仍工作）。
- [ ] 前台播放/暂停/切歌/seek/单曲循环/列表循环/随机：行为不回归。
- [ ] 静音 Web Audio 不引入可感知副作用：不影响音量、不产生第二个系统媒体卡片、系统媒体音量条正常。
- [ ] lint / `vue-tsc` build / `gradle assembleDebug` 全绿。

## Constraints

- 遵守 `.trellis/spec/frontend/features-player.md`：播放器/媒体会话改动全部收敛在 `src/features/player/`；controller 不得直连原生插件；密码不进日志。
- Web Audio 保活必须**可立即回退**（失败静默、无播放会话时不运行）。
- 不改动 `@capgo/capacitor-native-audio` 的配置参数（`focus/background/backgroundPlayback/showNotification=false`）。

## Notes

- **verify**：本任务改动后需构建 debug APK（`npm run build && npx cap copy android && cd android && ./gradlew :app:assembleDebug`）交用户真机（小米 15 + CarWith）验证；验收记录写回本 PRD。
- 若真机验证发现保活不生效（按钮仍失效），按既定结论评估升级到方案 B（通知按钮原生直控），另开任务。
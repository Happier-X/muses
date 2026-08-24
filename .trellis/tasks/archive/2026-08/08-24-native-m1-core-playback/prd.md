# M1：核心播放 + WebDAV（父任务 08-24-native-compose-rewrite 的子任务）

> 需求全集、架构决策（D1-D4）与全局设计见父任务 `../08-24-native-compose-rewrite/`（prd.md / design.md）。本文件只记录 M1 特有的范围与验收。

## Goal

交付第一个可日常使用的纯原生 APK：能配置本地/WebDAV 音源、扫描入库、浏览歌曲/专辑/艺术家、流播放 WebDAV 歌曲（含缓存）、系统媒体通知可控、后台稳定播放。UI 为 Salt Player 风格基础形态。

## In Scope（M1）

1. `native/` 工程脚手架：Gradle KTS + Version Catalog，模块 `app / core:{model,data,webdav,media} / feature:{library,player,sources}`；Hilt/Room/DataStore/Coil/Media3/navigation-compose 接线
2. SaltTheme 基础层 + GlassSurface 原型 + 侧边栏导航框架 + enableEdgeToEdge
3. 本地库扫描（MediaStore/SAF → jaudiotagger），WorkManager 后台扫描 + 进度 Flow
4. WebDAV 客户端移植（PROPFIND/GET/PUT/DELETE/MOVE + Basic Auth）+ 磁盘缓存移植（ETag/LRU）
5. 音源管理 UI：添加/编辑/删除音源、WebDAV 目录浏览
6. PlaybackService（Media3 MediaSessionService）+ PlayerConnection Flow 封装 + 队列管理
7. UI 对齐：歌曲/专辑/艺术家列表、LibraryDetail、播放页基础形态、MiniPlayer、队列页
8. 首次启动引导（添加音源 → 扫描）

## Out of Scope（后续里程碑）

歌词/AMLL、播放列表管理、响度均衡、刮削写回、云元数据编辑、封面在线匹配、设置页完善、平板双栏布局完善。

## Acceptance Criteria

- [ ] `cd native && ./gradlew lint testDebugUnitTest :app:assembleDebug` 全绿，产出可安装 debug APK
- [ ] 全新安装：引导页可添加本地目录音源与 WebDAV 音源（地址/账号/密码，密码 Keystore 加密存储）
- [ ] 扫描后歌曲/专辑/艺术家列表正确展示（含标签标题/艺术家/专辑、时长）
- [ ] 点击歌曲开始播放；WebDAV 歌曲走缓存或直链均可出声；暂停/继续/上一首/下一首/seek 正常
- [ ] 通知栏媒体卡片显示标题/艺术家/封面，四个按钮可用；锁屏同
- [ ] App 退到后台/熄屏后播放不中断；蓝牙耳机断连自动暂停
- [ ] 缓存命中时不重复下载；缓存有 LRU 上限
- [ ] 单元测试覆盖：WebDAV 解析（MockWebServer）、标签解析、DAO、队列逻辑
- [ ] 真机回归清单（后台播放、焦点抢占恢复、切歌连续操作）通过

## 环境事实

- Java：`C:/Program Files/Android/Android Studio/jbr/bin/java.exe`（PATH 无 java，需设 JAVA_HOME 或 gradle.properties org.gradle.java.home）
- Android SDK：`$LOCALAPPDATA/Android/Sdk`
- 新工程自带 gradle wrapper（参考旧工程用 Gradle 9.6.1）

## Notes

- 执行清单沿用父任务 `implement.md` 的阶段 0~5。

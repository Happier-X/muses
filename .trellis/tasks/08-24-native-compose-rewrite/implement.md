# 执行计划 — M1（核心播放 + WebDAV）

> 本计划覆盖父任务首个实施单元 M1。M2/M3 计划在各子任务中另行编写。
> 开工前：`task.py create "M1 核心播放+WebDAV" --slug native-m1-core-playback --parent .trellis/tasks/08-24-native-compose-rewrite`

## 阶段 0：工程脚手架

- [ ] 建 `native/` 工程：settings.gradle.kts、libs.versions.toml（Version Catalog）、模块骨架 `app / core:{model,data,webdav,media} / feature:{library,player,sources}`
- [ ] Hilt、Room、Coil、Media3、navigation-compose 接线；applicationId `com.muses.player.native`
- [ ] SaltTheme 基础层（colors/shapes/typography）+ GlassSurface 原型 + enableEdgeToEdge
- [ ] 主界面骨架：侧边栏导航框架 + 空白路由
- 验证：`./gradlew :app:assembleDebug` 出包可安装启动

## 阶段 1：数据基座

- [x] `core:model`：Song/Album/Artist/Source 实体与领域模型
- [x] `core:data`：Room 库（songs/albums/artists 表 + DAO）、DataStore 设置仓库、Keystore 凭据仓库
- [x] 本地扫描器（MediaStore/SAF → jaudiotagger 标签，移植 AudioMetadataReader），WorkManager 后台扫描 + 进度 Flow
- 验证：单元测试（标签解析、DAO）；真机扫描本地目录出列表

## 阶段 2：WebDAV

- [x] `core:webdav`：移植 WebDavPlugin 的 PROPFIND/GET/PUT/DELETE/MOVE + Basic Auth，接口化 + 单元测试（MockWebServer）
- [x] 移植 WebDavAudioCache 缓存设计（ETag 校验 + LRU 上限）
- [x] 音源管理 UI：添加/编辑/删除 WebDAV 源、目录浏览页
- [x] WebDAV 歌曲入库扫描（远端 PROPFIND 遍历 + 可选懒加载标签）
- 验证：对真实 WebDAV 服务器的手动验收清单；缓存命中/失效单测

## 阶段 3：播放核心

- [x] `PlaybackService : MediaSessionService` + ExoPlayer；PlayerConnection 封装（播放状态/进度/队列 Flow）
- [x] 队列管理：playNext/playPrevious/seekTo/shuffle/自动连播
- [x] 本地与 WebDAV 曲目统一数据源注入（缓存文件 URI）
- 验证：后台播放、锁屏控制、蓝牙断连暂停、音频焦点抢占恢复

## 阶段 4：UI 对齐

- [x] 歌曲/专辑/艺术家列表页（含虚拟滚动或分页）、LibraryDetail
- [x] 播放页基础形态：全屏封面 + 玻璃控制条 + 进度条 + 播放模式切换
- [x] MiniPlayer + 队列页
- [x] 首次启动引导（添加音源 → 扫描流程）
- 验证：Compose UI 测试关键交互；真机走查 Salt 视觉基准

## 阶段 5：收尾

- [x] 全量检查（lint + test + 真机回归清单）
- [ ] 沉淀 `.trellis/spec/android/` 初始规范（架构分层、Salt 主题、播放契约）
- [ ] 更新父任务 PRD 验收清单勾选

## 验证命令

```bash
cd native && ./gradlew lint testDebugUnitTest :app:assembleDebug
```

## 回滚点

- 每阶段一个 git 提交粒度；`native/` 与旧工程互不触碰，任何阶段可直接删除 `native/` 目录回退，零影响。

## task.py start 前置清单

- [x] prd.md 收敛完成
- [x] design.md / implement.md 就绪
- [ ] implement.jsonl / check.jsonl 各含至少一条真实规范条目
- [ ] 用户明确批准最终规划摘要

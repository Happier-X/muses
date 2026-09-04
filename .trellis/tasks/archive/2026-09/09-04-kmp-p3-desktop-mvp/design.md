# P3 桌面 MVP 技术设计

## 1. 总体结构

```text
composeApp(desktop)              # 新模块：桌面入口 + 自绘标题栏 + 三屏宿主
  └─ 依赖 :core:common（jvm target：Room/DataStore/Ktor/okio 复用）
  └─ 依赖 feature 屏 CMP 化后的 common 代码（库房/播放/设置，平板形态）
  └─ JvmPlayerPort（desktopMain 新写：VLCJ 解码 + 本地队列状态机）
:core:common jvmMain            # P3 补真实 actual
  ├─ DataStorePath.jvm（当前 TODO，占位待实现）
  ├─ Room JvmSQLiteDriver（bundled-sqlite 已在 commonMain api）
  └─ platformNowMs（已有 JDK 实现）
```

- 安卓侧不动：Media3 播放链路、`PlaybackService`、`PlayerConnection` 保持原样。
- VLCJ 为纯 JVM 依赖，只进 `composeApp(desktop)`，不进 commonMain（commonMain 保持平台无关）。
- 平板形态复用点：`TabsLayout.TabletLayout`（768dp 双栏）、曲库 `GridCells.Adaptive`、播放页 `TabletImmersiveLayout` + `TabletBottomBar`——CMP 化时把 `LocalConfiguration`/`statusBarsPadding` 等安卓相关抽成参数或 expect。

## 2. 解码选型（D1：VLCJ 先行，原型三项 gate）

| 候选 | 格式覆盖 | 体积 | 协议 | 结论 |
|---|---|---|---|---|
| VLCJ（VLC 内核） | FLAC/MP3/M4A/OGG/WAV 全 | 大（捆绑原生库几十MB） | LGPL（动态链接合规） | 首选，先行原型 |
| JavaFX Media | FLAC 弱 | 小 | GPL+例外 | 回退位 |
| javax.sound 系 | 仅 WAV/MP3 原生 | 最小 | 各 SPI 不一 | 工作量最大，不选 |

原型必须验证三项（任一不过回退 JavaFX Media）：
1. FLAC 实际可播（含 24bit/高采样常见规格）；
2. seek 精度满足进度条拖动（±1s 内）；
3. 打包后在干净 Windows 机器可跑（原生库随包、无需预装 VLC）。

## 3. JvmPlayerPort 设计

- 实现 `PlayerPort` 接口（commonMain 已定，见 `PlayerPort.kt`）。
- 内部：VLCJ `MediaPlayer` + 本地队列状态机（复刻 repeat/shuffle 语义，对齐 `PlaybackModels.QueueSnapshotData`：items/originalOrder/shuffleOrder）。
- 状态桥接：VLCJ 事件（playing/paused/finished/error/timeChanged）→ `playbackState`（Media3 STATE_* 整型映射）/ `playbackError`（沿用 `PlaybackErrorCopy` 8 条安全文案）/ 进度 StateFlow。
- 播放持久化：复用 `PlaybackStateRepository`（key `playback_snapshot` 冻结）+ `RecentPlaysRepository`（同曲去重置顶/上限50）；失败恢复沿用 `selectNextCandidate` 回绕语义（桌面侧纯逻辑复刻，不依赖 Media3）。
- WebDAV：Ktor Range 下载 + okio 本地 spiller（整文件入缓存复用 500MB LRU 语义；不做 CacheDataSource 边播边缓存对等）。
- 二期预留空实现 + TODO：托盘/SMTC/音频焦点（D2 决策）。

## 4. 三屏 CMP 化（D2：复用平板形态）

- 策略：feature 屏上移 common（或新建 `feature:*-cmp` 共用模块），安卓 `feature:*` 与桌面共用同一套屏代码；窗口宽默认 ≥768dp 自然落平板分支。
- 剥离点：`LocalConfiguration`→ 窗口宽参数；`statusBarsPadding/navigationBarsPadding` → 桌面传零或标题栏高度；`Haze/Blur` 桌面降级（纯色/弱模糊开关，首版可直接关）；Coil3 直接复用（已 KMP）；Koin `koinViewModel` 复用。
- 自绘标题栏（D3）：无装饰窗口 + 自绘栏（最小化/最大化/关闭 + 拖拽 + 双击最大化 + 边缘缩放）；高分屏/多显示器/最大化贴边为已知风险区，首版允许留有限已知问题。
- 图标：Tabler CMP 变体已就绪，经 `TablerIcons` 包装器引用（禁止直引本体，见规范图标约定）。

## 5. 数据层桌面接线（jvmMain 待办）

- `DataStorePath.jvm` TODO → 真实路径（Windows `%APPDATA%/muses` 或同级）。
- Room `JvmSQLiteDriver` 接线（bundled-sqlite 已备）；`schemas/` JSON 与 Migration 测试按 Room-KMP 布局复用。
- 凭据：`expect/actual`（Android Keystore / Windows DPAPI 或 Credential Manager），P2b 未做则 P3 补。
- okio `FileSystem`：`WebDavAudioCache` 500MB LRU、crash 日志 `error_log` 收敛到此（桌面路径 actual）。

## 6. 打包分发（D4：只出安装包）

- 工具：Compose Multiplatform Gradle 插件 + `packageMsi`/`packageExe`（WiX 链）；VLCJ 原生库随包（`--runtime-image` 包含）。
- 签名：msi/exe 代码签名（证书/自签方案原型期定），SmartScreen 警告处理随链解决。
- 不出绿色包；卸载/开始菜单/文件关联随安装包一步到位。

## 7. 兼容与回滚

- 安卓包零影响门禁：P3 全程 `:app:assembleMusesDebug` 通过；feature 屏 CMP 化不得改安卓行为（截图/回归对照）。
- 回滚：composeApp 为新模块，删除即回滚；commonMain 不接受桌面专属 API（VLCJ 零渗入）；单提交 `git revert`。

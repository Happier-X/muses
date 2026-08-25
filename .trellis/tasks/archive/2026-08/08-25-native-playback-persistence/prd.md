# 播放队列与会话持久化（父任务 08-24-native-compose-rewrite 的子任务）

> 背景：Web 层 `src/features/player/{queue,session,recent}.ts` 提供「关掉 App 再打开一切还在」的核心体验：播放队列三序列持久化、播放器配置、冷启动恢复（当前曲+进度）、最近播放记录。原生侧目前只有 Media3 运行时状态，杀进程即丢。本任务补齐该缺口，与 Salt UI 会话低冲突（只动 core 层）。

## Goal

- 播放队列快照持久化：items / originalOrder / shuffleOrder 三份 songId 序列
- 播放器配置持久化：repeatMode / shuffleEnabled / loudnessNormalizeEnabled
- 冷启动恢复：服务启动时按快照重建队列并 seek 到上次进度
- 最近播放记录：同曲去重置顶、上限 50、含展示元数据

## 核心方法论

1. **Web=规格书**：字段语义、默认值（repeat=all、shuffle=false、loudness=true、recent 上限 50）逐一对齐；**队列操作业务逻辑（enqueue/advance/shuffle 算法）不移植**——native 由 Media3 承担，只移植持久化数据契约与读写时机
2. **存储替换**：localStorage 三 key（muses:queue / muses:player-config / muses:playback-session）→ DataStore Preferences JSON snapshot；schema 带 version + 宽松解析
3. **分层铁律**：store 落 `core:data`（复用既有 DataStore 单例）；恢复/保存钩子落 `core/media` 的 PlaybackService（该模块已依赖 core:data 且 @AndroidEntryPoint）
4. **与 UI 会话隔离**：不修改 `feature:*`、`app/**` 下任何文件

## 范围分解

| 批次 | 内容 | Web 基准 |
|---|---|---|
| P0 基建 | core:model `PlaybackModels.kt`（QueueItem/QueueData/PlayerConfig/PlaybackSession/RecentPlayEntry）；core:data `PlaybackStateRepository` + `RecentPlaysRepository`（JSON 编解码内聚） | `queue.ts`(类型段)、`types.ts`(146)、`recent.ts`(58)、`session.ts`(53) |
| P1 服务接线 | PlaybackService onCreate 恢复（songId→song 解析 + seekTo + repeat/shuffle 应用）；EVENT_MEDIA_ITEM_TRANSITION / POSITION_DISCONTINUITY / 周期定时节流保存；onDestroy 强制保存 | `queue.ts`(save* 语义)、`session.ts`(save/load/clear) |
| P2 最近播放 | RecentPlaysRepository 登记（MEDIA_ITEM_TRANSITION 时 record）+ Flow 读取（供未来首页消费） | `recent.ts`(58) |
| P3 迁移测试基建 | core:data 增加 MigrationTestHelper 单测覆盖 v4→v5 / v5 全列校验（补歌词任务 L4 欠账） | — |

## Acceptance Criteria

- [ ] store 仅依赖 DataStore/kotlinx-serialization/core:model，JSON schema 带 version、宽松解析回退默认值
- [ ] 默认值对齐 Web：repeat=all、shuffle=false、loudnessNormalize=true（仅显式 false 关闭）、recent 上限 50、position 非负且 NaN 容错
- [ ] 服务冷启动：有有效快照时恢复队列并 seek；当前曲已被曲库删除时跳过该项但保留其余队列项
- [ ] 保存节流：转场/暂停/销毁触发，避免高频写盘；onDestroy 强制落盘
- [ ] 最近播放：同曲去重置顶、超限裁尾
- [ ] 单测：快照 roundtrip、坏数据回退、recent 去重置顶裁尾；迁移测试 v4→v5
- [ ] `cd native && ./gradlew :app:assembleDebug lint testDebugUnitTest` 全绿
- [ ] 不修改 `feature/*` 与 `nativem1` 下任何文件

## Out of Scope

- 队列操作 UI（QueuePage 归 Salt UI）、ViewModel 状态流暴露
- Android Auto / Wear 表现层
- 队列操作算法移植（Media3 shuffle/repeat 已承担）

## 关键决策

- **D1 store 落 core:data**（2026-08-25）：复用 DatabaseModule 的 DataStore 单例；PlaybackService 注入使用
- **D2 只移植持久化不移植队列算法**（2026-08-25）：Media3 天然承担 enqueue/shuffle/repeat，Web queue.ts 的操作函数是 Vue 响应式时代产物
- **D3 快照合并存储**（2026-08-25）：队列+会话合并为单个 JSON snapshot key（原子恢复语义），配置独立 key

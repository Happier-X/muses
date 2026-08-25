# 执行计划：播放队列与会话持久化

> 全程禁止修改 `feature/*`、`app/**` 下任何文件。每批次完成即提交。

## P0 模型与仓库

- [ ] core:model 新增 `playback/PlaybackModels.kt`（QueueItem/QueueSnapshotData/PlayerConfig/PlaybackSessionInfo/RecentPlayEntry）
- [ ] core:data 新增 `PlaybackStateRepository.kt`（快照+配置读写、JSON 编解码内聚、宽松解析）
- [ ] core:data 新增 `RecentPlaysRepository.kt`（record 去重置顶/裁尾 50、observe Flow）
- [ ] 单测：roundtrip、坏数据回退、position 容错、recent 去重/裁尾
- 验证：`:core:data:testDebugUnitTest`

## P1 服务接线

- [ ] 读 `PlaybackService.kt` / `PlayerConnection.kt` 现状全文
- [ ] onCreate 恢复：配置应用 + 快照重建队列 + seekTo
- [ ] onEvents 监听转场/暂停/discontinuity → 500ms debounce 保存；onDestroy 同步落盘
- [ ] MEDIA_ITEM_TRANSITION 时登记最近播放
- 验证：构建通过；现有测试无回归（服务逻辑以编译+人工冒烟为主）

## P2 迁移测试基建

- [ ] core/data test 新增 MigrationTest：MigrationTestHelper createDatabase(v4) → runMigrationsAndValidate(v5)，断言 username 列存在
- 验证：`:core:data:testDebugUnitTest`

## 收尾

- [ ] 全量验证：`:app:assembleDebug lint testDebugUnitTest`
- [ ] 隔离自检：git status 确认未触碰 feature:* / nativem1
- [ ] 更新 spec/android/index.md「播放契约」节补充持久化契约
- [ ] 提交（P0/P1/P2/P3 分批 commit）

## 回滚点

每批独立 commit。

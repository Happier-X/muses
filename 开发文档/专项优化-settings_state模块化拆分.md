# 专项优化 - settings_state 模块化拆分

## 背景

随着主题、背景、下载、启动刷新、播放控制等功能持续增加，`lib/app/state/settings_state.dart` 已经成为持续膨胀的集中式状态文件。

当前问题主要有三类：

1. 单文件过长，阅读成本逐步上升。
2. 不同领域设置混放，修改时容易误触无关逻辑。
3. 后续继续扩展设置项时，会进一步加剧耦合。

## 本次目标

1. 按设置领域拆分 `settings_state.dart`。
2. 保持外部引用方式尽量不变，降低迁移成本。
3. 为后续继续增加设置项留出清晰结构。

## 拆分策略

本次采用“内部模块化，外部聚合出口保持不变”的低风险方案：

- 新建多个设置模块文件
- `settings_state.dart` 只做导出汇总
- 现有业务文件继续 `import '.../settings_state.dart'`

这样可以避免本次改动扩散到全仓大量 import 调整。

## 拟拆分模块

- `settings_theme_state.dart`
- `settings_background_state.dart`
- `settings_layout_state.dart`
- `settings_playback_state.dart`
- `settings_cache_state.dart`
- `settings_library_state.dart`
- `settings_notification_state.dart`

## 预期收益

- 设置状态职责边界更清晰
- 后续增加设置项时不再持续堆进单文件
- 后面如果需要进一步按功能页重构设置模块，也更容易继续演进

## 风险控制

- 优先保持类名不变
- 优先保持外部导入路径不变
- 本次不改变设置行为，只做结构整理

## 验证建议

1. 各设置页正常打开并读取已有配置。
2. 修改主题、背景、下载路径、启动刷新、通知等开关后，重进应用仍能恢复。
3. `flutter analyze` 保持通过。

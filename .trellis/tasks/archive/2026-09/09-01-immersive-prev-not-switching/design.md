# 设计 — 沉浸式上一曲点击行为定界（>3s 回零语义）

## 1. 背景与现状

- `core/media/PlayerConnection.kt:257`：`skipToPrevious { if(currentPosition>3000) seekTo(0) else if(hasPrevious) seekToPrevious() }`，为 Media3 常见“3秒内回上一曲、超3秒回本曲开头”语义。
- MuMu 实测“进度归零了”：点击上一曲时若 `position>3s` 表现为回零而非切曲，用户误判为未切换。`skipToNext` 无此阈值故下一曲正常。
- 前序 `isInNoSwipeZone` 与 Bridge 已通，日志应有 `btn click previous -> -> previous -> seekTo(0)`，非阻断。

## 2. 目标与非目标

- 目标：在保留或按产品决策调整 3s 语义的前提下，使用户感知与预期一致且可通过沉浸页验证。
- 非目标：不改队列/洗牌/循环核心契约，不引入新依赖。

## 3. 方案总览

提供两档可选，按用户决策落地（实现时二选一）：

- **A 保留现语义（推荐）**：保持 `>3s 回零`，但在沉浸式层加可观测提示与二次点击即切的增强（短时内二次点上一曲必切上一曲），并补充 `PlayerConnection` 注释与 `FullPlayerWebView` 日志透出当前 `position` 以便排障。
- **B 始终切上一曲**：移除 `>3000` 分支，`hasPrevious` 时一律 `seekToPrevious()`，仅无上首时回零；语义更直接但偏离行业惯例，可能导致误触时难以回开头。

## 4. 详细设计

### 4.1 选项 A（推荐）

- `PlayerConnection.skipToPrevious` 保持不变，追加 `Log` 或注释说明 3s 语义；在 `FullPlayerWebView` 的 `onAction previous` 分支日志带 `position` 便于 `adb` 定界。
- 前端无需改；沉浸页二次点击增强（可选）：JS 侧记录上次 `previous` 点击时间戳，若 1.5s 内二次点击且上次已 `seekTo(0)`，则强制再发一次 `previous`（需与 Kotlin 联动，复杂度稍高，故首版仅加日志与说明）。

### 4.2 选项 B（始终切）

- `PlayerConnection.skipToPrevious` 改为：`if(hasPrevious) seekToPrevious() else seekTo(0)`，移除 3s 阈值；队首无前曲时回零。

## 5. 涉及文件

- `core/media/PlayerConnection.kt`（按决策二选一）
- `feature/player/lyric/FullPlayerWebView.kt`（可选日志透出 position）
- `spec` 按需补充 3s 语义契约

## 6. 验证

- 队列 ≥2、position 2s/4s 分别点上一曲：A 方案下 2s 切曲、4s 回零、二次点再切；B 方案下均切曲。
- `assembleMusesDebug`、`lintMusesDebug` 通过。

## 7. 回滚

- 回退 `PlayerConnection` 至原 3s 分支即可。


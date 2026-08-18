# Implement — 蓝牙/车机断开时暂停播放

## 执行清单（按序）

### Phase 1：原生判定逻辑 + 回调

- [ ] P1.1 `AudioPlayerPlugin.kt`：新增 `RemovedOutputDevice` 数据类 + companion 纯函数 `isDisruptiveDeviceRemoved`（design §3.1，TYPE_DOCK 按 `Build.VERSION.SDK_INT >= 26` 保护）
- [ ] P1.2 注册 `AudioDeviceCallback`（load() 中幂等注册，主线程 Handler）
- [ ] P1.3 去抖 + `executeDeviceRemovalPause()`（design §3.3：先置 `jsExpectedPlaying=false` 阻断预案，再 callNativeAudio pause）
- [ ] P1.4 `reportPlaybackStatus` 扩展记录 `jsCurrentAssetId`

### Phase 2：前端上报 assetId

- [ ] P2.1 `native.ts`：`reportBridgePlaybackStatus` 携带 `currentAssetId`；接口加 `assetId?: string`

### Phase 3：单测 + 回归

- [ ] P3.1 新增 `AudioDeviceRemovalPolicyTest.kt`：蓝牙 A2DP 触发 / 有线触发 / USB 触发 / 非输出设备不触发 / 非破坏类型（扬声器、HDMI）不触发 / 空列表不触发
- [ ] P3.2 `./gradlew :app:testDebugUnitTest` 绿
- [ ] P3.3 `npm run lint` + `npm run build` 绿
- [ ] P3.4 `./gradlew :app:assembleDebug` 产出 app-debug.apk

### Phase 4：真机验证（用户侧）

- [ ] P4.1 design §7 真机表 D1-D7，结果回填 PRD 验收勾选

## 验证命令

```bash
cd android && ./gradlew :app:testDebugUnitTest
cd android && ./gradlew :app:assembleDebug
npm run lint
npm run build
```

## Review gates

- G1（P1+P2 完成后）：diff review——判定集合无遗漏/无过度（不包含 TYPE_SPEAKER/HDMI）；`jsExpectedPlaying=false` 置位于 callNativeAudio 之前；manifest/权限/`.gradle` 无意外改动
- G2（P3 完成后）：单测 + 编译 + 前端 lint/build 全绿

## 回滚点

- 原生单类 revert；前端单函数 revert；两文件独立可回滚
- 与 08-18-carwith-bg-ctrl-fix 无代码交集

## 收尾（Phase 5）

- [ ] P5.1 更新 `.trellis/spec/frontend/features-player.md`：新增「音频输出设备移除→暂停」机制条目
- [ ] P5.2 记录 journal
- [ ] P5.3 提交 commit（含任务规划文档）
- [ ] P5.4 待真机验证通过后归档任务
# Implement — CarWith 后台播放修复（方案 A）

## 执行清单（按序）

### Phase 1：keepalive 保活模块

- [ ] P1.1 新增 `src/features/player/keepalive.ts`（设计 §3.2 API；Android 平台判断用 `Capacitor.getPlatform()`；静默降级 + `muses:debug-keepalive` 调试日志）
- [ ] P1.2 单测：`tests/` 下 keepalive 逻辑纯函数可测部分（平台判断/开关判定/幂等标志）；WebAudio 浏览器能力用 mock
- [ ] P1.3 controller 挂接：`playSongInternal` 成功 / `resumePlayback` 成功 → `startKeepAlive()`；`pausePlayback` / `stopPlayback` / 播放失败链终止 → `stopKeepAlive()`（设计 §3.3 表）

### Phase 2：finished 判定修复

- [ ] P2.1 `controller.ts`：`applyNativeState` finished 分支改为「仅 seek 保护窗内视为伪结束，窗外无条件自然播完」；删除 `shouldIgnoreFinished` near-end 判定与不再使用的 `isNearNaturalEnd`
- [ ] P2.2 核对边界：保留 `lastSeekAt`（1.5s 窗）、`resumeSeekGuard`（#53）、`seekPlayback` 缓冲上限限制；确认 `native.ts` 的 complete 事件路径不受影响

### Phase 3：回归与构建

- [ ] P3.1 `npm run lint`（src 0 错）
- [ ] P3.2 `npm run test:unit`（含 P1.2 新增用例）
- [ ] P3.3 `npm run build`（vue-tsc 类型检查 + vite build）
- [ ] P3.4 `npx cap copy android && cd android && ./gradlew :app:assembleDebug` 构建 debug APK（并产出可安装包路径交用户）

### Phase 4：真机验证（用户侧）

- [ ] P4.1 用户小米 15 + CarWith 执行 design §7 V1-V8，结果回填 PRD 验收勾选
- [ ] P4.2 若 V3/V5 判定保活未生效 → 记录证据，评估升级方案 B（另开任务），本任务按实际达成收尾

## 验证命令

```bash
npm run lint
npm run test:unit
npm run build
npx cap copy android
cd android && ./gradlew :app:assembleDebug
```

## Review gates

- G1（P1+P2 完成后）：diff review——keepalive 不阻塞播放路径；finished 判定边界无回归风险；无 node_modules/manifest/原生改动混入
- G2（P3 完成后）：构建全绿 + 改动仅限 2 个前端文件 + spec 文档更新

## 回滚点

- keepalive：单文件 revert；`AudioContext` 在任何异常下已静默降级
- finished 判定：单提交 revert 恢复旧判定（恢复 near-end 校验）

## 收尾（Phase 5，完成阶段执行）

- [ ] P5.1 更新 `.trellis/spec/frontend/features-player.md`：keepalive 机制说明 + finished 判定语义变更（complete 即自然播完，position 仅用于 UI）
- [ ] P5.2 提交 changelog 条目
- [ ] P5.3 归档任务 + 记录 journal
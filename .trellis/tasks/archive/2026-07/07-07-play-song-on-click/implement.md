# 点击播放歌曲实施计划

## 实施步骤

1. 新增 player feature 类型、原生插件接口、feature-local 响应式状态/controller。
2. 新增 `MiniPlayer.vue` 并挂载到 `App.vue`，实现跨页面常驻播放控制。
3. 更新 `SongsPage.vue`：歌曲项可点击播放，当前播放歌曲高亮。
4. 新增 Android Media3 依赖，确认版本与项目 Gradle/compileSdk 兼容。
5. 新增 Android `AudioPlaybackService`（MediaSessionService + ExoPlayer），支持本地 `content://` 和 WebDAV 带 Basic Auth 播放。
6. 新增 `AudioPlayerPlugin`，桥接前端 play/pause/resume/stop/getState 和原生状态事件。
7. 更新 Android Manifest：声明媒体播放前台服务、必要权限、服务 intent。
8. 实现通知栏基础控制：播放/暂停/停止；不做上一首/下一首，除非用户另行确认。
9. 增加播放失败提示，避免泄露认证信息。
10. 补充单元测试：播放本地歌曲调用原生本地参数、播放 WebDAV 歌曲从 SecureStorage 读取密码、密码不进入 localStorage、mini player 跨页面渲染/控制状态。
11. 运行 `npm run test:unit -- --run`、`npm run lint`、`npm run build`，并运行 `cd android && ./gradlew :app:compileDebugKotlin`。

## 验证命令

- `npm run test:unit -- --run`
- `npm run lint`
- `npm run build`
- `cd android && ./gradlew :app:compileDebugKotlin`

## 风险点

- Media3 依赖版本与当前 Capacitor/Android Gradle 配置可能需要调试。
- WebDAV + Basic Auth 的 ExoPlayer data source 需要正确配置 header；错误处理不能泄露密码。
- Android 13+ 通知权限可能影响通知显示，需要给出用户可理解提示或后续补权限请求。
- 使用 `ContextCompat.startForegroundService(...)` 启动播放服务后，服务必须在 5 秒内调用 `startForeground(...)`，否则系统会触发 `Context.startForegroundService() did not then call Service.startForeground()` 并杀掉应用。
- 后台播放和通知栏控制需要真机/模拟器手工验证，单元测试只能覆盖前端边界。
- 本地 `content://` 播放依赖 SAF 权限是否仍然有效。
- 前端不应记录 WebDAV 密码或把密码放入响应式 UI 状态。

## Android 手工验证清单

- 本地歌曲：点击播放、暂停、继续、停止、切换歌曲、无权限或文件失效时错误提示。
- WebDAV 歌曲：正确密码播放、密码缺失提示、401/403 或网络错误提示。
- 跨页面：在歌曲页播放后切到专辑/艺术家/音源页，迷你播放器仍显示并可控制。
- 后台播放：按 Home、锁屏后继续播放。
- 通知栏：通知出现，可播放/暂停/停止；停止后迷你播放器同步状态。
- 安全检查：扫描 `localStorage` 不含密码或 Authorization Header，日志不输出认证信息。

## 启动前检查

- 用户已确认需要跨页面常驻迷你播放器、后台播放和通知栏控制。
- 用户确认通知栏本轮是否只需要播放/暂停/停止，还是还需要上一首/下一首。
- `implement.jsonl` 和 `check.jsonl` 已加入前端规范上下文。

# 技术设计：设置页日志功能

## 总体形态

轻量自研日志设施（不引入 Timber 等框架——需求仅 warn/error + crash，依赖最小化），
落在 `core:data` 模块，接口 + Hilt 绑定对齐既有模式。

```
MusesApplication.onCreate ──安装──> CrashHandler(包装 defaultUncaughtExceptionHandler)
                                        │ 崩溃时写 filesDir/error_log/crash-latest.txt 并委托原 handler
core:* 各模块 ──注入──> ErrorLogStore.log(level, tag, msg, throwable?)
                                        │ 写入内存环形缓冲(ArrayDeque, cap=500, synchronized)
app SettingsScreen ──注入──> ErrorLogStore.dump() / latestSummary ──> 剪贴板
```

## 边界与契约

### ErrorLogStore（core/data/log/ErrorLogStore.kt）

```kotlin
interface ErrorLogStore {
    /** 记录一条日志（warn 及以上才会被保留） */
    fun log(level: Level, tag: String, message: String, throwable: Throwable? = null)
    enum class Level { WARN, ERROR }
    /** 最近一条 error 的摘要（时间 + 首行消息），无则 null —— 供设置页副标题 StateFlow */
    val latestSummary: StateFlow<String?>
    /** 格式化全部缓冲为可复制文本（含持久化的上次 crash），无内容返回 null */
    suspend fun dump(): String?
}
```

### CrashHandler（core/data/log/CrashHandler.kt）

- `install(context: Context)` 在 `MusesApplication.onCreate` 最先调用
- 包装线程默认 `UncaughtExceptionHandler`：序列化当前缓冲 + 堆栈写入
  `context.filesDir/error_log/crash-latest.txt`（阻塞 IO 可接受——进程即将死亡），
  然后**必须**委托给原 handler（保持系统崩溃流程/ANR 对话框行为）
- 启动时若 `crash-latest.txt` 存在，读回并以「上次会话崩溃」标记插入缓冲头部，读完即删

### Hilt 绑定

`@Binds abstract fun bindErrorLogStore(impl: RingBufferErrorLogStore): ErrorLogStore`
加入现有 `RepositoryModule`（impl 为 `@Singleton`）。

### R2 关键埋点点位

| 位置 | 改动 |
|---|---|
| `core/media/playback/PlaybackService.kt:197 onPlayerError` | 补 `log(ERROR, "Playback", ...)` |
| `core/media/scanner/WebDavLibraryScanner.kt:56` | 静默 catch 补 `log(ERROR, "WebDavScan", e)` |
| `core/media/scanner/ScanWorker.kt:58` | 同上 |
| 其余静默 catch | 本任务不顺手改，控制 diff 半径 |

### R3 设置页 UI（app 模块 SettingsScreen.kt）

- 「音频」分组后新增 `SettingsBlockTitle("反馈")` + 卡片容器 +
  `SaltListItem(title="复制报错日志", subtitle=latestSummary ?: "暂无报错记录")`
- onClick：`viewModel.dumpAndCopy()` → ClipboardManager 复制 → 复用现有 toastMessage 机制提示
- dump 为空时 Toast「暂无可复制的日志」

## 数据流示例（复制文本格式）

```
[Muses 错误日志] v<VERSION_NAME> @ <yyyy-MM-dd HH:mm>
--- WARN  2026-08-26 21:30:01 [WebDavScan] 列表请求失败 (attempt 1)
--- ERROR 2026-08-26 21:30:05 [Playback] 播放失败
    java.io.IOException: ...
== 上次会话崩溃 ==
--- FATAL ...
    StackTrace...
```

## 权衡记录

- **不引入 Timber**：仅两级日志 + 单一消费方，自研 <100 行；引入框架收益为负（多一个依赖、仍要写自己的 Appender）
- **放 core:data 而非新建 core:log**：media/webdav/scrape 已依赖 core:data，零构建脚本改动；代价是 core:data 继续"增重"，若未来日志需求膨胀再拆
- **crash 只存最新一份文件**：MVP 不做崩溃历史归档，避免存储管理复杂度
- **环形缓冲纯内存**：非崩溃日志不落盘，进程被杀即失——符合"反馈最近问题"的用例

## 回滚考虑

改动集中在新文件 + SettingsScreen 小段 UI + 3 处单行埋点 + Application 一行 install，
git revert 单 commit 即可完全回滚。

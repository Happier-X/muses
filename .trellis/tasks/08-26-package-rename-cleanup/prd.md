# app 模块包名清理（轻量任务）

M1 迁移期遗留命名 `com.muses.player.nativem1` 在纯原生收官后已无意义。
重命名为 `com.muses.player`：namespace、目录结构、全部 package/import 同步；
applicationId 不变（flavor 决定），升级安装兼容无影响。

## Acceptance Criteria
- [x] 全仓无 nativem1 残留引用
- [x] assembleMusesDebug 编译通过，APK package name = com.muses.player

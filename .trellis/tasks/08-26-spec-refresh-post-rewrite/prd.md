# Spec 文档体检更新（纯原生收官后）

> 背景：纯原生重写完成 + 仓库结构扁平化（native/ 移除）+ Web 层删除。spec 文档存在过时路径、废弃层、脱节描述。

## 问题清单
1. android/index.md 构建命令 `cd native && :app:assembleDebug` 双重过时 → 仓库根 + assembleMusesDebug（多 flavor）
2. frontend/ 层 10 文档随 Web 层废弃 → 删除目录，android/index.md 如有引用同步清理
3. index.md M1 时代表述（迁移策略/双轨过渡）清理；播放链路描述与现行实现对齐（流播+CacheDataSource 边播边缓存）
4. features-lyrics-playlist.md 对齐全 WebView 播放页现状

## Acceptance Criteria
- [x] 全 spec 无 `cd native`；frontend/index.md 加废弃声明保留历史参照
- [x] 构建命令已实测：仓库根 `./gradlew :app:assembleMusesDebug` BUILD SUCCESSFUL
- [ ] 抽查构建命令可直接执行成功

## 决策：frontend 层处置
frontend/ 10 个文档描述的 Vue3/konsta/Tailwind 栈已随 Web 层删除。
但其中部分内容仍有历史参照价值（features-player.md 是播放器行为规格书来源、
features-scrape.md 是刮削语义规格书来源）——**保留目录但重命名 index.md 加废弃声明**，
避免 AI 会话误把 Vue 规范当现行标准。

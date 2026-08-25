# 执行计划：歌词在线搜索数据层

> 前置：每批次开工前读对应 Web 基准文件全文；批次完成即提交一次（独立回滚点）。
> 全程禁止修改 `native/feature/*`、`native/app/**` 下已有文件（L4 的 ViewModel 最小接线除外，动手前先 git status 确认无 UI 会话未提交改动触碰同一文件，有则跳过并注明）。

## L0 基建

- [x] 新建 `native/core/lyrics` Android Library 模块（依赖 core:model / OkHttp / kotlinx-serialization / coroutines），注册 settings.gradle.kts
- [x] core:model 新增 `lyrics/LyricsModels.kt`（OnlineLyricsQuery/Format/Source/ProviderHit/MatchResult、AmllIndexEntry/AmllMatchQuery/AmllMatchResult/AmllRawIndexLine）
- 验证：`:core:lyrics:assembleDebug`

## L1 平台五源 provider

- [x] 读 `providers/wyCrypto.ts`(221)、`wy.ts`(161)、`kg.ts`(144)、`kw.ts`(94)、`mg.ts`(86)、`tx.ts`(235)、`qrc.ts`(58)、`util.ts`(66) 全文
- [x] `crypto/WyCrypto.kt`：MessageDigest MD5 + AES-128-ECB eapi 加密/解密
- [x] 五源 provider + `QrcDecoder.kt` + `LyricsProviderUtil.kt`(pickBest)
- [x] `PlatformChain.kt`：默认链 kw→tx→wy→kg→mg
- 验证：MD5 测试向量；MockWebServer 各源解析样例；wyCrypto 往返

## L2 LRCLIB + AMLL TTML DB

- [x] 读 `lrclib.ts`(149)、`amllTtmlDb.ts`(374)、`score.ts`(192)、`normalize.ts`(35) 全文
- [x] `lrclib/LrclibProvider.kt`
- [x] `amll/AmllIndex.kt`：jsonl 宽松解析 + exactTitles/titleTrigrams 索引 + 单飞加载（Mutex）
- [x] `amll/AmllScore.kt`：SCORE_WEIGHTS/DURATION_TOLERANCE_SEC=5/MIN_ACCEPT_SCORE/scoreEntry/classifyMatch/findBestMatch
- [x] `amll/AmllTtmlDbClient.kt`：主流程 + 双缓存 + 超时 20s/12s
- 验证：本地 jsonl 样本解析、评分分级、缓存过期单测

## L3 编排 + 接线

- [x] 读 `match.ts`、`index.ts` 全文；**mergeTranslation 不移植**（依赖 AMLL LyricLine/WebView JS 解析链路，留渲染层数据任务），决策已记入 prd
- [x] `LyricsMatcher.kt`：amll 优先 → fallback 串行 → network/parse 区分
- [x] core:scrape 增加 `:core:lyrics` 依赖；editmeta 新增 Port 适配器；Hilt 装配（LyricsModule + ScrapeModule 补绑）
- 验证：matchOnlineLyrics 四结局单测；mergeTranslation 关键分支；全量构建

## L4 username 补缺

- [x] Source 模型/实体加 username 列 + MIGRATION_4_5 + DB v5 + Mapper
- [x] SourcesViewModel 表单保存处最小接线（若该文件有 UI 会话未提交改动，跳过并注明）
- 验证：迁移单测（MigrationTestHelper 若已有基建）；全量构建

## 收尾

- [x] 全量验证：`cd native && ./gradlew :app:assembleDebug :lintDebug testDebugUnitTest`
- [x] 冲突自检：确认未触碰 feature:* / nativem1 已有文件（L4 例外项需显式说明）
- [x] 更新 `.trellis/spec/android/features-scrape-engine.md` 或新增 lyrics 特征规范
- [x] 提交（每批独立 commit）

## 回滚点

每个 L 批次一个 commit；失败 revert 该批次即可。

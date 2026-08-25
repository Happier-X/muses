# 歌词在线搜索数据层

> 任务 08-25-native-lyrics-online。翻译自 Web 层 `src/features/lyrics`，全部落在新建 `native/core:lyrics` 模块（依赖 core:model / OkHttp / kotlinx-serialization / Hilt）。

## 结构与链路

```
core:lyrics
├── crypto/WyCrypto.kt       eapi 参数加密：MD5(MessageDigest) + AES-128-ECB(PKCS5)，密钥 e82ckenh8dichen8，输出大写 hex
├── provider/                平台五源 kw→tx→wy→kg→mg（PlatformChain.defaultChain）
│   └── qrc/QrcDecoder.kt    QQ 私有 3DES（表机械提取自 AMLL lyric@1.0.2）+ zlib inflate + LyricContent 抽取
├── lrclib/LrclibProvider.kt 精确 get → 检索 fallback；仅 syncedLyrics；时长 ±3s 优先；UA Muses/0.1.2
├── amll/                    TTML 聚合库：jsonl 索引(20s) + trigram/exact 候选索引 + findBestMatch(权重 100/60/25/15/8、时长容差 ±5s)
│                            TTML 下载 12s；song 缓存 256、负缓存 TTL 5min；单飞加载（Mutex），失败不缓存
└── LyricsMatcher.kt         matchOnlineLyrics：amll 优先 → fallback 串行任一命中即停；network/parse 区分（parse 仅在 fallback 为空时上报）
```

## 接线契约

- `core.scrape.editmeta.LyricsSearchPort` 由 `AmllLyricsPort`(id=amll) 与 `ProviderLyricsPort`(平台/LRCLIB) 适配实现；依赖方向 scrape→lyrics→model 无环
- WebDAV 写回认证用户名：`Source.username`（Room v5，MIGRATION_4_5）；密码仍走 CredentialsRepository

## 踩坑记录（新增）

- **QRC 私有 3DES**：非标准 DES！必须逐表移植 AMLL 的 custom-des（KEY_1/2/3="!@#)(*$%"/"123ZXC!@"/"!@#)(NHL"）。JS `subarray` 输出视图 → Kotlin 需显式 outOffset 参数，否则多块解密互相覆盖数组头部
- **JS 无符号运算移植**：`>>>0` 用 Kotlin Int 位运算天然等价（同 32 位布局），比较/打印时才需 `.toLong() and 0xFFFFFFFF`
- **normalizeText 会剥掉 (Live)/Remix 等后缀词**：评分测试造数据时注意 "X (Live)" 与 "X" normalize 后相等是 EXACT 不是 CONTAINS
- **大常量表移植用脚本机械提取**（node 读 dist 提取数组生成 Kotlin），禁止手抄；提取脚本模式见任务 journal

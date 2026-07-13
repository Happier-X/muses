# 实施清单：在线匹配封面

## 有序步骤

1. **cover feature 骨架**
   - `src/features/cover/types.ts` / `match.ts` / `normalize.ts`（可复用 lyrics 侧标题归一思路或精简版）
   - provider 接口 + 编排：iTunes → kw

2. **iTunes provider**
   - search + 选结果 + artwork URL 放大
   - 单测 mock fetch

3. **kw provider**
   - 移植 search + getPic（最小必要）
   - HTTP 适配（CapacitorHttp 或现有 WebDav/原生 request 能力；优先已有可跨域方式）
   - 单测 mock

4. **原生 cacheRemoteCover**
   - 下载到 `cache/covers`
   - 返回 `file://`；失败 null
   - `native.ts` 封装

5. **controller 接入**
   - `scanSongMetadata` 完成后 / 已有 ready 仍无封面时调用
   - token、负缓存、upsert、sync UI + media session
   - 不阻塞播放

6. **测试**
   - 触发/跳过/双源/失败静默/防串/存储安全 URI

7. **spec**
   - state-management / features-player：在线封面仅补缺、写回、源顺序、与歌词分离

8. **验证**
   - vitest / lint / vue-tsc

## 回滚点

- controller 触发一行可关；provider 独立可删

## 启动前

- [x] 决策收敛
- [x] prd / design / implement
- [ ] 用户审阅并确认开始
- [ ] curate jsonl
- [ ] `task.py start`

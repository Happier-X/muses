# 实现清单：amll-ttml-db 歌词匹配

## 有序步骤

1. **新建 `src/features/lyrics/`**
   - `types.ts`：索引行、匹配结果类型
   - `normalize.ts`：歌名/歌手规范化
   - `score.ts`：打分 + 选最佳
   - `amllTtmlDb.ts`：ensureIndex / fetchTtml / matchAmllTtmlLyrics + 内存缓存
   - 导出 barrel（按需）

2. **扩展播放器状态**
   - `src/features/player/types.ts`：`lyricsFormat`、`onlineLyricsStatus`
   - `controller.ts`：`playSong` / `stopPlayback` / 切歌重置；触发匹配；token 防串曲
   - 成功写入 `state.lyrics` + `lyricsFormat: 'ttml'`；失败保持本地

3. **PlayerPage**
   - `lyricLines`：按 `lyricsFormat` 分支 `parseTTML` / `parseLrc`
   - 空态 / 匹配中文案（R8）

4. **单测**
   - normalize + score 边界
   - 匹配选取 / 阈值
   - 缓存命中
   - token 过期丢弃（controller 或 lyrics 模块）
   - Player 解析路径（可 mock fetch）

5. **spec**
   - `features-player.md`：在线匹配优先级、状态、不写回
   - `component-guidelines.md`：歌词页空态/匹配中文案

6. **验证**
   - `npx vitest run`
   - `npm run lint`
   - `npx vue-tsc --noEmit`

## 风险点

| 风险 | 缓解 |
|------|------|
| 索引 ~1.5MB 首次拉取慢 | 匹配中文案；可并行播放；内存缓存 |
| 模糊匹配误伤 | 阈值；仅歌名强相关才采纳 |
| 切歌串词 | songId + token |
| CDN 不可用 | 静默回退本地/空态 |
| parseTTML 与 LyricPlayer 类型 | 用 `parseTTML(...).lines` 对齐现有 `LyricLine[]` |

## 回滚点

- 去掉 controller 中匹配调用 + PlayerPage 分支即可完整回退。

## 启动前

- [x] prd / design / implement 齐备
- [ ] 用户审阅规划产物
- [ ] curate implement.jsonl / check.jsonl
- [ ] `task.py start`

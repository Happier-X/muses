# 实现清单

## 顺序

1. **纯函数** `src/features/player/loudness.ts`（或等价）  
   - [x] `parseReplayGainDb(raw: string): number | null`  
   - [x] `dbToPlaybackVolume(db: number | null | undefined, enabled: boolean): number`  
   - [x] 单测边界

2. **原生读标签** `AudioMetadataReader.kt`  
   - [x] 解析 track RG → `replayGainTrackDb`  
   - [x] 与现有 JSObject 元数据合并

3. **曲库类型与 sanitize**  
   - [x] `SongItem.replayGainTrackDb?`  
   - [x] 扫描/懒扫描写回路径确认

4. **player-config**  
   - [x] `loudnessNormalizeEnabled` 默认 `true`  
   - [x] load/save 兼容旧 config

5. **native + controller**  
   - [x] play 路径应用 volume  
   - [x] 开关即时生效  
   - [x] 切歌/stop 不串

6. **SettingsPage** toggle + 说明文案

7. **spec** `features-player.md` 边界

8. **验证** lint / unit ✅；关 #46；commit / archive（收尾中）

## 验证命令

```bash
npm run lint
npm run test:unit -- --run
```

## 审查门

- 无全库 R128 扫描
- 无标签不瞎调
- volume 始终 clamp 到插件允许范围

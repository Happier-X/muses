# 歌词云端获取：amll 聚合库始终参与（不随平台过滤）

## Goal

编辑歌曲信息「歌词」tab 的云端歌词获取中，AMLL 聚合库（amllTtmlDb，TTML 逐行时间轴、质量最高）应始终参与候选，不因用户选择了具体平台（如网易云/LRCLIB）而被跳过。

## Background

当前 `searchEditCloudMeta` 中，当 `lyricsPlatform !== 'all'` 时设置 `includeAmll = false`，理由是"保持纯平台来源"。但 amll 是独立的高质量聚合来源（TTML 格式在质量排序中排最前），跳过它会丢失最佳候选，与用户"选择来源"的预期不符。

## Requirements

1. amll 聚合库始终参与歌词候选搜索，与 `lyricsPlatform` 选择无关。
2. 平台 chips（网易云/QQ音乐/酷狗/酷我/咪咕/LRCLIB）仅过滤各平台 provider。
3. 质量排序逻辑不变（ttml/yrc/qrc 优先）——amll 的 TTML 自然排最前。
4. 候选来源显示 AMLL（已有 `cloudSourceLabel` 映射，无需改动）。

## Acceptance Criteria

- [ ] 选「网易云」获取歌词时，候选含 amll（source=AMLL）+ 网易云 provider 结果
- [ ] 选「LRCLIB」获取歌词时，候选同样含 amll
- [ ] 「全部」行为不变（本来就含 amll）
- [ ] lint / build / assembleDebug 通过；模拟器 CDP 实测平台选择后候选仍含 AMLL

## Notes

- 轻量任务，PRD-only。
- 改动集中在 `src/features/editMeta/searchEditCloudMeta.ts` 的 lyricsPlatform 分支（移除 includeAmll=false）。
- 清理不再需要的 `includeAmll` 参数逻辑（searchLyricsDimension 调用处恒传 true）。

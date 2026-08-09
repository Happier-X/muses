# 编辑歌曲信息：歌词渠道与元信息渠道拆分（tab）

## Goal

编辑歌曲信息弹窗内，将「歌词」获取渠道与「其他元信息」（文本/封面）获取渠道拆分为两个 tab，各自独立选择来源平台，互不干扰。

## Background

当前云端元信息 section 用一个来源平台 chips（全部/网易云/QQ音乐/酷狗/酷我/咪咕/iTunes）同时控制文本、封面、歌词三个维度的 provider 过滤。但歌词渠道与文本/封面渠道不同：

- 歌词：wy / tx / qrc / kg / kw / mg / lrclib（无 iTunes，有 LRCLIB）
- 文本：kw / tx / wy / kg / mg
- 封面：itunes / kw / tx / wy / kg / mg

用户希望像 MusicTag 一样按渠道分开管理。

## Requirements

1. 编辑弹窗顶部加两个 tab：「基础信息」与「歌词」。
2. 「基础信息」tab：标题/艺术家/专辑输入、封面选择、ReplayGain + 云端元信息 section（仅文本+封面维度）。
3. 「歌词」tab：歌词文本 textarea + 云端歌词 section（仅歌词维度）。
4. 来源平台 chips 分开：
   - 基础信息：全部/网易云/QQ音乐/酷狗/酷我/咪咕/iTunes（现状）
   - 歌词：全部/网易云/QQ音乐/酷狗/酷我/咪咕/LRCLIB
5. 各 tab 独立「获取」按钮、状态文案、候选列表、勾选/应用；切换 tab 不丢失已获取结果。
6. 保存/取消操作栏不随 tab 切换（表单底部固定）。

## Acceptance Criteria

- [ ] 弹窗内 tab 可切换，「基础信息」与「歌词」内容分区正确
- [ ] 基础信息 tab 获取仅搜文本+封面（歌词维度不请求）
- [ ] 歌词 tab 获取仅搜歌词（文本/封面维度不请求）
- [ ] 歌词来源 chips 含 LRCLIB、不含 iTunes；基础信息 chips 含 iTunes、不含 LRCLIB
- [ ] 歌词平台选择只影响歌词 provider（如选网易云仅 wy 歌词、选 LRCLIB 仅 lrclib 歌词、QQ音乐含 tx+qrc）
- [ ] 切 tab 后已获取候选仍保留
- [ ] 候选来源显示中文平台名（复用 cloudSourceLabel）
- [ ] lint / build / assembleDebug 通过；模拟器 CDP 实测两个 tab 的获取流程

## Notes

- 轻量任务，PRD-only，无 design.md / implement.md。
- 复用现有 `searchEditCloudMeta`（扩展 dimensions / lyricsPlatform 参数），不新写搜索函数。

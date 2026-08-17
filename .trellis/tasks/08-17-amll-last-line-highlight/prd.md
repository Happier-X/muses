# AMLL 歌词播完后保持最后一句高亮

## Goal

歌曲播放到最后一句之后（自然播完 / 暂停在末尾），AMLL 歌词**保持最后一句高亮**，而不是所有歌词失活变模糊。对齐主流音乐播放器（网易云、QQ 音乐等）的歌词行为。

## Background（根因）

`PlayerPage.vue` 中 AMLL `LyricPlayer` 的 `:current-time="lyricRenderTime"`：

```ts
const lyricRenderTime = computed(() => playerOverlayVisible.value ? playerState.position * 1000 : hiddenLyricTime.value)
```

播完后 `playerState.position` 停在 ≈时长（毫秒化后）**超出最后一行歌词的结束时间** → AMLL 找不到活动行 → 所有行失活模糊。

## 方案

在 `lyricRenderTime` 计算中钳制上限到最后一句歌词的开始时间（`lyricLines[last].startTime`）：

- 播放中：不受影响（position < 最后一句开始前正常走完）；
- 播完/暂停在末尾：current-time 钳制到最后一句开始 → AMLL 始终认为最后一行是当前行 → 保持高亮；
- 无歌词（lyricLines 空）：不钳制，原样返回。

注意：`lyricLines` computed 定义在文件后部（~1800 行），`lyricRenderTime`（852 行）在 getter 中引用——computed 惰性求值，组件 setup 完成后访问时已初始化，无 TDZ 问题；但需确认依赖链无循环（lyricLines 不依赖 lyricRenderTime）。

## Acceptance Criteria

- [ ] 歌曲自然播完后最后一句歌词保持高亮，其余行保持正常模糊/非高亮层级
- [ ] 暂停在末尾时最后一句同样保持高亮
- [ ] 播放中歌词行为不回归（切行、单词高亮、用户滚动）
- [ ] 无歌词曲目不回归
- [ ] lint / build 全绿

## Out of Scope

- 修改 AMLL 组件源码
- 歌词滚动/点击 seek 行为调整

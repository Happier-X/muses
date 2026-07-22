# 设计：歌词页浮动按钮按需显示

## 边界

| 在范围内 | 不在范围内 |
|----------|------------|
| `PlayerPage.vue` 歌词面板 `.lyric-floating-actions` | 控制页主控、MiniPlayer |
| 显示状态机 + 3s idle timer + CSS fade | AMLL 源码修改 |
| 与现有 touch/手势共存 | 新路由/全局 store |

## 状态机

```
hidden  --(歌词面板 tap / 歌词区 scroll-like move)--> visible
visible --(3s idle)--> hidden
visible --(再交互 / 点 fab)--> visible（重置 timer）
any     --(activePanel !== 1 或 overlay 关闭)--> hidden（清 timer）
```

- `lyricChromeVisible: boolean`（或等价 ref）
- `LYRIC_FAB_IDLE_MS = 3000`
- `LYRIC_FAB_FADE_MS ≈ 180`（CSS `transition: opacity`）

## 触发

1. **点击**：在 `.lyric-panel` 上 `pointerup`/`click`（或 touchend 且水平位移 < 阈值、非横滑切页、非下滑关闭），且目标不在已显示且可点的 fab 上时，调用 `revealLyricChrome()`。  
   - 点在已显示的 fab 上：走按钮原逻辑，并 `revealLyricChrome()` 重置 timer。
2. **滑动歌词**：`isLyricPanelTarget` 且判定为纵向滑动（或任意在歌词面板内的 move 超过阈值），`revealLyricChrome()`。  
   - 不改变 `canStartVerticalDismiss` 已禁止歌词区下滑关闭的语义。
3. **不在控制页**（`activePanel === 0`）触发 reveal。

## 可见性实现

- 容器始终挂载（保留翻译状态、避免 v-if 丢 DOM）：
  - 隐藏：`opacity: 0` + 容器 `pointer-events: none`
  - 显示：`opacity: 1`；子 `.lyric-fab` 仍 `pointer-events: auto`
- `transition: opacity 180ms ease`
- **禁止**隐藏态可点热区

## 生命周期

- `watch(activePanel)`：离开歌词页 → `hideLyricChromeImmediate()`
- `watch(playerOverlayVisible)`：关闭 → 同上
- `onUnmounted`：清 timer
- 打开 overlay 进歌词页：默认 hidden

## 手势兼容

| 手势 | 行为 |
|------|------|
| 歌词行 click seek | 可同时 reveal；seek 逻辑不变 |
| 横滑切面板 | 切走时 hide；reveal 不抢横滑阈值 |
| 控制页下滑关闭 | 无关 fab |
| 点 fab | 仅 visible 时可点；toggle + reset timer |

## 测试要点

- 初始 hidden（无 `.is-visible` 或 aria/class 契约）
- 触发 reveal 后 visible
- fake timer 3s 后 hidden
- 切 `activePanel` 0 立即 hidden
- 翻译/播放标签与图标既有测试仍通过

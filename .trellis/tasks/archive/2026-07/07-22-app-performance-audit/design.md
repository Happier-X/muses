# 性能审计报告

## 结论摘要

第一轮 #50 已处理「播放进度高频 bridge」和 SongsPage 全量列表，但仍有一个**高置信 P0** 和多个 P1：

1. **P0：扫描每首歌都全量序列化并写 localStorage，复杂度接近 O(N²)**。
2. **P1：队列解析反复全量 parse 曲库，并用 `find` 做 Q×N 查找**。
3. **P1：PlayerPage 关闭后仍保活，AMLL BackgroundRender/LyricPlayer 可能继续 GPU/RAF 工作**。
4. **P1：播放队列可包含整库，但 QueuePage 仍全量渲染 Ionic sliding item**。
5. **P2：首次在线歌词加载 1.44 MiB JSONL，并在主线程逐行 parse + 全索引评分**。
6. **P2：Albums / Artists / PlaylistDetail 等仍是全量列表；取决于库规模。
7. **P2：运行时负缓存 Map 无容量上限，长会话可能增长**。

---

## F1 · P0：曲库扫描逐首全量写库

### 证据

`scanner.ts` 循环每个文件调用 `upsertSong(input, songs)`；`upsertSong` 在 inserted/updated 时调用 `saveSongs(songs)`。

`saveSongs` 每次执行：

1. `songs.map(sanitizeSongForStorage)`
2. `JSON.stringify(整个曲库)`
3. `localStorage.setItem`（WebView 主线程同步 I/O）
4. dispatch `SONGS_UPDATED_EVENT`

扫描 N 首时，会写大小约 1 + 2 + ... + N 的数组，并触发 N 次页面刷新事件；接近 **O(N²)**。大曲库扫描期间非常容易表现为 UI 卡死。

### 建议

- `upsertSong` 增加 `persist: false` / 提供纯内存 upsert helper。
- scanner 循环只修改内存数组；扫描+对账完成后 **一次 `saveSongs`**。
- 进度 UI 可保留，但按 50–100ms 节流更新，避免每首 reactive render。
- 单测断言扫描多首仅一次（或至多常数次）`muses:songs` 写入 / updated event。

### 预期收益

最高；直接消除大规模同步 stringify/setItem 风暴。

---

## F2 · P1：队列解析反复 parse + Q×N find

### 证据

`queue.ts/resolveSongsFromQueue`：

- 每次调用 `loadSongs()` → localStorage get + JSON.parse + 校验/映射整个曲库；
- 每个 queue item 用 `songs.find` → Q×N；
- `refreshQueueState`、select、peek、next/prev、toggle 等多次调用。

全库入队时（随机播放全部），N≈Q，单次解析即接近 O(N²)，切歌/队列操作可卡。

### 建议

- 单次 load 后构造 `Map<songId, SongItem>`，queue items O(Q) 解析。
- 引入曲库内存快照/版本缓存：`loadSongs` 不必每次 JSON.parse；`saveSongs` 更新缓存，storage event/测试 reset 可失效。
- 最小修复先做 Map；缓存作为独立批次，避免一致性风险。

---

## F3 · P1：隐藏 PlayerPage 仍可能持续渲染 AMLL

### 证据

`App.vue` 有当前曲时永久挂载 PlayerPage；关闭态仅 `transform: translateY(100%)`，且明确 `visibility: visible`。

PlayerPage 内 BackgroundRender（mesh gradient）和 LyricPlayer 都保留；播放 position 仍每 500ms 响应式更新。第三方渲染器内部实现可能自带动画循环/GPU 合成，即便移到视口外也未必暂停。

### 风险/权衡

此前保活是为避免重新打开背景白闪（#22）。直接 `v-if` 卸载会回归闪烁，因此不能无条件删除。

### 建议

方案优先级：

1. 关闭态设置 `visibility:hidden` + `content-visibility:hidden`，先让浏览器跳过绘制/layout；验证重新打开是否闪。
2. 仅在 overlay 可见时向 LyricPlayer 推进 currentTime；隐藏时冻结输入。
3. 若第三方仍 RAF：可见时挂载重组件，关闭时保留上次背景的静态 `<img>`/CSS 背景作为 reopening 占位；待 BackgroundRender ready 再切换。
4. 真机用 Perfetto/GPU profiler 对比关闭播放器前后 CPU/GPU。

此项需要真机验证，不能只靠单测宣称解决。

---

## F4 · P1：QueuePage 全量渲染整库

### 证据

QueuePage 对 `queueState.items` 全量 `v-for`，每行还是 `ion-item-sliding`。而「随机播放全部」会把整个曲库放进队列。

Ionic sliding item DOM 成本高；数百/数千首队列打开可能直接卡顿。

### 建议

- QueuePage 虚拟化，固定行高；
- 但 sliding 手势和 virtualizer 组合需谨慎：可先改为普通行 + 右侧删除按钮，或实现虚拟行内 sliding 并测复用状态；
- 「跳当前」/currentIndex 要用 `scrollToIndex`。

---

## F5 · P2：AMLL 在线索引首次解析

### 证据

实测远程 JSONL 约 **1.44 MiB / 3125 行**。首次匹配：response.text → split → 每行 JSON.parse；每首匹配 `findBestMatch` 全索引逐条 normalize/score。

规模不算巨大，但在低端车机/安卓 WebView 首播时会形成明显主线程尖峰。

### 建议

- 解析后构建 normalized title Map / 首字符桶，匹配先缩候选；
- parse 可按 chunk `await` 让出事件循环，或 Web Worker；
- 只在需要在线升级时请求（现状每首都跑，即便已有本地歌词也会尝试升级，属于产品决定）。

---

## F6 · P2：其它全量列表

- QueuePage：已列 P1。
- PlaylistDetailPage：全量歌曲 + 封面；大歌单会卡。
- AlbumsPage / ArtistsPage：全量 ion-item；数量通常小于歌曲，但大库仍可能数百/数千。
- PlaylistsPage：通常数量较少，优先级低。

建议抽可复用虚拟音乐行组件，避免每页各写一套。

---

## F7 · P2：缓存增长与监听器

- `cover/metadata` negative Map、AMLL `ttmlBySongId/negativeBySongId` 无 max size；长时间切过大量歌曲会增长。
- App backButton listener 未保存 handle/remove；App 根通常只挂一次，当前不是主要卡顿，但 HMR/测试/未来重挂可能重复。
- PlayerPage/Tab resize listener均有 remove，计时器也有清理，未发现明显泄漏。

建议为 Map 加 LRU/最大 200–500 项；保存 App listener handle 并卸载 remove。

---

## 推荐实施批次

### 批次 A（强烈推荐，低风险高收益）

1. 扫描批量一次写库 + progress 节流（F1）。
2. 队列解析使用 Map，消除 Q×N（F2 最小修复）。

### 批次 B（高收益，需 UI 验证）

3. QueuePage 虚拟化（F4）。
4. 隐藏 AMLL 降载（F3，真机确认不闪）。

### 批次 C（优化尖峰/长期内存）

5. AMLL 索引候选 Map / chunk parse（F5）。
6. 其它大列表虚拟化（F6）。
7. 缓存容量上限 + App listener remove（F7）。

## 推荐任务拆分

- 子任务 1：`scan-batch-persist`（可独立验收）
- 子任务 2：`queue-resolve-map`（可独立验收）
- 子任务 3：`queue-virtual-list`（可独立验收）
- 子任务 4：`hidden-player-render-budget`（需真机验收）
- 后续任务：AMLL 索引 / 其它列表 / cache lifecycle

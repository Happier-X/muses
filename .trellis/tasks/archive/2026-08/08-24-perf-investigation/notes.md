# 性能排查取证记录

## 环境
MuMu 模拟器 1080x1920，debug 构建，CDP 测量。

## 数据

| 场景 | 进程 CPU | JS 堆 | 备注 |
|---|---|---|---|
| 冷启动静置（无当前曲） | ~0% | 10MB 稳定 | 正常基线 |
| 播放带封面歌曲，停在歌曲页（播放页已关闭） | **40-57% 持续** | RSS 291MB | 根因场景 |

## 根因

App.vue 常驻挂载 `<PlayerPage />`（k-popup 关闭=仍在 DOM）。只要有「当前曲+可展示封面」，`BackgroundRender`（MeshGradientRenderer，PIXI WebGL）以默认 30fps 持续渲染网格渐变动画——与播放页是否可见无关。这是「打开软件后手机发烫卡顿」的唯一根因。

## 关键发现

1. AMLL vue 包 `BackgroundRender` 的 `playing` prop 逻辑**写反**：
   ```js
   watchEffect(() => {
       if (props.playing) bgRenderRef.value?.pause();   // ← 反了
       else bgRenderRef.value?.resume();
   });
   ```
   （对照同文件 LyricPlayer :324 是正确的 playing→resume）。不能依赖该 prop，升级上游修复后会反向。
2. `MeshGradientRenderer` 有公开 `pause()/resume()/setFPS()/setStaticMode()` 可用。

## 修复方案

PlayerPage 持有 BackgroundRender 的模板 ref（`BackgroundRenderRef.bgRender`），watch `playerOverlayVisible`：
- 打开 → `renderer.resume()`
- 关闭 → `renderer.pause()`

保持常驻挂载契约不变（不销毁重建，规避 #20/#25）。

## 预期

关闭播放页静置 CPU 从 40-57% 回落至 <5%。

## 补充取证与修正（修复后实测）

**重要修正**：早期测量有误——取证前主会话曾点击迷你播放条打开过播放页（k-popup 不改路由导致误判为已关闭）。实际 `MPopup` 用 `v-if="opened"`，**关闭即卸载整个播放页组件**，不存在「关闭后渲染」场景。

## 最终实测矩阵（修复后构建）

| 场景 | 进程 CPU |
|---|---|
| 冷启动突发 | 96%（<10s 后回落） |
| 播放中、播放页关闭（组件已卸载） | 4-12% |
| 播放页打开 + 有封面（可见） | 57-71%（MuMu 软件渲染偏重；真机 GPU 会低） |
| 播放页打开 + 无封面（静态兜底） | ~0-7% |
| **播放页打开+有封面，App 切后台（HOME）** | **先过渡后降至 4%** ✓（visibilitychange 暂停生效） |
| 切回前台 | 恢复至 71% ✓（resume 正常） |

## 真实发烫场景定位

「一打开软件就发烫」的实际链路：用户打开 App→恢复会话/播歌→打开播放页查看→切后台或熄屏听歌——修复前 PIXI 在后台持续满速渲染；修复后切后台自动暂停。另外播放页可见时的渲染成本本身较高（MuMu 上 40-70%），后续可评估降帧/降 renderScale（未列入本任务）。

## 修复实施（08-24，仅改 src/views/PlayerPage.vue）

- `BackgroundRender` 加模板 ref `backgroundRenderRef`（类型 `BackgroundRenderRef`）。
- 新增统一同步函数 `syncBackgroundRenderLoop()`：仅「播放页打开 且 document 未隐藏」时 `renderer.resume()`，否则 `renderer.pause()`；pause/resume 包 try/catch（渲染器可能未初始化）。注意模板 ref 经组件实例代理后 Ref 已解包，直接取 `.bgRender` 即渲染器实例。
- 触发点：
  - `watch(playerOverlayVisible)`（复用 #25 的 watch）：打开 resume / 关闭 pause
  - `watch(showAlbumBackground, { flush: 'post' })`：封面变化触发 `:key` 重建后，新渲染器实例若处于不可见状态立即 pause
  - `document.visibilitychange` → `onBackgroundVisibilityChange`：切后台/锁屏 pause，回前台且播放页打开时 resume；onMounted/onUnmounted 成对注册移除
  - onMounted 初始同步一次（覆盖冷启动恢复会话：已有当前曲+封面但弹层关闭）
- 不改常驻挂载结构、不动 `v-if="showAlbumBackground"`（#20/#25 契约保持）；未使用逻辑写反的 `playing` prop。

### 验证

- `npm run lint; echo $?` → 0
- `npm run test:unit; echo $?` → 0（12 文件 143 用例全过）
- `npm run build; echo $?` → 0（vue-tsc + vite build）

### 待真机复测

关闭播放页静置进程 CPU 应从 40-57% 回落至 <5%（MuMu + CDP），播放页视觉/歌词滚动无回归。

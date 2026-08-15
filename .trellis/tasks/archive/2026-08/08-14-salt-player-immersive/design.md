# Design: 沉浸式播放页 1:1 复刻椒盐

## 1. 信息面板布局重构（D1 + D3 + D4）

### 当前结构（info-panel）
```
<info-panel-inner flex column center gap12 width min(100%,420px)>
  <cover-slot>        居中正方形大封面（aspect-ratio:1）
  <song-info>         歌名 h1 20px + 副标题
  <progress-area>     进度条 + 时间
  <controls>          上/播/下（side-btn 48 / play-btn 64）
  <mode-bar>          repeat/shuffle/list/more（mode-btn 40）
</info-panel-inner>
```

### 目标结构（椒盐：从上到下）
```
<player-page__topbar>          ← 新增：返回 ← + 右侧 ⋮（触发现有 actions）
  <m-icon-button @click="onClosePlayer"><ChevronLeft/></m-icon-button>
  <div class="player-page__topbar-title">正在播放</div>
  <m-icon-button @click="openPlayerActions"><MoreVertical/></m-icon-button>

<player-page__song-head>       ← 歌名/歌手移到顶部
  <p class="player-page__artist">{{ lyricArtist }}</p>  小字 14px 白 0.7
  <h1 class="player-page__title">{{ title }}</h1>       大字 44px 暖白

<player-page__cover-rotator>   ← 旋转方形封面
  <img class="player-page__cover-img" />  rotate(-45deg)

<player-page__progress-area>   ← 进度条（不变）
<controls>                     ← 上/播/下（播放按钮加大到 72）
<mode-bar>                     ← 保留 4 按钮（功能不变）
```

### 布局要点
- `song-head`：居中，歌名 `font-size: 44px; font-weight: 600; color: rgba(255,255,255,0.95)`，歌手 14px `rgba(255,255,255,0.7)`
- `cover-rotator`：`flex: 1 1 auto` 弹性区，封面垂直居中；`min-height: 0` 防溢出
- 进度/控制/模式栏 `flex: none`，底部固定
- 保持现有面板滑动容器（`panels` + `activePanel`）与歌词面板不变

## 2. 封面旋转方形（D2）

### 实现
```scss
&__cover-rotator {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 0;
  flex: 1 1 auto;
  overflow: visible;
}
&__cover-img {
  width: min(58vw, 260px);
  aspect-ratio: 1;
  object-fit: cover;
  transform: rotate(-45deg);          /* 椒盐：逆时针 45° 倾斜（左上尖/右下尖） */
  border-radius: 18px;
  box-shadow: 0 12px 40px rgba(0,0,0,0.35);
}
```
- 旋转角度初值 -45°，CDP 对比截图微调（30°~45° 区间）
- 圆角 18px（旋转后视觉圆角更大，对齐椒盐）

## 3. 顶部导航（D4）

- 返回按钮：`ChevronLeft` lucide 图标（椒盐 ←），触发 `closePlayerOverlay()`
- 中间标题"正在播放"（或留空——椒盐截图无文字，用占位保持居中）
- 右侧：现有 `ellipsisVertical` 更多按钮 → `openPlayerActions()`
- 按钮用 **MIconButton**（半透明圆形涟漪，与全局一致）
- 布局：flex 三栏，`justify-content: space-between`，上下 padding 12px

## 4. 控制行（D5）

- 播放按钮 64px → **72px**（椒盐大圆），图标 40px 保持
- 侧边按钮 48px 保持，图标 28px 保持
- 模式栏 4 按钮保持（repeat/shuffle/list/more），视觉不变
- 颜色：播放按钮主色（现状）、侧边白 0.9（现状）

## 5. 背景（D6，无改动）

- AMLL `BackgroundRender` + `MeshGradientRenderer` 保持
- `showAlbumBackground` 逻辑保持

## 6. 关闭/手势

- 顶部返回按钮 → `closePlayerOverlay()`（现有 overlay 逻辑）
- 下滑手势关闭保持（drag layer 不变）

## 风险与回滚

- 布局重构可能影响歌词面板滑动（panels 容器不变，风险低）
- 旋转封面可能溢出容器 → `overflow: visible` + 微调尺寸
- 歌名大字可能换行 → `white-space: nowrap; overflow: hidden; text-overflow: ellipsis`
- 每步独立提交可回滚

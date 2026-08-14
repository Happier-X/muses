# Design: 歌曲页 1:1 复刻椒盐

## 1. 行内结构改造（D1 + D5）

### 当前结构
```
<m-list-item>
  <template #after>
    <div class="songs-page__row-actions">
      <button class="songs-page__round-btn">  ← 移除
        <component :is="volume2/add" class="songs-page__round-icon" />
      </button>
      <button class="songs-page__more-btn">
        <span class="songs-page__more-dots"><i></i><i></i><i></i></span>
      </button>
    </div>
  </template>
</m-list-item>
```

### 目标结构（椒盐）
```
<m-list-item>
  <template #after>
    <button class="songs-page__more-btn">
      <span class="songs-page__more-dots"><i></i><i></i><i></i></span>
    </button>
  </template>
</m-list-item>
```

### 样式变更
- 移除 `.songs-page__round-btn` 和 `.songs-page__round-icon` 样式
- `.songs-page__row-actions` 简化（只剩 more-btn）
- `.songs-page__more-btn` 调整：从 36x48 改为合适尺寸，x 贴右缘

## 2. HQ 标签（D2）

### 位置
在 subtitle 区域的**最前面**，紧贴标题下方。

### 样式
```scss
&__hq-badge {
  display: inline-block;
  font-size: 9px;
  font-weight: 600;
  line-height: 1;
  padding: 1px 3px;
  border-radius: 2px;
  background: #F5A623;  // 橙色（椒盐实测）
  color: #fff;
  margin-right: 4px;
  vertical-align: middle;
  flex-shrink: 0;
}
```

### 模板
在 subtitle 文本前插入：
```html
<span class="songs-page__hq-badge">HQ</span>
```

注意：所有歌曲都显示 HQ（椒盐截图中每行都有），后续可改为按音质条件显示。

## 3. 播放行高亮（D3）

### 当前
```scss
.songs-page :deep(.songs-page__row.is-selected) {
  background-color: rgba(var(--m-primary-rgb), 0.08);
}
```
播放行通过 `.is-playing` class 高亮。

### 目标
- 移除播放行背景高亮
- 标题+副文字改为蓝色（#0470E6）
- 在 MListItem 模板中，通过 class 控制：
```scss
&__row.is-playing {
  .m-list-item__title,
  .m-list-item__subtitle {
    color: var(--m-primary) !important;
  }
}
```

## 4. 封面尺寸（D4）

### 当前
```scss
.songs-page__cover {
  --m-cover-size: 50px;
}
```

### 目标
```scss
.songs-page__cover {
  --m-cover-size: 54px;
}
```

## 5. 工具条（D6）

### 当前
- 左侧：全选/多选按钮
- 右侧：排序 + 多选

### 目标（椒盐）
- 左侧：随机播放图标 + 歌曲总数 "465"
- 右侧：排序图标（A↓）+ 多选图标（≡带勾）

### 实现
工具条已有结构，需要：
1. 左侧改为 shuffle 图标 + songs.length 计数
2. 右侧保持排序+多选（图标对齐椒盐）
3. 调整左侧按钮样式（去掉圆形底，改为纯图标+文字）

## 6. ⋮ 位置（D5）

移除圆按钮后，⋮ 自动右移。验证 x≈344dp（360-16=344）贴右缘。

## 7. 索引条（D7）

验证当前索引条位置与椒盐一致（紧贴右边缘）。

## 风险与回滚

- 移除圆按钮后，"加入队列"功能需要替代入口（可通过 ⋮ 菜单中的"下一首播放"或"添加到歌单"实现）
- 工具条结构变化影响多选模式的入口
- 每个改动可独立提交回滚

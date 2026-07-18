# 技术设计

## 方案

将进度轨道的三层视觉从浏览器 `input[type=range]` 伪元素背景中拆出，使用 `.progress-track` 下的绝对定位自绘层：未缓冲底轨、已缓冲层、已播放层。range 只负责原生键盘/拖动和圆点，背景透明。

## 交互状态

新增页面局部 `seekPreviewPosition: number | null`：

- 非交互时使用 `playerState.position`。
- `input` 拖动时立即写 preview，使圆点和已播放填充层同步。
- `change` 提交时调用 `seekPlayback`；成功后清除 preview，失败则恢复播放器位置并显示缓冲提示。
- 点击轨道时通过 `clientX` 与 `getBoundingClientRect()` 计算 0..1 比例，转换为总时长秒数，经过 `clampSeekTarget` 后提交。

## 手势与边界

- 所有轨道 pointer/touch 操作沿用 `lockSeekGesture`，阻止 overlay 左右切换及上一曲/下一曲点穿。
- 缓冲已知时 preview 和最终 seek 均不得超过 `bufferedPosition`；未知时上限为 duration。
- 点击/拖动共用一个 `commitSeekTarget`，避免两套行为分叉。
- 保留歌词点击 `seekPlayback`、自然结束保护窗和播放器状态机。

## 测试

组件测试覆盖：拖动 input 后 `--progress`/已播放层宽度即时变化；change 提交 seek；轨道点击按坐标 seek；缓冲边界 clamp；交互锁不回归。

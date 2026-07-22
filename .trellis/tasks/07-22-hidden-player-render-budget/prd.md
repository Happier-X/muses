# 隐藏播放器渲染降载

## Goal

沉浸式播放器关闭但保持挂载时，停止不必要的 AMLL 布局、绘制和时间输入，同时保持再次打开不白闪。

## Requirements

- 继续保活 PlayerPage，不能直接恢复 `v-if` 销毁重建。
- 隐藏态跳过页面绘制，并冻结 LyricPlayer 的高频时间输入。
- 背景、歌词和封面在再次打开时立即恢复到最新播放位置。
- 后台音频播放、MediaSession 和进度持久化不受影响。
- 不修改 `node_modules`。

## Acceptance Criteria

- [ ] 隐藏态 PlayerPage 不随每个 position tick 更新 AMLL currentTime
- [ ] 关闭和重开无默认背景白闪、半屏或错误歌词位置
- [ ] 下滑、Android back、队列 overlay 交互不回归
- [ ] 单测覆盖 visible/hidden 输入门控
- [ ] lint、build 通过
- [ ] 真机完成关闭前后 CPU/GPU 观察；若本环境无法执行，明确列为人工验收项

# 隐藏播放器渲染降载实现清单

1. [x] 保持 PlayerPage 保活，不恢复 `v-if` 销毁重建
2. [x] 增加 `lyricRenderTime`：可见时跟随实时 position，隐藏时冻结 AMLL 时间输入
3. [x] 重开时用最新 `playerState.position` 同步歌词时间，避免旧位置
4. [x] 隐藏态 App shell 使用 `visibility: hidden` + `contain: paint` 跳过不可见绘制；显示时恢复
5. [x] 增加 visible/hidden 门控契约测试
6. [x] player 单测、lint、build 通过
7. [ ] 真机观察 CPU/GPU（当前环境无法执行）
8. [ ] 提交并归档任务

## 验证结果

- player 单测：114 passed
- lint：通过
- build：通过
- 真机 CPU/GPU：待人工验收

# 修复 open issues #50 #51

## Goal

关闭卡顿/卡死与播放音量偏小两个体验问题。

## Child Task Map

| 子任务 | 目录 | 交付物 | 建议顺序 |
|--------|------|--------|----------|
| #51 音量偏小 | `07-22-fix-playback-too-quiet` | 播放足够响、可控 | 先（范围更清晰） |
| #50 卡顿卡死 | `07-22-fix-app-jank` | 明显减轻卡顿/卡死 | 后（需诊断） |

父任务不写业务代码；负责跨 issue 验收与收尾。

## Cross-child Acceptance

- [ ] GitHub #50 / #51 关闭
- [ ] lint / unit / build 通过
- [ ] 不回归：#46 响度均衡语义（除非 #51 明确改产品决策）、#47 进度、#49 会话恢复

## Out of Scope（父级）

- 发版
- 改 capgo 插件源码

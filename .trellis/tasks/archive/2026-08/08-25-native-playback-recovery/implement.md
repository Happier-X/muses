# 执行计划：播放失败自动恢复

> 只动 core/media。每批一 commit。

## R0 恢复控制器
- [ ] `PlaybackErrorCopy.kt`：errorCode→文案映射 + 兜底
- [ ] `PlaybackRecoveryController.kt`：attempted 集合、候选选择（回绕一次）、reset()
- [ ] 单测：候选选择三分支 + 文案映射
- 验证：`:core:media:testDebugUnitTest`

## R1 服务接线
- [ ] PlaybackService：注入 controller；player.addListener(onPlayerError → recover)；MEDIA_ITEM_TRANSITION(用户切歌) 清 attempted/error
- [ ] PlayerConnection：监听 Controller onPlayerError → playbackError StateFlow；play()/setRepeatMode 等主动操作置 null
- 验证：`:app:assembleDebug`

## 收尾
- [ ] 全量门禁；隔离自检；spec「播放契约」补恢复链条目；分批提交

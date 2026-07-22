# 运行时缓存与监听器治理

## Goal

限制长会话中的在线匹配缓存增长，并补齐应用级 listener 卸载，避免累计内存和重复回调。

## Requirements

- AMLL TTML、歌词/封面/文本负缓存设置统一、可测试的容量上限。
- 淘汰策略至少保证近期条目优先保留；TTL 语义保持。
- 不影响当前并发请求去重。
- App 根级 Capacitor listener 保存 handle 并在卸载时 remove。
- 已正确清理的页面 listener/timer 不做无收益重写。

## Acceptance Criteria

- [ ] 任一受控 Map 超过上限后自动淘汰
- [ ] 命中、负缓存 TTL、reset 测试不回归
- [ ] App listener 卸载测试通过
- [ ] lint、unit、build 通过

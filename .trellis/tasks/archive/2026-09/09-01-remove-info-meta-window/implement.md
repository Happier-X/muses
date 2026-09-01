# 实施计划 — 移除歌曲信息页五行歌词

## 前置检查

- [ ] `prd/design` 已评审，明确仅移除信息页预览

## 步骤

### 1. 前端移除

- [ ] 1.1 `full-player.js` 删除 `renderMetaWindow/findCurrentIndex`、`initDom` 中 `meta-window` 创建、`updateProgress` 与 `wrapLyrics` 中 `renderMetaWindow` 调用
- [ ] 1.2 `full-player.css` 删除 `.meta-window` 相关样式段

### 2. Compose 移除

- [ ] 2.1 `PlayerScreen.kt` `InfoPanel` 移除 `MetaWindow` 调用与关联 `Spacer`，删除 `private fun MetaWindow` 整段及仅服务该组件的 `computeCurrentIndex`（若未复用）
- [ ] 2.2 确认 `parsedLines` 收集保留或改为未使用（保留以不扩散改动）

### 3. 验证

- [ ] 3.1 `assembleMusesDebug`
- [ ] 3.2 `lintMusesDebug`
- [ ] 3.3 手工：沉浸式信息页无预览、完整歌词正常、布局无塌陷

## 验证命令

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug :app:lintMusesDebug
```

## 产出

- `full-player.{js,css}`、`PlayerScreen.kt`、`spec §7`


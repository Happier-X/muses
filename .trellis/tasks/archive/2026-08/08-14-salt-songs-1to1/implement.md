# Implement: 歌曲页 1:1 复刻椒盐

## 执行顺序（每步独立验证）

### 阶段 1：移除行内圆按钮（D1）
- [ ] 1.1 SongsPage.vue 模板中移除 round-btn（`<button class="songs-page__round-btn">` 及其内容）
- [ ] 1.2 移除 round-btn 相关样式（`.songs-page__round-btn`、`.songs-page__round-icon`、`.is-playing::before` 等）
- [ ] 1.3 移除 `onRowActionClick` 函数（入队逻辑）及相关导入（volume2、add 图标）
- [ ] 1.4 验证：`npm run lint` + 行内只剩 ⋮
- 提交：refactor(songs): 移除行内圆按钮，只保留 ⋮ 对齐椒盐

### 阶段 2：添加 HQ 标签（D2）
- [ ] 2.1 SongsPage.vue 模板中 subtitle 前添加 `<span class="songs-page__hq-badge">HQ</span>`
- [ ] 2.2 添加 HQ 标签样式（橙色背景 + 白色文字 + 圆角）
- [ ] 2.3 验证：每行副标题前显示 HQ 标签
- 提交：feat(songs): 副标题前添加 HQ 品质标签对齐椒盐

### 阶段 3：播放行高亮改蓝色（D3）
- [ ] 3.1 移除播放行背景高亮（`.is-playing` 背景色）
- [ ] 3.2 播放行标题+副文字改为蓝色（`var(--m-primary)`）
- [ ] 3.3 验证：播放中歌曲文字变蓝，无背景色
- 提交：feat(songs): 播放行文字蓝色高亮对齐椒盐

### 阶段 4：封面尺寸调整（D4）
- [ ] 4.1 封面从 50px 调整为 54px
- [ ] 4.2 验证：封面略大，与椒盐一致
- 提交：fix(songs): 封面尺寸调整为 54dp 对齐椒盐

### 阶段 5：工具条对齐椒盐（D6）
- [ ] 5.1 左侧改为 shuffle 图标 + 歌曲总数
- [ ] 5.2 右侧图标对齐椒盐（排序 A↓ + 多选 ≡）
- [ ] 5.3 调整工具条布局间距
- [ ] 5.4 验证：工具条视觉对齐椒盐截图
- 提交：feat(songs): 工具条布局对齐椒盐

### 阶段 6：全量验收（对照 AC1-AC10）
- [ ] 6.1 `npm run build` / `npm run lint` 全绿
- [ ] 6.2 CDP 实测行内布局（封面 x、⋮ x、行高）
- [ ] 6.3 截图与椒盐对比（整体视觉）
- [ ] 6.4 交互回归：⋮ 菜单、排序、多选、搜索
- [ ] 6.5 其他页面无回归
- 提交：chore(songs): 椒盐复刻收尾

## 验证命令

```bash
npm run build
npm run lint
npx cap sync android
adb -s emulator-5556 install -r android/app/build/outputs/apk/debug/app-debug.apk
node .tmp/cdp_probe.cjs  # CDP 验证布局坐标
```

## 风险与回滚

- 移除圆按钮后"加入队列"功能消失 → 可通过 ⋮ 菜单"下一首播放"替代，或后续加回
- HQ 标签可能与某些歌曲不匹配（无 HQ 音源）→ 可加条件渲染
- 工具条结构变化影响多选入口 → 确保多选按钮仍可达
- 每阶段独立提交可回滚

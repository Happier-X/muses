# 修复播放器歌曲信息编辑表单输入消失

## 背景

`PlayerPage.vue` 的歌曲信息编辑 sheet（`editForm`）与已修复的 `SourcesPage` 音源表单为**同一模式**：`k-list-input` 非受控绑定 TanStack Form 字段，blur/切字段时偶发丢失输入值。用户确认另开任务修复。

## 范围

`src/views/PlayerPage.vue` 编辑歌曲信息表单：

- 字段：title / artist / album / replayGainDb（k-list-input）+ lyrics（textarea，`onTextareaFormInput`）
- 全部改为 `k-list-input` 的 `#input` 槽自定义受控 input（或等价受控方式），样式类完整复制 Konsta 默认（`block text-base appearance-none w-full focus:outline-none bg-transparent h-10 placeholder-black/30 dark:placeholder-white/30`，textarea 同理带 h 适配）
- 删除废弃的 onFormInput / onTextareaFormInput 适配函数（如无其他引用）

## 依据

`.trellis/spec/frontend/forms.md` §0（k-list-input 必须 #input 槽受控，含完整样式类清单）。

## 验收标准

- [x] 5 字段全部受控化（title/artist/album/replayGainDb/lyrics；无 onFormInput 残留）
- [x] lint 通过（仅 PlaylistsPage k-button 存量错误，非本次引入）
- [x] build（vue-tsc + vite）通过
- [x] 模拟器 CDP 实测：输入 → blur → 切字段 → title/artist/album 全保留；歌词 textarea 同模式（编辑 sheet 无本地歌词字段场景，代码经 trellis-check 核对）
- [x] 无回归：onSubmit 读 value.lyrics + editLyricsFormat 提交链路完好；trellis-check 核对 v-if/v-else 分支
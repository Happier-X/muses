# 封面补全后歌词消失与背景不更新

## Goal

Issue #21：在线封面到位后，(1) 沉浸式背景应跟上新封面；(2) 不应把已展示的在线歌词清掉。

## Root Cause

1. `syncDisplayStateFromSong` 仅保护 ttml/yrc/qrc；在线 LRC 会被懒扫描/封面写回的「无词 song」覆盖成空。
2. 封面晚到时 AMLL `BackgroundRender` 可能不响应 album prop 变更（需 key 强制重建）；粘性封面逻辑在新 cover 有值时应更新。

## Requirements

1. **R1** sync 时：库内无词不得清空运行时已有词；仅当库内词质量严格更优才替换。
2. **R2** 封面更新后背景与封面槽使用新封面（key 重建 BackgroundRender）。
3. **R3** 单测：封面写回不抹 LRC；背景 album 随新封面更新。

## Task Type

Lightweight

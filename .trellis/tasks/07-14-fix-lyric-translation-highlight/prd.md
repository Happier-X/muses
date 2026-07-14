# 修复歌词翻译开关与高亮

## Goal

修复 Issue #27：英文歌总是展示翻译，但翻译行不高亮，翻译开关无效。

## Root Cause

1. 平台双语 LRC 常把原文/译文拆成两行主歌词（相同时间戳或紧邻），`parseLrc` 不会写入 `translatedLyric`。
2. 网易云 `tlyric` 已请求但未合并进主歌词行。
3. 翻译开关只清空 `translatedLyric`/`romanLyric`，对「伪翻译主行」无效。
4. AMLL 副行（`.FmKaba_lyricSubLine`）默认固定低透明度，激活行时也未抬高，观感像「不高亮」。

## Requirements

1. 解析后把同时间戳双语主行合并：第一行主词，第二行写入 `translatedLyric`。
2. 网易云返回时合并 `tlyric` 到行级 `translatedLyric`（yrc/lrc 均可）。
3. 翻译开关继续隐藏 `translatedLyric`/`romanLyric`，并对合并后的数据生效。
4. 激活主行时提升翻译副行可见度。
5. 单测覆盖合并与显示开关。

## Task Type

Lightweight

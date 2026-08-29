# 第三方源码声明

本项目包含以源码形式 vendored（内嵌）的第三方代码，均遵循其原始许可证（Apache License 2.0）。所有 vendored 文件头部均保留了来源、许可与本地改动说明。

---

## Accompanist lyrics-ui（AMLL 官方 Compose 渲染器）

| 项目 | 内容 |
| --- | --- |
| 名称 | Accompanist — lyrics-ui |
| 版本 | 1.0.19（快照自 `main` 分支） |
| 仓库 | https://github.com/6xingyv/accompanist-lyrics-ui |
| 作者 | The Accompanist / MochaRealm Authors（Simon Scholz） |
| 许可证 | Apache License 2.0 |

**用途**：沉浸式播放页的卡拉OK 歌词渲染（AMLL = Apple-Music-Like-Lyrics 的 Kotlin/Compose 官方实现），提供逐词渐变填充、长词字符级动画、非当前行距离模糊、间奏呼吸点、自动滚动与和声行样式。

**内嵌位置**：

```
feature/player/src/main/kotlin/com/mocharealm/accompanist/lyrics/ui/
├── composable/lyrics/
│   ├── KaraokeLyricsView.kt        # 滚动容器 / 焦点行 / 自动滚动 / 间奏检测
│   ├── KaraokeLineText.kt          # 逐词 Canvas 渲染与字符动画
│   ├── LyricsLayoutCalculator.kt   # 音节测量、换行布局（DP 均衡）
│   ├── LyricsLineItem.kt           # 单行效果容器（缩放/模糊/点击）
│   ├── SyncedLineText.kt           # LRC 整行渲染
│   └── KaraokeBreathingDots.kt     # 间奏呼吸点
└── utils/
    ├── String.kt                   # 文字属性判断（CJK/阿拉伯/天城文/RTL/标点）
    ├── Brush.kt / Color.kt
    ├── easing/{CubicBezierEasing, NewtonPolynomialInterpolationEasing}.kt
    └── modifier/SpringPlacementModifier.kt
```

**保留原包名 `com.mocharealm.accompanist.*` 的原因**：便于用 `diff` 与上游对比、后续同步升级；同时明确其第三方归属。

**相对上游的本地改动**（仅两项，均在文件头注释中标注）：

1. **`expect`/`actual` 折叠**：上游是 Kotlin Multiplatform 结构，`Char.isCjk()` / `isArabic()` / `isDevanagari()` 以 `expect` 声明、在 `androidMain` 提供 `actual` 实现。本项目是单目标 Android 模块，已将两者合并进 `utils/String.kt`，改为普通 Kotlin 函数。
2. **移除 gaze-capsule 依赖**：`LyricsLineItem.kt` 原用 `com.mocharealm.gaze.capsule.ContinuousRoundedRectangle(8.dp)` 做行裁剪，现替换为 `androidx.compose.foundation.shape.RoundedCornerShape(8.dp)`，从而不必引入 `com.mocharealm.gaze:capsule` 库。
3. **去掉 `ExperimentalAnimatableApi` opt-in**：`utils/modifier/SpringPlacementModifier.kt` 原带 `@OptIn(ExperimentalAnimatableApi::class)`（JetBrains Compose 的 KMP 注解）。AndroidX Compose 1.12 起 `DeferredTargetAnimation` 已转为稳定 API、该注解被移除，故删除 import 与注解。

**许可证声明**（Apache 2.0 摘要）：

> Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
>
> Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

完整许可文本见 https://www.apache.org/licenses/LICENSE-2.0

---

## 其他依赖（非源码内嵌，通过 Gradle 引入）

- `com.mocharealm.accompanist:lyrics-core` — Apache 2.0，歌词解析（TTML/LRC/YRC/KRC/Lyricify Syllable）
- 其余依赖见 `gradle/libs.versions.toml` 与各模块 `build.gradle.kts`

# 第三方源码声明

本项目当前没有以源码形式 vendored（内嵌）的第三方代码。

---

## Accompanist lyrics-ui（历史记录，U21 已下线）

沉浸式播放页曾以内嵌源码方式引入 AMLL 官方 Compose 渲染器（`com.mocharealm.accompanist:lyrics-ui`，Apache 2.0），
U21 已改写为自研渲染（`feature/player/.../lyric/LyricsPanel.kt` 起，另见文件头移植说明），内嵌源码全部删除。
U24 起连 `lyrics-core` 外部依赖也已摘除（唯一消费者随 U21 删除）。
当前源码中无任何 `com.mocharealm.*` 引用，无需保留第三方许可声明；此处仅保留历史记录供追溯。

---

## VLC 运行时（Windows 桌面端，二进制随包内置）

Windows 桌面安装包（MSI / EXE）内置一份裁剪后的 VLC 原生库（约 40MB），安装后位于
`app/resources/vlc`，由 VLCJ 经 JNA 加载 `libvlc.dll` 完成音频解码与输出
（发现顺序与裁剪白名单见 `desktop/.../JvmPlayerPort.resolveVlcDir()`、`scripts/vlc-trim-keep.txt`）。

- **来源**：官方 `vlc-3.0.21-win64.zip`（<https://get.videolan.org/vlc/3.0.21/win64/>），
  SHA256 `a0b7ec02b50adf6417eed014fb8df50af39690505a4225b85b3dc2ed17d14843`；
  发版时由 `.github/workflows/release.yml` 下载并校验，再经 `scripts/prepare-vlc-runtime.ps1`
  解压、按白名单裁剪为纯音频最小集（182MB → 约 40MB）。仅保留播放所需插件，未修改任何二进制。
- **许可**：VLC 为 GPLv2+ / LGPLv2.1+ 双许可项目。随包分发的副本保留了其
  `COPYING.txt` / `AUTHORS.txt` / `THANKS.txt` / `README.txt` / `NEWS.txt`（安装目录 `app/resources/vlc` 下）。
- **对应源码**：<https://code.videolan.org/videolan/vlc>（tag `3.0.21`）。
- **Java 绑定**：`uk.co.caprica:vlcj`（4.8.2，GPLv3）+ `vlcj-natives`，仅 `:desktop` 模块依赖，
  不进 `commonMain`，不随安卓包分发

---

## 其他依赖（非源码内嵌，通过 Gradle 引入）

- 其余依赖见 `gradle/libs.versions.toml` 与各模块 `build.gradle.kts`

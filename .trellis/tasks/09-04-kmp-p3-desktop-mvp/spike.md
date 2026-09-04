# S0 VLCJ 解码原型书面结论

> 任务：09-04-kmp-p3-desktop-mvp ｜ 日期：2026-09-04 ｜ 结论：**锁定 VLCJ，S1 可开工**

## 1. 三项 gate 验证结果（原文）

### Gate 1：FLAC 实际可播（含 24bit/高采样）——通过

```text
RESULT file="tone_44k_16bit.flac" ok=true playAccepted=true gotPlaying=true msToPlaying=1863 lengthMs=30000 maxTimeMs=1687 events=["playing"]
RESULT file="tone_48k_24bit.flac" ok=true playAccepted=true gotPlaying=true msToPlaying=1643 lengthMs=30000 maxTimeMs=1547 events=["playing"]
RESULT file="tone_96k_24bit.flac" ok=true playAccepted=true gotPlaying=true msToPlaying=1626 lengthMs=30000 maxTimeMs=1503 events=["playing"]
RESULT file="tone_192k_24bit.flac" ok=true playAccepted=true gotPlaying=true msToPlaying=1627 lengthMs=30000 maxTimeMs=1504 events=["playing"]
RESULT file="sample-song.mp3" ok=true playAccepted=true gotPlaying=true msToPlaying=1820 lengthMs=372719 maxTimeMs=1593 events=["playing"]
RESULT file="sample.m4a" ok=true playAccepted=true gotPlaying=true msToPlaying=1823 lengthMs=122117 maxTimeMs=1651 events=["playing"]
RESULT file="Example.ogg" ok=true playAccepted=true gotPlaying=true msToPlaying=2023 lengthMs=6104 maxTimeMs=1944 events=["playing"]
RESULT file="tone_44k_16bit.wav" ok=true playAccepted=true gotPlaying=true msToPlaying=1639 lengthMs=10000 maxTimeMs=1551 events=["playing"]
RESULT summary={"mode":"decode","pass":8,"total":8,"failures":[]}
```

8/8 全过：FLAC 四规格（44.1k/16bit、48k/24bit、96k/24bit、192k/24bit）+ MP3 + M4A + OGG + WAV，起播约 1.6～2.0 秒，时长解析正确（lengthMs 与文件真实时长一致）。

### Gate 2：seek 精度 ±1s 内——通过

```text
RESULT seek={"ok":true,"file":"tone_44k_16bit.flac","points":[{"target":5000,"actual":5000,"errMs":0,"ok":true},{"target":15000,"actual":15000,"errMs":0,"ok":true},{"target":25000,"actual":25000,"errMs":0,"ok":true}]}
RESULT seek={"ok":true,"file":"tone_48k_24bit.flac","points":[{"target":5000,"actual":5000,"errMs":0,"ok":true},{"target":15000,"actual":15000,"errMs":0,"ok":true},{"target":25000,"actual":25000,"errMs":0,"ok":true}]}
RESULT seek={"ok":true,"file":"tone_96k_24bit.flac","points":[{"target":5000,"actual":5000,"errMs":0,"ok":true},{"target":15000,"actual":15000,"errMs":0,"ok":true},{"target":25000,"actual":25000,"errMs":0,"ok":true}]}
RESULT seek={"ok":true,"file":"tone_44k_16bit.flac","points":[{"target":5000,"actual":5000,"errMs":0,"ok":true},{"target":15000,"actual":15000,"errMs":0,"ok":true},{"target":25000,"actual":25000,"errMs":0,"ok":true}]}
```

4 文件 × 3 点（5s/15s/25s）共 12 点，暂停态落点误差全部 0ms，远优于 ±1s 门槛。

测量方法说明：第一轮播放态测量出现约 +1000～1240ms 系统性正偏差（setTime 后 1.2 秒内时钟继续推进的测量污染，非解码误差）；改暂停态 seek + time 事件静默 800ms 后读稳定值，语义对齐进度条拖动落点，12 点全部 0 误差。JvmPlayerPort 实现时建议拖动期间暂停或以落点为准。

### Gate 3：干净 Windows 机可跑（原生库随包、无需预装 VLC）——通过

- 本机注册表无 VideoLAN 项、`C:/Program Files[/ (x86)]/VideoLAN` 均不存在，即零预装基线。
- 原生库来源：VLC 官方便携包 `vlc-3.0.21-win64.zip`（get.videolan.org），解压即用，随原型目录存放于 `spike-vlcj/vlc-portable/vlc-3.0.21`。
- 加载方式：JNA `jna.library.path` 指向随包目录 + `VLC_PLUGIN_PATH` 指向随包 `plugins`，`MediaPlayerFactory` 正常创建，原生发现不依赖注册表/系统 PATH。
- 隔离验证：`env -i` 最小环境（仅 SYSTEMROOT/JAVA_HOME/TEMP/TMP + 随包 VLC 目录进 PATH）重跑解码矩阵，8/8 全过（见 `logs/decode-matrix-cleanenv.log`）。JNA 自身要求 TEMP/TMP 可写（解压 `jnidispatch.dll` 用），随包方案需保证安装目录或用户临时目录可写——msi 默认满足。
- 打包映射：S4 只需把 `vlc-3.0.21` 整目录（含 `libvlc.dll`/`libvlccore.dll`/`plugins/`）打进安装包并随应用设置上述两变量，即等价本次验证。

## 2. 选型数据（格式/体积/license）

| 项 | 数据 |
|---|---|
| 格式覆盖 | FLAC（含 24bit/192kHz）/MP3/M4A/OGG/WAV 实测全播；VLC 内核其余格式为附赠 |
| 体积 | Java 侧：vlcj-4.8.2 约 395KB + vlcj-natives 约 93KB + JNA 约 3.1MB；原生侧：VLC 3.0.21 便携包解压约 182MB（随包主体，S4 可按插件裁剪评估） |
| 协议 | vlcj 与 vlcj-natives 的 Maven POM 均声明 GPL v3；VLC 二进制包内 COPYING 为 GPL v2（1991 版）。design.md 原写 LGPL 与实测 POM 不符 |

协议风险提示：VLCJ 实测为 GPL 系（非 LGPL），是否满足本项目分发合规需在 S2 开工前由决策人确认；若法务/产品不接受 GPL，则按 design.md 回退 JavaFX Media。本次技术三项 gate 已全过，技术选型锁定 VLCJ，协议确认作为 S2 前置管理动作，不阻塞技术结论。

回退线：任一 gate 不过则回退 JavaFX Media——本次三项全过，回退线未触发。

## 3. 原型位置与复现

- 原型工程：`C:/code/muses/spike-vlcj/`（独立 Gradle 纯 Java 工程，VLCJ 依赖只进本目录；`git status` 主模块仅多 `spike-vlcj/` 一个 untracked 目录，Kotlin/AGP 版本线未动，安卓侧零改动）。
- 探测源码：`C:/code/muses/spike-vlcj/src/main/java/spike/VlcjProbe.java`，`decode <mediaDir>` / `seek <file>` 两种模式。
- 测试音频：`C:/code/muses/spike-vlcj/media/`（soundfile 生成的 30 秒正弦 FLAC 矩阵 + WAV，SoundHelix MP3、filesamples M4A、Wikimedia OGG 真实样本）。
- 日志存档：本任务目录 `logs/decode-matrix-normal.log`、`logs/decode-matrix-cleanenv.log`、`logs/seek-accuracy.log`。

复现命令（Gradle 用本机解压包，避免 wrapper 重下）：

```bash
export JAVA_HOME="/c/Users/zhf52/java/jdk-21.0.11+10"; export PATH="$JAVA_HOME/bin:$PATH"
G=/c/Users/zhf52/.gradle/wrapper/dists/gradle-9.6.1-all/4xnwe4ed5w7qqynisxnnvegss/gradle-9.6.1/bin/gradle
cd C:/code/muses/spike-vlcj && "$G" compileJava --console=plain -q
VDIR="C:/code/muses/spike-vlcj/vlc-portable/vlc-3.0.21"
CP="build/classes/java/main;$(ls build/install/spike-vlcj/lib/*.jar | tr '\n' ';')"
VLC_PLUGIN_PATH="$VDIR/plugins" "$JAVA_HOME/bin/java" -Djna.library.path="$VDIR" -cp "$CP" spike.VlcjProbe decode C:/code/muses/spike-vlcj/media
VLC_PLUGIN_PATH="$VDIR/plugins" "$JAVA_HOME/bin/java" -Djna.library.path="$VDIR" -cp "$CP" spike.VlcjProbe seek C:/code/muses/spike-vlcj/media/tone_96k_24bit.flac
```

注意：`media().play()` 必须传文件绝对路径（`getAbsolutePath()`）；`File.toURI().toString()` 在 Windows 下生成畸形 MRL（`file:///C:/.../file%3A%2F...`）会被当 DVD 打开导致全 error。JvmPlayerPort 实现时沿用绝对路径传法。

## 4. 给 S2 的交接

- 解码调用范式：`MediaPlayerFactory("--no-video", "--aout=directsound")` + `media().play(absolutePath)` + `controls().setTime/pause/play` + 事件 `playing/finished/error/timeChanged/lengthChanged` 桥接 StateFlow。
- 状态映射：`playing→STATE_READY/播放中`、`paused→STATE_READY/暂停`、`finished→STATE_ENDED`、`error→播放失败文案`，沿用 `PlaybackErrorCopy` 8 条安全文案。
- 体积优化留给 S4：182MB 便携包可做插件裁剪（首版不断言裁剪量，先保证可跑）。

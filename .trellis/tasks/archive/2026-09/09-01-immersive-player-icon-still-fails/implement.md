# 实施计划 — 沉浸式图标点击仍无切换排障

## 前置检查

- [ ] `prd.md` / `design.md` 已评审，现象已定界为“仅 active 变、形状未变”
- [ ] 分支基于 `main` 最新，无未提交脏改

## 步骤

### 1. 资源可视性放大

- [ ] 1.1 `full-player.js` 将 `svgRepeatOne` 第二 path 改为大号“1”文本或更大 path（`text x=12 y=15 font-size=8 weight=800`），保证 20px 下占宽 ~30%
- [ ] 1.2 将 `svgOrder` 圆点 `r=1.5 -> 2.0`（三点 `cx 4.5 cy 6/12/18`），保持线段 `M8 ...`，提升对比

### 2. 可观测性

- [ ] 2.1 `initDom` 首行加 `Android.log('full-player.js v6 iconFix visible','info')`
- [ ] 2.2 `setRepeatIcon` 与 `setShuffleIcon` 内各加切换日志

### 3. 联调与自测

- [ ] 3.1 卸载重装 `app-muses-debug.apk` 到 MuMu，手机/平板各切循环/随机 3 次，形状清晰切换且 `active` 同步
- [ ] 3.2 `adb logcat -s FullPlayer:V` 核验 `v6 / setRepeatIcon / setShuffleIcon / btn click / -> toggle*` 链路
- [ ] 3.3 回归横滑/下滑/进度/歌词 seek 无异常

### 4. 本地验证

- [ ] 4.1 编译：`JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug`
- [ ] 4.2 Lint：`JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:lintMusesDebug`

## 验证命令

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug :app:lintMusesDebug
```

## 回滚点

- 回退 SVG 至 `aad86018` 小 path 版，保留日志

## 产出

- 代码：`app/src/main/assets/amll/full-player.js`
- 产物：`app-muses-debug.apk`

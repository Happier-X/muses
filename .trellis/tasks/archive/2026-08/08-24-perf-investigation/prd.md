# PRD：发烫卡顿性能排查与修复

## 目标

定位「打开软件后手机发烫、页面卡顿」的根因并修复。

## 初步取证（主会话已完成）

- 空闲态正常：JS 堆 10MB 零增长（5s 采样）、进程 CPU ~0%、RSS 255MB → **非常驻型泄露**
- 发烫发生在特定场景；嫌疑排序：
  1. AMLL BackgroundRender/MeshGradientRenderer（PIXI WebGL）——播放页关闭后弹层内容若仍挂载，渲染循环持续吃 GPU/CPU
  2. 冷启动批量元数据扫描（465 首，集中原生 tag 读+网络）
  3. 定时器/轮询在页面关闭后存活
  4. motion 动画链

## 需求

### Phase A 取证（逐场景 CDP Performance + 进程 CPU 对照）

1. 冷启动后静置 60s：CPU/GPU 曲线
2. 歌曲页滚动：帧率与 CPU
3. 打开播放页：CPU 峰值与稳定值；**关键实验：关闭播放页后 CPU 是否回落**（验证 PIXI 循环是否仍跑）
4. 检查存活的 setInterval/setTimeout/EventListener（CDP evaluate 遍历或代码审计结合）

### Phase B 修复（按取证结果，预期方向）

- 若 PIXI 后台渲染坐实：播放页不可见时暂停 MeshRenderer 渲染循环（如 document.hidden / popup opened=false 时 stop），可见时恢复
- 扫描限流：批量 tag 读取加并发上限/分片让出
- 定时器清理：页面卸载时移除
- 以实测数据决定修复范围，不做无证据的优化

## 验收标准

1. 有修复项：修复前后同场景 CPU 对比数据（目标：关闭播放页静置时进程 CPU < 5%，无持续 GPU 占用）
2. 无回归：播放页视觉正常（背景渐变仍工作）、歌词滚动正常、扫描功能不变
3. lint / test:unit / build 全过（禁止管道吞退出码）
4. 排查结论与数据记录进任务 notes/spec

## 范围外

- 不重构虚拟列表/AMLL 集成架构（仅做生命周期治理）
- 不改原生插件

## 约束

- 遵循 component-guidelines（深色覆盖写法、Motion 动画约定等）
- MuMu 模拟器 + CDP 作为主要测量通道

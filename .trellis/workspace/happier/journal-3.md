# Journal - happier (Part 3)

> AI development session journal
> Started: 2026-08-05

---

## Session 94: 编辑云端元信息 API

**Date**: 2026-08-05
**Task**: 云端强制搜与多候选编排 API（父：编辑接入云端元信息）
**Branch**: `main`

### Summary

规划父任务 D1–D6（混合预览、文本+封面+歌词、仅手动获取、分字段勾选、全维多候选、有值默认全勾）；落地 `searchEditCloudMeta`，与播放静默 `matchOnline*` 分离。

### Main Changes

- `src/features/editMeta/`：强制搜 + 三路并行多候选 + AbortSignal
- `features-player.md`：编辑强制搜契约与禁止项
- 父子任务规划文档

### Git Commits

| Hash | Message |
|------|---------|
| `33a45ad` | feat(editMeta): 编辑页云端强制搜与多候选编排 API |
| `c478b3e` | docs(task): 规划编辑接入云端元信息父子任务 |

### Testing

- [OK] lint / build
- [OK] 播放 match* 无 diff

### Status

[OK] **API 子任务完成**；下一步 `08-05-edit-cloud-meta-ui`

---

## Session 94b: 编辑 sheet 云端 UI

**Date**: 2026-08-05
**Task**: 编辑 sheet 云端预览勾选应用 UI
**Branch**: `main`

### Summary

PlayerPage 编辑 sheet 接入「从云端获取」：多维预览/换候选、分字段勾选、应用到表单；封面 cacheRemoteCover；关 sheet/切歌 abort。

### Main Changes

- `PlayerPage.vue` 云端区块 + apply/abort
- `features-player.md` UI 契约补充

### Testing

- [OK] lint / build

### Status

[OK] **UI 子任务完成**；父任务集成收尾

---

## Session 94c: 父任务集成归档

**Date**: 2026-08-05
**Task**: 编辑歌曲信息接入云端元信息（父）
**Branch**: `main`

### Summary

API + UI 两子任务完成；父 PRD AC 全勾；spec 已记编辑云端路径。

### Git Commits

| Hash | Message |
|------|---------|
| `33a45ad` | feat(editMeta): 编辑页云端强制搜与多候选编排 API |
| `c478b3e` | docs(task): 规划编辑接入云端元信息父子任务 |
| `0995644` | chore(task): archive 08-05-edit-cloud-meta-api |
| `8babac5` | feat(player): 编辑 sheet 云端获取预览勾选应用 |

### Status

[OK] **父任务完成**

---

## Session 95: 清除 Ionic 脚手架残留

**Date**: 2026-08-05
**Task**: 清除 Ionic 脚手架与代码残留
**Branch**: `main`

### Summary

D1=A：删除 ionic.config / ionic:* 脚本，标题 Muses，vite 死 chunk 与误导注释清理；同步 frontend spec；保留 Capacitor 与 changelog 历史。

### Main Changes

- 删除 `ionic.config.json`；`package.json` / `index.html` / `vite.config.ts`
- `src/theme/*`、`PlayerPage` 注释中性化
- `directory-structure` / `hook-guidelines` / `forms` / component 高度链表述

### Git Commits

| Hash | Message |
|------|---------|
| `a0ef82b` | chore: 清除 Ionic 脚手架与误导残留 |

### Testing

- [OK] lint / build
- [OK] AC1–AC6

### Status

[OK] **完成**

## Session 96: 依赖升级到最新（B + Android）

**Date**: 2026-08-05
**Task**: `.trellis/tasks/08-05-deps-upgrade-latest`
**Branch**: `main`

### Summary

按策略 B 升级 npm 与 Android 稳定最新；TS7 因 vue-tsc/typescript-eslint 工具链失败按 D5 pin 到 TypeScript 6.0.3；AGP 9.3.1 + Gradle 9.5.0 + OkHttp 5.4 成功 `assembleDebug`。

### Main Changes

**npm**
- Capacitor core/cli/android → 8.5.0；file-picker / native-audio patch；lucide 1.28；vite 8.2；eslint 10.8；tanstack / terser / plugin-legacy patch
- vue-router → 5.2.0（无业务改动）
- vue-tsc → 3.3.9
- typescript **pin 6.0.3**（非 7.0.2）：TS7 下 vue-tsc 找不到 `typescript/lib/tsc`；eslint typescript-eslint peer `<6.1.0`
- tsconfig：`moduleResolution: bundler` + `ignoreDeprecations: "6.0"`
- vite.config：`import.meta.url` 替代 `__dirname`

**Android**
- AGP 9.3.1 / Kotlin 2.4.10 / gms 4.5.0 / Gradle wrapper 9.5.0
- core 1.19 / activity 1.13 / webkit 1.16 / documentfile 1.1 / okhttp 5.4
- compileSdk **37**（core 1.19 要求）；targetSdk 仍 36；versionName 0.2.4 不变
- proguard → `proguard-android-optimize.txt`
- `android.builtInKotlin=false` + `android.newDsl=false` 兼容 Capacitor 旧 variant API
- OkHttp 5：`Response.body` 非空适配 AudioPlayer/WebDav/WebDavAudioCache

**故意不升**
- Pixi 7 / AMLL / happier-ui 0.0.8 / jaudiotagger 3.0.1 / appcompat 等仅有 rc 的包

### Testing

- [OK] `npm run lint`
- [OK] `npm run build`
- [OK] `npx cap sync android`
- [OK] `./gradlew :app:assembleDebug`
- [OK] `npm outdated` 仅剩 typescript latest=7.0.2（故意 pin）

### Status

[OK] **实现完成**（提交由主会话分段）


## Session 95: 修复歌曲页滚动到底部多余空白（底部 padding 双算 tab-bar）

**Date**: 2026-08-06
**Task**: 修复歌曲页滚动到底部多余空白（底部 padding 双算 tab-bar）
**Branch**: `main`

### Summary

诊断：SongsPage 虚拟列表底部 padding 重复计算了 tab-bar 高度（TabsPage main 已为其预留），导致滚动到底多出约 64px 空白。修复：SongsPage/PlaylistDetailPage 底部 padding 改为 mini-player-height + space-lg（移动端 80px）/ 平板端加 safe-area；SourcesPage 叠加预留并保留 24px 设计留白（space-xl）。同步修正 spec 避让职责边界约定（tab-bar 归 TabsPage main、列表只避让 MiniPlayer），防再双算。验证：lint/build 通过、Tailwind CSS 正确生成。

### Git Commits

| Hash | Message |
|------|---------|
| `7312c40` | (see git log) |

### Status

[OK] **Completed**

## Session 97 · 2026-08-06 · 修复歌单页弹层裸显 bug（08-06-playlist-page-fix）

### 现象
用户报告「一进入歌单页，列表下面出现『确定删除该歌单』提示」，确认无点击、无完整对话框。

### 根因（模拟器 WebView CDP 实证）
- PlaylistsPage.vue 模板用 `<h-bottom-sheet>`/`<h-dialog>`/`<h-input>` 但**未导入**对应组件（`import { HButton, HEmpty, HIcon, HNavBar, MCover }`）
- 项目无全局组件注册（main.ts 仅 use(router)）→ Vue 把未解析标签当原生自定义元素渲染
- 子内容无条件显示在 `.m-content` 文档流（列表下方），v-model 失效、无弹层样式
- 回归自 c7dc92b（脱离 Ionic 迁移）起存在；lint/build 未拦截（vue-tsc 对 kebab-case 未知标签视为自定义元素不报错）

### 修复（ff9da32）
1. PlaylistsPage 补导 HBottomSheet/HDialog/HInput → 弹层恢复 teleport/居中/遮罩
2. 启用 eslint `vue/no-undef-components`（error）防回归 → 顺带抓到 QueuePage 缺 HButton、TabsPage 缺 RouterLink 并修复
3. PlaylistsPage 列表底部补 MiniPlayer 避让 padding（与 SongsPage 一致）

### 验证
- 模拟器 CDP：裸文本消失；点更多→sheet 弹出；点删除→dialog 居中 + 全屏遮罩（显示「确定删除「test」」）；取消正常关闭
- lint + build 通过
- spec 更新：component-guidelines.md「Import Conventions」新增「组件必须显式导入」约定

### 方法论沉淀
- **工具链**：Android WebView 可用 `adb forward tcp:9222 localabstract:webview_devtools_remote_<pid>` + CDP（Runtime.evaluate）直接查真机/模拟器 DOM——比静态分析快得多
- **教训**：`vue/no-undef-components` 应纳入默认 lint 配置；模板组件使用必须与导入核对

## Session 97b · 2026-08-06 · BottomSheet 面板过窄（08-06-bottom-sheet-width）

### 现象
BottomSheet 面板不占满宽度（视口 360px 时仅 ~133px，内容宽）。

### 根因（模拟器 CDP 实证）
- HPopup 结构：`.h-popup`(flex) → `.h-popup__slot-anchor`(组件库无任何样式，flex item 宽度=内容宽) → `.h-popup__panel`(width:100% 相对 slot-anchor 解析)
- 0.0.9 的 #14 修复只改 `--h-bottom-sheet-max-width` 默认值，未解决宽度基准

### 修复（用户决策：只改组件库，Muses 不动）
- 组件库 Happier-X/happier-ui popup.css 合入 `.h-popup--position-bottom/top .h-popup__slot-anchor { width:100% }`，commit c4f119f 已 push；build:lib 产物验证通过
- 提 issue #15（https://github.com/Happier-X/happier-ui/issues/15）
- Muses 侧无代码改动；发布新版本后升级依赖即可

### 方法论沉淀
- flex 容器内无样式中间层（slot-anchor）会让子元素 width:100% 按内容宽解析——排查"宽度不对"时先看包含块链
- 相邻仓库发布前注意工作区未提交改动（HLoading 任务），避免污染发布包

## Session · 2026-08-06 · 设置页改用 HCell 组件（08-06-settings-cell-refactor）

### 背景
设置页（SettingsPage.vue）原为手写 h2/p/div 结构，不符合 spec 中"设置行统一用 HCellGroup/HCell"的约定。

### 改动
- Muses 版本信息、音量均衡两块改为 `h-cell-group`（title="关于"/"音频"）+ `h-cell`
- 音量均衡的 `h-switch` 放入 `suffix` 插槽；业务逻辑（watch 持久化、检查更新、toast）零改动
- 组间距用 `space-y-[var(--muses-space-lg)]`，外层保持 `md:max-w-[var(--muses-content-max-width)]`
- 从 `@/components/ui` 导入 HCell、HCellGroup（已转出）

### 验证
- `npm run build`（vue-tsc + vite）通过；`npm run lint` 通过

### 备注
- 页面 body 背景与 cell 同为 `--h-color-surface`，分组靠 inset 圆角区分，视觉协调

## Session 97c · 2026-08-06 · Muses 升级 happier-ui 0.1.1（BottomSheet 全宽）

- 组件库 0.1.1 已发布（含 #15 slot-anchor 修复），npm 0.1.1 dist 验证通过
- Muses 升级：happier-ui 0.0.10 → 0.1.1（精确版本），lint/build 通过，API 兼容
- 模拟器 CDP 实测：歌单操作 sheet 面板 360px=视口宽（修复前 133px）；删除确认 dialog 仍居中内容自适应（291px）
- 升级适配：PlayerPage h-toast 补 position="bottom"（0.1.0 HToast 重构默认居中，保持原行为）
- spec 同步：依赖版本 0.1.1 + BottomSheet 全宽约定（component-guidelines.md / directory-structure.md / quality-guidelines.md）
- 提交 9e96fdd

## Session 97d · 2026-08-06 · 设置页卡片风格 cell

- SettingsPage 两个 h-cell-group（关于/音频）加 variant="card"（happier-ui 0.1.1 支持）
- 模拟器实测：body x=16 / 宽 328（视口 360）/ 圆角 12px / margin 0 16px
- lint + build 通过；提交后归档

## Session 97e · 2026-08-06 · 全局页面背景改灰（凸显卡片）

- 问题：卡片（surface 白）与 body 背景（surface 白）同色，设置页卡片不可见
- 组件库 card 设计靠"灰底背景对比"凸显（playground 示例用 --h-color-bg-muted）
- 方案 B（用户选）：body 背景改 --h-color-surface-secondary（#f4f4f5），全 app 统一灰底
- SongsPage 顶部工具条同步改灰；navbar/tab-bar/MiniPlayer/平板 sidebar 保持 surface 白（独立表面）
- 暗色模式 token 自动适配（#2c2c2e）；模拟器实测卡片对比可见
- spec 补"页面背景 token"约定；提交并归档

## Session 97f · 2026-08-06 · 页面级 navbar 改灰底

- 灰底页面 + 白 navbar 突兀；.m-page .h-nav-bar → surface-secondary
- 选择器限 m-page 内：弹层（HPopup 白面板）内 navbar 保持 surface 白（播放队列验证 #ffffff）
- 模拟器实测：页面 navbar #f4f4f5 = body；弹层 navbar 白

## Session 97g · 2026-08-06 · 去掉页面级 navbar 底部分隔线

- .m-page .h-nav-bar 加 border-bottom: none（灰底与内容融合，细分隔冗余）
- 弹层内 navbar 保留默认线；实测设置页 navbar border 0px

## Session 97h · 2026-08-06 · 检查更新改为 cell 组件

- 设置页「检查更新」从独立 primary 大按钮改为「关于」卡片内 clickable cell（chevron）
- checking 时 description「正在检查更新…」，guard 防重入
- 实测：cell 出现/按钮移除/checking 状态正常；lint + build 通过

## Session 97i · 2026-08-06 · 歌曲页进入即滚到底（虚拟列表误滚动）

### 现象
进入歌曲页（冷启动或 tab 切换）列表自动滚到最底部。

### 排查过程（模拟器 CDP 实证）
- 复现：冷启动 scrollTop = sh - ch 精确到底（10 首 352 / 330 首 16984）
- 排除：scrollToCurrentSong 未被调用（localStorage 调用日志为空）、
  overflow-anchor:none 无效（全局注入仍滚）、行高 72=estimate（无 measure 偏差）
- reload 与冷启动均复现；挂载后注入/重建数据不滚 → "挂载过程"特有
- 疑 TanStack Virtual measure 期间 WebView 首屏布局时序漂移（机制未完全定位）

### 修复
- mount 后 4 秒内周期回顶兜底，用户 touchstart/wheel 即停（不打断手动滚动）
- SongsPage + PlaylistDetailPage 同款；QueuePage 补 [overflow-anchor:none]
- 验证：10/300 首冷启动 + tab 切回均 top=0

### 沉淀
- 虚拟列表首屏"精确滚到底" = sh-ch：优先排查 scrollToIndex/scroll anchoring/measure
- TanStack Virtual 建议 scrollElement 设 overflow-anchor:none

## Session 97j · 2026-08-06 · 列表最后一项与播放条间距

- 歌曲页滚到底后最后一项距 MiniPlayer 顶部 15px（space-lg 视觉间距）
- 用户要紧凑：padding 改为仅 mini-player-height（80→64px），最后一项紧贴播放条
- SongsPage + PlaylistDetailPage 一致；实测 gap 15px → ~0px
- spec 更新取值约定（去掉 space-lg）

## Session 97k · 2026-08-06 · tab 切换保留歌曲列表滚动位置

### 需求
从其他页面切回歌曲页时列表回到顶部，用户期望保留上次滚动位置。

### 关键发现（CDP 实证）
- onUnmounted 保存不可行：**Vue 卸载时 listParentRef.value 已为 null**（ref 清空）
- 模块级变量保存也不可靠：**懒加载 chunk 重新执行后模块变量归零**
  （切回时 mount 日志 saved=0，尽管滚动事件已保存 10000）
- 切回场景同样触发"误滚到底"：restore 设置 10000 后被拉到 max（27768），
  原恢复分支无防漂移兜底

### 方案
- sessionStorage 持久保存（'muses:songs-scroll-top'）：跨组件生命周期 + chunk 重执行
- scroll 事件实时保存（挂载初期 4 秒忽略，防误滚到底被存下）
- 统一 guard：4 秒内无用户交互且 scrollTop 漂移远离期望位置（保存值 clamp / 0）>500 则拉回；
  用户 touchstart/wheel 即停

### 验证
冷启动 top=0；滚到 10000 → 切走 → 切回 = 10000；用户触摸滚动 18000 不被拉回。

## Session 97l · 2026-08-06 · 翻译按钮激活态去背景

- 播放页歌词底栏翻译按钮（h-button ghost + is-active）打开翻译时有
  rgba(255,255,255,0.22) 常驻背景，用户要纯图标
- 修复：.player-overlay .h-button--ghost.is-active 背景三态（常驻/hover/active）
  全改 transparent，保留颜色提亮指示激活；build 产物验证 background:0 0

## Session 97m · 2026-08-06 · 歌词底栏左右图标视觉一致

- 翻译按钮（stroke 空心线条）与播放按钮（variant=fill 实心）同为
  HIcon size=20（几何一致），但实心填充在 20px 框内视觉更大 → 观感不一致
- 修复：歌词底栏播放按钮去掉 variant="fill"，两图标同为线条风格
- 注：封面 mode-bar 播放键保留 fill（该处左右均为填充风格）
- （回退 97m）用户决定播放/暂停保持 fill 风格（实心），翻译按钮 stroke 不变；
  两按钮视觉差异（实心 vs 空心）为用户接受的设计选择

## Session 97n · 2026-08-06 · 翻译图标改 fill 风格

- 用户要求翻译/取消翻译图标也 fill（与播放键实心一致）
- Captions/CaptionsOff 加 variant="fill"（svg fill=currentColor，线框变实心色块）
- 效果需真机确认（模拟器无歌词数据无法渲染）
- 翻译按钮打开态 Captions fill 效果不佳（实心色块）→ 换 lucide Languages
  （翻译图标），fill 后为"文"字符号实心；关闭态 CaptionsOff 保留
- 取消翻译也从 CaptionsOff 换 Languages：开/关同图标，靠 is-active 颜色提亮区分
- 翻译图标去掉 fill 恢复 stroke 线条风格（Languages 图标保持）；播放键仍 fill

## Session 97o · 2026-08-06 · 播放键与 mode-bar 亮色

- 歌词底栏播放/暂停按钮非激活色 0.58 → #fff（lyric-play-toggle 专属规则）
- 封面 mode-bar 四按钮（repeat/shuffle/list/more）非激活 0.58 → #fff
- 激活状态不依赖颜色：repeat/shuffle 用图标实心 vs 空心切换指示

## Session 97p · 2026-08-06 · 翻译按钮播放前不显示

- 原因：v-if=hasLyricTranslation 依赖歌词加载（播放后才拉取）→ 首次打开无按钮
- 修复：移动端（!isTabletLayout）浮层显示即常显翻译按钮；无翻译点击 toast 提示
- 平板保持仅 hasLyricTranslation（浮层本身逻辑）

## Session 97q · 2026-08-06 · 恢复会话即匹配在线歌词

- 用户：未播放（打开播放页）歌词已展示但翻译按钮不显示，播放后才出现
- 根因：restorePlaybackSessionIfNeeded 恢复会话时 lyricsTranslation=null
  且不触发 matchOnlineLyricsForSong；播放（playSong）才匹配
- 修复：恢复会话也发起在线匹配（++lyricsMatchToken 独立 token 防串曲），
  翻译/按钮尽快可用；上一轮还改了移动端翻译按钮常显 + 无翻译 toast

## Session 97r · 2026-08-06 · 歌曲页悬浮气泡外观优化

- 默认 HFloatingBubble 实心主题蓝圆球突兀 → 改 surface 底 + 主题色图标
  + border-subtle 细边框 + 柔和阴影（token 驱动暗色自适应）
- HFloatingBubble teleport 到 body，scoped 失效 → tailwind.css 全局覆盖
  （仅 SongsPage 使用）；hover 阴影加深
- 气泡图标 locateOutline → Crosshair 十字准星（更贴定位语义）
- 气泡 48→40px、图标 20→18px；offset 56→48 保持贴边

## Session 97s · 2026-08-06 · 跳转气泡不挡更多按钮

- 气泡固定右下与行更多按钮（右缘 8-48px）重叠
- 改为条件显示：当前歌曲在视口（virtualRows 含 index）不显示；
  滚动中（scroll 防抖 300ms）隐藏；跳转后自动消失
- 模拟器验证四场景全过

## Session 97t · 2026-08-06 · 歌词滚动后自动回高亮行

- 需求：沉浸歌词滚动后等待片刻自动回高亮行
- AMLL 0.5.2 自带 5 秒归位（beginScrollHandler 写死 5e3），仅播放中
  时间更新驱动时生效；暂停滚动不归位
- 实现：LyricPlayer @wheel/@touchmove（fallthrough 根元素）→ 停止
  2 秒后 player.resetScroll() + calcLayout()（公开 API），暂停也生效

## Session 97u · 2026-08-06 · 歌词自动回位修复（root cause）

- 用户反馈"滚动后自动回高亮行"不行；模拟器复现：滚动发生但 2 秒后不回位
- 排查链：事件 fallthrough 正常（touchmove 9 次）→ 滚动正常（activeY 319→-34）
  → getLyricPlayerInstance 返回 null（hasPlayer false）
- **Root cause**：Vue `expose()` 返回值经 proxyRefs 自动解包——`exposed.lyricPlayer`
  直接是 LyricPlayerBase 实例（非 Ref），原 `comp?.lyricPlayer?.value` 恒 undefined
- 修复：`comp?.lyricPlayer ?? null`；实测滚动后 2.9s activeY -34→319 回位成功（含暂停态）
- 模拟器歌词测试环境：注入带 lrc 的歌曲 + 正确 queue 格式（{items:[{songId}]}）
  + playback-session，写入后需等 ~2s 刷盘再 force-stop 才持久
- PlayerPage 面板切换在模拟器 swipe 不可靠（命中封面按钮），用 JS 强制设置
  .panels transform 使歌词面板可见再测

## Session 97v · 2026-08-06 · 翻译按钮无翻译不显示

- 需求：歌曲没有翻译时不展示底部翻译按钮（此前移动端常显、无翻译点击 toast）
- 改动：v-if "!isTabletLayout || hasLyricTranslation" → "hasLyricTranslation"
- 在线匹配中按钮隐藏，匹配成功后自动出现
- 模拟器验证：纯 lrc 歌 fab 仅播放键；带 tlyric 歌翻译+播放两键

## Session 97w · 2026-08-06 · 沉浸页无封面占位压扁修复

- 需求：无图片时封面占位也要正方形
- 复现：placeholder 259×72（扁）；aspect-ratio:1 在明确 width + max-height:100%
  下失效（max-height 优先），高度回落内容行高（♪）
- 修复：height 显式公式 = width 公式（min(72vw,340px,52dvh)）；height 公式
  不能含 100%（高度百分比依赖父容器高度→内容→循环，整个 min() 失效回 auto）；
  模板去掉 max-h-full；三处媒体查询同步
- 验证：占位 259×259 正方形；img 共用公式 + object-cover
- 环境坑：无效 uri 歌播放失败会触发 stopPlayback → clearPlaybackSession，
  注入会话会被 app 运行期清空（CDP 验证反复踩坑）

## Session 97x · 2026-08-06 · toast 恢复居中

- 需求：toast 改回默认居中（此前 BottomSheet 全宽时改过 bottom）
- 改动：PlayerPage/SettingsPage/SourcesPage 去掉 position="bottom"，
  h-toast 默认 position='center'

## Session 97y · 2026-08-06 · 发布 v0.2.5

- 版本：0.2.5 / versionCode 25（package.json / lock / build.gradle 同步）
- 新增 changelog/v0.2.5.md（自 v0.2.4 起约 40 个功能提交：列表滚动位置保留/气泡、
  歌词自动回位/翻译按钮、封面占位正方形、设置页卡片、toast 居中等）
- 本地 lint/build/assembleDebug 验证 → commit chore(release): v0.2.5 (455c144)
- push main + tag v0.2.5；GitHub Actions 3m16s 构建通过
- Release: https://github.com/Happier-X/muses/releases/tag/v0.2.5
  （muses-v0.2.5.apk 主包 + muses-v0.2.5-mi.apk MIUI 包）

## Session 97z · 2026-08-06 · 扫描弹窗改居中 dialog

- 需求：音源点击扫描时不用 bottom-sheet，换 modal
- 改动：SourcesPage 扫描设置 + 扫描进度 h-bottom-sheet → h-dialog（居中）；
  添加/编辑/WebDAV 仍 bottom-sheet
- h-dialog API：modelValue/title/closeOnOverlay/closeOnEsc/close 事件/#title slot
- 模拟器实测：扫描设置面板 y=202 h=211（屏幕 616 居中），非底部滑出

## Session 97aa · 2026-08-06 · 专辑/艺术家点击进详情

- 需求：专辑页和艺术家页的条目可点击进入详情（歌曲列表）
- 方案：通用 LibraryDetailPage.vue（/tabs/library/:kind/:name），
  kind 决定分组来源；虚拟列表复用 PlaylistDetailPage 模式（含冷启动回顶兜底）；
  AlbumsPage/ArtistsPage 卡片 @click + cursor-pointer
- 验证：专辑/艺术家点击进详情正常；播放全部正常；返回正常
- 注意：lib-play-test 曾出现 rows=0 偶发（播放失败+存储竞态），重挂载后不复现

## Session 97ab · 2026-08-06 · 详情页对齐歌曲页功能

- 需求：专辑/艺术家详情页与歌曲页一样：随机播放全部 + 跳转当前歌曲气泡
- 实现：LibraryDetailPage 加 shuffle 操作栏（onShuffleAll 同 SongsPage）；
  跳转气泡（currentPlayingInList && !inViewport && !isListScrolling，
  scrollToIndex + 1.2s 高亮）；fabOffset 无 tab bar
- 验证：459 首'未知专辑'进详情；气泡显示→完整 pointer 点击
  （pointerdown/up/click）scrollTop 0→32736 跳转成功；
  播放行 is-playing 可见、气泡自动隐藏
- 坑：程序化 b.click()（无 pointer 序列）不触发 HFloatingBubble emit，
  真实触摸走 pointerdown→up→click 才有效——测试须 dispatch 完整序列

## Session 97ac · 2026-08-06 · 横屏沉浸页封面重叠修复

- 复现：模拟器 wm user-rotation lock 1 转横屏（640x336），
  封面 128x141（非正方形）底部 148 与 song-info 顶部 143 重叠 5px
- 根因：cover-slot flex 0 1 auto 可收缩；横屏高度不足时槽位被压扁，
  封面显式 height 溢出槽位覆盖下方文字
- 修复：orientation: landscape 媒体查询——cover-slot flex 0 0 auto +
  封面公式 min(34vw,100%,220px,40dvh) + info-panel-inner 可滚动兜底
- 验证：横屏 640x336 封面 134x134 overlap 0；竖屏 259x259 overlap 0 无回归
- 模拟器旋转命令：adb shell wm user-rotation lock 1（锁横屏）/ lock 0（回竖屏）

## Session 97ad · 2026-08-06 · 播放队列改底部弹出 popup

- 需求：播放队列改用 popup 组件实现（用户感知 fullscreen 形态像页面非弹层）
- 现状：QueuePage 已是 HPopup（position=fullscreen，19a0096 迁移）
- 改动：fullscreen → position=bottom + handle；navbar 换自定义 header
  （标题+清空+关闭）；close-on-overlay/esc 恢复默认
- 验证：面板 y=418 贴底 h=198 全宽、handle 存在、背景可见、
  关闭按钮正常；可见面板选择器需过滤尺寸>50（多 popup keep-alive）

## Session 97ae · 2026-08-06 · 队列面板默认半屏 + bottom 面板 max-height 修复

- 需求：队列面板默认打开半屏
- 坑1：HPopup 类 fallthrough 不生效（teleport 根，queue-popup 类被吞）
  → 改在 slot 内容根 div 加 queue-popup-panel（min-height 50vh）
- 坑2：组件 max-height: min(88vh,100%) 的 100% 依赖父容器高度，
  父高不确定时整个 min() 无效 → 面板被虚拟列表内容撑爆
  （465 首实测 h=33606）→ tailwind.css 全局覆盖纯 88vh
- 验证：min-height 308px 生效；465 首面板 542px clamp+滚动；
  空态 430px（h-empty 撑高）；单首场景=50vh
- 注：queue 注入后 force-stop 需等刷盘（queue-one 脚本写入后立即
  force-stop 导致 queue 未恢复 rows=0——测试环境竞态非功能问题）

## 编辑歌曲信息：来源平台选择（MusicTag 式）
- 需求：用户希望像 MusicTag 一样在编辑弹窗里选择来源平台获取元信息
- 实现：云端 section 顶部加平台 chips（全部/网易云/QQ音乐/酷狗/酷我/咪咕/iTunes）
  - types.ts 新增 CloudPlatformId + SearchEditCloudMetaOptions.platform
  - searchEditCloudMeta：platform 过滤文本/封面/歌词 provider；歌词限定平台时跳过
    amll 聚合库；itunes 无文本/歌词（仅封面）
  - PlayerPage：cloudPlatform ref + chips UI（aria-pressed 高亮）+ 获取中文案
    "正在从酷狗获取…" + cloudSourceLabel 中文映射（kg→酷狗、tx/qrc→QQ音乐、
    lrclib→LRCLIB、amll→AMLL、wy→网易云、kw→酷我、mg→咪咕、itunes→iTunes）
- 验证：CDP 实测——chips 渲染 7 项、网易云/酷狗选中 aria-pressed 切换、
  获取中文案"正在从酷狗获取…"、全部模式混合获取（文本4·封面5·歌词4）、
  候选来源中文显示（酷我/iTunes）
- 提交：0116d49
- 坑：write 工具 /tmp 解析到 C:\tmp（git-bash /tmp 是 %TEMP%）——测试脚本
  写入后需 mv；adb server 会掉（emulator-5556 消失），kill-server 重启即可

## 编辑歌曲信息：歌词渠道与元信息渠道拆分（tab）
- 需求：歌词渠道（wy/tx/qrc/kg/kw/mg/lrclib）与文本/封面渠道不同，
  用户要求用 tab 分开（MusicTag 式渠道管理）
- 实现：
  - 编辑弹窗顶部 segmented tab：基础信息 / 歌词
  - 基础信息 tab：云端文本+封面（chips 含 iTunes 无 LRCLIB）+ 标题/艺术家/专辑/封面/RG
  - 歌词 tab：独立歌词渠道（chips 含 LRCLIB 无 iTunes）+ 候选列表/预览/
    应用所选歌词 + 歌词 textarea；歌词状态文案独立 lyricsStatusMessage
  - searchEditCloudMeta 扩展：dimensions（按需搜索）+ lyricsPlatform（独立歌词平台）
  - 结果按维度 merge（cloudResult 合并只覆盖本次 dims），切 tab 不丢结果
  - 歌词 tab 简化交互：候选点击选中 → "应用所选歌词"（无需勾选）
- 验证（CDP 实测）：基础 tab 获取="文本4·封面5"（无歌词）；
  歌词 tab="歌词4"（无文本/封面）；LRCLIB pressed 切换；歌词候选中文来源
  （酷我/QQ音乐/网易云/酷狗）；应用所选歌词填入 textarea 2332 字；
  切 tab 双向保留结果
- 坑：EditCloudMetaResult 用 EditDimKey 索引报 TS7053——改为
  cloudDim: Record<EditDimKey, EditDimResult<unknown>> 映射；
  spread 对象里 Ref 未解包（lyricsPlatform）→ 显式赋值 options
- 提交：b43aefa

## 歌词云端获取：amll 始终参与（不再随平台过滤跳过）
- 背景：用户质疑"选平台为什么跳过 amll"——当初为"纯平台语义"在
  lyricsPlatform!=='all' 时设 includeAmll=false
- 决策：amll 是质量最高的独立来源（TTML 逐行时间轴），平台 chips 只应过滤
  各平台 provider，amll 始终参与候选
- 实现：searchLyricsDimension 无条件执行 matchAmllTtmlLyrics；
  删除 includeAmll 参数与分支；platformLyricsIds 加 lrclib:['lrclib']
- 验证：模拟器实测——网易云/LRCLIB/全部模式获取行为一致（amll 均尝试）；
  **环境限制**：jsdelivr 1.5MB 索引在模拟器下载 25s+（>ensureIndex 20s 超时）
  → amll 在模拟器任何模式都无法加载索引（含"全部"），非本次改动引入；
  负缓存（queryKey 变化自动失效）确认无阻塞
- 提交：f243cf7

## 2026-08-09 Konsta UI 迁移完成（08-09-konsta-ui-migration）

- **阶段 2 逐页迁移**（由简到难）：Settings → Albums/Artists → LibraryDetail → Playlists/PlaylistDetail → Sources → Songs → Queue → PlayerPage 全部完成
- **关键 Konsta API 细节**：
  - k-toggle/k-checkbox/k-range 无 v-model：`:checked`/`:value` + 原生 `@change`/`@input` 事件手动同步
  - k-button 无 icon slot（只有默认 slot）；k-fab 有 icon slot；k-navbar 无 fixed prop（sticky 自带）
  - k-popup 无 position prop（iOS 默认全屏底部滑入），层叠靠 DOM 顺序，关闭态 translate-y-full 移出屏幕
  - k-tabbar 内部包一层 k-toolbar（选择器要用 .k-toolbar 或 .k-tabbar-link）
  - k-list-input 支持 type="textarea"（替代 HTextarea）；:value + @input 适配 TanStack Form 的 handleChange
  - k-progressbar 无 indeterminate → 扫描进度用 k-preloader 替代
  - k-dialog 用 :opened + #buttons slot + k-dialog-button（strong 强调）
  - k-actions（iOS action sheet）= k-actions-group + k-actions-label + k-actions-button
  - k-range iOS thumb/track 默认主题蓝，沉浸区需 nth-child 后代覆盖为白色
- **类冲突坑**：Konsta 组件 class 与自定义任意值（text-[#ff3b30]）可能被主题类覆盖 → 用 `!` important 前缀
- **tokens 清理**（方案 A）：--h-*/--muses-* 全部内联（space-xs 2px/sm 8px/md 12px/lg 16px、mini-player 64px、tab-bar 96px、song-row 72px、content-max 720px、immersive 色板 #05070d/#0a0c14/#171b2b 等）
- **自建组件**：MEmpty（iOS 空状态：72px 圆形图标底 + title + description）；MCover/MiniPlayer 样式同步 Konsta 体系
- **验证**：build + lint + vue-tsc 全通过；happier-ui 卸载；Android 模拟器 CDP 冒烟通过（tab/列表/开关/action sheet/播放器 k-range 播放/队列层叠/编辑表单/暗色）
- 提交：3869d83 / 031b03f / 8d081f7
- **k-tabbar-link 图标槽位坑**：图标须放 `#icon` 命名 slot；放默认 slot 会被并入 label span → 图标左文字右（横向）。`#icon` 才有 icon 容器（w-7 h-7 圆形激活底）。k-tabbar 容器实际渲染为 k-toolbar。提交后模拟器 CDP 验证：图标 top 546 < 文字 top 574、同列居中 ✓
- **Konsta 弹层 z-index 冲突修复**：Konsta 全部弹层默认 z-40，低于 Muses k-tabbar(950)/MiniPlayer(1000) → k-actions 等贴底浮层被盖。tailwind.css 统一覆盖层级阶梯：k-popup 1100 < k-sheet/k-dialog/k-actions 1200 < k-toast 1300；backdrop 无标识 class，用 `div:has(+ .k-xxx)` 选中面板前一兄弟同步提升（backdrop DOM 在前、面板在后）。k-fab 由 z-40 提至 z-[1100]。模拟器 CDP 验证：添加音源面板 z=1200、rect 352→616 全屏底覆盖 tabbar 区，backdrop z=1200 全屏可见；popup/sheet/toast z 阶梯正确
- **WebView 110 渐变失效（oklab）**：用户反馈 tabbar 不像 Konsta 默认。排查：Konsta toolbar/navbar 背景用 Tailwind v4 `bg-gradient-to-t/b`（生成 `to top in oklab` 插值语法），Android WebView 110（Chrome<111）不支持 → linear-gradient 整体无效（background-image:none）→ tabbar/navbar 透明。CDP 实测 CSS.supports=false；color-mix 半透明类 Tailwind 自带 @supports fallback（预计算 rgba）不受影响。修复：tailwind.css 加 `@supports not (linear-gradient(to top in oklab,...))` 兼容块，k-toolbar>div:first-child / k-navbar>div:nth-child(2) 补 sRGB 等价渐变（亮 from-ios-light-surface / 暗 rgba(0,0,0,0.5)），新版浏览器仍用原生 oklab。模拟器 CDP 验证亮/暗渐变方向正确
- **回归 Konsta 默认样式审查**：全面审查 k-* 组件魔改情况，用户拍板——① 删 `.m-page .k-navbar` 纯色覆盖恢复默认玻璃渐变（旧 WebView 由兼容层提供 sRGB 等价）；② k-range 沉浸白色保留（PlayerPage 专属业务）；③ 4 处危险按钮 `!text-[#ff3b30]` 移除改回 Konsta 默认主题蓝（PlaylistsPage 删除/QueuePage 清空、删除/SourcesPage 删除；PlayerPage 编辑表单错误提示红保留——iOS 错误提示规范）；④ z-index 阶梯 + WebView 兼容层保留。顺手修 LibraryDetailPage fab z-40 → z-[1100]。模拟器验证：navbar 渐变恢复、删除按钮 rgb(0,122,255)
- **危险按钮 Konsta 官方红色**（用户："k-button 明明有红色的按钮啊，然后按钮都用它默认的那个圆角按钮"）：k-button 颜色官方机制是 `colors` prop（filterColors 合并默认色类）；Konsta 生态危险色官方是 `text-red-500`，但 Tailwind v4 调色板用 **oklch 颜色 → WebView 110 失效**（`--color-red-500` 变量解析出的 oklch 无效 → background-color 透明）→ 改用 **iOS 系统红 `#ff3b30` 任意值 hex**（全兼容）：SourcesPage 删除 fill `bg-[#ff3b30] active:bg-[#e03428]`、QueuePage 清空/行删除 clear 图标 `text-[#ff3b30] active:bg-[#ff3b30]/15`、PlaylistsPage actions 删除 `text-[#ff3b30]`、dialog 删除确认 strong `fillBgIos` 红（**k-dialog-button 无 colors prop 但 attrs 自动透传到根 k-button，实测生效**）。按钮形状全 Konsta 默认：文字按钮 iOS 默认 4px 圆角（rounded prop 缺省=square `rounded` 类）、图标按钮 rounded 胶囊。模拟器实测：删除按钮 bg rgb(255,59,48)、actions 删除项红色、dialog 删除红色
- **文字按钮统一 rounded 胶囊**（用户纠正："不是，按钮不是有rounded的按钮吗，用那个"）：上一轮误用 Konsta iOS 默认 4px 方角（square），用户明确要 `rounded` prop 的 rounded-full 胶囊。全局给文字 k-button 加 rounded：SourcesPage 编辑/删除/扫描三件套 + 重新选择目录/保存修改/开始扫描/关闭/WebDAV 连接/上一级/进入/添加选中；PlayerPage 译文切换/云端获取/应用/选择图片/取消/保存等 10 处。图标按钮（size-8）本已 rounded 不动。模拟器实测三按钮 radius 9999px 胶囊 ✓
- **真机顶部安全区修复（Konsta 桥接）**：用户反馈真机顶部安全区不适配。排查结论——① Konsta 的 `--k-safe-area-*` 源是 `env(safe-area-inset-*)`，Android 上非刘海屏恒 0；② Capacitor 8 **原生内置 SystemBars 插件**（config 里 `insetsHandling:'css'` 即启用，非 npm 包），WebView≥140 时把真实 insets 注入 `documentElement` 的 `--safe-area-inset-*`（<140 注入 0px，模拟器 WebView110 即此）；③ 两套变量没桥接 → navbar `pt-[max(16px,var(--k-safe-area-top))]`=16px 顶部被状态栏盖。修复：tailwind.css 在 theme.css 之后同 specificity 覆盖 `.safe-areas`，`--k-safe-area-*` 桥接 `var(--safe-area-inset-*, env(…,0px))`；项目内 10 处直接 env() 引用（MiniPlayer/LibraryDetail/PlaylistDetail/Playlists/Queue/Songs/Sources/Tabs）统一为 var 三级回退。CDP 模拟注入 72px → navbar padding 16→72px、tabbar 高度含 bottom safe ✓。真机（WebView≥140）自动生效；模拟器需新 WebView 验证
- **Konsta 官方工具类替代自定义 safe-area 定位**（用户提示看 konstaui.com/vue/safe-areas 文档）：官方机制 = viewport-fit=cover + 根元素 safe-areas class（k-app safeAreas 默认 true）+ `--k-safe-area-*` 工具类（pt-safe-*/pb-safe-*/top-safe-*/bottom-safe-*/left-safe-*/right-safe-*，value 是 spacing 倍数 4px）。改造项目 9 处自定义 var() 定位 → 官方工具类：MiniPlayer bottom-safe-24/md:bottom-safe-0、SongsPage/LibraryDetail/PlaylistDetail md:pb-safe-16、PlaylistsPage pb-[80px] md:pb-safe-20、QueuePage pb-safe-6、SourcesPage pb-[88px] md:pb-safe-22 + sheet pb-safe-0、TabsPage md:pt-safe-3 + pb-safe-24、PlayerPage 歌词浮动 bottom-safe-2。验证：工具类规则生成于 @media(min-width:48rem) 内（md 变体）✓，语义 = calc(var(--k-safe-area-*) + spacing*n)，桥接后消费 Capacitor 真实 insets
- **iOS 26 悬浮胶囊 tabbar**（用户："demo 里看到苹果圆角风格"）：查证 Konsta 官方 kitchen-sink demo 与本地同款组件，Konsta v5.3 iOS tabbar 即通栏玻璃（无圆角悬浮）；用户指的是 iOS 26 Floating Tab Bar，Konsta 未内置 → 自定义扩展 k-tabbar：`fixed left-4 right-4 bottom-safe-3 !w-auto !pb-4 rounded-[28px] overflow-hidden shadow bg-white/75 dark:bg-[#1c1c1e]/75` + 自定义 `.tabbar-glass-blur` 原生 backdrop-filter:blur(24px)。**两个新坑**：① k-toolbar base `w-full` 压过 left/right 拉伸（要 !w-auto）；② Tailwind v4 `backdrop-blur-*` 生成 `var(--tw-backdrop-blur,) var(--tw-backdrop-brightness,)...` 组合语法，空逗号在 WebView<117 整条失效（Konsta bgBlur 层 backdrop-blur-[2px] 同理失效）→ 自定义原生 backdrop-filter 类。模拟器实测：328px 胶囊、28px 圆角、blur(24px)、半透明白、阴影 ✓；Vue 版 k-toolbar 结构 2 层（bg + inner，无 bgBlur）
- **Konsta 官方 iOS 26 玻璃 tabbar 找到了**（用户纠正：konstaui.com/vue/tabbar example 就是玻璃效果）：官方内置 **k-toolbar-pane** 组件（v5.3 新增，"finalize iOS toolbar pane"）——k-glass 玻璃胶囊（bg-ios-light-glass rgba(255,255,255,0.75) / dark rgba(50,50,50,0.5) + shadow-ios-light-glass 内阴影 + backdrop-blur-lg）+ rounded-full + **激活高亮滑条**（useIosTabbarHighlight 玻璃 thumb）。官方结构：`<k-tabbar class="left-0 bottom-0 fixed" :labels :icons><k-toolbar-pane><k-tabbar-link/></k-toolbar-pane></k-tabbar>`，k-toolbar base 自带 pb-safe-4/px-safe-4 → 胶囊天然悬浮（宽=屏宽-32px、底=16px+inset）。**重构**：删掉手写胶囊样式（!w-auto/!pb-4/rounded/bg-white/75/shadow），TabsPage 用 kToolbarPane 官方组件，pane 上保留 tabbar-glass-blur 兜底 WebView<117 的 Tailwind backdrop-blur 组合语法失效。模拟器验证：328px 胶囊 + rounded-full + glass 半透明 + blur(24px) + 高亮滑条 ✓
- **移除全部老 WebView 兼容层**（用户："新手机没必要保留"）：① 删 tailwind.css `WebView<111 oklab 渐变兼容层`（@supports not 块，k-toolbar/k-navbar 渐变 sRGB 兜底）；② 删 `.tabbar-glass-blur` 自定义类（WebView<117 Tailwind backdrop-blur 组合语法兜底）→ k-toolbar-pane 回归官方 backdrop-blur-lg。**保留** safe-area 桥接（`.safe-areas` 覆盖 --k-safe-area-*，真机 WebView≥140 必需，非兼容层）。spec 删 oklab 兼容节、补 k-toolbar-pane 官方结构契约（禁止手写胶囊样式）。模拟器 110 验证 blur none 属预期退化，真机新 WebView 正常
- **MiniPlayer 改 iOS 26 玻璃胶囊**（用户："跟 tabbar 一样更和谐"）：去掉通栏白底 + border-t 分隔条，改用 Konsta glass token 类（bg-ios-light-glass / shadow-ios-light-glass / backdrop-blur-lg + rounded-full），fixed left-4 right-4 悬浮胶囊（328px 宽、h-16 64px 高），与 tabbar 胶囊同宽对齐、间距 16px（bottom-safe-24 → 底 96px+inset，tabbar 顶 80+inset）；md 断点保持通栏贴底（md:left-0 md:right-0 md:rounded-none md:bottom-safe-0）。直接复用 token 类无需 k-glass 组件（避开 touch-none）。模拟器 110 验证：328px 胶囊 + rounded-full + 官方玻璃色/内阴影 ✓（blur none 属预期退化）
- **navbar 排查**（用户怀疑使用有问题）：对照官方文档 + 源码 + 实测——① k-navbar 自带 `sticky top-0`（官方 example 的 class="top-0 sticky" 冗余）；② left/right slot 官方自动用 k-glass 包裹（结构正确）；③ **centerTitle 默认值 = theme==='ios'** → iOS 主题下标题本来就居中，我们写的 `center-title` prop 冗余 → 已从 7 处删除（MPage + 6 页面），行为不变更贴近官方 example；④ 模拟器实测 navbar 背景 `background-image:none` = 删 oklab 兼容层后的预期副作用（WebView 110 不支持 oklab 渐变），真机新 WebView 正常
- **navbar 右侧图标按钮不是圆的排查**（用户反馈）：k-button 本身 32x32 rounded-full 正圆，但 Navbar 源码把 slots.right 包在 **k-glass** 里，且 NavbarClasses right 有 `h-full` → k-glass 被 inner 44px 拉伸成 **32x44 竖椭圆**，且 bg-ios-light-glass 白玻璃在浅色页面上几乎隐形（模拟器 blur 失效）→ 视觉只剩蓝图标。修复：k-navbar 传 `rightClass="!h-8"`（Navbar 有 rightClass prop）→ k-glass 变 32x32 正圆。全 7 处 navbar 统一加。⚠️ sed 批量替换误伤 `<k-navbar-back-link`（被 `<k-navbar[^>]*>` 正则替换成 k-navbar 开标签），LibraryDetailPage/PlaylistDetailPage 结构错乱，手动恢复
- **页面容器改用官方 k-page**（用户："应该使用 Page 组件"）：项目此前用自建 `<div class="m-page">`（flex 分区滚动，迁移时的决策）。迁移：7 处（MPage + 6 页面）根元素改为 `<k-page class="m-page flex flex-col overflow-hidden !h-auto !bottom-safe-24 md:!bottom-0">`——k-page 官方提供 absolute 定位 + iOS 表面色背景（bg-ios-light-surface），叠加 flex 保持分区滚动模型；**!h-auto 覆盖 k-page 自带 h-full**，用 bottom-safe-24 精确避让 tabbar 预留区（k-page 填满含 padding 会盖住底部）；**TabsPage main 加 relative**（k-page absolute 锚点）。实测 SongsPage：k-page top0 bottom520（616-96 精确）、bg #efeff4、navbar sticky、虚拟列表容器 412px（520-60-48）独立滚动 ✓ 行为与迁移前一致
- **QueuePage 补 k-page**（k-popup 内）：queue-popup-panel 根 div → `<k-page class="queue-popup-panel flex flex-col overflow-hidden">`（k-page 自带 h-full/absolute/背景），虚拟列表分区滚动不变。实测 popup 内 k-page 渲染正常
- **PlaylistDetailPage 虚拟行套 k-list-item 试点**（用户同意后）：行内自绘 div（cover+title+subtitle+按钮）→ `<k-list-item :title :subtitle titleClass="min-w-0 truncate" subtitleClass="truncate" class="h-full">` + #media（m-cover 48）/ #after（移除按钮）slot。验证：3 行渲染 ✓、官方布局（media 区 + titleWrap text-[17px] + subtitle text-sm）✓、虚拟化 measureElement 动态测量自适应行高 100px（原 72px，官方两行布局更高）✓、无分割线（hairline 独立渲染失效/不可见，与原自绘行一致）；移除按钮 after slot 正常。⚠️ 测试歌单注入 localStorage 后已清理
- **k-list-item 推广到全部虚拟列表页**（用户"推广"）：SongsPage（封面+标题+更多按钮 after）、LibraryDetailPage（无 after）、QueuePage（序号+删除按钮 after）、PlaylistDetailPage（试点）全部改用 k-list-item 虚拟行：title/subtitle prop + #media（m-cover 48 radius=sm）+ #after（操作按钮），titleClass/subtitleClass 截断，class 透传保留 data-song-id/is-playing/高亮。实测：行位置精确（108/208/308 连续）、行高稳定 100px（measureElement 动态）、虚拟滚动正常；SourcesPage 音源卡片（多行文本）不适合保留自绘。spec 虚拟列表行契约已更新
- **虚拟列表套 k-list 官方外壳**（用户指出"歌曲页面用的 list 不像官方推荐"）：官方 demo Songs = `k-list strong-ios outline-ios` + `k-list-item link`。查源码发现分割线机制：k-list provide ListContext（dividersIos 默认 true）→ item 的 dividers 未指定时从上下文取 → 裸 k-list-item 无 ListContext 就无 hairline-b（此前"分割线缺失"根因）。改造 4 页（Songs/Queue/PlaylistDetail/LibraryDetail）：虚拟容器外包 `<k-list strong-ios outline-ios class="!my-0">` + item 加 `link`。效果：strong 白底（bg-ios-light-surface-1）+ outline 上下 hairline + inset 分割线（文本区 hairline-b，媒体后开始，rgba(0,0,0,0.2) scaleY(1/dpr)）+ link 点击。实测行高 76px（link 结构 padding 在 itemContent）。**踩坑**：①闭合 tag 编辑时误删滚动容器 </div> 导致 build 失败（PlaylistDetail/LibraryDetail 各补一次）；②QueuePage 编辑误加重复 role/aria-label 已撤；③PlaylistDetailPage pushState 导航"歌单不存在"是路由缓存，真实 tab 点击正常。队列空态 = 播放会话 resolver 依赖内存歌曲缓存（环境限制，非改动问题）
- **k-list-item link 去掉 chevron 箭头**（用户："不想要右边箭头"）：link prop 会渲染 chevron（`chevron-icon v-if="isLink && hasChevron && !menuListItem"`，hasChevron = props.chevron ?? iOS chevronIos=true）→ 4 页 k-list-item 加 `:chevron="false"`（注意：`chevron="false"` 字符串会 TS 报错，必须绑定语法）。实测箭头消失、after slot 按钮不受影响
- **SongsPage 更多按钮垂直居中**（用户："右侧更多按钮能居中吗"）：根因 = k-list-item 的 #after slot 在 titleWrap 内（flex items-center，仅 32px 高、偏行上部）→ 按钮偏上。方案：行根加 `relative`（k-list-item class="h-full relative"）+ after 按钮 `!absolute !right-2 !top-1/2 !-translate-y-1/2` 脱离流垂直居中整行（offset=0 实测）。备选 titleWrapClass min-h 会撑高行（76→96）弃用；titleWrap h-full 对 auto 高父无效。行高 72 稳定（按钮脱流后）。其他页 after 按钮（移除/删除）用户未提，保持官方标题行位置
- **SongsPage 去掉行间分割线**（用户："底下的分割线不喜欢"）：k-list 加 `:dividers-ios="false"`（List.vue dividersIos 默认 true → provide ListContext → item hairline-b；关闭后无分割线）。注意 kebab 绑定必须 `:dividers-ios="false"`（字符串报 TS 错）。实测 hairline class 消失 + 像素行交界纯白；outline-ios 上下边线保留（列表分组边框非行间线）。行高 72 不变。仅 SongsPage（用户未提其他页）
- **SongsPage 随机播放横条 → k-list-item 列表第一项 + sticky 吸顶**（用户选方案 A，疑问"滚走怎么办"→ iOS 分区标题标准 sticky 行为，Konsta demo group-title 官方写法）：删 48px 白底横条；k-list 虚拟容器前加 `<k-list-item link :chevron="false" title="随机播放全部" :subtitle="N 首歌曲" class="sticky top-0 z-10 bg-ios-light-surface-1 dark:bg-ios-dark-surface-1" @click="onShuffleAll">` + #media shuffle 图标（size-5）。sticky top-0 吸在滚动容器顶（=navbar 下方），滚动时始终可见。实测：scrollTop=300 后 shuffle 行 top=60=scrollerTop（stickyStuck=true）；行高 72；列表从 navbar(60) 直接开始。虚拟容器在 shuffle 行后正常流 → 无重叠。k-list :dividers-ios=false 时 shuffle 行也无分割线，视觉统一
- **SongsPage shuffle 行再优化**（用户："矮一些，只有一个图标+歌曲总数"）：k-list-item 两行布局（title+subtitle 76px）不满足 → 换 k-button（clear，iOS 蓝）做 sticky 矮行：`sticky top-0 z-10 flex items-center justify-start gap-[10px] w-full h-11 bg-ios-light-surface-1 dark:bg-ios-dark-surface-1 px-4 rounded-none text-[15px]` + shuffle 图标(size-4) + `<span>{{ songs.length }} 首</span>`。放滚动容器内、k-list 前（sticky 吸滚动容器顶）。实测：高 44px（标准 iOS 行高）、top=60 吸顶（滚动后 stuck=true）、文字蓝 rgb(0,122,255)
- **shuffle 矮行 sticky 失效排查**（用户："现在不 sticky 了"）：CDP 实测 sticky 本身生效（stuck=true）但 **z-index 同级**：k-button 自带 z-10 + k-list 根也 z-10（ListClasses base 'z-10 relative'）→ DOM 靠后的 k-list 把吸顶的 shuffle 行盖住 → 视觉"不 sticky"。修复：shuffle 行 `z-10` → `z-20`（压过 k-list）。实测滚动 500px 后：行完整吸顶、图标/文字完整、无歌曲行透出。**教训：sticky 元素必须显式高于后续兄弟的 z-index（k-list z-10），否则被盖**
- **SongsPage shuffle 行文字蓝 → 黑**（用户："用默认的黑色"）：k-button clear 默认 text-primary（蓝）→ colors 覆盖 `:colors="{ textIos: 'text-black dark:text-white' }"`。实测 btn+span 黑色 rgb(0,0,0)，像素：图标 814 黑像素、文字 1247 黑像素、无蓝色残留
- **SongsPage shuffle 按钮点击蓝色高亮去掉**（用户："太丑了"）：k-button clear 默认 `clearBgIos: 'bg-transparent active:bg-primary/15'`（点击蓝 15%）→ colors 覆盖 `clearBgIos: 'bg-transparent active:bg-black/10 dark:active:bg-white/10'`（iOS 标准灰色反馈）。另查证 `touch-ripple-primary` class 虽在，但 useTouchRipple 的 needsTouchRipple 要求 material 主题（iOS 不触发涟漪，死类无碍）
- **SongsPage 跳转 fab 去掉高亮反馈**（用户："直接不加反馈，当前播放歌已有 is-playing 变灰"）：删 highlightedSongId + jumpHighlightTimer 全部逻辑（定义、songItemClass 分支、scrollToCurrentSong 赋值+定时器、onUnmounted 清理）——跳转后不再给行加 bg-primary/10（蓝）高亮，仅靠 is-playing bg-black/5 常驻灰效果。**踩坑**：删函数体块时多留一个 } 导致 TS1128，已修。实测 bgPrimary 行 = 0
- **全部 action sheet 改 one group**（用户："都改成 one group"）：5 处（SongsPage 歌曲操作/加入歌单、PlayerPage 编辑、PlaylistsPage 歌单操作、SourcesPage 添加音源）原来是"内容组 + 取消组"两段（中间有空隙）→ 合并成单个 k-actions-group（label + 操作按钮 + 取消按钮连续），取消按钮加 `bold`（iOS 单组惯例加粗区分）。python 脚本按 state 变量名精确替换 5 处（断言各 1 处）。实测：文本连续"歌曲操作 添加到队列 加入歌单… 取消"、cancelBold ✓、每 sheet 1 group（DOM 中 3 = 3 个常驻 sheet）
- **action sheet 取消按钮 bold 回滚**（用户指路官网 example https://konstaui.com/vue/action-sheet）：官网 one group 示例结构 = k-actions-label + k-actions-button(bold，操作按钮) + ... + 取消按钮（**不加粗**）。之前我自作主张给取消加 bold（iOS 惯例）与官网不符 → 5 处取消去掉 bold（fontWeight 400 验证）。**教训：官网 demo 是验收标准，别套别的惯例**
- **沉浸播放页进度条去白色滑块**（用户："不想要进度条上面白色圆角矩形块"）：Konsta k-range iOS 的 thumbWrap 是白色圆角矩形（w-9.5 h-6 rounded-full bg-white + shadow-ios-thumb），无 thumbClass prop 且类名无 k- 前缀 → 用内联样式特征定位：k-range class 加 `[&_[style*='inset-inline-start']]:hidden`（thumbWrap 独有 insetInlineStart 内联样式）。实测：thumb display:none、input range 保留可拖、轨道从 0% 起（thumbOffset=0 更准）
## Session · 2026-08-10 · 发布 v0.2.7

- 版本：0.2.7 / versionCode 27（package.json / lock / build.gradle 同步；lock 之前停在 0.2.5 顺手修）
- changelog/v0.2.7.md（自 v0.2.6 tag 起 25 提交：k-page/k-list 官方体系、tabbar k-toolbar-pane 玻璃胶囊、
  MiniPlayer 玻璃、shuffle 吸顶系列、action sheet 单组、进度条去滑块等）
- 本地 npm build + assembleDebug 通过 → commit chore(release): v0.2.7 (e614ee0)
- push main + tag v0.2.7；GitHub Actions 3m54s 通过（release workflow 15）
- Release: https://github.com/Happier-X/muses/releases/tag/v0.2.7
  （muses-v0.2.7.apk 8.3MB + muses-v0.2.7-mi.apk 8.3MB）
- **tabbar 背景不半透明修复**（用户指路官网文档 + "背景不是半透明"）：
  - 根因 1：k-toolbar-pane 强制 k-glass = `bg-ios-light-glass`（#ffffffbf **白 75%**）盖住 k-tabbar 官方渐变背景 → 实心白。修复：k-toolbar-pane 加 `!bg-transparent`（露出渐变，真机保留 backdrop blur 毛玻璃）
  - 根因 2：Tailwind v4 渐变默认 `--tw-gradient-position: to top in oklab`（oklab 插值语法），WebView 110 不支持 → 整条 linear-gradient 无效（backgroundImage: none）。修复：k-tabbar 加 `bg-class="[--tw-gradient-position:to_top]"` 强制老语法
  - 实测：bgImage = `linear-gradient(to top, rgb(239,239,244) 0%, rgba(0,0,0,0) 100%)` ✓、pane 背景透明 ✓、blur 模拟器不支持预期（真机有）
  - 官网 tabbar API 要点：k-tabbar > k-toolbar-pane > k-tabbar-link；bgIos 默认渐变背景由 bg 元素渲染（bgClass 可加类）；渐变高度 = calc(safe+16+64+16)
- **tabbar 灰色背景 → 白色半透明渐变**（用户："看到的灰色背景，像布局问题"）：排查发现**页面 k-page 背景本来就是灰 rgb(239,239,244)**（iOS 分组灰），而 k-tabbar 默认渐变起点 ios-light-surface 也是 239,239,244 → tabbar 区域与页面同灰无层次 → 用户感觉"灰色块/布局错乱"。修复：colors 覆盖 `bgIos: 'bg-gradient-to-t from-white to-transparent dark:from-black/60'`（iOS 原生 tabbar = 白玻璃；暗色黑玻璃）。实测：渐变白→透明、tabbar 区白底 246-247 + 图标深色分明、渐变顶部列表文字透出。另确认 bg 元素 96px 比 tabbar 根高 16px 是官网设计（渐变向上延伸渐隐区）
- **平板模式 MiniPlayer 圆角回归**（用户："平板模式底部播放条怎么没圆角了"）：git 定位回归点 = d690d0f（玻璃胶囊改造）引入 `md:left-0 md:right-0 md:bottom-safe-0 md:rounded-none` 把平板模式改成全宽矩形条。修复：去掉 4 个 md: 覆盖，改为 `md:bottom-safe-2`（平板无 tabbar，距底 8px 悬浮）→ 平板与移动端一致悬浮胶囊。CDP 800px 视口实测：borderRadius 99999px、left16/right784/宽 768、距底 8px ✓
- **平板下列表不占满修复**（用户："平板下歌曲列表没有占满屏幕"）：根因 = 9 个页面都带 `md:max-w-[720px] md:mx-auto`（iPad 内容列 720px 惯例）→ 平板（≥768）内容被限 720px 居中，大屏两侧大片留白。修复：全部移除（SongsPage 滚动容器、LibraryDetail×2、PlaylistDetail、Playlists、Settings、Sources、Albums/Artists 网格）。实测 800px：列表 540 占满 main；1280px：列表 1020 占满（侧边栏 260 保留）
- **沉浸播放页平板不占满修复**（用户："沉浸式播放页面平板上不是全屏了"）：根因 = Konsta k-popup 平板模式默认 `md:w-160 md:h-160 md:rounded-4xl`（**640×640 圆角居中面板**，移动端才是 w-screen h-screen 全屏）→ 平板下沉浸播放页变 640px 面板。修复：PlayerPage k-popup 加 `class="!w-screen !h-screen !rounded-none"`（Tailwind v4 important 前缀类 `.\!w-screen`，覆盖 md 变体）。CSS 验证：`.\!w-screen{width:100vw!important}` 已生成；注 v4 important 类名感叹号在**开头**（`.\!w-screen` 非 `w-screen\!`）
- **手势返回直接退出应用修复**（用户："专辑下一级手势返回退出应用"）：
  - 根因 1：targetSdk 36（Android 16）+ Manifest 未 opt-in `android:enableOnBackInvokedCallback` → 预测性返回手势不走 Capacitor OnBackPressedCallback → 系统直接 finish。修复：Manifest `<application android:enableOnBackInvokedCallback="true">`
  - 根因 2：App.vue 已有 backButton 监听但 overlay 未开时**直接 minimizeApp（退后台）**，无路由返回。修复：`router.options.history.state.back !== null` 时 `router.back()`，否则 minimizeApp
  - 旁证：原生 WebView.canGoBack() 对 SPA pushState 判定不可靠（CDP nav history 5 条但原生 canGoBack false）→ 用 Vue Router history state 判断更可靠
  - 实测：详情页→返回→专辑列表(albums)→返回→歌曲页(songs)→返回→桌面(进程存活 29735 不销毁)
## Session · 2026-08-10 · 发布 v0.2.8

- 版本：0.2.8 / versionCode 28（package.json / lock / build.gradle 同步）
- changelog/v0.2.8.md（自 v0.2.7 tag 起 7 提交：手势返回修复、平板列表占满/播放页全屏/MiniPlayer 圆角、tabbar 半透明白渐变）
- 本地 npm build + assembleDebug 通过 → commit chore(release): v0.2.8 (d7abc90)
- push main + tag v0.2.8；GitHub Actions 通过
- Release: https://github.com/Happier-X/muses/releases/tag/v0.2.8
  （muses-v0.2.8.apk 8.3MB + muses-v0.2.8-mi.apk 8.3MB）
- **一级页面返回=退出应用**（用户："一级页面手势返回应该直接退出应用"）：之前 router.back() 对 tab 间切换也生效（tab 切换也是 push → 一级页返回回上一个 tab）。修复：按路径深度区分——`route.path.split('/').filter(Boolean).length <= 2`（/tabs/songs 等一级页）→ minimizeApp 退桌面；二级页（/tabs/library/... 详情）→ router.back()。实测：歌曲页/专辑列表返回→桌面（lawnchair，进程保留播放）；详情返回→专辑列表 ✓
- **tabbar 灰底根因修复**（用户分析准确："列表只占除 tabbar 外的部分，玻璃透出灰色底"）：
  - 根因：8 处 k-page `!bottom-safe-24`（absolute bottom 缩到 tabbar 上方）+ TabsPage main `pb-safe-24` → 列表高度止于 tabbar 之上 → tabbar 玻璃背后永远是 k-page 灰底（MiniPlayer 在列表区域内所以透出内容正常）
  - 修复：7 处 k-page（6 view + MPage.vue）`!bottom-safe-24 md:!bottom-0` → `!bottom-0`；main 去 `pb-safe-24`；Settings m-content 加 pb-[64px]、Albums/Artists grid p-[16px] → px/pt/pb 拆分（pb-[64px]）；已有 pb 的页（Songs/Library/Playlist 滚动容器 64px、Playlists 80px、Sources 88px）保留
  - 实测：滚动容器 bottom=616（视口底，延伸到 tabbar 背后）；tabbar 区像素灰色占比 0%（透出列表白底内容）
- **滚动到底最后一行被挡问题**（用户："注意列表滚动到底是否被挡住"）：
  - 计算：滚动到底行底 = 视口底(616) - 内容pb；原 pb-64 → 行底 552，被 tabbar(536+) 盖 16px；播放中 MiniPlayer 顶 456 → 需行底 ≤ 456
  - 修复：8 处滚动内容底部 padding 统一 `pb-40`（160px，行底=456=MiniPlayer 顶）+ 平板 `md:pb-safe-24`（96px，盖过平板 MiniPlayer 72px 遮挡）；Songs/Library/Playlist 滚动容器、Playlists 内容、Sources 滚动容器、Settings m-content、Albums/Artists 网格
  - 实测：滚动到底 lastRowBottom=456 < tabbarTop=536 ✓ 完整可见；像素确认最后一行在 456 上方
- **tabbar 灰带修复升级：动态 contentInset 对齐 iOS**（用户："苹果上也是这么处理的吗，列表偏白最后一块灰不优雅"）：
  - 查证：iOS 用 contentInset（=tabbar 高 + miniplayer 高），滚动到底最后一行恰好停在上层悬浮元素上缘，无大留白；固定 pb-160 无播放时多 80px 灰带
  - 修复：CSS 变量方案——tailwind.css 定义 `:root{--content-pb:80px;--content-pb-md:0px}` + `html.muses-mini-visible{--content-pb:160px;--content-pb-md:96px}`；MiniPlayer.vue watch currentSong → 切 html.muses-mini-visible；8 处滚动容器 pb 改 `pb-[var(--content-pb)] md:pb-[var(--content-pb-md)]`
  - 验证：无播放 pb=80（行底 536=tabbar 顶，无灰带）；播放中 pb=160（行底 456=MiniPlayer 顶）
  - 测试环境恢复：模拟器无音频文件 → 生成 5 个 wav push /sdcard/Documents → SAF picker（adb 导航：Download 被 Android15 拒，Documents 通过）→ 扫描 5 首入库
- **tabbar 6→3 重组**（用户："6 个 tab 太多，改成首页/音乐/设置"）：
  - 方案（用户确认）：首页=最近播放（后续按需添加）；音乐=分段控制器（全部|专辑|艺术家|歌单，iOS 资料库风格）；专辑/艺术家/歌单并入音乐段；音源并入设置
  - 路由：/tabs/home（新 HomePage）、/tabs/music（新 MusicPage）；旧 songs/albums/artists/playlists 重定向 /tabs/music；/ 与 /tabs 重定向 home；playlists/:id、library/:kind/:name、sources、settings 保留
  - MusicPage：k-segmented 分段 + 4 子页 v-show 切换；段状态存 module（musicSegment.ts），详情返回显示 setMusicSegment 恢复段（专辑详情返回→专辑段）
  - HomePage：最近播放列表（recent.ts，playSong 时记录去重置顶上限 50；点击按 songId 从库解析播放）
  - SourcesPage 变二级页（navbar 加返回）；SettingsPage 加「音乐库>音源管理」入口
  - App.vue isTopLevelPage 改白名单（home/music/settings）；LibraryDetail/PlaylistDetail goBack 的 replace 目标改为 /tabs/music + setMusicSegment
  - 实测：3 tab ✓ 分段切换（navbar 标题随段）✓ 播放→最近播放→首页 ✓ 详情返回段恢复 ✓ 音源返回设置 ✓ 一级返回退桌面 ✓
  - 坑：installDebug 覆盖安装清了 localStorage（WebView 未 flush）→ 重扫音源恢复；wav 测试音频播放正常（0.8s 播放中→2s 播完消失）
- **音乐页标题改为「音乐」+ subnavbar 分段**（用户："音乐页面标题就叫音乐，下面放 segment"）：
  - MusicPage 加 k-navbar 标题「音乐」+ 分段条移入 navbar subnavbar slot；子页面 4 个去各自 navbar（Songs 删标题+无功能搜索按钮、Albums/Artists m-page 改 k-page 直写无 navbar、Playlists 删 navbar）
  - 功能按钮上移：新建歌单（歌单段）→ MusicPage navbar right，ref 调 PlaylistsPage defineExpose openCreateAlert
  - 验证：navTitle=音乐+4 段、全页 1 个 navbar、新建弹窗正常
- **音乐页分段改 Konsta 官方默认样式**（用户："太丑了，用 konsta 默认 iOS 样式"）：
  - 根因 1：k-segmented 无 strong → iOS 默认态 = 灰底 + active 蓝字（老式 tab 观感）；官方 iOS 资料库 = strong+rounded（白色圆角滑块滑动）
  - 根因 2：navbar subnavbar 容器是 flex（'relative flex items-center h-14 pl-safe-4 pr-safe-4'）→ 子 div 无 flex 撑开 → 分段条收缩到 219px（应占满 328）
  - 修复：k-segmented 加 `strong rounded`；subnavbar 内容 div 改 `flex-1 min-w-0`（去掉自定义 px-4 pt-2 pb-3，subnavbar 自带 pl/pr-safe-4 + h-14 垂直居中）
  - 实测：分段条 328/360 占满、滑块 bg-white、蓝字消失（之前 0 蓝像素）
- **tabbar 改为 歌曲/分类/音源/设置 4 项**（用户）：
  - 路由：/tabs/songs（歌曲，恢复独立路由+navbar）、/tabs/categories（新 CategoriesPage，标题"分类"+ 分段 专辑|艺术家|歌单）、/tabs/sources（音源恢复独立 tab，去返回按钮）、/tabs/settings；删 home/music 路由重定向（home/music→songs，albums/artists/playlists→categories）
  - 删 MusicPage/HomePage/musicSegment.ts；新建 CategoriesPage/categoriesSegment.ts（段类型 albums|artists|playlists）；LibraryDetail/PlaylistDetail goBack replace 目标 /tabs/music→/tabs/categories（原 /tabs/music 重定向 songs 导致返回跑错页）
  - SettingsPage 去音源管理入口（音源已是 tab）；App.vue 一级白名单 4 tab
  - 实测：4 tab ✓ 分类分段 328 占满 ✓ 详情返回分类专辑段 ✓ 一级返回退桌面 ✓
- **歌曲列表项恢复原样 + 滚动避开 tabbar/mini**（用户"每个列表项都让你改坏了，要一开始的那样，滚动到最后能避开 tabbar 和播放条"）：
  - 教训：别动列表项本体布局（行高 88/flex 居中/pb 都算改坏）——虚拟列表 estimateSize/行容器/item 高度必须一致 72 无缝
  - 真正需求 = 动态 contentInset（4a68df5 已实现）：播放中滚底行底=456 贴 MiniPlayer 顶、无播放完整可见；本次回滚 6a6ee2f/c3c7d07 的行布局改动
- **修复 WebDAV 添加表单输入消失**（任务 08-11-fix-webdav-add，用户"填写了表单以后填写的内容又消失了"，时机=切字段时）：
  - 根因：k-list-input 非受控（Konsta 源码 input 不绑 :value，值存 DOM；value prop 仅浮动 label 用）；tanstack vue-form 重渲染与非受控 DOM 竞态 → blur/切字段偶发丢值（CDP 实测复现）
  - 修复：8 字段（WebDAV 添加 3 + 编辑音源 5）改 k-list-input #input 槽自定义受控 input（:value=field.state.value + @input=field.handleChange + @blur=field.handleBlur）；删废弃 onFormInput
  - trellis-check 发现并修：自定义 input 漏 Konsta 默认类 h-10 + placeholder 色（视觉回归，16px 行高）→ 8 处补齐
  - spec：forms.md 新增 §0 k-list-input 必须 #input 槽受控（含完整样式类清单）
  - 实测：三轮输入+切字段全保留、提交失败后值保留、label 正常
  - 遗留（非本任务）：PlayerPage 编辑歌曲信息 5 字段同模式非受控（高风险，建议后续迁移）；PlaylistsPage k-button 未导入 lint 存量错
- **PlayerPage 编辑歌曲信息表单受控化**（任务 08-11-fix-player-edit-form，用户"另开任务修复"）：
  - 同 SourcesPage 根因：k-list-input 非受控 + TanStack Form 竞态丢值；按 forms.md §0 改 #input 槽受控
  - 改 5 字段（title/artist/album/replayGainDb + lyrics textarea py-2 resize-none）；删 onFormInput/onEditLyricsTextareaInput；onEditLyricsInput 保留（format='lrc' 逻辑）
  - 注意：fixed/transform 浮层 offsetParent 恒 null——CDP 判断浮层开关须用 class（-translate-y-full=closed / translate-y-0=opened）而非 offsetParent
  - 验证：CDP 输入→blur→切字段 title/artist/album 全保留；trellis-check 通过（lint 仅 PlaylistsPage 存量错）；build 通过
- **Konsta navbar 玻璃效果修复**（任务 08-11-fix-navbar-glass，用户"补一个"留档）：
  - 用户反馈 navbar 与官网（konstaui.com/vue/navbar）不一致、无玻璃效果——CDP 实测两层背景均失效
  - 根因 1：Tailwind v4 渐变用 `to bottom in oklab`，WebView<111 不解析 → bg 层 background-image:none（navbar 完全透明）
  - 根因 2：backdrop-blur-[2px] 任意值类未生成（mask-* 正常）→ blur 失效
  - 修复：tailwind.css 补 `.backdrop-blur-\[2px\]`（含 -webkit 前缀）+ `.k-navbar > .bg-gradient-to-b { --tw-gradient-position: to bottom }`（tabbar to_top 先例）
  - 关键经验：判定浮层/渐变问题时用 CDP getComputedStyle 看实际值（如背景消失却无报错）；Tailwind v4 的 `in oklab` 插值语法要求 WebView>=111
  - 验证：bg 渐变 linear-gradient(239,239,244→transparent) + blur(2px) + mask 生效；附截图像素采样对比
- **图标按钮规范 k-button+clear**（任务 08-11-fix-icon-button-clear，用户"可以"留档）：
  - 用户指出"图标按钮应该是 k-button 配合 clear"——查 Konsta ButtonClasses 确认 5 种 style：fill(默认实心)/outline/clear(透明底+文字色)/tonal/segmentedStrong
  - 纯图标按钮必须 clear，否则 fill 蓝底；带文字的主操作按钮保持 fill 合理
  - 全项目扫描修 2 处漏网：PlaylistDetailPage 移除按钮、PlaylistsPage 更多操作按钮（small rounded 无 clear）——commit 2013a30
- **发布 v0.2.9**（chore(release)，版本号 package.json/lock 0.2.9 + build.gradle versionCode 29）：
  - 自 v0.2.8 累计 30 提交：tabbar 4 项重组（歌曲/音乐库/音源/设置）、列表回归官方（去卡片/分割线/72px 无缝+contentInset）、WebDAV+PlayerPage 表单受控化、navbar 玻璃修复、图标按钮 clear 规范
  - changelog/v0.2.9.md 新文件
- **toast 文字白色不可见修复**（任务 08-11-fix-toast-text，用户"新任务"）：
  - 根因：项目 body { color: var(--color-ios-light-surface-1) } = #fff 白色；k-toast iOS textIos 为空 → 文字继承 body 白 → 白玻璃+白字
  - 修复：.k-toast 显式文字色（浅黑 dark 白）+ 顺带修 backdrop-blur-lg 未生效（WebView 对 Tailwind var() 链规则 backdrop-filter 忽略，直接给值 blur(16px) 覆盖；MiniPlayer/k-glass 全受益）
  - 验证：CDP computed——toast 浅色 rgb(0,0,0)/深色 rgb(255,255,255)、glass+mini blur(16px)
  - 经验：Konsta iOS 部分组件（toast textIos）颜色为空会继承 body 默认色；项目 body color=白是历史设定，组件需显式设色
- **MiniPlayer 改用 k-glass 组件**（用户"可以用konsta ui的glass组件呀"）：
  - 外层手写 div（bg-ios-light-glass+shadow+backdrop-blur-lg 6 个类）→ k-glass 组件（component/class/事件透传，默认即官方玻璃配方 bg+shadow+blur+dark 反色；需自留 rounded-full 圆角）
  - 验证：k-glass 渲染类/白 0.75 玻璃/blur16px/fixed/role=button/点击开播放页全正常；k-glass 默认 touch-none 不影响点击
  - 注意：querySelector('.k-glass') 会匹配到 k-actions 菜单里的玻璃，定位组件需用 aria-label 或业务类

## 08-13 navbar 覆盖式布局修复（官方玻璃模糊）
**用户问题**：顶部 navbar 没有官网 demo 的"内容滚到下方时半透明模糊"效果，猜测"顶上没有列表"。
**根因链**（用户猜测成立）：
1. k-navbar 在文档流（滚动容器外），滚动容器从 navbar 底部才开始 → 内容永远不经过 navbar 后方（y0-60 恒空）→ blur/渐变无内容可透
2. tabbar 是 fixed 覆盖在列表上 → 内容从其后经过 → 有玻璃感（对比差异来源）
3. 附带发现：SongsPage k-page 的 `!h-auto`（important）覆盖 `.m-page{height:100%}` → k-page 被内容撑高（20290px）→ 列表 h-full 失效不能滚（改覆盖式后暴露）
**修复**（SongsPage 单页）：
- k-navbar 包 `root-navbar-wrap absolute top-0 z-20`（覆盖式，k-page 加 relative）
- 滚动容器（listParentRef）从屏幕顶开始（无 pt），内容滚动时从 navbar 后方经过 → 官方玻璃生效
- 移除 `!h-auto`（k-page 锁定视口高）
- shuffle 行 `sticky top-[calc(max(16px,var(--k-safe-area-top))+44px)]` z-10（吸 navbar 正下方，保持可点，不再挡内容）
- empty 分支 pt 同 calc 避让 navbar
**关键认知**：① 结构上"内容经过 navbar 后"才是官方玻璃的前提（bg 渐变 239→transparent + blur2px + mask 上半实色，透出区 y38-76）；② 模拟器程序化滚动不产生合成帧 → backdrop 采样不更新 → 截图差分恒 0（工具限制，真实滚动正常）；③ CDP Input.dispatchTouchEvent 在此 WebView 无效（模拟器通道限制）；④ sticky 元素自然位置已在吸住线上方时立即吸住（无经过过程）
**验证**：kpageH 616 锁定 ✓ shuffle 吸 y60 ✓ 行位置随滚动移动 ✓
**提交**：a7a3d7a

## 08-11 navbar 覆盖式布局统一推广（4 页）
**背景**：歌曲页覆盖式布局确认后，用户选择统一推广（任务 08-11-unify-navbar-overlay，d6d0080）。
**改造**（LibraryDetail/PlaylistDetail/Settings/Sources 4 页，同 SongsPage 模式）：
- k-page 加 relative + 移除 !h-auto（否则 k-page 被内容撑高不能滚）
- navbar 包 root-navbar-wrap absolute top-0 z-20
- 无 sticky 行页面：滚动容器/empty 分支加 `pt-[calc(max(16px,var(--k-safe-area-top))+44px)]`（内容初始在 navbar 下、滚动经过其后方）
- LibraryDetail：随机播放条从文档流移入列表容器 sticky（吸 navbar 下 z-10，empty 时不显示）
- SourcesPage 原 8px 顶距保留：pt calc + 8px
**不改**：CategoriesPage（subnavbar 分段需常驻吸顶，覆盖式收益低风险高）
**验证**（CDP 真实点击导航）：4 页 wrap true + kpageH 616 + pt 生效 + 专辑详情 shuffle 吸顶 64 ✓ 返回/分段切换无回归 ✓ 歌曲页回归 ✓
**经验**：① vue-router history 模式外部 popstate 导航不可靠（破坏 RouterView 状态），用 location.href 整页导航（会断 CDP 需重连）或真实 UI 点击；② 子路由完整路径带 /tabs 前缀（/tabs/playlists/:id）；③ tabbar 链接是 button（component="button"）非 a；④ QueuePage 全局挂载在 App.vue（.k-page 查询会先命中它，需按 top 过滤）

## 08-12 随机播放吸顶条改回 k-glass 默认玻璃（任务 08-12-fix-shuffle-glass-default，920e99a）
**用户反馈**：navbar 有玻璃模糊，随机播放吸顶条没有（"下面的随机播放这一条不会，我也想要这样的效果"）。
**根因链**（CDP 像素分析）：
1. navbar = Konsta 双层：blur 层（blur2px + mask-b 渐显，无背景）+ bg 渐变层（无 blur 无 mask）→ 内容滚过时上半朦胧下半灰幕
2. 吸顶条 = 单层 k-glass + 覆盖类（!bg-transparent + !backdrop-blur-[2px] + mask-b + 渐变）：mask 在单层上同时裁掉 blur 和渐变的下半 → 下半内容清晰透出（方差 62-68 vs navbar 24）、上半实心灰板 → "没模糊"
3. 用户问"直接用 k-glass 就行吗"→ k-glass 默认（白0.75+blur16+阴影）确实自带完整玻璃，是覆盖类把它改没了
**修复**（用户选定方案 A）：裸用 k-glass（去全部覆盖类），SongsPage + LibraryDetailPage 两处；清理 tailwind.css 死代码（.\!backdrop-blur-[2px] 手写规则、.shuffle-glass > .bg-gradient-to-b）
**验证**：Edge 真实页面（注入 30 首种子歌曲 + reload）blur16 生效——滚动内容被模糊为纯白玻璃（y60-75 方差 4.7 vs 列表区 49）；模拟器 computed blur(16px)+白0.75 ✓；构建通过
**经验**：① 运行时改 class/注入 DOM 会被 Vue 重置（无法验证变体，必须改代码构建）；② 模拟器程序化滚动 backdrop 采样不更新（截图不可靠），桌面 Edge headless 也会滞后——只有"位置变化后首帧"可靠；③ 采样玻璃区域须避开按钮文字（文字方差高会误判为"内容透出"）；④ 判断玻璃层是否渲染可用"背景改红色"决定性测试；⑤ cap sync 未跑时 APK 里是旧 assets（改代码后必须 npx cap sync android 再 assembleDebug）
**spec**：component-guidelines.md 增 k-glass 条目（默认即完整玻璃；要 navbar 灰玻璃须双层结构，禁单层叠 mask）

## 2026-08-12 任务 08-12-unify-glass-effects：全 app 玻璃统一

**背景**：用户追问 k-glass 与 navbar/tabbar 玻璃差异 → 官方源码确认两套配方（k-glass 白0.75+blur16 单层 vs 系统栏灰渐变+blur2px+mask 双层）→ 用户拍板全统一（含底部播放条）。

**方案**：navbar/tabbar 官方不动；随机播放条（Songs/LibraryDetail）+ MiniPlayer 改官方灰玻璃双层（blur 层 blur2px + mask 渐显 + bg 渐变层）。MiniPlayer 胶囊无 mask（悬浮不贴边），rounded-full 两层都要。

**关键坑（WebView 110）**：Tailwind v4 渐变 `--tw-gradient-position: to bottom in oklab`（Chrome 111+ 插值语法）→ 老 WebView 解析失败 → bg 层 background-image: none！navbar 正常是因为早有手写覆盖规则。**修复：选择器必须是后代匹配 `.shuffle-glass .bg-gradient-to-b`**（不是 `>` 直接子级——双层结构后 bg 层在嵌套容器内，此前 `>` 选择器不匹配导致模拟器上渐变失效）。教训：Edge（Chrome 13x）验证通过 ≠ 模拟器 WebView 110 通过，渐变/backdrop 类必须实测 APK。

**验证**：Edge 像素（shuffle 上半遮/下半渐显随滚动切换、MiniPlayer 滚动内容模糊透出 40→14.6 方差）；模拟器 computed（shuffle/MiniPlayer blur2px + 灰渐变 + mask 全绿，与 navbar 同配方）；构建/lint 通过。

**顺手修**：PlaylistsPage.vue 存量 lint 错误（用了 k-button 未导入）。

**spec 更新**：component-guidelines k-glass 条目改为"全 app 统一灰玻璃双层结构"规范 + WebView<111 渐变选择器后代匹配要点。

## 2026-08-12 吸顶条"透明"修复（ca95811 后用户反馈）

**现象**：统一灰玻璃后吸顶条看起来透明。**根因**：mask-b 渐显 + 直渐变在 44px 矮条上失效——mask 裁掉下半 blur（内容锐利直透，方差 63 > 列表 44）、渐变 44px 内瞬间淡出（navbar 靠 76px 高度撑住灰感）。

**修复**：blur 层去 mask（全高 blur2px）；bg 层加 via 三段渐变 `from-ios-light-surface via-[rgba(239,239,244,0.4)] to-transparent`。**关键坑**：`via-ios-light-surface/40` 透明度修饰符生成 color-mix（Chrome 111+），WebView 110 失效 → 必须用任意值 `via-[rgba(239,239,244,0.4)]`。验证：同位置截图对比，上半方差 30→0.6（实灰）、下半 63→40（磨砂）；computed 渐变三段正常。

**经验**：官方 navbar/tabbar 的渐显配方不能无脑搬到矮条；玻璃条"实体感"由渐变 alpha 保持段提供（via 40% 是关键）。

## 08-14 歌曲页行内细节尺寸对齐椒盐（任务 08-14-songs-page-row-scale-fix）
**用户反馈**：歌曲页跟椒盐差距大——行内圆形按钮/更多按钮太小。
**依据**：SaltUI 源码（.tmp/saltui/，Apache-2.0）+ 椒盐 APK 模拟器实测（uiautomator bounds + PIL 像素 + CDP DOM），交叉验证。
**修复**（SongsPage.vue + theme/index.scss，逐项提交）：
- D1 body margin 0（全局 bug：默认 8px 使页面右移 8px 窄 16px）
- D2 圆按钮交互区 14→44x48px，::before 视觉圆 14px 居中 #ECECEC，图标 stroke-width 3 + #949fab（椒盐蓝灰 148,159,171）
- D3 ⋮ 按钮 32x32 细线 lucide → 36x48px 实心三点（点 3.5px、gap 2px、总高 ~14px）
- D4 圆/⋮ gap 0 紧挨（实测 x264-308 / x308-344），封面 x16
- D5 标题-副文字间距 1→2px（SaltUI Item 源码 2dp）
**验证**：lint/vue-tsc/build 全绿；CDP 实测 bodyMargin=0、app x0 w360、round 44x48+视觉圆14、more 36x48+dots h15、gap0、间距2px；点击 round→「已加入队列」toast ✓、more→m-actions 菜单 ✓（注意类名前缀是 m- 不是 k-）；专辑页导航无回归 ✓
**提交**：ae1c846（主体）+ 2798e1d（⋮ 颜色收尾）
**spec**：component-guidelines.md Salt 尺寸契约补 body margin:0 + 行内按钮契约（44x48/36x48/实心三点/紧挨）

## 08-13 歌曲页椒盐深仿任务收尾验收（任务 08-13-songs-page-salt-alignment）
**实现**：b268120/d260ef7 主体（工具条/排序/多选/索引条/搜索/行内布局）+ 多轮 fix（行高72dp/navbar/无分割线/索引条尺寸/工具条按钮/行内按钮复刻），实现早已完成，本次补全量验收：
- AC2 工具条 ✓（多选/排序按钮）；AC3 排序菜单 ✓（自定义/标题/专辑(音轨)/大小/文件夹(标题)/文件名/艺术家(专辑)/年份…13 项）
- AC4 多选 ✓（进入后左按钮变「全选」）；AC5 索引条 ✓（标题排序显示 0+A-Z+# 28 项，自定义排序隐藏）
- AC6 行内布局 ✓（08-14 CDP 实测覆盖）；AC7 搜索 ✓（placeholder「在 465 首歌曲中搜索」、KOKIA 过滤剩 1 行、取消恢复工具条）
- AC8 零回归 ✓（加队列 toast / 更多菜单实测正常）
**经验**：验收探针类名注意前缀——本项目自研组件类是 `m-*`（m-actions/m-toast），不是 Konsta 的 `k-*`；搜索输入用原生 value setter + input 事件才能触发 v-model。


## Session 96: 歌曲页 1:1 复刻椒盐 + 全局 MIconButton 图标按钮

**Date**: 2026-08-14
**Task**: 歌曲页 1:1 复刻椒盐 + 全局 MIconButton 图标按钮
**Branch**: `main`

### Summary

基于用户椒盐截图 1:1 复刻歌曲页：移除行内圆按钮/HQ 标识、MiniPlayer 胶囊液态玻璃+fill 实心图标、FAB 玻璃定位按钮、navbar/工具条对齐、MActions Teleport 修复 fixed 定位、新增 MIconButton 统一图标按钮涟漪反馈、文字颜色改中性深灰

### Git Commits

| Hash | Message |
|------|---------|
| `ad85bd2` | (see git log) |
| `f6daf14` | (see git log) |
| `9dbaddf` | (see git log) |
| `122b001` | (see git log) |
| `6ce9058` | (see git log) |
| `bb83744` | (see git log) |
| `50e5dd4` | (see git log) |
| `2e74068` | (see git log) |
| `d7633a9` | (see git log) |
| `5e79433` | (see git log) |
| `e138d6d` | (see git log) |
| `2aabb65` | (see git log) |
| `5e33aaf` | (see git log) |
| `91222ad` | (see git log) |
| `25dd0a6` | (see git log) |
| `30bc7b1` | (see git log) |
| `3aeb3e5` | (see git log) |
| `9256533` | (see git log) |
| `194f851` | (see git log) |

### Status

[OK] **Completed**


## Session 97: 沉浸式播放页 1:1 复刻椒盐（AMLL 背景 + 五行歌词窗口）

**Date**: 2026-08-15
**Task**: 沉浸式播放页 1:1 复刻椒盐（AMLL 背景 + 五行歌词窗口）
**Branch**: `main`

### Summary

复刻椒盐沉浸式播放页：移除顶部导航、歌名+歌手左上、大封面正方形、五行歌词窗口（AMLL 式连续滚动+缩放淡化动画）、三行歌词上下文、播放按钮统一尺寸无圆底、MPopup transparent 下滑露出底下、修复 TDZ/图标碎裂/页面跳动等；背景继续用 AMLL MeshGradientRenderer

### Git Commits

| Hash | Message |
|------|---------|
| `1679dd4` | (see git log) |
| `095d668` | (see git log) |
| `de3e2d3` | (see git log) |
| `eb6c646` | (see git log) |
| `45ea951` | (see git log) |
| `d226210` | (see git log) |
| `6a3d2ef` | (see git log) |
| `839a489` | (see git log) |
| `619a052` | (see git log) |
| `fadf96d` | (see git log) |
| `ece164d` | (see git log) |
| `5469eb0` | (see git log) |
| `da565b0` | (see git log) |
| `929d6b7` | (see git log) |
| `7c619c4` | (see git log) |
| `610d60d` | (see git log) |
| `e85d9d0` | (see git log) |
| `095a56f` | (see git log) |
| `2cfc3e3` | (see git log) |
| `8119ce6` | (see git log) |
| `e87f41a` | (see git log) |
| `9bed8ae` | (see git log) |
| `42dbaed` | (see git log) |
| `5caadbc` | (see git log) |
| `3ad8b5d` | (see git log) |
| `1715d46` | (see git log) |

### Status

[OK] **Completed**


## Session 98: fix(ui): 列表底部被 MiniPlayer 胶囊遮挡——内容止位 64px → 72px

**Date**: 2026-08-16
**Task**: fix(ui): 列表底部被 MiniPlayer 胶囊遮挡——内容止位 64px → 72px
**Branch**: `main`

### Summary

用户反馈真机上歌曲/专辑/艺术家页列表滚到底时最后一项被底部 MiniPlayer 遮挡。根因：08-14 MiniPlayer 胶囊化后 bottom=safe-area+8px（悬浮空隙），实际占 72px+safe-area，而 --m-content-pb 仍按 64px 止位，漏算 8px。修复：theme/index.scss 4 处 content-pb token 64px→72px；SongsPage 多选条/字母索引条 bottom 同步 72px（同源被盖）；MiniPlayer 注释与 spec 底部几何契约同步。lint+build 通过，真机待验证。

### Git Commits

| Hash | Message |
|------|---------|
| `7448833` | (see git log) |

### Status

[OK] **Completed**

## Session 99: fix(ui): navbar 与工具条对齐椒盐——液态玻璃改实心表面

**Date**: 2026-08-16
**Task**: 08-16-navbar-toolbar-solid-surface
**Branch**: `main`

### Summary

用户要求顶部 navbar 和工具条与椒盐音乐一致：灰的、跟列表同色（此前是半透明白液态玻璃 + blur(20px) + 内高光，滚动时透出列表糊影）。根因：08-15 的"液态玻璃"决策（半透明 alpha + 顶部内高光承担玻璃观感）与椒盐实际顶栏（纯色 subBackground，默认关 Liquid Glass）不符，且 MuMu WebView 上 sticky 定位 navbar 的 blur 会透出滚动内容。修复：MNavbar 背景 `--m-glass-bg` → `--m-surface-1`（浅 #f9f9f9 / 深 #262626，与 MList 列表同色），去 blur 与内高光；SongsPage `:deep(.m-navbar)` 与深色 `:global(.dark .songs-page .m-navbar)` 覆盖同步实心化；深色规则只留 background 赋值。边界：FAB/MiniPlayer 保持液态玻璃、`--m-glass-bg` 变量不动、MTabbar 本就是实心；`--transparent` 变体（PlayerPage）无回归。lint + vue-tsc 通过。spec(component-guidelines) 更新：常驻导航表面条目 MNavbar 回归干净表面，液态玻璃契约范围收窄至 MiniPlayer/FAB。

### Git Commits

| Hash | Message |
|------|---------|
| `ff75700` | fix(ui): navbar 与工具条对齐椒盐——液态玻璃改实心表面（与列表同色） |

## Session 100: fix(ui): navbar 背景修正为列表底色（--m-surface）

**Date**: 2026-08-16
**Task**: 08-16-navbar-bg-match-list-surface
**Branch**: `main`

### Summary

用户反馈上轮实心化后"navbar 和工具条还不是灰色"。排查：源码已改但用户看的是旧产物——dist 22:31 旧构建 + android assets 停留在 8月15 20:52（比源码修复还旧）。cap sync + assembleDebug + 安装 MuMu 后截图采样发现**真正根因**：navbar 用了 `--m-surface-1`（#f9f9f9，MList 卡片色），而 SongsPage 自建虚拟列表（非 MList）底色直接透出 body 的 `--m-surface`（#f3f3f3）——navbar 偏白，肉眼色差。修正：MNavbar 默认/深色、SongsPage navbar 覆盖/深色全部 `--m-surface-1` → `--m-surface`。MuMu 截图像素验证：navbar/工具条/列表区域全部 (243,243,243) 一致（状态栏区域除外）。流程验证闭环：改源码 → build → cap sync → assembleDebug → 安装 → screencap 像素采样。教训：与「下面列表」对齐时须以列表区域实际渲染色为准（列表组件与页面底可能不同 token）。spec 常驻导航表面契约已修正并记录。

### Git Commits

| Hash | Message |
|------|---------|
| `(see git log)` | fix(ui): navbar 背景修正为列表底色 --m-surface（#f3f3f3） |

## Session 101: feat(ui): navbar 灰底磨砂玻璃（三轮迭代定案）

**Date**: 2026-08-16
**Task**: 08-16-navbar-gray-frosted-glass
**Branch**: `main`

### Summary

用户三连反馈收敛为完整期望：顶栏 = 灰（与列表同色）+ 滚动磨砂（椒盐 Liquid Glass）。首轮实心 surface-1（偏白）、二轮实心 --m-surface（灰但对，去掉 blur 失去磨砂），三轮将两者结合：新增 `--m-navbar-glass-bg`（浅 rgba(243,243,243,0.8) / 深 rgba(32,32,32,0.8)，基底 = --m-surface 灰）与 --m-glass-bg（白玻璃，MiniPlayer/FAB 专属）分离；MNavbar + SongsPage 覆盖恢复 blur(20px) + 顶部内高光（浅 0.65 / 深 0.1）。验证闭环：cap sync + assembleDebug + 安装 MuMu → 滚动列表截图采样（navbar 区域出现 234..242 内容透出渐变 + 深色文字模糊痕迹 = 磨砂恢复）→ 滚回顶部采样（navbar/工具条/列表全 #f3f3f3 一致）。alpha=0.8 静止叠于 #f3f3f3 上视觉恒等列表色，滚动时内容透磨砂。spec 常驻导航表面契约记录最终形态与三轮教训。另发现 android assets 停留在 8月15（cap sync 后已最新）。

### Git Commits

| Hash | Message |
|------|---------|
| `(see git log)` | feat(ui): navbar 灰底磨砂玻璃——列表灰 0.8 alpha + blur 恢复滚动磨砂 |

## Session 102: feat(ui): navbar 去底部横线，全部页面统一

**Date**: 2026-08-16
**Task**: 08-16-navbar-no-bottom-border
**Branch**: `main`

### Summary

用户要求专辑/艺术家/歌单/音源/设置页 navbar 去掉底部横线，与歌曲页一致。MNavbar 默认样式删除 border-bottom；SongsPage 冗余的 border-bottom: none 覆盖清理（默认已无）；--transparent 变体清理冗余 border-bottom-color。验证：MuMu 安装后歌曲页 navbar 底部 y370-418 全 243 无突变（无回归）；导航抽屉切到专辑页，navbar 区 y100-160 全 243 无 hairline 突变，y166 起列表内容。全部页面共用 MNavbar 默认样式（此前 grep 确认仅 SongsPage 有 :deep(.m-navbar) 覆盖），统一生效。spec 常驻导航表面契约补充任务记录。

### Git Commits

| Hash | Message |
|------|---------|
| `(see git log)` | feat(ui): navbar 去底部横线，全部页面统一（对齐歌曲页） |

## Session 103: fix(player): 沉浸播放页下滑回弹卡住

**Date**: 2026-08-16
**Task**: 08-16-fix-player-drag-stuck
**Branch**: `main`

### Summary

用户反馈：真机沉浸式播放页，只下滑一点距离松手后页面卡在半屏不回弹。根因三条：① 回弹只依赖 watch(dragOffsetY) 单次机会，回弹动画(220ms)进行中再次松手被 `if (reboundControls) return` 早退吞掉；且 motion stop() = commitStyles + cancel 会把中间值写进 style 且不触发 onComplete，残留无人纠正。② 真机触摸序列被系统打断（通知栏下拉/多指/低端机丢事件）时 touchend/touchcancel 不至，dragOffsetY 残留。③ 回弹 animate 作用在外层 overlay，拖拽 :style 绑定在内层 drag-layer，元素分离导致松手瞬间内层先归 0、外层再动画（抖动 + 隐晦链路）。

修复（仅 PlayerPage.vue，+77/-25）：ref="dragLayerRef" 从外层移到内层统一动画元素；新增 startRebound(from) 显式回弹（stopRebound → 锁回起点 translateY(from) → animate 0.22s easeOut → onComplete 写死 translateY(0px)）；watch 删除早退改兜底（仅跳过「播放页已关闭」与「新触摸会话已开始」）；clearDragOffsetImmediate（stopRebound + DOM 写 0 + dragOffsetY=0）覆盖进度条/歌词点击/onTouchStart 残留/resetDragState；clearDragOnWindowHide 挂 blur + visibilitychange 兜底打断残留（成对注册/移除）。保留：阈值收起、goBack、歌词区手势隔离、seekGestureLocked、露底透明。

验证：vue-tsc/eslint/build 全过；trellis-check 通过（逐条核对所有 dragOffsetY 归零路径闭环，无阻断项）。spec features-player.md 补充「拖拽/回弹必须闭环，禁止残留半屏」契约。真机复测待做：多次只下滑一点松手逐次回弹、回弹中再拖再松、快速连滑、超阈值收起、重开无残留。

### Git Commits

| Hash | Message |
|------|---------|
| `0528c8f` | fix(player): 沉浸播放页下滑回弹卡住——显式回弹+兜底清零 |

## Session 104: feat(ui): 侧边栏对齐椒盐音乐并美化

**Date**: 2026-08-16
**Task**: 08-16-sidebar-salt-polish
**Branch**: `main`

### Summary

用户反馈侧边栏与椒盐音乐不一致。实机调研（MuMu 12.2.0，`com.salt.music` 汉堡抽屉）：uiautomator dump 得菜单 8 项（歌曲/专辑/艺术家/文件夹/歌单/扫描文件/音乐库/统计）+ header 三按钮（退出/用户界面/音频效果）；像素实测 header y96..264、行高 168px(64dp)、'歌曲' text x204(77dp)、主次组间留白+线、激活项无蓝底（文字统一 #1E1715）、图标为 23dp 彩色圆角方块、背景 #f9f9f9。

落地（TabsPage.vue + useSystemDark.ts + icons）：① 新增 `.tabs-layout__panel` 结构（移动端 drawer 与平板 aside 共用）：header 64px（移动端 ✕关闭/主题切换/⚙️设置 三格均分 + 底部 hairline；平板 主题切换/⚙️设置）；② 菜单行高 56→64、字号 16、图标区固定 60px → 文字左距 76px（≈椒盐 77dp）；③ 主（歌曲/专辑/艺术家/歌单）+ 次（音源/设置）分组：9+9px 留白 + hairline；④ 激活态去蓝底 0.12 背景 → 文字加粗 600 + 图标 primary，保留 :active 瞬态微反馈；⑤ 主题三态：useSystemDark 扩展 themeMode + cycleThemeMode（system→light→dark 循环，localStorage['muses-theme'] 持久化，手动模式解绑系统监听）；⑥ 图标保持 lucide 线性（用户确认不改彩色方块）；⑦ 侧边栏顶部 padding 改紧贴状态栏下沿（对齐椒盐 header y100 起）。

验证闭环：vue-tsc/eslint/build 全过；Edge headless CDP（504px 视口）量得 drawer 252px/header 64/行高 64×6/label 起点 76/激活背景 rgba(0,0,0,0)/分组 margin+padding 9/三态循环+持久化；平板 1248px 视口 aside 260px 同结构；MuMu 安装后真机截图（1080x1920）：抽屉 544px、header 图标 #8c8c8c、菜单文字 #191919、分组线 y1084、modlens 摘要确认 header 三图标+两组菜单+激活项蓝色图标。注：MuMu WebView viewport ≈364 CSS px（缩放 2.97，非 dp 2.625），故物理行距 190px = 64 CSS px × 2.97，比例与椒盐近似。

### Git Commits

| Hash | Message |
|------|---------|
| `(see git log)` | feat(ui): 侧边栏对齐椒盐——header+分组+64dp行高+77dp文字距+去蓝底激活；主题三态 |

## Session 105: feat(ui): 侧边栏改灰色卡片样式（二版迭代）

**Date**: 2026-08-16
**Task**: 08-16-sidebar-card-style
**Branch**: `main`

### Summary

用户对昨日 side-bar-salt-polish 反馈三点：① 不要顶部三按钮（header 删除）；② 图标不要蓝色；③ 只改侧边栏、改成卡片、背景灰色（明确不改推屏交互）。另用 MuMu 四帧实验（关→开→关→开，s1/s3 完全相同）证实椒盐抽屉为覆盖式、主内容不动，且抽屉是悬浮圆角卡片（左缘 x48 空隙 18dp、右缘 552、四边 1px #e9e9e9、顶部 y96→119 圆角渐宽）；与 Muses 推屏式的差异主要靠卡片视觉拉近（用户要求保留推屏）。

改动（仅 TabsPage.vue）：① 删除 aside/drawer 的 `.tabs-layout__panel-header` 及三按钮（✕/主题/⚙️）+ themeIcon/themeActionLabel/navigateToSettings 与相关 imports（useSystemDark 三态保留无入口，spec 已注明设区页后续可接）；② drawer 槽位透明化（去 surface-1/border-right，padding 左右 0）→ `.tabs-layout__panel` 卡片化：`margin 0 12px 0 18px`、background `--m-surface-1`、`border-radius 24px`、`border 1px hairline`、阴影 `0 8px 24px rgba(0,0,0,0.08)`（深色 0.35），`overflow hidden`；③ 图标去蓝：删除 `--active` 图标 primary 覆盖，图标恒 `--m-text-2`（浅 #8c8c8c / 深 token），激活仅文字加粗；④ drawer 内 nav-link padding-left 0 → 文字起点 = 18 空隙 + 60 图标列 = 78px（椒盐 204px@2.625 = 77.7dp ✓）；⑤ 平板 aside 不卡片化（保持现状）。

验证：vue-tsc/eslint/build 过；Edge CDP（480 视口）量得 panelBg #f9f9f9 / radius 24 / border 1px hairline / margin 18+12 / hasHeader false / iconColor+activeIcon 均 rgb(140,140,140)（无蓝）/ 文字起点 79 / 行高 64 / 深色面板 #262626；MuMu 真机 1080x1920 截图：卡片 x60..525 灰色表面、左缘阴影渐化、顶部圆角 y~100、第一行菜单 y175（无 header占位）、图标区纯 #8c8c8c 灰（无 #0470e6）、modlens 确认 6 项菜单+主列表正常。spec 侧边栏视觉契约与主题三态契约同步更新（header 已弃用禁止加回）。

### Git Commits

| Hash | Message |
|------|---------|
| `(see git log)` | feat(ui): 侧边栏改灰色悬浮卡片——删 header 三按钮、图标去蓝、24px 圆角+描边+空隙 |

## Session 106: (planned) 真机回归：卡片抽屉关闭/手势/暗色待用户验收

## Session 106: fix(ui): 侧边栏卡片改椒盐分段样式（三版迭代）

**Date**: 2026-08-16
**Task**: 08-16-sidebar-segmented-cards
**Branch**: `main`

### Summary

用户反馈二版卡片"圆角和阴影跟椒盐不一样"。重新用椒盐干净打开态 s2.png 线性扫描（非 4px 采样）发现**二版做错了**：椒盐抽屉不是整张大卡，而是**分段圆角面板**——主菜单段 y312..1152（圆角 ~15dp：y1130 左缘 x56 → y1148 x80；左右缘 x48..551 + 1px #e9e9e9 描边）、次菜单段 y1200..1700，段间 48px(18dp) 空隙纯 #f3f3f3 **无投影**；二版的 24px 大圆角 + `0 8px 24px rgba(0,0,0,0.08)` 阴影是臆造。

修复（仅 TabsPage.vue 样式）：`&__drawer &__panel` 整卡样式删除 → `&__drawer &__nav` 主/次菜单各自成卡：`margin 0 12px 0 18px` + surface-1 + `border-radius 16px` + `border 1px hairline` + **无阴影**；次卡 `margin-top 18px` + `border-top none`（aside 分组线仅平板用）。验证：MuMu 真机 y 结构扫描确认两卡分离（主卡 y96..864、空隙 y896..948、次卡 y972..1384、各卡顶底描边/圆角渐变），空隙区纯 #f3f3f3 无阴影；菜单文字/行高/图标灰不变。spec 侧边栏视觉契约更新为三版最终形态（分段卡 + 16px 圆角 + 无阴影）。教训：**像素分析必须用线性逐像素扫描定边界**，4px 步进色块图会产生渐变假象（此前误判为阴影/大圆角）。

### Git Commits

| Hash | Message |
|------|---------|
| `(see git log)` | fix(ui): 侧边栏拆整卡为椒盐分段卡片——16px圆角、无阴影、段间18dp |

## Session 107: (planned) 分段卡片观感验收

## Session 107: feat(ui): 侧边栏卡片顶部留间距（四版微调）

**Date**: 2026-08-16
**Task**: 08-16-sidebar-card-top-gap
**Branch**: `main`

### Summary

用户反馈：卡片距离上面应该有一个距离，就像 navbar 一样。drawer padding-top 从 `var(--m-safe-area-top, 0px)`（卡片贴状态栏下沿）改为 `calc(var(--m-navbar-pt, 16px) + var(--m-spacing, 16px))`：navbar 避让 token 口径 + 16px 悬浮空隙（浏览器 32px、真机状态栏下 16px）。MuMu 验证：卡片顶描边 y96 → y168（+32 CSS px 空隙），主卡 y172..960 / 段间隙 y992..1068 / 次卡 y1068..1476 结构无回归。spec 顶部 padding 契约更新。

### Git Commits

| Hash | Message |
|------|---------|
| `(see git log)` | feat(ui): 侧边栏卡片顶部留 navbar 式避让间距（navbar-pt + 16px） |

## Session 108: fix(ui): 侧边栏菜单取消点击背景效果

**Date**: 2026-08-16
**Task**: 08-16-sidebar-remove-active-feedback
**Branch**: `main`

### Summary

用户反馈：专辑/艺术家/歌单点击有蓝色按压背景，歌曲（激活项）没有，要求统一取消。根因：nav-link `&:active { background-color: rgba(primary, 0.08) }` 与 `&--active { background-color: transparent }` 同特异性（0,2,0）且 --active 定义在后，激活项覆盖了 :active、非激活项保留 → 表现不一致。修复：删除 `&:active` 规则与 transition。验证：MuMu 真机按住专辑行 600ms 截图，行区域无任何蓝色调像素（仅 #8c8c8c 图标/#191919 文字/#e9e9e9 描边）。spec 侧边栏契约更新（禁止按压蓝底）。

### Git Commits

| Hash | Message |
|------|---------|
| `(see git log)` | fix(ui): 侧边栏菜单取消按压背景，统一无点击效果 |

## Session 109: fix(ui): 侧边栏选中项去深黑加粗

**Date**: 2026-08-16
**Task**: 08-16-sidebar-remove-active-bold
**Branch**: `main`

### Summary

用户反馈：选中项深黑效果去掉。删除 nav-link `&--active` 的 `font-weight: 600`（此前激活项文字加粗显深黑），与普通项完全一致（对齐椒盐激活无视觉区分）。验证：Edge CDP 六项 font-weight 全 400、颜色 rgb(25,25,25) 一致；MuMu 真机文字行像素密度对比（歌曲 787 vs 专辑 674 = 1.17x）与笔画数比例（20:17 = 1.18x）吻合，属字形差异非加粗；初次检测的大差异系旧进程 CSS 缓存，force-stop 重启后正常。spec 更新：激活项无任何视觉区分（仅保留 --active 类与 aria-current 语义）。

### Git Commits

| Hash | Message |
|------|---------|
| `(see git log)` | fix(ui): 侧边栏选中项去深黑加粗，与普通项完全一致 |

## 2026-08-17 后台不自动切歌修复（bg-auto-next-fix）

**问题**：小米 15 锁屏时当前曲播完不自动切下一首。

**根因**：切歌链路全依赖 WebView JS（complete 事件 → JS 处理 → 原生 preload/play）。锁屏后 Chromium 对不可见 WebView 节流/冻结 JS，complete 事件投递（evaluateJavascript）无法被处理；前端 position 轮询后台被节流且不检测播完，无兜底。

**方案（用户选定 C）**：
1. 原生预案兜底（AudioPlayerPlugin.kt）：JS 注册下一首预案（setAutoNext），原生 1s 轮询 isMusicActive + jsExpectedPlaying，静音 2.5s 防抖后经 Bridge.callPluginMethod 驱动 capgo preload/play，15s 验证窗口发 autoNextStarted/Failed；旧 asset 先播后卸。
2. JS 对账：syncUiToNativeSong（playSongInternal nativeAlreadyPlaying 模式）+ reconcileAfterBackground（appStateChange/visibilitychange）。
3. 心跳兜底：hidden+playing 时 getState 检查，后台节流约 1 次/分钟。

**关键决策**：
- 不 patch node_modules/@capgo/*（spec 约束）：用反射读 Bridge.msgHandler 构造 PluginCall + CALLBACK_ID_DANGLING 调 capgo 公共方法；反射失败静默降级。
- 切歌窗口先 clearAutoNextPlan，防止旧预案误触发。
- 播放失败（native.ts play catch）上报 stopped，防止兜底轮询触发旧预案。
- 验证窗口 4s→15s（WebDAV 远程缓冲慢会误报失败）。

**产物**：muses-bg-auto-next-debug.apk（debug 签名，需卸载正式版后安装验证）。

### check 阶段（2.2 子代理）修复

1. **preload already exists 复用**（native.ts play()）：锁屏预案已 preload 曲目 N+1，回前台积压 complete(N) → 自动切歌 preload(N+1) reject already exists → 原逻辑误进失败恢复链跳 N+2 且可能双声。修复：检测 already exists 复用 asset，isPlaying 时保留锁屏进度不重启。
2. **getState isPlaying fallback**（native.ts）：预案 unload 旧 asset 后 isPlaying 查询失败，原 fallback `currentStatus==='playing'` 导致对账/心跳永远误判在播。修复：fallback false。

提交：6c9a6bf。三项验证（lint/build/gradle）全绿，debug APK 已重建（muses-bg-auto-next-debug.apk，gitignore 不入库）。

### 待办
- [ ] 小米 15 真机验证（锁屏自动切歌/回前台一致性/单曲循环/暂停不切）
- [ ] 验证通过后 task.py archive 归档


## Session 99: 锁屏/后台自动切歌修复：原生预案兜底 + JS 对账 + 心跳（方案 C）

**Date**: 2026-08-17
**Task**: 锁屏/后台自动切歌修复：原生预案兜底 + JS 对账 + 心跳（方案 C）
**Branch**: `main`

### Summary

修复小米 15 锁屏时当前曲播完不自动切下一首。根因：切歌链路依赖 WebView JS，锁屏后 Chromium 冻结/节流 JS，原生 complete 事件无法被处理。方案 C（用户选定）：1) 原生 AudioPlayerPlugin 新增 setAutoNext/clearAutoNext/reportPlaybackStatus + 1s 轮询 isMusicActive 防抖 2.5s，经 Bridge.callPluginMethod（反射 msgHandler + CALLBACK_ID_DANGLING）驱动 capgo 播放预案曲，15s 验证窗口发 autoNextStarted/Failed，旧 asset 先播后卸；2) JS 侧 registerAutoNextPlan（播放成功/队列变化时注册）、syncUiToNativeSong（playSongInternal nativeAlreadyPlaying 模式）、reconcileAfterBackground（appStateChange/visibilitychange 回前台对账）、hidden 心跳兜底；3) queue onQueueChanged 挂钩。check 阶段修复 2 个缺陷：preload already exists 复用（防误跳曲+双声）、getState isPlaying 查询失败 fallback false（修复对账误判）。验证：lint/build/gradle 全绿；debug APK 已构建待真机验证。spec 已更新 features-player.md。

### Git Commits

| Hash | Message |
|------|---------|
| `6c9a6bf` | (see git log) |
| `aeeb13b` | (see git log) |

### Status

[OK] **Completed**

## 2026-08-17 AMLL 歌词播完保持最后一句高亮（amll-last-line-highlight）

**问题**：歌曲播到最后一句后，AMLL 歌词全部失活变模糊。

**根因**：PlayerPage.lyricRenderTime = position*1000，播完/暂停在末尾时超过最后一句歌词 endTime，AMLL 找不到活动行。

**修复**：lyricRenderTime 钳制上限到最后一句 endTime（无 endTime 回退 startTime）；无歌词不钳制。播放中不受影响（正常时间线），只有越界时收敛到最后一句，保持完成高亮。

提交 c113199；lint/build 全绿；debug APK 已重建。


## Session 100: AMLL 歌词播完后保持最后一句高亮

**Date**: 2026-08-17
**Task**: AMLL 歌词播完后保持最后一句高亮
**Branch**: `main`

### Summary

修复 AMLL 歌词播完/暂停在末尾时全部失活变模糊的问题。根因：PlayerPage.lyricRenderTime = position*1000 超出最后一句歌词 endTime，AMLL 找不到活动行。修复：lyricRenderTime 钳制上限到最后一句 endTime（无 endTime 回退 startTime），最后一行保持完成高亮；播放中时间线不受影响；无歌词不钳制。验证：lint（src 0 错）/ build 全绿；debug APK 已重建。spec 已更新 features-player.md。

### Git Commits

| Hash | Message |
|------|---------|
| `c113199` | (see git log) |

### Status

[OK] **Completed**

## 2026-08-17 依赖升级到最新版（deps-upgrade-all）

- npm 升 10 个：@lucide/vue 1.31.0、@tanstack/vue-form 1.33.5、@vitejs/plugin-legacy 8.2.3、esbuild 0.28.2、eslint 10.8.1、motion-v 2.4.0、terser 5.50.0、vite 8.2.1、vue 3.5.41、vue-tsc 3.3.10
- **typescript 保持 6.0.3**（调研结论：TS7 Go 原生编译器无 programmatic API，7.1 才有；vue-tsc/typescript-eslint 依赖 JS API，Vue 项目暂不能升）
- Android 升 4 个：okhttp 5.5.0、appcompat 1.8.0、fragment 1.9.0、webkit 1.17.0；AGP 9.3.1/Kotlin 2.4.10/core 1.19.0 已最新
- 验证：lint/build/gradle 全绿，APK 重建；业务代码零改动


## Session 101: 所有依赖升级到最新版（TS7 保留 6.x）

**Date**: 2026-08-17
**Task**: 所有依赖升级到最新版（TS7 保留 6.x）
**Branch**: `main`

### Summary

npm 升级 10 个包到最新（@lucide/vue 1.31.0、motion-v 2.4.0、vite 8.2.1、vue 3.5.41、vue-tsc 3.3.10 等），Android 升级 4 个（okhttp 5.5.0、appcompat 1.8.0、fragment 1.9.0、webkit 1.17.0）。typescript 有意保留 6.0.3：调研确认 TS7（Go 原生编译器）无 programmatic API（7.1 才有），vue-tsc/typescript-eslint 依赖 JS API，Vue 项目暂不能升级。AGP 9.3.1/Kotlin 2.4.10 等已最新。验证：lint（src 0 错）/ build / gradle 编译 + assembleDebug 全绿，业务代码零改动，APK 已重建。

### Git Commits

| Hash | Message |
|------|---------|
| `bf90517` | (see git log) |

### Status

[OK] **Completed**

## Session 102: 复刻椒盐沉浸式播放页平板模式（tablet-immersive-player）

**Date**: 2026-08-17
**Task**: 复刻椒盐沉浸式播放页平板模式
**Branch**: `main`

### Summary

将 muses 播放页平板（横屏 ≥768px 且宽>高）从占位双栏升级为**双栏 + 全宽底部控制条**；竖屏（含竖屏平板）保持手机式全屏沉浸。

**调研**：模拟器实测椒盐 12.2.0 平板横屏（1280x800dp）播放页 = 内容限宽偏左 + 右侧音乐厅氛围区（非双栏）；竖屏 = 手机式全屏。用户决策：保留双栏（B）+ 控制全下移底部 + 竖屏手机式 + 保持 AMLL 背景。参考图存任务 refs/。

**实现**（改动 2 文件）：
- `src/views/PlayerPage.vue`：断点从 `viewportWidth>=768` 改 `>=768 && height<width`（isTabletLayout）；容器挂 `.player-page--tablet` class；模板控件区包 `.player-page__info-controls`（手机 display:contents / 平板 display:none）；新增底部条 `bottom-bar`（v-if isTabletLayout：进度条+时间一行 + 三段式按钮左 repeat/shuffle 中 prev/play/next 右 queue/more）；平板样式全部 media query → `.player-page--tablet &` class 驱动。
- `src/theme/index.scss`：`@media (min-width:768px)` 平板块 → `.player-overlay--tablet` 后代选择器（含矮屏组合块）。

**关键决策**：断点改 class 驱动避免竖屏平板（800px 宽 ≥768 但不横屏）落进 media query 缝隙；info-controls 用 `display: contents` 保证手机 flex 子项展开零回归；右栏 lyric-header 平板保持全局隐藏（左栏头部承担）。

**验证**：模拟器 WebView CDP 实测——1280x776 平板：panels 双栏 50/50、bottomBar 1280x110 全宽、进度条 1232 全宽、三段按钮中组中心 640dp 正好居中、封面 388x388 居中；进度 seek 01:40 ✓ 循环切换 ✓ 更多菜单 ✓；800x1256 竖屏 tabletClass=false 手机式全恢复 ✓；640x336 横屏窄高不双栏+矮屏单行 ✓；412x708 手机零回归 ✓。lint/vue-tsc/build 全绿；debug APK 已重建安装。spec（features-player/component-guidelines）平板契约已更新；changelog v0.3.6。

### Git Commits

| Hash | Message |
|------|---------|
| (see git log) | feat(player): 平板沉浸页双栏+底部全宽控制条，断点改 class 驱动（竖屏保持手机式） |

### Status

[OK] **Completed**

## Session 103: 发布 v0.3.6（平板沉浸播放页）

**Date**: 2026-08-17
**Task**: 发布新版本
**Branch**: `main`

### Summary

发布 v0.3.6（含平板沉浸播放页双栏+底部控制条改动）。版本号同步：package.json / package-lock.json 0.3.6 + android build.gradle versionCode 36。本地 npm build + cap copy + assembleDebug 全绿 → commit chore(release): v0.3.6 (b08c0ae) → push main + tag v0.3.6 → GitHub Actions release workflow 构建成功（4 分内）。

### Git Commits

| Hash | Message |
|------|---------|
| `b08c0ae` | chore(release): v0.3.6 |

### Status

[OK] **Completed** — Release: https://github.com/Happier-X/muses/releases/tag/v0.3.6（assets: muses-v0.3.6.apk / muses-v0.3.6-mi.apk）

## Session 104: 修复双语译文整体后移一行（08-17-fix-lyric-translation-offset）

**Date**: 2026-08-17
**Task**: `.trellis/tasks/08-17-fix-lyric-translation-offset`（归档 `archive/2026-08/`）
**Branch**: `main`

### Summary

用户反馈：英文歌翻译行整体后移一行——第一条歌词的翻译显示在第二条上，结尾多出孤立中文行。调研定位 `src/features/lyrics/mergeTranslation.ts` 的 `mergeDuplicateTimestampTranslations`：主词双语 LRC 中译文行时间戳打在**下一句原文**的时间上（而非自己的原文行），旧「同时间戳相邻配对」把译文配给下一句 → 整体后移 + 尾部落单。

**验证路径**：esbuild 转译 + 临时脚本（.tmp-mergeTest）复现 8 组场景；真实网易 Faded lrc/yrc+tlyric 双管道抽查；trellis-check 两轮审查。

**实现**（`mergeTranslation.ts` +244）：
- 交替结构感知配对：文件顺序固定窗口 (0,1)(2,3)...，译文并回前一行原文；同时间戳相邻配对保留为回退。
- 防误配双校验（check F1）：结构一致性（配对主行同脚本族/译文互补）+ 时间定义属性（译文时间戳贴近下一窗口原文行时间戳，cross-window ≤1000ms），垫词行打断交替结构时整体拒绝激活。
- yrc+tlyric 序列感知回退（check F2）：yrc 行时间与 tlyric 偏差数百 ms 时按行序双指针对齐（宽容差 2000ms、匹配率 ≥60%），Faded yrc 从 0/58 → 54/58；边界防误判（首行无译+末 stamp 无承接）挡住 tlyric 自身错移一句。
- 首遍 80ms 挂载消费的 stamp 精确剔除（避免序列回退重复挂同一译文）；pending 过滤补 isBG；wordsText 与 linePlainText 合并去重。

**测试基础设施**：恢复最小 vitest（spec frontend 技术栈声称存在但仓库 6043084 曾 drop tests；本次仅装 vitest ^4.1.10，不装 jsdom/cypress）——`tests/unit/mergeTranslation.spec.ts` 13 用例（bug 验收 a/b + 回归 c..k + 对抗 l/m）全绿；`npm run test:unit` 挂 package.json scripts。

**质量**：lint（src+tests）exit 0；build（vue-tsc+vite）exit 0；spec `features-player.md` 双语合并两条路径 + tlyric 序列回退契约已更新。

### Git Commits

| Hash | Message |
|------|---------|
| `5a63bee` | fix(lyrics): 修复双语译文整体后移一行（译文时间戳打在下一句时错配） |
| `1469144` | chore(task): archive 08-17-fix-lyric-translation-offset |

### Status

[OK] **Completed** — 遗留提醒：用户实际歌曲若是本地/酷我/酷狗来源且仍可复现，提供 LRC 原文再核对（本次验证覆盖网易真实数据 + 构造用例，来源差异可能有边界）。

## Session 105: 手势导航时列表底部被 MiniPlayer 遮挡（08-18-fix-content-pb-safe-area-freeze)

**Date**: 2026-08-18
**Task**: `.trellis/tasks/08-18-fix-content-pb-safe-area-freeze`（归档 `archive/2026-08/`）
**Branch**: `main`

### Summary

用户反馈：启用系统手势导航（底部手势提示线）时，歌曲列表滚动到底后最后一项仍有一小块被 MiniPlayer 胶囊盖住；三键导航正常。猜测：MiniPlayer 适配了底部安全区而上移，其它部位（列表/背景）没适配。

**根因定位（headless Edge + CDP 实测复现）**：全局内容止位 token `--m-content-pb: calc(72px + var(--m-safe-area-bottom, 0px))` 定义在 `:root`，而 `:root` 同时定义了 `--m-safe-area-bottom: 0px`——Chromium 计算根元素 custom property 时会在同一元素作用域内**立即展开 var() 依赖，把 `--m-content-pb` 冻结为字面常量 `calc(72px + 0px)` 向下继承**。`.m-app` 上桥接出的真实 `--m-safe-area-bottom`（手势导航时 = 手势条高度，实测 24px）再也无法影响它 → 列表 padding-bottom 恒为 72px；而 MiniPlayer **直接**消费 `var(--m-safe-area-bottom)`（在自身作用域解析，能拿到 24px）→ 正确上移。一边上移一边冻结不动，底部漏出一整个手势条高度的内容被胶囊盖住。`html.muses-mini-visible` 里同款重复定义是同一冻结源。

**复现实验**：CDP 注入 `--safe-area-inset-bottom: 24px` 模拟手势导航 → 修复前列表 padding=72px、MiniPlayer bottom=32px、最后一行与胶囊重叠 24px；把 `--m-content-pb` 重定义到 `.m-app` 后 padding=96px、重叠归零。三键导航（0px）两场景均不受影响（72px / overlap 0）。

**实现**（`src/theme/index.scss`）：删除 `:root`（§1）与 `html.muses-mini-visible` 块中的 `--m-content-pb`/`--m-content-pb-md`/`--content-pb*` 定义，改为注释说明冻结陷阱；在 `.m-app`（§3 桥接作用域，与 `--m-navbar-pt` 同模式）新增 `--m-content-pb` / `--m-content-pb-md`（`calc(72px + var(--m-safe-area-bottom, 0px))`）。一处改动覆盖全部 7 个消费页（歌曲/专辑/艺术家/歌单/歌单详情/专辑详情/设置/音源）；其它直接消费 `--m-safe-area-bottom` 的位置（多选条/索引条/抽屉/QueuePage）不经中间 token，不在影响范围。

**验证**：headless CDP 回归（safe=24 → padding 96px / overlap 0；safe=0 → 72px / overlap 0）；`npm run build`（vue-tsc+vite）exit 0；`npm run test:unit` 13/13 通过；lint 与 `src/theme/index.scss` 无关（scss 不在 eslint 扫描范围）。 spec `frontend/component-guidelines.md`「底部几何契约」「安全区处理」两条补充冻结陷阱与「含安全区计算的 token 必须定义在 .m-app」的约定。

**技术备注**：Chromium 对 custom property 的「立即展开」是标准行为（变量定义元素与其 var() 依赖同作用域时可提前 resolve）；`.m-app` 中 `--m-navbar-pt` 一直是「定义在桥接作用域」模式所以从未踩坑，本次只是把内容止位 token 归入同一模式。08-16（7448833）修的是漏算 8px 悬浮空隙，safe-area=0 场景两处一致故未暴露本缺陷。

### Git Commits

| Hash | Message |
|------|---------|
| （本 session 待提交） | fix(ui): 手势导航下列表底部被 MiniPlayer 遮挡——内容止位 token 移入 .m-app 桥接作用域 |

### Status

[OK] **Completed** — 待真机手势导航回归；遗留提醒：`--safe-area-inset-*` 桥接在 teleport 出 `.m-app` 的浮层中失效属既有行为（见 spec 侧边栏条目），本次不涉及。

---

## 2026-08-18 — 08-18-carwith-bg-ctrl-fix（CarWith 后台播放修复，方案 A）

### 任务
小米手机连 CarWith 时：播完不自动切下一曲直接暂停；媒体通知卡片上一曲/下一曲/播放/暂停按钮全失效。

### 根因（三处收敛到同一事实：CarWith 连接后 WebView JS 冻结/深度节流）
1. complete 事件（原生 → evaluateJavascript）与媒体按钮命令（MediaSession cb → JS keepAlive handler）都依赖 JS 存活；
2. JS 晚处理 complete 时 `shouldIgnoreFinished` 拿冻结的 `state.position` 误判「未接近结尾」→ 置 paused（播完暂停的直接原因之一）；
3. 原生预案 `tickAutoNext` 用 `isMusicActive()` 判定播完——CarWith 音频重定向下可能恒 true，预案永不触发（未实测确认）。

### 方案（用户选定 A：源头保活 JS，不 patch 第三方/manifest/原生直控）
- **新增 `src/features/player/keepalive.ts`**：播放中常驻 gain=0 静音 Web Audio 轨（ConstantSource → gain0 → destination），让隐藏页面携带 ongoing media 标签阻止 Chromium 冻结；仅 Android 且播放中运行；暂停/停即停；异常静默；`muses:debug-keepalive` 日志开关。挂接：playSongInternal 成功 / resume 成功 → start；pause / stop / 恢复链终止 → stop。
- **finished 判定语义变更**：complete/STATE_ENDED 唯一合法来源 = 播放器真正播完，移除 near-end（position ≥ duration-1.25）判定，仅保留 seek 保护窗（1.5s）丢弃 seek 后伪 complete。删除 `shouldIgnoreFinished`/`isNearNaturalEnd`/`NATURAL_END_EPSILON_SEC`。
- **预案第三项（isPlaying 判定）调研后不做**：capgo 插件方法返回值经 `PluginCall.resolve()` 异步发 WebView（`CALLBACK_ID_DANGLING` 丢弃），插件间无同步返回值通道，A 边界内无法实现；列为实测项，确定失效再升级方案 B。

### 验证
- lint（src+新测试 0 错）/ vitest 19 通过（新增 keepalive.spec.ts 6 例）/ vue-tsc build ✓ / `gradlew assembleDebug` ✓（app-debug.apk 11.4MB）
- 待用户真机：CarWith 自动切歌（本地+WebDAV）、通知按钮、保活生效性（V5）、锁屏/前台回归（design §7 V1-V8）
- 风险记录：keepalive 可能令 `isMusicActive()` 恒 true → 预案在保活生效期间本就不需要；若保活失效其副作用存在但被心跳/对账兜底。

### 遗留
- spec features-player.md 已更新：finished 语义变更（第 7 点）+ CarWith 常见错误条目（保留方案 C 原记录）
- diff 仅 2 个前端文件 + 测试 + spec；无 node_modules/manifest/原生改动

---

## 2026-08-18 — 08-18-bt-car-disconnect-pause（蓝牙/车机断开时暂停播放）

### 背景
用户发现 CarWith 断开后播放不暂停（继续播）。排查：代码无任何蓝牙/音频设备监听；蓝牙耳机断开能暂停/停止是系统焦点机制在起作用（capgo OnAudioFocusChangeListener：LOSS→stop/LOSS_TRANSIENT→pause）；CarWith 断开时系统不发焦点变化。

### 方案（用户确认「开」；默认决策 D1 暂停/D2 全量覆盖/D3 待真机确认）
原生 `AudioPlayerPlugin.kt`：
- 注册 `AudioDeviceCallback`（API 23+，minSdk 24 OK，无新权限/manifest）
- `onAudioDevicesRemoved` 过滤：isSink && type ∈ {A2DP, SCO, 有线, USB_DEVICE/HEADSET/ACCESSORY, DOCK} && jsExpectedPlaying → 500ms 去抖 → callNativeAudio pause
- **关键顺序**：pause 前必须 jsExpectedPlaying=false，否则 pause 后 isMusicActive=false 会触发 auto-next 预案 2.5s 后自动播下一首
- reportPlaybackStatus 扩展记录 jsCurrentAssetId（capgo pause 需 assetId）
- 判定抽成纯函数 isDisruptiveDeviceRemoved + RemovedOutputDevice 数据类，JVM 单测 9 例

前端 native.ts：reportBridgePlaybackStatus 携带 currentAssetId（JS 状态同步零改动，走 capgo playbackState 事件）

### 验证
- ./gradlew :app:testDebugUnitTest ✓（9 例）
- npm run lint（native.ts）✓ / vue-tsc build ✓
- ./gradlew :app:assembleDebug ✓（app-debug.apk 11.5MB，含 carwith-bg-ctrl-fix + 本任务全部改动）
- 待真机：蓝牙拔线暂停 / CarWith 断开暂停 / 已暂停拔出无副作用 / 重连不自动恢复 / 切换设备不误暂停（design §7 D1-D7）

### 待办
- carwith-bg-ctrl-fix 真机验证（V1-V8）未回；本任务真机验证（D1-D7）未做——同一次装车可合验

---

## 2026-08-18 — 两个 CarWith 相关任务验收通过并归档

用户小米 15 + CarWith + 蓝牙耳机真机实测全部通过：

1. **08-18-carwith-bg-ctrl-fix**（b3fd7b0）：CarWith 下播完自动切歌不暂停（本地+WebDAV）、媒体通知按钮全部可用、快速连点无串曲；普通锁屏/前台/音量条/媒体卡片无回归；keepalive（静音 Web Audio 保活）未引入副作用。
2. **08-18-bt-car-disconnect-pause**（3cc46e5）：播放中拔蓝牙耳机/断开 CarWith 均立即暂停且通知保留可恢复；已暂停拔出无抖动；重连不自动恢复；切换设备不误暂停。

经验沉淀：
- CarWith/锁屏后台「JS 冻结」是统一根因：complete 事件与媒体按钮命令都链到 WebView JS。三条可选路径按成本排序：A 保活 JS（WebAudio 轨）→ B 通知按钮原生直控 → C 车机按钮 patch capgo。本次 A 生效即解决，未走 B/C。
- 「断开即暂停」不能依赖系统音频焦点事件（CarWith 断开不发 focus 变化）；AudioDeviceCallback 设备移除检测 + 500ms 去抖 + pause 前先置 jsExpectedPlaying=false（阻断 auto-next 预案误触发）是可靠实现。

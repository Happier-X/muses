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

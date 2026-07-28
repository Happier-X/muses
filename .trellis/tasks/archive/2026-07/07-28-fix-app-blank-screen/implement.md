# 执行计划：修复白屏（方案 1 + failback 方案 2）

## 前置条件

- 当前 `main` 分支
- 模拟器已启动（`emulator-5556`），app `com.muses.player` 已部署
- `npm run build` 通过

## 执行步骤

### 步骤 1：改 manualChunks（方案 1）

编辑 `vite.config.ts`，把 `manualChunks` 里的 `amll-pixi` 段从合并改为拆开：

```diff
-if (
-  id.includes('@applemusic-like-lyrics') ||
-  id.includes('@pixi')
-) {
-  return 'amll-pixi'
-}
+if (id.includes('@applemusic-like-lyrics')) return 'amll'
+if (id.includes('@pixi')) return 'pixi'
```

同时确保其余 chunk 逻辑不动（ionic、vue-vendor）。

### 步骤 2：构建并检查产物

```bash
npm run build  # vue-tsc + vite build
```

检查通过条件：
- 构建 exitCode 0。
- `dist/assets/` 下有 `amll*.js` 和 `pixi*.js` chunk（不再有 `amll-pixi*.js`）。
- 产物无交叉 import 循环（用脚本扫 `amll` chunk 是否 `import` 自 `pixi` chunk 以及 `pixi` 是否 `import` 自 `amll`）。

扫环脚本：
```js
// node -e 快速扫
const fs=require('fs');
const amll=fs.readdirSync('dist/assets').filter(f=>f.startsWith('amll')&&f.endsWith('.js')&&!f.includes('legacy'))[0];
const pixi=fs.readdirSync('dist/assets').filter(f=>f.startsWith('pixi')&&f.endsWith('.js')&&!f.includes('legacy'))[0];
if(amll)console.log('amll chunk:',amll);
if(pixi)console.log('pixi chunk:',pixi);
if(amll&&pixi){
 const a=fs.readFileSync('dist/assets/'+amll,'utf8');
 const p=fs.readFileSync('dist/assets/'+pixi,'utf8');
 const aImpsP=a.includes('/pixi-');
 const pImpsA=p.includes('/amll-');
 console.log('amll imports pixi?',aImpsP,'pixi imports amll?',pImpsA);
 if(!(aImpsP&&pImpsA))console.log('NO CYCLE');
}
```

### 步骤 3：部署到模拟器

```bash
# 同步 dist 到 Android
npx cap sync android

# 先清理确保冷启动
adb -s emulator-5556 shell am force-stop com.muses.player
adb -s emulator-5556 logcat -c

# 启动 app
adb -s emulator-5556 shell monkey -p com.muses.player -c android.intent.category.LAUNCHER 1

sleep 5
```

### 步骤 4：CDP 验证 — 确认无 `t is not a function`

```bash
# 获取最新的 webview devtools socket pid
adb -s emulator-5556 shell "cat /proc/net/unix | grep webview_devtools_remote" | awk 'NR==1{print $NF}' | grep -oE '[0-9]+'

# forward 到 9222
adb -s emulator-5556 forward tcp:9222 localabstract:webview_devtools_remote_<PID>

# 用 tmp_cdp_grab.mjs 抓 console / exception
node tmp_cdp_grab.mjs "ws://localhost:9222/devtools/page/<PAGEID>"
```

验收：
- `console.error` 无 `TypeError: t is not a function`。
- `EXCEPTION` 无阻断性报错。
- 模拟器上肉眼看到首屏（歌单列表）渲染（也可用 CDP 截图 `Page.captureScreenshot`）。

### 步骤 5：若方案 1 仍报循环 → failback 方案 2

修改 `manualChunks`：移除 `@applemusic-like-lyrics` / `@pixi` 的规则，让 Rolldown 自动切分。保留 ionic / vue-vendor 规则。

重新执行步骤 2-4。

### 步骤 6：收尾

- 确保 `tmp_*.mjs` 不在工作目录里（清理工具脚本或移动）。
- commit 改动（仅 vite.config.ts），message 格式参考项目惯例。
- 更新 prd.md acceptance criteria 打勾。

## 回滚点

- 改 config 后首次 build 前，`git stash` 可立即回。
- 部署后 CDP 验证失败 → 切回 failback 方案 2（config 再改一次）或 `git checkout vite.config.ts` 回到原始状态。

## 风险文件

- `vite.config.ts` — 唯一改动文件，结构简单、可读性强，回滚仅需 resotre 一个文件。

## 实施结果

- 方案 1 产物仍有 `amll <-> storage` 循环，按预案切换到方案 2。
- 最终只删除 `vite.config.ts` 中 AMLL/PIXI 的强制 `manualChunks` 规则，其余 ionic / vue-vendor 规则不变。
- Rolldown 自动把 AMLL/PIXI 保留在异步 `PlayerPage` chunk，未进入首屏 `index` chunk。

## 检查清单

- [x] `npm run build` 通过
- [x] 产物无 `amll-pixi` chunk
- [x] 34 个 modern JS chunk 静态 import 图无环
- [x] `npx cap sync android` 与 `./gradlew installDebug` 成功
- [x] CDP 验证无 `TypeError: t is not a function`
- [x] 冷启动 URL 到达 `/tabs/songs`，DOM 可读到真实歌曲列表
- [x] `npm run lint` 通过
- [x] 验收标准全部满足
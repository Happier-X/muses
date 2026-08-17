# 实施计划：所有依赖升级

## 执行顺序

### 1. npm 依赖升级（10 个，排除 typescript）

```bash
npm install @lucide/vue@^1.31.0 @tanstack/vue-form@^1.33.5 @vitejs/plugin-legacy@^8.2.3 \
  esbuild@^0.28.2 eslint@^10.8.1 motion-v@^2.4.0 terser@^5.50.0 \
  vite@^8.2.1 vue@^3.5.41 vue-tsc@^3.3.10
```

- [ ] 1.1 执行安装（不升 typescript）
- [ ] 1.2 `npm run lint`（src 0 错误）
- [ ] 1.3 `npm run build`（vue-tsc + vite）
- [ ] 1.4 `npm outdated` 核对：仅 typescript 7 一项保留

### 2. Android Gradle 依赖核查

- [ ] 2.1 用 Maven Central 最新版对比：AGP（com.android.tools.build:gradle）、Kotlin、okhttp、androidx.*（variables.gradle 各版本）
- [ ] 2.2 升级有新版且兼容的（Capacitor 插件兼容性优先，native-audio/media-session 为源码依赖需与 AGP/Kotlin 兼容）
- [ ] 2.3 `cd android && ./gradlew :app:compileDebugKotlin` 通过

### 3. 回归验证

- [ ] 3.1 变更 diff 审查：升级不引入破坏性 API 变更（重点 vue 3.5.41 patch、motion-v minor）
- [ ] 3.2 `npx cap copy android && ./gradlew :app:assembleDebug` 重建 APK
- [ ] 3.3 关键路径代码走查（播放 controller/native、AMLL 歌词、WebDAV）

### 4. 提交

- [ ] 4.1 git commit（含 package.json/package-lock.json + gradle 文件 + APK 说明）

## 验证命令

```bash
npm run lint
npm run build
cd android && ./gradlew :app:compileDebugKotlin :app:assembleDebug
```

## 回滚点

- 升级失败/不兼容：`git checkout package.json package-lock.json && npm install` 还原；gradle 文件同理 git 还原

## 评审门

- G1：1.x 完成后 lint/build 绿
- G2：2.x 完成后 gradle 编译绿
- G3：3.x 完成后全量验证绿 → 提交

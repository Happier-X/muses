# 实施清单：发布 v0.2.1

## 前置

- [ ] 已读 `prd.md` / `design.md` / 本文件
- [ ] 确认远端无 `v0.2.1`：`git ls-remote --tags origin 'v0.2.1*'`
- [ ] 工作树仅允许本任务文件与版本相关改动

## 步骤

1. **改版本**
   - `package.json` → `0.2.1`
   - `package-lock.json` 根与 `packages[""].version` → `0.2.1`
   - `android/app/build.gradle`：`versionName "0.2.1"`、`versionCode 21`

2. **写 Changelog**
   - 新建 `changelog/v0.2.1.md`（中文，用户向）

3. **本地验证**

```bash
npm ci
npm run lint
npm run build
npm run test:unit -- --run
# 若 fork worker 偶发失败：
# npx vitest --run --pool=threads --maxWorkers=2
git diff --check
npx cap sync android
```

4. **提交**

```bash
git add package.json package-lock.json android/app/build.gradle changelog/v0.2.1.md
git commit -m "chore(release): v0.2.1"
```

（任务规划文件可在归档时一并提交，或单独 docs 提交，勿混入无关业务）

5. **推送与 Tag**

```bash
git push origin main
git tag -a v0.2.1 -m "v0.2.1"
git push origin v0.2.1
```

6. **验收 Release**
   - 监控 workflow `Release`
   - 确认 Release 页存在：
     - `muses-v0.2.1.apk`
     - `muses-v0.2.1-mi.apk`
   - 可用：`gh release view v0.2.1`

## 完成定义

- prd 全部 AC 勾选
- 远端 tag + 双 APK Release 成功

## 回滚

- 未 push tag：本地 `git reset` / 修正后重做
- 已 push tag 且错误：修 commit → 移动 tag → force-push **仅该 tag**（按现有规范），并删除错误 Release 资源后重建

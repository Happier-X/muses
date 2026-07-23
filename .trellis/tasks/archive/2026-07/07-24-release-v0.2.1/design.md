# 设计：发布 v0.2.1

## 边界

本任务 **不开发新功能**，只做版本元数据、Changelog、验证与远端发布。

| 做 | 不做 |
|----|------|
| 改版本号与 versionCode | 新功能 / 大重构 |
| 写 `changelog/v0.2.1.md` | 改业务源码（除版本相关文件） |
| 本地验证 + push main + tag | 本地签名 APK 替代 CI |
| 验收 GitHub Release | 覆盖未知远端 tag |

## 版本契约

| 文件 | 字段 | 值 |
|------|------|-----|
| `package.json` | `version` | `0.2.1` |
| `package-lock.json` | 根 `version` 与 `packages[""].version` | `0.2.1` |
| `android/app/build.gradle` | `versionName` | `"0.2.1"` |
| `android/app/build.gradle` | `versionCode` | `21`（相对 20 +1） |
| Git tag | annotated | `v0.2.1` |

## 发布流水线

```text
本地改版本 + changelog
  → npm ci / lint / build / unit / cap sync
  → commit chore(release): v0.2.1
  → push origin main
  → git tag -a v0.2.1 -m "v0.2.1"
  → push origin v0.2.1
  → Actions: npm ci → build → assembleRelease ×2 → gh-release + APK
```

## Changelog 结构建议

```markdown
# v0.2.1

## 播放
...

## 性能
...

## 界面与体验
...

## 问题关闭（若有明确 issue 号）
...
```

只写用户可感知项；禁止堆 task 归档、journal、spec 文案。

## 失败处理

| 阶段 | 动作 |
|------|------|
| 验证失败 | 不 commit / 不 tag |
| push main 失败 | 解决远端冲突后重试，仍不 tag |
| workflow `npm ci` 失败 | 按 quality-guidelines 修 lock/overrides，必要时移 tag |
| APK 缺失 | 不归档任务，直到 Release 完整 |

## 与规范关系

严格遵循 `.trellis/spec/frontend/quality-guidelines.md` 的「发布约定（v* tag）」。

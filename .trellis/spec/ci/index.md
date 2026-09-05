# CI / 发包流水线规范

> 本层记录 GitHub Actions 与打包链路的可执行契约。来源：v0.5.2 发版全链路实测（2026-09-05，run 33935428385 首次三 job 全绿）。

## 文档

| 文档 | 内容 |
|------|------|
| [release-pipeline.md](./release-pipeline.md) | release.yml 三 job 结构、版本注入契约、变量约定、已踩坑与排查矩阵 |

## 适用场景

- 修改 `.github/workflows/release.yml` / `build-test.yml`
- 调整 `composeApp` 的 `nativeDistributions`（MSI/EXE 打包）
- 排查发版 CI 失败

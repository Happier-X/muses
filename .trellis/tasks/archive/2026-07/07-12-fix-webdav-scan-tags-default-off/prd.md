# WebDAV 扫描默认不读取音乐标签

## Goal

WebDAV 音源扫描时「读取音乐标签」选项默认关闭，避免慢/卡。

## Issue

- GitHub #9：`webdav文件夹扫描的时候，读取音乐标签默认关闭即可`

## Confirmed Facts

- `src/views/SourcesPage.vue`
  - `const scanOptions = ref<ScanOptions>({ readTags: true })`
  - `openScanSettings(source)` 中 `scanOptions.value = { readTags: true }`
- `src/features/library/scanner.ts` 中 `options.readTags` 决定是否调 `readWebDavAudioTags`。
- `SourceItem` 有 `type: 'local' | 'webdav'`（`@/features/sources/types`）。

## Requirements

1. **R1** WebDAV 音源打开扫描设置时，`readTags` 默认 `false`。
2. **R2** 本地音源保持默认 `true`（不回归）。
3. **R3** 用户仍可手动打开开关。
4. **R4** 同步 component-guidelines 或 features-library（如有扫描默认约定）。
5. **R5** lint / type-check 通过；必要时补单测。

## Acceptance Criteria

- [ ] AC1：WebDAV 扫描设置打开时开关默认关闭。
- [ ] AC2：本地扫描设置仍默认开启。
- [ ] AC3：spec 已更新。

## Task Type

Lightweight

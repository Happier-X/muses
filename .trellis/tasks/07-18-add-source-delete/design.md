# 技术设计

## 方案

1. 在 `src/features/sources/storage.ts` 增加删除 API：
   - `deleteSource(sourceId, existingSources?)` 根据 id 删除音源并保存列表。
   - 若音源为 WebDAV，调用 `removeWebDavPassword(source.credentialKey)`。
   - 返回删除的音源与剩余列表，便于 UI 更新。
2. 在删除音源后调用 `reconcileSourceSongs(source.id, [])` 清理该音源歌曲。
   - 复用 #32 的对账能力，保证只清理当前 `sourceId`。
3. 在 `SourcesPage.vue` 音源卡片操作区增加删除按钮。
   - 使用 `ion-alert` 做确认弹窗，取消不产生副作用。
   - 确认后执行删除、更新本地 `sources` ref、持久化、清理歌曲并提示成功。
4. 单测在 `tests/unit/sources.spec.ts` 覆盖：
   - 本地音源删除仅更新 sources。
   - WebDAV 音源删除会移除 SecureStorage 密码。
   - 删除不存在音源安全返回。

## 边界

- 删除 WebDAV 凭据失败时，删除流程应失败并保留 sources 列表，避免 metadata 已删但密码残留或状态不一致。
- UI 不保存 WebDAV 密码；删除只使用已有 `credentialKey`。
- 删除歌曲发生在音源删除成功后；歌曲清理失败不应影响凭据安全，但应避免吞掉异常。

## 验证与回滚

验证：`npm run lint`、`npm run build`、`npm run test:unit`。回滚：移除删除按钮、确认弹窗与 `deleteSource` API。

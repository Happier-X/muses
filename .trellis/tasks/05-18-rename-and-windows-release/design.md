# Design: Project Rename & Windows Release

## Overview

双目标改造：(1) 全项目字符串替换 `nagomusic`/`NagoMusic` → `Muses`；(2) CI 增加 Windows MSIX 构建。两者部分重叠（CI 产物名需使用新名称）。

---

## 1. Rename Strategy

### Scope Classification

Three categories of rename targets:

| Category | Count | Approach |
|----------|-------|----------|
| **Display labels** (app title, window title, manifest labels) | ~15 | Simple string replace; no functional impact |
| **Identifier strings** (package names, method channels, bundle IDs, URI authorities) | ~10 | Must be kept consistent; Android `applicationId` must match method channel prefix for security |
| **Class/API names** (`NagoMusicApp`) | 2 | Dart class rename + all references |

### Rename Order (dependencies)

1. `pubspec.yaml` → package name → affects everything downstream via `flutter pub get`
2. Platform configs (Android, iOS, macOS, Windows, Web) → build identity
3. Dart source files → runtime strings and class names
4. CI workflow → artifact naming

**Risk**: Changing `pubspec.yaml` `name:` will change the `.flutter-plugins` generated code and plugin registrant. Run `flutter clean && flutter pub get` immediately after.

### Database Compatibility

- DB file renames from `nagomusic.db` → `muses.db`
- Old database will not be migrated. This is acceptable per constraints.

---

## 2. Windows MSIX Build Design

### Pipeline

The `msix` pub package handles MSIX creation via:

```bash
# After flutter pub add --dev msix
dart run msix:create
```

This reads `msix_config` from `pubspec.yaml` and produces a `.msix` file in `build/windows/runner/Release/`.

### CI Job Design

```yaml
build-windows:
  runs-on: windows-latest
  steps:
    1. checkout
    2. flutter setup (stable)
    3. flutter pub get
    4. flutter build windows --release
    5. dart run msix:create        # produces .msix
    6. rename .msix → muses-v{VERSION}-x64.msix
    7. upload to release
```

### MSIX Signing

MSIX requires a code signing certificate for store-ready packages. Two options:

| Option | Pros | Cons | Recommendation |
|--------|------|------|---------------|
| **Self-signed cert** | No cost, works for sideloading | Users see "unknown publisher" warning | **Chosen for MVP** |
| **Paid cert (e.g. DigiCert)** | Trusted by Windows | Cost, setup overhead | Future enhancement |

For CI, we'll generate a self-signed cert on the fly if `WINDOWS_CERT_BASE64` secret is not set. This allows sideloading.

### msix_config in pubspec.yaml

```yaml
msix_config:
  display_name: Muses
  publisher_display_name: Happier
  identity_name: com.happier.muses
  msix_version: 1.2.8.7    # mapped from pubspec version
  logo_path: 开发文档/NagoAPP图标.png    # same icon as Android
  capabilities: internetClient, backgroundExecution
```

---

## 3. CI Workflow Changes

### Current State
- Single job `build-android` on `ubuntu-latest`
- Triggers: push to main/master when pubspec.yaml changes, or workflow_dispatch

### Target State
- Two parallel jobs: `build-android` (unchanged trigger) + `build-windows`
- Same trigger conditions for both
- Windows job conditionally runs on `workflow_dispatch` only if secrets are missing (to avoid wasting runner minutes)

### Artifact Naming

```
Android:  muses-v{VERSION}-arm64-v8a.apk
          muses-v{VERSION}-armeabi-v7a.apk
          muses-v{VERSION}-x86_64.apk

Windows:  muses-v{VERSION}-x64.msix
```

---

## 4. Security Considerations

- Method channels with old `com.lanke.nagomusic` prefix must be updated to `com.happier.muses` to maintain Android security model (apps can only listen on their own package-prefixed channels)
- No credentials or secrets exposed in code
- MSIX self-signed cert generated at build time, not committed

---

## 5. Rollback Plan

- All changes are in tracked files; `git revert` is sufficient
- For database: old `nagomusic.db` is untouched (new name means new file); rollback by restoring `name:` in pubspec.yaml and reverting platform configs

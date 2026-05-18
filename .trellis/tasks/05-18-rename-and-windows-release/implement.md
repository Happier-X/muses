# Implement: Project Rename & Windows Release

## Execution Checklist

### Phase A: Project Rename

#### A1. Package Identity

- [ ] `pubspec.yaml`: `name: nagomusic` → `name: muses`
- [ ] Run: `flutter clean && flutter pub get`

#### A2. Android Config

- [ ] `android/app/build.gradle.kts`: `namespace` & `applicationId` → `com.happier.muses`
- [ ] `android/app/src/main/AndroidManifest.xml`:
  - `android:label="NagoMusic"` → `android:label="Muses"`
  - 2x lyricon metadata values → `Muses`

#### A3. iOS Config

- [ ] `ios/Runner/Info.plist`: `CFBundleDisplayName` → `Muses`; `CFBundleName` → `Muses`

#### A4. macOS Config

- [ ] `macos/Runner/Configs/AppInfo.xcconfig`: `PRODUCT_NAME` → `Muses`; `PRODUCT_BUNDLE_IDENTIFIER` → `com.happier.muses`

#### A5. Windows Config

- [ ] `windows/CMakeLists.txt`: `project()` & `BINARY_NAME` → `muses`
- [ ] `windows/runner/Runner.rc`: all `nagomusic` fields → `Muses`; `CompanyName` → `happier`; `LegalCopyright` → `happier`
- [ ] `windows/runner/main.cpp`: window title → `Muses`

#### A6. Web Config

- [ ] `web/manifest.json`: `name` & `short_name` → `Muses`

#### A7. Dart Source Files

- [ ] `lib/app/app.dart`: class `NagoMusicApp` → `MusesApp`; title `'NagoMusic'` → `'Muses'`
- [ ] `lib/main.dart`: `NagoMusicApp()` → `MusesApp()`
- [ ] `lib/pages/settings/version_info_page.dart`: `_appName` → `Muses`; debug log prefix → `muses-debug-`
- [ ] `lib/pages/settings/cache_settings_page.dart`: display strings → `Muses`
- [ ] `lib/components/layout/side_menu.dart`: display string → `Muses`

#### A8. Service Layer (Method Channels / URIs / DB)

- [ ] `lib/app/services/song_download_service.dart`: URI & subdirectory → `com.happier.muses` / `Muses`
- [ ] `lib/app/services/native_audio_thumbnail_service.dart`: content URI → `com.happier.muses`
- [ ] `lib/app/services/media_notification_service.dart`: channel ID → `com.muses.playback`
- [ ] `lib/app/services/lyrics/meizu_lyrics_service.dart`: method channel → `com.happier.muses/meizu_lyrics`
- [ ] `lib/app/services/lyrics/lyricon_service.dart`: method channel → `com.happier.muses/lyricon`
- [ ] `lib/app/services/db/db_constants.dart`: db name → `muses.db`
- [ ] `lib/app/services/android_platform_service.dart`: content URI → `com.happier.muses/downloads`

#### A9. App Update URL

- [ ] `lib/app/services/app_update_service.dart`: GitHub repo → `Happier-X/Muses`

#### A10. Verify Rename Completeness

- [ ] `rg "nagomusic|NagoMusic|Nagomusic" lib/ android/ ios/ macos/ windows/ web/ pubspec.yaml --glob '!**/build/**'` → expect 0 matches (except possibly generated files)

### Phase B: Windows MSIX Config

- [ ] Add `msix` dev dependency to `pubspec.yaml`
- [ ] Add `msix_config` section to `pubspec.yaml`
- [ ] Run: `flutter pub get`

### Phase C: CI Workflow Update

- [ ] `.github/workflows/build-release.yml`:
  - Rename APK artifacts: `nagomusic-v*` → `muses-v*`
  - Add `build-windows` job:
    - `runs-on: windows-latest`
    - steps: checkout → flutter setup → pub get → build windows --release → msix:create → rename → upload
  - Ensure Windows job also gets version extraction step
  - Add Windows MSIX to release `files:`

### Phase D: Validation

- [ ] `rg "nagomusic|NagoMusic|Nagomusic" --glob '!**/.git/**' --glob '!**/build/**' --glob '!**/generated/**' --glob '!**/.*/**'` → confirm zero remaining references
- [ ] `flutter analyze` → pass
- [ ] `flutter test` → pass
- [ ] Review CI workflow YAML syntax

---

## Validation Commands

```bash
# After A1
flutter clean && flutter pub get

# After all rename
rg -i "nagomusic" --glob '!**/.git/**' --glob '!**/build/**' --glob '!**/.dart_tool/**' --glob '!**/generated/**' --glob '!*.lock'

# Lint
flutter analyze

# Test
flutter test
```

---

## Rollback Points

| Checkpoint | How to Rollback |
|-----------|----------------|
| After A1 (pubspec rename) | Revert pubspec.yaml, `flutter clean` |
| After A2-A9 (all rename) | `git checkout -- .` |
| After B (msix config) | Remove msix config from pubspec, `flutter pub get` |
| After C (CI change) | Revert CI yaml |

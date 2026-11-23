# JoudJawadTube Implementation Plan

Master engineering checklist covering Tasks **T01–T18** across **Phases 0–3** for `JoudJawadTube_Antigravity` (Android Kotlin + Jetpack Compose + Media3 + Room).

---

## Guardrails (Non-Negotiable)

1. **100% Ad-Free Primary Engine**: Keep ExoPlayer + InnerTube + `YouTubeChunkedHttpDataSource_Antigravity` as the primary playback engine. `allowWebViewFallback` MUST default to `false`.
2. **Sideloading Only**: Do NOT remove `MANAGE_EXTERNAL_STORAGE`, `REQUEST_INSTALL_PACKAGES`, or `AppUpdater_Antigravity`. Harden them instead; ignore Google Play / Families Policy restrictions that conflict with sideloading or USB OTG access.
3. **Background Playback is Required**: Migrate ExoPlayer lifecycle to Media3 `MediaSessionService` in Phase 2 so audio/video playback continues seamlessly when enabled (`backgroundPlaybackEnabled = true`).
4. **Zero User Data Loss**: Preserve all Room database tables and upgrade paths from DB v4 (`AppDatabase.kt`).
5. **Naming Convention**: All **NEW** files created must use the `_Antigravity` suffix (never `_Codex`).
6. **Disjoint Verification**: Verify builds, unit tests, and lint via Gradle without breaking existing TV/Tablet/Phone layouts or EN/AR localization parity.

---

## Phase 0 — Foundation, Privacy & Updater Hardening

- [x] **T01 — Settings Schema & Localization Foundation**
  - **Goal**: Expand `AppSettingsState` with parental, background playback, fallback, and watch-timer preferences, persist them in `SharedPreferences`, optimize locale/theme re-application on update, and add matching EN/AR string resources.
  - **Target Files**:
    - `app/src/main/kotlin/com/joudjawadtube/player/data/settings/AppSettings_Antigravity.kt`
    - `app/src/main/res/values/strings.xml`
    - `app/src/main/res/values-ar/strings.xml`
  - **Verification**:
    - `.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest`

- [x] **T02 — Master Implementation Plan & Tracking**
  - **Goal**: Document Phases 0–3, tasks T01–T18, guardrails, target files, and verification commands in a versioned checklist.
  - **Target Files**:
    - `IMPLEMENTATION_PLAN_Antigravity.md`
  - **Verification**:
    - Confirm all tasks T01–T18, guardrails, and exact file paths are present.

- [x] **T03 — Thumbnail Privacy & OOM Hardening**
  - **Goal**: Stop leaking local family video filenames to YouTube Search (`searchVideoIdByTitle`), add two-pass `BitmapFactory.Options` downsampling (480×270 target) to prevent OOM, disconnect `HttpURLConnection` in `finally`, avoid synchronous index warmup on calling threads, and expose `getCacheSizeBytesAsync()`.
  - **Target Files**:
    - `app/src/main/kotlin/com/joudjawadtube/player/data/thumbnail/ThumbnailManager_Antigravity.kt`
  - **Verification**:
    - `.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest`

- [x] **T04 — In-App Updater Security Hardening** *(Core hardening, security tests, and SettingsScreen/MainActivity installer UI wiring complete)*
  - **Goal**: Sanitize downloaded APK filenames (`sanitizeApkFileName`), enforce canonical directory containment inside `updatesDir`, verify HTTP 2xx status and disconnect sockets in `finally`, and enforce mandatory SHA-256 signing certificate verification (removing silent fallback bypasses) with unit tests.
  - **Target Files**:
    - `app/src/main/kotlin/com/joudjawadtube/player/data/updater/AppUpdater_Antigravity.kt`
    - `app/src/test/kotlin/com/joudjawadtube/player/AppUpdaterSecurityTest_Antigravity.kt`
  - **Verification**:
    - `.\gradlew.bat :app:testDebugUnitTest --tests "com.joudjawadtube.player.AppUpdaterSecurityTest_Antigravity"`

---

## Phase 1 — Kid-Safety, Offline Resilience & Parental Controls

- [x] **T05 — Kid-Safe YouTube Resolution & Fallback Guardrails** *(YouTubeService, PlaybackScreen, CatalogViewModel, YouTubeSafetyTest complete)*
  - **Goal**: Honor `allowGeneralFallbackForSavedChannels` and `allowWebViewFallback` inside YouTube resolution and related-video filtering, differentiating kids-restricted, offline, and generic YouTube stream errors cleanly.
  - **Target Files**:
    - `app/src/main/kotlin/com/joudjawadtube/player/data/youtube/YouTubeService_Antigravity.kt`
    - `app/src/main/kotlin/com/joudjawadtube/player/data/repository/KidsCatalogRepository_Antigravity.kt`
    - `app/src/main/kotlin/com/joudjawadtube/player/data/repository/RelatedVideosResolver_Antigravity.kt`
  - **Verification**:
    - `.\gradlew.bat :app:testDebugUnitTest`

- [x] **T06 — Parental Gate & Kid Lock UI Components** *(ParentalGateDialog_Antigravity, MainActivity gating, PlaybackScreen error/WebView/subtitles, and SmartTubePlayerHud slider complete; KidLockOverlay_Antigravity scheduled next)*
  - **Goal**: Build a reusable multiplication/math Parental Gate modal (`parental_gate_title`, `parental_gate_prompt`, `parental_gate_wrong`) governed by `parentalGateEnabled`, plus a Kid Lock playback overlay requiring hold-to-unlock (`kid_lock_label`, `kid_lock_hold_to_unlock`, `kid_lock_unlocked`) for TV D-pad and touch screens.
  - **Target Files**:
    - `app/src/main/kotlin/com/joudjawadtube/player/ui/components/ParentalGateDialog_Antigravity.kt`
    - `app/src/main/kotlin/com/joudjawadtube/player/ui/playback/KidLockOverlay_Antigravity.kt`
  - **Verification**:
    - `.\gradlew.bat :app:compileDebugKotlin`

- [x] **T07 — Watch-Time Timer & Break Enforcement** *(Wave 2 MediaSessionService `JoudPlaybackService_Antigravity` + Manifest + `MainActivity` notification permission complete; WatchTimerManager/Dialog scheduled for Wave 3)*
  - **Goal**: Track active viewing duration against `watchTimeLimitMinutes`, display time's-up enforcement dialog (`watch_timer_times_up_title`, `watch_timer_times_up_message`), and allow parent-gated extensions (`watch_timer_extend_parent`).
  - **Target Files**:
    - `app/src/main/kotlin/com/joudjawadtube/player/data/settings/WatchTimerManager_Antigravity.kt`
    - `app/src/main/kotlin/com/joudjawadtube/player/ui/components/WatchTimerDialog_Antigravity.kt`
  - **Verification**:
    - `.\gradlew.bat :app:testDebugUnitTest`

- [x] **T08 — Network Connectivity Monitor & Offline Banner** *(Wave 2 PiP mode + `MainActivity` `enterPipMode` + `PlaybackScreen` PiP overlay suppression complete; `NetworkMonitor_Antigravity` & offline banner also complete)*
  - **Goal**: Observe network connectivity via `ConnectivityManager` and render a localized `offline_banner_message` component when offline while preserving uninterrupted local/USB playback.
  - **Target Files**:
    - `app/src/main/kotlin/com/joudjawadtube/player/data/scanner/NetworkMonitor_Antigravity.kt`
    - `app/src/main/kotlin/com/joudjawadtube/player/ui/components/OfflineBanner_Antigravity.kt`
  - **Verification**:
    - `.\gradlew.bat :app:compileDebugKotlin`

- [x] **T09 — Settings UI Integration & Async Cache Reporting** *(Wave 1 Settings UI + `getCacheSizeBytesAsync` & Wave 2 `NetworkMonitor_Antigravity` + `OfflineStatusBanner_Antigravity` complete)*
  - **Goal**: Add the Parental & Kid-Safety section (`settings_parental_section_title`, parental gate, background playback, general fallback, WebView fallback, watch timer) to Settings, guard sensitive actions/reset behind the Parental Gate, and load thumbnail cache size asynchronously via `getCacheSizeBytesAsync()`.
  - **Target Files**:
    - `app/src/main/kotlin/com/joudjawadtube/player/ui/catalog/SettingsScreen_Antigravity.kt`
  - **Verification**:
    - `.\gradlew.bat :app:compileDebugKotlin :app:lintDebug`

---

## Phase 2 — Media3 Background Playback & Player UX

- [x] **T10 — Media3 `MediaSessionService` Implementation** *(Wave 2 `JoudPlaybackService_Antigravity`, `AndroidManifest.xml`, and modular OkHttp InnerTube + 50-entry LRU cache + `InnerTubeResilienceTest_Antigravity` complete)*
  - **Goal**: Move ExoPlayer ownership into a Media3 `MediaSessionService` configured with `YouTubeChunkedHttpDataSource_Antigravity`, audio focus/becoming-noisy handling, and media notification metadata while keeping sideloading permissions intact.
  - **Target Files**:
    - `app/src/main/kotlin/com/joudjawadtube/player/playback/PlaybackService_Antigravity.kt`
    - `app/src/main/AndroidManifest.xml`
  - **Verification**:
    - `.\gradlew.bat :app:compileDebugKotlin :app:processDebugMainManifest`

- [x] **T11 — PlaybackScreen & HUD Migration to `MediaController`** *(Wave 2 `MediaController` binding, lifecycle surface detach, `YouTubeChunkedHttpDataSource_Antigravity` precompiled regex + redirect hardening + chunk retry complete; Kid Lock & Watch Timer overlays scheduled for Wave 3)*
  - **Goal**: Bind `PlaybackScreen` to `PlaybackService_Antigravity` via `MediaController`, expose localized loading and categorized error UI (`player_loading_stream`, `player_error_local`, `player_error_youtube`, `player_error_kids_restricted`, `player_error_offline`, `player_retry`), and integrate Kid Lock + Watch Timer overlays.
  - **Target Files**:
    - `app/src/main/kotlin/com/joudjawadtube/player/ui/playback/PlaybackScreen.kt`
    - `app/src/main/kotlin/com/joudjawadtube/player/ui/playback/SmartTubePlayerHud.kt`
  - **Verification**:
    - `.\gradlew.bat :app:compileDebugKotlin`

- [x] **T12 — Activity Lifecycle, Background Audio & PiP Polish** *(Wave 2 lifecycle/PiP + Wave 3 `WatchTimerManager_Antigravity`, `WatchTimerManagerTest_Antigravity`, Kid Screen Lock, Watch Timer overlay, and `isOnline` auto-reconnect complete)*
  - **Goal**: Update `MainActivity` lifecycle and Picture-in-Picture transitions so playback continues in the background when `backgroundPlaybackEnabled` is `true` and pauses cleanly when `false`.
  - **Target Files**:
    - `app/src/main/kotlin/com/joudjawadtube/player/MainActivity.kt`
  - **Verification**:
    - `.\gradlew.bat :app:compileDebugKotlin`

- [x] **T13 — Catalog, Channels & Offline UI Wiring** *(Wave 3 Kids-first 4-tab `CompactBottomNav_Antigravity`, 48dp touch targets, RTL HUD fix, Light Mode theme tokens, and 302/302 EN/AR localization complete)*
  - **Goal**: Wire `OfflineBanner_Antigravity`, parental-gated channel/playlist/reset actions, and non-blocking thumbnail loading across TV, Tablet/Phone, and Kids Picks screens.
  - **Target Files**:
    - `app/src/main/kotlin/com/joudjawadtube/player/ui/catalog/CatalogViewModel.kt`
    - `app/src/main/kotlin/com/joudjawadtube/player/ui/catalog/TvCatalogScreen.kt`
    - `app/src/main/kotlin/com/joudjawadtube/player/ui/catalog/TabletCatalogScreen.kt`
    - `app/src/main/kotlin/com/joudjawadtube/player/ui/catalog/KidsPicksScreen_Antigravity.kt`
    - `app/src/main/kotlin/com/joudjawadtube/player/ui/catalog/ChannelsPlaylistsCategoriesScreens_Antigravity.kt`
  - **Verification**:
    - `.\gradlew.bat :app:compileDebugKotlin`

---

## Phase 3 — Persistence, Scanner Reliability & Release Verification

- [x] **T14 — Room Database Integrity & Migration Verification** *(Wave 3 chunked `getProgressForKeys`, `syncMutex`, `ConcurrentHashMap`, `CatalogViewModel` `_dialogState.update` + IO init + USB debounce, and duplicate `UsbMountReceiver` removal complete)*
  - **Goal**: Audit `AppDatabase`, DAOs, and repositories to guarantee zero user data loss from DB v4 across schema upgrades and ensure all queries run off the main thread.
  - **Target Files**:
    - `app/src/main/kotlin/com/joudjawadtube/player/data/local/AppDatabase.kt`
    - `app/src/main/kotlin/com/joudjawadtube/player/data/local/VideoDao.kt`
    - `app/src/main/kotlin/com/joudjawadtube/player/data/repository/VideoRepository.kt`
  - **Verification**:
    - `.\gradlew.bat :app:testDebugUnitTest`

- [x] **T15 — Local & USB Storage Scanner Resilience** *(Wave 4 SurfaceView player layout, `VideoCard` Coil/DB-free cleanup, and `KidsPicksScreen_Antigravity` main-thread IO removal complete)*
  - **Goal**: Harden USB OTG mount detection, SAF/DocumentFile enumeration, and system/boot partition filtering across Android TV and tablet storage providers.
  - **Target Files**:
    - `app/src/main/kotlin/com/joudjawadtube/player/data/scanner/LocalVideoScanner.kt`
    - `app/src/main/kotlin/com/joudjawadtube/player/data/scanner/StorageVolumeHelper.kt`
    - `app/src/main/kotlin/com/joudjawadtube/player/data/scanner/UsbMountReceiver.kt`
  - **Verification**:
    - `.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest`

- [x] **T16 — Unit & Regression Test Suite Expansion** *(Wave 3 Room schema export `4.json`, `MIGRATION_1_2`, non-destructive migration path, and `WatchTimerManagerTest_Antigravity` complete)*
  - **Goal**: Cover new settings defaults/persistence, parental gate validation, watch timer countdowns, YouTube fallback flags, and security checks with deterministic JUnit tests.
  - **Target Files**:
    - `app/src/test/kotlin/com/joudjawadtube/player/`
  - **Verification**:
    - `.\gradlew.bat :app:testDebugUnitTest`

- [x] **T17 — Lint, String Parity & ProGuard Audit** *(Wave 4 `AppContainer_Antigravity`, `CatalogAction_Antigravity`, `CatalogViewModel`/`MainActivity` refactor, `StrictMode`, tightened `proguard-rules.pro`, and 302/302 EN/AR string parity complete)*
  - **Goal**: Verify 100% key parity between `values/strings.xml` and `values-ar/strings.xml`, confirm Media3/Room/InnerTube ProGuard keep rules, and resolve critical Android Lint findings.
  - **Target Files**:
    - `app/src/main/res/values/strings.xml`
    - `app/src/main/res/values-ar/strings.xml`
    - `app/proguard-rules.pro`
  - **Verification**:
    - `.\gradlew.bat :app:lintDebug`

- [x] **T18 — Debug & Release APK Build Verification** *(Wave 4 LeakCanary debug dependency, `ParentalAndKidLockUiTest_Antigravity`, `CatalogViewModelTest_Antigravity`, `testDebugUnitTest` 50/50, `compileDebugAndroidTestKotlin`, `assembleDebug`, and `assembleRelease` complete)*
  - **Goal**: Assemble signed/unsigned debug and release APKs cleanly, verify sideload installer output (`JoudJawadTube-v*.apk`), and complete end-to-end TV + Tablet smoke testing (local 4K, YouTube InnerTube, background playback, parental gate, watch timer, updater).
  - **Target Files**:
    - `app/build.gradle.kts`
  - **Verification**:
    - `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`
    - `.\gradlew.bat :app:assembleRelease`

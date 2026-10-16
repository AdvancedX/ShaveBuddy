# Settings and in-app updates implementation plan

> **For agentic workers:** Execute inline with the executing-plans skill. Steps use checkboxes to track delivery.

**Goal:** Add the approved settings/update flow and fix the Today date heading.

**Architecture:** Keep local shave data independent. A dedicated activity-scoped update ViewModel owns state; a GitHub repository queries published releases and streams a verified APK into private cache. Android package inspection gates FileProvider installation.

**Tech Stack:** Kotlin, Compose, coroutines, HttpURLConnection, Android PackageManager, JUnit, Compose instrumentation.

**Spec:** ../specs/2026-10-10-settings-and-in-app-updates-design.md

## Global constraints

- Public AdvancedX/ShaveBuddy GitHub Releases is the only source; no tokens or arbitrary URL input.
- Include demo prereleases; only vX.Y.Z[-demo] tags, compare numeric components, require a project APK.
- Explicit check only, then automatic download and system installation; no background or silent installation.
- com.shavebuddy.demo, same signing certificate, increasing versionCode; first package versionCode 4.
- Cache-only file sharing, finite size/time limits, optional SHA-256 required when supplied, delete incomplete files.
- Preserve original main checkout and all local shave records.

## Task 1: Release selection and file/package validation

Files: update/ReleaseSelection.kt, update/ApkValidation.kt; matching unit tests.
Interface: selectUpdate(List<PublishedRelease>, currentVersion: String): ReleaseSelection; copyVerifiedApk(InputStream, File, Long?, String?, ...); validateApkIdentity(installed, candidate).

- [x] Write selection tests covering v0.10.0-demo versus v0.9.9, drafts, malformed tags, equal/old versions, wrong assets, and no available package. Example: assertEquals("v0.10.0-demo", (selectUpdate(releases, "0.9.9-demo") as ReleaseSelection.Update).release.tag).
- [x] Run ./gradlew testDebugUnitTest --tests '*update*'; observe missing-feature failures.
- [x] Implement numeric parsing and trusted project-asset selection; distinguish Latest from NoPackages.
- [x] Add file tests using temporary files and ByteArrayInputStream: assert no file survives wrong length, wrong digest, size cap, interruption, or cancellation. Test identity mismatch branches independently.
- [x] Implement streaming SHA-256, length/cap enforcement, cleanup and identity rules; rerun unit tests.

## Task 2: Network and update state machine

Files: update/GitHubUpdateRepository.kt, update/AndroidApkInspector.kt, update/UpdateViewModel.kt; UpdateViewModelTest.kt and GitHubReleasesTest instrumentation.
Interface: UpdateRepository.findUpdate(), download(release, progress), clearCache(); UpdateViewModel.check(), cancel(), installationRequested(), installationFailed().

- [x] Test state transitions with controlled repository responses: Latest, NoPackages, progress, Ready, Failure, cancellation, duplicate taps and retry.
- [x] Implement bounded paginated GitHub metadata requests, HTTPS redirects and bounded streamed download; inspect package/version/signers before Ready.
- [x] Retain operation in ViewModel scope across navigation/rotation; clear stale cache on process creation and when cleared. No automatic network calls.
- [x] Run focused unit tests and compile Android instrumentation tests.

## Task 3: Navigation, settings and adaptive date heading

Files: ui/ShaveApp.kt, ui/SettingsScreen.kt, ui/Shared.kt, ui/TodayScreen.kt, ui/CalendarScreen.kt, ui/GearScreen.kt, MainActivity.kt, strings.xml, AndroidManifest.xml, update_paths.xml, build.gradle.kts; SettingsFlowTest.kt, DateHeaderTest.kt.

- [x] Add UI tests: settings entry exists on each tab; bottom tabs hidden in settings; back and activity recreation preserve originating tab/calendar selection. Header test measures line count for both date parts at narrow widths and fontScale 2.
- [x] Save screen state with rememberSaveable and SaveableStateHolder; shared 48dp settings action in each header. Dedicated settings UI renders all update states.
- [x] Wire system install permission launcher and FileProvider ACTION_VIEW installer; return from permission continues installation once, cancelling retains Continue install. Validate again before every installer launch.
- [x] Measure complete date and weekday units, wrap as units and move illustration below when necessary. Provide the full date semantics.
- [x] Bump to 0.1.3-demo/versionCode 4, preserve default local debug signing; manifest only INTERNET/REQUEST_INSTALL_PACKAGES and narrow FileProvider path.

## Task 4: Verification and handoff

Files: docs/releases/0.1.3-demo.md, docs/demo-verification.md.

- [x] Run testDebugUnitTest, lintDebug, assembleDebug and connectedDebugAndroidTest on the available emulator.
- [x] Visually inspect October 10 Saturday at normal and large fonts and dark theme; retain screenshots.
- [x] Check APK identity/version/signature and emulator FileProvider/installer integration. Record real-device upgrade/data-preservation as pending when no device is connected.
- [x] Request review, fix material issues, re-run affected checks; keep result in isolated worktree for user review.
- [x] Document future tag v0.1.3-demo and APK ShaveBuddy.apk: existing display name 0.1.2-demo uses legacy tag 0.1.2-release and is intentionally excluded. Do not publish a release in this task.

## Execution results

All four tasks implemented inline. Final verification: 34 unit tests and 18 emulator tests passed; lint 0 errors / 17 warnings; Debug APK versionCode 4, original signing certificate. No physical device was available, so true system upgrade/data-preservation verification remains explicitly pending as allowed by the spec. Result and artifacts remain in the native isolated worktree; no Release was published.

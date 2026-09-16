# Focus Lock project state — 2026-09-16

This is the stable handoff for future app enhancements. Read it alongside the
README and current source; source code remains authoritative when details drift.

## Current product

- Native Kotlin/Jetpack Compose Android app. The Play package name
  (`applicationId`) is `com.vasimakram.focuslock`; the internal Kotlin
  package (`namespace`) stays `com.focuslock.app`. This split is deliberate
  and supported: AGP expands the manifest's relative component names against
  the namespace, so the merged manifest ships `package=com.vasimakram.focuslock`
  with `android:name="com.focuslock.app.MainActivity"` and friends. Source
  packages, imports, the proguard rule and the accessibility config's
  `settingsActivity` all correctly keep the `com.focuslock.app` prefix.
- Users select up to two apps to block and set one weekly time window per app,
  including windows crossing midnight. Home shows each app's window; app and
  schedule editing are available there. Multiple windows are deferred until Pro.
- Choose apps to block lists launchable apps and places selected apps first.
  Android's package visibility filtered the list after `QUERY_ALL_PACKAGES` was
  removed for Play preparation. A narrow MAIN/LAUNCHER `<queries>` declaration
  fixed the missing apps. Individual scheduling did not cause that filtering.
- During an active window, an Accessibility service detects the foreground app
  and an overlay shows a blocking screen. A foreground service and notification
  help keep protection running. OnePlus background-activity instructions live
  under Settings > Background protection.
- The blocked user can go back or complete an escalating wait for a ten-minute
  unlock. Wait lengths start at one minute, then three, seven, fifteen and
  thirty-plus minutes. Cancelling an incomplete wait does not increase the daily
  unlock count. The wait timer is persisted and checkpointed.
- If a protected app is already open as a schedule window begins, the engine's
  one-second reevaluation detects the boundary and presents the block screen.
- App selection and enabled-schedule saving are allowed when required access is
  missing. A setup dialog says the choices were saved but blocking cannot work
  until Accessibility and overlay access are enabled, and opens Permissions.
- The Permissions page explains Accessibility, Display over other apps and
  Notifications. Granted access has a Manage access button that opens Android
  settings so the user can revoke it there. The Accessibility path shown for the
  user's phone is Accessibility > General > Downloaded apps > Focus Lock
  Protection. APK-installed builds may need App info > three-dot menu > Allow
  restricted settings; help for this is in the permission screen.
- Privacy policy content is visible inside the app from Settings > Information.
  Its row matches About; the duplicate link on About was removed. Support/Buy
  me a coffee is hidden for a future release.
- Settings > Security has optional Lock Focus Lock. It uses the phone's existing
  biometric or PIN/pattern/password rather than storing a separate PIN. Changing
  the switch requires authentication. Opening the app prompts after it has been
  backgrounded. A short transitional loading state prevents the former lock
  screen flash while Android closes its authentication prompt.

## Important implementation choices

- The app is offline: Room v6 stores block lists, one unique per-app window,
  waits, grants and events; DataStore stores theme, onboarding, battery guidance
  and app-lock flag. Old shared-schedule storage and all migration code were
  removed at the user's request because the app is still in development.
  Room now exports its schema to `app/schemas` (v6 committed as the first
  released baseline) and destructive fallback is limited to debug builds.
  Release builds carry no destructive fallback, so every schema change from v6
  onwards needs a real `Migration` added to `FocusLockDatabase.MIGRATIONS`, its
  exported JSON committed, and a `MigrationTestHelper` test. Debug installs
  still reset their Room data on schema churn; permissions and DataStore
  settings remain.
- `ProtectionEngine` evaluates accessibility events and a one-second tick.
  `BlockDecisionEngine` and `ScheduleEvaluator` hold the core decision rules.
- `MainActivity` owns phone authentication via AndroidX BiometricPrompt. Android
  11+ permits weak biometric or device credential. Android 8–10 uses device
  credential because the combined authenticator mode is not supported there.
  Authentication gates the real Compose navigation UI, not the blocking overlay.
- A brief delayed relock on Activity stop avoids flicker when the system phone
  credential UI momentarily stops the host. The relock is cancelled if the host
  immediately resumes; if it stays in the background, the app relocks.
- Required blocking permissions are Accessibility and overlay. Notifications
  support background protection but are not part of the critical gate. Unused
  Usage Access, `QUERY_ALL_PACKAGES` and direct battery-exemption requests were
  removed from the manifest for Play policy review. MAIN/LAUNCHER `<queries>`
  now provides the app picker's limited package visibility. Android backup is
  disabled for app data.

## Build and device

- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`, AGP 8.10.1, Gradle 8.11.1.
- Release builds are minified and resource-shrunk by R8, and are signed from a
  `release` signing config that reads a gitignored `keystore.properties`
  (template in `keystore.properties.sample`). Without that file the release
  build still compiles but comes out unsigned. `*.jks`, `*.keystore` and
  `keystore.properties` are gitignored. The upload keystore has not been
  created yet.
- R8 minification exposed a real crash that debug builds never showed:
  `IllegalStateException: CompositionLocal LocalLifecycleOwner not present`.
  lifecycle-runtime-compose 2.8.2 resolves its own `LocalLifecycleOwner` through
  compose-ui 1.6.8's provider (Compose BOM 2024.06.00); the two CompositionLocals
  are separate until Compose UI 1.7, which unifies them. R8 stripped
  `AndroidCompositionLocals_androidKt$LocalLifecycleOwner$1`, so every
  `collectAsStateWithLifecycle` call threw. That is six files including
  `OverlayRoot`, so the whole release build was unusable. A keep rule for
  `androidx.compose.ui.platform.AndroidCompositionLocals_androidKt**` in
  `proguard-rules.pro` fixes it; remove that rule if the Compose BOM ever moves
  to 1.7+. Diagnose obfuscated release crashes with
  `android-sdk/cmdline-tools/latest/bin/retrace.bat` against
  `app/build/outputs/mapping/release/mapping.txt`; `usage.txt` beside it lists
  what R8 removed.
- Verified on device on 2026-09-16 with a release APK signed by the local debug
  key (`apksigner` + `~/.android/debug.keystore`) purely for testing: Home and
  the app picker render, the app list is complete, and no crash occurs. The
  user then confirmed the release build works as expected on the device. Re-test
  once after the first build signed with the real upload key, since that
  produces a different signing certificate.
- Local SDK at `D:/research/focuslock/android-sdk` has Android Platforms 35/36
  and Build Tools 34/35/36. `local.properties` points to that SDK and is ignored
  by Git. The SDK itself and `gradle-dist` are also ignored.
- Build/test: `gradlew.bat :app:assembleDebug :app:testDebugUnitTest --no-daemon`.
  The clean single-window schema, scheduling and app-picker build and unit
  tests passed on 2026-09-15.
- Connected OnePlus phone serial: `7028cafc`. Debug APK installs with
  `android-sdk/platform-tools/adb.exe -s 7028cafc install -r
  app/build/outputs/apk/debug/app-debug.apk`. The latest build was installed
  successfully, including the clean v6 development schema. Opening Room data
  on this update resets old locally selected apps and schedules. The user
  confirmed the unlock-screen flash is fixed and all apps appear in the picker.

## Google Play status and next work

- The user has a personal Play Console developer account. Identity verification
  completed and the `Focus Lock` app entry was created on 2026-09-16. Nothing has
  been uploaded to any track yet; do not claim Focus Lock is published or
  distributed to testers.
- Play Console gates the tracks in this order: Internal testing is available
  immediately; Closed testing stays locked behind the `Finish setting up your
  app` tasks (store listing plus the App content declarations); production
  access then needs the closed test. So the listing and declarations are the
  critical path, not parallel work, because they unlock the 14-day clock.
- A signed `app-release.aab` (4.0 MB) is built and verified, signed with
  `CN=Vasim Akram Shaik, O=Focus Lock, L=Hyderabad, ST=Telangana, C=IN`. The
  upload keystore is `focuslock-upload.jks` at the project root with alias
  `focuslock-upload`; it and `keystore.properties` are gitignored.
- Production access requires a closed test with at least 12 testers opted in for
  at least 14 days. The dashboard showed 0 testers opted in on 2026-09-16. That
  clock is the schedule's long pole.
- For friend testing, create the app entry and an Internal testing track after
  account verification. Internal testing can invite up to 100 testers by Google
  email and distribute via Play Store opt-in link.
- Play assets drafted on 2026-09-16 under `play/`: `icon-512.png` (rendered from
  the adaptive icon's vector path, cropped to the central 72 of 108 so it matches
  the launcher), `privacy-policy.md` and a ready-to-host `index.html` of the same
  text, `store-listing.md` (name/short/full description, all within Play's limits)
  `declarations.md` (Accessibility justification plus demo-video script, the
  specialUse justification, and the Data safety answers), and
  `feature-graphic-1024x500.png` (same lock path, wordmark and tagline on the
  app's dark palette; no device frames, ratings or calls to action, which Play
  prohibits). Both privacy policy
  files still carry a `SUPPORT_EMAIL` placeholder that must be replaced before
  hosting. Screenshots are still to be produced.
- Still needed: Play listing
  text, icon/graphics/screenshots and contact address; public privacy-policy URL;
  accurate Accessibility service and foreground-service special-use declarations
  with demonstration videos; Data safety and other App content questionnaires;
  tester list and rollout. Keep signing credentials out of Git and back them up.
- The first uploaded bundle permanently binds the Play package name, so
  `com.vasimakram.focuslock` cannot change after that upload.
- The app currently has `versionCode = 1` and `versionName = 1.0.0`. Increase
  versionCode for every subsequent Play upload. A Play-signed install may use a
  different signing certificate from the existing USB debug APK, so testers
  might have to uninstall the debug build before their first Play installation.

## Agreed feature roadmap — 2026-09-15

These are the user's product decisions. The Free one-window rule is enforced;
Pro and Billing are pending.

1. App-specific schedules store one time window per app. Room v6 enforces that
   with a unique package index. The runtime checks the foreground app's enabled
   window. Home displays each app's window;
   Schedule can set, edit, disable and remove it. Removal uses a trash icon by
   the window row and a confirmation dialog. The agreed Free tier is two apps
   with one window each. Pro will permit more apps and multiple windows per app
   when Billing and a future schema update are implemented.
2. Add Pro with monthly and yearly Google Play subscription base plans. Pro
   allows more blocked apps, multiple schedules per app and Child Protect.
3. Do not require Google sign-in to use Free. Restore an active subscription by
   querying Google Play Billing on app startup/resume and via a visible Restore
   purchases action, using the purchasing Google Play account. Google sign-in
   could be optional when account and cloud backup features are built. Local
   configuration will not return after uninstall until backup/sync is added.
   The user proposed Supabase for a small subscription-verification backend and
   possible later Google authentication and cloud backup. Supabase has not been
   created or integrated. Google Play still processes subscription payments;
   Supabase would verify purchase tokens with Google and store entitlements.
   Keep core protection and schedules operational from local storage offline.
4. Child Protect: a parent picks a list of apps and starts a ten-minute session
   in which those apps are accessible. When time is up, show a clear neutral
   time-limit message (do not say the phone is broken). Keep Child Protect
   active until a parent explicitly removes it. Parent actions should use the
   phone's existing authentication; the user does not want a separate parent
   PIN. Define and test behavior across app backgrounding,
   reboot, permission revocation and device limitations. Accessibility/overlay
   can block selected apps but cannot guarantee control of the whole phone.
5. Friends should test the app through Google Play's Internal testing track
   before a wider release. Subscription testers should also be Play license
   testers so test purchase methods do not charge them.

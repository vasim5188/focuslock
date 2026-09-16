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
- Verified on device on 2026-09-16. First with a release APK signed by the local
  debug key (`apksigner` + `~/.android/debug.keystore`) to test R8 before the
  upload key existed: Home and the app picker rendered, the app list was
  complete, no crash. Then again with the real upload-key-signed
  `app-release.apk`, which installed and ran cleanly. The user confirmed the
  app works as expected.
- Release builds are produced with `gradlew.bat :app:bundleRelease` (the `.aab`
  for Play) or `:app:assembleRelease` (a signed APK for sideloading). Without
  `keystore.properties` the release output is `app-release-unsigned.apk`, which
  Android refuses to install — "package appears to be invalid" means unsigned,
  not broken. Sideloading a debug-signed APK is separately blocked by Play
  Protect; `adb install` bypasses that.
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

## Google Play status and next work — 2026-09-16

### Account, app entry and signing

- Personal Play Console developer account; identity verification complete. The
  `Focus Lock` app entry was created on 2026-09-16.
- The Play package name is `com.vasimakram.focuslock`, bound permanently by the
  first upload and no longer changeable.
- The upload keystore is `focuslock-upload.jks` at the project root, alias
  `focuslock-upload`, DN `CN=Vasim Akram Shaik, O=Focus Lock, L=Hyderabad,
  ST=Telangana, C=IN`. It and `keystore.properties` are gitignored and have
  never been committed at any point in history — the GitHub repo
  `vasim5188/focuslock` is public, so that matters. The user must keep an
  off-machine backup of the `.jks` and its password.
- Play App Signing is enabled, so builds delivered by Play carry Google's
  certificate rather than the upload key. Moving between a locally signed build
  and a Play-delivered one is therefore a fresh install, not an update, and
  resets Accessibility and overlay grants and all Room data each time.

### Tracks

- An Internal testing release was published on 2026-09-16 with the signed
  `app-release.aab` (version code 1, 1.0.0, 1.5 MB install size). Three email
  lists of one tester each exist; consolidate into a single list before closed
  testing.
- Two upload warnings are expected and benign. One says the track has no
  testers. The other reports native code without debug symbols: that is
  `libdatastore_shared_counter.so` from androidx.datastore across four ABIs,
  not app code, so it will appear on every upload and can be ignored.
- Closed testing stays locked until the `Finish setting up your app` tasks are
  done. Production access then needs at least 12 testers opted in for at least
  14 continuous days; 0 were opted in on 2026-09-16. That clock is the long
  pole, and it cannot start until the listing and App content are complete.
- Internal testing does not count toward the 12-tester requirement, but it
  takes up to 100 testers immediately and is the right channel for friends.
- Newly added testers commonly see "Item not found" after opting in and tapping
  the download button; on 2026-09-16 it resolved by itself after a wait. It is
  propagation, not a broken track. The other cause worth checking first is an
  account mismatch: the opt-in happens in the browser while the download opens
  the Play Store app, so a tester signed into a different account there gets the
  same screen. Warn testers to wait and retry rather than give up, since the
  14-day closed-test clock depends on them staying opted in.
- `versionCode` must increase on every subsequent upload.

### Store assets, committed under `play/`

- `icon-512.png` and `feature-graphic-1024x500.png`, both rendered by a small
  Pillow script that parses `ic_launcher_foreground.xml`'s path data directly
  and flattens the Béziers, so the artwork matches the installed launcher icon
  exactly. The icon crops to the central 72 of the 108 adaptive-icon canvas,
  which is the safe zone the launcher actually shows. The feature graphic
  deliberately carries no device frames, ratings or calls to action, all of
  which Play prohibits.
- `store-listing.md` — app name, short and full descriptions, all verified
  against Play's character limits (10/30, 78/80, 2063/4000).
- `declarations.md` — the Accessibility justification, a demo-video shot list,
  the `specialUse` foreground-service justification and the Data safety
  answers. The Accessibility text argues why no alternative API works, which is
  the part reviewers weigh: `UsageStatsManager` reports after the fact and
  cannot block before content is shown, and Android exposes nothing else that
  reports a foreground-app change.
- `privacy-policy.md` — the policy text as Markdown.

### Privacy policy

- Live at `https://vasim5188.github.io/focuslock/`, served from `docs/index.html`
  by GitHub Pages from `main`. Pages only serves a repo's root or `/docs`, which
  is why the page does not live under `play/`.
- Verified at 375px wide with no horizontal overflow and a working `mailto:`
  link. Contact address is `vasimakram.aem@gmail.com`; both the page and the
  Play listing contact can be changed later.
- An in-app copy is not sufficient for Play: App content requires a public URL,
  because the policy must be readable before installing.

### Still outstanding

- At least 2 phone screenshots; the block screen explains the product fastest.
- The Accessibility demo video, showing consent obtained before the grant.
- 12 testers recruited and opted in.
- The Console forms: Main store listing, then App content end to end.
- A custom domain was discussed and deliberately deferred; `github.io` is
  accepted by Play and buying a domain would only delay the 14-day clock.

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

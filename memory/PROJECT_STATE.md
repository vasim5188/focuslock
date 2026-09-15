# Focus Lock project state — 2026-09-15

This is the stable handoff for future app enhancements. Read it alongside the
README and current source; source code remains authoritative when details drift.

## Current product

- Native Kotlin/Jetpack Compose Android app, package `com.focuslock.app`.
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
  Room uses destructive fallback for earlier development databases: updating
  an existing debug install resets its Room data, including selected apps,
  schedules, wait progress, grants and event history. Android permissions and
  DataStore settings remain. Plan a deliberate migration strategy before a
  production Play release, when user data must be retained.
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

- The user created a personal Play Console developer account. Identity documents
  were uploaded and Google was reviewing them as of 2026-09-14. The contact
  phone verification remained locked pending identity approval. A screenshot
  no longer listed Android device verification, but check the dashboard again
  rather than assuming all account tasks are complete.
- `Create app` was disabled while verification was outstanding. Do not claim
  that Focus Lock is published or internally distributed yet.
- For friend testing, create the app entry and an Internal testing track after
  account verification. Internal testing can invite up to 100 testers by Google
  email and distribute via Play Store opt-in link.
- Still needed: permanent secure upload key and signed `.aab`; Play listing
  text, icon/graphics/screenshots and contact address; public privacy-policy URL;
  accurate Accessibility service and foreground-service special-use declarations
  with demonstration videos; Data safety and other App content questionnaires;
  tester list and rollout. Keep signing credentials out of Git and back them up.
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

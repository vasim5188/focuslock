# Focus Lock project state — 2026-09-15

This is the stable handoff for future app enhancements. Read it alongside the
README and current source; source code remains authoritative when details drift.

## Current product

- Native Kotlin/Jetpack Compose Android app, package `com.focuslock.app`.
- Users select up to two apps to block and set a weekly time window. Windows
  crossing midnight are supported. The home screen shows protected apps and
  schedule timing; app and schedule editing are directly available there.
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
- Privacy policy content is visible inside the app. Support/Buy me a coffee is
  hidden for a future release.
- Settings > Security has optional Lock Focus Lock. It uses the phone's existing
  biometric or PIN/pattern/password rather than storing a separate PIN. Changing
  the switch requires authentication. Opening the app prompts after it has been
  backgrounded. A short transitional loading state prevents the former lock
  screen flash while Android closes its authentication prompt.

## Important implementation choices

- The app is offline: Room stores block lists, schedule, waits, grants and
  events; DataStore stores theme, onboarding, battery guidance and app-lock flag.
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
  Usage Access, broad package visibility and direct battery-exemption requests
  were removed from the manifest for Play policy review. Android backup is
  disabled for app data.

## Build and device

- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`, AGP 8.10.1, Gradle 8.11.1.
- Local SDK at `D:/research/focuslock/android-sdk` has Android Platforms 35/36
  and Build Tools 34/35/36. `local.properties` points to that SDK and is ignored
  by Git. The SDK itself and `gradle-dist` are also ignored.
- Build/test: `gradlew.bat :app:assembleDebug :app:testDebugUnitTest --no-daemon`.
  The latest app-lock flicker build and unit tests passed on 2026-09-15.
- Connected OnePlus phone serial: `7028cafc`. Debug APK installs with
  `android-sdk/platform-tools/adb.exe -s 7028cafc install -r
  app/build/outputs/apk/debug/app-debug.apk`. The latest build was installed
  successfully, and the user confirmed the unlock-screen flash is fixed.

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

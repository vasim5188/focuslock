# Focus Lock — Native Android App

Focus Lock helps you stop opening distracting apps on autopilot. When you open a
protected app during your focus schedule, Focus Lock shows a full-screen lock
screen and asks you to make a conscious choice: **Go back**, or deliberately
**Wait** for escalating amounts of time to earn a temporary 10-minute grant.

This is a **real native Android app** (Kotlin + Jetpack Compose). The blocking
system is genuine — it uses an `AccessibilityService` to detect the foreground
app and a `SYSTEM_ALERT_WINDOW` overlay to block it. Nothing is faked.

## Tech stack
- Kotlin, Jetpack Compose (Material 3)
- ViewModel + Kotlin Coroutines / Flow
- Room (local database) + DataStore (preferences)
- AccessibilityService, Foreground Service, BroadcastReceivers, Notifications
- minSdk 26, targetSdk 35, AGP 8.6, Gradle 8.9
- No backend, no account, no analytics — 100% offline core

## Build

### Android Studio (recommended)
1. Open the project root in Android Studio (Koala or newer).
2. Let it sync; it creates `local.properties` with your SDK path automatically.
3. Run the `app` configuration on a device/emulator (API 26+).

### Command line
```bash
# local.properties must point to your Android SDK, e.g. sdk.dir=/path/to/Android/sdk
./gradlew :app:assembleDebug            # builds app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest        # runs the unit tests
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## First-run setup on device
Focus Lock needs these permissions (explained in-app under Onboarding → Permissions):
1. **Accessibility Service** — detect which app is in the foreground.
2. **Display over other apps** — draw the lock screen.
3. **Notifications** — the ongoing protection notification.

Also follow the **Battery optimization** guidance for your device (Xiaomi, Oppo,
Realme, Vivo, Samsung, OnePlus, etc.) so the background service is not killed.

## How the core loop works
1. You protect up to 2 apps and set a schedule (supports midnight-crossing windows).
2. Opening a protected app during the schedule shows the block screen — this is a
   `BLOCKED_ENCOUNTER` and does **not** increment any counter.
3. **Go back** → `RESISTED`, counter unchanged.
4. **Wait** → `UNLOCK_STARTED`, the daily counter stays unchanged and a
   persistent timer starts. Required wait escalates: 60s → 3m → 7m → 15m → 30m+.
   The counter is **global** across all protected apps and resets at local midnight.
5. **Cancel** a wait → `WAIT_CANCELLED`, counter is not touched.
6. Wait completes → `UNLOCK_COMPLETED`, the counter increments once and a 10-minute `ActiveGrant` is created.
7. Grant expires → `GRANT_EXPIRED`, the app is blocked again.

Wait timers are measured in monotonic time (`elapsedRealtime`) and checkpointed
to the database every few seconds, so they survive leaving the screen, calls,
process death and reboot without restarting. A reboot or a clock change breaks
the boot-session anchor, which discards the unbanked delta rather than trusting
it — so moving the clock can only ever cost progress, never grant it. Time with
the device powered off does not count toward a wait.

## Project layout
```
app/src/main/java/com/focuslock/app/
├── data/            Room entities/DAOs/database, DataStore, repository (counter rules)
├── domain/          Pure logic: ScheduleEvaluator, EscalationPolicy, WaitCalculator,
│                    BlockDecisionEngine, event model, rewarded-ad abstraction (future)
├── service/         ProtectionEngine, AccessibilityService, ForegroundService, OverlayHost
├── receiver/        BootReceiver, PackageChangeReceiver
├── ui/              Compose screens (onboarding, home, picker, schedule, permissions,
│                    settings, battery, about) + overlay block/waiting screens + theme
└── util/            PermissionChecker, OemHelper, NotificationHelper, InstalledAppsProvider
app/src/test/        Unit tests for schedule/escalation/wait/decision logic
```

## What is intentionally NOT in this MVP (architecture reserved)
Billing/Pro, rewarded ads, Deep Focus, statistics dashboard, cloud/login. These
are stubbed behind interfaces/flags (e.g. `RewardedUnlockProvider`,
`deepFocusActive`) so they can be added without reworking the runtime.

## Building
Standard Android build — open in Android Studio, or from the command line with
an Android SDK installed (`ANDROID_HOME` set):

```
./gradlew :app:assembleDebug     # build the APK
./gradlew :app:testDebugUnitTest # run the unit tests
```

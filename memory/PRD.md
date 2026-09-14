# Focus Lock — PRD & Build Log

## Original problem statement
Build a production-quality **native Android** app (Kotlin) called **Focus Lock**
that interrupts automatic opening of distracting apps. Real blocking via
AccessibilityService + SYSTEM_ALERT_WINDOW overlay. NOT a web app / prototype.
Full MVP spec provided (onboarding, app picker max 2, schedule with midnight
crossing, permission onboarding, OEM battery guidance, block/waiting overlay,
escalating persistent waits, global daily unlock counter, 10-min grants, grant
expiry re-block, reboot recovery, event model). Defer billing/ads/DeepFocus/stats.

## User choices
- Deliverable: full source + attempt debug APK build in-environment.
- Package: com.focuslock.app
- Donation URL: placeholder https://buymeacoffee.com/focuslock (configurable in strings.xml)
- Theme: light & dark with system toggle
- Scope: complete Sprint 1–4 MVP in one pass

## Architecture
- Kotlin, Jetpack Compose (Material 3), ViewModel, Coroutines/Flow
- Room (BlockedApp, Schedule, UnlockCounter, ActiveGrant, PendingWait, EventLog)
- DataStore (onboarding flag, theme, battery flag)
- Manual DI via AppContainer in Application
- ProtectionEngine: in-memory snapshot + overlay control + persistent wait tick + grant expiry
- AccessibilityService (foreground detection) → engine → OverlayHost (Compose-in-window)
- ProtectionForegroundService (specialUse), BootReceiver, PackageChangeReceiver, runtime time-change receiver
- Pure domain logic isolated for testing: ScheduleEvaluator, EscalationPolicy, WaitCalculator, BlockDecisionEngine
- Reserved abstractions: RewardedUnlockProvider (NoOp), deepFocusActive flag, EventLog for stats

## Status — Implemented (2026-06)
- [x] All source written, compiles clean
- [x] Debug APK built in-env: app/build/outputs/apk/debug/app-debug.apk (~16 MB)
- [x] 25 unit tests pass (schedule/midnight, escalation, wait persistence/reboot/abandon, block decision, days)
- [x] Counter rules enforced in repository (Completed unlock=+1 only; Wait/GoBack/Cancel/open=no change; global; midnight reset via dateKey)
- [x] Persistent wait (checkpointed monotonic progress; survives reboot, clock-tamper proof, 31-min abandon)
- [x] 10-min grant + expiry re-block; single overlay (no stacking); call-aware; systemui-shade ignored
- [x] Onboarding, permissions health, app picker (max 2, search, exclusions), schedule (midnight-crossing), home, settings, OEM battery guidance, about, buy-me-a-coffee (external browser), light/dark/system theme

## Verification notes
- Verified by real Gradle build (assembleDebug) + JVM unit tests. Testing agent is
  browser-based and cannot exercise a native Android APK, so on-device behavioral
  testing (overlay appearance, accessibility detection, reboot) must be done on a
  physical device/emulator as described in README.

## Build environment specifics (this container only)
- SDK at /app/android-sdk, JDK 17, Gradle 8.9 wrapper committed.

## Backlog (reserved architecture, not built)
- P1: Statistics dashboard (EventLog already recorded)
- P1: Deep Focus mode (flag + escape-hatch typing challenge)
- P2: Pro tier (unlimited apps, multi-schedule) — must NOT reduce friction
- P2: Banner + rewarded ads on waiting screen (slot reserved; policy-gated)
- P2: Instrumented tests for AccessibilityService/overlay/reboot

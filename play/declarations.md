# Focus Lock — Play Console declarations

The two declarations below carry the real approval risk. Everything else on the
App content page is routine.

---

## 1. Accessibility Service declaration

Google restricts the Accessibility API to accessibility purposes unless you
disclose a different use, justify it, and get user consent. Expect to supply
this text plus a demo video.

### Why the app needs the API

```
Focus Lock is a digital wellbeing app that blocks user-selected apps during
time windows the user configures. To do this it must know, in real time, which
app has just come to the foreground, so it can present its blocking screen at
the moment a protected app is opened.

Focus Lock uses AccessibilityService with TYPE_WINDOW_STATE_CHANGED and
TYPE_WINDOWS_CHANGED events and reads only the package name of the foreground
app. It does not read window content, does not capture text the user types, and
does not access passwords, messages or any on-screen information. Nothing
observed through the service is collected, transmitted or shared. The app makes
no network requests of any kind and has no servers.

The user grants this permission explicitly, after an in-app screen that explains
what the service does and why, and can revoke it at any time from Android
Settings or from the app's own Permissions screen.
```

### Why no alternative API works

```
UsageStatsManager (Usage Access) was evaluated and removed from the app. It
reports usage statistics after the fact rather than delivering foreground
events, so detection requires continuous polling and still arrives too late to
block the app before content is shown. Android provides no other API that
notifies an app when a different app enters the foreground. AccessibilityService
is the only mechanism that supports this use case.
```

### Demo video script

Unlisted YouTube is fine. Keep it under two minutes, no narration needed.

1. Fresh launch — show the onboarding screen explaining what the app does.
2. Permissions screen — show the plain-language explanation of the Accessibility
   service *before* granting.
3. Tap through to Android Settings and enable **Focus Lock Protection**.
4. Return to the app, choose a blocked app (YouTube), set a window covering now.
5. Press Home, open YouTube — the block screen appears immediately.
6. Show "go back" and the escalating wait.
7. Finish on Permissions, using **Manage access** to revoke, showing the user
   stays in control.

The reviewer needs to see consent obtained *before* the grant, and the service
used for exactly the purpose declared.

---

## 2. FOREGROUND_SERVICE_SPECIAL_USE justification

The manifest declares `android:foregroundServiceType="specialUse"` with subtype
`focus_lock_app_blocking`.

```
Focus Lock runs a foreground service for the entire duration of a user-configured
blocking window so that Android does not stop the app while protection is meant
to be active. If the process is killed, the Accessibility-driven blocking engine
stops evaluating and the user's chosen apps stop being blocked, which silently
defeats the feature the user switched on.

No existing foregroundServiceType describes this work. The service does not play
media, sync data, access location, camera, microphone or health data, does not
project the screen, manage a connected device, or handle calls or messaging. Its
only job is to keep the app's own blocking engine alive and to show the ongoing
notification that tells the user protection is running. specialUse is therefore
the only accurate type.
```

---

## 3. Data safety answers

Focus Lock genuinely collects nothing, so this section is short. Answer exactly:

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | N/A — no data is transmitted |
| Do you provide a way for users to request that their data is deleted? | Uninstalling the app deletes all local data |

Do not tick any data category. The app has no network code at all, so any
"collected" or "shared" answer would be inaccurate.

Note the distinction Google draws: data that never leaves the device is **not**
"collected" for Data safety purposes. Locally stored blocked-app lists and
schedules therefore do not need declaring.

---

## 4. The rest of App content

| Item | Answer |
|---|---|
| Privacy policy | URL of the hosted `play/index.html` |
| App access | All functionality available without restrictions — no login |
| Ads | No ads |
| Content rating | Complete questionnaire; expect Everyone |
| Target audience | 18+ avoids the extra Families policy requirements |
| News app | No |
| COVID-19 contact tracing | No |
| Data safety | See above |
| Government apps | No |
| Financial features | No |
| Health | No |

# MASTER BUILD PROMPT

## Project: NumbPhone — Focus & Digital Wellbeing Android Launcher

You are an expert Android engineer, Kotlin architect, UI/UX engineer, and QA engineer.

Your task is to design, BUILD, maintain, and expand a production-quality Android application called:

**NumbPhone** (formerly Minimal Phone)

The application is a distraction-resistant minimalist Android launcher and digital wellbeing application designed to transform modern smartphones into intentional tools. It provides the simplicity, calm, and focus of a "dumb phone" while preserving the reliability of essential communication and utility tools.

The application replaces the default Android home screen, displays only permitted applications, enforces daily app usage limits, schedules automated focus sessions, blocks distracting applications, provides notification digests, tracks usage analytics, and introduces deliberate exit friction when the user attempts to leave an active focus session or exceed daily time limits.

Do NOT build this as a simple demo or mockup.

Build a real, robust, modular Android application using modern Android APIs, real Room database persistence, DataStore preferences, system services, and clean multi-module architecture.

---

# 1. PRODUCT VISION

The core philosophy of NumbPhone is:

> *"Make intentional actions easy and impulsive actions deliberate."*

Modern smartphones are engineered for addiction through infinite feeds, red notification badges, colorful icon grids, and friction-free impulsive app launches. NumbPhone breaks this cycle through a dual-defense architecture:

1. **Proactive Friction**: Typography-first monochrome interface, removal of distracting feeds/badges, hidden status bar (Immersive Mode), and intentional app pinning.
2. **Enforced Boundaries**: Daily app usage budgets with auto-closing countdown alerts, strict focus sessions with adaptive exit delays, background notification digests, and system-level display desaturation (Grayscale mode).

The application does NOT attempt to permanently trap the user inside the launcher or make the device impossible to recover. Essential functionality is always guaranteed:

* Emergency calling (112, 911, SOS launchers)
* Phone & speed-dial quick contacts
* Messages / SMS
* Camera
* Clock & Alarms
* Contacts
* Settings
* Navigation / Maps where configured
* Explicitly designated user essential applications

On stock Android, users can ultimately disable permissions or switch launchers. The product therefore focuses on:

1. **Deliberate Friction**: Calibrated countdown delays (5s, 30s, 2m, 10m) giving users time to reconsider impulsive urges.
2. **Daily Usage Limits**: Per-app time budgets with 10-second countdown warnings, automatic app closing, and lock until tomorrow.
3. **Reliable Interception**: Central launch pipeline backed by an Accessibility Service backstop.
4. **Banking App Security**: Zero window-content inspection (`canRetrieveWindowContent="false"`) ensuring high-security banking/financial apps run without conflict.
5. **Calm Minimalist UI**: High-contrast typography, zero icon grids, and OLED black / E-ink paper / warm amber themes.
6. **Local-First Privacy**: 100% on-device processing with zero telemetry, zero cloud tracking, and local JSON backup/restore.

---

# 2. BRAND IDENTITY & ASSETS

NumbPhone uses a distinctive brand visual system optimized for Android launchers, system contexts, documentation, and the Google Play Store:

| Version | Asset Location | Target / Usage Context | Purpose |
|:---|:---:|:---|:---|
| **1. Flat Version** | `icons/flat version.png` | App branding, README, splash screen, GitHub | Clean primary brand artwork |
| **2. Monochrome Version** | `icons/monochrome version.png` | Android 13+ Themed Icons (`ic_launcher_monochrome`) | Integrates with Material You themed icons |
| **3. Black & White Version** | `icons/black and white.png` | Marketing, high-contrast dark/light documentation | Flexible brand variant |
| **4. Adaptive Android Icon** | `icons/adaptive android icon.png` | Android Launcher Adaptive Icon (`mipmap/ic_launcher`) | Primary icon displayed in system launcher settings |
| **5. Play Store 512×512** | `icons/512×512 Play Store version.png` | Google Play Store store listing (`docs/ic_playstore_512.png`) | High-resolution store listing graphic |

Brand drawables inside the application:
- `core/ui/src/main/res/drawable/numbphone_logo.png`: In-app headers and onboarding branding.
- `core/ui/src/main/res/drawable/numbphone_brand_bw.png`: High-contrast watermark and about dialogs.

---

# 3. PRIMARY TARGET USER

The primary target audience includes:

## Students & Examinees
Students preparing for competitive examinations who need to eliminate distractions from:
* YouTube
* Instagram & Reels
* TikTok
* X / Twitter
* Games
* Reddit & Forums
* Streaming & Entertainment

## Knowledge Workers & Engineers
Software developers, researchers, and writers who require sustained deep-work periods without notification pings and mindless phone checking.

## Digital Minimalists
Users seeking a permanent dumb-phone lifestyle on capable smartphone hardware without sacrificing modern camera quality, UPI/banking tools, or navigation.

---

# 4. CORE USER EXPERIENCE FLOW

The user onboarding and daily flow is structured as follows:

```text
Install NumbPhone
  ↓
Interactive 4-Step Onboarding Wizard
  ├── 1. Philosophy & Purpose ("Reclaim your focus. Pure monochrome simplicity.")
  ├── 2. System Permissions (Usage Stats, Accessibility Service, Default Launcher)
  ├── 3. Core Essentials (Auto-pinning 6 essential default apps)
  └── 4. Ready State ("Your calm, distraction-free environment is configured.")
  ↓
Set NumbPhone as Default Launcher (Intent to android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
  ↓
Dynamic Package Discovery via LauncherApps API
  ↓
Automatic Classification (Always Available vs. Managed)
  ↓
Launch Minimal Immersive Home Screen (Status bar hidden)
```

The home screen displays zero icon grids, zero notification badges, and zero infinite feeds:

```text
┌─────────────────────────────────────────────────────────┐
│ [📊 Analytics]     [⚙️ Quick Controls]    [⚙️ Settings] │
│                                                         │
│                        10:42 PM                         │
│                    Tuesday, Oct 24                      │
│                                                         │
│  [📞 Mom]   [📞 Alex]   [📞 Clinic]   (+ Quick Contacts) │
│                                                         │
│  Phone                                                  │
│  Messages                                               │
│  Camera                                                 │
│  Clock                                                  │
│  Contacts                                               │
│  Maps                                                   │
│                                                         │
│  + Add app                                              │
│                                                         │
│                                                         │
│                    Focus Mode: OFF                      │
│               [ Tap to Start Focus ]                    │
└─────────────────────────────────────────────────────────┘
```

---

# 5. TECHNOLOGY STACK

NumbPhone is built using modern Android development practices, ensuring high performance, low battery footprint, and clean separation of concerns:

* **Language**: Kotlin 2.0+ (Strict type safety, coroutines, sealed interfaces)
* **UI Toolkit**: Jetpack Compose & Material 3 (Monochrome design system, dynamic theming)
* **Dependency Injection**: Dagger Hilt (Multi-module DI containers)
* **Local Persistence**: Room Database (SQLite with type converters and reactive Flows)
* **Lightweight Preferences**: Jetpack DataStore Preferences (Settings, theming, onboarding flags)
* **Concurrency**: Kotlin Coroutines & Reactive `StateFlow` / `SharedFlow`
* **Navigation**: Navigation Compose with type-safe routing
* **Android OS APIs**:
  - `LauncherApps` API (Dynamic app detection, install/uninstall callbacks)
  - `PackageManager` (Metadata, component resolution, launch intents)
  - `UsageStatsManager` (Real-time screen-time and app launch tracking)
  - `AccessibilityService` (Non-intrusive backstop for bypass protection and daily limit auto-close)
  - `NotificationListenerService` (`MinimalNotificationListenerService` for notification digest)
  - `AlarmManager` (`SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM` for scheduled focus sessions)
  - `WindowInsetsControllerCompat` (Immersive Full-Screen mode hiding status bar)
  - `PowerManager` (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)
  - `SecureSettings` / Display API (`WRITE_SECURE_SETTINGS` for system grayscale)
* **Build & Release Optimization**: Gradle Kotlin DSL, Version Catalog (`libs.versions.toml`), ProGuard / R8 code shrinking and resource minification enabled.

---

# 6. MODULAR MULTI-MODULE ARCHITECTURE

NumbPhone enforces Clean Architecture across decoupled Gradle modules:

```text
                          NUMBPHONE ARCHITECTURE
                                    │
                           ┌────────┴────────┐
                           │                 │
                        UI Layer          Android
                     (Jetpack Compose)     APIs
                           │                 │
                     ┌─────┴─────┐     ┌────┴────────────┐
                     ↓           ↓     ↓                 ↓
                 Launcher    Settings  LauncherApps    UsageStats
                                       Accessibility   NotificationListener
                                       AlarmManager    Boot/Power
                     └──────┬───────────────┘
                            ↓
                        DOMAIN LAYER (Pure Business Logic)
                            │
       ┌──────────┬─────────┼──────────────┬──────────────┐
       ↓          ↓         ↓              ↓              ↓
  Launch Engine Focus   Time Limit   Bypass Engine Recovery System
             Scheduler    Engine
       │          │         │              │              │
       └──────────┴─────────┼──────────────┴──────────────┘
                            ↓
                         DATA LAYER
                            │
                     ┌──────┴──────┐
                     ↓             ↓
                   Room         DataStore
```

### Module Responsibilities:

| Module | Purpose & Components |
|:---|:---|
| **`:app`** | Application entry point (`MinimalPhoneApplication`), `MainActivity`, navigation graph (`MinimalPhoneNavHost`), services (`MinimalAccessibilityService`, `MinimalNotificationListenerService`), receivers (`MinimalBootReceiver`, `FocusScheduleReceiver`), and ProGuard release configuration. |
| **`:core:model`** | Pure Kotlin domain data models: `InstalledApp`, `AppCategory`, `AppRule`, `FocusSession`, `FocusMode`, `BlockedAttempt`, `ExitAttempt`, `FocusGoal`, `AppTimeLimit`, `DailyAppUsage`, `FocusPreset`, `FocusSchedule`, `DigestNotification`, `QuickContact`, `AppTheme`, `AppLaunchDecision`. |
| **`:core:common`** | Coroutine dispatchers, Result wrappers, logging abstractions (`MinimalLog`), `AccessibilityHelper`, `GrayscaleHelper`, `DndHelper`, `SystemHealthHelper`. |
| **`:core:data`** | Room database (`MinimalPhoneDatabase`), DAOs, `DefaultAppRepository`, `DefaultFocusSessionRepository`, `DefaultBlockedAttemptRepository`, `DefaultExitAttemptRepository`, `DefaultSettingsRepository`, `DefaultUsageStatsRepository`, `AppTimeLimitRepository`, `DefaultScheduleRepository`, `DefaultNotificationDigestRepository`, `DefaultContactRepository`, `DefaultEssentialAppRepository`, `AppScanner`. |
| **`:core:domain`** | Business logic use cases: `LaunchEngine` (`EvaluateAppLaunchUseCase`), `FocusEngine`, `FocusScheduler`, `BypassEngine`, `TimeLimitExpiryNotifier`, `ExitFrictionEngine`, `EmergencyAccessManager`, `LauncherPolicy`, `RuleEngine`, `BootRecoveryEngine`, `ExportBackupJsonUseCase`, `ImportBackupJsonUseCase`, `GetBehavioralInsightsUseCase`, `GetWeeklyWellbeingReportUseCase`. |
| **`:core:ui`** | Design system: `MinimalTheme`, `MinimalTypography`, shared dialogs (`BlockedAppDialog`, `ExitFrictionDialog`, `IntentReflectionDialog`, `SetTimeLimitDialog`, `TimeLimitExpiredDialog`), and UI components (`MinimalClockHeader`, `FocusStatusIndicator`, `MinimalAppItemRow`). |
| **`:feature:homescreen`** | Immersive launcher home screen: dynamic clock, quick contacts row, filtered allowed apps list, "+ Add app" button, settings gear, quick control & notification panel, active session indicator. |
| **`:feature:appslist`** | App management & classification: search bar with instant clear, filter chips (All, Home Apps, Always Available), per-app daily time limit dialog, focus block toggle, home pinning. |
| **`:feature:focussession`** | Focus session manager: quick presets (Sprint, Deep Work, Exam), recurring weekly schedules, custom goal setup, active session countdown, adaptive friction exit flow. |
| **`:feature:screentime`** | Screen-time metrics: daily total usage, category breakdown, per-app usage with direct time limit shortcuts, behavioral insights, and blocked impulse attempt history. |
| **`:feature:onboarding`** | 4-step first-run wizard: philosophy, runtime permission requests, default launcher setup, and core essential app confirmation. |
| **`:feature:settings`** | Categorized settings hub: Appearance & Themes, Focus & Detox, App Limits & Whitelist, System & Diagnostics ("NumbPhone Health"), Data Portability (JSON backup/restore), and Google Form suggestions link. |

---

# 7. ANDROID LAUNCHER & IMMERSIVE HOME SCREEN

### Manifest HOME Activity
`MainActivity` is declared in `app/src/main/AndroidManifest.xml` with:
```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTask"
    android:clearTaskOnLaunch="true"
    android:stateNotNeeded="true"
    android:showWhenLocked="true"
    android:turnScreenOn="true"
    android:showForAllUsers="true"
    android:theme="@style/Theme.MinimalPhone">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

### Immersive Full-Screen Mode
The Home screen hides the Android status bar using `WindowInsetsControllerCompat` to eliminate notification bar temptation:
```kotlin
val insetsController = WindowCompat.getInsetsController(window, window.decorView)
insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
insetsController.hide(WindowInsetsCompat.Type.statusBars())
```
When leaving the home screen to open apps or settings, the status bar is cleanly restored.

### Essential Default Apps Auto-Pinning
On first launch, NumbPhone automatically identifies and pins 6 essential system apps to the home screen:
1. Phone (`Intent.ACTION_DIAL`)
2. Messages (`Intent.CATEGORY_APP_MESSAGING`)
3. Camera
4. Clock / Alarm (`android.intent.action.SET_ALARM`)
5. Contacts (`Intent.CATEGORY_APP_CONTACTS`)
6. Settings

### Quick Control Center & Notification Panel
Tapping the header clock or the center tune icon opens a minimalist control overlay displaying:
- Current DND status and quick toggle.
- System Grayscale mode status.
- Notification Digest feed with unread count badge.
- Quick navigation to Diagnostics.

### "+ Add App" Button
At the bottom of the home screen app list, an "+ Add app" button allows instant navigation to App Rules & Classification to pin/unpin apps without drilling into settings (automatically hidden in Dumb Mode).

---

# 8. INSTALLED APP DISCOVERY & TAXONOMY

Never hardcode package lists. Use Android system APIs:
* **`LauncherApps`**: Retrieves launchable activities for the current user profile.
* **`LauncherApps.registerCallback()`**: Reacts dynamically to `onPackageAdded`, `onPackageRemoved`, and `onPackageChanged`.
* **`PackageManager`**: Resolves labels, categories, and system flags.

### Automatic Classification Logic
1. **Always Available (Essential)**:
   - System Emergency dialers (`EmergencyAccessManager.SYSTEM_EMERGENCY_PACKAGES`)
   - Intent-resolved essential categories (Dialer, SMS, Contacts, Clock, Calendar)
   - User-designated essential apps in `essential_apps` table
   - Non-entertainment Google utility apps (e.g., Maps, Drive)
2. **Managed**:
   - Social media (Instagram, TikTok, X, Facebook, Reddit)
   - Video & Entertainment (YouTube, Netflix, Prime Video, Twitch)
   - Games and leisure applications
   - Any app assigned a daily usage limit or blocked in focus sessions

*Note: YouTube is strictly classified as MANAGED despite having a `com.google.` package name.*

### App Filtering (`AppListFilter`)
In the App Management screen, apps can be filtered via horizontal chips:
- **`ALL`**: All installed launchable applications.
- **`HOME`**: Only apps currently pinned to the Home Screen.
- **`ESSENTIAL`**: Apps designated as Always Available.

---

# 9. CENTRAL APP LAUNCH PIPELINE

All app launches initiated from the launcher or quick controls pass through a domain-level decision pipeline:

```text
User taps app row
       ↓
LaunchAppUseCase()
       ↓
EvaluateAppLaunchUseCase()
       ↓
Is Emergency or Essential App?
       ├── YES → AppLaunchDecision.EmergencyAllow → Launch Intent
       │
       └── NO
            ↓
       Is Daily Time Limit Exceeded?
            ├── YES → AppLaunchDecision.Block ("Daily Limit Exceeded")
            │
            └── NO
                 ↓
            Is Focus Mode Active?
                 ├── NO → AppLaunchDecision.Allow → Launch Intent
                 │
                 └── YES
                      ↓
                 Is App Managed / Blocked in Focus?
                      ├── NO → AppLaunchDecision.Allow → Launch Intent
                      └── YES → AppLaunchDecision.Block / ShowFriction
                                   ↓
                             Show BlockedAppDialog / ExitFrictionDialog
```

---

# 10. DAILY APP USAGE LIMITS & TIME LIMIT EXPIRY ENGINE

To prevent endless scrolling during non-focus hours, NumbPhone includes a unified daily usage limit engine:

1. **Daily Limit Configuration**:
   - Configurable per-app limit in minutes via `SetTimeLimitDialog` (e.g. 15m, 30m, 45m, 1h, 2h).
   - Stored in Room database table `app_time_limits`.
2. **Real-Time Foreground Tracking**:
   - `UsageStatsRepository` calculates daily foreground minutes against the calendar date (ISO-8601).
   - When foreground time reaches or exceeds `dailyLimitMinutes`, the app is marked as expired.
3. **10-Second Countdown Expiry Warning (`TimeLimitExpiredDialog`)**:
   - When an expired app is launched or running, the user is presented with a 10-second countdown alert.
   - Shows remaining seconds, total time spent today, and reflection prompt.
4. **Automatic Close**:
   - If the countdown reaches 0 without an extension, `MinimalAccessibilityService` executes `performGlobalAction(GLOBAL_ACTION_HOME)`, smoothly closing the app and returning the user to NumbPhone.
5. **Lock Until Tomorrow**:
   - The app remains locked until midnight (00:00).
6. **Deliberate Emergency Extensions**:
   - In case of genuine need, users can grant a +5 minute or +15 minute extension.
   - Extensions record an exit reason and update `emergencyExtensionMinutes` in the database.

---

# 11. FOCUS SESSIONS, PRESETS & AUTOMATED SCHEDULING

### Focus Modes
* **LIGHT**: Managed apps restricted; low exit delay (5s).
* **STRICT**: Managed apps blocked; adaptive exit friction (5s → 30s → 2m); distracting apps hidden from launcher.
* **DEEP FOCUS**: Maximum friction (up to 10m delay); status bar hidden; Grayscale mode engaged; DND silence enabled; no rule modifications allowed during session.

### Focus Presets (`FocusPresetEntity`)
Quick-start presets available on the Focus Session screen:
- **Quick Sprint**: 25 minutes, Light Mode (Pomodoro)
- **Deep Work**: 50 minutes, Strict Mode
- **Exam Preparation**: 120 minutes, Deep Focus Mode

### Automated Recurring Focus Schedules (`FocusScheduler`)
Using `AlarmManager.setExactAndAllowWhileIdle()` and `FocusScheduleReceiver`:
- Users can schedule recurring focus blocks (e.g., Mon–Fri from 09:00 to 17:00).
- Automatically transitions into active focus mode and applies configured mode/presets.
- Automatically tears down the session when the duration elapses.
- Recovers seamlessly across device reboots (`BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`).

---

# 12. ADAPTIVE EXIT FRICTION & INTENT REFLECTION

NumbPhone prevents impulsive early cancellation of focus sessions:

```text
User taps "End Session" or attempts early exit
                 ↓
    ExitFrictionEngine calculates required delay
                 ↓
     1st attempt  →  5 seconds
     2nd attempt  →  30 seconds
     3rd attempt  →  2 minutes
     4th+ attempt →  10 minutes
                 ↓
       Show ExitFrictionDialog
                 ↓
  Displays Focus Goal ("Prepare for Exam")
  Displays Time Remaining in Session
  Live countdown timer (Cannot be clicked until 0)
                 ↓
  [ Continue Focusing ]   or   [ Confirm Exit Anyway (after countdown) ]
```

### "Why Are You Opening This?" (`IntentReflectionDialog`)
When a user attempts to launch a blocked app or request an emergency limit extension, an intent reflection dialog asks:
* *Important / Urgent*
* *Just checking*
* *I'm bored*
* *Someone sent me something*
* *Other*

Responses are stored locally in `blocked_attempts` for behavioral wellbeing analysis.

---

# 13. BYPASS PROTECTION & ACCESSIBILITY BACKSTOP

While the launcher controls home screen launches, secondary avenues exist:
* Notification tap
* Deep links
* Recent apps screen
* External Intents from other applications

### Accessibility Service Implementation (`MinimalAccessibilityService`)
A lightweight, privacy-preserving Accessibility Service acts as a backstop:
- Listens strictly to `typeWindowStateChanged` events.
- Evaluates the foreground package against active focus sessions and daily time limits.
- If an unpermitted app is detected, it immediately launches `MainActivity` with `Intent.FLAG_ACTIVITY_NEW_TASK` and triggers `performGlobalAction(GLOBAL_ACTION_HOME)`.

### Banking & Security Compliance: Zero Content Scraping
To prevent conflict with banking, UPI, and financial apps that block accessibility tools:
- `canRetrieveWindowContent` is strictly set to `false` in `res/xml/accessibility_service_config.xml`.
- Flags like `flagRetrieveInteractiveWindows` and `typeWindowsChanged` are excluded.
- The service **cannot** read screen contents, view text, or capture keystrokes. It only reads the package name identifier.

```xml
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault"
    android:canRetrieveWindowContent="false"
    android:notificationTimeout="100"
    android:description="@string/accessibility_service_description"
    android:summary="@string/accessibility_service_summary" />
```

---

# 14. NOTIFICATION DIGEST & NOTIFICATION LISTENER

To eliminate notification anxiety during study or deep work:
- `MinimalNotificationListenerService` extends `NotificationListenerService`.
- Active during focus sessions.
- Suppresses incoming non-essential notification alerts and heads-up banners.
- Bundles notifications into `digest_notifications` Room table.
- Essential calls and priority messages bypass the digest.
- Users can view the summarized Notification Digest via the Home Screen Quick Control Center.

---

# 15. SYSTEM-LEVEL HARDWARE GRAYSCALE MODE

Color is the primary driver of smartphone dopamine loops. NumbPhone provides automated display desaturation:
- Managed by `GrayscaleHelper`.
- Toggles `Settings.Secure.accessibility_display_daltonizer_enabled` and `accessibility_display_daltonizer` (`DALTONIZER_MONOCHROMACY`).
- Automatically turns the entire phone screen monochrome upon starting a Deep Focus session.
- Restores full color when the session finishes.
- Requires one-time ADB permission grant:
  ```bash
  adb shell pm grant com.minimalphone android.permission.WRITE_SECURE_SETTINGS
  ```
- If permission is not granted via ADB, the UI provides clear step-by-step guidance without crashing.

---

# 16. EMERGENCY ACCESS & QUICK CONTACTS SPEED DIAL

Safety is paramount. NumbPhone ensures emergency access is never compromised:

### System Emergency Whitelist (`EmergencyAccessManager`)
The following package patterns are permanently exempted from all blocking, focus sessions, and time limits:
* `com.android.emergency` (AOSP Emergency SOS)
* `com.android.phone`, `com.android.dialer`
* `com.samsung.android.dialer`, `com.sec.android.emergencylauncher`
* `com.google.android.dialer`, `com.oneplus.dialer`, `com.motorola.phone`
* System dialer, SMS, contacts, and alarm intents

### Pinned Quick Contacts (`QuickContactEntity`)
Users can pin family members, doctors, or emergency contacts:
- Displayed as a horizontal speed-dial row right under the Home screen clock.
- One-tap initiates a direct call via `Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))`.
- Accessible even during active focus sessions.

---

# 17. ROOM DATABASE ARCHITECTURE & SCHEMA

Database: `MinimalPhoneDatabase` (SQLite via Room).

### Entity Roster:
1. **`InstalledAppEntity`**: Package name, activity, label, category, system app flag, essential flag, favorite on home flag, classification source.
2. **`AppRuleEntity`**: Package rules, category overrides, always available state, blocked in focus state, custom label.
3. **`FocusSessionEntity`**: Session ID, start/end timestamps, duration, goal title/category, mode (Light/Strict/Deep), status, bypass attempt count, friction seconds completed.
4. **`BlockedAttemptEntity`**: ID, package name, label, timestamp, session ID, route (Launcher, Recents, Notification), user intent reflection reason.
5. **`ExitAttemptEntity`**: ID, session ID, attempt number, required/completed friction seconds, timestamp, success flag, exit reason.
6. **`FocusGoalEntity`**: ID, title, category, custom flag, usage count, last used timestamp.
7. **`EssentialAppEntity`**: User-designated essential packages that bypass all restrictions.
8. **`DailyAppUsageEntity`**: Date (ISO-8601), package name, foreground minutes, launch count, blocked attempts.
9. **`FocusPresetEntity`**: Quick-start presets (Sprint, Deep Work, Exam Prep).
10. **`FocusScheduleEntity`**: Recurring alarms (preset ID, days of week, start hour/minute, duration, enabled flag).
11. **`DigestNotificationEntity`**: Intercepted notifications (package, label, title, text, timestamp, read status).
12. **`QuickContactEntity`**: Speed-dial contacts (name, phone number, pinned flag).
13. **`AppTimeLimitEntity`**: Daily limit rules (package name, daily limit minutes, enabled flag, emergency extension minutes, last extension timestamp).

All database queries return reactive `Flow` objects to ensure instant UI reactivity.

---

# 18. SETTINGS HUB & COMMUNITY FEEDBACK

The Settings screen is organized into dedicated hubs:

```text
SETTINGS
│
├── 🎨 APPEARANCE & THEMES
│   └── Minimalist color palettes (Pure OLED Black, E-Ink Paper, Warm Amber)
│
├── 🧘 FOCUS & DETOX
│   └── Default session durations, adaptive exit friction toggle, dumb phone mode
│
├── ⏳ APP LIMITS & WHITELIST
│   └── Daily app budgets, set time limits, essential access management
│
├── 🛡️ SYSTEM & DIAGNOSTICS
│   └── DND access, accessibility backstop, "NumbPhone Health" diagnostics, setup wizard
│
└── 📦 DATA & ABOUT
    ├── Local JSON backup & clipboard export / restore
    ├── Community feedback link (Google Form)
    └── Local-First Privacy Charter & v1.0.0 architecture specs
```

### Community Feedback Integration
A "Suggestions & Feedback" button is prominently placed in settings, opening the official Google Form:
- URL: `https://forms.gle/T3xsTRHTx2uDaoVP6`
- Opens via standard `ACTION_VIEW` browser intent.

---

# 19. LOCAL DATA PORTABILITY (BACKUP & RESTORE)

In alignment with local-first privacy:
- **`ExportBackupJsonUseCase`**: Serializes all app rules, focus session history, pinned contacts, time limits, and DataStore preferences into a structured JSON payload copied directly to the clipboard.
- **`ImportBackupJsonUseCase`**: Validates JSON schema integrity and restores entities into Room and DataStore in a single atomic transaction.
- Zero server communication or cloud accounts required.

---

# 20. MINIMALIST DESIGN SYSTEM & THEMING

NumbPhone strictly avoids colorful gamification, flashy gradients, and noisy animations.

### Supported Themes (`AppTheme`):
1. **`PURE_BLACK` (OLED)**: Background `#000000`, Text `#FFFFFF`, Surface `#18181B`. Maximizes battery savings on OLED screens.
2. **`E_INK_PAPER`**: Background `#F5F5F0`, Text `#1A1A1A`, Surface `#EBEBE4`. Simulates physical paper / e-reader devices.
3. **`WARM_AMBER`**: Background `#12100E`, Text `#E6A15C`, Surface `#1F1B17`. Late-night low blue-light reading mode.

### Typography
- Primary: Modern high-readability sans-serif (Inter / System Clean).
- Numerical displays: Monospaced high-contrast numerals for clocks and countdowns.

---

# 21. PROGUARD & R8 PRODUCTION RELEASE HARDENING

Production release builds have full R8 optimization enabled in `app/build.gradle.kts`:
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```
`app/proguard-rules.pro` contains explicit keep rules for:
- Jetpack Compose & Material 3 internals
- Room Database Entities, DAOs, and Type Converters
- Dagger Hilt generated code and bindings
- DataStore Preferences serialization
- Kotlin Coroutines and reflection models

---

# 22. COMPLETE DEVELOPMENT MILESTONES & ROADMAP

The application has been executed in disciplined, verified milestones:

### Core Foundation & MVP:
* **M0 — Project Foundation**: Multi-module Gradle architecture, Hilt, Room, Compose setup.
* **M1 — Launcher Shell**: Android HOME activity registration, minimal UI, default launcher setup.
* **M2 — Real App Discovery**: `LauncherApps` API, `PackageManager` callbacks, dynamic scanning.
* **M3 — App Management**: Categorization, Room persistence, whitelist management.
* **M4 — Usage Tracking**: `UsageStatsManager` integration, screen time calculation.
* **M5 — Blocking Engine**: Accessibility Service backstop, foreground interception, redirect to launcher.
* **M6 — Focus Sessions**: Active session state machine, timers, goal specification, Room history.
* **M7 — Exit Friction**: Deliberate delay countdowns, adaptive progression (5s, 30s, 2m, 10m), reflection prompts.
* **M8 — Bypass Protection**: Multi-route coverage (Recents, notifications, deep links, external intents).
* **M9 — Boot & OEM Resilience**: `BOOT_COMPLETED` receivers, service diagnostics, battery guidance.
* **M10 — Polish & Onboarding**: 4-step wizard, DataStore preferences, settings navigation.

### Production Hardening & Phase 2 Advanced Features:
* **M11 — Release Optimization & Theming**: Local JSON backup/restore, custom themes (Pure Black, E-Ink, Warm Amber), search filter, R8/ProGuard shrinking.
* **M12 — Immersive Full-Screen Launcher**: Status bar hiding via `WindowInsetsControllerCompat`, 6 essential default apps auto-pinning, "+ Add app" quick button.
* **M13 — Banking & Security Compatibility**: Set `canRetrieveWindowContent="false"`, eliminated screen inspection, guaranteed zero conflict with banking/UPI apps.
* **M14 — Daily App Usage Limits Engine**: Per-app daily minutes budget, 10-second countdown expiry pop-up, auto-close via AccessibilityService, lock until tomorrow, emergency +5m/+15m extensions.
* **M15 — Automated Focus Scheduling**: `FocusScheduler` with exact alarms, recurring day-of-week schedules, focus presets.
* **M16 — Notification Digest Interception**: `MinimalNotificationListenerService` quiet notification buffering during focus, quick control center digest UI.
* **M17 — Hardware / System Grayscale Mode**: `GrayscaleHelper` with `WRITE_SECURE_SETTINGS` display desaturation during focus.
* **M18 — Quick Contacts Speed Dial**: Emergency contact pinning row on home screen with one-tap dialing.
* **M19 — Home Apps Filtering**: `AppListFilter.HOME` chip in app management to manage pinned apps effortlessly.
* **M20 — Community Feedback & Branding**: Official Google Form suggestions link, NumbPhone logo assets, themed monochrome icons, 512×512 Google Play Store package.

---

# 23. TESTING & VERIFICATION REQUIREMENTS

Maintain high test coverage across all layers:

### Unit Tests:
* App classification logic (`AppTaxonomyClassifierTest`)
* Central launch decisions (`LaunchEngineTest`)
* Focus session state & expiry calculations (`FocusEngineTest`)
* Exit friction duration calculations (`ExitFrictionEngineTest`)
* Bypass route interception decisions (`BypassEngineTest`)
* DataStore settings use cases (`SettingsUseCasesTest`)
* JSON backup export and import validation (`BackupUseCasesTest`)
* ViewModel state machines (`HomeViewModelTest`, `AppsManagementViewModelTest`, `FocusSessionViewModelTest`, `ScreenTimeViewModelTest`)

### Database Tests:
* Room DAO entity inserts, queries, cascading updates, and schema migrations.

### Physical Device Verification:
* Test default launcher behavior, home button responsiveness, and recents button interception.
* Test banking app launch compatibility with active Accessibility Service.
* Test OEM battery management behavior on Samsung (OneUI), Xiaomi (MIUI/HyperOS), OnePlus (OxygenOS), and Google Pixel.

---

# 24. SUMMARY COMMAND & EXECUTION DIRECTIVE

When instructing or continuing development on **NumbPhone**:
1. Treat this document as the single source of truth for features, architecture, and design rules.
2. Maintain Clean Architecture separation across all modules.
3. Preserve all security constraints (notably `canRetrieveWindowContent="false"`).
4. Keep the design calm, minimalist, and typography-centered.
5. Ensure all data remains 100% on-device and local-first.

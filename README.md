<p align="center">
  <img src="icons/flat%20version.png" alt="NumbPhone Logo" width="128" height="128" style="border-radius: 28px;" />
</p>

# NumbPhone 📱
### Focus & Digital Wellbeing Android Launcher

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF.svg?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-brightgreen.svg?style=flat&logo=android&logoColor=white)](https://developer.android.com)
[![Android Target SDK](https://img.shields.io/badge/Target%20SDK-34%20(Android%2014)-3DDC84.svg?style=flat&logo=android&logoColor=white)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%26%20Material%203-4285F4.svg?style=flat&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Hilt](https://img.shields.io/badge/DI-Dagger%20Hilt-orange.svg?style=flat)](https://dagger.dev/hilt/)
[![Room](https://img.shields.io/badge/Persistence-Room%20DB-blue.svg?style=flat)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

> *"Make intentional actions easy and impulsive actions deliberate."*

**NumbPhone** is a distraction-resistant Android launcher and digital wellbeing application designed to transform modern smartphones into intentional tools. It provides the simplicity and calm of a "dumb phone" while preserving the reliability of essential communication and utility tools.

---

## 📸 Screenshots

| Minimal Home Screen | Focus Session Active | Screen Time Analytics |
|:---:|:---:|:---:|
| ![Minimal Home Screen](screen_front.png) | ![Focus Session](screen_focus.png) | ![Screen Time Analytics](screen_screentime.png) |

| App Whitelist Management | Launcher Settings & Diagnostics |
|:---:|:---:|
| ![App Management](screen_addapp_real.png) | ![Settings](screen_settings.png) |

---

## 🎨 Icon & Brand Assets

| Version | Asset File | Where to Use It | Purpose |
|:---|:---:|:---|:---|
| **1. Flat version** | [`icons/flat version.png`](icons/flat%20version.png) | App branding, README, website, GitHub, splash screen | Main clean brand artwork |
| **2. Monochrome version** | [`icons/monochrome version.png`](icons/monochrome%20version.png) | Android themed icon / system contexts | Works with Android's monochrome/themed icon system |
| **3. Black/White version** | [`icons/black and white.png`](icons/black%20and%20white.png) | Dark/light backgrounds, documentation, marketing | Flexible brand variant |
| **4. Adaptive Android icon** | [`icons/adaptive android icon.png`](icons/adaptive%20android%20icon.png) | **Actual Android launcher icon** | Primary icon users see on their phone |
| **5. 512×512 Play Store version** | [`icons/512×512 Play Store version.png`](icons/512×512%20Play%20Store%20version.png) | **Google Play Store listing** | Store listing/app promotion |

---

## ✨ Key Features

- **🧘 Clean Typography Launcher:**
  - Zero icon grids, no distracting notification badges, and no infinite feeds.
  - High-contrast typography with quick-launch access to permitted applications.
  - Built-in dynamic date, time, and active focus status indicators.

- **🎯 Focus Sessions & Custom Goals:**
  - Multiple focus tiers: **Light**, **Strict**, and **Deep Focus**.
  - Goal-oriented sessions (e.g., *"Prepare for Exam"*, *"Deep Work Sprint"*).
  - Configurable session durations with real-time countdown and persistence across reboots (`BOOT_COMPLETED`).

- **🛡️ Adaptive Exit Friction & Anti-Bypass Protection:**
  - Deliberate delay countdowns (5s, 30s, 2m) and reflective prompts (*"Why are you opening this?"*) before premature exits.
  - Background Accessibility service backstop intercepts sneaky secondary launch avenues (recents screen, push notifications, deep links).
  - Always preserves access to **Emergency Calling**, **Phone**, and critical system settings.

- **📊 Screen Time & Wellbeing Insights:**
  - Real-time device usage tracking powered by Android `UsageStatsManager`.
  - Detailed daily app usage breakdowns and blocked impulse attempt metrics.

- **🔒 Local-First & Privacy-Centric:**
  - 100% on-device data storage using Room DB and Jetpack DataStore.
  - No cloud sync, no tracking telemetry, no account required.

---

## 🏗️ Architecture & Tech Stack

NumbPhone is built following **Clean Architecture** principles and a strict **multi-module Gradle** setup:

```
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
                                        Accessibility   Boot/Power
                      └──────┬───────────────┘
                             ↓
                         DOMAIN LAYER (Pure Business Logic)
                             │
                  ┌──────────┼──────────────┬──────────────┐
                  ↓          ↓              ↓              ↓
             Launch Engine Focus Engine Bypass Engine Recovery System
                  │          │              │              │
                  └──────────┼──────────────┴──────────────┘
                             ↓
                          DATA LAYER
                             │
                      ┌──────┴──────┐
                      ↓             ↓
                    Room         DataStore
```

### Module Breakdown

| Module | Responsibility |
|---|---|
| **`:app`** | Application entry point, `MinimalPhoneApplication`, Navigation Graph, Hilt dependency container. |
| **`:core:model`** | Pure Kotlin domain data models (`InstalledApp`, `AppCategory`, `FocusSession`, `FocusMode`, etc.). |
| **`:core:common`** | Coroutine dispatchers, Result wrappers, logging abstractions, Kotlin extensions. |
| **`:core:data`** | Room database (`MinimalPhoneDatabase`), DAOs, `AppRepository`, `FocusSessionRepository`, `UsageStatsRepository`. |
| **`:core:domain`** | Core business logic: `LaunchAppUseCase`, `FocusEngine`, `BypassProtectionManager`, `RecoverySystem`. |
| **`:core:ui`** | Design system: `MinimalTheme`, `MinimalTypography`, shared components, and dialogs. |
| **`:feature:homescreen`** | Minimal launcher home UI, clock, date, focus indicator, and permitted app list. |
| **`:feature:appslist`** | App discovery, whitelist configuration, categorization, and search. |
| **`:feature:focussession`** | Focus mode configuration, goal selector, active timer, and friction unlock dialogs. |
| **`:feature:screentime`** | Screen-time breakdown, categorized usage charts, and blocked attempt history. |
| **`:feature:onboarding`** | Multi-step permission granting flow and default launcher setup. |
| **`:feature:settings`** | Launcher preferences, diagnostics dashboard ("NumbPhone Health"), and privacy controls. |

### Technologies & Libraries
- **Language:** Kotlin 2.0+
- **UI Toolkit:** Jetpack Compose & Material 3
- **Dependency Injection:** Dagger Hilt
- **Local Persistence:** Room Database & Jetpack DataStore Preferences
- **Asynchronous Flow:** Kotlin Coroutines & Reactive `StateFlow` / `SharedFlow`
- **Android APIs:** `LauncherApps`, `UsageStatsManager`, `AccessibilityService`, `PowerManager`

---

## 🔑 Permissions Overview

NumbPhone requires standard system permissions to act as your primary launcher and provide focus boundaries:

| Permission | Purpose | Status |
|---|---|---|
| `android.intent.category.HOME` | Registers NumbPhone as the default home launcher. | **Essential** |
| `QUERY_ALL_PACKAGES` / `LauncherApps` | Dynamically detects installed applications and launch intents. | **Essential** |
| `PACKAGE_USAGE_STATS` | Calculates screen time and categorized usage metrics. | **Optional** |
| `BIND_ACCESSIBILITY_SERVICE` | Intercepts attempts to open distracting apps via notifications or recents during active focus. | **Recommended** |
| `RECEIVE_BOOT_COMPLETED` | Recovers active focus sessions seamlessly after device reboots. | **Recommended** |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevents aggressive OEM task killers from terminating focus session timers. | **Recommended** |
| `POST_NOTIFICATIONS` | Displays a persistent focus session timer status indicator. | **Optional** |

> **Privacy Note:** The Accessibility Service only monitors foreground package change events (`TYPE_WINDOW_STATE_CHANGED`). It does **not** read screen contents, record keystrokes, or transmit any data.

---

## 🚀 Getting Started

### Prerequisites
- **JDK:** Java 21 or higher
- **Android SDK:** API Level 34 (Android 14)
- **Android Studio:** Android Studio Iguana (2023.2.1) or newer
- **Target Device / Emulator:** Android 8.0 (API 26) or higher

### Building from Source

1. **Clone the repository:**
   ```bash
   git clone https://github.com/kaladharroyal/Numbphone.git
   cd Numbphone
   ```

2. **Open in Android Studio:**
   - Launch Android Studio.
   - Select **Open** and choose the cloned project root folder.
   - Wait for Gradle sync to complete.

3. **Build the Debug APK via CLI:**
   ```bash
   # On Windows (PowerShell)
   .\gradlew.bat assembleDebug

   # On macOS / Linux
   ./gradlew assembleDebug
   ```

4. **Install onto connected device / emulator:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

5. **Set as Default Launcher:**
   - Open Android **Settings** → **Apps** → **Default Apps** → **Home App**.
   - Select **NumbPhone**.

---

## 🧪 Testing

Run local unit tests across all modules:
```bash
# Run unit tests
.\gradlew.bat test

# Run Android instrumentation tests (device/emulator connected)
.\gradlew.bat connectedAndroidTest
```

---

## 📄 License

```
Copyright 2026 NumbPhone Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

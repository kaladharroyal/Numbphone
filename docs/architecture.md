# Architecture Document — Minimal Phone

## 1. Architectural Overview
Minimal Phone follows Clean Architecture principles with a multi-module architecture:

```
                    MINIMAL PHONE ARCHITECTURE
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

## 2. Module Responsibilities
- **`:app`**: Application entry point, `MinimalPhoneApplication`, Navigation Graph, Hilt Application container, Android Manifest HOME activity declaration.
- **`:core:model`**: Pure Kotlin domain data models (`InstalledApp`, `AppCategory`, `AppRule`, `FocusSession`, `FocusMode`, `BlockedAttempt`, `ExitAttempt`, `FocusGoal`).
- **`:core:common`**: Coroutine dispatchers, Result wrappers, Kotlin extensions, logging abstraction.
- **`:core:data`**: Room database (`MinimalPhoneDatabase`), DAOs, `AppRepository`, `FocusSessionRepository`, `UsageStatsRepository`, `LauncherApps` callbacks.
- **`:core:domain`**:
  - `launch`: `EvaluateAppLaunchUseCase`, `LaunchAppUseCase`, `AppLaunchDecision`.
  - `focus`: `FocusEngine`, `FocusState`, `FocusSessionManager`.
  - `bypass`: `BypassProtectionManager`, `BypassRoute`, `BypassDecision`.
  - `recovery`: `RecoverySystem`, `HealthDiagnostics`.
- **`:core:ui`**: Typography (`MinimalTypography`), Theme (`MinimalTheme`), Shared components (Countdowns, App Rows, Minimal Dialogs).
- **`:feature:homescreen`**: Minimal launcher home screen (clock, date, focus indicator, allowed apps).
- **`:feature:appslist`**: Settings-level app management, search/filter, category overrides, whitelist management.
- **`:feature:focussession`**: Focus session creation, mode selector, goal setter, active session controls.
- **`:feature:screentime`**: Screen-time metrics, category breakdown, blocked attempts summary.
- **`:feature:onboarding`**: Onboarding step flow (Launcher, Usage Access, Accessibility, Battery).
- **`:feature:settings`**: Settings, Diagnostics dashboard ("Minimal Phone Health"), privacy controls.

## 3. Data Flow & Source of Truth
1. **Dynamic App Updates:**
   `LauncherApps.registerCallback()` → `onPackageAdded`/`onPackageRemoved` → `AppRepository` → Room Database → `Flow<List<InstalledApp>>` → UI.
2. **Launch Decision Pipeline:**
   User tap → `LaunchAppUseCase` → `EvaluateAppLaunchUseCase` → `AppLaunchDecision` (`ALLOW`, `BLOCK`, `SHOW_FRICTION`).
3. **Bypass Protection:**
   Accessibility Event → `BypassProtectionManager` → Package & Focus evaluation → Interception / Return to Launcher.
4. **Focus Management:**
   `FocusEngine` (StateFlow) ← `FocusSessionManager` ↔ `FocusSessionRepository` (Room).

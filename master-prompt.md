# MASTER BUILD PROMPT

## Project: Minimal Phone — Focus & Digital Wellbeing Android Launcher

You are an expert Android engineer, Kotlin architect, UI/UX engineer, and QA engineer.

Your task is to design and BUILD a production-quality Android application called:

**Minimal Phone**

The application is a minimalist Android launcher designed to transform a normal Android smartphone into a controlled, distraction-resistant "dumb phone" experience.

The application must replace the default Android home screen, display only permitted applications, provide focus sessions, block distracting applications, track usage, and introduce deliberate exit friction when the user attempts to leave an active focus session.

Do NOT build this as a simple demo or mockup.

Build a real Android application using real Android APIs, real persistence, real services, and a modular architecture.

---

# 1. PRODUCT VISION

The core philosophy is:

> Make intentional actions easy and impulsive actions deliberate.

The application should NOT attempt to permanently trap the user inside the launcher or make the device impossible to recover.

Instead, it should make distraction difficult while always preserving essential functionality such as:

* Phone
* Emergency calling
* Messages/SMS
* Camera
* Clock
* Contacts
* Settings
* Navigation/maps where configured
* Other explicitly designated essential applications

On stock Android, acknowledge that users can ultimately disable permissions, change the default launcher, uninstall the application, or otherwise override restrictions.

The product should therefore focus on:

1. Strong friction
2. Reliable blocking
3. Minimal UI
4. Bypass resistance
5. Clear user intent
6. Essential-app accessibility

---

# 2. PRIMARY TARGET USER

The initial target audience is:

## Students

Especially students preparing for examinations who want to prevent:

* YouTube
* Instagram
* TikTok
* X/Twitter
* Games
* Reddit
* Entertainment applications

from interrupting study sessions.

The architecture must remain general enough for:

* Software engineers
* Professionals
* Minimalists
* Parents configuring controlled environments

but the initial UX should prioritize students.

---

# 3. CORE USER EXPERIENCE

After installation:

```text
Install
  ↓
Onboarding
  ↓
Set Minimal Phone as default launcher
  ↓
Grant required permissions
  ↓
Scan installed applications
  ↓
Automatically classify applications
  ↓
Show minimal home screen
```

The home screen should NOT look like a conventional Android launcher.

Do NOT create:

* Traditional app drawer
* Excessive widgets
* Decorative UI
* Ads
* Social feeds
* Recommendation feeds
* Unnecessary animations

The home screen should feel calm and intentional.

Example:

```text
┌─────────────────────────────┐
│                             │
│          10:42 PM           │
│                             │
│          Tuesday            │
│                             │
│  Phone                      │
│  Messages                   │
│  Camera                     │
│  Maps                       │
│  Calendar                   │
│                             │
│                             │
│      Focus Mode: OFF        │
│                             │
└─────────────────────────────┘
```

Use Jetpack Compose for the UI.

---

# 4. TECHNOLOGY STACK

Use modern Android development practices.

Preferred stack:

* Kotlin
* Jetpack Compose
* Material 3 where appropriate
* AndroidX
* Room
* Hilt
* Coroutines
* Flow / StateFlow
* Navigation Compose
* Gradle Kotlin DSL
* Version Catalog
* LauncherApps API
* PackageManager
* UsageStatsManager
* AccessibilityService
* Foreground Service where required
* Boot receiver
* DataStore for lightweight preferences where appropriate

Use the latest stable Android SDK/tooling available in the development environment.

Before implementing APIs that vary by Android version, check the SDK/API level and use appropriate compatibility handling.

Do not use deprecated APIs when a modern supported alternative exists.

---

# 5. ARCHITECTURE

Use a clean, modular architecture.

Recommended structure:

```text
minimal-phone/
│
├── docs/
│   ├── prd.md
│   ├── architecture.md
│   ├── permissions.md
│   ├── testing.md
│   └── risks.md
│
├── app/
│   └── Application entry point
│
├── core/
│   ├── ui/
│   ├── model/
│   ├── data/
│   ├── domain/
│   ├── common/
│   └── di/
│
├── feature/
│   ├── homescreen/
│   ├── appslist/
│   ├── focussession/
│   ├── screentime/
│   ├── onboarding/
│   └── settings/
│
└── build.gradle.kts
```

Responsibilities:

### core:model

Models such as:

```text
InstalledApp
AppCategory
FocusSession
BlockedAttempt
UsageRecord
FocusGoal
ExitAttempt
AppRule
```

### core:data

Responsible for:

* Room
* DataStore
* PackageManager/LauncherApps integration
* Repositories
* Local persistence

### core:domain

Business logic:

* App classification
* Blocking rules
* Focus state
* Exit-friction calculation
* Session management
* Bypass detection logic

### core:ui

Shared:

* Typography
* Theme
* Buttons
* Cards
* Dialogs
* Countdown components
* App rows
* Empty states

### feature:homescreen

The actual launcher home screen.

### feature:appslist

Installed-app management and whitelist UI.

### feature:focussession

Focus creation, activation and management.

### feature:screentime

Usage analytics.

### feature:onboarding

Initial setup and permissions.

### feature:settings

Application configuration.

---

# 6. ANDROID LAUNCHER

Register the appropriate Activity as an Android HOME activity.

The application must be selectable as the default launcher.

Implement:

```text
ACTION_MAIN
CATEGORY_HOME
CATEGORY_DEFAULT
```

Use the correct manifest configuration.

When the user presses the Home button while Minimal Phone is the default launcher, Android should return to Minimal Phone.

The launcher must be functional on a physical Android device.

Do not rely exclusively on an emulator.

---

# 7. INSTALLED APPLICATION DISCOVERY

Never hardcode the user's installed-app list.

Use Android's application/package APIs.

Prefer:

```text
LauncherApps
```

for launcher-relevant applications and:

```text
LauncherApps.registerCallback()
```

to react to changes such as:

* Application installed
* Application removed
* Application changed

Use PackageManager where appropriate for additional package/application metadata.

The application list must dynamically reflect the actual device.

Handle:

* Normal apps
* System apps
* Work profiles
* Multiple Android profiles where supported
* Apps being installed/uninstalled

Do not assume every installed package is launchable.

---

# 8. APPLICATION CLASSIFICATION

Every discovered application should be classified.

There are two primary categories:

## ALWAYS AVAILABLE

Examples:

* Phone
* Messages/SMS
* Camera
* Clock
* Contacts
* Settings
* Essential system apps
* Google apps except YouTube

These are available without restriction.

## MANAGED

Examples:

* YouTube
* Instagram
* TikTok
* X/Twitter
* Games
* Reddit
* Netflix
* Entertainment applications
* Other non-essential applications

Managed apps are subject to focus rules.

Classification logic:

```text
if system app:
    ALWAYS_AVAILABLE

else if package starts with "com.google."
    AND package != "com.google.android.youtube":
    ALWAYS_AVAILABLE

else:
    MANAGED
```

However, make the classification system configurable rather than permanently embedding assumptions throughout the code.

Store the classification/rule in a maintainable form.

YouTube MUST remain managed despite being a Google application.

---

# 9. HOME SCREEN

The home screen should display only applications that are currently allowed.

There must be NO traditional app drawer in MVP.

Example:

```text
Phone
Messages
Camera
Chrome
Calendar
```

If an application is blocked during Focus Mode:

```text
Do NOT display it on the home screen.
```

The user should not be encouraged to browse applications.

Each visible application row should launch the application through a proper Android Intent.

Do not fake application launching.

---

# 10. APP LAUNCH PIPELINE

All launches initiated from the launcher must pass through a central launch decision.

Conceptually:

```text
User taps app
      ↓
openApp()
      ↓
Check application rule
      ↓
Is Always Available?
      ├── YES → Launch
      │
      └── NO
           ↓
       Is Focus Mode active?
           ├── NO → Launch
           │
           └── YES
                ↓
              BLOCK
                ↓
          Show friction UI
```

Do NOT scatter blocking logic across multiple UI components.

Create a central domain-level decision mechanism.

For example:

```text
AppLaunchDecision
```

with states such as:

```text
ALLOW
BLOCK
SHOW_FRICTION
```

---

# 11. FOCUS MODE

Users must be able to create a focus session.

A focus session contains:

```text
Session ID
Start time
End time
Duration
Focus goal
Blocked applications/rules
Status
Number of bypass attempts
```

Example:

```text
Study Session

Goal:
"Complete Machine Learning Unit 3"

Duration:
2 hours

Start:
8:00 PM

End:
10:00 PM
```

When active:

```text
Focus Mode = ON
```

Managed applications are blocked.

Always Available applications remain accessible.

---

# 12. FOCUS MODES

Implement three conceptual levels.

## LIGHT

* Managed apps can be restricted
* Minimal exit friction
* Essential apps always available

## STRICT

* Managed apps blocked
* Stronger exit friction
* Apps hidden from launcher
* Bypass routes monitored

## DEEP FOCUS

* Strongest friction
* Managed apps blocked
* No changing rules during session
* Distracting apps hidden
* Bypass routes monitored
* Optional notification reduction
* Exit requires deliberate confirmation

Do not make Deep Focus irreversible.

---

# 13. EXIT FRICTION

This is one of the core product features.

Do NOT simply provide:

```text
Disable Focus Mode → OFF
```

Instead:

```text
User attempts to exit
        ↓
Show exit-friction screen
        ↓
Display focus goal
        ↓
Display remaining time
        ↓
Require deliberate action
```

Example:

```text
┌───────────────────────────────┐
│                               │
│       Leave Focus Mode?       │
│                               │
│ Your goal:                    │
│ "Prepare for ML examination"  │
│                               │
│ 47 minutes remaining          │
│                               │
│       Wait: 30 seconds        │
│                               │
│           27...               │
│                               │
│       [ Continue ]            │
│                               │
│       [ Exit Anyway ]         │
│                               │
└───────────────────────────────┘
```

The UI should never shame or insult the user.

---

# 14. ADAPTIVE EXIT FRICTION

Track bypass attempts per focus session.

Suggested progression:

```text
1st attempt → 5 seconds
2nd attempt → 30 seconds
3rd attempt → 2 minutes
4th attempt → 10 minutes
```

Make this configurable.

The system should never create an infinite lockout.

Do not use punishment language.

Instead communicate:

> "You chose a focus session. Take a moment before leaving."

---

# 15. FOCUS GOALS

Before starting a focus session, allow the user to specify:

```text
What are you focusing on?

Study
Coding
Reading
Work
Sleep
Custom
```

Allow custom text.

Example:

```text
Goal:
"Finish my DBMS revision"
```

If the user attempts to exit, show that goal.

This creates a connection between the user's original intention and the exit decision.

---

# 16. "WHY ARE YOU OPENING THIS?" FEATURE

When a user attempts to access a blocked application, optionally ask:

```text
Why do you want to open this?

○ Important
○ Just checking
○ I'm bored
○ Someone sent me something
○ Other
```

Record the response locally.

Store:

```text
timestamp
app
focusSession
reason
```

This will later support behavioral analytics.

Do not upload this information anywhere in MVP.

Keep it local-first.

---

# 17. BYPASS PROTECTION

The launcher itself cannot control every Android app-opening route.

Implement AccessibilityService as a backstop.

Potential bypass routes include:

```text
Notification tap
Deep link
Recent Apps
External Intent
Other app launching a managed application
```

Architecture:

```text
                  Android OS
                      │
        ┌─────────────┼─────────────┐
        ↓             ↓             ↓
    Launcher      Notification   External Intent
        │             │             │
        └─────────────┼─────────────┘
                      ↓
              Bypass Protection
                      ↓
              Managed application?
                 /           \
               YES            NO
                ↓              ↓
              Block          Allow
```

Use AccessibilityService ONLY for the backstop.

Do not route every normal launcher tap through AccessibilityService.

Normal launcher launches should be handled directly through the launcher's own decision logic.

This minimizes unnecessary complexity and reduces dependence on Accessibility for the common path.

---

# 18. ACCESSIBILITY SERVICE

Implement a real AccessibilityService.

It should:

1. Detect relevant foreground/window changes.
2. Determine the package currently in the foreground.
3. Check whether that package is managed.
4. Check whether Focus Mode is active.
5. If blocked, return the user to Minimal Phone or otherwise prevent continued access using supported Android behavior.

Optimize aggressively.

Do NOT process every accessibility event unnecessarily.

Filter events by relevant event types and package information.

Avoid:

* Infinite loops
* Excessive CPU usage
* Memory leaks
* Unnecessary logging
* Accessibility abuse

Clearly explain to the user why Accessibility permission is needed.

---

# 19. USAGE TRACKING

Use:

```text
UsageStatsManager
```

to retrieve application usage data.

Track:

* App
* Start/foreground time where available
* Duration
* Daily usage
* Focus-session usage
* Block attempts

MVP analytics:

```text
Today's usage

YouTube       42 min
Instagram     31 min
Chrome        24 min
Maps           8 min
```

Also calculate:

```text
Blocked attempts
Focus sessions
Focus completion
Time spent in managed apps
```

Do not claim precise real-time tracking if the underlying Android API cannot provide it.

---

# 20. ROOM DATABASE

Use Room.

Recommended entities:

```text
InstalledAppEntity

FocusSessionEntity

AppRuleEntity

BlockedAttemptEntity

ExitAttemptEntity

FocusGoalEntity
```

Possible relationships:

```text
FocusSession
    │
    ├── BlockedAttempts
    ├── ExitAttempts
    └── FocusGoal
```

Create DAOs and repositories.

Use Flow/StateFlow for reactive UI updates.

Do not place database operations directly inside Composables.

---

# 21. SETTINGS

Create a settings screen containing:

```text
General
────────────
Default launcher status
Always available apps

Focus
────────────
Default focus duration
Default focus mode
Adaptive friction
Focus goal

Blocking
────────────
Managed apps
Blocked apps
Bypass protection

Privacy
────────────
Local-only analytics
Data reset

System
────────────
Accessibility status
Usage access status
Battery optimization status
```

The settings screen should clearly show which permissions are missing.

---

# 22. ONBOARDING

The onboarding should be short and clear.

Step 1:

```text
Welcome to Minimal Phone
```

Explain the purpose.

Step 2:

```text
Set as Default Launcher
```

Step 3:

```text
Enable Usage Access
```

Step 4:

```text
Enable Accessibility
```

Step 5:

```text
Battery Optimization
```

Step 6:

```text
Your apps are ready.
```

Do not ask for permissions without explaining why.

---

# 23. BOOT PERSISTENCE

The application should recover correctly after reboot.

Use:

```text
BOOT_COMPLETED
```

where appropriate.

Restore:

* Focus session state
* App rules
* Preferences
* Blocking configuration

Do not assume background services will automatically remain alive on every OEM.

Detect service state and provide guidance.

---

# 24. BATTERY / OEM HANDLING

Android manufacturers may kill background processes.

Support common OEM behavior as much as reasonably possible.

Provide a diagnostics page:

```text
Accessibility      ✓
Usage Access       ✓
Default Launcher   ✓
Battery status     ⚠
Focus Service      ✓
```

If battery restrictions may interfere, explain the required user action.

Do not claim 100% enforcement.

Test on physical devices when possible.

Prioritize testing on:

* Samsung
* Xiaomi
* OPPO
* OnePlus
* Pixel/reference Android

---

# 25. EMERGENCY / ESSENTIAL ACCESS

Never block:

* Emergency calling
* Essential phone functionality

Always preserve the ability to recover the device.

Always Available applications should remain accessible.

The application should never create a situation where the user cannot access essential communication or system recovery functionality.

---

# 26. PRIVACY

The MVP should be local-first.

Do NOT implement:

* User accounts
* Cloud analytics
* Advertising
* Tracking
* Social profiles
* Remote monitoring

Usage data should remain on the device.

Clearly document:

```text
What is collected
Why it is collected
Where it is stored
How it can be deleted
```

---

# 27. UI DESIGN

Design language:

* Minimal
* Calm
* Typography-focused
* Low visual noise
* Dark/light theme
* High readability
* Fast
* Accessible

Avoid:

* Gamification overload
* Excessive gradients
* Large illustrations
* Unnecessary animations
* Notification-like red warning screens

Use subtle animation only where useful, especially countdowns and state transitions.

---

# 28. HOME SCREEN INFORMATION

The home screen can show:

```text
Current time
Date
Focus state
Remaining focus time
Allowed apps
```

Optional:

```text
Today's blocked attempts
```

Do not overload the home screen with statistics.

The launcher should feel like a tool, not an analytics dashboard.

---

# 29. ANALYTICS DASHBOARD

Implement basic analytics after the core blocking system works.

Display:

```text
Today's screen time
Weekly screen time
Blocked attempts
Focus sessions
Completed sessions
Focus completion rate
```

Later support behavioral insights such as:

```text
Most bypass attempts:
Monday
2–4 PM

Most attempted app:
YouTube
```

Do NOT implement ML-based behavioral prediction in MVP.

Keep the architecture ready for future analytics.

---

# 30. FUTURE FEATURES — DO NOT BUILD IN MVP

Keep these in architecture/documentation but DO NOT implement them until the MVP is stable:

### Root/Magisk enforcement

Optional stronger enforcement for rooted devices.

### Contextual notification digest

Show only important notifications during focus.

### Advanced behavioral analytics

Correlate:

```text
time of day
day of week
application
bypass reason
focus success
```

### Calendar integration

Automatically schedule study sessions.

### Exam Mode

Student-focused preset:

```text
Exam Mode
    ↓
Predefined distraction blocklist
    ↓
Calendar-linked sessions
    ↓
Focus streaks
```

### Cloud synchronization

Only consider after privacy architecture is established.

---

# 31. SECURITY / SAFETY PRINCIPLES

Do not implement malicious persistence.

Do not hide the application from Android settings.

Do not prevent the user from uninstalling the application through exploitative techniques.

Do not attempt to bypass Android security mechanisms.

Do not exploit AccessibilityService beyond its legitimate documented purpose.

The goal is behavioral friction, not device imprisonment.

---

# 32. DEVELOPMENT MILESTONES

Build the application in this exact order.

## MILESTONE 0 — PROJECT FOUNDATION

Create:

* Gradle project
* Modules
* Version catalog
* Hilt
* Compose
* Room
* Basic navigation
* Documentation

Before moving forward:

```text
BUILD SUCCESSFUL
```

---

## MILESTONE 1 — LAUNCHER SHELL

Implement:

* HOME activity
* Minimal Compose UI
* Static sample apps
* Default launcher setup
* App launching

Exit criteria:

```text
Application can become default launcher.
Home button returns to Minimal Phone.
Visible apps launch correctly.
```

Do NOT implement blocking yet.

---

## MILESTONE 2 — REAL APP DISCOVERY

Implement:

* LauncherApps
* PackageManager integration
* Installed app scanning
* Classification
* Dynamic app list
* Install/uninstall callbacks

Exit criteria:

```text
Install app
    ↓
Minimal Phone detects it

Uninstall app
    ↓
Minimal Phone removes it
```

---

## MILESTONE 3 — APP MANAGEMENT

Implement:

* Always Available apps
* Managed apps
* Whitelist UI
* App rules
* Room persistence

Exit criteria:

```text
User can change managed/allowed state.
Changes persist after restart.
```

---

## MILESTONE 4 — USAGE TRACKING

Implement:

* UsageStatsManager
* Daily usage
* App usage repository
* Basic analytics

Exit criteria:

```text
Actual device usage appears correctly.
```

---

## MILESTONE 5 — BLOCKING

This is the highest-risk milestone.

Implement:

* AccessibilityService
* Foreground detection
* Managed-app blocking
* Redirect behavior
* Physical-device testing

Exit criteria:

```text
Focus Mode ON

Open blocked application
        ↓
Application is blocked
        ↓
Minimal Phone returns to foreground
```

Test multiple applications.

---

## MILESTONE 6 — FOCUS SESSIONS

Implement:

* Start/end times
* Manual focus mode
* Focus modes
* Focus goal
* Session state
* Session persistence

Exit criteria:

```text
Start focus session
       ↓
Managed apps blocked
       ↓
Session ends
       ↓
Apps become available
```

---

## MILESTONE 7 — EXIT FRICTION

Implement:

* Countdown
* Focus goal reminder
* Adaptive friction
* Exit attempt recording
* "Why are you opening this?"
* Strict/Deep modes

Exit criteria:

```text
User cannot immediately disable a focus session
through the normal UI.
```

There must always be a recovery path.

---

## MILESTONE 8 — BYPASS PROTECTION

Test:

```text
Launcher tap
Notification tap
Deep link
Recent Apps
External intent
```

Use Accessibility as the backstop.

Exit criteria:

```text
Managed applications cannot easily bypass
focus restrictions through common routes.
```

---

## MILESTONE 9 — BOOT + OEM RESILIENCE

Implement/test:

* Boot receiver
* Focus restoration
* Service recovery
* Battery guidance
* Diagnostics

Test physical devices.

---

## MILESTONE 10 — POLISH

Implement:

* Onboarding
* Settings
* Analytics
* Error handling
* Empty states
* Accessibility
* Dark/light theme
* Performance improvements
* Crash handling

---

# 33. TESTING REQUIREMENTS

Write tests for:

### Unit tests

* App classification
* Focus state
* Blocking decisions
* Exit friction
* Adaptive friction
* Session calculations

### Database tests

* Insert/update/delete
* Session persistence
* App rule persistence

### UI tests

* Launcher
* App list
* Focus creation
* Exit friction
* Settings

### Integration tests

```text
App discovery → DB → UI

Focus session → blocking

Exit attempt → friction → session

Boot → state restoration
```

---

# 34. DEBUG / DIAGNOSTICS SCREEN

Create an internal debug screen during development.

Show:

```text
Default Launcher: YES/NO
Accessibility: YES/NO
Usage Access: YES/NO
Battery unrestricted: YES/NO

Focus Mode: ON/OFF
Session End: timestamp

Foreground App:
package.name

Last Accessibility Event:
event type

Last Block:
package.name

Bypass Attempts:
count
```

This screen can be hidden or disabled in release builds.

---

# 35. LOGGING

Use structured logging.

Create meaningful tags such as:

```text
Launcher
AppDiscovery
FocusEngine
BlockingService
Accessibility
UsageStats
Database
BootRecovery
```

Never log:

* passwords
* sensitive user content
* notification contents unnecessarily
* private messages

Remove verbose development logging from release builds.

---

# 36. ERROR HANDLING

Never silently fail.

If Accessibility permission is missing:

```text
Accessibility is required for bypass protection.

[Open Settings]
```

If Usage Access is missing:

```text
Usage Access is required for screen-time statistics.

[Open Settings]
```

If the app is not the default launcher:

```text
Minimal Phone is not your default launcher.

[Set as Default]
```

---

# 37. PERFORMANCE

The launcher must start quickly.

Avoid:

* Heavy database queries during startup
* Blocking the main thread
* Continuous polling
* Excessive accessibility processing
* Unnecessary recompositions
* Memory-heavy application icons

Use:

* Coroutines
* Background dispatchers
* Caching
* Lazy lists
* StateFlow
* Efficient icon loading

---

# 38. IMPORTANT IMPLEMENTATION RULE

Do NOT try to build the entire application in one pass.

Work milestone-by-milestone.

After each milestone:

1. Build the project.
2. Run tests.
3. Inspect compiler errors.
4. Fix issues.
5. Run the application.
6. Verify functionality.
7. Update documentation.
8. Only then proceed.

If an Android API behaves differently than expected:

STOP.

Investigate the correct Android-supported implementation before inventing a workaround.

Do not create fake APIs or placeholder implementations for core functionality.

---

# 39. SOURCE CODE QUALITY

Follow:

* SOLID principles
* Clean Architecture
* Dependency inversion
* Small focused classes
* Meaningful names
* Kotlin idioms
* Immutable UI state where practical
* Repository pattern
* Use-case pattern
* Dependency injection

Avoid:

```text
God classes
Huge Activities
Huge Composables
Global mutable state
Hardcoded package lists
Business logic in UI
Database calls in UI
```

---

# 40. DOCUMENTATION

Maintain:

```text
docs/prd.md
docs/architecture.md
docs/permissions.md
docs/testing.md
docs/risks.md
```

Document important Android limitations.

Especially document:

* Accessibility limitations
* Launcher behavior
* OEM battery restrictions
* Android version differences
* Stock Android enforcement limitations
* Permission requirements

---

# 41. PRODUCT SUCCESS CRITERIA

The MVP is successful when:

### Launcher

```text
User can set Minimal Phone as default launcher.
```

### Application discovery

```text
Installed apps appear dynamically.
```

### Classification

```text
Essential apps remain available.
Managed apps can be controlled.
```

### Blocking

```text
Managed apps are blocked during Focus Mode.
```

### Focus

```text
User can create and complete a focus session.
```

### Exit friction

```text
Leaving a session requires deliberate action.
```

### Bypass protection

```text
Common alternate launch routes are detected and handled.
```

### Persistence

```text
Rules survive application restart and reboot where Android permits.
```

### Performance

```text
Launcher remains responsive.
```

---

# 42. FINAL PRODUCT PHILOSOPHY

Minimal Phone should feel like:

```text
Less phone.
More intention.
```

The experience should communicate:

> "Your phone is a tool. You decide what deserves your attention."

Not:

> "The application controls your phone."

The product should reduce impulsive behavior through:

```text
Minimal UI
      +
Intentional app access
      +
Focus sessions
      +
Reliable blocking
      +
Bypass resistance
      +
Exit friction
      +
Behavioral feedback
```

---

# 43. FIRST ACTION

Before writing substantial code:

1. Inspect the current project directory.
2. Determine whether an Android project already exists.
3. Inspect installed Android Studio/Gradle/JDK/SDK versions.
4. Determine the available Android SDK.
5. Create a short implementation plan.
6. Create/update the project documentation.
7. Then begin MILESTONE 0.

Do NOT ask me to manually create every file.

You are expected to create the project structure and implementation yourself.

However, do not blindly proceed through all milestones.

Complete one milestone, verify it, and then continue.

At every major milestone, report:

```text
MILESTONE:
STATUS:
IMPLEMENTED:
TESTED:
KNOWN ISSUES:
NEXT STEP:
```

Start now with **MILESTONE 0 — PROJECT FOUNDATION**.

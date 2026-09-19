# Product Requirements Document (PRD) — NumbPhone

## 1. Executive Summary
NumbPhone is a distraction-resistant Android launcher and digital wellbeing application designed to transform modern Android smartphones into intentional tools ("dumb phone" simplicity with modern reliability). The core philosophy is: **"Make intentional actions easy and impulsive actions deliberate."**

## 2. Target Audience
- **Primary Audience:** Students preparing for competitive exams who need zero-distraction environments without missing essential communication.
- **Secondary Audience:** Software developers, knowledge workers, minimalists, and parents seeking structured phone boundaries.

## 3. Key Value Propositions
1. **Calm Minimal UI:** No app drawer, no recommendation feeds, no visual clutter. High-contrast typography displaying only permitted apps.
2. **Dynamic Application Classification:**
   - **Essential / Always Available:** Phone, SMS/Messages, Camera, Clock, Contacts, Settings, Maps, and explicitly designated essential tools.
   - **Managed:** Social media (YouTube, Instagram, TikTok, Twitter/X), entertainment, games, and non-essential apps subject to focus rules.
3. **Dedicated Focus Engine:** Support for Light, Strict, and Deep focus modes with customized goals (e.g. "Prepare for ML Exam").
4. **Adaptive Exit Friction:** Progressive delays (5s, 30s, 2m, etc.) and goal reflection prompt ("Why are you opening this?") to eliminate impulse opening.
5. **Accessibility Backstop:** Real-time interception against secondary bypass avenues (notifications, recent apps list, external deep-links).
6. **Local-First Privacy:** Zero cloud telemetry, zero account requirement, full on-device data persistence.

## 4. User Journey
1. **Onboarding:** Explanation of launcher purpose, permissions request (Default Launcher, Usage Access, Accessibility, Battery).
2. **App Discovery:** Device is scanned dynamically via `LauncherApps`; applications are classified into Essential vs Managed.
3. **Home Experience:** Clean typography clock, date, active focus indicator, and list of permitted applications.
4. **Focus Activation:** User selects goal, duration, and focus mode. Managed apps are hidden and locked.
5. **Exit / Interception:** Deliberate countdown with goal reminder when user attempts premature exit.

## 5. Non-Functional Requirements
- **Startup Latency:** Launcher home screen renders in <100ms.
- **Battery Efficiency:** Minimal background overhead; event-filtered Accessibility scanning.
- **Reliability:** State recovery after system reboot (`BOOT_COMPLETED`).
- **Safety:** Always preserve emergency calling and system settings access.

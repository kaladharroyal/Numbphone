# Technical Risks & Mitigation Strategies — NumbPhone

## 1. OEM Aggressive Background Killing (Samsung, Xiaomi, Huawei, OnePlus)
- **Risk:** OEM battery managers may kill background services, leading to missed focus session end-times or delayed timers.
- **Mitigation:**
  - Design the `FocusEngine` to rely on **absolute wall-clock timestamps** (`startTime` & `endTime`) stored in Room/DataStore rather than live running in-memory timers.
  - When the app returns to foreground or accessibility triggers, it compares `System.currentTimeMillis()` against `endTime`.
  - Provide a dedicated OEM Diagnostics screen guiding users to disable aggressive battery optimization.

## 2. Accessibility Service Disablement or Battery Suspension
- **Risk:** User or OS may disable Accessibility permissions, breaking bypass detection.
- **Mitigation:**
  - Launch engine directly enforces blocking at the launcher level as the primary gate.
  - Diagnostics and Settings show real-time health indicator for Accessibility status with a one-tap recovery link.

## 3. Package Visibility Restrictions (Android 11+)
- **Risk:** `QUERY_ALL_PACKAGES` is subject to Play Store policy review.
- **Mitigation:**
  - NumbPhone is a launcher application (`CATEGORY_HOME`), which qualifies for standard Play Store exception for full package visibility.
  - Use `LauncherApps.getActivityList()` which is permitted for default launchers.

## 4. Work Profiles & Multiple Users
- **Risk:** Managed work profile applications might have distinct `UserHandle` instances.
- **Mitigation:**
  - Use `LauncherApps` API passing `android.os.Process.myUserHandle()` and iterating across profiles returned by `UserManager.getUserProfiles()`.

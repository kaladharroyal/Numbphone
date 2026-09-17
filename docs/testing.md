# Testing Strategy — Minimal Phone

## 1. Unit Tests
- **Launch Engine:** Unit tests for `EvaluateAppLaunchUseCase` covering:
  - Essential app launch when Focus is INACTIVE (ALLOW).
  - Essential app launch when Focus is ACTIVE (ALLOW).
  - Managed app launch when Focus is INACTIVE (ALLOW).
  - Managed app launch when Focus is ACTIVE in Light/Strict/Deep modes (BLOCK / SHOW_FRICTION).
- **Focus Engine:** Unit tests for session creation, countdown math, state transitions, and expiry.
- **Exit Friction:** Unit tests for adaptive delay calculation (attempt count progression: 5s, 30s, 120s, 600s).
- **Recovery System:** Unit tests for `BOOT_COMPLETED` state restoration (validating remaining duration vs expired duration).

## 2. Database & Repository Tests
- In-memory Room database tests for `InstalledAppDao`, `AppRuleDao`, `FocusSessionDao`, and `BlockedAttemptDao`.
- Foreign key constraints, conflict strategies, and reactive Flow emissions.

## 3. Integration & Physical Device Verification Gate
- **Launcher Validation:** Set as default launcher, verify Home button returns to Minimal Phone.
- **Dynamic App Lifecycle:** Install a sample app via adb; verify instant appearance in Minimal Phone without manual refresh.
- **Accessibility Interception:** Start focus session, send test notification with deep link to a managed app; verify immediate redirect to launcher.
- **Reboot Recovery:** Initiate 10-minute focus session, execute `adb reboot`; verify session state restores with correct remaining time.

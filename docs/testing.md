# Testing Strategy — NumbPhone

## 1. Automated Testing Setup
- **Unit Tests:** Run via `test` Gradle task for data layers, domain use cases, and focus calculation logic.
- **Compose UI Tests:** In-memory testing of screen composables using Compose Test Rule.
- **State Recovery Tests:** Validate state serialization / deserialization under process death and phone restart simulations.

## 2. Manual Test Matrix

### Core Launcher Loop
- **Launcher Validation:** Set as default launcher, verify Home button returns to NumbPhone.
- **Dynamic App Lifecycle:** Install a sample app via adb; verify instant appearance in NumbPhone without manual refresh.
- **Accessibility Interception:** Start focus session, send test notification with deep link to a managed app; verify immediate redirect to launcher.
- **Reboot Recovery:** Initiate 10-minute focus session, execute `adb reboot`; verify session state restores with correct remaining time.

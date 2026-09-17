# Permissions Guide — Minimal Phone

## 1. Required & Optional Permissions

| Permission | Purpose | Essential / Optional | Fallback if Denied |
|---|---|---|---|
| `android.intent.category.HOME` | Standard Android launcher registration | Essential | App operates as standard activity |
| `QUERY_ALL_PACKAGES` / `LauncherApps` | Discover installed launchable applications | Essential | Fallback to standard package queries |
| `PACKAGE_USAGE_STATS` | Track daily app usage & screen-time metrics | Optional | Screen-time screen displays permission prompt |
| `BIND_ACCESSIBILITY_SERVICE` | Intercept secondary app launch routes (notifications, recents) | Highly Recommended | Central launcher launch engine still operates |
| `RECEIVE_BOOT_COMPLETED` | Restore active focus session after reboot | Recommended | Session restores on next manual launch |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevent OEM background killing of focus timer | Recommended | Session time recalculated via timestamps on app reopen |
| `POST_NOTIFICATIONS` | Foreground service indicator for active focus sessions | Optional | In-app focus banner only |

## 2. Accessibility Service Scope & Transparency
- **What it monitors:** Only foreground window changes (`TYPE_WINDOW_STATE_CHANGED`).
- **Data retention:** Zero keystroke tracking, zero screen reading, zero credential logging.
- **Action taken:** When an active focus session is running and a managed package is brought to foreground via notification/recents, the service triggers `ACTION_MAIN / CATEGORY_HOME` to return the user to Minimal Phone.

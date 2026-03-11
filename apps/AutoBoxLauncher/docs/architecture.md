# AutoBox Launcher — Project Overview

## Purpose

AutoBox Launcher is a **privileged system app** built for the AutoBox ROM (LineageOS 23.2 / Android 16) running on the OnePlus 8 (Snapdragon 865). It is the primary in-car UI that starts automatically on boot and occupies the full screen.

It is **not a home screen replacement** — it does not declare `CATEGORY_HOME` and does not act as an Android launcher. It is a regular activity that runs in **permanent lock task mode** (`lockTaskMode="always"`), which prevents the user from leaving it via Android navigation. Exit is only available through an explicit button in Settings.

---

## Device & ROM Context

| Property | Value |
|---|---|
| Device | OnePlus 8 (`instantnoodle`, SM8250) |
| ROM | AutoBox (LineageOS 23.2, Android 16) |
| Screen orientation | Landscape (forced, 1080 × 2400 rotated 90°) |
| App type | Privileged system app (`/system/priv-app/`) |
| Package | `com.autobox.autoboxlauncher` |
| Signing | Platform certificate |

---

## Screens & Navigation

Navigation is handled by a bottom navigation bar with 5 tabs. The bar is 52 dp tall with no labels, icons only.

### 1. Home

Three-column layout filling the full landscape screen:

**Left column — Driver Info**
- Current speed (from GPS via `SpeedRepository`)
- Active speed limit sign (camera-based recognition via `SignsRepository` + `CameraCapture`)
- Other recognized road sign (warnings, prohibitions, etc.)
- Road signs cover German and Polish traffic sign sets

**Center column — Navigation Map**
- Embedded TomTom SDK map (`TomTomMapView`)
- Route planning: tap destination on map → calculate route → start turn-by-turn navigation
- Active navigation shows: next instruction, distance remaining, arrival time
- Guidance announcements via TomTom Navigation SDK

**Right column — Media Widget**
- Media controls for the currently connected media app
- Album art, track title, artist
- Play/Pause, Previous, Next
- Auto-connected to any installed media app (via `MediaBrowserService`)
- Configurable in Settings

### 2. Map

The TomTom map widget from the Home center column expands to fill the entire screen. Full navigation functionality available. Switching away from this screen does not destroy the map instance (controlled via a ready-gate in `TomTomMapView.kt` to prevent "MapView already created" crashes).

### 3. Multimedia

The media widget from the Home right column expands to fill the entire screen. Full media controls, album art, progress bar.

### 4. Calls

Incoming/outgoing call screen. Powered by `CarInCallService` (binds as `InCallService` with `IN_CALL_SERVICE_UI = true`). Shows caller name (resolved from contacts), number, call duration. Accept / Decline / Hang up buttons. When a call arrives, the app automatically navigates to this screen regardless of which tab is active.

### 5. Settings

- **Theme** — Light / Dark / Auto
- **Unit system** — Metric / Imperial (affects speed display)
- **Media app** — select from installed media apps
- **Sign recognition** — enable/disable camera-based traffic sign recognition
- **Brightness** — screen brightness slider
- **Volume** — media volume slider
- **Exit** — show system bars and finish the activity (leaves lock task mode)

---

## Architecture

The app uses a lightweight **MVVM** approach (not full MVI — there is no Reducer or explicit Intent layer). State is held in `MainViewModel` using Compose `State` and `StateFlow`.

```
AutoBoxApplication
└── MainActivity
    ├── MainViewModel
    │   ├── MediaRepository       — MediaBrowser connection, playback control
    │   ├── SpeedRepository       — GPS-based speed via LocationManager
    │   ├── SignsRepository       — Traffic sign recognition results (StateFlow)
    │   ├── CameraCapture         — Camera2 capture → byte array → SignsRepository
    │   └── TomTomSdk.navigation  — Turn-by-turn navigation state
    └── UI (Jetpack Compose)
        ├── HomeScreen
        ├── MapScreen
        ├── MultimediaScreen
        ├── CallScreen
        └── SettingsScreen
```

Settings persistence: **DataStore Preferences** (`SettingsDataStore`).

---

## Key Technical Decisions

### Lock Task Mode
The activity declares `android:lockTaskMode="always"` in the manifest. This grants `LOCK_TASK_AUTH_LAUNCHABLE_PRIV` authentication, which permanently pins the app without requiring Device Owner or user confirmation dialogs. GApps compatibility is maintained.

A framework-level fix was applied to `ActivityTaskSupervisor.java` and `DisplayContent.java` to defer the lock task transition until after the `endFixedRotation` viewport commit to InputFlinger, preventing touch coordinate desync on first launch.

### Navigation Transitions
All NavHost transitions are disabled (`EnterTransition.None` / `ExitTransition.None`). This ensures `TomTomMapView.onDispose` fires before the next screen's map is created, avoiding the "MapView already created" SDK crash.

### Rotation
The display is locked to landscape at the system level. `attachBaseContext` writes `ACCELEROMETER_ROTATION=0` / `USER_ROTATION=1` to `Settings.System`. `BootReceiver` freezes rotation to `ROTATION_90` after boot via `IWindowManager`.

### Map Lifecycle
`TomTomMapView` uses a `ready-gate` StateFlow: the composable waits until the previous map instance is fully disposed before creating a new one. This is necessary because the TomTom SDK enforces a single-instance constraint per process.

---

## Permissions

| Permission | Purpose |
|---|---|
| `ACCESS_FINE_LOCATION` | GPS speed + map positioning |
| `CAMERA` | Traffic sign recognition |
| `MEDIA_CONTENT_CONTROL` | MediaBrowser privileged binding |
| `READ_PHONE_STATE` / `READ_CONTACTS` / `READ_CALL_LOG` | Call screen |
| `ANSWER_PHONE_CALLS` / `CALL_PHONE` / `MANAGE_OWN_CALLS` | Call handling |
| `WRITE_SETTINGS` | Lock screen rotation via `Settings.System` |
| `SET_ORIENTATION` | `IWindowManager.freezeRotation` |
| `RECEIVE_BOOT_COMPLETED` | `BootReceiver` for rotation fix |

Privileged permissions are whitelisted in `permissions/privapp-permissions-AutoBoxLauncher.xml`.

# AutoBox Launcher — AI Development Rules

These rules apply to all AI-assisted code generation and modification within this project.

> **Note:** Despite the name, AutoBox Launcher is **not an Android launcher** (no `CATEGORY_HOME`). It is a privileged system app that runs pinned via `lockTaskMode="always"`.

---

## 1. Language

- **Kotlin only.** Do not write Java unless a specific library or system API has no Kotlin alternative.
- Use idiomatic Kotlin: extension functions, data classes, sealed classes, `when` expressions, named parameters.
- Avoid Java-style verbosity (no `get`/`set` methods when properties suffice, no `!= null` when `?.let` is cleaner).

---

## 2. UI Framework

**Jetpack Compose exclusively.** No XML layouts.

Rules:
- All UI is `@Composable` functions.
- Use **Material3** components and theming (`MaterialTheme.colorScheme`, `MaterialTheme.typography`).
- State must be hoisted to `ViewModel` or the nearest appropriate owner — not held inside composables unless truly local.
- Use `remember`, `derivedStateOf`, `LaunchedEffect`, `SideEffect` correctly; do not use them as workarounds.
- Keep composable functions small and single-purpose.
- Previews (`@Preview`) are welcome but not required.

---

## 3. Architecture

The project uses **MVVM**:

```
Repository / SDK layer
       ↓
   ViewModel  (MainViewModel — single shared VM for the whole app)
       ↓
   UI (Compose screens)
```

Rules:
- Business logic lives in `ViewModel` or `Repository`. Screens are dumb — they observe state and emit events.
- State exposed from `ViewModel` uses `State<T>` (for simple values) or `StateFlow<T>` (for streaming data).
- Do not add new `ViewModel` classes unless a screen has clearly independent, non-shared state.
- Do not use `MutableState` inside `Repository` or data-layer classes — use `Flow` / `StateFlow` there.

---

## 4. Dependency Injection

**No DI framework** (no Hilt, no Koin). The app is a privileged system app with `platform_apis = true`. Manual construction in `ViewModel` and `Application` is sufficient and keeps the build simple.

Rules:
- Instantiate repositories in `MainViewModel`'s constructor or `init` block.
- Pass dependencies via constructor parameters, not singletons.
- Do not add Hilt or Koin unless explicitly requested.

---

## 5. Asynchronous Programming

Use **Kotlin Coroutines** and **Flow**.

Rules:
- All async work runs inside `viewModelScope.launch { }` or `viewModelScope.async { }`.
- Data streams are `Flow<T>` at the repository level, converted to `StateFlow<T>` in `ViewModel` via `.stateIn(viewModelScope, ...)`.
- Use `SharingStarted.WhileSubscribed(5_000)` for UI-facing `StateFlow`s.
- **Never** use `Thread`, `AsyncTask`, `Handler.post` for business logic.
- **Never** use `runBlocking` on the main thread.
- Prefer `suspend fun` over callback-based APIs where possible.

---

## 6. Navigation

Use **Navigation Compose** (`NavHost`, `rememberNavController`).

Rules:
- Screens are registered in `MainActivity.kt` inside the single `NavHost`.
- Routes are defined in `Screen.kt` as a `sealed class`.
- **All transitions must remain disabled** (`EnterTransition.None` / `ExitTransition.None`). This is required to prevent TomTom MapView lifecycle crashes — do not change this.
- Deep links and back-stack manipulation must be done through the `NavController` only.

---

## 7. State Management

- Use `StateFlow` for all repository-level streams.
- Use `State<T>` (Compose `mutableStateOf`) for UI-only state in `ViewModel`.
- **Do not use `LiveData`** — the project uses Compose, not XML/Fragment UI.
- Immutability: prefer `val` over `var`, `copy()` for updating data classes.

---

## 8. System Integration

This is a **privileged system app** on a custom ROM. Some rules differ from standard app development:

- **`IWindowManager` and `ServiceManager`** — direct system service calls are allowed and used. Do not replace them with public APIs that may not work for privileged apps.
- **`Settings.System`** — writing to `Settings.System` is allowed (the app holds `WRITE_SETTINGS`).
- **Lock task mode** — do not remove or work around `lockTaskMode="always"`. This is a core requirement.
- **Platform certificate** — the app is signed with the platform key. Do not add certificate-pinning or signature checks.
- **`android.os.ServiceManager`** — allowed for direct binder access to system services.

---

## 9. Project Structure

```
src/
└── com/autobox/autoboxlauncher/
    ├── AutoBoxApplication.kt
    ├── MainActivity.kt
    ├── MainViewModel.kt
    ├── SettingsDataStore.kt
    ├── BootReceiver.kt
    ├── call/
    │   ├── CarInCallService.kt
    │   ├── CallRepository.kt
    │   ├── CallState.kt
    │   └── ContactsRepository.kt
    ├── media/
    │   ├── MediaRepository.kt
    │   └── MediaState.kt
    ├── speed/
    │   └── SpeedRepository.kt
    ├── signs/
    │   ├── SignsRepository.kt
    │   └── CameraCapture.kt
    └── ui/
        ├── Screen.kt
        ├── HomeScreen.kt
        ├── MapScreen.kt
        ├── MultimediaScreen.kt
        ├── CallScreen.kt
        ├── SettingsScreen.kt
        ├── components/
        │   ├── TomTomMapView.kt
        │   └── LauncherIcons.kt
        └── theme/
            ├── Theme.kt
            ├── Color.kt
            └── Type.kt
```

Rules:
- New screens go in `ui/`.
- New data sources go in their own subdirectory (e.g., `weather/WeatherRepository.kt`).
- Shared UI components go in `ui/components/`.
- Do not flatten the structure — keep the feature-based grouping.

---

## 10. SDK Versions

```
minSdk     = 35   (Android 16 ROM only)
targetSdk  = 35
compileSdk = 35
```

Do not add `minSdk` version guards (`if (Build.VERSION.SDK_INT >= ...)`) — this app only runs on Android 16.

---

## 11. Testing

Unit tests use:
- **JUnit 5**
- **Mockito-Kotlin**
- **Turbine** (for Flow testing)

Integration and UI tests are not required for in-ROM iteration. Focus on unit-testing repositories and ViewModel logic.

---

## 12. Code Style

- Follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Max line length: **120 characters**.
- No trailing whitespace.
- Single blank line between top-level declarations.
- `TODO` comments must include a description — no empty `TODO`.
- Do not leave commented-out code in committed files.

---

## 13. Forbidden APIs & Patterns

| Forbidden | Reason |
|---|---|
| `AsyncTask` | Deprecated, removed in API 33 |
| `LiveData` | Replaced by `StateFlow` in Compose apps |
| `Fragment` / XML layouts | Project is fully Compose |
| `ButterKnife` / synthetic imports | Obsolete |
| `runBlocking` on main thread | Blocks UI |
| Hilt / Koin | Not used in this project |
| `Handler.postDelayed` for business logic | Use coroutine `delay` instead |
| Transition animations in NavHost | Breaks TomTom MapView lifecycle |

---

## 14. Output Quality Requirements

All generated code must:

- Compile without warnings on the first attempt.
- Not introduce new dependencies unless explicitly approved.
- Be self-contained — no placeholder `TODO` implementations unless specifically requested.
- Follow the existing patterns in the codebase (naming, structure, state management).
- Not break existing functionality — understand the impact before modifying shared components like `MainViewModel` or `MainActivity`.

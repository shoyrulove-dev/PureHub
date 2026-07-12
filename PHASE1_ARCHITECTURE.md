# PureHub Phase 1

## Package Tree

```text
app/src/main/java/com/purehub/app
├── MainActivity.kt
├── data
│   └── CommunityPreferences.kt
├── feature
│   ├── cleaner
│   │   ├── CleanerRepository.kt
│   │   └── CleanerViewModel.kt
│   ├── compass
│   │   ├── CompassSensorManager.kt
│   │   └── CompassViewModel.kt
│   └── lunar
│       └── LunarCalendarConverter.kt
├── navigation
│   └── PureHubDestination.kt
└── ui
    ├── PureHubApp.kt
    ├── screens
    │   ├── BoostScreen.kt
    │   ├── CleanerScreen.kt
    │   ├── CommunityScreen.kt
    │   ├── CompassScreen.kt
    │   ├── HomeScreen.kt
    │   ├── LunarCalendarScreen.kt
    │   ├── ScanScreen.kt
    │   └── ToolsScreen.kt
    └── theme
        ├── Theme.kt
        └── Type.kt
```

## Phase 1 Deliverables

- `NavHost` with 5 bottom tabs: `Home`, `Tools`, `Scan`, `Boost`, `Community`
- `DataStore Preferences` for Pro unlock state
- Telegram deep link + browser fallback for community growth flow
- Build dependencies prepared for Compose, Navigation, Coroutines, DataStore, CameraX, and bundled offline ML Kit scanning
- `AndroidManifest.xml` intentionally omits `INTERNET`

## Current Live Modules

- Home: solar/lunar summary, can-chi, holiday markers, embedded lunar calendar
- Tools: live compass preview
- Boost: MediaStore-based deep cleaner with review/delete selection
- Community: Pro unlock DataStore flow

## Phase 2 Targets

- Bill Splitter
- QR Studio
- Doc to PDF
- Zen Pomodoro with local white noise

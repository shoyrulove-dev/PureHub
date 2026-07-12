# PureHub Phase 1 Architecture

`PureHub` is organized as a single-activity, offline-first Android app with one shared navigation shell and strongly isolated feature modules by package.

## Package Tree

```text
app/
  src/
    main/
      java/com/purehub/app/
        MainActivity.kt
        PureHubApplication.kt
        data/
          CommunityPreferences.kt
          local/
            PureHubDatabase.kt
            dao/
              ExpenseDao.kt
              HabitCheckInDao.kt
              HabitDao.kt
            entity/
              ExpenseEntryEntity.kt
              HabitCheckInEntity.kt
              HabitEntity.kt
          security/
            VaultCipherManager.kt
            VaultRepository.kt
        di/
          AppModule.kt
        navigation/
          PureHubDestination.kt
        feature/
          lunar/
            data/
            domain/
              LunarCalendarConverter.kt
            ui/
          zenhabit/
            data/
            domain/
            ui/
          pomodoro/
            data/
            domain/
              PomodoroAudioManager.kt
              PomodoroViewModel.kt
            ui/
          zenbreath/
            data/
            domain/
            ui/
          compass/
            data/
            domain/
              CompassSensorManager.kt
              CompassViewModel.kt
            ui/
          bubblelevel/
            data/
            domain/
            ui/
          decibel/
            data/
            domain/
            ui/
          flashlight/
            data/
            domain/
            ui/
          converter/
            data/
            domain/
            ui/
          qr/
            data/
            domain/
              QrBitmapGenerator.kt
            ui/
          docpdf/
            data/
            domain/
              DocPdfRepository.kt
            ui/
          ocr/
            data/
            domain/
            ui/
          colorgrabber/
            data/
            domain/
            ui/
          cleaner/
            data/
            domain/
              CleanerRepository.kt
              CleanerViewModel.kt
            ui/
          speakercleaner/
            data/
            domain/
            ui/
          wifianalyzer/
            data/
            domain/
            ui/
          wallpaper/
            data/
            domain/
            ui/
          billsplitter/
            data/
            domain/
              BillPresetRepository.kt
              BillSplitterCalculator.kt
            ui/
          expensetracker/
            data/
            domain/
            ui/
          decisionwheel/
            data/
            domain/
            ui/
          community/
            data/
            domain/
              CommunityViewModel.kt
            ui/
        ui/
          AppMessage.kt
          PureHubApp.kt
          screens/
            SettingsScreen.kt
            TabOverviewScreen.kt
          theme/
            Theme.kt
            Type.kt
      res/
        raw/
        drawable/
        mipmap-anydpi-v26/
        values/
        xml/
          file_paths.xml
```

## Bottom Navigation Mapping

1. `Zen & Time`
   - Lunar Calendar
   - Zen Habit
   - Zen Pomodoro
   - Zen Breath
2. `Measure & Tools`
   - Compass
   - Bubble Level & Ruler
   - Decibel Meter
   - Smart Flashlight
   - Unit Converter
3. `Vision`
   - QR Studio
   - Doc to PDF
   - OCR Text Extractor
   - Color Grabber
4. `System & Security`
   - Deep Cleaner
   - Speaker Cleaner
   - WiFi Analyzer
   - Password Vault
   - Wallpaper Changer
5. `Finance & Fun`
   - Bill Splitter
   - Expense Tracker
   - Decision Wheel
   - Community Pro Unlock

## Storage Rules

- `Room`: structured local data for Zen Habit and Expense Tracker.
- `DataStore Preferences`: lightweight flags and Pro Unlock code.
- `EncryptedSharedPreferences`: future Password Vault secure secrets.
- `WorkManager`: wallpaper rotation and later background-safe local jobs.
- `MediaStore` and app-private files: document export, cleaner, scan assets.

## Phase Boundary

Phase 1 intentionally stops at architecture, dependencies, permissions, navigation scaffold, and database setup. Feature-specific Compose screens and offline engines for the new mini-apps start in `PROCEED TO PHASE 2 (Tab 1)`.

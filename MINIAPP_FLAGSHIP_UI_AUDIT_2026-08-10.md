# PureHub 22 Mini-App Flagship UI Audit

Date: 2026-08-10

## Outcome

- All 22 catalog entries resolve to working implementations on both PWA and Android.
- No `coming soon`, empty route, or compile-blocking mini-app was found.
- Shared PWA and Android UI is now denser, icon-led, and brand-consistent.
- Touch targets remain usable even though visual spacing was reduced.
- Three replacement icon concepts are stored in `marketing/brand/icon-concepts-2026-08-10/`; the launcher icon is intentionally unchanged until a concept is selected.

## Platform-wide improvements in this pass

- Compact PWA shell, page header, tool cards, search, promise tiles, panels, and bottom navigation.
- Icon-only status signals on small screens with accessible labels and hover titles.
- Technical route/storage copy removed from the primary mini-app flow.
- Compact Android home, tool list, mini-app scroll host, flagship header, and icon-first bottom navigation.
- PureHub brand colors are now stable on Android instead of being replaced by device dynamic colors.
- All tool cards are presented as flagship apps; the dashboard highlights four starting points instead of repeating all 22 in a second long list.

## 22-app audit

| Mini-app | Current flagship baseline | Remaining worthwhile improvement |
|---|---|---|
| Lunar Calendar | Local solar/lunar conversion, month view, traditional markers | Searchable events, favorites, optional local reminders |
| Zen Habit | Local habits, streaks, weekly insight | Backup/export and notification reminders |
| Zen Pomodoro | Presets, local timer, soundscapes, weekly totals | Android foreground timer notification and interruption recovery |
| Zen Breath | Guided patterns, animation, session totals | Haptics, reduced-motion mode, custom pattern builder |
| Compass | Live heading and calibration guidance | Accuracy indicator and magnetic-interference warning |
| Bubble Level | Two-axis level and calibration | Saved calibration profiles and ruler overlay |
| Decibel Meter | Live estimate, rolling average, safety context | User calibration and session CSV export; never market as certified SPL |
| Smart Flashlight | Torch/screen light and safety modes | Quick Settings tile and lock-screen-safe SOS controls |
| Unit Converter | Fast local conversion | More scientific categories and favorite conversion pairs |
| QR Studio | Scan, inspect, create, share, local history | Batch scan/export and stronger suspicious-link explanation |
| Doc to PDF | Capture/import, reorder, rotate, crop, export | Perspective correction and searchable-PDF handoff to OCR |
| OCR Studio | Camera/files, cleanup, language packs, edit/export | Layout preservation, searchable PDF, confidence visualization |
| Color Grabber | Camera sampling and color values | Saved palettes, contrast checker, CSS/design-token export |
| Deep Cleaner | Review-first scoped file cleanup | Duplicate hashing and Android media-category filters |
| Speaker Cleaner | Timed controlled frequencies and safety copy | Device presets and an optional automated sweep |
| WiFi Analyzer | Nearby network/channel insight on Android | Channel graph history; PWA remains limited by browser security APIs |
| Password Vault | Encrypted local entries and protected reveal | Encrypted backup/import, biometric unlock, Android Autofill integration |
| Wallpaper Changer | Local preview and scheduled rotation | Crop/focal-point editor and battery-aware scheduling controls |
| Bill Splitter | Equal/itemized split, tax and tip | Receipt OCR import, settlement links, rounding reconciliation |
| Expense Tracker | Local ledger, categories, budget | Recurring entries, charts, CSV/JSON backup |
| Decision Wheel | Local picker and editable choices | Weighted choices, saved sets, result history |
| PureHub Community | Tester links, roadmap and contribution flow | In-app release notes and privacy-safe issue template handoff |

## Priority recommendation

1. Document Suite integration: OCR Studio -> Doc to PDF -> searchable PDF.
2. Android reliability: Pomodoro foreground service, local reminders, encrypted backup.
3. User trust: calibration/accuracy messaging for Compass, Decibel, Bubble Level and WiFi.
4. Portability: a single encrypted export/import center for Habit, Expense, Vault and histories.

## Verification

- PWA lint: passed.
- PWA production build and service worker generation: passed.
- Android unit tests: passed.
- Android lint: passed.
- Android debug assemble: passed.
- Debug APK installed on connected device: passed with debug downgrade allowed because the existing debug build used a higher version code.
- Connected device security tests passed; two Compose navigation tests could not find the app hierarchy while the phone remained on the lock screen. This is an environment/device-lock limitation, not a compile failure.

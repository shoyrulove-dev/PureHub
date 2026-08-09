# PureHub Important Upgrades — 2026-08-10

Implemented for Android beta 14:

- OCR Studio now sends image pages and corrected OCR text into Doc to PDF. Exported documents include a searchable/copyable text layer.
- Expense Tracker and Bill Splitter can import a receipt image, run OCR fully on-device, and prefill merchant, total, tax, tip, and detected items for review.
- Zen Pomodoro now runs as a user-started foreground timer with a persistent countdown notification and completion alert.
- Settings includes password-based encrypted export/import for Habit, check-ins, Expense, Vault, and OCR/QR history. The portable `.purehub` envelope uses PBKDF2-HMAC-SHA256 and AES-256-GCM.
- Compass reports sensor accuracy and magnetic interference warnings.
- Bubble Level supports persistent zero calibration and motion/accuracy warnings.
- Sound Meter supports a persistent -20 dB to +20 dB calibration offset and keeps its non-certified measurement warning visible.

Verification:

- Android debug compile: passed.
- Android lint: passed with zero blocking errors.
- Receipt parser tests (English and Vietnamese totals): passed.
- Debug APK installed to the connected M1906G7G as a separate debug package.

Safety notes:

- Receipt OCR values always require user review before saving.
- Import replaces the backed-up local collections only after decrypting and validating the backup format.
- Sensor readings remain estimates and clearly show accuracy/calibration state.

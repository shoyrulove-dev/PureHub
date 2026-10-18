# PureHub 1.0.0-beta.59

PureHub beta.59 modernizes Android security and Wi-Fi integration while preserving existing local data.

## Secure local storage

- Migrated Authenticator, Password Vault, Lunar reminders, and router credentials to AES-256-GCM keys managed directly by Android Keystore.
- Added a non-destructive one-time migration from the former AndroidX encrypted preferences; legacy files remain available for rollback.
- Migration now retries after read failures instead of marking incomplete data as migrated.
- Added concurrency protection around key creation and on-device encryption tests.

## Android platform APIs

- Replaced deprecated Wi-Fi connection access with NetworkCapabilities and LinkProperties on current Android versions.
- Updated SSID handling for Android 13+ while retaining a compatibility path for older supported devices.
- Replaced the deprecated credential-confirmation intent with BiometricPrompt supporting strong biometrics or device credentials.

## Verification

- Android unit tests, lint, debug assembly, and APK size checks passed.
- Fifteen physical-device instrumentation tests passed, including Android Keystore encryption, migration, replacement, and localized navigation.

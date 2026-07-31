# Password Vault internal security review

Date: 2026-07-31

Scope: Android encrypted local storage, PWA Web Crypto storage, clipboard/preview behavior, backup exposure, malformed data handling, and user-facing claims.

This is an internal engineering review, not an independent audit.

## Threat model

Protected against:

- Casual inspection of Android app preferences or browser local storage.
- Offline theft of PWA ciphertext without the master passphrase.
- Accidental screenshots of the Android vault screen.
- Indefinite Android clipboard retention after a PureHub copy action.
- Malformed local vault JSON crashing or injecting invalid records into the Android vault.

Not protected against:

- Malware, rooted devices, hostile accessibility services, browser extensions, or code executing in the PureHub origin.
- An attacker using an already unlocked app/device session.
- Weak or reused master passphrases.
- Destructive loss of browser storage, Android app data, or the signing/Keystore environment.
- Traffic or supply-chain compromise during the first browser OCR/language-resource download; OCR data is separate from Vault data.

## Controls implemented

### PWA

- AES-256-GCM authenticated encryption.
- Unique 128-bit salt and 96-bit IV for every new entry.
- Versioned KDF metadata; PBKDF2-HMAC-SHA-256 increased from 310,000 to 600,000 iterations for new entries while preserving legacy decryption.
- Minimum 12-character passphrase for new entries, bounded fields and entry count, validated backup import, five-minute passphrase auto-lock, and 30-second reveal timeout.
- Encrypted JSON backup export. Labels are explicitly identified as plaintext metadata.
- Vault and OCR are separate lazy chunks so security-sensitive code and OCR language packs are not loaded on unrelated routes.

### Android

- AES-256-GCM master key and encrypted preferences backed by Android Keystore.
- Android backup disabled so ciphertext is not restored without its device-bound key.
- `FLAG_SECURE` blocks screenshots and non-secure display capture while Vault is visible.
- Clipboard data marked sensitive; the prior clear job is cancelled before scheduling a new 30-second clear.
- Synchronous encrypted-store commit, bounded data, defensive JSON parsing, and fail-closed malformed-record handling.

## Remaining gate

Before removing the experimental warning, commission a third-party review that includes dependency/supply-chain review, Android device compromise scenarios, PWA XSS/CSP analysis, cryptographic format compatibility tests, backup recovery tests, accessibility-service exposure, and destructive migration testing.

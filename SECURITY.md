# PureHub Security Policy

## Reporting a vulnerability

Please do not publish exploitable details in a public issue. Use GitHub's private vulnerability reporting for this repository when available, or contact the maintainer through the private channel listed on the GitHub profile. Include the affected version, reproduction steps, impact, and any suggested mitigation.

PureHub does not offer a bug bounty and cannot promise a fixed response time, but credible reports will be acknowledged and prioritized.

## Supported versions

PureHub is currently beta software. Security fixes target the newest GitHub prerelease and the production PWA. Older APKs and cached PWA builds should be upgraded before reporting a problem that is already fixed on `main`.

## Password Vault status

Password Vault is experimental and has not completed an independent third-party security assessment. Do not use it as the only copy of critical credentials.

- Android uses the platform Keystore through encrypted preferences, disables cloud backup, blocks screenshots while the vault is visible, and marks copied passwords as sensitive clipboard data.
- The PWA encrypts secret values with AES-256-GCM and a per-entry random salt/IV. New entries derive keys with PBKDF2-HMAC-SHA-256 and 600,000 iterations, auto-lock after five minutes, and support encrypted backup export/import.
- Entry labels remain visible in local storage. A malicious browser extension, compromised device, injected script, unlocked Android session, forgotten passphrase, or deleted browser storage remains outside the vault's protection boundary.

See `docs/PASSWORD_VAULT_SECURITY_REVIEW.md` for the internal review and remaining external-audit gate.

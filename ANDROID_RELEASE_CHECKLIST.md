# Android Release Checklist

## One-time owner tasks

1. Create and securely back up the PureHub app-signing keystore.
2. Copy `signing.properties.example` to `signing.properties` for local release builds.
3. Create the GitHub environment `android-release` with required reviewers.
4. Add GitHub secrets:
   - `PUREHUB_KEYSTORE_BASE64`
   - `PUREHUB_KEYSTORE_PASSWORD`
   - `PUREHUB_KEY_ALIAS`
   - `PUREHUB_KEY_PASSWORD`
   - `PUREHUB_RELEASE_HOOK_SECRET` (the same random value as Vercel `RELEASE_WEBHOOK_SECRET`)
5. Register `com.purehub.app` and its signing certificate in Android Developer Console/Play Console.

Create the base64 secret in PowerShell without printing the keystore contents:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\secure\purehub-release.jks")) |
  Set-Clipboard
```

Paste the clipboard value into `PUREHUB_KEYSTORE_BASE64`, then clear the clipboard.

Never publish a debug APK or rotate the signing key casually. The same app-signing certificate must remain available for future updates and Google Play migration.

## Every release

1. Update and test release notes.
2. Run unit tests, lint, and release builds for both `standard` and `fdroid`; build the AAB from `standard`.
3. Confirm `fdroidReleaseRuntimeClasspath` contains no ML Kit, Play Services, or Firebase dependencies.
4. Verify APK signatures and SHA-256 checksums, including the F-Droid artifact.
5. Install a release APK on at least one physical Android device and run the offline QR/OCR instrumented tests.
6. Publish a GitHub prerelease first, then promote it after community validation.
7. Add the final asset URLs and checksum to Release Hub, generate channel drafts, review them, and approve only the English API posts you intend to publish.

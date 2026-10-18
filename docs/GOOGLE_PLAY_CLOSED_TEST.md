# Google Play closed-test plan

PureHub needs a closed test before a production Google Play launch. The product UI only invites people to a Google Group; it does not claim that a Play build is already available.

## Operating flow

1. Create or confirm the Google Group `purehub-testers` and allow intended testers to join.
2. In Play Console, create a **Closed testing** track and attach that Google Group as the tester list.
3. Upload the signed Android App Bundle (`.aab`), complete Data safety, App content, store listing, privacy-policy, and tester instructions.
4. Copy the exact Play Console opt-in URL. Replace `PLAY_TESTER_GROUP_URL` in Android and `PLAY_TESTER_GROUP_URL` in the PWA only if the Group is not the desired first step; otherwise keep the group URL and post the opt-in URL to members when the track is live.
5. For a personal developer account created after November 13, 2023, keep at least 12 real testers opted in continuously for 14 days before applying for production access. PureHub's internal target remains 50 testers for broader device and language coverage. Do not use fabricated accounts, incentivized installs, or bulk automation.
6. Review Android Vitals, crashes, ANRs, feedback, and consent/privacy disclosures before requesting production access.

## Product behavior

- The invite is shown once on the normal app/PWA home experience.
- **Join** and **Later** both suppress repeat prompts locally.
- A permanent entry in Settings remains available for people who want to join later.
- External group links open outside the PWA/WebView flow, so Google account and Play authentication stay in the user’s normal browser.

## Release gate

Do not point users to a Play opt-in page until the track contains the signed production-candidate bundle and the group/tester policy is configured in Play Console.

## Beta.54 readiness snapshot

- Application ID: `com.purehub.app`
- Version code: `57`
- Target SDK: Android 16 / API 36
- Minimum SDK: API 26
- Signed AAB: `PureHub-1.0.0-beta.57.aab`
- Store listing copy: English, Vietnamese, Simplified Chinese, and Spanish
- Current screenshots: Home, complete tool catalog, and Bubble Level
- Privacy policy: `https://hub.blissbiovn.com/en/privacy`, linked from Android Settings
- Ads declaration: no ads in the Android app
- Account requirement: none
- F-Droid distribution: discontinued

Before rollout, complete the Play Console Data safety, App content, content rating, target audience, ads, and foreground-service declarations. Keep the privacy answers aligned with the exact Standard bundle and its connected Wi-Fi diagnostics; do not reuse the retired offline F-Droid description.

## Beta 57 release gate

- Install the signed APK as an update over beta.55 or beta.56 and confirm local preferences, favorites, reminders, Wi-Fi history, and Room data remain available.
- Smoke-test Home, Tools, Community, Settings, Wi-Fi Analyzer, Lunar Calendar, Bubble Level, Smart Flashlight, OCR, and QR in English, Vietnamese, Simplified Chinese, and Spanish.
- Repeat the core flow with camera, microphone, nearby Wi-Fi, location, notification, and screen-recording permissions denied; every denial must keep navigation usable and explain the recovery action.
- Test system font scales at 100%, 125%, and 150%, TalkBack focus order, descriptive controls, 48dp touch targets, and light/dark contrast.
- Use only beta.57 screenshots in the Play listing. Capture Home, Tools, Wi-Fi Analyzer, Lunar Calendar, Smart Flashlight, and the permission explanation on a physical phone.
- Confirm Settings shows `1.0.0-beta.57 (57)` and that Report a problem opens a prefilled GitHub issue containing only version, model, Android API, and selected language.
- Review Android vitals, tester reports, permission failures, and the three most common failed workflows after days 1, 3, 7, and 14. Do not promote to production while a reproducible crash, ANR, data-loss migration, or blocked primary flow remains open.

# Google Play closed-test plan

PureHub needs a closed test before a production Google Play launch. The product UI only invites people to a Google Group; it does not claim that a Play build is already available.

## Operating flow

1. Create or confirm the Google Group `purehub-testers` and allow intended testers to join.
2. In Play Console, create a **Closed testing** track and attach that Google Group as the tester list.
3. Upload the signed Android App Bundle (`.aab`), complete Data safety, App content, store listing, privacy-policy, and tester instructions.
4. Copy the exact Play Console opt-in URL. Replace `PLAY_TESTER_GROUP_URL` in Android and `PLAY_TESTER_GROUP_URL` in the PWA only if the Group is not the desired first step; otherwise keep the group URL and post the opt-in URL to members when the track is live.
5. Ask at least 50 real testers to install, use core tools, and leave opt-in active for the required testing period. Do not use fabricated accounts, incentivized installs, or bulk automation.
6. Review Android Vitals, crashes, ANRs, feedback, and consent/privacy disclosures before requesting production access.

## Product behavior

- The invite is shown once on the normal app/PWA home experience.
- **Join** and **Later** both suppress repeat prompts locally.
- A permanent entry in Settings remains available for people who want to join later.
- External group links open outside the PWA/WebView flow, so Google account and Play authentication stay in the user’s normal browser.

## Release gate

Do not point users to a Play opt-in page until the track contains the signed production-candidate bundle and the group/tester policy is configured in Play Console.

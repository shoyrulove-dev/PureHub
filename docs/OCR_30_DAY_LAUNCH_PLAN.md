# PureHub private OCR: 30-day launch plan

Campaign window: 6 September–5 October 2026. The single acquisition promise is: scan printed documents into editable text or a searchable PDF on the phone, without an account or document-image upload.

## Product and measurement baseline

- Android beta.44: camera/gallery input, batches up to 20 pages, edge detection, adjustable four-corner correction, rotation and cleanup filters, compact retained previews, and orphan-document cleanup.
- Browser OCR: camera/gallery input, batches, rotation and cleanup filters, editable text, and current-page PDF handoff. Its saved library retains searchable text, not complete page images.
- OCR languages: English, Vietnamese, and Simplified Chinese. Market handwritten notes only as an experiment; printed-text OCR is the supported promise.
- Output: editable text, TXT, searchable PDF, receipt handoff, and a private on-device library that can reopen complete pages.
- Primary funnel: campaign visit → OCR page open → useful completion → Android download click → actionable tester report.
- Privacy: do not collect document images or recognized text. Campaign links use aggregate source/campaign attribution only.

## Daily execution

| Days | Product/proof | Distribution | Decision target |
|---|---|---|---|
| 1–3 | Publish beta.44 candidate and record one real Vietnamese receipt demo plus one printed-note demo. | Launch the OCR landing page and short-form proof on Bluesky, Mastodon, Telegram, and YouTube. | No crash or blocked export on the physical-phone smoke test. |
| 4–7 | Collect device, Android version, language, input type, result quality, and export outcome from testers. | One outcome-led post daily; invite 10 named beta testers directly. | At least 5 completed OCR runs and 3 actionable reports. |
| 8–14 | Fix only repeatable edge, rotation, OCR, persistence, or export failures. | Publish one honest comparison and one DEV technical note; Reddit stays manual and community-specific. | 15 useful completions, 8 Android clicks, 5 reports. |
| 15–21 | Ship the highest-impact reliability patch if evidence warrants it. | Rotate receipt, study-note, searchable-PDF, and privacy use cases; reuse proof, not identical copy. | At least 20% visit-to-OCR-start and 30% start-to-completion. |
| 22–27 | Test deletion, library reopen, 20-page batch limits, and low-memory behavior. | Ask active testers for one screenshot-free quote; never invent testimonials. | 30 useful completions, 15 Android clicks, 8 reports. |
| 28–30 | Publish the evidence report and rank the next three fixes. | Final demo, manual Reddit review, DEV recap, and community roadmap vote. | Continue OCR only if completion and feedback beat the stop rules below. |

## Stop and scale rules

- Scale the campaign when at least 30 useful OCR completions, 15 Android download clicks, and 8 actionable tester reports are recorded by day 30.
- Fix onboarding before adding features if fewer than 20% of qualified OCR visitors start the workflow.
- Fix scan quality or export before promotion if fewer than 30% of starts reach a useful completion.
- If fewer than 10 useful completions arrive after 150 qualified visits, pause daily promotion and interview five target users before expanding the OCR feature set.
- Do not add QR as a second flagship during this window. It can remain a supporting utility while one clear OCR promise accumulates proof.

## Command Center operating rules

- Campaign ID: `private-ocr-30d-v1`; browser-capable topics resolve to `/en/ocr-text`, while explicitly labelled `Android beta:` topics resolve to `/en/download`. Both retain the OCR campaign attribution.
- Bluesky and Mastodon run daily; Telegram runs three times weekly; DEV runs weekly; YouTube uses the existing collision-safe queue.
- Reddit posts remain `ready_manual`. Community replies remain approval-first.
- Keep auto-publishing paused for the first three campaign days. Approve proof-based drafts, then enable it only after copy and attribution are confirmed.
- Check daily: failed posts, duplicate YouTube slots, OCR funnel starts/completions, Android clicks, and open tester feedback.

## Next app and Command Center updates

The next app patch should be driven by device reports. Low-memory page retention and orphan cleanup are now included in beta.44. Remaining candidates, in order, are page reordering, share-to-PureHub image intake, and optional offline OCR-pack management. Command Center should next add an OCR-only funnel card by campaign/source, tester-report quality labels, and a day-7/day-14/day-30 stop-rule alert. Avoid broader social automation until the OCR funnel shows a repeatable useful result.

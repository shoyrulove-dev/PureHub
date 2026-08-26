# PureHub Community Growth Operations

## Positioning

PureHub is a community-built collection of 26 free, ad-free, privacy-first tools for Android and the web.

Primary call to action: try one tool, report one issue or idea, and vote on what should improve next.

Current flagship tools: Zen Habit, Zen Pomodoro, Zen Breath, QR Studio, and OCR Studio. Community content should lead with one of these tools often enough to build recognition, while the remaining catalog continues to receive supporting demos and SEO coverage.

## Privacy-first journey funnel

Admin tracks aggregate daily journey signals for visits, tool opens, downloads, first browser opens, Early Tester joins, and device reports. Source and campaign values are restricted to an allowlist; unknown values become `other` or `none`. No IP address, account, user ID, device ID, free-form referrer, or feedback content is stored in funnel records.

The public Early Testers form sends its voluntary report text to Support Inbox, where it follows the existing human-review workflow. The initial target is 20 useful device reports across the flagship set. Never ask testers to submit names, email addresses, phone numbers, serial numbers, or account details.

## Daily automation schedule

All Vercel cron expressions use UTC. The times below use Asia/Bangkok (UTC+7).

| Local time | Automated job | Result |
| --- | --- | --- |
| Real time | Telegram webhook | Ingest questions and create an AI-assisted support draft. |
| 07:00 | Support monitor | Scan DEV, Bluesky, and Mastodon; refresh engagement; create reply drafts. |
| 19:00 | Growth Autopilot | Generate channel-specific English content and publish channels scheduled for that campaign day. |
| 21:00 | Metrics monitor | Refresh post metrics and YouTube metrics without delaying the publishing job. |

Automatic publishing cadence in each repeating 30-day campaign:

- Social opportunity discovery: five dedicated scans per Bangkok day (00:15, 05:15, 09:15, 14:15, and 19:15). Query windows rotate to stay within serverless limits; two consecutive empty scans enable a broader recovery query set without bypassing relevance or safety filters.
- Telegram: three times per seven-day cycle.
- DEV: four useful technical articles per 30 days.
- YouTube: ten real-device miniapp Shorts are scheduled natively on YouTube at 19:30 Asia/Bangkok every two days from 2–20 August 2026.
- Reddit: two carefully prepared drafts per 30 days; review the destination, then publish from Command Center.

## Daily owner routine (10-15 minutes)

1. Open Admin and read the August Growth Overview: outcome score, lagging milestone, and next actions.
2. Edit and approve replies that are accurate and helpful.
3. Reply personally to high-value feedback or contributor questions.
4. On a YouTube day, upload one 15-30 second demo using the prepared title and description.
5. Record repeated requests as roadmap candidates instead of promising dates immediately.

The dashboard tracks five August outcomes from connected APIs: 100 community members, 1,500 qualified views, 30 interactions, 20 useful feedback items, and eight Shorts. The schedule and low-frequency controls stay collapsed; selecting a next-action chip opens the relevant section.

Product Signals uses anonymous daily aggregate counters only. It tracks which miniapps are opened, marked helpful, shared, or accompanied by feedback, without a user ID, device ID, content payload, cookie, or stored IP. Journey events also respect Do Not Track and the anonymous-metrics setting. Community roadmap voting is intentionally lightweight and stores the one-vote marker only in the visitor's own browser.

## Weekly community loop

1. Monday: select one mini-app and one user problem for the week's story.
2. Tuesday: post a practical tip or before/after demonstration.
3. Wednesday: ask one specific question or run a roadmap poll.
4. Thursday: publish a short build log with a real engineering lesson.
5. Friday: share one mini-app demo and invite device testers.
6. Saturday: thank testers, answer open questions, and highlight a contribution.
7. Sunday: publish a transparent weekly recap with shipped, learned, and next items.

## 30/60/90-day targets

### First 30 days: trust

- Maintain the schedule without spam or duplicate posts.
- Reach 100 combined followers or community members.
- Collect at least 20 actionable feedback items and five Android device reports.
- Publish four DEV articles and at least eight useful short demos.

### Days 31-60: participation

- Run two roadmap votes and ship the winning improvement from at least one vote.
- Start a contributor-friendly issue list and recognize every accepted contribution.
- Turn the strongest support questions into searchable FAQ or demo content.

### Days 61-90: repeatable growth

- Establish a monthly public roadmap and transparency report.
- Recruit five recurring testers or contributors.
- Double down on the two platforms that produce meaningful replies, installs, or GitHub activity rather than raw impressions.

## Automation boundaries

- AI creates different channel-specific posts; Telegram, DEV, Bluesky, and Mastodon can publish automatically.
- Community replies remain draft-and-approve to prevent incorrect, unsafe, or off-brand responses.
- Reddit requires a human review and explicit Publish click because community rules vary by subreddit; OAuth handles the actual posting and records the permalink.
- YouTube rendering and upload run locally from the connected Android device; publishing is unattended because YouTube stores the private `publishAt` schedule.
- When future YouTube Shorts are already scheduled, Growth Autopilot suppresses extra script-only YouTube packages. Admin shows the next automatic publish time instead of asking for a duplicate manual upload.

## Hosting decision

Keep the web app and current daily automation on Vercel. Add a small VPS worker only when one of these becomes necessary:

- monitoring every 1-5 minutes instead of daily;
- durable retries and a persistent background queue;
- unattended video rendering and YouTube upload;
- an always-on Telegram worker or WebSocket service;
- jobs that regularly exceed the Vercel Function execution window.

The recommended future architecture is hybrid: keep the PWA and HTTP admin on Vercel, while moving only long-running workers and media processing to a VPS.

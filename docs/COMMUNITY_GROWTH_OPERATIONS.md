# PureHub Community Growth Operations

## Positioning

PureHub is a community-built collection of 22 free, ad-free, privacy-first tools for Android and the web.

Primary call to action: try one tool, report one issue or idea, and vote on what should improve next.

## Daily automation schedule

All Vercel cron expressions use UTC. The times below use Asia/Bangkok (UTC+7).

| Local time | Automated job | Result |
| --- | --- | --- |
| Real time | Telegram webhook | Ingest questions and create an AI-assisted support draft. |
| 07:00 | Support monitor | Scan DEV, Bluesky, and Mastodon; refresh engagement; create reply drafts. |
| 19:00 | Growth Autopilot | Generate channel-specific English content and publish channels scheduled for that campaign day. |
| 21:00 | Metrics monitor | Refresh post metrics and YouTube metrics without delaying the publishing job. |

Automatic publishing cadence in each repeating 30-day campaign:

- Bluesky and Mastodon: daily.
- Telegram: three times per seven-day cycle.
- DEV: four useful technical articles per 30 days.
- YouTube: three prepared upload entries per seven-day cycle; a video file still needs to be selected and uploaded in Admin.
- Reddit: two carefully prepared drafts per 30 days; manual review and posting remain required.

## Daily owner routine (10-15 minutes)

1. Open Admin and check failed jobs and new support drafts.
2. Edit and approve replies that are accurate and helpful.
3. Reply personally to high-value feedback or contributor questions.
4. On a YouTube day, upload one 15-30 second demo using the prepared title and description.
5. Record repeated requests as roadmap candidates instead of promising dates immediately.

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
- Reddit remains manual because community rules and promotion limits vary by subreddit.
- YouTube account data and metrics are connected, but video generation/storage/upload is not yet a reliable unattended serverless workflow.

## Hosting decision

Keep the web app and current daily automation on Vercel. Add a small VPS worker only when one of these becomes necessary:

- monitoring every 1-5 minutes instead of daily;
- durable retries and a persistent background queue;
- unattended video rendering and YouTube upload;
- an always-on Telegram worker or WebSocket service;
- jobs that regularly exceed the Vercel Function execution window.

The recommended future architecture is hybrid: keep the PWA and HTTP admin on Vercel, while moving only long-running workers and media processing to a VPS.

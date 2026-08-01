# PureHub Growth Autopilot

Growth Autopilot runs a 30-day, build-in-public community campaign without requiring the owner to remember platform credentials or a daily posting calendar.

## Automatic channels

- Bluesky and Mastodon: one original post per campaign day.
- Telegram: three posts per week. Only the channel is targeted; its linked discussion group receives the channel post naturally.
- DEV Community: one educational article per week, published rather than drafted when `devto_publish_as_draft=false`.
- Community support: mentions and replies are synchronized after every campaign run. AI replies remain drafts for approval by default.
- Engagement: aggregate platform metrics and per-post metrics are refreshed by the scheduler.

The unique `(campaign_id, day_number, channel)` database key makes every run idempotent. A retry cannot create a duplicate record for the same campaign day and channel.

## Human-review channels

- Reddit content is generated only on campaign days 14 and 28 and is never posted automatically.
- YouTube packages are generated three times per week. They include a title, link-free Shorts description, and shot script.

## YouTube connection

Do not share a Google password with an operator or store it in PureHub.

1. Create a Google Cloud OAuth web client and enable YouTube Data API v3.
2. Add this exact authorized redirect URI:
   `https://hub.blissbiovn.com/admin/youtube/callback`
3. In Admin > Advanced > Growth Autopilot, save the OAuth client ID and client secret.
4. Click **Connect YouTube securely** and approve the requested upload/read-only scopes as the primary channel owner.
5. In Growth Autopilot, choose a prepared MP4 for a YouTube queue item. The browser uploads it directly to Google's resumable upload endpoint.

Uploads default to **Unlisted**. Change that only after validating the workflow and the finished Short on a phone.

## Safety boundaries

- Platform passwords are never accepted.
- Public replies stay human-approved unless the owner explicitly changes reply mode.
- Reddit remains manual because community rules and account context cannot be safely automated.
- Generated copy is constrained to verified product facts and must not invent metrics, users, testimonials, audits, or shipped features.
- Failed platform calls are retained with an error and can be retried from Admin.
- The campaign publishes at most one item per platform and campaign day.

## Scheduler

Vercel calls `/public-api/growth-automation` at `12:00 UTC` (`19:00 Asia/Bangkok`) with `CRON_SECRET`. The existing support sync also continues at `00:00 UTC`.

# Admin Setup

## Local environment

1. Copy `.env.example` to `.env`.
2. Fill in:
   - `MONGO_URI`
   - `MONGO_DB_NAME`
   - `ADMIN_USERNAME`
   - `ADMIN_PASSWORD`
   - `SESSION_SECRET`
3. Install dependencies:

```bash
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
```

4. Start the server:

```bash
python -m uvicorn main:app --reload --port 8000
```

5. Open:

```text
http://127.0.0.1:8000/admin
```

## Security model

- Admin passwords are hashed before being stored in MongoDB.
- Session cookies are signed with `SESSION_SECRET`.
- Runtime credentials for Grok, Dev.to, and Telegram are editable from `/admin`.
- The dashboard also exposes mini-app catalog and API catalog controls so route, priority, and availability changes can be managed without code edits.

## Main admin areas

- `Credential vault`
  Grok, Dev.to, Telegram, Pro code, and site URL config.
- `Manual triggers`
  Start generator, publisher, and Telegram worker from the browser.
- `Admin security`
  Rotate admin username and password.
- `Mini-app catalog`
  Manage routes, enabled state, notes, and traffic priority for each tool.
- `API catalog`
  Manage exposed admin endpoints and auth metadata.
- `Referral loop`
  Monitor invite counts and reward state.
- `Schema & audit`
  Review Mongo migration state and admin activity history.

## JSON endpoints

- `/admin/api/health`
- `/admin/api/config`
- `/admin/api/stats`
- `/admin/api/articles`
- `/admin/api/referrers`
- `/admin/api/miniapps`
- `/admin/api/catalog`

## Vercel deployment notes

- `vercel.json` rewrites `/admin` and `/admin/*` to the Python function at `api/index.py`.
- The PWA still serves from `pwa/dist`; `/admin` is reserved for FastAPI.
- Set these environment variables in the Vercel project:
  - `MONGO_URI`
  - `MONGO_DB_NAME`
  - `ADMIN_USERNAME`
  - `ADMIN_PASSWORD`
  - `SESSION_SECRET`
- The first request to the backend will seed:
  - `config`
  - `admins`
  - `miniapps`
  - `api_catalog`
  - `article_jobs`
  - `users`

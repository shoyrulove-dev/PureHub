# Vercel Deployment Checklist

## Goal

Serve the PureHub PWA at the root domain and the FastAPI Command Center at:

```text
https://hub.blissbiovn.com/admin
```

## Current repo behavior

- `pwa/` builds the static PWA
- `api/index.py` exposes the FastAPI app for Vercel Python runtime
- `vercel.json` rewrites:
  - `/admin`
  - `/admin/*`
  to the Python backend

## Vercel project settings

- Framework Preset: `Other`
- Root Directory: repository root
- Build Command:
  handled by `vercel.json`
- Output Directory:
  handled by `vercel.json`

## Required environment variables

Add these in Vercel Project Settings -> Environment Variables:

- `MONGO_URI`
- `MONGO_DB_NAME`
- `ADMIN_USERNAME`
- `ADMIN_PASSWORD`
- `SESSION_SECRET`

## Recommended values

- `MONGO_DB_NAME`
  `purehub_command_center`
- `SESSION_SECRET`
  a long random string, at least 32 characters
- `ADMIN_PASSWORD`
  a strong unique password dedicated to the admin panel

## First deploy expectation

On the first backend request, Mongo will auto-seed:

- `config`
- `admins`
- `miniapps`
- `api_catalog`
- `article_jobs`
- `users`
- `audit_logs`
- `schema_migrations`

## Quick verification after deploy

1. Open `/admin/login`
2. Sign in with the admin credentials from Vercel env
3. Open `/admin/api/health`
4. Open `/admin/api/schema`
5. Save one config value and confirm it appears in audit logs

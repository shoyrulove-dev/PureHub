# 🔧 CRON Secret Sync Fix - Both Vercel Projects

## 📊 Problem Identified

**Status Check Result:**
- ✅ purehub (PWA) - CRON_SECRET exists (Updated Aug 1)
- ✅ purehub-command-center - CRON_SECRET exists (Updated 2d ago)
- ❌ **But values are MISMATCHED** → causing "Invalid cron authorization"

The cron endpoint keeps rejecting requests because the two Vercel projects have different secret values.

---

## ✅ Solution: Use One Master Secret in Both Projects

### Master CRON_SECRET to Use Everywhere:
```
cron_d14898c9be2e4a7dba47c555ba32cbc8c2ece3d4e2f54b7ebe0207c0879e3dec
```

**COPY THIS VALUE** - You'll need it for both projects below.

---

## 📋 Step-by-Step Fix

### STEP 1: Update `purehub` (PWA/Homepage Project)

1. Open: **Vercel Dashboard**
2. Navigate to: **shoyrulove-devs-projects → purehub → Settings**
3. Click tab: **Environment Variables**
4. Find: **CRON_SECRET** (marked as Sensitive, Production environment)
5. Click: The **"..."** (three dots) menu on the right
6. Select: **Edit**
7. **Clear** the old value completely
8. **Paste** the new master secret:
   ```
   cron_d14898c9be2e4a7dba47c555ba32cbc8c2ece3d4e2f54b7ebe0207c0879e3dec
   ```
9. Verify: Environment is set to **Production**
10. Click: **Save**
11. **Wait** 2-5 minutes for Vercel to redeploy

---

### STEP 2: Update `purehub-command-center` (Backend/API Project)

1. Open: **Vercel Dashboard**
2. Navigate to: **shoyrulove-devs-projects → purehub-command-center → Settings**
3. Click tab: **Environment Variables**
4. Find: **CRON_SECRET** (marked as Sensitive, Production environment)
5. Click: The **"..."** (three dots) menu on the right
6. Select: **Edit**
7. **Clear** the old value completely
8. **Paste** the SAME master secret as Step 1:
   ```
   cron_d14898c9be2e4a7dba47c555ba32cbc8c2ece3d4e2f54b7ebe0207c0879e3dec
   ```
9. Verify: Environment is set to **Production**
10. Click: **Save**
11. **Wait** 2-5 minutes for Vercel to redeploy

---

### STEP 3: Verify the Fix Works

After **both projects finish redeploying**, test in PowerShell:

```powershell
$secret = "cron_d14898c9be2e4a7dba47c555ba32cbc8c2ece3d4e2f54b7ebe0207c0879e3dec"

$response = Invoke-WebRequest `
  -Uri "https://hub.blissbiovn.com/public-api/growth-automation" `
  -Method POST `
  -Headers @{"Authorization" = "Bearer $secret"} `
  -TimeoutSec 30

Write-Host "Status Code: $($response.StatusCode)"
Write-Host "Response: $($response.Content)"
```

**Expected Result:**
- Status Code: `200`, `202`, or `204` ✅
- No more "Invalid cron authorization" error ✅

---

## ✅ What Happens After Fix

### Automatic Cron Execution
- Vercel automatically triggers `/public-api/growth-automation` at **12:00 UTC** (19:00 Bangkok time)
- Sends proper authorization header with the CRON_SECRET
- Endpoint validates and processes the cron job

### Expected Outcomes (24-48 hours)
- ✅ New community replies appear in admin dashboard
- ✅ Support sync captures recent messages from DEV/Bluesky/Mastodon
- ✅ Cron logs show successful executions in Vercel
- ✅ System generates new content automatically

---

## 🔍 Verification Checklist

After both projects are updated and redeployed, verify:

- [ ] Vercel shows both projects as "Ready"
- [ ] Deployment history shows recent builds
- [ ] Manual test (PowerShell command above) returns 200
- [ ] Admin dashboard at https://hub.blissbiovn.com/admin logs in successfully
- [ ] Check back in 24-48 hours for new replies/content

---

## 📝 Reference Files

- **Vercel Rewrite Config**: [vercel.json](vercel.json)
- **Cron Schedule Docs**: [docs/GROWTH_AUTOPILOT.md](docs/GROWTH_AUTOPILOT.md)
- **Community Operations**: [docs/COMMUNITY_GROWTH_OPERATIONS.md](docs/COMMUNITY_GROWTH_OPERATIONS.md)

---

## ❓ Troubleshooting

| Issue | Solution |
|-------|----------|
| Still getting 401 Unauthorized | Verify secret was copied correctly (no spaces), wait longer for redeploy |
| Vercel shows deployment error | Check build logs, may need to revert and try again |
| 405 Method Not Allowed | Endpoint expects POST, not GET/HEAD |
| 500 Internal Server Error | Cron code has a bug, check application logs |
| Still no new content after 48h | Check if source platforms (DEV/Bluesky/Mastodon) have new content |

---

## Summary

**The Fix:**
- Generate ONE master CRON_SECRET
- Update it in BOTH Vercel projects (purehub + purehub-command-center)
- Both must have the exact same value
- Wait for redeploys
- Test with curl/PowerShell
- Cron will run automatically

**Expected Timeline:**
- Update time: 2-5 minutes per project
- Redeploy time: 2-5 minutes per project
- Total time: ~15-20 minutes
- First cron run: Next scheduled time (12:00 UTC)
- First results visible: Within 24-48 hours

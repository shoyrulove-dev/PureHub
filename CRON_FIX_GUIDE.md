# 🔧 CRON Authorization Fix Guide

## Problem Identified
- **Error**: `{"detail":"Invalid cron authorization."}`
- **Root Cause**: CRON_SECRET in Vercel is invalid or empty
- **Impact**: Growth automation, support sync, and Telegram webhook are NOT running
- **Result**: No new community replies for 24+ hours

## Current Status
✅ Route exists and is deployed  
✅ Route is properly protected with auth validation  
❌ CRON_SECRET value is invalid/empty  
❌ Cron jobs are being rejected

---

## Fix Steps (Manual - Vercel Dashboard)

### Step 1: Generate New CRON_SECRET
In PowerShell, run:
```powershell
$secret = ([guid]::NewGuid().ToString().Replace('-','') + [guid]::NewGuid().ToString().Replace('-','')).Substring(0,64)
Write-Host "Your new CRON_SECRET:"
Write-Host $secret
Write-Host ""
Write-Host "Copy this value to clipboard for next step"
```

**Output example:**
```
Your new CRON_SECRET:
a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f
```

### Step 2: Update Vercel Environment Variable

1. Open **Vercel Dashboard**
2. Navigate to: **shoyrulove-devs-projects → purehub-command-center → Settings**
3. Click **Environment Variables**
4. Find **CRON_SECRET** in the list
5. Click the **"..."** (three dots) menu next to it
6. Select **"Edit"**
7. Clear the current value (it's likely empty or corrupted)
8. Paste the new secret from Step 1
9. Make sure **Environment** is set to **"Production"**
10. Click **"Save"**
11. **Wait** for Vercel to redeploy (2-5 minutes)

### Step 3: Test the Fix

Once Vercel redeploys, test in PowerShell:

```powershell
$secret = "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f"  # Use YOUR secret from Step 1

$response = Invoke-WebRequest `
  -Uri "https://hub.blissbiovn.com/public-api/growth-automation" `
  -Method POST `
  -Headers @{"Authorization" = "Bearer $secret"} `
  -TimeoutSec 30

Write-Host "Status Code: $($response.StatusCode)"
Write-Host "Response: $($response.Content)"
```

**Expected Result:**
- Status: `200` or `202` ✅
- Response: Some JSON data about automation status

**If still getting 401:**
- Double-check you copied the full secret correctly
- Make sure there are no extra spaces
- Verify it's set for "Production" environment
- Wait another minute for Vercel to finish deploying

---

## Verification After Fix

Once cron is working:

1. **Cron Schedule** (automated, runs every 12:00 UTC / 19:00 Bangkok time):
   - `/public-api/growth-automation` - Generates new community posts
   - `/public-api/support-sync` - Syncs support messages from DEV/Bluesky/Mastodon

2. **Expected Results in 24-48 hours**:
   - New replies appear in admin dashboard
   - Support sync shows recent community messages
   - System logs show successful cron executions

3. **Check Dashboard**:
   - Go to: https://hub.blissbiovn.com/admin
   - Username: `admin`
   - Password: `q3_Dpu2fCY34wQ0LsYkvh_rI0PMGNj2B`
   - Look for new items in Support Inbox / Growth log

---

## Troubleshooting

| Symptom | Likely Cause | Solution |
|---------|-------------|----------|
| Still getting 401 after update | Secret value is wrong | Re-copy from Step 1, check for spaces |
| Vercel still deploying | Normal delay | Wait 3-5 minutes, then test again |
| 405 Method Not Allowed | Code issue (not secret) | Check if endpoint code changed |
| 500 Internal Server Error | Code logic error | Check Vercel logs for details |
| Still no new replies 48h later | Source platforms have no new content | Check if DEV/Bluesky/Mastodon have relevant posts |

---

## File Reference
- **Vercel Config**: [vercel.json](vercel.json) - Routes `/public-api/*` to purehub-command-center
- **Documentation**: [docs/GROWTH_AUTOPILOT.md](docs/GROWTH_AUTOPILOT.md) - Describes cron schedule
- **Documentation**: [docs/COMMUNITY_GROWTH_OPERATIONS.md](docs/COMMUNITY_GROWTH_OPERATIONS.md) - Automation details

---

## Questions?
If step 2 doesn't work or you need help with Vercel UI, let me know and I'll assist directly.

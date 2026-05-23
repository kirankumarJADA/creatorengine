# Meta Webhook & OAuth Setup

CreatorEngine triggers off Instagram events (comments, DMs) via Meta's Graph API webhooks, and connects users' Instagram accounts via OAuth. This guide covers the Meta-side setup: app creation, permissions, webhook subscription, and the verify-token handshake.

> **Prerequisite:** Your backend must already be deployed and publicly reachable over HTTPS (Render gives you this automatically). Meta will not subscribe a webhook to a URL it can't reach.

## 1. Create a Meta app

1. Open [developers.facebook.com](https://developers.facebook.com) → **My Apps** → **Create app**.
2. **Use case:** "Other" → **Business**.
3. Pick a name (e.g. `CreatorEngine Production`) and the business account that owns it.

After creation you land on the app dashboard.

**Capture from Settings → Basic:**
- **App ID** → `META_APP_ID`
- **App Secret** (click "Show") → `META_APP_SECRET` — used to verify webhook signatures (`X-Hub-Signature-256`)

> Treat the App Secret like a password. Never commit it; only set it as a Render secret.

## 2. Add Instagram product

1. Left sidebar → **Add products** → find **Instagram** → **Set up**.
2. Choose the "Instagram API with Instagram Login" option (not the legacy Basic Display).
3. Under **Use cases**, enable:
   - `instagram_business_basic`
   - `instagram_business_manage_comments`
   - `instagram_business_manage_messages`

## 3. Configure OAuth redirect URI

1. Left sidebar → **Use cases** → **Instagram** → **Settings**.
2. Scroll to **Business login settings**.
3. **Redirect URI:** add your backend callback exactly. No trailing slash:
   ```
   https://your-backend.onrender.com/api/instagram/callback
   ```
4. Save.

Set the same value in your backend env as `META_REDIRECT_URI`. The match must be character-exact — Meta will reject any deviation in scheme, host, port, or path.

Set `META_SUCCESS_REDIRECT_URI` to where the backend bounces the browser after a successful OAuth:
```
https://yourapp.vercel.app/instagram/callback
```

## 4. Webhook subscription

This is what makes the comment→DM automation fire in real time.

1. Left sidebar → **Use cases** → **Instagram** → **Webhooks** (or **Webhooks** standalone in older dashboards).
2. **Callback URL:**
   ```
   https://your-backend.onrender.com/api/webhooks/instagram
   ```
3. **Verify Token:** paste the same value you set as `META_VERIFY_TOKEN` on Render.
   - Generate one: `openssl rand -hex 32`
   - Must be ≥16 characters; the validator rejects shorter values.
4. Click **Verify and save**. Meta sends a GET request with `hub.challenge`; the backend echoes it back if `META_VERIFY_TOKEN` matches. You should see a "Successfully verified" message.
5. **Subscribe** the fields you need:
   - `comments` — fires when someone comments on a post
   - `messages` (if you want DM-based triggers)
   - `mentions` (optional — for story mentions)

### Test the handshake manually
```bash
curl "https://your-backend.onrender.com/api/webhooks/instagram?hub.mode=subscribe&hub.verify_token=YOUR_VERIFY_TOKEN&hub.challenge=test123"
# Expected output: test123
```
If you get `403 Forbidden`, the token doesn't match. If you get `404`, the path is wrong.

### Test signature verification
Meta signs every webhook body with HMAC-SHA256 using your App Secret, in the `X-Hub-Signature-256` header. The backend verifies this on every incoming request — set `META_APP_SECRET` correctly or every webhook will 401.

## 5. Add a test Instagram account

In development (before App Review):
1. **App Roles** → **Testers** → add Instagram users by handle.
2. They get an in-app notification to accept the tester invite.
3. Only tester accounts can OAuth-connect until your app is approved.

The Instagram account being tested must be a **Business** or **Creator** account linked to a Facebook Page. Personal accounts can't use the messaging APIs.

## 6. App Review (going public)

Once you want any Instagram user to be able to connect:

1. **App Review** → **Permissions and Features**.
2. Request each permission used:
   - `instagram_business_basic`
   - `instagram_business_manage_comments`
   - `instagram_business_manage_messages`
3. For each, Meta requires:
   - A short description of how you use it
   - A screencast of the flow (record yourself triggering an automation and seeing the DM go out)
   - Test credentials so Meta reviewers can repro
4. Submit and wait. Review typically takes 3–10 business days.

Until approved, your app stays in **Development mode** and only tester accounts can connect.

## 7. Token lifecycle

CreatorEngine stores the long-lived Page Access Token returned by the OAuth flow. These tokens last ~60 days. Meta will eventually require refreshing them — there's a placeholder hook in `InstagramApiClient.refreshLongLivedTokenPlaceholder()` and `@EnableScheduling` is already on the main application class for when you wire a scheduled refresh job.

For now, users whose tokens expire will see their Instagram account disconnected and need to reconnect via the Settings page.

## Production checklist

- [ ] `META_APP_ID` and `META_APP_SECRET` set as Render secrets
- [ ] `META_VERIFY_TOKEN` set to the **same** value on both Render AND in the Meta webhook dashboard
- [ ] `META_REDIRECT_URI` is HTTPS and matches Meta's OAuth redirect URI exactly
- [ ] `META_SUCCESS_REDIRECT_URI` is HTTPS and points at your Vercel frontend
- [ ] Webhook subscription shows "Subscribed" with a green dot in the Meta dashboard
- [ ] Manual handshake test returns the challenge string
- [ ] You've tested OAuth → real comment → real DM end-to-end with a tester account
- [ ] App Review submitted (or you're staying in dev mode with tester accounts only)

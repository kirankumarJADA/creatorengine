# CreatorEngine — Deployment Guide

Production deployment uses **Render** (backend, Spring Boot in Docker) and **Vercel** (frontend, Vite + React). This guide takes you from a fresh repo to a working production deploy.

## Prerequisites

- A GitHub repository containing this code
- Accounts: [Render](https://render.com), [Vercel](https://vercel.com), [Firebase](https://console.firebase.google.com), [Meta for Developers](https://developers.facebook.com)
- Local tools (optional, for verification): `mvn`, `node ≥18`, `docker`

Two companion guides cover the third-party setup:
- [FIREBASE_SETUP.md](./FIREBASE_SETUP.md) — service account, Firestore, password reset
- [META_WEBHOOK_SETUP.md](./META_WEBHOOK_SETUP.md) — app config, verify token, OAuth, webhooks

Complete those first; you'll need the resulting credentials below.

---

## 1. Backend on Render

The repo includes [`render.yaml`](./render.yaml), a Render Blueprint, so the service can be provisioned in one click.

### Option A — Blueprint (recommended)
1. Push this repo to your default branch on GitHub.
2. In Render → **New** → **Blueprint** → connect your repo.
3. Render reads `render.yaml` and creates a `creatorengine-backend` Docker service.
4. Render prompts you to fill in every env var marked `sync: false`. Set them now (see [env reference](#backend-env-reference) below).
5. Click **Apply**. Render builds the image from `backend/Dockerfile`, runs it, and exposes a public URL like `https://creatorengine-backend.onrender.com`.

### Option B — Manual web service
1. In Render → **New** → **Web Service** → connect repo.
2. **Runtime:** Docker. **Root Directory:** `backend`. **Dockerfile path:** `./Dockerfile`.
3. **Health check path:** `/api/health`.
4. Set environment variables (see below) — at minimum `SPRING_PROFILES_ACTIVE=prod`, the JWT secret, Firebase config, Meta config, and CORS origins.

### Backend env reference

| Variable | Required in prod | Example | Notes |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | yes | `prod` | Enables `EnvironmentValidator`, masks error details |
| `JWT_SECRET` | yes | (64-byte base64) | Generate: `openssl rand -base64 64` |
| `JWT_ISSUER` | no | `creatorengine` | Default fine |
| `FIREBASE_PROJECT_ID` | yes | `my-app-12345` | Firebase console → Project settings |
| `FIREBASE_WEB_API_KEY` | yes | `AIzaSy…` | Firebase console → Project settings → General |
| `FIREBASE_CREDENTIALS_JSON` | yes | `{"type":"service_account",…}` | Paste the entire service-account JSON as one line |
| `FIREBASE_PASSWORD_RESET_REDIRECT_URL` | yes | `https://yourapp.vercel.app/login` | HTTPS only |
| `META_APP_ID` | yes | `1234567890` | Meta → app → Settings → Basic |
| `META_APP_SECRET` | yes | `abc…` | Same place. Used for webhook signing |
| `META_VERIFY_TOKEN` | yes | (high-entropy string) | Generate: `openssl rand -hex 32`. Must match the value in Meta's webhook settings |
| `META_REDIRECT_URI` | yes | `https://creatorengine-backend.onrender.com/api/instagram/callback` | HTTPS. Must match Meta exactly |
| `META_SUCCESS_REDIRECT_URI` | yes | `https://yourapp.vercel.app/instagram/callback` | HTTPS |
| `META_GRAPH_API_VERSION` | no | `v19.0` | Bump as Meta releases new versions |
| `CORS_ALLOWED_ORIGINS` | yes | `https://yourapp.vercel.app` | Comma-separated. No localhost in prod |
| `FRONTEND_BASE_URL` | yes | `https://yourapp.vercel.app` | HTTPS, no localhost |

> **The validator runs first.** If anything required is missing or holds a placeholder default, the service fails to start with a detailed list of what's wrong in the Render logs.

### Verify the backend
```bash
curl https://creatorengine-backend.onrender.com/api/health
# → {"status":"UP","components":{…}}
```

---

## 2. Frontend on Vercel

The repo includes [`frontend/vercel.json`](./frontend/vercel.json) with SPA rewrites + cache headers.

1. In Vercel → **Add New Project** → import the GitHub repo.
2. **Root Directory:** `frontend`. Vercel auto-detects Vite from `package.json`.
3. Add environment variables before the first deploy:
   - `VITE_API_BASE_URL` = `https://creatorengine-backend.onrender.com/api`
   - `VITE_APP_NAME` = `CreatorEngine` (optional)
4. Click **Deploy**.

Vercel will give you `https://yourapp.vercel.app`. Copy this URL — you need it for `CORS_ALLOWED_ORIGINS`, `FRONTEND_BASE_URL`, `META_SUCCESS_REDIRECT_URI`, and `FIREBASE_PASSWORD_RESET_REDIRECT_URL` on the backend.

> Vercel's `vercel.json` rewrites every non-API path to `/index.html` so direct URL hits (`/dashboard`, `/automations`) work with React Router.

---

## 3. The wire-up dance

A few values need to match across all three services. Get them in this order to avoid bouncing back and forth:

1. **Deploy backend to Render first.** You can leave Vercel-dependent vars (`CORS_ALLOWED_ORIGINS`, `FRONTEND_BASE_URL`, `META_SUCCESS_REDIRECT_URI`) blank initially — the service will refuse to start until they're set, but that's fine; you'll get the URL.

2. **Deploy frontend to Vercel.** Once you have the Vercel URL, you have both halves of the puzzle.

3. **Set the cross-references on Render:**
   - `CORS_ALLOWED_ORIGINS` = Vercel URL
   - `FRONTEND_BASE_URL` = Vercel URL
   - `META_SUCCESS_REDIRECT_URI` = `<vercel-url>/instagram/callback`
   - `FIREBASE_PASSWORD_RESET_REDIRECT_URL` = `<vercel-url>/login`
   - `META_REDIRECT_URI` = `<render-url>/api/instagram/callback`

4. **Set `VITE_API_BASE_URL` on Vercel** to `<render-url>/api` and trigger a redeploy.

5. **Set the Meta app's OAuth redirect URI** to match `META_REDIRECT_URI`. See [META_WEBHOOK_SETUP.md](./META_WEBHOOK_SETUP.md).

6. **Subscribe Meta webhooks** to `<render-url>/api/webhooks/instagram` with `META_VERIFY_TOKEN`.

---

## 4. Post-deploy verification

```bash
# 1. Backend health
curl https://your-backend.onrender.com/api/health
# Expected: {"status":"UP",…}

# 2. CORS — preflight from your Vercel origin
curl -i -X OPTIONS https://your-backend.onrender.com/api/auth/login \
  -H "Origin: https://yourapp.vercel.app" \
  -H "Access-Control-Request-Method: POST"
# Expected: 200 with Access-Control-Allow-Origin: https://yourapp.vercel.app

# 3. Webhook verification (simulates Meta's handshake)
curl "https://your-backend.onrender.com/api/webhooks/instagram?hub.mode=subscribe&hub.verify_token=YOUR_VERIFY_TOKEN&hub.challenge=test123"
# Expected: test123
```

Then in a browser:
1. Open the Vercel URL.
2. Register a user.
3. Log in.
4. Open the Settings → Connect Instagram. Verify the OAuth bounce lands back at `/instagram/callback`.
5. From your test IG business account, comment on a post matching one of your automations. Watch Activity Logs.

---

## 5. Troubleshooting

| Symptom | Likely cause |
|---|---|
| Backend fails to start with "Production environment validation failed" | Read the bulleted list in Render logs — `EnvironmentValidator` tells you exactly which env vars need attention |
| Frontend loads but every API call is CORS-blocked | `CORS_ALLOWED_ORIGINS` doesn't contain your exact Vercel URL (note: scheme + no trailing slash) |
| Direct URL like `/automations` returns 404 from Vercel | `vercel.json` is missing or not in the `frontend/` directory |
| Meta webhook subscription rejects the URL | `META_VERIFY_TOKEN` in Render doesn't match the value entered in the Meta app dashboard |
| Instagram OAuth fails with "redirect_uri mismatch" | The exact URI registered in Meta differs from `META_REDIRECT_URI` (case, trailing slash, scheme) |
| Backend boots fine in dev but `EnvironmentValidator` rejects on Render | The validator only runs when `SPRING_PROFILES_ACTIVE=prod`. Locally you can preview validation: `SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run` |
| 500 on every API call after deploy | Check Render logs for stack traces. Usually a missing Firebase credential or a malformed `FIREBASE_CREDENTIALS_JSON` (one-line, properly escaped JSON) |

---

## 6. Optional: local dry-run of the production profile

Before the first real deploy, verify your env values pass validation:
```bash
cd backend
export SPRING_PROFILES_ACTIVE=prod
export JWT_SECRET=$(openssl rand -base64 64)
export FIREBASE_PROJECT_ID=...
export FIREBASE_WEB_API_KEY=...
# … all required vars
mvn spring-boot:run
```
A passing validator logs `✅ Production environment validation passed.` A failing one prints the bulleted list of problems and refuses to start.

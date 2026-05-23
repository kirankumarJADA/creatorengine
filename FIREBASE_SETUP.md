# Firebase Setup Guide

CreatorEngine uses Firebase for two things:
1. **Firebase Authentication** — user accounts, password storage, password reset emails
2. **Cloud Firestore** — automations, contacts, execution logs, all per-user data

You'll create one Firebase project and produce three artifacts: a project ID, a Web API key, and a service-account JSON.

## 1. Create the project

1. Open [Firebase console](https://console.firebase.google.com) → **Add project**.
2. Pick a name (e.g. `creatorengine-prod`). Disable Google Analytics if you don't need it — it's not used.
3. Wait for provisioning, then click into the project.

**Capture this:**
- **Project ID** — top of the page, looks like `creatorengine-prod-abc12`. This is `FIREBASE_PROJECT_ID`.

## 2. Enable Authentication

1. Left sidebar → **Build** → **Authentication** → **Get started**.
2. **Sign-in method** tab → enable **Email/Password**. Leave passwordless link disabled.
3. (Optional) **Templates** tab → customise the password reset email to mention your product.

### Web API Key
1. ⚙ (gear) → **Project settings** → **General** tab.
2. Scroll to **Your apps** → **Add app** → **Web** (the `</>` icon).
3. Register an app named "CreatorEngine Web" (no need to enable Hosting).
4. The page shows a `firebaseConfig` object. **Copy the `apiKey` value** — that's `FIREBASE_WEB_API_KEY`.

> The backend uses this key to verify passwords via the Firebase REST API. It's safe to expose, but we keep it server-side here.

## 3. Enable Firestore

1. Left sidebar → **Build** → **Firestore Database** → **Create database**.
2. **Location:** pick a region close to your Render region (e.g. `us-west` for Render's `oregon`).
3. **Start in production mode** — CreatorEngine writes its own security rules.
4. After provisioning, go to the **Rules** tab and paste:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // All writes go through the backend's Admin SDK (which bypasses
    // these rules). Reject every client-direct attempt.
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

Then **Publish**. This locks Firestore down — only the backend (via Admin SDK with the service account) can touch the data.

## 4. Service Account credentials

The backend talks to Firestore using a service account, not a user account.

1. **Project settings** → **Service accounts** tab.
2. Click **Generate new private key** → **Generate key**. A JSON file downloads (e.g. `creatorengine-prod-firebase-adminsdk-xyz.json`).
3. **Treat this file like a password.** Anyone with it has full admin access to your Firebase project.

### For Render (production)
1. Open the JSON file.
2. Convert to a single line (preserving JSON validity). On macOS/Linux:
   ```bash
   jq -c . path/to/firebase-service-account.json | pbcopy   # macOS
   jq -c . path/to/firebase-service-account.json | xclip    # Linux
   ```
   Or use any online JSON minifier.
3. In Render dashboard → your service → **Environment** → add `FIREBASE_CREDENTIALS_JSON` and paste the single-line JSON as the value.
4. Leave `FIREBASE_CREDENTIALS_PATH` unset.

### For local development
1. Save the JSON file as `backend/firebase-service-account.json`.
2. Add `firebase-service-account.json` to your `.gitignore` (verify it's not committed).
3. In `backend/.env`, set `FIREBASE_CREDENTIALS_PATH=./firebase-service-account.json`.
4. Leave `FIREBASE_CREDENTIALS_JSON` unset.

## 5. Password reset emails

When a user clicks "Forgot password", Firebase sends them an email with a reset link. The link includes a redirect URL once the password is changed.

Set `FIREBASE_PASSWORD_RESET_REDIRECT_URL` to your login page:
- Dev: `http://localhost:5173/login`
- Prod: `https://yourapp.vercel.app/login`

> **Authorised domains:** Firebase only sends to authorised domains. In the Authentication → Settings → Authorized domains list, add your Vercel domain (e.g. `yourapp.vercel.app`).

## 6. Verify

After setting `FIREBASE_PROJECT_ID`, `FIREBASE_WEB_API_KEY`, and credentials, start the backend. The startup log should include:
```
Firebase initialised for project 'creatorengine-prod-abc12'.
```

If you see warnings about Application Default Credentials, your credentials weren't picked up — re-check the path or JSON env var.

Test password login from the frontend's register/login flow. Successful login indicates Auth + Firestore both work end-to-end.

## Production checklist

- [ ] Firestore is in production mode with the "deny all" rule above
- [ ] Service account JSON is NOT committed to git
- [ ] `FIREBASE_CREDENTIALS_JSON` is set as a secret env var on Render (not in code)
- [ ] Your Vercel domain is in Firebase's Authorized Domains list
- [ ] `FIREBASE_PASSWORD_RESET_REDIRECT_URL` points to the production Vercel URL
- [ ] You've tested password reset end-to-end (the link in the email should work and bounce back to your login page)

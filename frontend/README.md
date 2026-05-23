# CreatorEngine — Frontend

React 18 + Vite frontend for the CreatorEngine authentication foundation.

## Tech stack

- **React 18** + **Vite 5**
- **Tailwind CSS** with a custom design-token palette
- **React Router v6** with protected + public-only route guards
- **Zustand** for the auth store (hand-rolled localStorage hydration)
- **React Hook Form** for typed, validation-aware forms
- **Axios** with request + response interceptors
- **Framer Motion** for entry animations
- **Lucide React** icons
- **React Hot Toast** for inline error / success feedback

## Quick start

```bash
npm install
cp .env.example .env
npm run dev                # → http://localhost:5173
```

The dev server proxies `/api` to `http://localhost:8080` so you can
hit the Spring Boot backend without CORS headaches.

## Project structure

```
src/
├── components/
│   ├── form/             # Reusable form primitives
│   │   ├── Button.jsx
│   │   ├── Checkbox.jsx
│   │   ├── FormField.jsx
│   │   └── PasswordField.jsx
│   ├── PageLoader.jsx    # Suspense / hydration fallback
│   ├── ProtectedRoute.jsx
│   └── PublicOnlyRoute.jsx
├── hooks/
│   └── useAuth.js        # Selector hook over the auth store
├── layouts/
│   └── AuthLayout.jsx    # Split-screen marketing + form
├── pages/
│   ├── Login.jsx
│   ├── Register.jsx
│   ├── ForgotPassword.jsx
│   ├── Dashboard.jsx     # Protected — placeholder
│   └── NotFound.jsx
├── routes/
│   └── AppRoutes.jsx     # All routing wired here
├── services/
│   ├── api.js            # Axios instance + interceptors
│   └── authService.js    # Calls /api/auth/*
├── store/
│   └── authStore.js      # Zustand store + hydration
├── styles/
│   └── globals.css       # Tailwind layers + base styles
├── utils/
│   ├── constants.js      # ROUTES, STORAGE_KEYS, etc.
│   ├── helpers.js
│   ├── storage.js        # localStorage wrapper
│   └── validators.js     # Email / password rules
├── App.jsx
└── main.jsx
```

## Auth flow

1. `App` calls `useAuthStore.bootstrap()` on mount, which reads
   `access_token`, `refresh_token`, and `user` from localStorage and
   sets `isHydrated: true`.
2. Both `ProtectedRoute` and `PublicOnlyRoute` block rendering until
   `isHydrated` is true — this prevents a flash-of-login on page
   reload for already-signed-in users.
3. Successful `login` / `register` calls write tokens to localStorage
   and the store atomically via `_persistSession`.
4. Every outgoing axios request gets `Authorization: Bearer <token>`
   from localStorage via the request interceptor.
5. On any 401 response, the response interceptor clears local state
   and redirects to `/login` — unless the call was made with
   `{ silent: true }` (used internally for logout).

## Environment variables

All client-exposed vars must be prefixed with `VITE_`.

| Var                    | Default                          | Purpose                                  |
|------------------------|----------------------------------|------------------------------------------|
| `VITE_API_BASE_URL`    | `http://localhost:8080/api`      | Backend base URL used by axios           |
| `VITE_API_PROXY_TARGET`| `http://localhost:8080`          | Vite dev-proxy target for `/api`         |
| `VITE_APP_NAME`        | `CreatorEngine`                  | Brand string shown in UI                 |

## Scripts

| Command           | What it does                       |
|-------------------|------------------------------------|
| `npm run dev`     | Start the Vite dev server          |
| `npm run build`   | Production build into `dist/`      |
| `npm run preview` | Serve the production build locally |

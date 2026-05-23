# CreatorEngine — Backend (Auth Service)

Spring Boot 3.3 + Java 17 authentication backend.

## Tech stack

- **Spring Boot 3.3** — Web, Security, Validation, Actuator
- **Java 17**
- **Spring Security** + **JWT** (jjwt 0.12) — stateless authentication
- **Firebase Authentication** — credential storage (password hashing, reset, future MFA/social)
- **Firestore** — user profile + role storage
- **Lombok** — boilerplate reduction
- **spring-dotenv** — `.env` support in dev

## How the auth flow works

```
Register                                   Login
────────                                   ─────
 ┌───────────┐                              ┌───────────┐
 │  Client   │                              │  Client   │
 └─────┬─────┘                              └─────┬─────┘
       │ POST /api/auth/register                  │ POST /api/auth/login
       ▼                                          ▼
 ┌─────────────┐                            ┌──────────────────────┐
 │ AuthService │                            │     AuthService      │
 └─────┬───────┘                            └──┬───────────────────┘
       │                                       │  verifyPassword
       │ createUser()                          │      ▼
       ▼                                       │  ┌─────────────────┐
 ┌───────────────┐                             │  │ Firebase REST   │
 │ Firebase Auth │                             │  │ signInWithPwd   │
 └─────┬─────────┘                             │  └─────────────────┘
       │ uid                                   │
       ▼                                       │  loadByUid
 ┌─────────────┐                               │      ▼
 │  Firestore  │                               │  ┌─────────────┐
 │  users/{uid}│                               │  │  Firestore  │
 └─────────────┘                               │  └─────────────┘
       │                                       │
       ▼                                       ▼
 issue JWT (access + refresh) ───────► issue JWT (access + refresh)
```

- Passwords **never live in our database**. Firebase handles them.
- We issue **our own JWTs** so the rest of the API doesn't depend on
  Firebase being online for every request.
- The Firestore `users/{uid}` document mirrors the Firebase user with
  whatever profile data + roles we want to track.

## Quick start

```bash
# 1. Copy environment template and fill in secrets
cp .env.example .env

# 2. Drop a Firebase service-account JSON at ./firebase-service-account.json
#    (Firebase console → Project settings → Service accounts → Generate new private key)

# 3. From the Firebase console, copy the Web API key into FIREBASE_WEB_API_KEY
#    (Project settings → General → Web API Key)

# 4. Build & run
./mvnw spring-boot:run
```

The API boots on `http://localhost:8080`.

## Project structure

```
src/main/java/com/creatorengine/
├── auth/
│   ├── controller/   AuthController         — REST endpoints
│   ├── service/      AuthService            — credential lifecycle
│   │                 FirebaseAuthClient     — REST wrapper for sign-in
│   ├── dto/          RegisterRequest, LoginRequest, ForgotPasswordRequest
│   │                 AuthResponse, UserResponse
│   ├── entity/       User, Role
│   └── repository/   UserRepository         — Firestore CRUD
├── common/           ApiResponse            — uniform envelope
├── config/           AppProperties, FirebaseConfig, CorsConfig
├── security/         SecurityConfig, JwtTokenProvider, JwtAuthenticationFilter,
│                     UserPrincipal, RestAuthenticationEntryPoint, SecurityUtils
└── exception/        GlobalExceptionHandler + custom exceptions
```

## Endpoints

| Method | Path                       | Auth | Body                              | Returns                |
|--------|----------------------------|------|-----------------------------------|------------------------|
| POST   | `/api/auth/register`       | ❌   | `{ name, email, password }`       | `AuthResponse`         |
| POST   | `/api/auth/login`          | ❌   | `{ email, password }`             | `AuthResponse`         |
| POST   | `/api/auth/forgot-password`| ❌   | `{ email }`                       | message only           |
| POST   | `/api/auth/logout`         | ✅   | —                                 | message only           |
| GET    | `/api/auth/me`             | ✅   | —                                 | `UserResponse`         |

All responses follow the envelope:

```json
{
  "success": true,
  "message": "OK",
  "data":    { ... },
  "errors":  null,
  "timestamp": "2026-..."
}
```

## JWT strategy

| Token   | TTL      | Purpose                                      |
|---------|----------|----------------------------------------------|
| access  | 15 min   | Sent on every request                        |
| refresh | 7 days   | Only valid at the refresh endpoint           |

Tokens are distinguished by a `typ` claim — a refresh token can never
be used in place of an access token.

Generate a strong signing secret with:

```bash
openssl rand -base64 64
```

## Role-based authorization

The skeleton ships with two roles (`USER`, `ADMIN`) — add more as
needed by extending `auth/entity/Role.java`.

Method-level checks via `@PreAuthorize` are enabled globally
(`@EnableMethodSecurity` in `SecurityConfig`). Example:

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/users")
public ApiResponse<List<UserResponse>> listAll() { ... }
```

## Configuration reference

| Env var                                | Default                  | Notes                                 |
|----------------------------------------|--------------------------|---------------------------------------|
| `SPRING_PROFILES_ACTIVE`               | `dev`                    | `dev` or `prod`                       |
| `SERVER_PORT`                          | `8080`                   |                                       |
| `CORS_ALLOWED_ORIGINS`                 | localhost:5173,3000      | CSV                                   |
| `JWT_SECRET`                           | placeholder              | **required for prod**                 |
| `JWT_ACCESS_EXPIRATION_MS`             | `900000` (15m)           |                                       |
| `JWT_REFRESH_EXPIRATION_MS`            | `604800000` (7d)         |                                       |
| `FIREBASE_PROJECT_ID`                  | —                        | **required**                          |
| `FIREBASE_WEB_API_KEY`                 | —                        | **required for login**                |
| `FIREBASE_CREDENTIALS_PATH`            | —                        | service-account JSON path             |
| `FIREBASE_CREDENTIALS_JSON`            | —                        | inline JSON (takes precedence)        |
| `FIREBASE_PASSWORD_RESET_REDIRECT_URL` | localhost:5173/login     | continue-URL in reset emails          |

## Security notes

- Email enumeration is mitigated: invalid login and unknown-email
  forgot-password both return the same generic message.
- Passwords are validated server-side (8–128 chars, must contain a
  letter and a digit). Tune in `RegisterRequest`.
- The Firebase Web API key is required for the login endpoint. It is
  considered safe to expose (it identifies your project, not your
  credentials), but you should still restrict it to your domain in the
  Google Cloud console.
- `passwordEncoder` (BCryptPasswordEncoder) is exposed even though
  Firebase performs the actual password hashing — it's available for
  any other secrets you may need to hash later.

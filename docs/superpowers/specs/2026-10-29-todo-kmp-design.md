# ToDo KMP — Design Spec

Date: 2026-10-29
Status: approved

## 1. Goal

A full stack ToDo application:

- Android and iOS clients with a shared Compose Multiplatform UI.
- A Kotlin backend (Ktor) that owns all data; clients talk to it only through a REST API.
- Showcases Clean Architecture, MVI, a shared KMP API contract, automated tests and CI.

## 2. Functional requirements

### 2.1 Authentication

| ID | Requirement |
|----|-------------|
| A1 | A user can create an account with email + password. |
| A2 | A user can sign in with email + password. |
| A3 | A user can sign in with Google. The first Google sign-in creates the account; if an account with the same verified email already exists, it is linked. |
| A4 | The session survives app restarts (tokens are kept in secure storage). |
| A5 | When the access token expires the client refreshes it transparently; if refresh fails the user is sent to the login screen. |
| A6 | A user can log out; the refresh token is revoked on the server. |

Validation rules (client and server):

- Email: non-blank, matches `^[^@\s]+@[^@\s]+\.[^@\s]+$`, max 254 chars, compared case-insensitively.
- Password: 8..128 chars.

### 2.2 Todos

A todo has: `id` (UUID), `title` (1..200 chars, trimmed), `description` (0..2000 chars), `dueAt` (date + time, an `Instant`), `status` (`TODO | IN_PROGRESS | DONE`), `createdAt`, `updatedAt`.

| ID | Requirement |
|----|-------------|
| T1 | The list screen shows todos for one of three periods: **Today**, **This week** (Monday–Sunday), **This month**. Default: Today. |
| T2 | Each row shows time, date, title and a status chip. Rows are sorted by `dueAt` ascending. |
| T3 | Tapping a row opens the detail screen where all fields can be edited, saved, or the todo deleted (with confirmation). |
| T4 | A floating action button opens the detail screen in "create" mode with `dueAt` defaulting to the next full hour. |
| T5 | Pull-to-refresh reloads the list; returning from the detail screen reloads it as well. |
| T6 | A user only sees and modifies their own todos. |

Period boundaries are computed on the client in the device time zone and sent to the server as a half-open interval `[from, to)` of `Instant`s.

## 3. Architecture

```
app/androidApp ─┐
app/iosApp ─────┴─> app/shared (presentation: MVI + Compose UI + DI)
                       ├─> app/domain (entities, repository interfaces, use cases)
                       └─> app/data  (Ktor client, repositories, token storage) ─> app/domain, core
server (Ktor) ─> core
core: @Serializable DTOs + API paths shared by client and server
```

Dependency rule: `app/domain` depends only on `kotlinx-coroutines-core` and `kotlinx-datetime`. `app/shared` references `app/data` only for DI wiring.

Server packages: `api` (routes, DTO mapping) → `application` (services) → `domain` (models, ports); `infrastructure` implements the ports (Exposed, JWT, bcrypt, Google verifier).

### 3.1 MVI

- `MviViewModel<State, Intent, Effect>`: exposes `state: StateFlow<State>`, `effects: Flow<Effect>`, `onIntent(Intent)`.
- Flow: `Intent` → use case → `reduce(state, result)` (pure) → new `State`. One-off events (navigation, snackbar) go through `Effect`.
- Each screen = `XxxRoute` (wires the ViewModel, collects effects) + stateless `XxxScreen(state, onIntent)`.

## 4. REST API (`/api/v1`)

All bodies are JSON. Timestamps are ISO-8601 instants (`2026-10-29T13:00:00Z`).

| Method | Path | Auth | Request | Response |
|--------|------|------|---------|----------|
| POST | `/auth/register` | – | `EmailCredentialsDto` | 201 `TokenPairDto` |
| POST | `/auth/login` | – | `EmailCredentialsDto` | 200 `TokenPairDto` |
| POST | `/auth/google` | – | `GoogleLoginDto {idToken}` | 200 `TokenPairDto` |
| POST | `/auth/refresh` | – | `RefreshTokenDto` | 200 `TokenPairDto` |
| POST | `/auth/logout` | – | `RefreshTokenDto` | 204 |
| GET | `/me` | Bearer | – | 200 `UserDto` |
| GET | `/todos?from=&to=` | Bearer | – | 200 `List<TodoDto>` |
| GET | `/todos/{id}` | Bearer | – | 200 `TodoDto` |
| POST | `/todos` | Bearer | `TodoInputDto` | 201 `TodoDto` |
| PUT | `/todos/{id}` | Bearer | `TodoInputDto` | 200 `TodoDto` |
| DELETE | `/todos/{id}` | Bearer | – | 204 |
| GET | `/health` | – | – | 200 `{"status":"UP"}` |

DTOs:

```kotlin
EmailCredentialsDto(email: String, password: String)
GoogleLoginDto(idToken: String)
RefreshTokenDto(refreshToken: String)
TokenPairDto(accessToken: String, refreshToken: String, expiresInSeconds: Long)
UserDto(id: String, email: String)
TodoInputDto(title: String, description: String, dueAt: Instant, status: TodoStatusDto)
TodoDto(id: String, title: String, description: String, dueAt: Instant, status: TodoStatusDto, createdAt: Instant, updatedAt: Instant)
enum TodoStatusDto { TODO, IN_PROGRESS, DONE }
ErrorResponse(code: String, message: String)
```

Errors (`ErrorResponse.code`):

| HTTP | code | When |
|------|------|------|
| 400 | `VALIDATION_ERROR` | invalid body / query |
| 401 | `UNAUTHORIZED` | missing/expired access token, wrong credentials, invalid refresh/Google token |
| 404 | `NOT_FOUND` | todo does not exist **or belongs to another user** |
| 409 | `EMAIL_TAKEN` | register with an existing email |
| 500 | `INTERNAL_ERROR` | anything else |

## 5. Security

- Passwords: bcrypt (cost 12).
- Access token: JWT HS256, 15 minutes, claims `sub` = user id, `iss`, `aud`.
- Refresh token: 32 random bytes (base64url), 30 days, only its SHA-256 hash is stored; every refresh rotates the token (old one deleted).
- Google: `GoogleIdTokenVerifier` checks signature, expiry, issuer and that `aud` is one of the configured client IDs; `email_verified` must be true.
- Client: tokens in Keychain (iOS) / EncryptedSharedPreferences (Android) via KVault. Cleartext HTTP only in debug builds.

## 6. Persistence

PostgreSQL, Flyway migrations:

- `users(id uuid pk, email varchar(254) unique, password_hash varchar null, google_subject varchar unique null, created_at timestamptz)`
- `refresh_tokens(id uuid pk, user_id uuid fk on delete cascade, token_hash varchar unique, expires_at timestamptz)`
- `todos(id uuid pk, user_id uuid fk on delete cascade, title varchar(200), description varchar(2000), due_at timestamptz, status varchar(16), created_at timestamptz, updated_at timestamptz)`, index `(user_id, due_at)`.

## 7. Client screens

- **AuthGate** — reads the session; routes to Login or TodoList.
- **Login** — email, password, "Sign in", "Sign in with Google", link to Register.
- **Register** — email, password, confirm password, "Create account".
- **TodoList** — top bar with logout, tabs Today / Week / Month, list, empty state, pull-to-refresh, FAB.
- **TodoDetail** — title, description, date picker, time picker, status selector, Save; Delete (edit mode only) with confirmation dialog.

Error handling: field errors are shown inline; network/server errors are shown in a snackbar with the list keeping its last data.

## 8. Testing

- `app/domain`: period range calculation (week/month boundaries, year change), validation, use cases with fakes.
- `app/data`: API + repositories with Ktor `MockEngine`, error mapping, token refresh.
- `app/shared`: reducers and ViewModels (Turbine, fake use cases).
- `server`: `testApplication` + Testcontainers PostgreSQL — auth flows, CRUD, period filter, user isolation, error codes.

## 9. Out of scope

Offline cache, push notifications, reminders, sharing todos, password reset, paging.

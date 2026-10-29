# ToDo — Kotlin Multiplatform + Ktor

A full stack todo app: Android and iOS clients with a shared Compose Multiplatform UI, and a Ktor backend
that owns all data. Clients talk to the backend only through a REST API.

- Sign up / sign in with email and password, or with Google (native ID token, verified on the server).
- Todo list for **Today**, **This week** and **This month**; each row shows time, date, title and status.
- Detail screen to create, edit and delete a todo (date and time pickers, status `To do / In progress / Done`).
- Pull-to-refresh, inline validation, snackbar errors, transparent access token refresh.

The full specification is in [docs/superpowers/specs/2026-10-29-todo-kmp-design.md](docs/superpowers/specs/2026-10-29-todo-kmp-design.md).

## Architecture

```
app/androidApp ─┐
app/iosApp ─────┴─> app/shared   presentation: MVI ViewModels, Compose UI, navigation, Koin
                       ├─> app/domain   entities, repository interfaces, use cases (pure Kotlin)
                       └─> app/data     Ktor client, repositories, token storage ─> app/domain, core
server (Ktor) ─> core
core: @Serializable DTOs and API routes shared by the clients and the server
```

**Client — Clean Architecture + MVI.** `app/domain` has no framework dependencies besides coroutines and
kotlinx-datetime. Each screen is a `Contract` (State / Intent / Effect / Mutation), a pure `Reducer`,
a `ViewModel` built on `MviViewModel`, and a stateless `Screen(state, onIntent)` wired by a `Route`.
Navigation reacts to the session: logout or a failed token refresh sends the user back to login.

**Server — layered.** `api` (routes, DTO mapping, error handling) → `application` (services) → `domain`
(models, ports). `infrastructure` implements the ports with Exposed + PostgreSQL, JWT, bcrypt and the Google verifier.

| Concern | Choice |
|---------|--------|
| UI | Compose Multiplatform, Material 3, navigation-compose with type-safe routes |
| DI | Koin (client and server) |
| Networking | Ktor client (OkHttp / Darwin), bearer auth with automatic refresh |
| Secure storage | KVault (EncryptedSharedPreferences / Keychain) |
| Backend | Ktor, Exposed, PostgreSQL, Flyway, HikariCP |
| Auth | JWT access token (15 min), rotating refresh token (30 days, stored as SHA-256), bcrypt |
| Tests | kotlin-test, coroutines-test, Turbine, Ktor MockEngine, Ktor testApplication, Testcontainers |

## Running locally

Requirements: JDK 21, Docker, Android Studio; Xcode 16+ for iOS.

```bash
# 1. Database (PostgreSQL on localhost:5434)
docker compose up -d

# 2. Backend on http://localhost:8080
./gradlew :server:run
curl http://localhost:8080/health   # {"status":"UP"}

# 3. Android app (emulator reaches the host as 10.0.2.2)
./gradlew :app:androidApp:installDebug
```

iOS: open `app/iosApp/iosApp.xcodeproj` in Xcode and run the shared `iosApp` scheme on a simulator.
The simulator reaches the host as `localhost`.

### Configuration

Server (environment variables, defaults in `server/src/main/resources/application.yaml`):

| Variable | Default | Purpose |
|----------|---------|---------|
| `PORT` | `8080` | HTTP port |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | local docker compose database | PostgreSQL connection |
| `APP_ENV` | `development` | `production` refuses to start without a real `JWT_SECRET` (the Docker image sets it) |
| `JWT_SECRET` | built-in key in development | HS256 signing key, at least 32 characters — **required in production** |
| `AUTH_RATE_LIMIT_PER_MINUTE` | `20` | Requests per client IP per minute to `/api/v1/auth/*` |
| `TRUST_PROXY` | `false` | Take the client IP from `X-Forwarded-For`; enable only behind a reverse proxy |
| `GOOGLE_CLIENT_IDS` | project web and iOS clients | Comma-separated OAuth client IDs accepted as the Google ID token audience |

Android (Gradle properties, project defaults in `gradle.properties`, overridable in `~/.gradle/gradle.properties`):

| Property | Default |
|----------|---------|
| `todolist.debugApiBaseUrl` | `http://10.0.2.2:8080` — the local server, used by debug builds |
| `todolist.releaseApiBaseUrl` | `https://todolist-server-zyck.onrender.com` — the deployed server, used by release builds; must be `https://`, or the release build fails |
| `todolist.googleWebClientId` | project web client (empty disables Google sign-in) |

iOS (`app/iosApp/Configuration/Config.xcconfig`): `API_BASE_URL` for the Debug configuration (`http://localhost:8080`),
`API_BASE_URL[config=Release]` for the deployed server (must be `https://`, a Release build stops at launch otherwise),
`GOOGLE_IOS_CLIENT_ID`, `GOOGLE_REVERSED_CLIENT_ID`.

Cleartext HTTP is allowed only for local hosts: through a debug-only network security config on Android,
and `NSAllowsLocalNetworking` on iOS.

### Enabling Google sign-in

1. In Google Cloud Console create OAuth clients: **Web application**, **Android** (package
   `com.pelypets.todolist` + debug SHA-1), and **iOS** (bundle id `com.pelypets.todolist`).
2. Server: `GOOGLE_CLIENT_IDS=<web client id>,<ios client id>` (the default contains the project's clients).
3. Android: `todolist.googleWebClientId=<web client id>`. The ID token is issued for the web client
   through Credential Manager.
4. iOS: set `GOOGLE_IOS_CLIENT_ID` and `GOOGLE_REVERSED_CLIENT_ID` in `Config.xcconfig`. The GoogleSignIn
   SDK is added through Swift Package Manager.

Without these values the "Continue with Google" button shows an error message; email sign-in keeps working.

## Deployment

The backend ships as a Docker image ([`Dockerfile`](Dockerfile)) and deploys to Render with a Neon PostgreSQL
database through the [`render.yaml`](render.yaml) Blueprint: every push to `main` is deployed once CI passes.
Step-by-step setup: [docs/deploy.md](docs/deploy.md).

## REST API (`/api/v1`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | – | Create an account, returns a token pair |
| POST | `/auth/login` | – | Email + password sign-in |
| POST | `/auth/google` | – | Exchange a Google ID token for a token pair |
| POST | `/auth/refresh` | – | Rotate the refresh token |
| POST | `/auth/logout` | – | Revoke the refresh token |
| GET | `/me` | Bearer | Current user |
| GET | `/todos?from=&to=` | Bearer | Todos with `dueAt` in `[from, to)` |
| GET / PUT / DELETE | `/todos/{id}` | Bearer | Read, update, delete one todo |
| POST | `/todos` | Bearer | Create a todo |
| GET | `/health` | – | Liveness check |

Errors are returned as `{"code": "...", "message": "..."}` with codes `VALIDATION_ERROR`, `UNAUTHORIZED`,
`NOT_FOUND`, `EMAIL_TAKEN`, `INTERNAL_ERROR`. A todo owned by another user is reported as `NOT_FOUND`.

## Tests

```bash
./gradlew :core:jvmTest :server:test               # server tests need Docker (Testcontainers)
./gradlew :app:domain:testAndroidHostTest :app:data:testAndroidHostTest :app:shared:testAndroidHostTest
./gradlew :app:shared:iosSimulatorArm64Test         # macOS only
```

| Module | What is covered |
|--------|-----------------|
| `core` | DTO serialization contract |
| `server` | Auth flows (register, login, Google, refresh rotation, logout), rate limiting, production config checks, todo CRUD, period filter, user isolation, error codes — against a real PostgreSQL |
| `app/domain` | Period boundaries (week start, month and year changes), validation, use cases |
| `app/data` | Requests and mapping via `MockEngine`, HTTP → domain error mapping, transparent token refresh, session end on failed refresh |
| `app/shared` | Reducers and ViewModels of every screen |

With Colima instead of Docker Desktop, point Testcontainers to its socket:
`export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"`.

## Code quality

```bash
./gradlew spotlessApply            # format with ktlint (+ Compose rules); spotlessCheck only verifies
./gradlew :app:androidApp:lintDebug # Android Lint; errors fail the build
./gradlew koverHtmlReport          # merged coverage report in build/reports/kover/html
```

Formatting rules live in [`.editorconfig`](.editorconfig) and match Android Studio's Kotlin style.

## CI/CD

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on every push and pull request:

- **quality** (Linux): ktlint formatting check and Android Lint.
- **server** (Linux): contract and server tests with Testcontainers, coverage report.
- **docker** (Linux): builds the server image that Render deploys.
- **android** (Linux): domain, data and shared tests with coverage, the debug APK (uploaded as an artifact)
  and a minified release build.
- **ios** (macOS): the same shared tests on the iOS simulator, plus linking the iOS framework.

When all checks pass on `main`, Render deploys the server ([docs/deploy.md](docs/deploy.md)).
Pushing a `v*` tag runs [`.github/workflows/release.yml`](.github/workflows/release.yml), which builds a signed
Android APK and App Bundle and publishes a GitHub release; with the store credentials set it also uploads the
bundle to Google Play and the iOS build to App Store Connect / TestFlight ([docs/release.md](docs/release.md)).
[Dependabot](.github/dependabot.yml) opens weekly pull requests for Gradle, GitHub Actions and Docker updates.

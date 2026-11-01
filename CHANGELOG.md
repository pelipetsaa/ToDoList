# Changelog

User- and operator-visible changes. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versions are the `v*` tags that trigger the release workflow.

## Unreleased

### Fixed
- Sign-up and sign-in with a password longer than 72 bytes (e.g. 128 characters, or 40 Cyrillic letters) no longer
  fail with a server error.
- Signing in with Google to an account registered with the same email removes that account's password and signs
  out its other sessions, so whoever registered an address they do not own loses access. An email already linked
  to another Google account is rejected.
- The apps no longer sign the user out when a token refresh fails because the server is restarting or the network
  is down; only a rejected refresh token ends the session. Refresh and logout are no longer rate limited.
- The Google sign-in button no longer stays disabled after the Android screen is rotated while the account picker is
  open.
- Validation errors reported by the server show the server's explanation.
- Expired refresh tokens are deleted when the user signs in again.

### Changed
- An account that has signed in with Google can no longer sign in with its old password.

## 1.1.0 — 2026-10-29

### Added
- Google Play upload of release bundles (internal track by default) once `PLAY_SERVICE_ACCOUNT_JSON` is set.
- iOS release builds uploaded to App Store Connect / TestFlight once the App Store Connect secrets are set.
- Server: `/health/ready` readiness check including the database, Prometheus metrics at `/metrics`
  (with `METRICS_TOKEN`), JSON logs in the Docker image, Swagger UI at `/docs`.
- Russian translation of the apps, including day and month names in dates; the language follows the device.

### Changed
- The apps wait up to 75 seconds for the server, so the first request after the free Render instance has slept no
  longer fails; reads are retried while the server restarts.
- The iOS bundle ID is always `com.pelypets.todolist`, no longer suffixed with the team ID.

## 1.0.0 — 2026-10-29

First release: Android and iOS apps (Compose Multiplatform) with a Ktor backend.

### Added
- Sign-up and sign-in with email and password or Google; JWT access tokens with rotating refresh tokens.
- Todos for today, this week and this month; create, edit and delete with date, time and status.
- Backend deployed on Render with a Neon PostgreSQL database; release apps talk to it over HTTPS.
- Production safeguards: the server refuses to start without a real `JWT_SECRET`, and auth endpoints are rate
  limited per client IP.
- CI: formatting (ktlint), Android Lint, server, Android and iOS tests with coverage, Docker image and minified
  release builds. Signed Android releases on `v*` tags; Dependabot for dependency updates.

### Fixed
- Timestamps returned on create and update now match the stored ones (PostgreSQL keeps microseconds).

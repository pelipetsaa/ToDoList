## What and why

<!-- What changes for users or developers, and why. Link the issue if there is one. -->

## How it was checked

<!-- Tests added or run, manual checks on Android / iOS / server. -->

## Checklist

- [ ] `./gradlew spotlessApply` — formatting is clean
- [ ] Tests cover the change (server, `app/*` or both)
- [ ] API change: `core` DTOs, `openapi/documentation.yaml` and the clients are updated together
- [ ] Database change: a new Flyway migration in `server/src/main/resources/db/migration`, never an edited one
- [ ] New user-visible text exists in both `composeResources/values` and `composeResources/values-ru`
- [ ] `CHANGELOG.md` has an entry under **Unreleased** if users or operators will notice the change

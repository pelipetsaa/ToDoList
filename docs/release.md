# Releasing the Android app

Release builds are minified with R8, talk to the deployed server over HTTPS and are signed with the release key.
Pushing a tag `vX.Y.Z` runs [`.github/workflows/release.yml`](../.github/workflows/release.yml), which builds a
signed APK and App Bundle, publishes the APK as a GitHub release and, once Google Play is set up (step 5), uploads
the bundle to Google Play. `versionName` comes from the tag and `versionCode` from the workflow run number, so every
release gets a higher code, as Google Play requires.

## One-time setup

### 1. Create the release key

```bash
keytool -genkeypair -v -keystore ~/todolist-release.jks -alias todolist -keyalg RSA -keysize 4096 -validity 10000
```

Keep the file and its passwords somewhere safe (a password manager). **Losing the key means the app can never be
updated in Google Play under the same listing.** Never commit it.

### 2. Local release builds (optional)

Add to `~/.gradle/gradle.properties` (outside the repository):

```properties
todolist.signing.storeFile=/Users/<you>/todolist-release.jks
todolist.signing.storePassword=...
todolist.signing.keyAlias=todolist
todolist.signing.keyPassword=...
```

Without these properties a release build is signed with the debug key: it installs for testing but cannot be
uploaded to Google Play.

### 3. GitHub secrets and variables

In the repository: **Settings → Secrets and variables → Actions**.

| Name | Kind | Value |
|------|------|-------|
| `ANDROID_KEYSTORE_BASE64` | secret | output of `base64 -i ~/todolist-release.jks` |
| `ANDROID_KEYSTORE_PASSWORD` | secret | keystore password |
| `ANDROID_KEY_ALIAS` | secret | `todolist` |
| `ANDROID_KEY_PASSWORD` | secret | key password |

The server address is not a secret: it comes from `todolist.releaseApiBaseUrl` in `gradle.properties`.

### 4. Google sign-in for release builds

Google checks the signing certificate of the app. Add an **Android** OAuth client in Google Cloud Console with
package `com.pelypets.todolist` and the SHA-1 of the release key:

```bash
keytool -list -v -keystore ~/todolist-release.jks -alias todolist | grep SHA1
```

Each SHA-1 needs its own Android client (one client holds one fingerprint), all in the same project as the web
client the app requests tokens for. Without a matching client the account picker never opens and Google Play
services logs `Developer console is not set up correctly`.

### 5. Google Play (optional)

Until this is done, releases still build and go to GitHub; the Google Play upload step is skipped.

1. **Play Console account** ($25 once) at play.google.com/console, then **Create app** with package
   `com.pelypets.todolist`. Fill in the store listing, privacy policy URL, Data safety (the app stores an email
   address and the user's todos), content rating and target audience.
2. **First upload by hand.** The Play API cannot create an app, so upload the first `androidApp-release.aab` from
   the `android-release` artifact of a release run to **Internal testing** in Play Console. Opt in to
   **Play App Signing** when asked: the release key then becomes the *upload key*.
3. **Google sign-in for Play builds.** Google re-signs the app with its own key. Copy the **SHA-1 of the app signing
   key** from **Test and release → App integrity → App signing** and add a third Android OAuth client with it
   (see step 4).
4. **Service account for CI.** In Google Cloud Console (any project) create a service account and a JSON key for it.
   In Play Console → **Users and permissions → Invite new users**, invite the service account's email and grant it
   **Release apps to testing tracks** (and **Release to production** if CI should publish to production).
5. **GitHub secret** `PLAY_SERVICE_ACCOUNT_JSON`: the whole content of the JSON key file.
6. While the app has never been published, Google Play accepts only draft releases: set the repository
   **variable** `PLAY_RELEASE_STATUS` to `draft` and remove it after the first release is live.

New personal developer accounts must run a closed test with at least 12 testers for 14 days before the first
production release; organization accounts are exempt.

## Cutting a release

```bash
git tag v1.0.0
git push origin v1.0.0
```

The workflow uploads the APK, the App Bundle (`.aab`, for Google Play) and the R8 `mapping.txt` (needed to read
stack traces from release builds) as build artifacts, attaches the APK to the GitHub release and uploads the bundle
with its mapping file to the Google Play **internal testing** track (`PLAY_TRACK` variable to change the default).

### Promoting to production

Run **Actions → Release → Run workflow**, choose the release **tag** in "Use workflow from", and set:

| Input | Value |
|-------|-------|
| `play_track` | `production` (or `alpha` / `beta` for closed and open testing) |
| `play_status` | `completed` for everyone, or `inProgress` for a staged rollout |
| `play_user_fraction` | share of users for a staged rollout, e.g. `0.1` |

A manual run rebuilds the bundle with a new `versionCode` and does not create another GitHub release. Promoting an
existing build between tracks without rebuilding is done in Play Console.

## iOS

iOS release builds use the `Release` configuration with `API_BASE_URL[config=Release]` from
`app/iosApp/Configuration/Config.xcconfig` and the shared `iosApp` scheme.

When the App Store Connect secrets below are set, a `v*` tag also runs the **iOS release** job on macOS: it archives
the app, lets Xcode create the signing certificate and provisioning profile through the API key, and uploads the
build to App Store Connect, where it shows up in **TestFlight** after processing (usually 5–30 minutes). Without the
secrets the job is skipped and the Android release runs as before. The App Store version comes from the tag
(`v1.2.0` → `1.2.0`) and the build number from the workflow run number.

### One-time setup

1. **Apple Developer Program** membership ($99 per year) at developer.apple.com.
2. **App record.** In App Store Connect → **Apps → New App**, platform iOS, bundle ID `com.pelypets.todolist`
   (register it under **Certificates, Identifiers & Profiles → Identifiers** first if it is not offered).
3. **API key.** App Store Connect → **Users and Access → Integrations → App Store Connect API → Team Keys**,
   generate a key with the **Admin** role (needed for Xcode to create cloud-managed distribution certificates).
   Download the `.p8` file — it can be downloaded only once.
4. **GitHub secrets** (Settings → Secrets and variables → Actions):

   | Secret | Value |
   |--------|-------|
   | `APP_STORE_CONNECT_KEY_ID` | Key ID shown next to the key |
   | `APP_STORE_CONNECT_ISSUER_ID` | Issuer ID shown above the keys list |
   | `APP_STORE_CONNECT_API_KEY_P8` | the whole content of `AuthKey_<KEY_ID>.p8`, including the BEGIN/END lines |
   | `APPLE_TEAM_ID` | Team ID from developer.apple.com → **Membership details** |

The bundle ID is fixed in `Config.xcconfig`, so the Google Sign-In iOS client (registered for
`com.pelypets.todolist`) works in release builds without changes.

### Publishing on the App Store

CI stops at TestFlight. To release a version: App Store Connect → the app → **+ Version**, fill in what's new,
screenshots and review information on first release, pick the uploaded build and **Submit for Review**. The app
declares `ITSAppUsesNonExemptEncryption = NO` (it only uses HTTPS), so no export compliance question is asked per
build.

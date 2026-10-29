# Releasing the Android app

Release builds are minified with R8, talk to the deployed server over HTTPS and are signed with the release key.
Pushing a tag `vX.Y.Z` runs [`.github/workflows/release.yml`](../.github/workflows/release.yml), which builds a
signed APK and App Bundle and publishes the APK as a GitHub release. `versionName` comes from the tag and
`versionCode` from the workflow run number, so every release gets a higher code.

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

If the app is distributed through Google Play with Play App Signing, also add the SHA-1 of the app signing key
shown in Play Console.

## Cutting a release

```bash
git tag v1.0.0
git push origin v1.0.0
```

The workflow uploads the APK, the App Bundle (`.aab`, for Google Play) and the R8 `mapping.txt` (needed to read
stack traces from release builds) as build artifacts, and attaches the APK to the GitHub release.

## iOS

iOS release builds use the `Release` configuration with `API_BASE_URL[config=Release]` from
`app/iosApp/Configuration/Config.xcconfig`. Signing and App Store upload need an Apple Developer account and are
done from Xcode (**Product → Archive**); they are not automated yet.

# Building ProxMoxOpen

## Prerequisites

- JDK 17 or newer (21 is tested)
- Android SDK with platforms 31, 32, 33 and 34
- At least 4 GB of free RAM for the Gradle daemon
- macOS, Linux or Windows

## Quick build

```sh
./gradlew :app:assembleDebug
```

The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`. Install it with
`adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## Release build

```sh
./gradlew :app:assembleRelease
```

This produces an unsigned release APK at
`app/build/outputs/apk/release/app-release-unsigned.apk`. Signing is left to the
release process (F-Droid signs it for you in the main repo pipeline).

## Running tests

```sh
./gradlew testDebugUnitTest :domain:test    # unit tests
./gradlew :app:lintDebug detekt              # static analysis
```

Instrumented tests require a running emulator or device:

```sh
./gradlew :app:connectedDebugAndroidTest
```

## Reproducible builds

ProxMoxOpen is configured for reproducible builds: JDK, Android SDK components
and Gradle distribution are pinned via the version catalog and
`gradle/wrapper/gradle-wrapper.properties`. Two successive release builds from
the same commit should yield byte-identical APKs.

To verify locally:

```sh
./gradlew clean :app:assembleRelease
sha256sum app/build/outputs/apk/release/app-release-unsigned.apk
./gradlew clean :app:assembleRelease
sha256sum app/build/outputs/apk/release/app-release-unsigned.apk
```

Both hashes must match. If they do not, open an issue.

## Project layout

```
:app              Compose UI, Navigation, ViewModels
:domain           Pure Kotlin models, repository interfaces, UseCases
:data:api         Ktor Proxmox client, repository impls, TOFU TrustManager
:data:db          Room entities + DAO + database
:data:secrets     Android Keystore AES-GCM wrapper
:core:ui          Material3 theme + shared composables
:core:common      Dispatcher provider
```

## Android SDK location

Create `local.properties` with the path to your SDK:

```properties
sdk.dir=/Users/you/Library/Android/sdk
```

This file is gitignored.

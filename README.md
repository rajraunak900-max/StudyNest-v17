# StudyNest 17

StudyNest is an offline-first Android app for tracking study sessions, library visits, tasks, breaks, and study history.

## Features

- Track study sessions manually or automatically when you arrive at or leave your library.
- Organize study tasks, daily goals, breaks, and progress streaks.
- Review daily, weekly, and monthly history with Actual Study, Sitting Time, distraction, and efficiency summaries.
- Keep your study records on your device and use core features offline, with no account or app backend.

## Release

- App version: `17` (`versionName`)
- Android version code: `25`
- Application ID: `com.studynest.app`
- Minimum Android: 8.0 (API 26)
- Target Android: API 36
- Kotlin, Jetpack Compose, Material 3, Room, and WorkManager

This source snapshot follows the 1.6.12 stable release. Version 17 standardizes efficiency as Actual Study divided by Sitting Time and surfaces sitting, distraction, and efficiency summaries on Home and in History. See [STUDYNEST_PROGRESS.md](STUDYNEST_PROGRESS.md) and [docs/IMPLEMENTATION_17.md](docs/IMPLEMENTATION_17.md) for implementation and validation details.

## Build

Open the project in Android Studio with JDK 17 and Android SDK Platform 36, or run:

```powershell
./gradlew.bat assembleDebug
./gradlew.bat testDebugUnitTest
./gradlew.bat lintDebug
```

On macOS or Linux, use `./gradlew` in place of `./gradlew.bat`. Android Studio creates `local.properties` for the local SDK path; it is intentionally ignored by Git.

The release build configuration refers to `development.keystore`, which is intentionally excluded from this repository. Provide your own local debug keystore or adjust the local signing configuration to build. This development signing setup is not suitable for publishing a production release. Keep production signing keys and passwords outside the repository.

## Privacy and permissions

StudyNest stores its study records on the device. It has no account or backend service. Features such as library presence monitoring require Android permissions, including location or Wi-Fi related access; review the in-app permission explanations and Android settings before enabling them.

## Project notes

- The Room schema snapshots are in `app/schemas/`.
- Automated unit and UI tests are under `app/src/test/` and `app/src/androidTest/`.
- Build outputs, local configuration, archives, APKs, and signing keys are excluded by `.gitignore`.

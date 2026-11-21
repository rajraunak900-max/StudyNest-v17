# Resume StudyNest

Read `../STUDYNEST_PROGRESS.md` first. It is the authoritative staged checkpoint.
The current source update is 1.4.0 (versionCode 6), package com.studynest.app, Room schema 4.
Clean build/79 tests/lint passed. Verified APK is now1.4.0 (code6).
Original signing certificate retained. Physical-device acceptance remains.
Read `VALIDATION.md` for actual verification and remaining phone checks.
Do not recreate the app or redo completed changes. Preserve development.keystore.

Build with JDK 17, Gradle wrapper 8.7, Android platform/build tools 34.
Run `./gradlew clean assembleDebug testDebugUnitTest lintDebug`.
If dependencies need a proxy, derive it from the current environment, never a
previous run's port. The saved project excludes caches, build output and local.properties.
The development key uses standard debug credentials and enables same-key updates.

Save modified files and update STUDYNEST_PROGRESS.md after every stable stage.
Replace the saved source archive with a new version; only replace the APK after
verification. Include source, wrapper, schemas, docs, tests and signing key.
Phone-only checks must be recorded honestly, never inferred from Robolectric.

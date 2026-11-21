# StudyNest 1.6.11 requirements

Baseline: complete 1.6.10 source and artifacts. Preserve package, signing certificate, schema, user data, existing Actual/Library accounting, UI features, session rules, and all previous regression tests.

When automatic tracking is enabled, a manual timer may continue outside the library and be adopted by automatic library tracking when Wi-Fi or geofence presence is detected. Keep the current manual session and all accumulated focus time, avoid duplicate intervals, and honor user pause, app pause, call pause, screen lock, midnight, and duplicate detection events. When automatic tracking is disabled, preserve manual behavior.

A manual timer started while library presence is already active and automatic tracking is enabled must also be managed by library detection. After confirmed departure, the study segment pauses and normal sound restoration proceeds independently. Re-entry inside the existing Wi-Fi 30-minute grace resumes the same timer/visit; after the full grace, close the old timer/visit at the detected departure cutoff and start the appropriate new timer/visit on return. Away time is excluded from Library and Actual accounting unless valid study has an independently recorded focus interval. Do not introduce schema migration, repair, polling, or duplicate tracking loops.

Actual Study remains the established all-valid-focus total on Home, including manual study outside. Library Time remains based on detected presence. Existing History/library-specific metrics and 1.6.10 display scope stay intact. Verify daily/weekly/monthly projections, notification consistency, midnight, unknown coverage and pause rules without changing already completed 1.6.10 behavior.

Audit resources and dependencies; remove only proven unused assets. Specifically inspect the approximately 2 MB old logo candidate while preserving the active adaptive launcher and notification icons. Do not perform broad refactoring or unrelated upgrades.

Target release identity: `com.studynest.app`, versionName `1.6.11`, versionCode `21`, schema `8`, original 1.6.10/1.6.9 signing certificate. Preserve all older release artifacts. Run focused tests, full retained regression, lint, release build and signature/package/archive checks. Physical device acceptance must be labeled separately from simulation.

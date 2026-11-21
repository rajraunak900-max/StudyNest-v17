# StudyNest 1.6.12 requirements

Baseline is signed StudyNest 1.6.11. Preserve package ID, signing key, schema/data, and all prior behavior. Do not alter or replace 1.6.11 artifacts.

Define Actual Study consistently across Home, live focus, History session/day/period reports, Calendar, week/month chart, and notification as the union of valid focus time from all study sessions: library study plus valid Manual study outside the library, with no duplicate overlap. Use “Sitting Time” for the user-facing total of library-presence time and Manual timer time; merge overlapping intervals so each minute counts once. Sitting Time includes recorded distraction while the user remains sitting, while Actual excludes distraction, unknown coverage, and pauses according to existing FocusMath rules. Efficiency remains library Actual divided by detected library presence.

A Manual timer outside the library should create a focus window and its valid open interval should advance Home/day Actual and the notification Actual clock while Library remains unchanged. Preserve the existing notification's monotonic chronometer and Android usage/distraction classification. Add tests for active and completed Manual study outside the library, all-location day/calendar/period values, and library-only presence values.

Apply the configured minimum session duration equally to Manual and Automatic timer periods at home and in libraries; shorter periods remain in daily totals but are omitted from separate session-history rows. Present Manual, Manual-Automatic and Automatic timer entries with a common History card layout. The About section includes a brief feature summary and credits the author, RAUNAK RAJ.

Do not change Room schema or add a migration. Keep all previous 1.6.11 transition tests and features. The October 3 historical display cause remains unresolved.

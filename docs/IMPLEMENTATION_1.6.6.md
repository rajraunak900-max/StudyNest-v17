# StudyNest 1.6.6 — implementation and testing report

Final signed normal release completed on 4 October 2026. VersionName **1.6.6**, versionCode **16**, package **com.studynest.app**, Android8/API26 minimum, target/compile36, Room schema7. Continue this project from its saved checkpoints; no completed UI/lifecycle work was restarted or discarded.

## Completed changes

- Simplified native History navigation to Tracked study and Calendar. Date-filtered Session Information remains available through View all sessions; returning preserves date/context.
- Kept the primary Actual Study/Library Time week/month chart, five daily metrics, genuine optional distraction-app detail and concise session report. Break/Distraction trend is optional and supplies different information; repeated daily dashboards removed.
- Calendar displays compact numeric durations and its full Actual Study/Library Time legend, clean empty dates and inactive future dates. Normal phones fit seven columns; very large fonts can scroll complete readable values. Selected date leads to the same metrics/report/session list without redundant graphs. Timestamps and measurements remain exact.
- Goal/streak/minimum presets have equal sizes, gaps and centered incomplete rows, with responsive column counts. Custom validation, explicit save, Clear, state restoration and shared onboarding Skip remain intact.
- Existing qualifying active visits appear separately as In progress. Completion updates the same underlying visit ID, completed report/count and open session list through Room/lifecycle flows. UI never inserts/finalizes a visit.
- Corrected two reproduced lifecycle defects: finalize an expired persisted departure before new presence can erase it; recover at the earlier exit/checkpoint and never create a phantom visit from grace alone. Existing event handling, grace, conservative process recovery and combined detection rules are retained. Original failing reproductions and regression coverage are preserved.

Production files changed from1.6.5: app/build.gradle.kts; domain/SessionEngine.kt, domain/FocusRecorder.kt; ui/History.kt, ui/FocusMetrics.kt, ui/StudyChart.kt, ui/HistoryCalendar.kt, ui/GoalFields.kt; added ui/CalendarDuration.kt and ui/ActiveLibrarySession.kt. Tests/docs were added or updated separately. Database entities/queries/migrations, schema JSON1–7, preferences keys, resources/icons, dependencies, manifest, signing key and background automation implementation remain unchanged.

## Behavioral rules preserved

Library arrival inserts its own visit record immediately; completed qualification occurs at closure and uses continuous end-start >= the minimum captured at arrival. Default10min/custom1–1440; exact10 qualifies,9:59 does not. Break/distraction does not subtract from qualifying duration. Separate short visits cannot combine; their legitimate totals remain. Legacy nullable qualification/minimum fields keep existing earlier-study-period behavior. Same-day timer identity may resume while actual visits stay distinct. Continuous overnight visits keep their identity; daily allocation splits at local midnight. No database repair, destructive migration, artificial historical reclassification, new polling or unnecessary background work was added.

Manual/automatic timers, Wi-Fi/location detection, locked/background monitoring, selected-app distraction measurement, calls/audio options, breaks, sound/vibration/override reminders, notifications, tasks, goal/streak settings, calendar/history analytics, themes, quotes, onboarding and permissions remain. Foreground location precedes version-aware background-location setup; settings status rechecks on return and Skip/Set up later remain available. The final release restores normal tracking after the temporary read-only diagnostic; it contains no diagnostic activity/button/logger.

## Automated/simulated testing — exact provenance

| Run | Result | Meaning |
|---|---|---|
| Final full ordinary suite |465/469 passed;4 failed initially|All469 cases executed;0errors/0skips. One Tasks async-load assertion and three test-teardown database-closed exceptions. Original log/XML retained.|
| Corrected UI fixture confirmation |30/30 passed|All Onboarding/Redesign cases on normal, small large-text and wide layouts. Room close was removed from premature @After cleanup because Compose-rule disposal happens afterward; Tasks assertion waits for asynchronous content. Test-only changes.|
| Distinct validated coverage |469/469;0 unresolved failures|Combined case-by-case evidence, not a claim that the initial full command was green. All418 stable1.6.5 case identities retained; repeated confirmations are not added to the total.|
| Earlier full checkpoint |466/466 passed|Retained original evidence atdeabb06, plus existing-case confirmation after UI-only open-visit caching.|
| Lint |0Fatal/0Error;14warnings|Final lintDebug passed. Existing non-blocking warnings retained. Narrow RememberReturnType suppression is in archived test-only165 replay source, not production.|
| Release |assembleRelease passed|Final normal signed APK generated separately after regression checkpoint cc1261d24aa82655e909e0644c5640d35d06d060.|

Automated coverage includes real threshold boundaries/custom values, separate/short/manual visits, expired re-entry, recovery/grace, ordinary60min and simulated locked-screen90min Wi-Fi arrival/departure without Activity, combined detection, midnight, live active-to-completed report/list refresh, date filters, compact Calendar formatting/geometry, goal presets/custom controls, onboarding/access return, tasks, Room migrations1–7, selected usage/calls/audio, sound and notification/reminder rules. Native Robolectric screenshots were inspected; they are simulated Android renders. Three native structural replays use actual exported phone visit/session/segment rows, including archived165 History filtering and its live completion refresh. They do not execute the original APK on the phone or recreate unknown past UI state.

Raw evidence is kept beside artifacts in Verification_1.6.6/final-full-initial, final/ui-fixture-confirmation, full-regression, focused lifecycle and phone-session directories. The source includes all test source/fixtures and documents needed to rerun or understand coverage. Historical implementation checkpoint journal is preserved in IMPLEMENTATION_1.6.6_CHECKPOINT_HISTORY.md.

## Package, update and data compatibility

- apksigner verification passed; final signerSHA256 **1a1fd08ef47edfbc56c21d5d0a60205092885ba4705d662b8967a91b34c75d17**, matching stable1.6.5 exactly. Package unchanged; code16 advances stable13 and diagnostic14/compatible temporary15. APK badging verifies name/version/min26/target36;16KB zip alignment and archive integrity checks pass.
- Roomschema7, all historical migrations, stored preferences keys and database filename are unchanged. Existing-data/onboarding and migration simulations pass. Protected source comparisons and unchanged package/key establish the intended Android update path; no uninstall, Clear data or migration/repair is required.
- Physical installation/update and actual private-phone data retention were **not performed by Codex**. Structural compatibility and automated preservation tests were verified; physical acceptance remains the user's next step.
- All22 protected earlier release/diagnostic APK/ZIP/source files rehashed unchanged. Stable1.6.5 source and original diagnostic source remain clean and preserved. The temporary HistoryTest checkpoint is retained separately and is not included in this normal release.

## October3 finding — exact historical cause unresolved

Read-only snapshot exported4Oct09:46 shows eveningvisit13 completed/qualified,18:54:58.404→20:49:42.624 on3Oct, duration6,884,220ms (1h54m44.220s), captured minimum10. It is included by the unchanged3October filter and displays once in165/166 native replays. Shortvisit12 correctly fails its10min threshold; legacy visits keep their earlier-period presentation. No database repair is justified.

The user left20:49 and checked missing session around08:00 next morning. The export has no completion-write timestamp or prior displayed date/lifecycle/scroll state. The7:07PM screenshot was during the active visit and explains then-zero completed count, but does not explain the later observation. **Neither the precise historical cause nor a physical fix for that incident has been proven.** The user explicitly accepted this limitation; the two separately reproduced lifecycle defects remain protected by tests.

## Physical-device acceptance still pending

Install the normal1.6.6 update over the existing app without uninstalling or clearing data. Confirm saved settings/history/tasks remain, then perform a new qualifying Wi-Fi visit, including locked-screen time; leave and allow the existing90-second departure grace. Check the correct date in History, Session Report, View all sessions and Calendar. Confirm one completed visit appears and that an already-open report/list refreshes. Check Samsung/OEM permission/background/sound behavior on the phone. No battery measurement or new physical session test has been completed here.

## Deliverables and future handoff

StudyNest_1.6.6.apk (13,207,771bytes), StudyNest_1.6.6_APK.zip containing only that APK, complete StudyNest_1.6.6_source.zip, this report and SHA256SUMS_1.6.6.txt. APK SHA256 **20C2C3DF11694150FCAA7903362C94B8A6872E9D15ADBB336DCCA7A6B456B7C1**. All archive contents are checked; source ZIP is verified byte-for-byte against its final Git checkpoint.

Start any fresh conversation by reading **STUDYNEST_PROGRESS.md** at the project root. It records architecture, major features, minimum/session rules, History/Calendar, permission/background behavior, database/migrations, tests, package/signing/version, known limitations and the unresolved historical finding. Older through1.6.4 handoff and all1.6.6 checkpoint journals are preserved. The final source commit and artifact hashes/sizes are recorded in the external verification JSON/checksums/report copy; source archives exclude local SDK configuration, caches, build products and .git.

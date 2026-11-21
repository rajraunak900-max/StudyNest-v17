# StudyNest 1.6.6 — phone session diagnostic analysis, 4 October 2026

The user confirmed that the missing visit was the evening visit on 3 October, not the afternoon legacy record. The original JSON is preserved byte-for-byte under Verification_1.6.6/phone-session. SHA-256: D4595362E9AA234E94B613CBDF3F6B85FE207D9604324D2EE733F9C82711D0FE.

## Confirmed phone evidence

Samsung SM-A528B, Android 13/API33, Asia/Kolkata; snapshot 4 October 09:46:53 IST. Database schema7, Wi-Fi enabled, geofence disabled, tracking enabled, configured minimum10 minutes. Diagnostic exporter used read-only SQLite and did not run the normal engine, migrations or recovery.

Visit13 was saved with start1791033898404 (3 October18:54:58.404 IST), end1791040782624 (20:49:42.624), continuous duration6,884,220ms =1h54m44.220s, captured minimum10 and qualified=true. It is neither open nor unqualified. The automation snapshot is outside, with no pending departure. The record existed, completed and qualified before export; this snapshot does not establish exactly when the database completion write occurred.

There is one visit13, not a duplicate. Its timer identity is session8, whose separate tracking segments cover an earlier and later visit according to existing same-day automatic timer rules. Distinct library-visit IDs remain the basis of qualifying session cards, rather than the broad timer identity. The 20.583-second manual break points at visit13 and does not disqualify it. App distractions are stored separately; they do not reduce its qualification duration.

The other 3 October visits:10 at08:07–08:09 and11 at12:06–15:35 have legacy NULL minimum/qualification fields. Existing earlier-study-period presentation must preserve them. Visit12 at15:35–15:42 lasted7m32.156s with a10-minute minimum and is correctly unqualified; its time is retained in daily totals. None of these rows should replace/hide visit13.

## Query and presentation verification

FocusData DAO, VisitQualification, HistoryAnalytics and StudyPeriods are text-identical between stable1.6.5 and current1.6.6 (normalizing line endings). The production DAO selects all visits; qualification selects completed, persisted-qualified visits overlapping the chosen local date, deduplicates by visit ID and sorts by start. On 3 October it selects exactly[13]; on4 October it does not select13. It uses the continuous visit, not Actual Study, break-free time or foreground usage.

Two new native Compose/Room replay tests passed2/2 onAPI33 using the actual13 visits,8 timer identities and22 segments from the export. The fixture omits focus/app/break records and private identifiers because these tests verify session identity, filtering and navigation, not the user's exact focus analytics. The archived stable1.6.5 History implementation displays qualifying-library count1 and visit13 once after Calendar3October → View all sessions. The current1.6.6 displays Sessions:3 (visit13 plus two original earlier study periods), preserves the same card once, excludes shortvisit12 from qualifying cards, and opens exact Full visit01:54:44 / minimum10 details. No fixture records were repaired to achieve this.

The archived screen is test-only, with its entry point renamed and three equivalent non-null assertions required across the test-module boundary. Shared components run from the current project. This is not execution of the original stable APK or the user's past screen state, nor a physical-device test. Initial compilation diagnostics for that copied test screen are preserved; they were cross-module nullability adaptations, not production app failures. Final replay build/test succeeded in1m28s. Screenshots/raw XML/log are saved alongside analysis.json.

## What is resolved and what remains unknown

The reported evening visit was not lost in the saved database. In this snapshot, creation, finalization, qualification, persistence and the date predicate all succeed. No evidence supports repairing that row, changing its minimum, changing the schema, fabricating another session or altering history totals.

The original7:07PM screenshot precedes the18:54–20:49 visit's end. Its completed count0 is consistent with stable1.6.5 hiding active visits. That only explains the screenshot; it does not explain the user's subsequent report that the completed visit was still absent after leaving. Do not dismiss that later observation or claim it is disproven by a later snapshot.

Exact prior UI invisibility remains unconfirmed: the export contains no previous UI selected-date/scroll/lifecycle state, no timestamp of the completion write, and no history of display observations. A currently complete row cannot prove it was already complete when checked. Stale display or delayed closure are possibilities, not established causes. User was asked for approximate physical departure and last-check times to compare against20:49; answer pending at this checkpoint.

The two previously reproduced lifecycle defects (expired departure erased on delayed re-entry; recovery during departure grace creating a phantom visit) remain minimally fixed and tested in1.6.6. This JSON does not establish either as the cause of visit13's past invisibility. Preserve those fixes and the independently validated In progress presentation without inventing a root-cause claim.

## Preservation and next steps

No production code, APK, source schema, signing key, permissions, feature configuration or user history was modified during this analysis. Only test fixtures, two replay tests, a test-only archived screen and investigation/checkpoint documentation were added. All20 earlier release artifacts plus the diagnostic checksum artifacts were rehashed unchanged; stable1.6.5 and the diagnostic source repositories remain clean. Existing1.6.6 checkpoints are retained.

Previously466 distinct ordinary tests passed in the full run, plus9 cache confirmations of existing cases. These two new phone-replay cases bring validated distinct ordinary cases to468 across saved runs; a full468-case run has NOT been performed. Do not call this a new full-regression pass. There are no unresolved failures in the new two-test run.

Next incorporate the user's departure/check-time answer; preserve the confirmed completed row and investigate any actual reproducible visibility/lifecycle discrepancy without blind data repairs. Final release remains pending that investigation and final regression/lint/signing/packaging. Final normal1.6.6 must use versionCode15 or higher (temporary diagnostic14, stable16513), original package/certificate/schema, and install without uninstall/data clearing. The temporary diagnostic continues to pause normal tracking while installed; do not present it as the normal feature release. No final normal166 APK has been built here.


### Phone timing and refresh confirmation — 4 October

User left at20:49 on3October and last checked missing visit approximately08:00 on4October. Normal90-second grace cannot explain an overnight missing display; retain this real-device observation. Export later09:46containscompletedqualified13; it lacks completion-write timestamp/pastUIstate. Third native replay confirms archived165 open SessionInformation reacts correctly to Room completion of same visit13 without navigation/reopen. Final replay3/3pass in1m27s;469distinct ordinarycases acrosssavedfull466+3new, NOT full469run. Debuglint0errors/0fatal;14warnings. Compose lint misresolves non-Unit main-module types in archived test-only source; narrowly scoped RememberReturnType suppression there (productionlintunchanged), Kotlin/native testsusecorrectvalues. Initiallintreports retained. No production code/data repairs or APKchanges in ordinary166. Next prepare separate normal-tracking History test APK with read-only export of last rendereddate/sessionIDs forphoneverification; preservefirstplainreadonlydiagnostic/source/artifacts. This is temporarytest, NOT final166release; mainnormalversioncode must advance above temporarycode15 for final.

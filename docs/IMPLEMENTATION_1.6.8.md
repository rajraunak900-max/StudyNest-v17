# StudyNest 1.6.8 implementation and testing report

The signed 1.6.8 release passed all **539 automated and simulated tests**. Lint found **zero errors and 30 existing warnings**. The package and signing certificate match the previous release. Physical locked-screen sound, installation and battery testing remain for the user's phone.

## What caused the two problems

Library Time used the visit's continuous start-to-end span while the visit remained pending. Reconnection removed the departure timestamp, so the gap could no longer be excluded. This inflated Library Time, affected session qualification and daily totals, and reduced Efficiency while Actual Study was paused.

The fix records confirmed away intervals independently of visit identity. After 90 seconds without legitimate presence, Library Time and Actual Study freeze at the original disconnect cutoff. Efficiency also freezes. The visit remains pending for up to 30 minutes from departure. Returning within that limit resumes the same visit and timer, excludes the confirmed away interval from both totals, and restores library vibration. A brief disconnect under 90 seconds retains the established tolerance. Finalization uses legitimate accumulated presence rather than adding grace time. No artificial Break or Distraction is created.

The sound scheduler selected wake protection using its last deadline, now 30 minutes away, and therefore dropped protection for the earlier 90-second action. The fix protects the first short deadline with the existing official partial wake lock, bounded to at most 100 seconds including processing allowance. It releases the lock after confirmation, or on return, cancellation or failure. No lock is held throughout the 30-minute grace. The confirmation deadline also applies when study is manually paused but library presence still needs to freeze. Existing sound access, Normal-mode restoration and manual override rules remain intact.

The supplied silent video shows Library Time rising from 28:53 to 29:02 while Actual Study remains 15:11 and Efficiency falls from 52.6% to 52.3%. It does not show locked-screen sound restoration. That separate behaviour is the user's observation; the code regression is established, but its exact physical execution chain has not been traced.

## Testing and preservation

- Final full regression: **539/539 passed**, zero failures, errors or skips. All **519 previous test identities** are retained, plus 20 new checks.
- Focused checks: **54/54 passed**, followed by **116/116 completion checks** covering the final consumers and previously failing cases.
- Coverage includes 90-second confirmation and independent 30-minute grace; returns after 5, 10 and 29:59 minutes; repeated cycles; qualification; midnight allocation; History/Calendar/session visibility; breaks and monitoring; native frozen notification views on API 26/33/36; background scheduling without an Activity; wake cancellation/recovery; and manual timer, geofence and sound override interactions.
- Migration tests validate older schemas through schema 8, preserve every populated original table and column for schemas 5/6/7, retain settings, and verify persisted away intervals after database reopen. A battery guard confirms no INSERT, UPDATE or DELETE during pending-away ticks.
- Initial reproduction: five of seven checks failed on the unchanged accounting logic. The initial focused runs exposed test-only UTF-8 expectations and a concurrent trace collection, which were corrected. The first full run passed 524/535: seven plain JVM cases exposed unnecessary Android JSON for empty ranges; three migration fixtures still expected schema 7; one native test closed Room before its outgoing screen collectors had stopped. The pure no-gap path and schema expectations were corrected, and native test teardown now destroys the Activity and joins its jobs before closing Room. Raw results are retained; no failures were hidden.
- Signed build, APK CRC, APK signature, 16 KB alignment and manifest checks passed. The merged manifest is identical to 1.6.7 except for version. APK ZIP contains only the exact final APK; source ZIP matches all 228 tracked source files and their saved bytes.
- The clean 1.6.7 baseline and 31 earlier release files/documents are unchanged. Only departure accounting/scheduling and their direct consumers changed. Existing UI layouts, dependencies, resources, icons, permissions, settings, usage queries, service loops and location requests remain unchanged.

Room schema 8 adds only `awayRanges` and nullable `awaySince` to library visits through an explicit 7-to-8 migration. No rows were deleted, replaced or repaired. Existing history and qualification flags remain as recorded; past unknown gaps are not inferred. Package: **com.studynest.app**; versionName **1.6.8**; versionCode **18**; minimum API **26**; target API **36**. The original signer matches 1.6.7 and 1.6.5. Update identity is compatible; physical in-place installation and data preservation have not been tested by Codex.

## Thirteen release gates

1. Library Time freezes after confirmed departure: automated pass.
2. Actual Study pauses at the original loss cutoff: automated pass.
3. Efficiency stays frozen during temporary absence: automated pass; unknown measurements remain unknown.
4. The 30-minute grace controls continuity and finalization only: automated pass.
5. Reconnection continues the same visit and excludes confirmed away time: automated pass.
6. Sound has an independent background path without a UI or screen-wake dependency: automated pass; physical locked-screen acceptance pending.
7. No duplicate visit or timer identities: automated pass.
8. History, Calendar, qualification and daily totals: automated pass.
9. No destructive migration: additive schema and original values verified.
10. No unrelated intentional feature changes: source scope verified.
11. Full regression: 539/539 passed.
12. Package, certificate and update identity: verified; physical installation pending.
13. Battery architecture: no new polling, alarms, services, usage queries or location requests; pending ticks perform no writes; bounded short wake protection verified. Physical battery measurement pending.

## Limits and next steps

Android deep Doze can ignore partial wake locks and defer WorkManager. Manufacturer restrictions, process termination, delayed radio callbacks, unavailable SSIDs or denied sound access can delay or prevent actions. Exact 90-second execution under every power state is not guaranteed. No aggressive alarms, repeated wakeups, accessibility workaround or security bypass was added. [Official Doze guidance](https://developer.android.com/training/monitoring-device-state/doze-standby), [wake-lock release guidance](https://developer.android.com/develop/background-work/background-tasks/awake/wakelock/release).

Existing conservative process-death recovery is preserved. The October 3 evening visit exists, completed and qualified correctly; its original missing-display cause remains **unresolved**. These fixes do not prove that historical cause and do not repair its database.

Update without uninstalling or clearing data, then follow `docs/PHYSICAL_ACCEPTANCE_1.6.8.md`. Test sound while the screen remains locked, returns within grace, finalization after grace, preserved data and normal battery usage. Read `STUDYNEST_PROGRESS.md` for the full architecture and future handoff. All evidence is in `../Verification_1.6.8/validation.json` and the archived reports and logs.

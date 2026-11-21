# STUDYNEST 1.6.2 — PROFESSIONAL ONBOARDING, PERMISSION FLOW, UI ORGANIZATION & LIVE NOTIFICATION TIMER

Continue directly from the completed and working **StudyNest 1.6.1** project.

This is primarily a **UX, onboarding, navigation, screen organization and notification improvement update**.

StudyNest 1.6.1 is the authoritative working baseline.

## CRITICAL RULE

DO NOT rebuild the application from scratch.

DO NOT redo the Android 8–16 compatibility migration.

DO NOT unnecessarily rewrite working timer, Wi-Fi, library detection, UsageStats, database, sound/vibration, background-service or notification logic.

Preserve all existing user data and functionality.

---

# A. INSTALLATION / FIRST-RUN SETUP NEEDS PROFESSIONAL REDESIGN

The current first-run setup experience looks unfinished and confusing.

Some required Android permissions/system-access settings do not take the user directly to the appropriate Android settings page. The user may have to manually search through Android Settings to find the required authorization.

This is unacceptable for a polished consumer application.

Redesign the first-run/onboarding/setup experience.

The user should clearly understand:

1. What StudyNest needs.
2. Why it needs it.
3. Whether the permission/access is optional or required for a particular feature.
4. What feature will be unavailable if skipped.
5. Where to grant it.

---

# B. DIRECT NAVIGATION TO CORRECT ANDROID SETTINGS

Where Android provides an appropriate Intent/API, StudyNest should take the user **directly to the relevant settings/authorization screen** instead of simply opening generic Android Settings.

Examples include, where applicable:

- Notification permission
- Usage Access
- Do Not Disturb / Notification Policy Access
- Location permission
- Background location where legitimately required
- Battery optimization exemption/settings
- Wi-Fi/network-related settings
- Exact alarm access only if genuinely required
- Other special system access currently used by StudyNest

Use the most specific legitimate Android Intent available for the user's Android version.

Where Android/OEM restrictions make direct per-app navigation impossible, use the closest legitimate system page and provide concise instructions explaining what the user needs to select.

Do NOT pretend direct navigation is possible where Android does not expose it.

Do NOT use hidden APIs or hacks.

---

# C. RETURN-FROM-SETTINGS DETECTION

When StudyNest sends the user to an Android Settings page:

1. User grants the permission/access.
2. User returns to StudyNest.
3. StudyNest should automatically re-check the permission/access.
4. The UI should immediately update to:

**Granted ✓**

The user should NOT have to manually press another unnecessary verification button if the state can be detected automatically.

---

# D. SKIP BUTTON ON EVERY ONBOARDING/SETUP PAGE

Currently the Wi-Fi setup can effectively force the user to select a Wi-Fi network.

This must be changed.

A user might install StudyNest:

- at home
- while travelling
- without Wi-Fi
- before joining a library
- before knowing the library Wi-Fi
- while testing the application

Therefore, EVERY setup/onboarding step must provide an obvious:

**Skip**

or, where more appropriate:

**Set up later**

option.

This is especially important for:

- Wi-Fi setup
- library configuration
- location/geofence setup
- Usage Access
- notification/system access
- sound/vibration authorization
- optional monitoring configuration

No optional setup step should trap the user.

---

# E. SKIPPING MUST NOT BREAK THE APP

If a user skips Wi-Fi configuration:

StudyNest should still open normally.

Manual timer should remain available.

Tasks should remain available.

History should remain available.

Quotes should remain available.

Settings should remain accessible.

Other features that do not depend on Wi-Fi should continue functioning.

Features requiring library Wi-Fi should simply show an appropriate state such as:

**Library Wi-Fi not configured**

with a professional:

**Set up**

button.

Do NOT show repeated intrusive warnings.

---

# F. ONBOARDING PROGRESS

Create a clean onboarding flow with progress indication.

For example:

**2 of 5**

or an appropriate subtle progress indicator.

Each page should preferably contain:

- clear icon
- short title
- concise explanation
- primary action
- Skip / Set up later where applicable

Avoid large walls of technical text.

---

# G. PERMISSION & SYSTEM ACCESS — COMPLETE REORGANIZATION

The current **Permission & System Access** area looks messy and unprofessional.

Redesign it into a clean centralized screen.

Suggested title:

# Permissions & System Access

Suggested description:

"Manage the permissions StudyNest uses for automatic study tracking and library features."

---

# H. GROUP PERMISSIONS LOGICALLY

Do not display permissions as an unorganized technical list.

Group them into understandable categories.

For example:

## Study Tracking

Usage Access  
Used to identify selected distracting apps.

Status:

**Granted ✓**

or:

**Action required**

---

## Library Automation

Location  
Used for library/location-based automation.

Wi-Fi / Nearby device access  
Used where required for library Wi-Fi detection.

---

## Sound Automation

Do Not Disturb Access  
Used to manage vibration/sound behaviour where required.

---

## Background Reliability

Battery Optimization  
Helps StudyNest continue automatic library monitoring when the screen is off.

---

## Notifications

Notification Permission  
Displays active study-session information.

Use the actual permissions/features implemented in the app. Do not invent unnecessary permissions merely to fill these categories.

---

# I. PROFESSIONAL PERMISSION CARDS

Each permission/system-access item should have:

[Icon]

**Permission Name**

Short user-friendly explanation

Status:

✓ Granted

or

Action required

and an appropriate button:

**Grant**

**Open Settings**

or

**Manage**

Avoid displaying raw Android permission identifiers to normal users.

Do not show things like:

`android.permission.xxxxx`

unless inside a developer/debug area.

---

# J. PERMISSION SUMMARY

At the top of Permission & System Access, optionally provide a simple summary such as:

**4 of 5 configured**

or:

**All recommended access configured ✓**

Do not use alarming red warnings for optional permissions.

Clearly distinguish:

- Required for specific functionality
- Recommended
- Optional

---

# K. HOME SCREEN — NEW FINAL ORDER

Reorganize the Home screen again.

The exact desired order is now:

## 1. Today's Tracked Study Session

FIRST.

This should provide today's useful study summary.

Keep it compact and professional.

Then:

## 2. Study Timer

The interactive timer card.

Then:

## 3. Current Library Visit

Display:

Library Time  
Actual Study  
Distraction  
Efficiency

Then:

## 4. Motivational Quote

This should be the final major Home section.

Therefore the Home hierarchy must be:

**Today's Tracked Study Session**

↓

**Study Timer**

↓

**Current Library Visit**

↓

**Motivational Quote**

Do not duplicate the same statistics unnecessarily across these cards.

---

# L. HOME SCREEN VISUAL POLISH

Keep the cleaner StudyNest 1.6.1 visual style.

Improve where necessary:

- card spacing
- typography
- alignment
- padding
- visual hierarchy
- icon consistency

Avoid turning the Home page into a long analytics dashboard.

Important information should be understandable at a glance.

---

# M. HISTORY — EXACT SECTION ORDER

Reorganize the History screen.

The required order from TOP to BOTTOM is:

## 1. Line Chart

The line chart should appear FIRST.

↓

## 2. Today's Session Report

Today's consolidated study/library statistics.

↓

## 3. Week & Month Report

Weekly/monthly summaries and comparisons.

↓

## 4. Calendar Section

Calendar/history navigation.

↓

## 5. Session Information

Detailed session information should appear LAST.

Use exactly this general hierarchy.

---

# N. REMOVE "TRACKED STUDY OVERVIEW"

Remove the existing:

**Tracked Study Overview**

window/card/section from History.

Reason:

Too many sections currently display similar information, creating duplication and visual clutter.

Do NOT delete underlying study data.

Only remove this redundant presentation component.

Any unique information that exists only inside this component should be evaluated before removal. If genuinely important, integrate it naturally into one of the remaining History sections rather than duplicating an entire overview card.

---

# O. PRESERVE DAILY HISTORY CONSOLIDATION

StudyNest 1.6.1 already changed History to consolidate multiple library visits into one daily presentation.

PRESERVE THIS.

Do not bring back multiple large library-visit cards for the same day.

Underlying individual visit records must still remain intact.

---

# P. SETTINGS — PROFESSIONAL ORGANIZATION

Polish the Settings screen so it feels like a finished Android productivity application.

Organize settings into logical groups where appropriate, such as:

General

Library Automation

Study Tracking

App Monitoring

Notifications

Permissions & System Access

Appearance

About

Do not create unnecessary categories if the application does not need them.

Keep navigation simple.

---

# Q. PERMISSION & SYSTEM ACCESS PAGE

This is a high-priority Settings improvement.

It must no longer feel like a developer/debug screen.

Use:

- clean Material cards
- recognizable icons
- short descriptions
- clear permission state
- consistent action buttons
- logical grouping
- appropriate whitespace

Avoid technical jargon wherever possible.

---

# R. NOTIFICATION BAR — LIVE CURRENT SESSION TIMING

This is an important functional/UI update.

Currently Library Time and Actual Study Time shown in the persistent notification effectively update based on the approximately 15-second monitoring refresh.

This makes the notification appear delayed.

I want the persistent notification to display the **current running session timing**.

For example:

**StudyNest • Studying**

Library Time     01:24:37  
Actual Study     01:12:16

The displayed running time should feel current rather than visibly freezing for approximately 15 seconds.

---

# S. DO NOT SIMPLY POLL EVERYTHING EVERY SECOND

IMPORTANT BATTERY REQUIREMENT:

Do NOT solve the notification problem by performing expensive:

- UsageStats queries every second
- database queries every second
- Wi-Fi scans every second
- GPS/location requests every second
- foreground app checks every second

That would unnecessarily increase battery consumption.

Instead, design an efficient running-timer architecture.

Use stored timestamps/state to derive continuously progressing elapsed time where possible.

Keep heavier UsageStats/distraction reconciliation at an efficient interval.

---

# T. NOTIFICATION TIMER BEHAVIOUR

The notification should accurately represent the current session state.

### Normal studying

Library Time increases.

Actual Study Time increases.

### Selected distracting app active

Library Time continues.

Actual Study Time should stop increasing for the distraction period according to the existing App Monitoring logic.

### Manual pause

Actual Study Time follows the existing pause behaviour.

### Leaving library

Library Time stops according to the existing library departure rules.

### Session stopped

The notification should stop showing an actively increasing session.

Preserve existing session semantics.

---

# U. NOTIFICATION SYNCHRONIZATION

When the heavier monitoring process reconciles UsageStats approximately every existing interval, synchronize the timer baseline with the authoritative calculated values.

Avoid:

- double counting
- negative jumps
- large unexplained backward jumps
- counting distraction as study
- losing time when screen is locked

The notification display should be a responsive representation of the authoritative StudyNest session state.

---

# V. LOCKED SCREEN / BACKGROUND

The live notification timing must continue functioning appropriately when:

- StudyNest is not open
- another app is open
- the screen is locked

subject to legitimate Android/OEM restrictions.

Do NOT weaken the existing background reliability architecture.

---

# W. NOTIFICATION PRESENTATION

Make the notification concise and professional.

Prioritize:

StudyNest / current state

Library Time

Actual Study Time

Avoid filling the notification with excessive information.

If existing notification controls/actions are useful, preserve them.

---

# X. PRESERVE APP MONITORING 1.6.1 REDESIGN

Do NOT undo the professional App Monitoring changes from 1.6.1.

Preserve:

- Search
- app icons
- clean app rows
- selected-state indication
- Usage Access status
- dedicated Save Changes button
- persistent selections
- unsaved-change handling
- master App Monitoring toggle

Only refine further if genuinely necessary.

---

# Y. PRESERVE ALL WORKING FEATURES

Regression-test at minimum:

1. Manual timer
2. Automatic timer
3. Wi-Fi library detection
4. Location/geofence behaviour
5. Library Time
6. Actual Study Time
7. Distraction Time
8. Efficiency
9. App Monitoring
10. Save Changes
11. Usage Access
12. History
13. Daily History consolidation
14. Week/month statistics
15. Calendar
16. Streaks
17. Tasks
18. Quotes
19. Notifications
20. Vibration automation
21. Sound restoration
22. Locked-screen monitoring
23. Existing settings
24. Light theme
25. Dark theme
26. Android 8–16 compatibility

---

# Z. DATA SAFETY

Preserve all existing user data.

DO NOT clear:

- history
- library visits
- tasks
- streaks
- app selections
- Wi-Fi/library configuration
- settings
- statistics

Do not use destructive database migration.

If no database schema change is necessary, do not modify the schema merely for this UI update.

---

# VERSION

Treat this as:

**StudyNest 1.6.2**

Preserve:

- existing application/package ID
- signing certificate
- upgrade compatibility

The APK must install directly over StudyNest 1.6.1 without uninstalling it.

---

# CONTINUOUS SAVE / CHECKPOINT REQUIREMENT

This project may run for a long time.

DO NOT wait until everything is finished before saving progress.

Save/checkpoint after every stable major change.

Suggested checkpoints:

1. Preserve 1.6.1 baseline
2. Onboarding redesign
3. Skip/setup-later functionality
4. Direct system-settings navigation
5. Permission & System Access redesign
6. Home reorganization
7. History reorganization
8. Remove redundant Tracked Study Overview
9. Settings polish
10. Live notification timer architecture
11. Notification/background regression testing
12. Full regression tests
13. Final signed APK

Keep the project compilable/recoverable whenever practical.

---

# TEST FIRST-RUN EXPERIENCE

Before finalizing, specifically test scenarios such as:

### Scenario 1 — User has library Wi-Fi available

Install/open StudyNest.

Complete setup normally.

Configure Wi-Fi.

Grant appropriate access.

Reach Home successfully.

### Scenario 2 — User installs at home without library Wi-Fi

Install/open StudyNest.

Skip Wi-Fi configuration.

Skip optional permissions.

Reach Home successfully.

Manual Study Timer remains usable.

Later go to Settings and configure library Wi-Fi.

### Scenario 3 — Permission initially skipped

Skip Usage Access or another optional setup.

Use StudyNest normally where possible.

Later:

Settings → Permissions & System Access

Grant permission.

Return to StudyNest.

Status automatically changes to:

**Granted ✓**

### Scenario 4 — Reopening/upgrading existing installation

Upgrade from StudyNest 1.6.1.

Do NOT unnecessarily show first-run onboarding again to an existing configured user.

Preserve their settings and data.

---

# FINAL QUALITY CHECK

Before considering StudyNest 1.6.2 complete, inspect the app from the perspective of a normal user rather than a developer.

There should be no screen that requires the user to understand Android technical terminology unnecessarily.

There should be no optional onboarding page that traps the user.

There should be no generic Settings navigation where a more specific legitimate Android settings destination is available.

There should be no obvious duplicate analytics cards.

There should be no 15-second-looking frozen timer in the active-session notification if an efficient running-time representation can be implemented.

---

# FINAL OUTPUTS

After implementation:

- compile successfully
- run automated tests
- run lint/static checks
- fix regressions
- create final signed APK
- create updated source ZIP
- create implementation/testing report

Generate:

`StudyNest_1.6.2.apk`

`StudyNest_1.6.2_APK.zip`

`StudyNest_1.6.2_source.zip`

Place them in the same accessible `outputs` directory used for StudyNest 1.6.0 and 1.6.1.

Do not claim physical-device/OEM behaviour has been verified unless actually tested on a physical device.
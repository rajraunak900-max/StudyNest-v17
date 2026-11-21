# STUDYNEST 1.6.1 — UI POLISH, HOME SCREEN REORGANIZATION, HISTORY CLEANUP & APP MONITORING REDESIGN

Continue from the **current completed and working StudyNest 1.6.0 project**.

StudyNest 1.6.0 is already functioning correctly on my physical phone. The new Library Time, Actual Study Time, Distraction Time, Efficiency and App Monitoring features have been implemented.

This update is mainly a **UI/UX refinement and organization update**.

## VERY IMPORTANT

Do NOT rebuild the app from scratch.

Do NOT redo the Android compatibility migration.

Do NOT unnecessarily change any working business logic.

In particular, preserve the existing working:

- Automatic Wi-Fi study timer
- Manual timer
- Library presence detection
- Library Sitting Time
- Actual Study Time
- Distraction calculation
- Study Efficiency calculation
- App Usage monitoring logic
- Usage Access implementation
- Background monitoring
- Locked-screen behaviour
- Automatic vibration/sound restoration
- History database
- Streaks
- Tasks
- Quotes
- Notifications
- Existing settings
- Database migrations
- Android 8–16 compatibility
- Package ID
- Signing/update compatibility

The goal is to make StudyNest look **cleaner, more organized, more polished and more professional** without breaking its working functionality.

Use the existing StudyNest visual identity rather than completely redesigning the application.

---

# 1. HOME SCREEN — MOVE STUDY TIMER ABOVE CURRENT LIBRARY VISIT

The current Home screen displays:

Current Library Visit

above:

Study Timer

I want this order reversed.

The Home screen should show:

### Study Timer

FIRST.

Then directly below it:

### Current Library Visit

The preferred hierarchy is:

Home summary/header

↓

**Study Timer**

↓

**Current Library Visit**

↓

remaining Home content

The Study Timer is the main interactive element of StudyNest and should therefore have higher visual priority.

Do NOT change the actual timer functionality.

Only reorganize the layout.

---

# 2. CLEAN UP THE STUDY TIMER CARD

Keep the existing functionality, including:

- Manual / Automatic selection
- Current timer mode
- Wi-Fi studying status
- Running timer
- Pause
- Stop automatic / Stop session

However, visually refine the card.

Improve:

- spacing
- padding
- alignment
- button sizing
- typography hierarchy
- status presentation

The large timer should remain the primary focus.

Avoid unnecessary explanatory text directly inside the main card if the same information can be presented more cleanly.

Do NOT remove information that users genuinely need.

---

# 3. CURRENT LIBRARY VISIT CARD

Keep:

- Library Time
- Actual Study
- Distraction
- Efficiency

but make this section more compact and visually polished.

Instead of making it look like four plain text rows, consider a clean dashboard-style presentation.

For example:

**Current Library Visit**

Library Time        Actual Study  
00:11:04            00:06:57

Distraction         Efficiency  
00:04:07            62.8%

This is only a design direction. Use the layout that works best with the existing Compose/StudyNest architecture.

Important requirements:

- easy to read
- balanced spacing
- no visual clutter
- responsive on smaller phones
- readable in light and dark themes

The values should remain live.

---

# 4. REMOVE DEVELOPMENT-LIKE TEXT FROM MAIN UI

The current screen contains text similar to:

"Updates from Android usage timestamps about every 15 seconds. The original study timer remains available below."

This sounds like technical/developer information rather than normal user-facing application text.

Remove this from the main Home screen.

If this information genuinely needs to exist, move it to an appropriate informational/help area in Settings.

The Home screen should feel like a finished consumer application, not a development/testing build.

---

# 5. HISTORY SCREEN — REMOVE MULTIPLE LIBRARY VISIT CLUTTER

The current History section looks messy because it can display multiple separate library-visit records for the same day.

I do NOT want several library-visit cards cluttering the daily history.

For the normal History UI, show **ONE consolidated library summary per calendar day**.

Example:

## Today

Library Time  
6h 32m

Actual Study  
5h 14m

Distraction  
1h 18m

Efficiency  
80%

Even if StudyNest internally recorded several library visits during that day, the normal History screen should combine them into **one daily summary**.

---

# 6. IMPORTANT — DO NOT DELETE RAW LIBRARY VISITS

Do NOT solve the previous requirement by deleting database records.

Individual visits may still be useful internally.

Keep the underlying records intact.

The History UI should simply aggregate them for presentation.

For example:

Visit 1: 8:00–10:00  
Visit 2: 11:00–1:00  
Visit 3: 2:00–5:00

The normal History screen should display one combined daily result rather than three separate large visit cards.

---

# 7. DAILY HISTORY CALCULATIONS

When multiple library visits exist on the same day:

### Library Time
Sum all valid library-presence durations.

### Actual Study Time
Sum actual-study durations.

### Distraction Time
Sum distraction durations.

### Efficiency
Calculate from the aggregated totals:

Actual Study Time ÷ Total Library Time × 100

Do NOT simply average the percentages from individual visits.

Handle zero-duration cases safely.

---

# 8. HISTORY VISUAL REDESIGN

Clean up the overall History screen.

It should feel more like a professional productivity dashboard.

Prefer:

- clear date headings
- one daily summary
- strong hierarchy
- consistent cards
- appropriate whitespace
- readable statistics
- minimal clutter

Keep existing charts/statistics if useful, but improve their presentation where necessary.

Do not overload the screen.

---

# 9. APP MONITORING SCREEN — PROFESSIONAL REDESIGN

The existing App Monitoring feature works, but its current UI looks somewhat unfinished/unprofessional.

Redesign this screen while keeping the existing monitoring logic intact.

The page should have a clean title such as:

## App Monitoring

Below it, provide a short description:

"Select apps that should count as distractions during your study sessions."

Keep the wording concise and professional.

---

# 10. APP LIST DESIGN

Each application row should look polished and consistent.

Suggested structure:

[App Icon]  WhatsApp                [Toggle]

[App Icon]  Instagram               [Toggle]

[App Icon]  YouTube                 [Toggle]

Use:

- proper spacing
- aligned icons
- consistent icon sizes
- readable app names
- modern toggles/check controls
- adequate touch targets

Avoid making the page look like a raw Android package/application list.

Do NOT prominently expose package names such as:

`com.whatsapp`

unless there is a genuine need.

Normal users should primarily see:

**WhatsApp**

not:

**com.whatsapp**

---

# 11. SEARCH BAR

Retain the App Monitoring search function but improve its appearance.

Use a clean search field with:

- search icon
- placeholder such as "Search apps"
- clear/reset behaviour where appropriate

Search should remain fast and responsive.

---

# 12. SELECTED APP INDICATION

Make it immediately obvious which applications are currently selected as distraction apps.

The visual state should clearly differentiate:

Selected

versus

Not selected.

Do not rely on subtle differences that users may miss.

---

# 13. DEDICATED SAVE CHANGES BUTTON

This is an important new requirement.

Currently selections may be applied immediately.

I want a dedicated:

**Save Changes**

button.

The user should be able to:

1. Open App Monitoring.
2. Select/deselect multiple apps.
3. Review the selections.
4. Press **Save Changes**.
5. Only then commit those changes to the persistent monitoring configuration.

Use a prominent professional button near the bottom of the screen.

---

# 14. UNSAVED CHANGES

Before pressing Save Changes, modifications should remain temporary UI selections.

If the user leaves the screen without saving, do NOT silently commit those changes.

Prefer one of these professional behaviours:

- discard the unsaved selections automatically and retain the previously saved configuration

OR

- show a small confirmation dialog:

"Discard unsaved changes?"

Use whichever integrates best with the existing architecture.

Do NOT create annoying repeated dialogs.

---

# 15. SAVE CONFIRMATION

After pressing:

**Save Changes**

give subtle confirmation.

For example:

"App monitoring preferences saved."

Use a Snackbar/toast or equivalent Material UI feedback.

Do NOT use a large intrusive popup.

---

# 16. OPTIONAL SELECTED COUNT

Where visually appropriate, show something like:

**5 apps selected**

This helps users understand their monitoring configuration.

Do not add it if it creates clutter.

---

# 17. USAGE ACCESS STATUS

Present Usage Access permission status professionally.

Instead of technical text, show something like:

**Usage Access**

Granted ✓

or:

Permission required

[Grant Access]

Make the permission state immediately understandable.

Do not repeatedly ask for permission once it has already been granted.

---

# 18. MASTER APP MONITORING TOGGLE

Keep the existing App Monitoring ON/OFF control.

Make its relationship to selected apps clear.

For example:

**App Monitoring**       ON

"Pause actual study time when selected apps are being used."

If App Monitoring is OFF, do not delete the user's saved selected-app list.

When it is enabled again, previously saved selections should remain.

---

# 19. GENERAL UI POLISH THROUGHOUT STUDYNEST

Perform a careful UI polish pass across the existing application.

Do NOT completely redesign StudyNest.

Keep its existing visual identity, navigation and general theme.

Improve where appropriate:

- card spacing
- margins
- padding
- typography hierarchy
- alignment
- button consistency
- corner radius consistency
- icon alignment
- information density
- section headings
- visual balance
- light-theme readability
- dark-theme readability

The goal is:

**clean + calm + modern + student-focused + professional**

NOT:

flashy, over-designed or corporate.

---

# 20. CONSISTENT DESIGN SYSTEM

Use the existing Material/Compose architecture consistently.

Avoid having:

- different button styles for equivalent actions
- inconsistent card radii
- inconsistent spacing
- random text sizes
- mismatched icon sizes

Where practical, define/reuse consistent spacing and typography rather than adding arbitrary values throughout the application.

---

# 21. RESPONSIVE DESIGN

Test layouts on different Android screen sizes.

The UI must not depend on my exact phone dimensions.

Avoid:

- clipped buttons
- overflowing text
- overlapping cards
- cut-off statistics
- fixed widths that fail on smaller phones

Maintain Android 8–16 compatibility.

---

# 22. PRESERVE ACCESSIBILITY

Maintain appropriate:

- touch target sizes
- readable text
- contrast
- content descriptions for important icons
- scalable layouts

Do not make important information excessively small simply to fit more content.

---

# 23. DO NOT CHANGE WORKING CALCULATIONS

This UI update must NOT change the meaning of:

Library Time

Actual Study Time

Distraction Time

Efficiency

Keep the StudyNest 1.6 calculation architecture intact unless an actual bug is discovered.

---

# 24. DO NOT BREAK APP MONITORING

The redesigned App Monitoring screen must still use the existing working:

- Usage Access implementation
- package-based app identification
- distraction detection
- persistent storage
- filtering
- background calculations

The new Save Changes behaviour should sit cleanly on top of the existing logic.

---

# 25. PRESERVE EXISTING USER DATA

Do not clear:

- history
- library visits
- tasks
- streaks
- app selections
- settings
- library configuration
- statistics

If a database change becomes necessary, use a non-destructive migration.

Do not use destructive migration.

---

# 26. VERSION

Treat this as a refinement update to StudyNest 1.6.0.

Suggested version:

**StudyNest 1.6.1**

Preserve:

- package ID
- signing compatibility
- update compatibility

The resulting APK should install directly over StudyNest 1.6.0 without requiring uninstall.

---

# 27. REGRESSION TESTING

Before finalizing, verify that:

1. Manual timer still works.
2. Automatic Wi-Fi timer still works.
3. Library presence tracking still works.
4. Library Time still updates correctly.
5. Actual Study Time still updates correctly.
6. Distraction tracking still works.
7. Efficiency calculation remains correct.
8. App Monitoring still detects selected apps.
9. New Save Changes behaviour works.
10. Unsaved selections are not accidentally committed.
11. Saved selections persist after reopening the app.
12. Usage Access flow still works.
13. History correctly aggregates multiple same-day visits into one daily summary.
14. Raw visit records remain intact.
15. Existing historical data remains readable.
16. Vibration/sound automation remains unchanged.
17. Locked-screen/background behaviour remains unchanged.
18. Notifications remain functional.
19. Tasks and streaks remain functional.
20. Light and dark themes render correctly.

---

# 28. PHYSICAL-DEVICE LIMITATIONS

Do not claim that physical-device/OEM behaviour has been verified unless it actually has been tested on a physical phone.

Automated tests should verify what they reasonably can.

Clearly identify anything that still requires physical-device testing.

---

# 29. CONTINUOUS SAVE / CHECKPOINT REQUIREMENT

Do NOT wait until the entire update is finished before saving.

Save progress after each stable group of changes.

Suggested checkpoints:

Checkpoint 1 — StudyNest 1.6.0 baseline preserved

Checkpoint 2 — Home screen reordered/polished

Checkpoint 3 — Current Library Visit card redesigned

Checkpoint 4 — History aggregation implemented

Checkpoint 5 — History UI polished

Checkpoint 6 — App Monitoring UI redesigned

Checkpoint 7 — Save Changes behaviour implemented

Checkpoint 8 — General UI polish

Checkpoint 9 — Regression tests

Checkpoint 10 — Final signed build

Keep the project compilable/recoverable whenever practical.

---

# 30. FINAL OUTPUT

After completing the update:

- run automated tests
- run lint/static checks where appropriate
- resolve compilation errors
- generate a signed APK
- preserve the updated source
- generate a source ZIP
- provide an implementation/testing report

Preferred outputs:

`StudyNest_1.6.1.apk`

`StudyNest_1.6.1_source.zip`

If direct APK download through the interface is inconvenient, also create:

`StudyNest_1.6.1_APK.zip`

containing the APK.

Place all final outputs in the existing accessible `outputs` directory as done with StudyNest 1.6.0.

---

# FINAL UI PRIORITY

The most important visual hierarchy should now be:

**HOME**

Study Timer  
↓  
Current Library Visit  
↓  
Other dashboard information

**HISTORY**

One consolidated daily library summary  
instead of multiple separate library-visit cards

**APP MONITORING**

Professional app list  
+ search  
+ clear selected state  
+ Usage Access status  
+ dedicated **Save Changes** button

Overall, StudyNest should feel like a polished consumer productivity application rather than a development/testing interface.

Do not sacrifice existing functionality or reliability merely for visual changes.
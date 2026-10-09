# Approved Logic Locks

## LOCKED: Twelve Eyes campaign contract
- Scope: the twelve MAIN bosses, subordinate pools, global flags, Eye delivery/recovery, portal protection, visible advancement branches, and cartographer locators
- Protected behavior: exactly twelve pre-End MAIN bosses; zero to four native-first subordinate bosses; no extra subordinate gates; world-global prerequisite flags; Creative-compatible summon and kill verification; one distinct End Remastered Eye per MAIN boss; stable `jemcompat` resource IDs and `jemcompat_main_campaign` SavedData; all twelve MAIN bosses and all sixteen subordinate bosses have exact reusable locators sold through stable random selections by cartographers at levels one and two and by wandering traders; subordinate bosses without a registered native structure receive one non-overlapping saved encounter site and spawn there on approach; locator use creates or replaces one persistent localized Xaero waypoint and opens the centered map; only MAIN structures with subordinate prerequisites add a campaign lock to the shared invisible physical structure shell, derive unlock directly from all prerequisite flags, remove that lock immediately when complete, and display missing-boss feedback without teleporting or rewriting movement
- Allowed changes: installed-version compatibility, asynchronous on-demand locator/site execution that never blocks server ticks, and presentation fixes that preserve the contract
- Unlock condition: explicit user request to change campaign progression
- Created: 2026-09-17
- Updated: 2026-09-18 after the user required exact locators for every subordinate boss, non-overlapping generated encounter sites for naturally spawning targets, and structure-entry boundaries for gated MAIN bosses
- Updated: 2026-09-19 after fixing cached unlock desynchronization and roof-entry teleport loops without changing campaign progression
- Updated: 2026-09-19 after making all sixteen prerequisite flags recover from exact campaign/native advancement proof and fire immediately from redundant defeat signals, while one shared flag unlocks every duplicate MAIN structure at runtime for all players
- Updated: 2026-09-29 after the user replaced position-ejection boundaries with a shared physical shell and party-authorized passage

## LOCKED: World-global post-End reveal
- Scope: Ender Dragon completion detection, the per-player first portal-exit sequence, and mystery locator delivery
- Approved signal: "Start"
- Protected behavior: the world's first credited Ender Dragon defeat unlocks the sequence globally; every player sees it once on their own first later End-to-Overworld portal exit without needing a personal Dragon advancement; vanilla ending remains for twelve seconds, the glitch remains for nine seconds, and closing the scene leaves the player at the vanilla portal exit without an extra teleport
- Allowed changes: networking and compatibility fixes that preserve timing, per-player one-time behavior, and server authority
- Unlock condition: explicit user request to change the post-End sequence
- Created: 2026-09-21

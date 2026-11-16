# Night player QA: F6, resource packs and quests

## Scope

- Confirm that the current managed JEM resource-pack release is installed and enabled in `Test JustEnoughMods`
- Correct the F6 player preview outside and inside Epic Fight combat mode
- Replace hard-coded F6 key names with the current localized key bindings
- Restore native FTB Quests access on the dedicated local server
- Exercise the fixes against the ordinary local dedicated server and the `Test JustEnoughMods` profile

## Boundaries

- Source branch: `feature/f6-quests-runtime-repair`
- Source base: `develop` at `84443817eb6b53397a4c3e3b1c585e02f4601463`
- Stable `JustEnoughMods`, MineRENT and the release baseline stay unchanged during local QA
- Atlas changes were deferred by explicit user direction until every local runtime case passed
- Existing unrelated changes under `Server/config` are preserved and excluded from task commits

## Status

The local run completed successfully at 2026-11-17 04:35 +05:00. All requested local cases passed. The exact results are in `TIMELINE.md` and `SESSION_STATE.json`.

## Result

- Native managed generation `944e80eb061fe9e397420e7a5226817e314409f8` is active in `Test JustEnoughMods`
- `JEM Release v0.3.zip` is enabled in the active resource-pack list
- F6 player models remain visible and neutral outside and inside Epic Fight combat mode after world-camera direction changes
- F6 uses the localized names of the current `Up` and `Down` bindings
- Native FTB Quests opens on the local dedicated server and a concrete quest detail is accessible without a locked message
- The original ServerLogin users database was restored byte-for-byte after QA
- The final Atlas rebuild, audit, scan and validation completed successfully

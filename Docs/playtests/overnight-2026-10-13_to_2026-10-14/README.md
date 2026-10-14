# Overnight JEM player QA

**26 observed player cases: 16 PASS, 5 FAIL, 5 PARTIAL; 42 indexed findings/setup observations.** This is a critical bounded audit, not a modpack-wide acceptance pass. Desktop work ended early when the user pressed physical Escape at approximately07:10 local; no further desktop input was performed. The local report is finalized with explicit untested gaps. Run began23:51:43 local13October; confirmed activity is timestamped in the timeline.

The stock fix was actually replayed through native generation2: sale/purchase each consume one use; empty-input and affordable exhausted clicks mutate nothing. Coin supply conserved100,000. A separate prepared Human purchase minted an emerald without spending one; that resource bug remains open. Most profession perks are visible but unavailable, several core screens clip, and quest onboarding needs native synchronization diagnosis.

**Packaging qualification:** Before the full-artifact correction, JEM Server was older than source despite matching installed hashes. Independent Economy/Professions/Roles/Settlements observations remain bounded valid evidence; JEMServer-owned results are explicitly generation-qualified. A fresh full JAR then failed beforeworld because refmaps were omitted; repair/native replay is tracked independently. SHA parity alone is insufficient. MineRENT, original Server/world, immutable baseline and stable JustEnoughMods were not used as QA targets.

- [Findings](MASTER_FINDINGS.md) · [Prioritized actions](NEXT_ACTIONS.md)
- [Actual journey](PLAYER_JOURNEY.md) · [Playability](OVERALL_PLAYABILITY.md) · [Balance](BALANCE.md)
- [Environment and qualifications](ENVIRONMENT.md) · [Metadata](RUN_METADATA.json)
- [Registry scope](REGISTRY_COVERAGE/INDEX.md) · [Coverage](COVERAGE.json) · [Risk matrix](RISK_MATRIX.md)
- [Fixes](FIXES/applied.md) · [Before/after](FIXES/before-after.md) · [Independent review](EVIDENCE/traces/independent-review.md)
- [Timeline](TIMELINE.md) · [State](SESSION_STATE.json) · [Resume](RESUME.md)
- [Unapproved generated concepts](EVIDENCE/screenshots/concepts/README.md)

## System navigation

- [01-onboarding-and-multiplayer](SYSTEMS/01-onboarding-and-multiplayer/README.md)
- [02-economy-and-market](SYSTEMS/02-economy-and-market/README.md)
- [03-professions-and-races](SYSTEMS/03-professions-and-races/README.md)
- [04-weapons-armor-magic-and-combat](SYSTEMS/04-weapons-armor-magic-and-combat/README.md)
- [05-bosses-world-tier-and-progression](SYSTEMS/05-bosses-world-tier-and-progression/README.md)
- [06-settlements-villagers-and-workers](SYSTEMS/06-settlements-villagers-and-workers/README.md)
- [07-mountables-entities-and-ai](SYSTEMS/07-mountables-entities-and-ai/README.md)
- [08-blocks-biomes-worldgen-and-builds](SYSTEMS/08-blocks-biomes-worldgen-and-builds/README.md)
- [09-create-machinery-transport-and-native-mods](SYSTEMS/09-create-machinery-transport-and-native-mods/README.md)
- [10-ftb-quests-guides-and-events](SYSTEMS/10-ftb-quests-guides-and-events/README.md)
- [11-ui-visuals-shaders-and-audio](SYSTEMS/11-ui-visuals-shaders-and-audio/README.md)
- [12-home-inventory-persistence-and-network](SYSTEMS/12-home-inventory-persistence-and-network/README.md)
- [13-performance-crashes-and-compatibility](SYSTEMS/13-performance-crashes-and-compatibility/README.md)
- [14-other-legacy-and-cross-system](SYSTEMS/14-other-legacy-and-cross-system/README.md)

## Evidence scope

32,281 unique static registry-ID references are a coverage ledger, not32,281 played objects. Native runtime getter snapshots additionally inspect824canonical food stacks and56boss profiles. Exact visual/player samples and source-reviewed omissions are separate. Large raw captures/population JSONL remain local; the eventual compact Git snapshot is not a remote copy of every raw artifact. No second authenticated player, completed campaign, settlement catch-up or balance certification is invented.


Current full Server packaging is native-replayed: fresh and ordinary build maps present, clone startup, generation3 commit/relaunch/join and current non-op home missing-respawn rejection. A prepared bell scenario was not interacted with and is NOT_TESTED. Native raw mouse remains OFF after user desktop stop; restoration to originalON is pending in native settings. Bloop shader is already restoredON.


[Final cleanup proof](EVIDENCE/traces/final-cleanup.json): clone saved/exited normally, diagnostics and prepared bell removed, logging/daylight restored, heartbeat paused. The client stays under user control. The compact task-only Git snapshot is identified by the final assistant response; raw local evidence omissions are documented in [evidence ownership](EVIDENCE/README.md).

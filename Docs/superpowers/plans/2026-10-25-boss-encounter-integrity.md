# Boss encounter integrity implementation plan

User-approved specification: attached "Boss Barriers & Encounter Lifecycle: Final Integration Plan" and mandatory Party Outcomes & Reward Transaction Integrity acceptance criteria, 2026-10-25.

This is a correction of the existing owners. Canonical behavior remains documented in `Docs/events/BOSS_FIGHT.md` and `Docs/events/BOSS_RAID.md`. This plan records implementation and verification work, not a competing gameplay specification.

## Scope and permissions

- Work on current `main`, preserve local player/Chunky/cache changes, no branch switch or worktree
- Focused regression tests are explicitly requested for this task and override the older no-tests instruction within this scope
- Production build, ordinary Server artifact synchronization and native AutoModpack delivery follow standing project authorization
- No gameplay client launch, MineRENT deployment, stable-profile mutation, world deletion or release-baseline promotion
- Multiplayer acceptance remains unverified until an authorized multiplayer run actually occurs

## Work packages

1. Reward settlement: JEMRewards owns immutable batch persistence and claim idempotency, WorldBossTiers HostedEncounters owns frozen recipients/loot/attempt, pending settlement and durable-success-only terminal transitions. Regressions cover write failure, retries, duplicate callbacks, offline recipients, full inventories and Creative claims
2. Party and travel outcomes: JEMServer party/admission/travel/context owners validate the full Start snapshot and destinations, durably preserve return intent, roll back partial movement, enforce host/member outcomes and restore pending returns after login. Regressions cover late joins, irreversible elimination, stale tokens, partial travel failure and terminal recovery
3. Structural barriers and Raid: shared six-face server collision uses actual structure volume and full entity/vehicle boxes independently of client discovery. Preserve native combat and summons, remove authored Raid waves, apply safe size only at Start and unwind failed scaling. Regressions cover underground bounds, passage admission and native summon authorization
4. Canonical boss lifecycle: existing WorldTierData/RaidArenaApi/ArenaService/respawn scheduler retain one durable live identity, distinguish unknown from absent, reconcile retirement and replacement, preserve the exact ordinary 72,000-game-tick due time and keep Raid recovery independent
5. Integration: review cross-owner transitions and persistence acknowledgment, run the explicitly authorized regression suite, build only affected production artifacts, update Atlas history/effective state and owning docs, synchronize ordinary local targets, publish/apply native generation and commit/push exact task paths

## Acceptance record

For each invariant record source owner, executable regression result, artifact delivery and remaining multiplayer scenario separately. A compile or source inspection does not establish multiplayer acceptance. An initial failed write has no crash-durability guarantee until a subsequent durable acknowledgment succeeds.

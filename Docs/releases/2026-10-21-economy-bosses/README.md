# Economy, settlements and hosted boss release — 21 October 2026

This release combines the economy/races/settlements plan and all subsequent boss, official-marker travel and starter-supply additions. The destination is the existing MineRENT `world`. Its save directory, authentication, player inventories and progression are not imported from a local or backup world. The user explicitly canceled the copy-world runtime phase and requested direct application to the current world, with a backup only for recovery.

## Scope and owners

- CurrencyCore/Economy: backed pooled Coins, native stack merging and partial withdrawal, public exclusive Shipping sessions, account-bound recovery and ledger payout, five-denomination icon exchange selectors, deposit/withdraw/send, inventory bulk delivery and agreed sale prices. Standalone Buyer/wallet screens retire; native merchants remain. Shipping uses its existing 3D model and oak-plank destruction particles at one-third emission. The productive copper-mincer recipe exception remains by explicit user choice.
- Roles: native racial spells bypass learning only for their innate contract, guardian retaliation/cooldown and authoritative Jade race display. PackCore removes only Ancient Guardian from the Paladin prerequisite and retains the native End tower association.
- Settlements: ownership/overlap and resident membership protection, native homeward navigation/territory constraints, explicit Xaero expansion purchase and happiness-based daily town income. The user permits issuance for this income; starter20Gold and its funding are unchanged.
- Server/WorldBossTiers/TwelveEyes: barrier-free ordinary and raid arenas, persistent dormant AI/damage/presentation hold, attack-to-open existing Solo/Party admission, fixed accepted participant scaling and raid preparation before confirmation. Blood Moon and unrelated event boundaries remain. Official boss/event markers alone gain native Xaero teleport adapters with server-side eligibility and safe-arrival validation. `/death` and `/home all` use their existing state owners.
- StarterWelcome/StarterKitData: one durable entitlement per verified canonical account, including existing accounts:64cooked beef,10iron, full leather armor, native backpack and Nature's Compass. Full inventories retain undelivered items. Auth-locked or mandatory-race-unset players cannot receive damage or attack; protection ends as soon as both conditions are satisfied. Once-per-account delivery and cooked beef are Codex implementation choices announced during work.

## Boss duplication: established evidence and limits

The local existing save contains four Colossus entities with REVIVAL provenance, one legacy arena and identical coordinates, plus another Colossus associated with its canonical structure. This proves saved JEM revival duplicates locally. It does not prove the exact UUIDs or creation sequence of the three entities reported on MineRENT. Detailed UUIDs, coordinates and native-template evidence remain in [BOSS_FIGHT](../../events/BOSS_FIGHT.md).

The causal source defects were early Chunk Load/`hasChunkAt` checks before native entity storage was ready, loaded-only absence checks despite persisted active UUIDs, and parallel center-derived/structural arena identities. Raid restoration had the corresponding unresolved-identity risk. `HostedLobby.loaded()` also released a persisted hold and enabled AI on entity load.

WorldTierData now preserves canonical live/conflicting UUID claims and narrowly proven legacy aliases within SchemaVersion2. ArenaService and BossRespawnScheduler require full native entity readiness; an unloaded UUID is never interpreted as a dead boss. Spawn/raid replacement intent and claims are saved with disk acknowledgement before creation. Raid cleanup retains pending unresolved ownership and recovers after startup. Dormancy survives load without activating combat.

The exact installed Legendary Monsters2.2.2 templates each embed one boss. Mossy Temple uses35/24 chunks and Collapsed Kingdom110/80 in separate structure sets without a mutual exclusion zone. Those numbers do not guarantee separation between the two different structures. Existing structures and legitimate nearby bosses are not moved or deleted; density changes were not applied.

`/jem boss-audit list`, `plan`, `confirm` and `archive` provide loaded-only diagnostics and explicit exact-UUID repair. Repair excludes ambiguous/independent/admin/egg/multipart/active cases, durably archives full entity NBT before removal, preserves the keeper/reservation and replays only confirmed quarantined tombstones after interrupted saves. No actual quarantine or proximity-based deletion was performed during this release.

## Verification boundary

Production reobfuscated artifacts were built and synchronized to ordinary Server and ManagedPack. PackCore's incremental compilation omitted its Mixin refmap; packaging now declares generated outputs, compiles with the processor and refuses a missing refmap. Fresh and repeat production packaging succeeded.

Ordinary local dedicated startup completed for publication. Native generation53, token `eb4b76bda7a547d685430caba312445a27278f6e`, was published and fully applied through native AutoModpack to the closed `test jem` profile. All seven installed artifact hashes match the approved source outputs, with no pending transaction. The local development-world economy ledger remains outside its allowed development boundary; it was not changed or deployed.

The existing local Windows tax-persistence limitation also recurred at12:41:30 after publication: unchanged `TaxDurability` reported native replacement error5 for `jem_settlements`, stopped the local server and saved all dimensions at12:41:31; the shutdown watchdog subsequently recorded a timeout. This is not a clean local runtime acceptance and is not reported repaired. The earlier original-handoff receipt already records this unresolved Windows limitation. MineRENT's Linux runtime remains running; no local ledger/save was imported there.

No automated tests, gameplay/client launch or player combat acceptance ran. The isolated copy startup was canceled by the user after AutoModpack rejected its absent journal, before an active server was available; restart/unload/crash, Solo/Party, Epic Fight and individual mod-boss scenarios are **unrun**, not passing. Source fixes remain **in testing** until player approval. Atlas validation and native publication are separate from gameplay acceptance.

## Deployment receipt

Before changing hosted artifacts the server saved all dimensions and stopped. A fresh backup-only root archive `archive-2026-10-21T103924+0300.tar.zst` appeared at2.69GB. Archive integrity was not checked, as explicitly requested. It is not restored or used as the running world.

The final upload `jem-release-20261021-final.zip` is51807111bytes, SHA256 `5d344d7a6c077a7c63305571753039b4929c9bc0a310cbf2408b32b2db10539b`. It contains15entries: seven runtime JAR replacements, the same seven host-source JARs and the native manifest. There are no world files, server properties or deletion operations. The earlier uploaded `jem-update-20261021-economy-bosses.zip` remained unapplied and is superseded by this final upload.

MineRENT loaded `Preparing level "world"` and reached `Done` at12:48:18+05:00, with production test privileges disabled. Native preview showed exactly+0/~8/−0; publication completed at12:50:24+05:00 as ENTRY8, content prefix `eb4b76b`, matching local generation53. Startup also logged advancement-parent/optional Epic Fight compatibility warnings; these were not diagnosed or represented as fixed by this task.

The cumulative friend package is `JustEnoughMods-update-2026-10-21_12-33-20.zip`, SHA256 `c726afd57221c4b41efa0d986ca30b874fbf8217e93122b32a4cc43d1016096d`. It carries previous still-current payload plus current replacements and the complete current kubejs tree. Stable bootstrap synchronization has no drift; its managed installation remains owned by native AutoModpack. Baseline promotion completed at12:51:39+05:00 after hosted publication, without replacing the running world.

Atlas's current tracked projection validates103change records. Installed artifact drift is empty. The six preexisting source/index differences and sixteen unrelated untracked obsolete partitions were preserved separately from this delivery; validation used the tracked projection with those obsolete partitions temporarily isolated and restored. This does not resolve historical Atlas coverage gaps or the unrelated edits.

[Artifact identities](artifacts.json) describe the exact shared outputs; [changed files](changed-files.txt) enumerate task-owned source and documentation rather than unrelated runtime data. The [machine-readable receipt](release.json) records completed publication and package facts. The enclosing source commit is SELF and its actual remote SHA is reported after normal push.

![MineRENT native publication](minerent-published.png)

# Boss Fight

Current contract, 2026-10-25: server-authoritative dedicated encounter integration. This supersedes earlier source-only delivery labels and barrier-free descriptions below. Multiplayer acceptance remains separate from automated regressions, packaging and native installation. Exact evidence belongs to `encounter-integrity-delivery-20261025.json` beside this document.

WorldBossTiers owns boss profiles, canonical arena/live UUID, dormant combat, frozen attempts, ordinary cooldown and progression. JEMServer adapts parties, admission, travel and the six-face structure barrier. TwelveEyes owns Campaign prerequisites. JEMRewards is the only item-entitlement owner. HostedRewards retains existing non-item advancement/XP recovery.

| Logical phase | Existing owner and behavior |
| --- | --- |
| DORMANT | Native boss/support held, protected from damage, targeting, movement and custom combat ticks |
| PREPARING | Existing Solo/Party GUI, prerequisite and ready-member checks, one arena reservation |
| ACTIVE | Frozen accepted/ready/online UUIDs and scale, durable party link before native AI release |
| VICTORY | Legitimate death and attempt frozen, native loot captured once, durable settlement before terminal success |
| DEFEAT | Irreversible member elimination, retryable return and cleanup, same surviving ordinary boss restored dormant |
| RECOVERY | Existing journals reconcile incomplete writes, party links, native creation/retirement and pending returns |
| COOLDOWN | Ordinary respawn due exactly three Minecraft days after legitimate death, persistent across restart |

An eligible attack on a dormant boss opens the existing encounter interface without applying damage or starting combat. Explicit Start validates every member and full destination before movement. Origins persist before transfer. Failed admission or partial teleport rolls back moved members, retaining a pending return when rollback cannot complete. Mounted/passenger admission is rejected before travel rather than splitting a vehicle family.

The canonical Start operation persists one attempt UUID and frozen participant state in WorldTierData, then acknowledges the matching Server party/event record before releasing native combat. No late joining, ready-state filtering, eliminated reactivation or scaling recalculation occurs after Start. Stale party actions use the canonical attempt token, including retries using the same boss UUID.

Registered structure geometry supplies the shared translucent physical shell: four sides, floor and ceiling, native minimum/maximum Y with the existing horizontal margin. Server collision comes from saved canonical arenas rather than player discovery or renderer caches. Complete entity/vehicle boxes and swept movement guard corners and high speed. Authorized participants enter once, then containment remains solid during ACTIVE. Native player/mob teleports and portals receive authority checks. Blood Moon and Resource Rush retain their own boundary policies.

Native AI, phases, attacks and summons remain upstream-owned. Waiting ticks resume only after canonical Start and durable party recovery. Preserve original/current native NoAI and valid native targets, rather than imposing a nearest-player target every tick. Damage/control filters recognize frozen participants and proven owners without granting outsiders membership.

Actual death immediately records ELIMINATED plus NativeDeath, preserving normal respawn and existing inventory/armor/offhand/Curios retention inside an active structural encounter. Voluntary exit and disconnect elimination are different reasons and retain safe return information. Nonhost exit affects only that member. Token-valid host forfeit, or host absence beyond configured grace, fails the whole attempt. Death of the host alone is ordinary participant elimination. All members eliminated fails the attempt. The shared grace configuration defaults to five minutes, with explicit existing world overrides honored by both owners.

Ordinary victory offers Return/Stay only after durable settlement and safe terminal admission. Ordinary defeat prefers a validated configured home/bed where available, otherwise the preserved pre-fight origin. Offline living members keep pending returns until login. Player position persistence is verified before return metadata is cleared. NativeDeath never teleports a player back into the fight or replaces native respawn.

## Failure-safe personal settlement

1. Confirm legitimate death and current canonical attempt
2. Freeze eligible UUIDs and exact personal entitlements, preserving earned contribution/support even after elimination
3. Capture native loot once and serialize one immutable batch under boss UUID plus attempt UUID
4. Retain the frozen settlement in existing WorldTierData, independently of the removable boss entity
5. Persist and verify the complete JEMRewards batch and existing personal XP intent
6. Only then commit canonical victory, ordinary cooldown, party outcome and cleanup, with separate idempotent phase receipts
7. Deliver each recipient's complete personal copy independently through pending reward slots

Enqueue errors never call successful close. Only an actual uncanceled native death freezes the first victory time, and loot/XP capture requires that confirmed dying identity. A repeated death callback cannot move the frozen time. Retry uses the frozen roll and recipients, not a new loot evaluation. Per-source and per-recipient receipts survive duplicate callbacks/reconnects. Full inventories leave items pending. Claims remain take-only and server-authoritative, including Creative inventory synchronization. Native XP is suppressed only into the captured personal XP owner, avoiding simultaneous orbs plus direct grants. Revive, tanking, attributed useful buffs and actual attributed instant healing preserve genuine support credit, excluding self/proximity/idle awards.

An initial write that never reaches durable storage remains recoverable only in the running process. Logs explicitly identify this limit, and native autosave failure retains dirty intent rather than killing the recovery process. No crash-durability claim is made until verification succeeds.

The ordinary delay remains `72_000` Overworld game-time units. Duplicate callbacks preserve the original death ID/due time. A later legitimate death has its own new due time. Native saved-entity loading must finish before absence is usable. Pending creation stores the exact native UUID/NBT before insertion, writes through native entity storage with actual future acknowledgment, and replays only that never-released identity after loading. Known retired UUIDs are rejected on later joins so an older saved chunk cannot resurrect a moving defeated boss. Unknown UUIDs remain unresolved, never presumed dead.

Integrated/LAN ordinary bosses retain native combat and World Tier/Campaign rules. SMP, parties and Raid stay dedicated-only. No MineRENT deployment, stable-profile mutation or baseline promotion follows this integration.

## Verification scope

The current user explicitly requested focused regressions for this task, superseding the older blanket test restriction only within this scope. Run the opt-in suite from the project root:

```powershell
.\gradlew.bat -PjemEncounterRegression=true :JEMRewards:test :JEMWorldBossTiers:test :JEMServer:test --no-watch-fs
```

Ordinary production builds do not enable these tests. Regression results and exact artifact hashes belong to the delivery receipt. Native Mixin binding, mod-specific projectiles/teleports, multiplayer reconnects, mounted movement and real disk/process-loss scenarios remain runtime acceptance until actually observed. Compilation and mock-based tests do not establish multiplayer acceptance.

## Integration findings

| Issue and concrete cause | Correction | Source owners |
| --- | --- | --- |
| Failed enqueue still closed victory, leaving loot on a removable entity | Immutable independent settlement, verified personal batches before successful cleanup | HostedEncounters, HostedSettlement, RewardsData |
| Inventory transfer and reward removal could receive different persistence outcomes | Recoverable claim receipt, verified native player save, full-inventory pending and Creative synchronization | PendingRewardContainer, RewardsData |
| Separate Start commits and partial movement could release AI or lose origins too soon | Whole-roster preflight, durable origins, rollback and pre-release party/event acknowledgment | HostedParties, PartyTeleportFlow, SmpData |
| Reason-agnostic elimination could erase living offline returns or reopen participation | Monotonic elimination, distinct native death/absence, exact attempt tokens and verified pending returns | HostedEncounters, EncounterContext, PartyMember, EventMember |
| Host Raid forfeit reset the event while canonical combat remained active | Exact hosted close acknowledgment before replacement/reset, retry intent prevents recursive callbacks | HostedParties, EventScheduler, RaidEvent |
| Incomplete shell geometry and discovery caches could allow passage or block surface columns | Saved canonical bounds, six faces, swept complete bodies, vehicle families and scoped safe travel | StructureStaging, SolidBoundary, EventBoundaryRenderer, teleport Mixins |
| Native creation/removal could disagree with saved live claims or replay an older body | Exact native UUID/NBT intent, actual entity-write acknowledgment, retirement tombstones and alias migration | NativeBossStorage, WorldTierData, RaidArenaApi, EventHooks |
| Repeated death callbacks could move the first death time or reset revival | Actual dying identity required, frozen first death time and death-specific persistent due time | HostedEncounters, BossRespawnScheduler, WorldTierData |
| Authored Raid waves and stale/native effect provenance conflicted with native combat | Remove authored waves, preserve upstream combat, apply safe frozen scale once, bind proven effects to source/dimension/attempt | RaidEvent, HostedRaidScaling, NativeEncounterSource, HostedBossTickMixin |

The delivery receipt includes all executable test cases, the 26 still-unrun multiplayer scenarios, artifact/publication/application evidence and operational incidents. These findings remain in testing until player acceptance.

## Preserved exclusions

- Gear-based scaling and a manual per-boss Raid allowlist remain rejected.
- No automatic proximity start, hidden extra lives or permanent player lobby immunity.
- At most one approved JEM layer accompanies the native encounter; Raid uses the shared lifecycle rather than a separate arena owner.


## Duplicate-boss investigation and source repair —2026-10-21

Status: **in testing**. The following local evidence is established; isolated-copy runtime checks, explicit repair execution, fresh artifact delivery and player acceptance are not yet claimed. The user has renewed authorization to fix and then deploy; the earlier temporary MineRENT pause is revoked. This is authorization, not a deployment receipt.

### Observed local identities

The loaded local evidence contains four Overgrown Colossi at the same position, each with `REVIVAL` provenance and the same legacy arena identity `minecraft:overworld|legendary_monsters:overgrown_colossus|-6663|-1657`:

| Boss UUID | Position | Provenance |
| --- | --- | --- |
| `6c7d9fe1-54fd-401e-aa05-7ec51923f663` | -6676.5,147,-1735.5 | REVIVAL |
| `853b8266-9914-47d4-9dcb-f042c12fc09b` | -6676.5,147,-1735.5 | REVIVAL |
| `7d1efbf5-7451-4c1f-a623-4bff62f6007a` | -6676.5,147,-1735.5 | REVIVAL |
| `346b6858-0d4c-4113-8269-e2701f2029f7` | -6676.5,147,-1735.5 | REVIVAL |

A fifth Colossus, `0aa7348e-6110-4df1-bccf-8cb70d7b23b9`, is at -6662.5,151,-1656.5 with `REVIVAL` provenance and structural arena `892ea2fa-b840-30d7-8c5b-a8da55c229a1`. Paladin `89069b32-530c-4657-b96c-ff8d6063e57e` is separately observed at -6766.5,69,-1724.5 with `NATIVE` provenance and its legacy arena identity. A nearby different-type boss is not evidence of duplication. Loaded observations do not enumerate unloaded chunks or prove that no other duplicates exist.

### Native generation and density evidence

The inspected Legendary Monsters JAR manifest is2.2.2 forMC1.20.1, SHA-256 `8fc236e768acabe168c575f6ec76a3a587f0178a14f2706f6afa26dc470a69a8`. The exact native structure templates each contain one boss, not a repeated boss group. Native mossy-temple placement uses spacing35, separation24, salt12381234; collapsed-citadel placement uses spacing110, separation80, salt542863. These are independent structure sets with no cross-set `exclusion_zone`; no Sparse Structures override was found for these placements. Their observed native start chunks, (-415,-105) and (-425,-108), differ by10and3chunks. Same-set separation24/80 is not a mutual exclusion radius between different sets, so those nearby cross-type structures are compatible with the native definitions.

No density/worldgen change is implemented or authorized by this investigation. A future optional proposal could add explicit cross-set exclusion for newly generated chunks after separate approval; it would not remove existing structures or repair duplicated entities.

### Causal chain and owner correction

1. The repeated entities are JEM `REVIVAL` records, while the inspected native templates contain one boss each.
2. `ChunkEvent.Load` and `hasChunkAt` previously established terrain availability before native saved-entity storage finished loading. Startup also enqueued every unlocked arena even when a prior active UUID existed.
3. A loaded-only boss lookup could therefore return null during recovery, and that incomplete observation was treated as permission to spawn another replacement.
4. The same encounter could also persist under a center-derived legacy arena and a separately registered structure UUID. Those parallel identities/timers admitted independent replacement paths.
5. The shared arena lifecycle lacked one durable live-UUID authority coupled to complete native entity loading. The correction belongs to `WorldTierData`, `ArenaService`, `BossRespawnScheduler`, `RaidArenaApi` and the StructureStaging adapter, not to template density or per-boss deletion patches.

Current source adds a persisted canonical live-boss claim and source-backed legacy aliases. Only an exact legacy identity derived from its original center may alias to a same-profile/same-dimension verified structure containing that center. Historical records stay archived; canonical timers, unlock state, pending replacements and participation follow one identity without losing their evidence. Spawn/admission paths require full native entity-storage load acknowledgment and a synchronously saved/read-back UUID claim. An unresolved UUID remains unknown rather than being cleared by a null lookup. Native creation/revival and Raid cleanup reuse this authority instead of creating a second registry.

### Operator audit and explicit repair contract

`/jem boss-audit list [page]` is OP2, bounded and loaded-only. It reports UUID, type, canonical/saved/positional arena identity, dimension, position, provenance, canonical live claim, hosted/Raid state and spawn ownership. Registry identities unresolved in loaded entities are labelled unknown, not missing/dead. It never force-loads chunks or repairs automatically.

`/jem boss-audit plan <keeperUUID> <duplicateUUID>` requires two exact loaded same-type roots, a single-type registered profile, matching canonical arena/dimension and a verified native anchor. Keeper must be in the canonical structure. An out-of-structure candidate is eligible only with proven `REVIVAL` provenance and an actual persisted legacy alias to that same arena. Independent structures, unknown/foreign canonical claims, admin/egg/unverified provenance, multipart/vehicle families and active hosted/Raid/event/party ownership are excluded.

The same operator confirms the one-use plan within0.05Minecraft-day with `/jem boss-audit confirm <token> <keeperUUID> <duplicateUUID>`. It revalidates, preserves complete original entity NBT and prior canonical ownership in the existing `SmpData` table `boss_quarantine`, synchronously saves it and reads back the exact record. If necessary, canonical ownership transfers to the explicit keeper by compare-and-set and must be durably acknowledged before removal. `JEMQuarantinedDuplicate` then prevents the explicit discard from entering defeat/advancement/unlock/reward/drop handling. No world chunks, structures or valid progression are deleted. `/jem boss-audit archive <duplicateUUID>` inspects retained recovery metadata; it does not automatically respawn a duplicate. Failure keeps recovery evidence and requires a new explicit operator review rather than blind retry.

This section owns the investigation and expected source behavior. A later release/copy-execution receipt must separately establish what was actually run, quarantined, installed or deployed. User acceptance is still required before changing the defect status from `in testing` to `resolved`.

## Native support and hosting-role correction —2026-10-21

The user explicitly retained solo/party admission, dormant preparation and existing revive. A World Tier profile is no longer itself permission to open a hosted lobby: `BossProfile.hostedEncounter` owns that role independently from scaling, unique defeats and historical saves. Installed Legendary Monsters 2.2.2 declares only cloud_golem, the_obliterator and posessed_paladin in native forge:bosses; its expanded ordinary mob profiles now retain their progression/scaling without a lobby.

Paladin support uses the installed `legendary_monsters:possessed_armor_team` tag and the canonical registered structure, not every living entity in its zone. HostedGroup persists the support arena, resolves the existing canonical live-boss claim and reuses JEMHostedBoss for combat ownership. Loaded support shares protection/dormancy, Start and failed-attempt hold. It cannot open its own solo/party window. Success releases surviving support. Unknown/unloaded root identity is never absence proof or permission to spawn.

The user identified the second presence as a spirit. Installed bytecode identifies native SoulStrike as an owner-bound ThrowableProjectile rendered by the native `soul` renderer. The paladin constructs it with itself as the living owner. It has no independent hosted admission; owner resolution pauses its tick before Start, then preserves native combat ticks and presentation. This evidence does not claim an observed player session or a second registered living boss. Existing revive, unique defeats, native anchor and duplicate-repair controls remain intact.

Status: in testing; local next-update work only. MineRENT remains unchanged under the explicit user instruction.

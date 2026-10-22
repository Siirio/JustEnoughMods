# Boss Fight

## Universal ownership — 2026-10-22

The same logical-server encounter implementation serves integrated worlds and dedicated hosts. WorldBossTiers owns dormant native tick/presentation hold, canonical structure discovery, participation/scaling, rewards and revival. Without Server, its own solo admission opens on an attempted dormant-boss hit; with Server, the existing encounter/party UI owns that flow. Matching WorldTier protocol is 6. Server's scheduled/seasonal event switch does not gate ordinary boss Start or solo Raid. Strict TwelveEyes Start prerequisites remain independent from content-aware WorldTier progression; see the [module contracts](../universal/README.md). Runtime/player acceptance is still separate from source implementation.

## Current source contract — 2026-10-21

Status: source implementation / in testing. Runtime acceptance is unrun; artifact delivery, native AutoModpack installation and MineRENT deployment are pending.

The user explicitly removes boss barriers entirely. Ordinary structural Boss Fight, Campaign and Boss Raid no longer create visible walls, collision shells, movement correction or teleport containment. Registered structure geometry still owns arena identity, discovery, safe positions, progression and encounter/reward classification; it is not a physical prison. Blood Moon and Resource Rush retain their separate approved walls and movement policies.

Registered living bosses enter a persistent dormant lobby on entity join/reload. HostedLobby holds native AI/targets/movement and presentation until canonical Start and rejects damage/death of the held boss. The logical-server dispatcher hold also pauses mod-native custom combat/cinematic ticks. Reload reapplies the hold instead of releasing it, and there is no idle timeout that silently starts combat. Nearby players do not acquire general damage/death immunity merely by standing near a waiting boss.

An attempted player attack, including a resolved projectile owner, is cancelled against the dormant boss and opens the existing encounter choice/host GUI after current authority, proximity, prerequisite and availability checks. Proximity alone no longer prompts or starts combat. Start remains an explicit server-owned operation; it releases the lobby once and binds the canonical party/attempt. Cancellation/defeat of a surviving boss returns it to the same dormant owner. An empty active arena eventually closes through the existing grace lifecycle instead of remaining active forever.

Boss HP и damage увеличиваются на 20% за каждого дополнительного присутствующего участника. После этого к итоговому damage hosted boss применяется только один correction: 12% reduction для direct, projectile, effect и hardcoded damage. Gear score не участвует.

Смерть участника является окончательной для текущей попытки: JEM помечает его eliminated, но не отменяет `LivingDeathEvent`, не выдаёт скрытые дополнительные жизни, половину здоровья или post-death immunity и не переносит игрока к боссу. При фактической смерти внутри активной структурной арены обычный inventory, armor, offhand и Curios сохраняются на respawn без grave; это не отменяет elimination и не действует вне активного Boss Fight/Boss Raid. Групповое downed/revive состояние может существовать только до фактической смерти через установленный revive lifecycle.

Помеченные тематические bosses, включая Magnetron, Scylla и Cloud Golem, используют общий эффект «Любимчик Зевса»: раз в семь секунд позиция одного живого участника фиксируется компактным электрическим телеграфом на полсекунды, затем туда ударяет boss-owned молния. Игрок может увернуться, покинув отмеченную область до impact.

Verified native anchor записывается только при первом допустимом наблюдении native boss и после этого остаётся неизменным. Смерть, текущая позиция переместившегося босса, revive command и encounter cleanup не могут заменить его координатой смерти или передвинуть живую entity обратно к anchor.

Операторский revive является восстановлением структуры, а не rematch unlock: зарегистрированной структуры и отсутствия живого босса достаточно даже до первой победы. Не найденный среди загруженных entity UUID остаётся неизвестным/возможно выгруженным: это не доказательство смерти и не разрешение очистить canonical claim или создать замену. После доказанного освобождения identity первая последующая легитимная победа сохраняет право прогрессировать World Tier, а Raid остаётся закрытым до этой победы.

Start of an ordinary fight requires the existing legitimate live boss and does not spawn or move it. Safe arrival/exit validates full collision boxes, loaded geometry, world border and dimension, preferring sheltered in-structure floors over exposed roofs. With barriers removed, ordinary movement and teleport do not use a boss wall or arenaEntered lock. Other event restrictions and actual world collision remain authoritative.

Host cancellation is bound to Party ID and current attempt token; stale or replayed actions cannot close a newer attempt. Actual participant death is immediate elimination; inventory retention and progression remain owned by the active encounter lifecycle. Registered bosses retain their canonical shared arena/revival identity and native anchor.

Official discovered boss and active event markers may expose native Xaero teleport actions. Only server-projected object identities receive this adapter; the server resolves the boss arena/event ID anew, checks availability/eligibility and safe arrival, and never trusts client coordinates. Personal/home/base/death waypoints remain creator-private and retain native controls; forged or stale IDs fail. No replacement map screen is introduced.

Campaign prerequisites gate Start, not access to structure terrain. Per the user's correction, only ancient_guardian is removed from the posessed_paladin prerequisite list; bone_stalker and overgrown_colossus remain. Coppermincer and other progression edges remain. The registered ancient_guardian/ancient_tower_remains association is corrected to The End; this is not a fabricated defeat or worldgen retrofit. Legacy barrier registry IDs remain inert save compatibility and known legacy cells retain migration cleanup.

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

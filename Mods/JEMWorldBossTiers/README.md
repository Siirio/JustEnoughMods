# JEM World Boss Tiers

One logical-server owner for world progression, boss profiles/scaling, hosted encounters, native arena/revival identity and pending rewards. The production implementation supports integrated worlds and dedicated hosts; client presentation uses the same synchronized state. Required metadata: Forge `[47.4.10,)`, Minecraft `[1.20.1,1.21)`; builds target 1.20.1. Iron's Spells is optional behind its audited adapter. See the [complete domain inventory](../../Docs/universal/README.md).

## Progression and persistence

`WorldTierData` retains SchemaVersion 2, historical unique defeats, arena aliases/live-UUID claims, respawn queues and Raid participation. A fresh world captures actual registered qualifying bosses and a reachable threshold profile. Full content retains 0/3/10/18/32; reduced content scales the original thresholds proportionally, keeps them strictly increasing and omits unreachable tiers. The world persists that profile. Removing content later does not reduce its thresholds, remove historical proof or auto-award a tier. Existing saves without the new profile retain the original configured roster/thresholds. Very small fresh rosters can expose fewer than five reachable tiers.

`StructureEncounterApi` owns native structure associations at `data/jem_server/jem/events/structures/campaigns.json`, loaded-only discovery and the existing arena UUID derivation. It inspects current loaded chunks and already-loaded reference origins, then delegates registration/native-anchor proof to `RaidArenaApi`. Server uses this API rather than a second association map. Revivals and Raid cleanup retain one canonical living-boss claim and one verified native anchor; unknown/unloaded entities are not absence proof.

## Admission and combat

Registered hosted bosses and native support remain dormant until canonical Start. WorldTier owns the native tick pause, noAI/target/damage/presentation hold and reload recovery in both server environments. Without optional `jem_server`, a canceled dormant-boss hit opens its native solo admission screen. Start revalidates the expiring player token, exact living boss, proximity, player state and optional campaign requirements. Raid requires historical defeat, its canonical native structure, full player collision clearance inside it and no prior participation in that arena. With Server installed, its existing UI/party flow owns admission instead; no second prompt opens.

Preparation boost, participant scaling, actual-death elimination, contribution, rewards and normal-boss recovery reuse the existing hosted lifecycle. Reload restores orphaned native preparation or queues the same canonical replacement. No ordinary boss/Campaign/Raid containment wall is introduced. Scheduled/seasonal Events remain Server's separate dedicated opt-in domain. Matching client/server TierNetwork protocol is `6`; ID 0 remains TierSync and IDs 1/2 carry standalone admission.

Boss/reward data formerly in Pack Core now lives in this module. Public IDs and save names remain unchanged. Encounter behavior is documented in [Boss Fight](../../Docs/events/BOSS_FIGHT.md) and [Boss Raid](../../Docs/events/BOSS_RAID.md); source/build ownership is in [DEVELOPMENT](../../Docs/DEVELOPMENT.md). Runtime/gameplay acceptance is not established by these source contracts.

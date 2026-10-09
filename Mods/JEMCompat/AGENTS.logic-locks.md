# Approved Logic Locks

## LOCKED: Runtime Enhanced AI tier activation
- Scope: EnhancedAiTierController, EnhancedAiTierPolicy, and EnhancedAiSpawningMixin
- Approved signal: "enhancement AI works perfect"
- Protected behavior: Enhanced AI features unlock at their configured World Tier, scripted boss namespaces remain excluded, and already loaded ordinary mobs receive newly unlocked behavior without requiring a relog
- Reuse pattern: WorldTierChangedEvent updates feature state and performs one bounded loaded-mob reconciliation
- Allowed changes: additions outside this path that do not change unlock tiers, exclusions, or runtime reconciliation
- Unlock condition: explicit user request to change Enhanced AI progression or evidence of a regression in this exact workflow
- Created: 2026-09-16

## LOCKED: World Tier compatibility boundary
- Scope: JEMCompat Enhanced AI, boss mechanic, reward ability, revival, and arena-destruction adapters
- Approved signal: "Approved. The architecture is correct" and "Lock these additional requirements before coding"
- Protected behavior: adapters query JEMWorldBossTiers as the global authority; scripted bosses are excluded from Enhanced AI; rematches never progress the tier; block entities and protected functional blocks cannot be destroyed outside an eligible arena
- Reuse pattern: narrow mixins and optional adapters around installed-version behavior
- Allowed changes: installed-version compatibility fixes that preserve the core contract
- Unlock condition: explicit user request to change the compatibility boundary
- Created: 2026-09-15
- Updated: 2026-09-16 after the user explicitly moved MinerMobs, ItemDisruption, Pathfinding, MeleeAttacking, RandomStroll, and PearlerMobs to World Tier 3

## LOCKED: Eye and campaign provenance contract
- Scope: vanilla Eye of Ender throws, portal-frame insertion, campaign boss defeats, and campaign-eye rewards
- Approved signal: the user explicitly required vanilla eyes to locate strongholds from world start, legitimate spawn-egg boss kills to count, `/kill` not to count, and the campaign to be replaced with exactly twelve native-first MAIN bosses without old-world migration
- Protected behavior: vanilla eyes always locate strongholds; portal-frame insertion remains campaign-gated; survival encounters may reuse core encounter provenance, while spawn eggs, `/summon`, and `/kill` also qualify without checking the player's game mode; a nearby player receives each missing earned Eye even when command damage has no killer entity; the exact campaign roster and unique Eye mapping are owned by `CampaignBoss`; campaign state starts fresh in `jemcompat_main_campaign` and never reads the retired gate save
- Reuse pattern: End Remastered configuration plus JEMCompat frame protection and JEMWorldBossTiers encounter provenance
- Allowed changes: installed-version compatibility fixes that preserve these outcomes and native boss access paths
- Unlock condition: explicit user request to change eye access or legitimate encounter provenance
- Created: 2026-09-15
- Updated: 2026-09-17 after the user replaced the old campaign, explicitly declined old-world migration, and required unrestricted Creative verification through spawn eggs, `/summon`, and `/kill`
- Updated: 2026-09-17 after this complete contract moved unchanged into the standalone JEMTwelveEyes mod; JEMCompat no longer owns campaign runtime code

## LOCKED: Native-first subordinate boss network
- Scope: CampaignBoss prerequisites, CampaignPrerequisite, CampaignSavedData global flags, CampaignGateEvents, and the twelve visible campaign advancement branches
- Approved signal: the user explicitly rejected the one-sub-boss-per-MAIN model and supplied the exact thematic pools while forbidding any new gate around the sub-bosses
- Protected behavior: MAIN campaigns may require zero to four subordinate bosses; each subordinate remains directly accessible through native content and records one world-global defeat flag; Ancient Remnant requires Dune Sentinel, Mummy, and Golden Hermit King; Possessed Paladin requires Ancient Guardian, Bone Stalker, and Overgrown Colossus; Leviathan requires Ghost of Captain Cornelia and Sea Viper; Maledictus requires Skor, Frostbitten Golem, and Yeti; Luxtructosaurus requires Skeletosaurus; Ashlord requires Withered Abomination, Lava Eater, Piglin Executioner, and Furnace; the other six MAIN bosses use their substantial native progression without external prerequisites; summon, spawn-egg, and command kills count so every campaign milestone is Creative-testable
- Reuse pattern: exactly twelve direct branches from the campaign root; each branch is a visible linear sequence of zero to four subordinate defeats followed by its MAIN defeat, with exact missing-boss hints and no prerequisite items
- Allowed changes: installed-version compatibility fixes and player-facing text corrections that preserve the assignments and single-layer progression
- Unlock condition: explicit user request to change a sub-boss assignment, MAIN prerequisite, or native-first progression rule
- Created: 2026-09-17
- Updated: 2026-09-17 after the user supplied the final multi-boss pools and required unrestricted Creative verification
- Updated: 2026-09-17 after campaign ownership moved unchanged into JEMTwelveEyes

## LOCKED: Universal leash server behavior
- Scope: universal lead eligibility, native mob attachment, wall knots, item consumption/drop, and rope rendering
- Approved signal: the user explicitly removed player leashing and required native leash behavior for all valid creatures after arbitrary entities produced broken rope visuals
- Protected behavior: every `Mob` that refuses a vanilla lead may still be attached, including hostile and modded mobs; players, dropped items, projectiles, vehicles, decorations, and other non-mob entities cannot be attached; attachment, pulling, breaking, persistence, packets, and rendering use the native `Mob` leash implementation
- Reuse pattern: `UniversalLeashEvents` bypasses only the native eligibility decision, then calls `Mob.setLeashedTo`; walls reuse native leash knots
- Allowed changes: installed-version compatibility fixes that preserve native mob leash ownership and the player/non-mob exclusion
- Unlock condition: explicit user request to change universal leash behavior
- Created: 2026-09-16
- Updated: 2026-10-04 after the user replaced arbitrary-entity leashing with native mob-only behavior

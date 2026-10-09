param(
    [Parameter(Mandatory = $true)][string]$AuditDirectory,
    [Parameter(Mandatory = $true)][string]$OutputDirectory
)

$ErrorActionPreference = 'Stop'
[System.Threading.Thread]::CurrentThread.CurrentCulture = [System.Globalization.CultureInfo]::InvariantCulture
[System.Threading.Thread]::CurrentThread.CurrentUICulture = [System.Globalization.CultureInfo]::InvariantCulture
$projectDirectory = Split-Path -Parent $PSScriptRoot
$bossDataPath = Join-Path $projectDirectory 'src/main/resources/data/jem_world_boss_tiers/jem_world_boss_tiers/bosses/major_bosses.json'
$rewardDataPath = Join-Path $projectDirectory 'src/main/resources/data/jem_world_boss_tiers/jem_world_boss_tiers/rewards/major_rewards.json'
$bossData = Get-Content -Raw -LiteralPath $bossDataPath | ConvertFrom-Json
$rewardData = Get-Content -Raw -LiteralPath $rewardDataPath | ConvertFrom-Json
$auditRows = Import-Csv -LiteralPath (Join-Path $AuditDirectory '03_BOSS_MASTER_TABLE.csv')
$auditById = @{}
$auditRows | ForEach-Object { $auditById[$_.registry_id] = $_ }
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null

function Values($boss, $attribute) {
    $property = $boss.attributes.PSObject.Properties[$attribute]
    if ($null -eq $property) { return @('', '', '', '', '') }
    return @($property.Value)
}

function Adapter($boss) {
    switch ($boss.key) {
        'block_factorys_bosses:kraken' { return 'KrakenStaggerMixin' }
        'block_factorys_bosses:sandworm' { return 'SandwormStaggerMixin' }
        'aquamirae:captain_cornelia' { return 'CaptainCorneliaHealingMixin' }
        default { return 'EncounterScaler/data profile' }
    }
}

$matrix = foreach ($boss in $bossData.bosses) {
    $audit = $auditById[$boss.entity_ids[0]]
    $health = Values $boss 'minecraft:generic.max_health'
    $damage = Values $boss 'minecraft:generic.attack_damage'
    $special = @($boss.special_damage_multiplier)
    if ($special.Count -eq 0) { $special = @('', '', '', '', '') }
    [pscustomobject][ordered]@{
        boss_registry_id = $boss.entity_ids -join ';'
        boss_name = $boss.display_name
        native_hp = $audit.health
        native_damage = $audit.attack_damage
        native_threat = $boss.native_threat
        t1_hp = $health[0]
        t1_damage = $damage[0]
        t1_special_modifiers = $special[0]
        t2_hp = $health[1]
        t2_damage = $damage[1]
        t2_special_modifiers = $special[1]
        t3_hp = $health[2]
        t3_damage = $damage[2]
        t3_special_modifiers = $special[2]
        t4_hp = $health[3]
        t4_damage = $damage[3]
        t4_special_modifiers = $special[3]
        t5_hp = $health[4]
        t5_damage = $damage[4]
        t5_special_modifiers = $special[4]
        healing_changes = $boss.healing_adapter
        stagger_changes = $boss.stagger_adapter
        anti_cheese_changes = $boss.anti_cheese_profile
        revival_method = $boss.revival_strategy
        counts_toward_tier = $boss.counts_toward_world_tier
        scales_with_tier = $boss.scales_with_world_tier
        hp_scaling_safety = $boss.hp_scaling_safety
        compatibility_adapter_class = Adapter $boss
    }
}
$matrixPath = Join-Path $OutputDirectory 'WORLD_TIER_2_BALANCE_MATRIX.csv'
$matrix | Export-Csv -LiteralPath $matrixPath -NoTypeInformation -Encoding utf8

$qualifying = @($bossData.bosses | Where-Object counts_toward_world_tier)
$scaled = @($bossData.bosses | Where-Object scales_with_world_tier)
$explanation = [System.Collections.Generic.List[string]]::new()
$explanation.Add('# World Tier 2 balance explanation')
$explanation.Add('')
$explanation.Add('The curves are per-encounter targets derived from installed-version native health/damage, audit threat, phase safety, healing, pressure, mobility, caps, summons, and arena behavior. The special columns are design metadata for verified ability adapters; they are not a global outgoing-damage multiplier.')
$explanation.Add('')
$explanation.Add('T1 deliberately compresses strong bosses downward while leaving weaker fights near native. T2 restores strong encounters toward native and raises weak ones. T3 moves into native+ territory. T4 imposes the Cataclysm threat floor. T5 uses moderate stats plus retained mechanics rather than sponge scaling.')
$explanation.Add('')
foreach ($row in $matrix) {
    $boss = $bossData.bosses | Where-Object { $_.entity_ids[0] -eq ($row.boss_registry_id -split ';')[0] } | Select-Object -First 1
    $curve = "HP $($row.t1_hp)/$($row.t2_hp)/$($row.t3_hp)/$($row.t4_hp)/$($row.t5_hp); attack $($row.t1_damage)/$($row.t2_damage)/$($row.t3_damage)/$($row.t4_damage)/$($row.t5_damage)."
    $reason = if ([double]$boss.native_threat -ge 9) {
        'This is already a mechanically strong encounter, so T1 is meaningfully normalized and later tiers use restrained numerical growth while preserving its phases and native pressure systems.'
    } elseif ([double]$boss.native_threat -le 7) {
        'This encounter is below the late-tier threat floor, so its upper curve grows more aggressively to converge on Cataclysm-level durability and pressure.'
    } else {
        'This mid/high-mid encounter stays recognizable near T2, then receives measured durability and pressure growth to reach the T4 floor and a credible T5 rematch.'
    }
    $adapter = "HP safety is $($boss.hp_scaling_safety); healing adapter $($boss.healing_adapter); stagger adapter $($boss.stagger_adapter); anti-cheese $($boss.anti_cheese_profile)."
    $explanation.Add("## $($boss.display_name)")
    $explanation.Add('')
    $explanation.Add("$curve $reason $adapter")
    $explanation.Add('')
}
$explanation | Set-Content -LiteralPath (Join-Path $OutputDirectory 'WORLD_TIER_2_BALANCE_EXPLANATION.md') -Encoding utf8

$implementation = [System.Collections.Generic.List[string]]::new()
$implementation.Add('# World Tier 2 implementation report')
$implementation.Add('')
$implementation.Add('## Final progression contract')
$implementation.Add('')
$implementation.Add('- Thresholds: T1 0–2, T2 3–5, T3 6–9, T4 10–14, T5 15+ unique qualifying victories.')
$implementation.Add("- Qualifying roster: $($qualifying.Count) bosses. Cornelia is the seventeenth qualifying major boss.")
$implementation.Add('- State is global per world in overworld SavedData. Players never own an independent tier.')
$implementation.Add('- Encounters snapshot current global tier when created or activated, store it in persistent entity NBT, and never resnapshot during the fight.')
$implementation.Add('- Legitimate progression requires a qualifying profile, legitimate native/structure provenance, a non-rematch encounter, and at least one participating player. Generic /kill deaths are rejected.')
$implementation.Add('- Revivals snapshot current global tier, persist the rematch flag and arena UUID lock, drop current-tier rewards, and never count as a unique victory.')
$implementation.Add('')
$implementation.Add('## Tier-scaled bosses')
$implementation.Add('')
$scaled | ForEach-Object { $implementation.Add("- ``$($_.key)`` — $($_.display_name); counts=$($_.counts_toward_world_tier)") }
$implementation.Add('')
$implementation.Add('## Per-boss T1–T5 values')
$implementation.Add('')
$implementation.Add('| Boss | HP T1/T2/T3/T4/T5 | Attack T1/T2/T3/T4/T5 | Special profile T1/T2/T3/T4/T5 |')
$implementation.Add('|---|---:|---:|---:|')
foreach ($row in $matrix) {
    $implementation.Add("| $($row.boss_name) | $($row.t1_hp)/$($row.t2_hp)/$($row.t3_hp)/$($row.t4_hp)/$($row.t5_hp) | $($row.t1_damage)/$($row.t2_damage)/$($row.t3_damage)/$($row.t4_damage)/$($row.t5_damage) | $($row.t1_special_modifiers)/$($row.t2_special_modifiers)/$($row.t3_special_modifiers)/$($row.t4_special_modifiers)/$($row.t5_special_modifiers) |")
}
$implementation.Add('')
$implementation.Add('## Reward behavior')
$implementation.Add('')
$implementation.Add("$($rewardData.rewards.Count) reward profiles add stable, current-world-tier attribute contributions without deleting native, enchantment, or third-party modifiers. No acquisition tier is written to item NBT. The former global player-damage multiplier is absent. Infernal Forge shockwave scaling is ability-specific; unverified hard-coded abilities remain native rather than being guessed.")
$implementation.Add('')
$implementation.Add('## AI, arena safety, and performance')
$implementation.Add('')
$implementation.Add('Enhanced AI is off at T1 and enabled in reviewed stages from T2. Script-heavy namespaces are excluded at the Enhanced AI spawning gate. Loaded ordinary mobs are reconciled once per tier transition, with no tick scan. Boss explosion block lists are arena-gated; outside registered arenas they cannot break blocks, while block entities and protected storage/ore/functional blocks are preserved inside too.')
$implementation.Add('')
$implementation.Add('## Migration')
$implementation.Add('')
$implementation.Add('No migration path is included by explicit owner decision because all pre-redesign worlds will be deleted. Schema v2 accepts only fresh v2 state.')
$implementation.Add('')
$implementation.Add('## Verification boundary and unresolved risks')
$implementation.Add('')
$implementation.Add('- Gradle unit/static tests and release builds are verified separately from runtime playtesting.')
$implementation.Add('- Minecraft was not launched during this implementation; mixin application, native altar/respawner timing, simultaneous multiplayer fights, and tooltip rendering still require an in-game smoke test.')
$implementation.Add('- True Ending remains untouched and unenrolled; its function/score encounter cannot safely use entity-only scaling. Its native progression therefore remains intact, but it is not World-Tier-scaled.')
$implementation.Add('- Legendary Monsters and Cataclysm keep native damage caps, percentage-health damage, regeneration, and phase logic. No speculative cap rewrite was applied.')
$implementation.Add('- Explosion destruction is guarded centrally. Direct third-party destroyBlock calls that bypass Forge explosion hooks require runtime confirmation and targeted follow-up mixins if observed.')
$implementation | Set-Content -LiteralPath (Join-Path $OutputDirectory 'WORLD_TIER_2_IMPLEMENTATION_REPORT.md') -Encoding utf8

@'
# World Tier 2 compatibility report

## Installed-version patches

- Bosses'Rise 2.1.2 Kraken: `KrakenStaggerMixin` rebases the verified 100 recent-damage knockdown threshold against scaled maximum health, retaining the native 20% ratio.
- Bosses'Rise 2.1.2 Sandworm: `SandwormStaggerMixin` rebases the verified 25 recent-damage segment threshold against scaled maximum health, retaining the native one-sixth ratio.
- Aquamirae 7.1.13 Captain Cornelia: `CaptainCorneliaHealingMixin` rebases the installed 20 HP emergency-regeneration gate against scaled maximum health. Installed bytecode heals 0.1 HP/tick at that gate and applies vanilla regeneration during rage; neither amount is double-scaled.
- Cataclysm 3.31 Infernal Forge: `InfernalForgeAbilityMixin` scales only the verified EarthQuake damage calculation. Merely holding the item cannot affect another weapon or projectile.
- Enhanced AI 3.3.7.3: its feature flags are driven by global tier, loaded ordinary mobs are reconciled once on transition, and scripted namespaces are excluded inside Enhanced AI's own spawning gate.

## Conservative integrations

- Cataclysm bosses retain native phase gates, damage caps, token buckets, range attenuation, percentage-health damage, healing, lava absorption, and invulnerability logic. JEM changes selected base attributes only.
- Legendary Monsters retains native caps, adaptation, phase goals, projectiles, damaging blocks, and anti-cheese behavior. No second JEM cap is layered over it.
- Luminous major encounters scale but deliberately do not advance World Tier.
- True Ending, Companions, Methuselah, and Deep Dark Regrowth are not enrolled in the active profile data, so their scripted semantics remain untouched.

## Runtime risks

Mixin signatures were checked against the installed JAR bytecode and compile/refmap generation. Actual mixin application still needs a Minecraft smoke test. Native revival trigger ordering and direct block-destruction paths also need in-game observation.
'@ | Set-Content -LiteralPath (Join-Path $OutputDirectory 'WORLD_TIER_2_COMPATIBILITY_REPORT.md') -Encoding utf8

$revival = [System.Collections.Generic.List[string]]::new()
$revival.Add('# World Tier 2 revival table')
$revival.Add('')
$revival.Add('| Boss | Strategy | Arena strategy | Cost/trigger | Progresses tier |')
$revival.Add('|---|---|---|---|---:|')
foreach ($boss in $scaled) {
    $cost = if ($boss.revival_strategy -eq 'arena_offering') { "$($bossData.default_arena_revival_offering) + $($bossData.default_arena_revival_xp_levels) XP levels" } elseif ($boss.revival_strategy -eq 'none') { 'No JEM revival' } else { $boss.revival_strategy }
    $revival.Add("| $($boss.display_name) | $($boss.revival_strategy) | $($boss.arena_strategy) | $cost | No |")
}
$revival.Add('')
$revival.Add('Every created rematch stores the current global tier and rematch provenance on the entity. Arena offerings require an unlocked registered arena and one active encounter per arena. Native respawners, shell horn, and phoenix eggs remain owned by their source mods.')
$revival | Set-Content -LiteralPath (Join-Path $OutputDirectory 'WORLD_TIER_2_REVIVAL_TABLE.md') -Encoding utf8

@'
# World Tier 2 Enhanced AI table

| Tier | Newly available reviewed features | Result |
|---:|---|---|
| I | None | Enhanced AI escalation disabled; native/default mob behavior. |
| II | Targeting, Sprint, StuckFix, SkeletonShoot, PillagerShoot | Better pursuit and ranged fundamentals without griefing. |
| III | BlazeAttack, GhastShooting, ShulkerAttack, Jump, Climbing, AvoidExplosions, DrowningTargets, ThrowingWeb | Native+ ranged and anti-cheese pressure. |
| IV | Leaders, OpenDoors, Swimmers, TeleportAntiCheese, VehicleAntiCheese | Coordinated late-game pursuit with no destructive miner/item-disruption modules. |
| V | CreeperSwell, CreeperLaunch, ShulkerBullets, WitchPotionThrowing, AlliedMonsters | Maximum reviewed package; incompatible and destructive features remain disabled. |

Excluded namespaces: `cataclysm`, `block_factorys_bosses`, `legendary_monsters`, `aquamirae`, `luminous_beasts`, `luminous_nether`, `alexscaves`, `alexsmobs`, `companions`, `deep_dark_regrowth`, and `unearthed_journey`.

Pathfinding, MeleeAttacking, RandomStroll, MinerMobs, and ItemDisruption are deliberately not enabled. Their global or destructive behavior is not safe for this pack's scripted encounters and forever-world requirement.
'@ | Set-Content -LiteralPath (Join-Path $OutputDirectory 'WORLD_TIER_2_ENHANCED_AI_TABLE.md') -Encoding utf8

@'
# World Tier 2 migration notes

No migration is implemented.

The pack owner explicitly confirmed that no one has played this build and all existing worlds will be deleted. The redesign therefore uses a clean SavedData schema v2 and does not carry forward legacy defeated-boss sets or first-damage encounter snapshots.

Operational requirement: create a new world after installing these JARs. Do not reuse a world created with the earlier World Tier implementation.
'@ | Set-Content -LiteralPath (Join-Path $OutputDirectory 'WORLD_TIER_2_MIGRATION_NOTES.md') -Encoding utf8

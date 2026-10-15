# Power formulas

## Armor, accessories and durability Stage 3.5C

Vanilla armor reduction for incoming damage `D`, armor `A` and toughness `T`:

```text
effective armor = min(20, max(A / 5, A - 4 * D / (T + 8)))
after armor = D * (1 - effective armor / 25)
after Protection = after armor * (1 - min(20, EPF) * 0.04)
EHP = reference health * D / final lost HP
```

`bypasses_armor` skips the first reduction. `bypasses_enchantments` skips Protection. The two flags are independent; no shield, Resistance, absorption, iframe or school resistance is silently assumed.

For a complete four-piece material set, attributes are the exact sum of helmet/chestplate/leggings/boots getters. Conditional JEM Progression armor uses native values per enchanted piece: Tier I `×1.00`, Tier II `×1.05`, Tier III–V `×1.10`; boots separately add `+0/+0.15/+0.25` knockback resistance.

Standard armor wear per qualifying accepted hit is `max(1, floor(incoming damage / 4))` per equipped piece. Expected wear with Unbreaking level `n` is `wear × (0.60 + 0.40 / (n + 1))`; Mending repairs 2 durability per consumed XP point.

An exact constant-false `isDamageable(ItemStack)` override supersedes nominal material durability. Curios attribute modifiers retain their native operation (`ADDITION`, `MULTIPLY_BASE`, `MULTIPLY_TOTAL`) and are not flattened into armor unless their target attribute is exactly armor.

## Boss-reward Stage 3.5A

Обычный удар: `1 player base + exact main-hand attack_damage addition`, затем фактическая цепочка DamageSource/armor/Protection. JEM Progression применяется только если зачарование реально есть: `×1.00` на Tier I, `×1.05` на Tier II, `×1.10` на Tier III–V; на Tier III–V отдельная ветка с вероятностью 15% даёт ещё `×1.5`. Tier сам по себе оружие не усиливает.

Special cooldown window для безопасно известного burst upper bound `B` и cooldown `C` ticks:

`activations(T seconds) = 1 + floor((20T - 1) / C)`

`special-only upper DPS(T) = B × activations(T) / T`

Первое применение считается в `t=0`; ordinary attacks и способности с runtime-dependent hit count не добавляются. Поэтому эта таблица не является whole-weapon DPS.

Процентный Incinerator считается отдельно от обычной защиты: `6 + 0.02 × maxHP` на принятое flame-strike application. Его exact damage type находится в `bypasses_armor`, `bypasses_enchantments` и `bypasses_shield`; обычную vanilla mitigation table к нему применять нельзя.

These are composition contracts, not replacement mechanics. `?` means unresolved and never means zero.

## Boss combat Stage 3.5B

Absolute registered-boss melee starts from the profile's tier-specific attack attribute when present, otherwise the exact native attribute builder:

```text
effective owned melee = profile-or-native ATTACK_DAMAGE
                      * (1 + 0.15 * world tier)
                      * (non-Raid profile special, otherwise 1)
                      * hosted participant damage factor
                      * (Raid factor, otherwise 1)
                      * 0.88 hosted-owned factor
```

The same JEM outgoing chain composes after a native special expression; it does not replace percent-health or config terms. Exact Stage 3.5B special forms include:

```text
Ignis AreaAttack = attack_damage * damage_parameter
                 + target_max_health * hpdamage_parameter
Ignis Poke       = attack_damage + 0.10 * target_max_health
Ignis mounted    = 4 + 0.02 * target_max_health per accepted scheduled hit
Maledictus       = DMG * damage_parameter
                 + min(DMG * damage_parameter, target_max_health * hpdamage_parameter)
Leviathan bite   = attack_damage + 0.10 * target_max_health
```

Animation ticks and modulo schedules are attempted-application evidence only. They do not prove AI selection probability, target contact or iframe acceptance and therefore are not automatically converted to sustained DPS.

## General damage

```text
effective hit = native attack expression
              -> attack-specific native modifiers
              -> applicable JEM outgoing modifiers in event order
              -> native damage-type resistance/armor rules
              -> applicable JEM incoming modifiers
              -> caps/cancellation
```

Useful scenario measures:

```text
burst = sum(successful hits inside the burst window)
sustained DPS = total post-mitigation damage / elapsed seconds
effective HP against damage type x = max health / post-mitigation fraction(x)
resource efficiency = useful post-mitigation output / mana, cooldown, durability or economic cost
army output != unit output * count unless target access and AI uptime are both 100%
```

## Iron's Spells

For a registered player-cast spell against a non-player:

```text
event amount = native spell expression * world-tier factor
             + (1 if Human and native direct-hit conditions apply else 0)
tier factor = [0.65, 0.80, 0.95, 1.10, 1.20]
post-Iron amount = event amount * school-resistance soft-cap multiplier
```

The tier listener runs at HIGH priority and the Human listener at LOW priority, so the `+1` follows the tier multiplication. Against a player, both JEM adapters decline: tier factor is effectively `1.00` and Human `+1` is absent.

Exact installed Iron damage types are magic damage and are not added by Iron to `minecraft:bypasses_armor`. That does not prove how every other installed armor integration treats magic; the target's actual school/general resistance remains part of the scenario.

Priority beam candidates:

- `sunbeam`: `0.5 * spell_power`, instant delayed native hit.
- `ray_of_frost`: `3 + 1.5 * spell_power`, plus freeze buildup.
- `ray_of_siphoning`: `0.25 * spell_power` per successful native channel application, with native lifesteal equal to post-event damage.
- `electrocute`: `1 + 0.75 * spell_power` per successful native projectile/channel hit.

None of those four bytecode paths computes a percentage of target health.

## Progression weapons and armor

Eligible progression weapon outgoing hit:

```text
tier I: native * 1.00
tier II: native * 1.05
tier III-V: native * 1.10; independently 15% of qualifying hits then * 1.50
```

Eligible progression equipment attributes use native base values: Tier II weapons add `+1 damage`, `+0.25 attack speed`; Tier III+ add `+2`, `+0.40`. Armor adds 5%/10% of native defense and toughness. Boots add `+0.15/+0.25` knockback resistance.

Progression armor reductions use the largest reduction in full and every additional simultaneous reduction at 25% strength:

```text
combined multiplier = (1 - largest) * product(1 - secondary_i * 0.25)
```

Helmet projectile reduction is 15%/25%; Tier III has 20% full reflection/cancellation with reflected damage capped at 4. Chest heavy-hit reduction (incoming amount at least 8) is 10%/15%; Tier III guard adds 35% on a 240-tick cooldown. Boots reduce fall damage 35%/60%.

## World Tier enemies and bosses

Ordinary hostile max health and attack damage use `MULTIPLY_BASE` factors:

```text
tier I-II 1.00; tier III 1.10; tier IV 1.15; tier V 1.20
```

Registered scaling bosses use the native/profile attributes plus a `MULTIPLY_TOTAL` boss tier factor:

```text
boss tier factor = 1 + 0.15 * tier
```

Outgoing registered boss damage composes the same tier factor, optional profile special damage and hosted damage factor. Hosted boss-owned attacks finally multiply by `0.88`.

Hosted party factors before an optional raid multiplier:

| Players | Health | Damage |
|---:|---:|---:|
| 1 | 1.00 | 1.00 |
| 2 | 1.45 | 1.10 |
| 3 | 1.80 | 1.18 |
| 4 | 2.10 | 1.25 |
| >4 | +0.20 each | +0.05 each, cap 1.45 |

Default raid health and damage multipliers are both `2.5`, with profile overrides allowed. A boss below 30% health gains 15% movement and attack speed, not a direct damage multiplier.

## Event mobs

Blood Moon and raid mobs have their own dedicated JEM Server owner. Tier, wave, elite, archetype, player-count, penetration and set-piece factors can compose. The generated modifier/warning view names this as a high-stacking context; it must not be merged with ordinary-hostile or registered-boss scaling without checking their explicit exclusions.

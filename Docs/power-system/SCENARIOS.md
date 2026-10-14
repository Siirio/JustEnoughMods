# Combat scenarios

The canonical machine-readable definitions are `Catalog/views/power/scenarios.json`. Results remain symbolic where the exact ItemStack, attribute, moveset or native attack channel is absent. These are reference calculations, not automated gameplay tests.

| # | Scenario | Current expression/result |
|---:|---|---|
| 1 | Weak early player | `minecraft:stone_sword` and basic iron protection versus a native ordinary hostile at Tier I; native attributes unresolved per stack. |
| 2 | Mid-game player | `minecraft:diamond_sword`/diamond armor plus only the actual stack enchantments; enchant levels must be supplied. |
| 3 | Advanced player | Adds exact accessory and buff expressions; candidate names alone are insufficient. |
| 4 | Legitimate endgame | `minecraft:netherite_sword` and netherite defense with verified modifiers and target mitigation. |
| 5 | Endgame, no consumables | Same loadout with consumable effects omitted; exposes dependence on preparation. |
| 6 | Mixed two-player party | Hosted boss health `x1.45`, damage `x1.10`, before raid/profile factors. |
| 7 | Full four-player party | Hosted boss health `x2.10`, damage `x1.25`, before raid/profile factors. |
| 8 | Comparable PvP | Iron spell PvE tier adapter does not apply; compare actual post-mitigation burst/control. |
| 9 | Undergeared vs top PvP | Report burst, time-to-defeat, range and control uptime; do not reuse PvE conclusions. |
| 10 | One recruit/companion | Native attributes, AI and equipment; JEM roster/payroll does not invent combat power. |
| 11 | Mixed troops | Sum only source-resolved reachable attacks; include uptime and target access. |
| 12 | Funded army | Include acquisition/payroll, congestion, survival and command limits; no blind linear multiplier. |
| 13 | Iron's Spells mage | Example `irons_spellbooks:ray_of_siphoning`: native per-application expression -> tier factor -> conditional Human +1 -> resistance. |
| 14 | Epic Fight melee | Requires exact weapon moveset, animation timing, stamina and Nightfall/Epic Fight source evidence. |
| 15 | Heavy armor vs beam | Compare `sunbeam` and `ray_of_siphoning` with actual school/general resistance and hit count; neither inspected formula is percentage-health damage. |
| 16 | World Tier hosted raid boss | Native attack `* boss tier * special profile * party damage * raid 2.5 * hosted 0.88`; health uses corresponding factors. |

For any numerical comparison, record: exact IDs, stack data, tier, participants, PvE/PvP, attacker attributes, target health/armor/toughness/resistances, effects, distance, hit count/window, cooldown/mana and evidence level.

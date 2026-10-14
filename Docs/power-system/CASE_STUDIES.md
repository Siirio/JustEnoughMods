# Case studies

## Reported beam book

Observation: an apparently easy-to-obtain, unenchanted book/scroll removed about half the health of a player wearing Nightfall armor.

Source conclusion: the exact identity is not provable from the repository. Iron's Spells stores spell ID and level in the `irons_spellbooks:scroll` stack variant, while the installed loot tables randomize spell variants in chests and mob drops. The report lacks the observed stack's spell ID/level, caster spell-power attributes and target resistance state.

Strongest source-backed candidates are `irons_spellbooks:sunbeam`, `ray_of_frost`, `ray_of_siphoning` and `electrocute`. The first two use fixed single-hit expressions; the latter two are continuous and can accumulate several native applications. `ray_of_siphoning` also heals the caster for post-event damage. None of the inspected paths calculates target-health percentage.

No nerf was applied. A global spell-book reduction would affect unrelated spells and violate native ownership. The next decisive evidence is the localized spell name or stack spell ID/level from the reported item. Then compare actual hit count and post-resistance damage in scenario 15 and correct the native spell/config owner if the imbalance is confirmed.

## Before/after implementation decisions

| Exact ID | Original | JEM before | JEM after | Reason | Source | Verification |
|---|---|---|---|---|---|---|
| `irons_spellbooks:sunbeam` | `0.5 * spell_power` | Existing PvE tier/Human pipeline | No gameplay change; expression now traceable | Identity not unique; avoid speculative nerf | Exact 3.15.6 bytecode | Source-reviewed, runtime unverified |
| `irons_spellbooks:ray_of_siphoning` | `0.25 * spell_power` per native application; lifesteal 1.0 | Existing PvE tier/Human pipeline | No gameplay change; channel uncertainty/warning recorded | Strong aggregate-damage candidate but observed hit count absent | Exact 3.15.6 bytecode | Source-reviewed partial |
| Registered boss profiles | Native attack-specific | Tier/profile/hosted wrappers | No gameplay change; attack-channel gap is explicit | `ATTACK_DAMAGE` cannot represent every boss attack | Boss config and JEM source | Source-reviewed partial |
| Entire combat inventory | Scattered owners | Scattered Atlas records | Derived lookup, scenarios, coverage and deterministic warnings | Prevent future guesswork without duplicating runtime owners | Atlas + exact sources | Offline generation verified |

## Regression rules implemented

`Catalog/views/power/warnings.json` currently emits compact review findings for the unresolved beam identity and incomplete boss attack-channel coverage. It records exact candidates/context, native and JEM expressions, suspicion, evidence, confidence and suggested correction owner. It does not modify gameplay.

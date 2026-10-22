# Serene Seasons crop compatibility

## Ownership and policy

JEMCompat owns the pack additions under `src/main/resources/data/sereneseasons/tags/`.
Those native tag files are the editable crop policy; this document explains their
scope and exceptions. The Atlas change record links directly to the tag files as
source sets, rather than keeping a second editable crop registry.

The 2026-10-23 user request is to implement the supplied crop-compatibility
patch and coordinate with parallel handbook/Hint/Jade work. Seasonal windows and
the sunflower adapter follow that supplied technical direction. Existing native
and JEM windows are retained, except Farm & Charm tomato and lettuce now match
Kaleidoscope Cookery. These pack windows are not upstream balance claims.

This adds 34 reviewed crop/plant families and aligns two existing families. Across
the native JEM tag files, unique growth-block coverage rises from 23 to 76 and
tooltip-item coverage from 21 to 65: 53 additional block IDs and 44 additional
item IDs. Several IDs can belong to one crop's growth stages; these counts also
include explicit year-round entries, not only seasonal restrictions.

Native Serene Seasons 9.1.0.3 behavior remains authoritative:

- `seasonal_crops=true`, `out_of_season_crop_behavior=0`: an infertile crop's normal
  random tick proceeds with probability 1/6; existing crops are not killed or
  completely stopped by this pack change.
- Seasons apply to `minecraft:overworld`. Native underground protection below Y48
  without sky visibility, greenhouse glass 1вЂ“16 blocks above, blacklisted biomes,
  and dimension exemptions remain in force. An explicit infertile-biome rule has
  its native precedence; year-round membership is not an override for that rule.
- In a tropical-tagged biome, native fertility accepts summer-listed crops all
  year. Desert turnip therefore includes summer as well as spring/autumn. A cold
  biome can use winter membership independently of the displayed calendar season.
- Every addition uses `replace:false` and optional `required:false` members. Native
  seasonal and year-round memberships remain loaded. New winter files add the
  cold-adapted psycho berry without replacing native winter crops.
- Block tags govern natural growth. Matching item tags provide planting/seed
  tooltips; they cannot replace missing block coverage. Actual heads, bodies,
  saplings and productive leaves are included where their native behavior grows
  the crop. A same-ID block and item receive the same window.

## Existing coverage retained

| Provider | Existing crop policy |
| --- | --- |
| Vanilla / Serene Seasons | All native crop, sapling, mushroom and berry tags retained |
| Kaleidoscope Cookery | Tomato, chili and rice: summer/autumn; lettuce: spring/autumn; rice and wild rice retain their shared planting crop |
| Farm & Charm | Barley/corn: summer; oat/strawberry: spring; onion: spring/autumn; tomato and lettuce alignment below |
| Brewery | Both hops head/body: summer/autumn |
| HerbalBrews | Tea: spring/summer/autumn; coffee, rooibos and yerba mate: year-round |
| Vinery | Ordinary and taiga grape bushes: summer/autumn; jungle and savanna bushes: year-round |
| Snowy Spirit | Ginger: summer/autumn |

## Added or aligned families

IDs below are the actual registered growth blocks and real tooltip items. A blank
item column means that the native route shares another crop's seed; it does not
mean that a new seed item should be invented. The mature Spawn sunflower is an
adapter exception explained below.

| Provider / family | Growth blocks | Tooltip items | Fertile seasons |
| --- | --- | --- | --- |
| farm_and_charm / lettuce | `farm_and_charm:lettuce_crop` | `farm_and_charm:lettuce_seeds` | spring, autumn |
| farm_and_charm / tomato | `farm_and_charm:tomato_crop`, `farm_and_charm:tomato_crop_body` | `farm_and_charm:tomato_seeds` | summer, autumn |
| vinery / shared trellis grapes | `vinery:grapevine_stem` | `vinery:grapevine_stem` | year-round |
| vinery / apple orchard | `vinery:apple_tree_sapling`, `vinery:apple_leaves` | `vinery:apple_tree_sapling`, `vinery:apple_leaves` | summer, autumn |
| vinery / cherry orchard | `vinery:dark_cherry_sapling`, `vinery:dark_cherry_leaves` | `vinery:dark_cherry_sapling`, `vinery:dark_cherry_leaves` | spring, summer |
| beachparty / coconut palm | `beachparty:palm_sprout`, `beachparty:palm_leaves`, `beachparty:hanging_coconut` | `beachparty:palm_sprout`, `beachparty:palm_leaves` | summer |
| kaleidoscope_nether / poisonous fruit | `kaleidoscope_nether:poisonous_fruit` | `kaleidoscope_nether:poisonous_fruit` | year-round |
| kaleidoscope_nether / soul pepper | `kaleidoscope_nether:soul_pepper` | `kaleidoscope_nether:soul_pepper` | year-round |
| kaleidoscope_nether / warped fruit vines | `kaleidoscope_nether:twisting_cave_vines`, `kaleidoscope_nether:twisting_cave_vines_plant` | `kaleidoscope_nether:warped_fruit` | year-round |
| kaleidoscope_nether / crimson fruit vines | `kaleidoscope_nether:weeping_cave_vines`, `kaleidoscope_nether:weeping_cave_vines_plant` | `kaleidoscope_nether:crimson_fruit` | year-round |
| kaleidoscope_end / dream berry | `kaleidoscope_end:dream_berry_head`, `kaleidoscope_end:dream_berry_plant` | `kaleidoscope_end:dream_berry` | year-round |
| kaleidoscope_end / ender mint | `kaleidoscope_end:ender_mint` | `kaleidoscope_end:ender_mint` | year-round |
| primal / litchi | `primal:litchi_sapling`, `primal:litchi_tree` | `primal:litchi_seeds` | summer |
| primal / kiwano | `primal:kiwano_sapling`, `primal:kiwano_bulk` | `primal:kiwano_seeds` | summer |
| primal / starfruit | `primal:starfruit_sapling`, `primal:starfruit_tree` | `primal:starfruit_seeds` | summer |
| spawn / date palm | `spawn:date_sapling` | `spawn:date_seed` | summer |
| spawn / sunflower seed crop | `spawn:sunflower_plant` | `spawn:sunflower_seeds`, `spawn:sunflower` | summer, autumn |
| natures_spirit / desert turnip | `natures_spirit:desert_turnip_stem` | `natures_spirit:desert_turnip` | spring, summer, autumn |
| natures_spirit / olive | `natures_spirit:olive_sapling`, `natures_spirit:olive_branch` | `natures_spirit:olive_sapling`, `natures_spirit:olive_branch` | summer, autumn |
| natures_spirit / coconut | `natures_spirit:coconut`, `natures_spirit:coconut_sprout` | `natures_spirit:coconut`, `natures_spirit:coconut_sprout` | summer |
| natures_spirit / shiitake | `natures_spirit:shiitake_mushroom` | `natures_spirit:shiitake_mushroom` | year-round |
| blastfromthepast / psycho berry | `blastfromthepast:psycho_berry_sprout`, `blastfromthepast:psycho_berry_bush` | `blastfromthepast:psycho_berry`, `blastfromthepast:psycho_berry_bush` | spring, winter |
| netherexp / sorrowsquash | `netherexp:sorrowsquash_stem`, `netherexp:sorrowsquash_stem_plant`, `netherexp:sorrowsquash` | `netherexp:sorrowsquash` | year-round |
| netherexp / soul torchflower | `netherexp:soul_torchflower_crop` | Shared native seed; see exceptions | year-round |
| netherexp / cerebrage | `netherexp:cerebrage_skull` | `netherexp:cerebrage_seeds` | year-round |
| netherexp / warped wart | `netherexp:warped_wart` | `netherexp:warped_wart` | year-round |
| unearthed_journey / coconut palm and fruit | `unearthed_journey:cocos_nucifera`, `unearthed_journey:unripe_coconut` | `unearthed_journey:cocos_nucifera`, `unearthed_journey:unripe_coconut` | year-round |
| unearthed_journey / platypodium seed-pod tree | `unearthed_journey:platypodium_elegans_sapling_plant` | `unearthed_journey:platypodium_elegans_sapling_plant`, `unearthed_journey:platypodium_seed_pods` | year-round |
| oceanicrealms / giant kelp | `oceanicrealms:giant_kelp`, `oceanicrealms:giant_kelp_plant` | `oceanicrealms:giant_kelp` | year-round |
| alexscaves / pewen pine-nut tree | `alexscaves:pewen_sapling` | `alexscaves:pewen_sapling` | year-round |
| alexscaves / ancient tree-star food tree | `alexscaves:ancient_sapling` | `alexscaves:ancient_sapling` | year-round |
| alexscaves / thornwood cooking-ingredient tree | `alexscaves:thornwood_sapling` | `alexscaves:thornwood_sapling` | year-round |
| alexscaves / licoroot edible tree | `alexscaves:licoroot_sprout` | `alexscaves:licoroot_sprout` | year-round |
| alexscaves / fiddlehead food herb | `alexscaves:fiddlehead` | `alexscaves:fiddlehead` | year-round |
| deep_dark_regrowth / apatine | `deep_dark_regrowth:apatine_seeds`, `deep_dark_regrowth:apatine_stage_1`, `deep_dark_regrowth:apatine_plant` | `deep_dark_regrowth:apatine_seeds`, `deep_dark_regrowth:apatine_stage_1`, `deep_dark_regrowth:apatine_plant` | year-round |
| luminous_nether / golden_mushroom | `luminous_nether:golden_mushroom` | `luminous_nether:golden_mushroom` | year-round |

## Native growth adapters and deliberate exceptions

### Spawn sunflower

`spawn:sunflower_plant` is the ordinary tagged growth stage. Mature
`spawn:sunflower` is deliberately absent from seasonal **block** tags: its random
and scheduled ticks both advance sun tracking, and both can refill seeds at dawn.
Tagging the whole mature random tick would leave the scheduled route open while
slowing visual rotation.

`SpawnSunflowerSeasonsMixin` intercepts only a false-to-true seed transition in
`tryAdvanceRotation` and the mature `performBonemeal` route.
`SpawnSunflowerSeasons.allowSeedGrowth` delegates once to the native
`SeasonalCropGrowthHandler.onCropGrowth` using the juvenile's tagged state at the
actual mature flower position. No independent calendar, greenhouse rule, growth
multiplier, persistent state or scheduler is introduced. Denied natural refills
still advance rotation; already ripe seeds remain harvestable. A native removal
or replacement during the policy call is never overwritten by the old flower.
Mature bonemeal calls, including dispenser/direct calls to that native method,
receive the same single check.

The adapter reuses native fertility rules and configured growth probability;
fertilizer consumption and feedback stay with the original caller. An ordinary
valid bonemeal use still consumes one item when growth is denied. Unlike Serene
Seasons' cancelled player-interaction wrapper, this block-method gate can leave
normal success particles and a successful interaction result after denied growth.
It does not claim identical interaction feedback or impose player item handling
on dispenser/direct callers.

The adapter is loaded only with both `spawn` and `sereneseasons`. Its separate
required mixin configuration makes injection failures visible for an installed
contract. The compile-only Spawn/Serene Seasons files are existing pack mods,
not additional bundled runtime libraries. The juvenile is also protected by the
native `unbreakable_infertile_crops` tag, like other perennial growth entries.

### Other explicit exceptions

- **Vinery shared trellis:** `grapevine_stem` stores ordinary, taiga and savanna
  grape variants in one block ID. Native tags cannot distinguish its `GRAPE`
  property. It explicitly remains year-round, preserving its existing behavior
  and the savanna exception. Temperate grapes on this shared stem therefore still
  differ from seasonal bushes; no per-variety equivalence is claimed. Decorative
  `grapevine_leaves` does not produce fruit and is excluded.
- **Unearthed Journey:** Cocos and Platypodium saplings grow through native queued
  structure-placement callbacks; seed pods also queue a young tree. Their explicit
  year-round policy preserves prehistoric ecology and these timers, including
  transplantation. Tags do not intercept the callbacks. Unripe coconut itself
  enables random ticks; vanilla forwards `randomTick` to its overridden `tick`,
  so that natural ripening is inside the native random-tick route.
- **Shared mutation seeds:** Nether soul pepper reuses
  `kaleidoscope_cookery:chili_seed`; Nether Expansion reuses vanilla pumpkin and
  torchflower seeds. Those shared items retain their original terrestrial windows.
  They are not added to year-round item tags. The mutated blocks have their own
  year-round policy; the separate soul-pepper food item can show that policy.
- **Nature's Spirit coconut:** coconut-to-sprout propagation uses bonemeal, and
  the inspected sprout registration does not enable natural random ticks. The
  seasonal tags govern the native player bonemeal route; they do not add a new
  natural-growth mechanic.
- **Aquatic, cave and other-dimension plants:** giant kelp, cave crops, mushrooms,
  and Nether/End plants explicitly preserve year-round growth. Year-round tags
  also document their seed/item tooltips when transplanted.
- **Fruit leaves:** native seasonal gating cancels the whole random tick, so
  Vinery fruit leaves and Beachparty palm leaves can also decay more slowly out
  of season. Beachparty leaf coverage includes its bonemeal-created coconut.
  Newly covered perennial blocks use the native unbreakable tag so selecting
  destructive fertility later does not erase those plants.

Native Serene Seasons does not globally gate every mod fertilizer, block-entity
producer, scheduled callback, or direct growth call. Outside the specific sunflower
adapter above, this change claims native random-tick and native player-bonemeal
integration, not universal automation interception. It does not restrict harvesting
already ripe crops or change recipes, food values, drop counts, world generation,
calendar length, climate-biome classification, or the dimension whitelist.

## Reviewed exclusions

Static forage, stored produce, decorative flowers, potted blocks, ordinary timber
saplings and nonexistent legacy IDs are not turned into seasonal crops merely by
name. Reviewed examples include:

- Farm & Charm/Brewery/HerbalBrews wild forage and decorative flowers; Bakery has
  no independent living crop in the installed 2.0.6 roster.
- Nature's Spirit bearberries (decorative tall grass) and unregistered
  `young_coconut`; Spawn date leaves and static date bunches (the fruit route is
  seasonal tree planting).
- Deep Dark Regrowth galmium, infected mushrooms and exxo growth; Luminous pale
  melon and mushroom-cap blocks. The real Luminous growable is `golden_mushroom`.
- Oceanic Realms red algae (a coral-plant decoration), Unearthed Journey static
  Ganoderma, and Vinery juice-processing blocks.
- The reviewed Barnyard Buddies, Alex's Mobs, Critters n' Crawlers, Spud's Shops,
  VanillaBackport and Trek rosters did not add another cultivated food/fruit crop
  in this scope. For example, Alex's Mobs banana is a drop, not a banana crop.

## Evidence and delivery boundary

The original Atlas snapshot remains unchanged. Candidate IDs were checked against
release registrations and native source/bytecode; Atlas asset names alone were
not accepted as proof of registration. The [crop tag change record](../../Catalog/changes/records/compat-seasonal-crop-coverage-20261022.json)
and [sunflower adapter change record](../../Catalog/changes/records/compat-spawn-sunflower-seasons-20261022.json)
record source ownership, exact artifact references, before/after behavior and
indirect effects. The table above is an explanatory snapshot of the owning tags,
not a new machine-readable crop registry.

Serene Seasons semantics and the public callback signature were inspected at
[release source commit 61df18c](https://github.com/Glitchfiend/SereneSeasons/tree/61df18c2029d50e1294e7fc8d61824bbd6facb75):
[global random-tick hook](https://github.com/Glitchfiend/SereneSeasons/blob/61df18c2029d50e1294e7fc8d61824bbd6facb75/common/src/main/java/sereneseasons/mixin/MixinBlockStateBase.java),
[fertility and tooltip policy](https://github.com/Glitchfiend/SereneSeasons/blob/61df18c2029d50e1294e7fc8d61824bbd6facb75/common/src/main/java/sereneseasons/init/ModFertility.java),
[growth and bonemeal handler](https://github.com/Glitchfiend/SereneSeasons/blob/61df18c2029d50e1294e7fc8d61824bbd6facb75/common/src/main/java/sereneseasons/season/SeasonalCropGrowthHandler.java).
The exact installed 9.1.0.3 binary was inspected locally during integration. Its
SHA256 is `650c35858d335d45adea5445ffaed25ccd433f5cbfe4936ac2694b250177b674`;
Spawn 4.0.7 is `4c2385dcb8058ff6549a68a482d145d7212aa609574697b0812db4336d212fcf`.
Both match the Atlas evidence. Their bytecode confirms the public callback and
the native sunflower rotation/seed-write and bonemeal methods targeted here.

Release/source references used for crop registration and behavior:

- farm_and_charm 1.0.14: [binary](https://cdn.modrinth.com/data/HJetCzWo/versions/9fzY3YV6/letsdo-farm_and_charm-forge-1.0.14.jar); [sources](https://cdn.modrinth.com/data/HJetCzWo/versions/9fzY3YV6/letsdo-farm_and_charm-forge-1.0.14-sources.jar).
- vinery 1.4.41: [binary](https://cdn.modrinth.com/data/1DWmBJVA/versions/2I4cH49O/letsdo-vinery-forge-1.4.41.jar); [sources](https://cdn.modrinth.com/data/1DWmBJVA/versions/2I4cH49O/letsdo-vinery-forge-1.4.41-sources.jar).
- beachparty 2.0.3: [binary](https://cdn.modrinth.com/data/GyKzAh3l/versions/b3xwehb1/letsdo-beachparty-forge-2.0.3.jar).
- kaleidoscope_cookery 1.4.1-forge+mc1.20.1: [binary](https://cdn.modrinth.com/data/v17FatAc/versions/NXvhd04p/kaleidoscopecookery-1.4.1-forge%2Bmc1.20.1.jar); [sources](https://cdn.modrinth.com/data/v17FatAc/versions/NXvhd04p/kaleidoscope-cookery-1.4.1-forge%2Bmc1.20.1-sources.jar).
- kaleidoscope_nether 1.1.8-forge+mc1.20.1: [binary](https://cdn.modrinth.com/data/O4YuhMsI/versions/kn43NNqm/kaleidoscope_nether-1.1.8-forge%2Bmc1.20.1.jar).
- kaleidoscope_end 1.0.14-forge+mc1.20.1: [binary](https://cdn.modrinth.com/data/8GKUHBM6/versions/YS2tfy6v/kaleidoscope_end-1.0.14-forge%2Bmc1.20.1.jar).
- primal 1.1.6: [release/source](https://github.com/AngelDGr/Primal/blob/7826a74507166844f1be0c3e63cfb0dd1d618e5f/src/main/java/org/primal/registry/Primal_Blocks.java#L484-L542).
- spawn 4.0.7-1.20.1: [release/source](https://cdn.modrinth.com/data/rex9wwpz/versions/zR1peDoK/spawn-4.0.7-1.20.1.jar).
- natures_spirit 2.2.5-1.20.1: [release/source](https://mediafilez.forgecdn.net/files/6962/783/natures_spirit-2.2.5-1.20.1.jar).
- blastfromthepast 1.0.3: [release/source](https://github.com/ZigyTheBird/BlastFromThePast/blob/ce2714ea0be44cd7b2c8bacc611fe6ae7d0a9b68/src/main/java/team/recrafted/blastfromthepast/block/PsychoBerrySprout.java#L44-L66).
- netherexp 2.3.5: [release/source](https://github.com/ThatJadenXgamer/Jadens-Nether-Expansion/blob/674e1dccfc2159eda8d8bdfaef5758d073583726/src/main/java/net/jadenxgamer/netherexp/registry/block/JNEBlocks.java#L187-L200).
- unearthed_journey 2.2.3: [release/source](https://cdn.modrinth.com/data/SX3EgkYd/versions/O9xJ0NwG/unearthed_journey-2.2.3.jar).
- oceanicrealms 1.0.7: [release/source](https://mediafilez.forgecdn.net/files/7258/141/oceanicrealms-1.0.7-last-laster-ver.jar).
- alexscaves 2.0.2: [release/source](https://github.com/AlexModGuy/AlexsCaves/blob/4718f4287c65b810aecb211789b45f54d405a94d/src/main/java/com/github/alexmodguy/alexscaves/server/block/ACBlockRegistry.java#L165-L169).
- Deep Dark Regrowth 1.2.6.1: [binary](https://mediafilez.forgecdn.net/files/5308/579/Deep%20Dark%20Regrowth%201.2.6.1%20-%201.20.1.jar).
- Luminous Nether 1.3.3: [binary](https://cdn.modrinth.com/data/2CL6BDdJ/versions/b3lN4Qrs/Luminous%20Nether%20V1.3.3%20-%20Forge%201.20.1.jar).

Current production, Atlas and native AutoModpack delivery evidence is recorded
in [seasons-delivery.json](seasons-delivery.json). The original supplied notes
described an incomplete source-only checkout; those missing-artifact claims do
not describe this full local workspace. Gameplay acceptance remains pending.
No tests or gameplay client launches, MineRENT/stable changes or release-baseline
promotion are included in this implementation.

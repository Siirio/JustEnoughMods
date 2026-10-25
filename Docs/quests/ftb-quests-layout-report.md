# Adventure Atlas layout and preservation inventory

The supplied 2026-10-25 brief requests one native central index, genuine thematic subchapters, distinct content geometry and complete progress preservation. This report describes saved production data rather than in-game acceptance

| Inventory | Before | After | Preserved original identities |
| --- | ---: | ---: | ---: |
| Authored cards | 589 | 602 | 589 |
| Active objectives | 564 | 577 | 564 |
| Informational cards | 25 | 25 | 25 |
| Visible authored chapters | 32 | 39 | All original chapter IDs |
| Invisible legacy chapters | 7 | 7 | All source files unchanged |
| Native quests including archives | 606 | 619 | 606 |
| Native tasks | 742 | 755 | 742 |
| Native rewards including choice options | 2171 | 2197 | 2171 |
| Choice tables | 61 | 61 | 61 |
| Native dependency edges | 30 | 30 | 30 |

The immutable baseline is [baseline-20261025.json](baseline-20261025.json). The current machine-readable inventory is [layout-audit-20261025.json](layout-audit-20261025.json). Every original source card has an old/new chapter and coordinate row in [chapter-migration-20261025.csv](chapter-migration-20261025.csv)

Preservation comparison excludes presentation fields only. Nested tasks retain their actual detection configuration. Original bindings, objective conditions, recommended paths, rewards, claim keys, quantities, consume behavior and legacy hashes remain identical. No original identity is missing or duplicated, and no gameplay contract changes are reported

## Layout map

| Theme | Real content chapters | Authored pattern |
| --- | ---: | --- |
| Very Many Biomes | 8 | Irregular regional arms, continuing local branches and isolated discoveries |
| Acompany | 4 | Unequal care, livestock, mount, retained wildlife and aquatic tributaries |
| Cozy Home | 6 | Backplate islands with one to three cards, plus storage wing |
| Farm 'n Food | 4 | Crops and seasons, kitchens, tavern work and fishing |
| Cogs & Trains | 4 | Kinetic spine, factory sidings, supplies and railway operation |
| Secrets of Magic | 2 | Asymmetric spell-school and enchanting branches |
| Boss Busters | 5 | Preparation, hunting trails, encounters and event references |
| Power System | 1 | Existing choice and five race paths without quest-granted powers |
| Nightfall | 1 | Three native acquisition milestones and nine retained exercises |
| The Twelve Eyes | 3 | Twelve MAIN leaves, SUB preparation and actual post-ending arc |

The separate index contains ten native image portals in three unequal spatial groups, with a protected title center. Every content chapter returns to it. There are 170 supported image navigation links

The native dependency graph is acyclic with maximum retained rank four. Existing active source paths were recommendations rather than native gates. Geometry does not promote them to prerequisites. Bracket rails and gallery backplates have no progress semantics

The campaign has exactly twelve canonical MAIN leaves, six on each side. Corresponding eye icons come from canonical profiles. The existing final twelve-eye objective is the convergence. Six preparation links open genuine retained SUB objectives without fabricated rounds

Engineering retains 41 mechanical objectives and links three existing controls in other chapters. Approximately 93 percent of these destinations belong to the mechanical collection. No quest or reward is copied to draw the short interchange

## Added content and installed evidence

Installed backpack version is 3.26.14.2226, storage is 1.5.9.2197 and their core is 1.5.15.2427. Ten added targets cover the basic backpack, held backpack use, iron backpack, pickup upgrade, crafting upgrade, storage barrel, basic-to-iron upgrade, controller, linking tool and hopper upgrade

Exact installed recipes include `data/sophisticatedbackpacks/recipes/backpack.json`, `iron_backpack.json`, `pickup_upgrade.json`, `crafting_upgrade.json` and `data/sophisticatedstorage/recipes/oak_barrel.json`, `basic_to_iron_tier_upgrade.json`, `controller.json`, `storage_tool.json`, `hopper_upgrade.json`. The wooden barrel returns `sophisticatedstorage:barrel` with native wood-type NBT. The task accepts the actual barrel item/block across wood variants

Existing event adapters observe held-item use, placement and inventory acquisition. Upgrade possession is explicitly a preparation proxy. It does not claim to detect an installed capability, automatic transfer, connected network or worn backpack slot

Installed Nightfall 3.4.1 supplies acquisition advancements `efn:ruinsgreatsword`, `efn:yamato_dmc_in_sheath` and `efn:air_tachi`. All three files exist and use `inventory_changed` with criterion `obtain`. New native tasks use those exact hooks

Its complete entity registry contains effects, projectiles and owner-bound summons. Guardian and doppelganger are invulnerable tame summons. Native owner, lifetime and weapon conditions rule them out as challenge bosses. No false kill objective is introduced

## Exact native navigation facts

- `ftb-quests-forge-2001.4.22.jar`, `dev.ftb.mods.ftbquests.client.gui.quests.ChapterImageButton.onClicked` delegates to native `Widget.handleClick`
- `ftb-library-forge-2001.2.13.jar`, `dev.ftb.mods.ftblibrary.ui.Widget.handleClick(String)` splits the first colon into scheme and value
- `dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen.handleClick(String,String)` handles empty scheme plus `#` prefix through native lookup and `open(object,true)`
- `QuestObjectBase.getTitle`, `Quest.getSubtitle`, description parsing and chapter subtitle construction use `dev.ftb.mods.ftbquests.util.TextUtils.parseRawText`
- `ChapterImage.addHoverText` uses `TooltipList.translate`, so hover values are plain localization keys
- `ChapterImage.readData` reads integer RGB `color` and separate integer `alpha`. Image positions are centers in native quest units
- `QuestPanel.addWidgets` sorts image order. Captions still have interactive bounds, so those bounds remain separate from portals and quests
- Native fresh selection supports an images-only hub. Existing saved chapter, zoom and scroll remain respected

## Inventory results and limits

The current report records zero missing original IDs, changed original contracts, duplicates, cycles, broken links, unreachable visible chapters, quest collisions, title-zone intrusions, interactive-image overlaps, missing translation keys or literal implementation-provenance matches

The static Atlas item projection lacks three original base-game block-item rows: `minecraft:iron_ore`, `minecraft:crying_obsidian` and `minecraft:end_stone`. They remain explicit original coverage gaps rather than being silently replaced. No added objective has an unresolved Atlas target. A fresh live registry dump is not claimed

All six declared background PNGs exist at their final native paths and match the reviewed Gemini exports. The later companion sanctuary revision is included in the final Guide artifact. Original identity JPGs retain exact source bytes. Native images keep the 3072 by 2048 ratio and authored outer margins. Panning can still move beyond a finite PNG

Runtime acceptance remains unexercised. No game launch, normal/small GUI screenshots, saved-team opening, detection or reward claim was performed. Native API inspection and data preservation do not establish in-game navigation, visual readability or player acceptance

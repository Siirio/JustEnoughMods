# Item-help evidence and remaining scope

The authored source is `Mods/JEMAchievementGuide/src/main/handbook/items.json`. Its 56 bilingual entries cover summon components, all twelve campaign Eyes, Aquamirae relics, Alex's Caves research and summoning, Tide equipment, all six Simply Bows upgrade components, Backpacked storage and Exposure photography. Each materialized entry has one main-item spotlight and localized root `jem_summary` for the common tooltip/JEI reader. Recipes and gameplay are unchanged.

`items-coverage.json` records the exact evidence for every card. Of these 56 entries, 55 have their documented source chain implemented. `backpacked:unlock_token` has proven spending behavior, but no established survival acquisition route. This is authored-source coverage, not runtime or exhaustive pack-wide item coverage.

## Three difficult identities

`items-reference-evidence.json` records a direct symbolic-reference census across 286 current managed JARs, including class constants, recipes, loot, advancements and tags, and canonical module/pack sources. This bounds the result to the inspected artifacts; it cannot establish what additional live-world datapacks or dynamically constructed identifiers might do.

- `aquamirae:rune_of_the_storm`: Frozen Chest loot and the native obtain-rune advancement are confirmed. `RuneOfTheStormItem` supplies the foil presentation; registration makes it an epic relic. The inspected reference corpus contains no weapon-installation handler or consuming recipe. The player card describes the relic's acquisition and advancement, without promising a combat upgrade.
- `aquamirae:dead_sea_scroll`: Eel loot and plain item registration are confirmed. No activated interaction or consuming recipe appears in the inspected corpus. JEM's `cooking/world_ingredients` tag marks possible world-source leaves of an existing food-recipe graph; `CookingRecipes.describe` does not turn every tagged item into a food recipe. The card presents the scroll as an expedition trophy.
- `backpacked:unlock_token`: `UnlockableController` and `UnlockableContainerScreen` prove cursor-held token payment for native unlockable slots. Current configuration permits tokens for storage, while other slot classes have separate payment settings. No native recipe/loot or current FTB quest reward reference to this token was found. A price in `building_purchase_prices.json` alone does not prove a market offer: `MarketSupply.policy` admits that fallback only for an eligible building item.

All 15 native `data/backpacked/backpacked/*.json` model definitions were read. Their challenges unlock cosmetics. `UnlockManager.lambda$onServerTick$6` calls `BackpackManager.unlockBackpack`; its completion lambda updates the unlocked model set and sends `MessageUnlockBackpack`. This path does not grant an Unlock Token. Cosmetic challenges must not be advertised as its acquisition source.

## Separate quest owner mismatch

The current `aquamirae_06` card in `Mods/JEMAchievementGuide/tools/questbook/book.json`, materialized as `20.41 · Руна холодного оружия` in `Client/ManagedPack/config/ftbquests/quests/chapters/jem_20.snbt`, claims permanent weapon specialization and installation for cold terrain. That interaction is unsupported by the inspected installed Aquamirae handlers and reference corpus. Its actual native FTB Item task checks inventory possession. Its rewards are one Abyssal Upgrade Smithing Template, 24 Abyssal Amethyst and experience; they do not grant an Unlock Token. Quest files remain under the parallel FTB owner's scope and were not edited by this handbook work.

## Explicit limits

The pack-wide inventory of every survival item lacking a JEI recipe is still incomplete. Live-world datapacks, changed world-specific settings, runtime layout and player acceptance were not observed. No automated tests, game launch or build was performed for this content subtask. Existing native interfaces remain the route for crafting, unlocking and printing.

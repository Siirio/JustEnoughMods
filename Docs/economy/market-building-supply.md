# Public building supply and permanent NPC restrictions

Current source contract, 2026-10-16. The user requested implementation of the approved plan:
all wood/building variants, wool, glass, paintings and existing Building/Create and
Redstone category contents become purchasable; permanent exclusions override every
NPC sale; main Create is excluded from the public market while appropriate villagers
retain it. Ore exclusions mean ore blocks, not all ingots/gems/refined mineral blocks.

## Admission and ownership

`JEMCompat/catalog/JemCatalogCategories` owns the shared item-category decision used
by JEI and the dedicated server. `MarketSupply` consumes its public
`isMarketBuilding(ItemStack)` API, together with explicit vanilla wood/log/plank/sapling,
wool, Forge glass/glass-pane and painting selectors. Economy does not reimplement
category naming rules. Classifier or price-resource integration faults fail explicitly;
there is no unrelated item-price fallback. The required matching JEMCompat source
must be distributed with this Economy change.

The explicit acquisition-budget file is
`Mods/JEMEconomy/src/main/resources/data/jem_economy/economy/building_purchase_prices.json`.
It contains exact IDs from the saved installed registry capture, including candidates
which are not Building items. A price is **not admission**: shared category/material
membership and permanent restrictions are checked by the server. Unlisted future items
require a new authored budget. Existing commodity policies retain their two-way behavior
except prohibited public-market identities. New admissions are BUY_ONLY: the expansion
creates no NPC buying offer and cannot enter Shipping Box sales through that price file.

Codex selected initial acquisition budgets independently: retain authored canonical/native
prices; otherwise use material/decorative tiers of 12/16/24/32/128 Copper with a historical
captured ingredient-cost floor. These are initial product prices, not a claim that every
installed conversion has been proven free of arbitrage. The production authoring tool
`Mods/JEMEconomy/tools/author_building_supply.py` reproduces them with explicit
`--author-prices --capture <saved capture>`; it does not start a game or run tests.

Purchase quotas remain the existing persisted supplier-stock mechanism. New building
supplies use 256 units/account/day and 4096 units/server/day; existing commodities retain
their own caps. Purchases debit the existing backed Wallet/physical-Coin deposit path,
credit NPC_MARKET once and retain request replay protection and capacity preflight.
NPC payout quotas, stock/restocking and native offer ownership remain intact.

## Permanent rule

The single canonical version-controlled minimum is
`Mods/JEMEconomy/src/main/resources/data/jem_economy/economy/trade_restrictions.json`.
It is packaged into the mod and cannot be relaxed by a datapack policy reload.
`TRADE_RESTRICTIONS.csv` is its generated readable export, not a second owner.

- Exact items use installed translation/model/loaded-registration evidence, including
  `trials:heavy_core`, backported `minecraft:resin_clump`, Iron's
  `trial_spawner`/`vault`, and all three unfinished Skarrier stone golems.
- Ores use Forge item/block ore tags, the ore path suffixes and exact untagged Skarrier tangled/wrong deepslate ore identities, ancient debris
  and gilded blackstone identities. Refined ingots/gems are not selected.
- All trophy identities use trophy suffix/prefix selectors plus the native trophy
  button whose identity does not carry that suffix. Barrier identities are explicit.
- Spawn snail shells are prohibited; processed shell tiles/stairs/slabs remain eligible
  building goods. The installed Deep Dark door, door keyframe and door update items
  all have the exact loaded name “Unbreakable Door Keyframe”, so those three identities
  share the user's keyframe prohibition. Legacy translation aliases are guarded too.
- Namespace `create` is prohibited only in public market/Buyer/Shipping Box channels.
  Addon namespaces remain governed by shared categories. Existing profession specialties
  still include Create stonework, goggles, metal sheets, clipboards and schematics.

Permanent exclusions are applied when extending NPC offers, at quotation, when choosing
native barter/result slots, and before monetary settlement. Old saved offers and reverse
companions cannot deliver prohibited goods. Their removal does not reroll saved profession
choices or replace native merchant screens, ordinary restock, prices/discount mechanisms,
item counts or permitted NBT. The rule blocks NPC *sale outputs*; it does not delete player
items or restrict gifts/player shops/player barter.

## Paintings and presentation

Every currently registered painting variant receives its own offer. The offer ID is
`jem_economy:painting/<variant namespace>/<variant path>`. The server validates that alias
against `Registries.PAINTING_VARIANT` and reconstructs a real painting with native
`EntityTag.variant`; the client cannot choose arbitrary delivery NBT. Variants share the
painting item's supplier quota. Native placement owns the selected image after delivery.
Cards show the variant's localized title (or exact variant ID when a title is absent).
Search uses the packaged `market_search_terms.json`: exact en_us/ru_ru item/block names
and painting title keys from the matching vanilla bootstrap/assets, installed mod JARs,
packaged resource packs and KubeJS asset overlays. Native and overlay names are unioned,
so either visible packaged name is searchable without depending on server language.
The server matches only bounded aliases for an already admitted listing; the client
renders the server-filtered page without a second incompatible language filter.
`author_building_supply.py --author-search --assets-root <local Mojang assets>` records
source-relative paths/content hashes and reproduces this search resource; it requires
no game launch, client registry upload or runtime locale API.
No network schema change is required: existing packets already carry the offer ID and
an ItemStack. Purchase-only cards say that there is no buyback, and unavailable actions
cannot be clicked. The existing market and native trading layouts remain intact.

## Evidence and generated files

`BUILDING_PURCHASE_PRICES.csv` exports budgets and conditional admission, not live
category membership. `MARKET.csv` covers retained two-way commodities after restrictions.
`VILLAGERS.csv` marks prohibited result directions. `EXCLUDED.csv` distinguishes the
permanent rule, Create's channel restriction and conditional building purchases.
`reconcile_economy.py --export` applies the same export qualification automatically.

The registry/recipe capture used for authoring is historical (`2026-10-15T22:53:32.832415700Z`).
No automated test, gameplay QA or new arbitrage analysis was run for this prompt.
Production compilation, synchronization and native AutoModpack publication are recorded
by the task's delivery/change record; they do not establish player or visual acceptance.
Integrated singleplayer remains outside the dedicated-server runtime registration.

<!-- BEGIN BUILDING SUPPLY -->
The 2026-10-16 building-supply change supersedes the older catalog totals and two-way-only coverage below.
`MARKET.csv` contains retained two-way commodities after public-market restrictions.
`BUILDING_PURCHASE_PRICES.csv` contains acquisition budgets, **not a live availability list**: only the
shared JEMCompat classifier/material selectors admit items, then permanent restrictions win.
Newly admitted purchases have no new NPC buyback. `TRADE_RESTRICTIONS.csv` exports the single canonical
restriction resource. The capture and any older recipe audit predate this change; no new gameplay,
arbitrage, visual or automated-test acceptance is claimed. See ../market-building-supply.md.
<!-- END BUILDING SUPPLY -->

# Economy balance

Generated from current authored resources and loaded registry captured 2026-10-15T22:53:32.832415700Z.
All values below are Copper equivalents; actual GUI prices use existing Coin icons.

## Catalog coverage

13,880 loaded Item IDs are partitioned into 627 admitted
NPC merchandise identities and 13,253 explicit catalog exclusions.
Market has 260 real two-way item policies; native-only merchandise
adds 367 identities. Weighted additional pools contain
149 entries. Duplicate profession/provider rows are intentionally retained.
Prior Drive exclusions and their reasons remain in EXCLUDED.csv unless the user's
explicit preserve-native contract supplies the documented native-only exception.
There is no PLAYER SHOPS ONLY classification: NPC exclusion does not restrict player
shops, gifts, dropping or player-to-player barter.

## Price scale and production

Copper/Silver/Gold/Aether/Arcane are worth 1/10/100/1000/10000. Quantities and
recipe yields matter: crop processing, milling, smelting fuel, compression,
unpacking, cutting and expected coproducts are included in the recorded audit.
Each MARKET.csv row records the actual current prices, caps, demand group and
item-specific reason. Finished goods remain native specialist merchandise;
unique boss/progression equipment is not admitted by a generic price fallback.
Existing approved commodity prices are retained except individually evidenced
recipe/exploit corrections documented by the task owner.

Native equipment/books keep exact NBT; acquisition premiums follow actual
enchantment rarity/level. Purchase discounts clamp after racial adjustment to
half the base budget or maximum whole-offer buyback plus one, whichever is higher.
Native sell_units is a denominator: whole-offer payout is the ceiling of
sell_copper × quantity × multiplier / (100 × sell_units), minimum one Copper.
Canonical Market commodities retain their original per-unit rounding contract.
Original native offer counts are never changed to conceal fractional prices.
VILLAGERS.csv distinguishes exact loaded samples from unobserved price policies.

## Earning, spending and finite supply

An account can receive at most 240 Copper per economic day from NPC sales, with
4000 Copper shared across the server, item caps and the funded NPC_MARKET reserve.
At the payout cap, one Copper-valued unit yields at most240 units/day before item
caps; higher-value goods reach the same money ceiling sooner. Purchases consume
persisted daily supplier stock separately from NPC buying quotas. Primary demand
pays150%, secondary125%, otherwise100%, subject to integer rounding. Time rewind
does not reset an already consumed cycle. Automation is useful surplus production,
not an unlimited reserve or bypass of player/global limits.

Default recruit hiring/weekly wages: ordinary120/20, ranged200/40, mounted400/70,
command700/120. Hiring adds5% of base per native experience level above1, capped3×.
Seven Minecraft days equal168000 monotonic game ticks; each contract's immutable
period roster and ledger key prevent duplicate weekly charging. Three actual unpaid
weeks cause persistent rebellion; death does not forgive it. Town employers pay
the existing treasury; standalone employers pay their real Wallet. Native combat,
ownership and paid defeat/rehire remain with their existing owners.

Founding defaults300 Copper after validation and creates the existing97×97 area.
Expansion preserves the approved1 backed physical Copper per newly added inclusive
block of area; shrink/no-op cannot charge or mutate. For example one new97-block
column costs97 Copper, while a new10×10 area costs100 Copper.

## Evidence limits

The latest recorded recipe report models 15445
recipes and reports 0 unsafe single-output
and 0 unsafe coproduct routes.
This report may predate current resource hashes: rerun --audit --export after
final changes. Expected-value processing analysis does not prove arbitrary lucky
craft sequences or runtime/player behavior. Visual acceptance, optional merchant
boundaries and crash durability must be reported honestly in IMPLEMENTATION_REPORT.

## Practical scale and progression

At base demand,64 crops worth2 Copper each earn128 Copper; primary demand
raises that to192 before shared caps. A120-Copper ordinary recruit is about
half a capped earning day, its20-Copper weekly wage is one twelfth of that day,
and300-Copper founding is1.25 capped days. Ten ordinary recruits require1200
to hire and200 per Minecraft week to maintain. These are caps/benchmarks,
not promised passive income; supply, reserve, item quotas and actual harvests apply.

Ordinary timber costs8 Copper. Wheat and all four Farm & Charm mincer grains
cost8 because four-flour output and native wholesale discounts matter. Cod and
salmon, raw or cooked, cost14 because cooked fish produces two bones and enough
meal/dyes to return7 Copper even through the cheapest preserved native dye batches.
Bone costs4; wine-bottle buyback pays1 Copper for the native two-bottle batch.
These corrections follow exact conversions rather than a uniform rarity multiplier.

Boss rewards, exclusive progression keys and protected equipment retain their
gameplay owners and have no generic NPC pricing fallback. Ordinary ingredients
do not inherit a boss gate merely because a protected recipe uses them. Rare
specialty weight never grants permission to bypass a protected World Tier endpoint.
NPC exclusion leaves gifts, barter, player shops and other player transfers intact.

The dedicated mincer recipe now makes1 cobblestone from1 stone. Create's native
fan remains fuel-free; ordinary generators/farms remain valid surplus production,
bounded by the same NPC reserve and daily budgets. Integrated singleplayer skips
the recipe adapter through KubeJS's physical-client Platform binding.

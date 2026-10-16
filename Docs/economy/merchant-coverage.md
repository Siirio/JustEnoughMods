# Native Coin Merchant Coverage

Updated 2026-10-16 for the explicitly requested complete economy overhaul.
Native `MerchantOffer` remains the owner of goods, counts, NBT, original order,
stock, demand, XP and restocking. Original vanilla and mod-installed offers are
never deleted or rebuilt from a generic fixed profession inventory.

| Provider | Implemented handling | Remaining validation |
| --- | --- | --- |
| Vanilla villager, all thirteen professions | Existing native offers retained; exact merchandise prices; paired buy/sell offers; weighted specialty additions across levels 2–5 | Forge boot and player interaction acceptance belong to integrated verification |
| Wandering trader | Existing offers retained; same price and paired buyback adapter | Native no-restock behavior remains; no invented merchant refresh |
| Actual AbstractVillager subclasses | Existing class-based native menu/settlement adapter; approved canonical goods and explicitly priced specialty goods | Unknown modded merchandise remains visible but unavailable until its real value/progression is established |
| Buyer tagged jem_market_buyer | Native demand-derived global Market offers; unchanged funded reserve and account/global quotas | Does not receive ordinary profession specialty offers |
| Native item-only barter | Original result slot and callbacks | Not reinterpreted as currency |
| Merchants outside AbstractVillager | Their own native interaction | No guessed API, replacement GUI or claim of universal optional-mod coverage |

## Merchandise and exclusion boundary

`native_item_prices.json` explicitly admits native vanilla profession merchandise,
including enchanted books, equipment, maps, dyed goods and prepared food. These
prices are separate from global Market admission. `profession_pools.json` contains
exact modded specialty IDs and is also the editable source of added prices,
profession, level, weight, count and stock. Canonical BUY_AND_SELL commodity policy
wins wherever it applies; all forty previously approved commodity prices remain
owned by ItemPolicyRegistry.

This intentionally changes the older unresolved native boundary: foods and bulk
materials marked NONE for the universal Market may now be traded by their native
profession. The user's new explicit requirement to retain all vanilla trades is
the authority for this narrow exception. It does not admit those goods to Shipping
Box or the universal Market. Emeralds never receive a Coin conversion policy;
unique boss rewards, netherite, elytra and arbitrary unclassified equipment have
no native fallback. Player-to-player transfers remain independent.

Native noncanonical NBT is retained exactly, not stripped. Enchantment acquisition
adds a level-squared premium weighted by native rarity/treasure status. Names,
contents, damage, maps and enchantments do not inflate buyback. Damaged equipment
has reduced buyback. Only exact priced item IDs may use this path; arbitrary NBT
cannot turn an excluded item into admitted merchandise.

## Selection and financial ownership

Each merchant unlocks one weighted extra item per eligible level. Individual choice
is persisted under `jem_economy_profession_choices_v1` in the entity's native Forge
persistent data; native offers themselves are serialized by Minecraft, including
uses and demand. Reopening/restocking/restarting does not reroll. Existing duplicate
items are skipped before selection; original offers and counts remain intact.
Companion reverse offers copy goods/NBT/counts and add zero XP, preventing effortless
level farming through extra buyback rows. New level unlocks are checked while the
native menu is open as well as on interaction.

All monetary operations stay in EconomyApi and the existing ledger, funded
NPC_MARKET reserve, identity, daily player/global item and account limits. Native
bulk merchandise uses 64/player and 512/global limits; unstackable equipment is
limited to 8/128. Specialty pools use 8/128. Canonical commodity limits retain their
existing owner and can deny an entire native batch when already exhausted.

Native purchase price follows the offer's current/original emerald-count ratio
only as a demand/reputation modifier of the item's real Coin value. It never uses
an emerald-to-copper rate. After all racial discounts, a purchase costs at least
half the base merchandise budget and strictly more than the maximum 150%-demand
buyback for the exact output. No event may lower this floor. Actual transactions
revalidate acknowledged price, exact original terms, stock, funds, quota, reserve,
input orientation, nonce and output capacity before one native callback.

## Evidence and exports

Six deterministic Python production-data checks cover consistent prices across
professions, positive spreads, Atlas source identities, native NBT/food coverage,
progression exclusions, per-level specialty variation/stock, Market separation and
several high-risk batch conversions. They do not prove Forge lifecycle, whole-pack
recipe safety, rendering or multiplayer acceptance.

`NativeTradeCatalog.snapshot(ServerLevel)` exposes the loaded native profession and
wandering-provider pools for the opt-in runtime exporter. It includes profession,
level, provider/index, deterministic sample stacks/NBT/counts, exact price policy,
uses, XP and restock owner. Randomized NBT samples are explicitly nonexhaustive.
World-dependent treasure-map generation and unknown optional provider callbacks
are not invoked by an audit: their pool rows remain present with an exact reason.

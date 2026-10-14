# Native Coin Merchant Coverage

Source restoration implemented2026-10-14 following the approved
[native-interface design](native-trading-design.md). Compilation, Mixin application,
gameplay and rendered acceptance remain unverified; installed JARs were not changed.

| Provider | Source handling | Limits |
|---|---|---|
| minecraft:villager and wandering_trader | Original native opening, MerchantMenu/Screen, full offers/input/result/inventory/scroll/restock | Coin-priced monetary offers require existing authoritative prices; unknown/protected/NBT rows remain visible/unavailable. |
| Actual AbstractVillager subclasses | Same class-based native adapter, no enrollment | Exact optional-mod lifecycle and monetary coverage remain unverified. |
| Buyer tagged jem_market_buyer | Native demand-derived offers, all rows, no chat | Whole sales share funded NPC_MARKET/demand/quota and credit Wallet. |
| /market | Transient read-only native merchant presentation | No remote sales, NPC search/spawn, world save or chunk loading. |
| Item-only native barter | Original result slot and callbacks | Both input orientations retained; no Coin reinterpretation. |
| Custom merchants outside AbstractVillager | Native behavior untouched | No guessed Coin integration API. |
| Service/crafting blocks, Recruits/Workers | Existing native/Settlements owners retained | Not converted into Coin providers. |
| Spudacious Shops1.10.2 | Existing JEMServer escrow adapter retained | Physical stock and verified shop ownership unchanged. |

Market policy remains exactly BUY_AND_SELL or NONE. Native monetary offers without
a canonical price are an unresolved boundary, not a new market outcome. This task
does not invent diamond-gear prices or an emerald exchange rate.

Coin result actions acknowledge quote revision/selection and a bounded nonce.
Server validation checks current original item terms, either native input order,
identity/distance, stock, funds/reserve/quota and delivery capacity. Guarded display
coins cannot become physical cash. Native notifyTrade owns uses once; Roles prices
one Human Coin benefit and suppresses emerald refund only for Coin settlement.

The old176-pixel/18-offer screen/menu/provider are removed; its registered menu ID
is retained as an unused native-screen compatibility alias. Wallet protocol3 needs
matching Economy/Roles/Server client-and-server artifacts after a separately
requested build/release. Original coin assets and supply remain unchanged.

One fresh read-only review found stale-price acknowledgement, shrinking-demand
selection, reversed monetary inputs and native-tooltip overlap; one source fix
pass addressed them. Source review is not proof of runtime success.

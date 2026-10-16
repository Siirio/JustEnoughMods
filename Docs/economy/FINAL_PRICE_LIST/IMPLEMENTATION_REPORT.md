# Economy implementation report

The user requested and authorized implementation, balance decisions, builds and automated verification.
The following describes current source/resources and captured evidence, not acceptance of every gameplay path.

## Implemented owners

- JEMEconomy: global two-way policies, supplier stock, sale quotas/reserve, existing Wallet/backed Coin ledger, native merchandise and persisted weighted specialties.
- Native MerchantOffer: original goods/counts/NBT, progression, uses/demand/restock and saved individual offers. Compatible AbstractVillager merchants reuse this adapter.
- JEMSettlements/Recruits: existing native hire ownership, Coin charges, per-contract seven-day payroll, real employer/treasury debit, persistent three-miss rebellion and atomic validated territory charges.
- Shared client/text currency presentation: the five actual registered Coin assets and exact denomination amounts; native screens remain their original owners.

## Reconciliation

```json
{
  "loaded_items": 13880,
  "loaded_recipes": 15378,
  "market_items": 260,
  "native_only_items": 367,
  "npc_admitted_unique": 627,
  "excluded_items": 13253,
  "unverified_ids": 3537,
  "native_provider_rows": 643,
  "additional_pool_rows": 149,
  "villager_export_rows": 924,
  "active_market_ids_missing": [],
  "invalid_native_rows": [],
  "capture_utc": "2026-10-15T22:53:32.832415700Z",
  "resource_sha256": {
    "item_policies.json": "7352d9b0584b2faea48ca9b41a1646fa659180e2d6007927127b5e35b72f91e2",
    "native_item_prices.json": "c81c3255ee0acc09ce2ec62404b3fa3d5faf3268166f4596ce756fc080da3f1b",
    "profession_pools.json": "5ea8a5f2bf009431cfdc7172eaf28afc73b7a287d670ed2cf7435ed40fdbe559"
  },
  "native_provider_unsampled": 20,
  "latest_audit_native_coverage_complete": false,
  "latest_audit_pending_runtime_recipe_overrides": [],
  "latest_recorded_audit_unsafe": 0,
  "latest_recorded_audit_coproduct_unsafe": 0
}
```

MARKET.csv contains only real loaded active policies. VILLAGERS.csv labels loaded native provider samples, authored weighted pools and price policies not observed in the current sample separately.
EXCLUDED.csv covers every other real loaded item with its exact prior/current reason or an honest no-approved-NPC-channel exclusion. It does not claim unresearched items are all unobtainable or overpowered.
UNVERIFIED_IDS.csv contains only candidate IDs missing from the actual loaded registry, including static/Drive evidence that must never be silently activated.

## Verification and delivery status

| Activity | Actual evidence/status |
| --- | --- |
| Production builds | PASS: reobfuscated Economy1.2.0, Settlements2.0.0, Compat3.7.0 and full Server2.0.0-dev. Exact source/Server/ManagedPack artifact hashes recorded below. |
| Latest dedicated registry/recipe capture | captured 2026-10-15T22:53:32.832415700Z; loaded identity evidence only |
| Native provider capture | PASS: 643 actual provider entries;623 safe providers sampled with no failure;20 world-dependent/mutable/unaudited callbacks intentionally not executed. |
| Native Java arithmetic | 331036 standalone assertions passed during this task; does not prove Forge lifecycle |
| Merchant production-resource contracts | PASS:6 Python resource checks on final prices; all149 specialty entries and all native-price IDs resolve in actual loaded Item registry. |
| Recruit/settlement contracts | PASS:27 production contract assertions. Actual live recruit/Noble hiring and weekly server gameplay were not player-tested. |
| Coin denomination arithmetic | standalone production contract passed per owning presentation note |
| Client rendering/player acceptance | NOT RUN: no controllable Minecraft client surface in this session. Coin assets, narrow native rendering hooks and mapped client refmaps compile; visual acceptance and live multiplayer interactions remain untested. |
| Final Server/ManagedPack synchronization | PASS: exact four freshly reobfuscated JARs installed on both local surfaces; Vinery config fixes and dedicated-only recipe guard synchronized. No stable profile, MineRENT or baseline promotion. |
| Native AutoModpack publication | PASS: inspected preview +1/~6/-0, native publish journal sequence17; staged/published manifest SHA1 both bc4691d21ceb4beaf577522c95b481843e1656cf. |
| Git delivery | This report is committed with the source on main; its owning commit and remote confirmation are reported in task delivery. No PR or force push. |
| MineRENT/stable/baseline | not authorized for this task; no release promotion claimed |
| Native merchant persistence contracts | PASS:18970 assertions,13 professions x32 seeds;13 demonstrate individual variety. Native NBT roundtrip and reopen stability passed; no spawned NPCs or actual world restock. |
| Recipe conversion audit | PASS:15445 modeled routes;0 profitable endpoints,0 profitable coproduct liquidations;converged6passes using final runtime recipe counts, no authoring overrides. Expected-value analysis;20 provider callback gaps remain explicit. |
| Google Drive source | Accessible; all seven tabs and17063 Full Registry design rows read. Treated as prior design, not runtime proof. |

## Material limits

- A sampled randomized native provider is not an exhaustive list of every enchanted-book/map/potion/NBT variant. World-dependent treasure-map and unknown optional provider callbacks can remain explicitly unsampled.
- Unknown or explicitly excluded optional-mod merchandise remains visible/unpriced; existing native interfaces are preserved without inventing APIs or boss/World Tier exemptions.
- Resource/audit hashes and loaded captures must refer to the final artifacts before claiming final runtime validation. This generator never runs a build, modifies prices, starts a server or publishes a pack.
- Same-thread payment/item handling does not establish crash-atomic persistence across separate player/NPC/SavedData files. No deliberate crash durability acceptance is claimed.
- A remaining unsafe recipe count, missing active ID, absent native capture or pending acceptance is outstanding work, not successful completion.

Owning detail: ../merchant-implementation-notes.md, ../settlement-implementation-notes.md and ../presentation-implementation-notes.md.


## Installed production artifacts

| Module | JAR | SHA-256 | Build / Server / ManagedPack identical |
| --- | --- | --- | --- |
| JEMEconomy | jem-economy-1.2.0.jar | c3f5422d2a558d3f0ceaf5adaac2249e5bec9bd2816589b0653ecf65ca163789 | True |
| JEMSettlements | jem-settlements-2.0.0.jar | 5427eb401b6e6fe5ac23c624eb89e6a6a15ac0113c9648c7ad7be04047a05e2e | True |
| JEMCompat | jemcompat-3.7.0.jar | f0e43c86a07fd357e6de67ff9824ec13f6e5c906108a8f32c53f51be777a2657 | True |
| JEMServer | jem-server-2.0.0-dev.jar | 11848268e8da8c41769de780c892b2af3b8c1460fdb7dd07370500cb9f6569ed | True |

## Player entry and reproducibility

Open `/smp` and its Market for the two Coin price rows. Native villagers and compatible special merchants keep their usual interaction and controls. Personal employers pay Wallet; assigned town recruits use the real treasury. Existing Wallet withdrawal supplies backed physical Coins for the protected exact-area expansion path.

The opt-in dedicated JVM property `-Djem.economy.export=true` writes `Server/local/economy-audit/loaded-registry.json` and executes unsaved native-NBT contracts. Use it only for an authorized local verification run. `python Mods/JEMEconomy/tools/reconcile_economy.py --audit --export --runtime Server/local/economy-audit/loaded-registry.json` re-reads installed JAR recipes, audits the capture and regenerates these files. Drive classifications require the captured seven-tab design source; they are not item-registration proof.

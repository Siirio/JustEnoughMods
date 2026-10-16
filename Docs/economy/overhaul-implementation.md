# Complete economy overhaul — 2026-10-16

User specification: complete autonomous economy overhaul supplied 2026-10-16.
The user explicitly authorizes design choices, builds, automated tests and local
runtime validation for this task. No MineRENT deployment, world resets, stable
profile changes or release-baseline promotion. Preserve unrelated race/VFX work.

Experience: farmers, fishers, miners and explorers can turn ordinary surplus into
useful supplies, individual specialist villagers, an affordable settlement and a
small paid guard. Automation is useful without buying infinite currency. Boss and
World Tier accomplishments remain earned through gameplay.

Architecture: Economy owns wallet, backed coins, ledger, two-way prices and NPC
budgets. Native MerchantOffer owns goods/NBT, progression, stock and persistence.
Settlements owns territory, workforce and payroll; native recruits own combat.
Client renders server quotes using the five existing coin items. Integrated
singleplayer keeps native behavior. EXCLUDE affects NPC channels only.

Execution uses the subagent-driven-development skill for the independent owner
tasks below, with root integrating catalog, validation and distribution. All work
stays in the current main checkout. Only root commits/pushes and edits CHANGELOG.

- [x] Research: read all seven Drive tabs, canonical policy, Atlas and installed
  registrations/recipes. Retain source snapshots outside tracked production data.
- [x] Catalog/market (root): reconcile confirmed registry and prior classifications;
  explicit editable two-way resource policies, quotas and stock; compute recipe
  conversion bounds; export all six requested final deliverables.
- [x] Native merchants: preserve native offers; profession-appropriate weighted
  additions at levels 2–5; persistent choices and paired buyback; price native NBT
  goods without an emerald conversion, protect progression and discount floors.
- [x] Recruits/settlements: exact native API integration, fair hiring, seven-day
  persisted payroll, actual missed periods, permanent rebellion, atomic founding
  and expansion using the existing treasury/payment owner.
- [x] Presentation: shared exact denomination renderer with actual coin assets;
  compact buy/sell Market rows, wallet and native monetary cells; existing native
  merchant geometry and SMP palette remain.
- [x] Verification: deterministic production-contract tests, affected reobfuscated
  builds and safe local dedicated-server validation without overwriting saves.
  Review replay, overflow, discounts, conversion chains, NBT and restart behavior.
- [x] Delivery: update owning docs/Atlas/CHANGELOG; synchronize exact fresh JARs
  to Server and ManagedPack, native preview/publication, verify manifest agreement,
  commit exact task paths and normal-push main; report remaining runtime limits.

Review focus: recipe cycles across multiple machines, discount-induced buyback
profit, vanilla book/equipment NBT preservation, recruit payment replay/unloaded
rosters, territorial no-op/alternate-interface charges, stale native trade quotes,
and incomplete client generation publication.

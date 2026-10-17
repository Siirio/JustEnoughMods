# Feature audit

Status: original before-change source audit plus implementation record, 2026-10-17. No in-game acceptance claims. Paths below are repository-relative. `S` abbreviates `Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/`.

## Implemented after approval

- `JEMCurrencyCore` now owns the existing Coin registrations, saved ledger, physical provenance, wallet/native merchant network and earning adapter. Full Economy delegates and retains shipping/full market functionality; Jar-in-Jar embeds the same shared artifacts.
- `SettlementAuthority` supports integrated and authenticated dedicated servers without changing global SMP authority. Optional NPC events/methods remain guarded compat code; NativeManagement opens verified installed native APIs.
- `SettlementActions`, `SettlementRequests` and `SettlementViewDetails` implement explicit selected IDs, paginated owner/food views, persistent operation terms/results, physical stock, actual receipts, storage/resident/workforce data and acknowledged actions.
- `SettlementScreen`, `SettlementMap` and client state implement all eight sections, selector/creation/details/confirmations, true territory fill and Gold-priced exact unions, separate bars, searches/filters/sorts, native portraits, RU/EN guide, optional capabilities and same-intent reconciliation. Production code never uses preview balances or artificial residents.
- Vanilla safe food uses native classification alongside modded food, harmful policy stays negative, and standalone demands use persisted actual dimension visits plus recipe-ingredient reachability. Existing fatigue/payroll/rebellion/catch-up limits remain owned by existing services.
- `SettlementWarnings` observes bounded real persisted conditions, sends notices only on new critical/payroll/rebellion/full-storage transitions, suppresses reconnect warning floods and preserves warnings in their management screens.
- An editable control-family candidate is produced; the remaining custom PNG artwork and in-game visual acceptance remain outstanding. Unknown crash outcomes are quarantined for operator reconciliation; interrupted cross-file inventory transactions are not declared proven atomic.

The matrix below records the **before** state and rationale for these changes; it must not be read as evidence that the old source remains installed.

## Coverage matrix

| Requirement | Actual source evidence | Required work / limit |
| --- | --- | --- |
| Independent startup | `Mods/JEMSettlements/src/main/resources/META-INF/mods.toml` requires Identity and Economy; `build.gradle` compiles against sibling outputs | Isolate identity and money ownership; packaging audit before dependency changes |
| Singleplayer | `S/api/SettlementApi.java` mutators, `command/SettlementCommands.register`, `runtime/SettlementRuntime.serverTick` use dedicated guards | Settlements-local integrated authority; leave global SMP gate intact |
| Multiple settlements | `town/SettlementSavedData.ownedTowns`, `SettlementApi.create` have no per-owner settlement cap | Existing data supports many; GUI/commands must consistently use explicit selected ID |
| Selection safety | `SettlementApi.ownedTown(player)` -> `SettlementSavedData.ownedBy` -> `findFirst`; `SettlementNetwork.sendOwned` uses it | Remove implicit-first use from management operations; paginated owner selector |
| Entry | `client/SettlementClientState.openSmp` reflects JEMServer screen and swallows missing class; key default unbound | Own screens, compatible-server detection and real request path |
| Command | `/settlements` currently registers only `help`; `/jem town` opens first town | Executable root `/settlements`, selector behavior, backward-compatible alias |
| Founding | `SettlementApi.create`: bell within 16 blocks, name 3–32, global name conflict, 64-block anchor check, territory separation; config default 300 copper | Preserve restrictions and fee; idempotent request/result, exact created ID, informative failures |
| Territory geometry | `town/TownRecord`: inclusive X/Z rectangle, initial radius48, schema6, bounds ±256 | Preserve rectangle and migration; do not switch to chunks/cell sets |
| Expansion | `SettlementApi.expandBounds`, `town/TerritoryQuote`: union area; physical backed Copper debit, bounded64 town receipt IDs | New Gold-per-added-block price and full-strip preview; durable request intent/replay audit |
| Built-in map | No built-in MapCanvas in this module | New pan/zoom map; explored terrain policy; authoritative bounds; optional Xaero adapter stays native |
| Main GUI | Only founding and hire/noble quote clients in module; town management in `Mods/JEMServer/.../client/smp/SmpScreen.java` | Own persistent shell, eight sections, detail routes, scale/focus/scroll |
| Persistence | `SettlementSavedData` stores Towns/Residents/Workforce/Employers in overworld SavedData; UUID keys; Town schema6 | Preserve keys and IDs; explicit migration diagnostics. Existing load catches skip malformed rows; do not silently discard them in new migration |
| Living residents | `SettlementRuntime.death` removes resident records; view filters living; purge excludes dead non-recruits | Integrated lifecycle and death while unloaded need evidence; unloaded != dead |
| Removal | `SettlementApi.removeRegistration` clears affiliation; `removeWorkforce` refuses recruit removal | Add hostile resident/rebellion/debt checks; preserve actual entity and native profession |
| Rename/delete | ID-aware API exists; delete blocks unpaid payroll, refunds balance, moves recruits to personal payroll | UI confirmation and ack; prohibit hostile-state bypass; preserve debts and loyalty through transfer |
| Happiness | `HappinessService.calculate`: housing25%, food25%, safety20%, jobs15%, community15%; decoration≤10 bonus; tax penalties | Display actual contributors; no extra category to fill a screen |
| Aggression | `SettlementRuntime.tickHostileResidents`: mood<10 targets owner, navigation and actual damage | Shared integrated/dedicated authority and server-derived hostile projection; runtime acceptance pending |
| Food fatigue | `resident/ResidentMood`: per-villager/item `ServingsToday`, limit10, day=floor(gameTime/24000), save/load | Expose count/refusal without mutating it during rendering |
| Food use | `SettlementRuntime` consumes drops and physically extracted linked-storage food; resident applies mood/satiety; native breeding inventory path | Verify actual extraction/one-consumption and special-food effects; projection must use villager context, not player-context guesses |
| Vanilla food | `FoodValueRegistry.classify` returns empty for safe `minecraft` namespace before reviewed value use | Explicit master request requires safe vanilla food support. Harmful food retains negative policy; no guessed new balance |
| Modded food | FoodProperties-based classifier, override resource; dangerous effects inspected | Preserve consumer-sensitive food behavior and actual item/NBT; unsupported data gets a reason |
| Food requests | `AvailableDishService` reflects JEMServer CookingRecipes and returns empty when missing | Standalone progression/demand adapter; do not invent inaccessible food or transfer eating restrictions from demand rules |
| Workers | `compat/workers/WorkersAdapter` maps installed native types, owner UUID, work-area evidence | User clarification: optional mod only. Isolate class loading; UI can manage only supported native actions |
| Catch-up | `SettlementSimulationService.activate`: max6 days, only evidenced Workers FARMER wheat obligation; other roles unsupported | Preserve exact supported recipe; disclose unsupported role catch-up. Do not fabricate miner/cook yields |
| Physical output | `deliverPending`: loaded capability slots, seed input, wheat output, capacity check, bounded pending obligations/quarantine | Crash/partial-handler transaction audit before claiming no duplication/loss; service output not spendable items |
| Recruits/payroll | `payroll/PayrollService`, `workforce/WorkforceRecord`, `compat/recruits`: 168000-tick cycles, per-contract generation, unpaid periods, three misses, persisted rebellion/defeat/paid rehire | User clarification: Recruits only. Preserve roster/arrears and exact existing rates; capability-isolated runtime |
| Treasury | Economy account by town UUID; ID-aware treasury overload exists; legacy packet lacks town ID | Protocol replacement, explicit town ID/request ID and server receipt; one money owner |
| Jade | `compat/jade/SettlementJadePlugin` exists | Optional discovery audit; reuse same projection and bars, no invented membership |
| Guide | `command/SettlementGuidebook` exists | Own illustrated localizable guide; update to implemented standalone capabilities and new price |
| Notifications | No standalone notification service established by this audit | Transition-based deduplication; persistent warnings live in relevant section |
| Visual fixtures | No new fixture suite created | Future isolated developer-only preview; not a production packet/mutation path |

## Ownership and dependency impact

Settlements retains town identity, residents, storage links, happiness, payroll contract metadata and catch-up obligations. Economy retains money/provenance/receipts when present. Workers owns its entity AI, work areas and native production. Recruits owns its entities, hiring/combat/native ownership. JEMServer owns SMP/cooking progression integration, not standalone entry. Client owns presentation only.

Making a dependency optional in metadata is insufficient: `SettlementRuntime` has a `RecruitEvent.Dismissed` subscriber and `RecruitHireMenu` checks, `SettlementNetwork` has recruit-typed public methods, PayrollService and workforce adapters reference native classes. These must move behind guarded registration into compat-owned classes. The common snapshot/request DTOs must not mention optional classes. The existing Mixin plugin requires Recruits and selects common gameplay Mixins only on physical dedicated servers; an integrated-server adaptation requires a Settlements-specific authority guard, not broad removal of side checks.

Five current coin values are copper1/silver10/gold100/aether1000/arcane10000 (`Mods/JEMEconomy/.../physical/CoinDenomination.java`). Gold per block therefore means 100 copper ledger units per newly added block, not one Gold per edge drag. No currency balance or old holdings conversion is authorized.

## Delivery sequence and exit evidence

1. Approve the proposed shared currency/earning adapter and full GUI contract; prepare the exact source migration and implementation plan.
2. Extract dependency seams and local authority; compatible-server handshake; world-preserving persistence migration and optional-module loading.
3. Owner selector, creation, explicit town actions, typed snapshots/results, shell and real map. Retain exact ID and idempotency through all consumers.
4. Resident/food/happiness/detail routes and guide, truthful capabilities; optional Workers/Recruits management with real API evidence.
5. Durable money/claim/delivery audit; approved art control-family candidate; exact exports; replace temporary GUI materials.
6. Authorized compilation, local installation, native pack publication and permitted acceptance work; finish Atlas/docs/changelog and push full state.

Every stage labels source existence separately from compilation, runtime and user acceptance. This implementation adds `Catalog/changes/records/settlements-standalone.json`; delivery evidence and remaining acceptance live in TEST_MATRIX.md.

Control-family review candidate: [native1x sheet](../assets/review/control-family-r1/dark-native1x.png), [nearest4x sheet](../assets/review/control-family-r1/light-nearest4x.png), [editable source/manifest](../assets/source/control-family-r1/README.md). Eight original exports cover panel, four button states, tabs and two bars. The offline reference sheet is art-only, has no gameplay transport or world mutation, and is not an in-game screenshot. `CANDIDATE_NOT_APPROVED / SOURCE_EDITABLE / WAITING_FOR_REAL_GAME_CAPTURE`; no JAR/resource integration.

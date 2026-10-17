# Settlements standalone design

Status: **approved and implemented in source; production artifacts built; runtime and visual acceptance tracked in TEST_MATRIX.md**. Approval and implementation date: 2026-10-17.

This directory owns the standalone GUI contract and delivery evidence. Settlements2.1.0 supersedes the dedicated-only entry and Copper expansion behavior in `../settlements-update-1.md` and `../../economy/settlement-implementation-notes.md`. Source comments and historical test reports are not acceptance evidence.

## User decisions

- Forge 1.20.1 / Java 17; independently installable Settlements, integrated logical-server authority and the same screens on compatible dedicated servers.
- Native Minecraft proportions and compact icons; map atmosphere informed by Antique Atlas. No Overview, web dashboard, replacement merchant GUI or replacement Xaero controls.
- Existing mechanics survive except explicit changes: Gold replaces Copper expansion pricing; vanilla edible items must work; Settlements itself becomes available in singleplayer. This exception must not activate unrelated JEM SMP systems in singleplayer.
- **Clarification in this chat:** workers and recruits exist only when their respective Workers/Recruits mods are installed. No built-in replacement NPCs, jobs, recruitment or combat AI. This overrides the master prompt's implication that all such systems work in a clean Forge+Settlements installation.
- Actual state/actions and exact asset contracts precede artwork production. Production artwork is not approved by this design.

## Documents

| Document | Owns |
| --- | --- |
| [FEATURE_AUDIT.md](FEATURE_AUDIT.md) | Source evidence, existing behavior, gaps, ownership and delivery stages |
| [GUI_SPEC.md](GUI_SPEC.md) | Screen/component geometry, fields and action contracts |
| [GUI_STATE_MACHINE.md](GUI_STATE_MACHINE.md) | Entry, selection, request/acknowledgment, synchronization and lifecycle |
| [ART_BRIEFS.md](ART_BRIEFS.md) | Exact PNG contracts, editable sources, states, placement and art gates |
| [TEST_MATRIX.md](TEST_MATRIX.md) | Actual evidence and pending acceptance; no fabricated passes |

## Implementation choices by Codex

1. Keep `Mods/JEMSettlements` as the owning module. Replace its reflection-based SMP entry with its own screens and typed, bounded network projections. JEMServer becomes an optional navigation consumer of those screens, never their implementation owner.
2. Add a local Settlements authority boundary, not a global change to `DedicatedSmp`. Use player UUID in a standalone world; use authenticated `IdentityPolicy` when that integration is installed. Preserve saved owner/town/entity UUIDs.
3. Extract reusable coin/ledger primitives into `Mods/JEMCurrencyCore`, proposed mod ID `jem_currency_core`, artifact `jem-currency-core`, bundled with standalone Settlements using Forge Jar-in-Jar and also consumed by JEMEconomy. The master prompt explicitly permits a small shared dependency. This is **one runtime mod**, not a source directory compiled twice: identical Maven identity/compatible bounded version range permits Forge to resolve a single dependency when both consumers bundle it. CurrencyCore alone registers the five existing `jem_economy:*_coin` IDs and obtains/writes the one `jem_economy` SavedData. Preserve copper unit values, provenance, receipt semantics and deterministic `jem:town:<UUID>` account derivation. Remove those registrations/classes from Economy when moving them; Economy retains a delegating public facade for callers and full market features. Existing old Economy cannot coexist with the new core: enforce the exact migrated compatibility floor and provide matching artifacts together. Never combine balances or shade duplicate classes. Verify legacy namespace/save migration before delivery; a proposed mod ID is not proof of Forge loading compatibility.
4. Audit all Economy registrations and callers before moving source ownership. The standalone earning route must reuse existing funded Coin merchant transactions/approved quotes through an extracted adapter; do not invent starting grants, new earning recipes, creative-only progression or emerald fallback. Keep native offers/slots/restocking and existing prices. Full market/catalog/SMP event behavior is not activated by extracting settlement transactions. Exact class/file migration and adapter closure belong to the implementation plan and require evidence before compilation/startup claims; simply making metadata dependencies optional is insufficient.
5. Native Workers and Recruits remain optional gameplay owners. Their event subscribers and typed API calls load only after mod-presence checks. Their absence is a supported capability state. Jade and Xaero remain optional presentation integrations.
6. Use block-area expansion: exact rectangular union, price = newly added blocks × one canonical Gold Coin (currently 100 copper units). Initial territory remains 97×97, center-relative bounds ±256, 16-block separation. A clicked neighboring block may add a whole strip because the saved territory is rectangular; show the entire strip and its total cost before confirmation.
7. Keep the existing legitimate founding cost (config default 300 copper), bell interaction, 3–32-character normalized name and location restrictions. No extra GUI fee.
8. Default window 352×240 logical GUI pixels with8px screen margins; compact layout below368×256 available screen pixels. Shared widgets bind immutable server views; cached view state never becomes gameplay authority.

## Approval and completion boundaries

The user approved this contract and full implementation on2026-10-17. Production source now lives in `Mods/JEMSettlements` and `Mods/JEMCurrencyCore`; migrated full Economy1.3.0 shares the same currency implementation. The full `jem-settlements-2.1.0.jar` contains CurrencyCore and Identity dependencies. Do not install its `-slim` artifact by itself. Workers, Recruits, Jade, Xaero, full Economy, Compat and Server are not standalone prerequisites.

Native Minecraft buttons/items and programmatic pixel surfaces currently render the GUI. The separate control-family candidate is produced but unapproved; remaining custom artwork briefs remain unproduced; in-game visual review and clean-instance singleplayer acceptance have not been claimed. No automated tests, visual fixtures or gameplay clients were created/launched. Ordinary dedicated-server startup is permitted solely for installing these artifacts and native pack publication. Stable profiles, MineRENT and immutable release baseline remain outside this update.

## Recovery limits

Mutations persist a bound intent before execution and save backing data before a terminal result. Known results replay without a new charge. Server sessions stamp new request IDs; completed retired-session entries may be bounded safely, while unknown old IDs cannot become new financial operations. Interrupted intents remain explicitly uncertain and block automatic resubmission with a new operation ID. They require operator reconciliation against actual ledger receipts, town state and inventory; there is no claim of atomic commits across arbitrary third-party item handlers and separate player/chunk/SavedData files. Physical production quarantines ambiguous partial delivery rather than retrying or refunding all inputs after a partial output.

## Manual entry

Install the full standalone JAR in a supported Forge1.20.1 instance. Use `/settlements`, configure the Settlements key, or place a bell. With several towns, choose one in the selector. Shift+configured key while looking at a registered NPC focuses its details after authorized selection. Shift+bell interactions retain physical storage/resident registration; ordinary villager trading remains native. `/settlements help` opens the guide. Optional worker/recruit Manage opens the owning mod's actual menu when the real NPC is loaded and within reach.

Control-family review candidate: [native1x sheet](../assets/review/control-family-r1/dark-native1x.png), [nearest4x sheet](../assets/review/control-family-r1/light-nearest4x.png), [editable source/manifest](../assets/source/control-family-r1/README.md). Eight original exports cover panel, four button states, tabs and two bars. The offline reference sheet is art-only, has no gameplay transport or world mutation, and is not an in-game screenshot. `CANDIDATE_NOT_APPROVED / SOURCE_EDITABLE / WAITING_FOR_REAL_GAME_CAPTURE`; no JAR/resource integration.

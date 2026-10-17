# Settlements standalone design

Status: **design for approval; implementation not started**. Audit date: 2026-10-17.

This directory owns the new standalone GUI contract. Existing shipped behavior remains described by `../settlements-update-1.md` and `../../economy/settlement-implementation-notes.md` until implemented changes explicitly supersede it. Source comments and historical test reports are not acceptance evidence.

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

## Proposed implementation choices by Codex

1. Keep `Mods/JEMSettlements` as the owning module. Replace its reflection-based SMP entry with its own screens and typed, bounded network projections. JEMServer becomes an optional navigation consumer of those screens, never their implementation owner.
2. Add a local Settlements authority boundary, not a global change to `DedicatedSmp`. Use player UUID in a standalone world; use authenticated `IdentityPolicy` when that integration is installed. Preserve saved owner/town/entity UUIDs.
3. Extract reusable coin/ledger primitives into `Mods/JEMCurrencyCore`, proposed mod ID `jem_currency_core`, artifact `jem-currency-core`, bundled with standalone Settlements using Forge Jar-in-Jar and also consumed by JEMEconomy. The master prompt explicitly permits a small shared dependency. This is **one runtime mod**, not a source directory compiled twice: identical Maven identity/compatible bounded version range permits Forge to resolve a single dependency when both consumers bundle it. CurrencyCore alone registers the five existing `jem_economy:*_coin` IDs and obtains/writes the one `jem_economy` SavedData. Preserve copper unit values, provenance, receipt semantics and deterministic `jem:town:<UUID>` account derivation. Remove those registrations/classes from Economy when moving them; Economy retains a delegating public facade for callers and full market features. Existing old Economy cannot coexist with the new core: enforce the exact migrated compatibility floor and provide matching artifacts together. Never combine balances or shade duplicate classes. Verify legacy namespace/save migration before delivery; a proposed mod ID is not proof of Forge loading compatibility.
4. Audit all Economy registrations and callers before moving source ownership. The standalone earning route must reuse existing funded Coin merchant transactions/approved quotes through an extracted adapter; do not invent starting grants, new earning recipes, creative-only progression or emerald fallback. Keep native offers/slots/restocking and existing prices. Full market/catalog/SMP event behavior is not activated by extracting settlement transactions. Exact class/file migration and adapter closure belong to the implementation plan and require evidence before compilation/startup claims; simply making metadata dependencies optional is insufficient.
5. Native Workers and Recruits remain optional gameplay owners. Their event subscribers and typed API calls load only after mod-presence checks. Their absence is a supported capability state. Jade and Xaero remain optional presentation integrations.
6. Use block-area expansion: exact rectangular union, price = newly added blocks × one canonical Gold Coin (currently 100 copper units). Initial territory remains 97×97, center-relative bounds ±256, 16-block separation. A clicked neighboring block may add a whole strip because the saved territory is rectangular; show the entire strip and its total cost before confirmation.
7. Keep the existing legitimate founding cost (config default 300 copper), bell interaction, 3–32-character normalized name and location restrictions. No extra GUI fee.
8. Default window 352×240 logical GUI pixels with8px screen margins; compact layout below368×256 available screen pixels. Shared widgets bind immutable server views; cached view state never becomes gameplay authority.

## Approval and completion boundaries

These documents are a concrete design checkpoint, not a completed mod. Financial extraction and the standalone earning adapter must be completed before implementation claims. GUI assets must remain replaceable without changing action logic. No source, installed JAR, saved world, stable profile or release baseline changes belong to this documentation checkpoint.

The project-size design approval boundary comes from `superpowers:brainstorming/SKILL.md`. After approval, produce the implementation plan in this owning directory, then implement each complete source/network/persistence/UI chain with matching Atlas and distribution changes. Tests and launches remain subject to the user's current activity permissions; the acceptance matrix is not itself authorization to run them.

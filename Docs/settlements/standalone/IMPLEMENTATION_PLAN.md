# Standalone Settlements Implementation Plan

> For agentic workers: use superpowers:subagent-driven-development for scoped implementation and review. The user approved execution without another plan approval on2026-10-17.

**Goal:** implement the approved standalone settlement simulation and native GUI with optional Workers/Recruits.

**Architecture:** one shared currency runtime owner; Settlements-local logical-server authority; optional integrations load behind presence guards; one real-state GUI driven by bounded acknowledged actions.

**Tech stack:** Forge1.20.1-47.4.10, Java17, current ForgeGradle, Mojmap, NBT SavedData, native Screen/widgets and SimpleChannel.

**Spec:** README.md, GUI_SPEC.md, GUI_STATE_MACHINE.md, ART_BRIEFS.md in this directory.

## Global constraints

- Current main checkout; preserve unrelated changes; no task branch/worktree.
- Workers/Recruits only with installed respective mods; no replacement AI.
- Preserve native trade/Xaero, save UUIDs, coin registry IDs and ledger identity.
- Production builds/artifact synchronization authorized by standing project rules; no tests, fixtures, gameplay client or disposable server launches.
- Ordinary dedicated server may restart only for fresh artifact installation/native pack publication. No MineRENT/stable/baseline mutation.

## Shared interfaces

`TownViewPacket(boolean open, CompoundTag view)` remains the S2C envelope. Root fields: `Directory` compound-list (Id/Name/Dimension/Happiness/Residents), `Selected` compound (existing town view fields), `WorkersAvailable`, `RecruitsAvailable`, `GameTime`, optional `Request` UUID, `Result` enum name, `Reason` translation key. Existing founding fields retained. Selected view adds `Storage` rows, `Transactions`, `FoodCatalogue`, `Position` entity references, resident food-day counters and loaded entity ID where real.

`SettlementNetwork.action(String action, UUID townId, CompoundTag payload, UUID requestId)` sends a mutation/query; nullable townId allowed for open/create. `SettlementNetwork.requestOpen()` requests entry. `SettlementNetwork.supported()` is client endpoint presence. Actions: open/select/refresh/create/rename/delete/remove/expand/deposit/withdraw/payroll/foodstorage. Read requests use explicit IDs. Payload bounds server-owned. UI never implements gameplay state.

## Tasks

- [x] Currency: `Mods/JEMCurrencyCore`, required Economy source/resource/build modifications; one currency registration/save implementation and legacy facade, integrated settlement transactions, bundled dependency and existing funded trade earning route. Build production artifacts only.
- [x] Runtime: Settlement authority, optional compat subscriptions, ID-safe API, resident removal/death, vanilla/modded food, demand progression, gameplay persistence and truthful native worker/recruit actions. Own core packages except network/client.
- [x] GUI logic: client screens, real snapshot/actions and optional capability states. Native controls/materials used; authored artwork and visual acceptance remain pending.
- [x] Network (root): bounded explicit-ID action envelope, directory/selection, query/change projections, persistent operations, real map/food/payroll/storage/transaction data and native SMP entry delegation.
- [x] Integration preparation: independent source review, permitted production compile/reobfuscation, compiler/startup corrections, exact artifacts in ordinary Server/ManagedPack and native publication generation20. Atlas/history/docs and exact-path delivery are included with this implementation; Git history records the delivered commit. No player/visual acceptance is implied.

## Review focus

- Replayed/stale financial operations: original intent bound to saved receipt, no new request on timeout.
- Optional class absence: common reflective subscribers and DTO signatures cannot link native NPC/client classes.
- Multiple towns: every mutation explicitly validates selected UUID, no first-town fallback.
- World lifecycle: disconnect clears client state; saved IDs, debt, hostility and physical inventory remain authoritative.
- Geometry: inclusive rectangle union and newly added area×100 copper; renderer previews entire union and exact cost.

## Execution ledger

-2026-10-17: approved full implementation; user reiterates optional native worker/recruit ownership. Atlas before-change query and scan completed; unrelated Roles/sandbox warnings exist and are not this task's work.

-2026-10-17: final full artifacts compiled, synchronized and ordinary dedicated loaded; final native generation20 published with matching manifests. Dedicated stopped after saving. Custom artwork and real gameplay/visual acceptance remain separate pending stages.

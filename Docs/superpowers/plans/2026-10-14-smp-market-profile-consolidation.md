# Cohesive SMP Market, Profile and Command Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: use `superpowers:executing-plans` for native inline execution. This plan deliberately contains no test, Gradle, launch, deployment, commit or push steps because the current user request forbids them.

**Goal:** Turn `/smp` into the single cohesive player-facing entry point for Market, wallet, profile, settlements, recruits and event actions; remove obsolete command and land-claim surfaces without weakening their independent gameplay owners.

**Architecture:** JEMServer owns navigation and server-authorized read projections. JEMEconomy owns catalog policy, quotes, demand, wallet and physical-coin exchange. JEMSettlements and native Recruits remain the only settlement/workforce owners; JEMServer receives bounded read-only snapshots. Xaero remains the map owner. World Boss Tiers remains the revival owner. JEMClaims and Flan are removed from intended source/pack ownership only after event placement and protection no longer depend on them.

**Tech Stack:** Java 17, Forge 1.20.1/47.4.10, JEMServer SMP protocol, JEMEconomy wallet/market APIs, JEMSettlements snapshots, installed Recruits integration, Xaero World Map 1.46.0, Registry Atlas v2.

**Spec:** User-supplied 2026-10-14 Part B–K brief, with existing contracts in `Docs/smp-gui/README.md`, `Docs/DEVELOPMENT.md`, `Docs/economy/native-trading-design.md` and `AGENTS.logic-locks.md`.

## Global Constraints

- `/smp` remains the only public SMP root; player actions move to authenticated typed GUI requests before their old command is removed.
- The server re-quotes and validates every economy mutation. The client never supplies a price, account, reserve, quota, backing state or accepted result.
- Digital Wallet remains one copper-unit ledger. Five-denomination display is a lossless decomposition only; physical exchange consumes authentic backed lots and issues equally backed destination lots.
- Universal Market lists standardized ordinary goods only. Profession-specific or unique villager wares remain native and are not cloned into Market.
- Prepared meals stay excluded from the newly approved universal ingredient catalog. Existing entries that contradict this decision become `NONE` only after direct recipe/acquisition review; player shops remain unaffected.
- Settlements and Recruits projections are read-only for everyone, including owners. Native Bell, Town Hall, Recruits and other legitimate management mechanics remain unchanged.
- `Поселения` is the exact Russian label. `Магазины` means player-owned Spud shops; `Маркет` means the system-owned NPC commodity catalog.
- Home map viewing is read-only and private. `/home` retains its existing teleport behavior and safeguards.
- Boss arenas, event boundaries, terrain restoration and structure safety must not inherit land-claim removal. Settlement territory remains soft guidance only.
- No automated tests or fixtures; no Gradle, Minecraft launch, linter, formatter, static analysis, MineRENT/stable-profile mutation, commit or push in this task.

## Review Focus

1. A stale Market card or replayed packet must not consume items, debit twice, mint money or bypass demand/reserve/caps.
2. Physical denomination exchange must preserve exact value when inventory space is tight or delivery fails.
3. Offline profile views must never disclose Wallet balance, inventory or home coordinates to another player.
4. Removing JEMClaims/Flan must leave event placement, arena barriers, restoration and modded placement safety owned by their real systems.
5. No public command may disappear until every legitimate flow it carried has a real GUI request and server handler.

---

### Task 1: Freeze ownership, Atlas scope and exact runtime-GUI capability

**Files:**
- Create outside the repo only if still absent: `Taste-Engine/pairs/minecraft-runtime-gui/forge-1.20.1-jem-smp-screens/{TASTE.md,TECHNICAL.md}`.
- Modify later in this task: `Catalog/changes/records/economy-item-policies.json`, the owning SMP/claims records discovered by scan.

**Interfaces:** Produces the reviewed item-ID worklist, current uncovered-path list, protected behavior checklist and exact UI technical contract consumed by every later task.

- [x] Resolve the custom Forge SMP screen pair from the user's approved warm brown/copper/cream compact pixel-art brief and the actual `SmpScreen`, `SmpGuiAssets`, `JemPalette`, item/entity rendering and scissor contracts. Mark runtime screenshot verification pending rather than borrowing website taste.
- [x] Enumerate every `BUY_AND_SELL` policy and query `behavior` plus `impact` for each changed or newly approved ID. Record exact recipe inputs/outputs and unresolved machine/barter routes before deciding eligibility.
- [x] Run `python Catalog/generator/maintain_atlas.py scan` and keep unrelated dirty paths separate from this task's coverage.
- [x] Record the preserved contracts: native villager offers/restock; Wallet backing; Spud shop ownership; event/arena ownership; Xaero native UI/privacy; `/home` teleport semantics; read-only SMP settlements/recruits.

### Task 2: Canonical commodity policies and anti-loop review

**Files:**
- Modify `Mods/JEMEconomy/src/main/resources/data/jem_economy/economy/item_policies.json`.
- Modify only if necessary `Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/policy/ItemPolicyRegistry.java`.
- Modify `Catalog/changes/records/economy-item-policies.json`.

**Interfaces:** Existing per-item policy JSON remains the sole source for `outcome`, base buy/sell values, caps, category/demand group and notes. No parallel UI price table is permitted.

- [x] Classify each reviewed ID as crop, raw ingredient, processed intermediate, building resource, farmable drop, farmable valuable mineral or rare natural resource.
- [x] Apply selling caps from the brief: `64/2048`, `32/512`, `16/256`, `32/512`, `16/128`, `16/128`, `2/32` respectively. Do not treat those caps as purchase restrictions.
- [x] Exclude finished meals from the universal Market and preserve ingredients/intermediates only where exact recipes and acquisition are verified.
- [x] For every repeatable crafting, smelting, cutting, machine, barter, trade or container-return route, compare maximum demand-adjusted guaranteed NPC proceeds with minimum ingredient purchase cost for the exact recipe yield. Set the specific unsafe output to `NONE` or correct its verified price; do not inflate unrelated common goods.
- [x] Keep unresolved IDs fail-closed with the exact missing recipe/acquisition evidence in `notes` and the Atlas record.

### Task 3: Server-owned Market catalog, effective quotes and item tooltips

**Files:**
- Modify `Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/api/EconomyApi.java`.
- Create `Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/api/MarketCatalogSnapshot.java`.
- Create `Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/api/MarketItemSnapshot.java`.
- Modify `Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/market/MarketSavedData.java` and `policy/ItemPolicyRegistry.java`.
- Modify `Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/network/EconomyNetwork.java` and add bounded Market query/action packets beside it.
- Modify `Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/client/WalletClientState.java` and localization files.

**Interfaces:**
- `EconomyApi.marketCatalog(ServerPlayer, MarketQuery)` returns bounded ItemStack-based rows with effective buy/sell quote, category, availability, cap remainder and revision.
- Typed buy/sell requests contain request UUID, item registry ID, requested units and observed revision only; the server re-resolves policy and price.

- [ ] Build catalog rows from the canonical policy registry and real registered ItemStacks; never hardcode production offers on the client.
- [ ] Show effective demand-adjusted payout, not a base price mislabeled as final. Keep raw copper totals out of all components.
- [ ] Add one shared tooltip projection headed `Рынок`, using actual denomination ItemStacks for buy/sell and localized category. Suppress `NONE` tooltips by default; show compact unavailable text only in an explicit Market context.
- [ ] Bound search/category/page sizes, reject unknown IDs/negative units/stale revisions and refresh the catalog after every accepted mutation or daily demand rollover.

### Task 4: Exact Wallet and physical coin exchange contract

**Files:**
- Modify `Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/physical/PhysicalCoinService.java`, `CoinDenomination.java` and `EconomyApi.java`.
- Extend `EconomyNetwork.java` with one typed exchange request/result.
- Modify `WalletStatusPacket.java` or its current equivalent to carry denomination ItemStacks and exact decomposed balances without five stored balances.

**Interfaces:** `exchangePhysical(ServerPlayer, sourceDenomination, sourceCount, targetDenomination, requestId)` validates authentic backed lots, preserves copper value exactly and commits consume/delivery as one replay-safe operation.

- [ ] Permit only exact base-10 conversions among Copper, Silver, Gold, Aether and Arcane with no fee and no remainder loss.
- [ ] Treat digital display conversion as a no-op explanation; never write fake ledger transfers merely to change displayed denomination.
- [ ] Preflight inventory capacity for destination stacks, reserve the operation, consume only verified original coin lots, issue equally backed lots and roll back or retain source on delivery failure.
- [ ] Reuse the existing replay ledger and invalid/duplicate lot rejection; never create an unbacked physical item.

### Task 5: Compact Market screen inside SMP

**Files:**
- Create focused Market view components under `Mods/JEMServer/src/main/java/com/siirio/jemserver/client/smp/market/` for header, controls, product grid and exchange panel.
- Modify `SmpScreen.java`, `SmpRoute.java`, `SmpClientRequests.java`, `SmpClient.java`, `SmpGuiAssets.java`, `SmpIcons.java` and JEMServer localization.
- Reuse JEMEconomy ItemStacks/packets; do not duplicate Economy state in JEMServer.

**Interfaces:** SMP route `market` hosts `Витрина` and `Обмен монет`; Economy packets provide catalog/wallet state directly and authoritative mutation results.

- [ ] Render one compact header: half-body client-only villager portrait on the left, integrated tabs, and five real coin ItemStacks with counts on the right. Do not spawn or retain a server entity and do not add a second giant title.
- [ ] Add working search plus one dropdown with `Все`, `Еда`, `Ресурсы`, `Блоки`, `Дропы`.
- [ ] Render dense scrollable cards from real ItemStacks and server quotes with name, coin-icon price and actionable buy control; adapt columns/rows to GUI scale without overlap or unreadable labels.
- [ ] Implement the exchange tab from Task 4 with source/amount/target, exact receive preview, real sprites, resulting balance and confirmation.
- [ ] Preserve sharp pixel geometry, `JemPalette`, balanced pose/scissor state and semantic icons. No gradients, bloom, rounded web cards, fake controls or placeholder offers.

### Task 6: Sidebar and route consolidation

**Files:**
- Modify `SmpSidebar.java`, `SmpScreen.java`, `SmpRoute.java`, `SmpNetwork.java`, `SmpViews.java`, `SmpQuery.java` and localization.

**Interfaces:** Canonical routes are `home`, `profiles`, `players`, `parties`, `events`, `shops`, `market`, `settlements`, `recruits`, `prizes`, plus existing useful Roles/Professions/Quests/map/settings entry points when their real owners are available.

- [ ] Replace the current flat tab list with the requested Russian navigation and module-aware visibility; omit inactive routes rather than displaying dead buttons.
- [ ] Keep Shops and Market separate in routes, data, balances and permissions.
- [ ] Remove Claims/Territories navigation and all `ClaimsScreenBridge` paths.
- [ ] Preserve back/history/selection semantics and reuse one route model instead of adding per-screen navigation state.

### Task 7: Complete privacy-aware profile projection

**Files:**
- Modify `Profiles.java`, `SmpViews.java`, `SmpActions.java`, `SmpActionKind.java`, `SmpActionInput.java` and associated typed request records.
- Create narrow optional adapters under `smp/profile/` for Roles, Professions, Settlements, Recruits, Economy, Death History and TPA.
- Modify `SmpScreen.java`, `PlayerPreviewWidget.java` and focused profile renderer files split from the oversized screen.

**Interfaces:** `Profiles.view(viewer, target)` returns a bounded public projection plus owner-only capabilities; optional adapters contribute only verified fields and never reconstruct another module's state.

- [ ] Preserve all existing counters and add verified race, profession levels, settlement summary and recruit count. Unsupported values remain absent, never fabricated as zero.
- [ ] Render full-body skin, name, online state and compact scrollable statistic tiles without UUID or recent-achievement sections.
- [ ] Owner actions: view home, Wallet, Death History, Settlements, Recruits, Race, Professions. Other-player actions: message, TPA, party invite, shops, public settlement/recruit and permitted role/profession data.
- [ ] Enforce offline constraints and hide private balances, inventory and home coordinates from non-owners at the server projection layer, not only in rendering.

### Task 8: Read-only Home → Xaero navigation

**Files:**
- Modify `SmpActions.java` and add a bounded home-view response to `SmpNetwork`/typed packets.
- Modify `Mods/JEMServer/src/main/java/com/siirio/jemserver/client/XaeroLandmarks.java` and the existing Xaero screen-opening adapter.
- Preserve `HomeTravel.java` and `/home` execution unchanged.

**Interfaces:** Owner-only request resolves the target player's vanilla respawn dimension/position server-side and returns it only to that same player; client opens native Xaero and creates an ephemeral private focus marker.

- [ ] Read the actual respawn state, dimension and coordinates; do not create another home database.
- [ ] Reuse verified native Xaero opening/centering seams. If cross-dimension centering is not supported, open the best native view and show exact dimension/coordinates honestly.
- [ ] Never teleport from `Посмотреть дом`, publish the marker or persist it as a shared waypoint.

### Task 9: Read-only Settlements and Recruits projections

**Files:**
- Add compile-only access to actual JEMSettlements/native Recruits versions in `Mods/JEMServer/build.gradle` only after exact API/source inspection.
- Create `smp/settlements/SettlementProjection.java` and `smp/recruits/RecruitProjection.java` as read-only adapters.
- Modify `SmpViews.java`, `SmpScreen.java`, sidebar/icons/localization.

**Interfaces:** Server-authorized snapshots expose only fields owned by JEMSettlements/workforce and native Recruits; no mutation action kind exists for either route.

- [ ] Project the exact requested settlement fields, distinguishing physical storage, pending/quarantined production and permission-filtered treasury summaries.
- [ ] Project verified troop count, unit type/name/living state, morale, hunger, readiness and payroll fields; omit fields unavailable in the installed version.
- [ ] Provide scrolling information views with zero create/edit/pay/assign/order/equipment controls, including for the owner.
- [ ] Leave native Bell, Town Hall and Recruits management/AI untouched.

### Task 10: Replace player/event commands with GUI actions, then remove entry points

**Files:**
- Modify `SmpRuntime.java`, `EventCommands.java`, `EventEntryActions.java`, `BloodMoonVoting.java`, `PartyTeleportFlow.java`, `HostedParties.java`, `SmpActions.java` and `SmpViews.java`.
- Modify `CoinCommands.java`, `DeathHistory.java` and their registrations only after replacement actions are complete.
- Create a World Boss Tiers command branch beside `TierCommands` for `/jem boss revive`, registered by `JemWorldBossTiers.registerCommands`, so it exists in integrated singleplayer and dedicated servers through the real `BossRevivalApi` owner.
- Register `/jem event start|stop` from JEMServer's dedicated SMP event owner using the real event registry/scheduler.

**Interfaces:** All legitimate player mutations become typed `SmpActionKind` operations with the existing session, revision, replay and authorization checks. Admin commands use permission level 2 and exact event UUID targeting.

- [ ] Add real GUI flows for event vote, return/stay, leave, forfeit, invitations/party management, boss hosting and reward claims before deleting their command literals.
- [ ] Remove `/smp` tab/host/arena/boss/event-open/event-return/vote/forfeit/encounter-leave branches and obsolete `/smp event ...`; keep only `/smp` opener.
- [ ] Add `/jem boss revive`, `/jem event start <event>` with real registry suggestions/required optional parameters, and `/jem event stop <exact-event-id>`. Replace every `Sirio_o` check with operator permission.
- [ ] Keep `/home`. Remove public `/coins`, `/market`, `/death`, `/deaths` only after Wallet/Market/Death History GUI paths are functional. Preserve other untargeted native-mod commands.
- [ ] Keep boss/event safety, gates, cleanup, progression and recovery in their existing owners; command removal must not delete implementation methods still used by GUI actions.

### Task 11: Remove JEMClaims and Flan ownership without weakening independent safety

**Files:**
- Delete obsolete `Mods/JEMClaims/` source project after dependency migration.
- Delete JEMServer claim UI/backend/network/render files that no longer have an independent owner: `claims/`, `ClaimBoundaryNetwork.java`, `ClaimBoundaryGeometry.java`, `client/ClaimBoundaryClient.java`, `client/smp/ClaimsScreenBridge.java` and claim-only map/UI branches.
- Modify `JemServer.java`, `MapNetwork.java`, `MapClient.java`, `MapScreenMixin.java`, `EventRegions.java`, `EventRegistryExport.java`, `Mods/JEMServer/build.gradle`, `mods.toml` and Mixin/resource configuration.
- Modify canonical `Client/ManagedPack` manifests/configs to remove intended JEMClaims/Flan distribution only; do not touch stable profiles or runtime worlds.

**Interfaces:** Event region selection uses event/structure/world safety inputs directly. Boss arena protection remains World Boss Tiers/hosted encounter owned. Settlement boundaries remain non-protective guidance.

- [ ] Trace every claim call and classify it as obsolete ordinary claim behavior or independently required event/arena/placement safety.
- [ ] Move only genuine independent safety to its existing owner with bounded data; do not retain a renamed generic claim backend.
- [ ] Remove Claims/Flan registrations, commands, network channels, overlays, configs and pack dependencies. Preserve SavedData/world files in place; absence of a reader is not authorization to delete player world data.
- [ ] Confirm source references and intended pack metadata contain no active JEMClaims/Flan dependency while boss/event/terrain contracts still have an owner.

### Task 12: Documentation, Atlas rebuild and source-only completion review

**Files:**
- Modify `Docs/smp-gui/README.md`, `Docs/DEVELOPMENT.md`, `Docs/SERVER_COMMANDS.md`, `Docs/JEM_SMP_COMMANDS.docx` only if the document can be safely regenerated without claiming runtime verification, `Docs/MODS.md`, `Docs/TWEAKED_MODS.md`, `CHANGELOG.md`.
- Modify the smallest applicable `Catalog/changes/records/*.json`; rebuild `Catalog/effective/index.json` and related generated coverage only through permitted Atlas tools.

- [ ] Update canonical docs so Market/Profile/Settlements/Recruits/commands/claims have one current owner and obsolete claims/navigation text is removed rather than contradicted.
- [ ] Add same-task Atlas history with `SELF`, before/after behavior, direct/indirect systems, removed source paths and unresolved recipe/native API evidence.
- [ ] Run `maintain_atlas.py rebuild`, `audit`, `scan` and `validate_atlas.py`; re-query every changed policy ID and inspect recipe impacts/conflicts.
- [ ] Perform source-only review: required command literals absent/present as specified, server authorization present for every new action, client screens contain no direct mutations, no duplicate policy map, no claim dependency, no placeholder UI data and no unrelated dirty files changed.
- [ ] Update `CHANGELOG.md` with user decisions versus implementation choices and state the boundary honestly: source/Atlas review only, with builds, runtime rendering, gameplay acceptance, packaging, commit, push and deployment unperformed.

## Execution Boundary

After approval, execute tasks in order in the current checkout while preserving unrelated dirty files. Stop and report rather than inventing values if an exact item ID, recipe/acquisition route, installed-mod API or Xaero centering capability cannot be proved from local evidence. The final summary must enumerate real source changes against all twelve user-requested deliverable categories and explicitly list unresolved IDs/native limitations.

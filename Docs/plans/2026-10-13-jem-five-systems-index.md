# JEM Five-System Implementation Plan Index

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` and implement these plans in dependency order. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the five approved specifications from `JEM_5_CODEX_PROMPTS_ALL_CLARIFICATIONS_FINAL.zip` as owner-aligned Forge 1.20.1 modules and integrations without creating a monolithic RPG core.

**Architecture:** One small shared principal owner (`JEMIdentity`) and four gameplay domain owners (`JEMRoles`, `JEMProfessions`, `JEMEconomy`, `JEMSettlements`) expose narrow immutable APIs and Forge events. Existing owners remain authoritative for their domains: `JEMServer` for SMP shops, map overlays, events and server UX; `JEMWorldBossTiers` for world-tier combat scaling; `JEMPackCore`/KubeJS for pack content; native mods for their own AI and mechanics.

**Tech Stack:** Java 17, Forge 1.20.1/47.4.10, Mojmap, Forge `SavedData`, SimpleChannel, existing JEM menu/rendering primitives, JSON/TOML resources.

**Specs:** `C:/Users/user/Downloads/JEM_5_CODEX_PROMPTS_ALL_CLARIFICATIONS_FINAL.zip`

## Global Constraints

- Do not create or run any test class, test source tree, fixture, mock, test script, test framework, GameTest, smoke workflow or runtime test scenario. Compilation of affected production modules is allowed and must exclude test tasks; do not launch Minecraft, lint, format or run static analysis.
- Roles and Professions register and persist normally in integrated singleplayer. Economy, Settlements, shared-server events and other SMP-owned gameplay remain dedicated-server-only; clients retain only join/render/network compatibility.
- Preserve `AGENTS.logic-locks.md`, existing world schemas, registry IDs, the 12 Eyes campaign, hosted encounter lifecycle, current map protocol-v5 work and the existing SMP GUI visual contract.
- `online-mode=false` and `white-list=false` are the current server truth, but they do not by themselves disable gameplay. Inspect and reuse the server authentication owner. Dedicated-server account-bound writes require one connection-bound authenticated principal; the approved provider is Server Login's exact `AuthGate.isLocked(ServerPlayer)` seam. Store by authenticated UUID, never display name, and fail closed only while that live connection is unauthenticated or the provider is unavailable. Integrated singleplayer trusts the local owner UUID for Roles and Professions only.
- No module may reconstruct another module's state. Optional integrations activate only after exact `ModList` detection and isolated class loading.
- No direct synchronization into active AutoModpack profiles; no deployment, staging, publication, baseline promotion or MineRENT mutation belongs to these plans.
- All player-facing strings are present in `en_us` and `ru_ru`.
- The existing user changes in `MapNetwork.java`, `MapClient.java`, event docs and `Docs/smp-gui/README.md` are preserved and extended in place.

## Dependency Order

0. Acquire and inspect the server-side authentication provider, then establish the shared principal boundary consumed by every account-owning module.
1. [RPG Roles](2026-10-13-jem-rpg-roles.md) — race owner and role events.
2. [Professions](2026-10-13-jem-professions.md) — player progression owner; consumes Roles optionally.
3. [Economy](2026-10-13-jem-economy.md) — account, ledger, five backed physical denominations, physical-outstanding, escrow and funded transaction owner.
4. [Settlements](2026-10-13-jem-settlements.md) — town/workforce/happiness owner; consumes Economy optionally.
5. [Integrations and Events](2026-10-13-jem-integrations-events.md) — owner-specific adapters, Xaero project regions, FTB content, holidays, Iron integration and peaceful activities.

## Fixed Reuse Decisions

- Do not add Origins. Three custom modest races, the JEM confirmation/grace workflow and the required compatibility rules would replace most of Origins' default selection/power behavior. `JEMRoles` is smaller and keeps one owner.
- Keep installed Pehkui `3.8.2` optional. Physical dwarf scaling remains disabled by default; only an isolated adapter may enable it after a future explicitly requested compatibility exercise.
- Do not add Lightman's Currency. JEM requires one fixed supply shared by digital Wallet balances and five backed physical denominations, plus cross-item escrow and connection-authenticated writes; a second upstream balance owner would split that authority.
- Do not require Pufferfish Skills. `JEMProfessions` owns progression and uses the existing JEM UI primitives; a future display-only adapter may be added without moving XP ownership.
- Preserve installed Spud's Shops `1.10.2` physical blocks and stock. JEMServer keeps the shop projection/mixins while `JEMEconomy` exclusively owns Wallet and validated inventory-cash settlement, fees, change and conservation.
- Add the official Forge 1.20.1 Recruits `1.15.2`, Workers 2 `2.0.3`, Xaero Map Regions `1.0.8`, FTB Quests `2001.4.22` stack and Iron's Spells `3.15.6` stack during the integrations phase, then inspect their exact JAR metadata/classes before writing adapters. Iron `3.16.3` is intentionally not selected because its published 1.20.1 artifact has an open resource-generation regression; `3.15.6` is the latest 3.15.x release matching the approved data-driven contract.

## Cross-Module Public Contracts

```java
// JEMRoles owner
Optional<RaceSnapshot> RaceApi.get(MinecraftServer server, UUID accountId);

// JEMProfessions owner
ProfessionProgressResult ProfessionApi.credit(ServerPlayer player, ProfessionProgressRequest request);

// JEMEconomy owner
TransferResult EconomyApi.transfer(MinecraftServer server, TransferRequest request);
EscrowResult EconomyApi.reserve(MinecraftServer server, EscrowRequest request);

// JEMSettlements owner
Optional<TownSnapshot> SettlementApi.town(UUID townId);
Optional<WorkforceSnapshot> SettlementApi.worker(UUID npcId);
```

Each snapshot is immutable. Each mutation contains a bounded server-generated idempotency key and rejects an unsafe identity before touching persistent state.


# Evidence-backed findings

Runtime environment is versioned per case. Sources and static inclusion are never a system-wide gameplay pass.

## JEMQA-20261014-0013

**HIGH — Bundled guide children still reference the explicitly retired boss root**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T19:25:57.297356+00:00; latest: 2026-10-13T20:30:58.890561+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Start freshly built Pack Core in a disposable world; compare orphaned-guide-excerpts.txt and bundled resources with ServerAdvancementManagerMixin.

**Expected player experience:** Retained story/guide content is reachable beneath current campaign ownership without restoring jem_guide:bosses/root.

**Actual behavior and impact:** The isolated fresh runtime rejects 11 bundled jem_guide advancements whose parentId is jem_guide:bosses/root. Guide filter discards the root but admits owned bundled children without removing their retired parent.

**Established cause / stated uncertainty:** Five whys: guide nodes fail loading -> parent is missing -> retired root is deliberately filtered -> isOwnedNamespace branch admits every bundled child without removeRetiredBossBranch -> removal/reparent lifecycle was applied to foreign branches but not surviving owned branches.

**Proposed owner correction and native reuse:** Audit which exact bundled children remain approved; reparent surviving content to current campaign contract or remove obsolete children. Never restore the retired root or reparent native foreign roots into JEM.

**Acceptance:** No orphan-parent errors for retained bundled guide content; exact campaign graph and native root locks preserved; player can discover/earn retained nodes in Survival.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** `advancement:jem_guide:bosses/3_2`, `advancement:jem_guide:bosses/root`

**Source/evidence links:**

- [EVIDENCE/logs/orphaned-guide-excerpts.txt](../../EVIDENCE/logs/orphaned-guide-excerpts.txt)
- [Mods/JEMPackCore/src/main/java/com/justenoughmods/achievementguide/mixin/ServerAdvancementManagerMixin.java:35](../../../../../Mods/JEMPackCore/src/main/java/com/justenoughmods/achievementguide/mixin/ServerAdvancementManagerMixin.java)
- [Mods/JEMPackCore/src/main/resources/data/jem_guide/advancements/bosses/3_2.json](../../../../../Mods/JEMPackCore/src/main/resources/data/jem_guide/advancements/bosses/3_2.json)

## JEMQA-20261014-0033

**HIGH — FTB Teams local identity lookup uses the launcher UUID; quest-book barrier has a separate unresolved gate**

Status: **OPEN** · Evidence: **IN_GAME_OBSERVED**. First recorded observation: 2026-10-13T21:36:48.759072+00:00; latest: 2026-10-13T21:50:54.925142+00:00. Recheck: REPRODUCED: two native quest button attempts; candidate repair pending.

**Mode/environment:** Authenticated dedicated Survival / exact installed FTB Teams2001.3.2, Library2001.2.13, Quests2001.4.22; no admin XP

**Attempt/reproduction:** Authenticated Survival test jem on offline-mode loopback clone: E -> native red Quests button (hover label Квесты). Retry produces no quest screen. Native client log contains exact UUID mismatch.

**Expected player experience:** After normal local registration, the visible native Quests inventory button opens the installed JEM guidance.

**Actual behavior and impact:** Two actual Quests button attempts close inventory and give a barrier/error toast instead of opening the quest book. Native FTB Teams log says launcher profile918bacc0-970d-445c-a0cc-922e59fd57ba is absent from known-player map, which correctly contains server profile5cd61f31-75aa-3676-bd2a-c2413e2cd3be. Exact installed ClientTeamManagerImpl.initSelfDetails(UUID) sets selfTeam from received server ID but fetches selfKnownPlayer using Minecraft.getUser().getGameProfile().getId(), leaving it null. Normal clone registration has completed; this is not the old auth-lock status. Independent review corrected the causal claim: ClientQuestFile.openQuestGui gates disable_gui or selfTeamData.isLocked; syncTeamData assigns self data without reading Teams.selfKnownPlayer. Native Quests barrier is observed but its connection to the proven Teams UUID defect is unestablished. Canonical disable_gui=false; initial Loading self-data is locked. A normal authenticated reload/request comparison is next.

**Established cause / stated uncertainty:** Proven five-whys chain applies to FTB Teams selfKnownPlayer only: native self lookup uses launcher UUID instead of the existing joined profile and misses the correct server-keyed record. The Quests barrier may be lost/locked self-data synchronization; the owning request/auth lifecycle still requires evidence. Do not claim the identity adapter alone will open Quests.

**Proposed owner correction and native reuse:** Inspect FTB public API and packet timing. Prefer joined connection GameProfile for the local client view using a narrow optional client adapter if native seams cannot express this. Do not alter authoritative UUIDs, account data, security, server team state or launcher/stable-profile metadata.

**Acceptance:** FTB Teams selects existing joined-profile known-player record without changing auth/team authority. Independently establish and repair the Quests lock/request lifecycle; ordinary quest UI must be replayed on fresh legitimate login/reconnect, without clearing locks as fallback.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** `minecraft:player`

**Source/evidence links:**

- [EVIDENCE/logs/ftb-client-identity-mismatch.txt](../../EVIDENCE/logs/ftb-client-identity-mismatch.txt)
- [EVIDENCE/traces/ftb-init-self-bytecode.txt](../../EVIDENCE/traces/ftb-init-self-bytecode.txt)
- Canonical lookup for `minecraft:player`: [Catalog/registries/items/minecraft.json](../../../../../Catalog/registries/items/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:player`: [Catalog/registries/blocks/minecraft.json](../../../../../Catalog/registries/blocks/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:player`: [Catalog/registries/entities/minecraft.json](../../../../../Catalog/registries/entities/minecraft.json) (registry identity must be respected).

## JEMQA-20261014-0034

**MEDIUM — Russian newcomer guide opens with English engineering prose instead of initial player actions**

Status: **OPEN** · Evidence: **IN_GAME_OBSERVED**. First recorded observation: 2026-10-13T23:36:57.005040+00:00; latest: 2026-10-13T23:36:57.005047+00:00. Recheck: NOT_FIXED.

**Mode/environment:** Actual native Russian-locale Quests GUI

**Attempt/reproduction:** Open native Quests after actual ftbquests reload, read first page with Russian client locale.

**Expected player experience:** A Russian newcomer learns concrete first steps and actual dedicated-server progression without understanding module architecture.

**Actual behavior and impact:** First Welcome page is English and explains one owner per fact, Roles/Professions solo operation and ownership of inventory/money/races. Current dedicated-only policy disagrees with its solo claim. No completion/rewards clicked.

**Established cause / stated uncertainty:** Authored literal English description in getting_started.snbt, no localization projection for first page; stale solo claim.

**Proposed owner correction and native reuse:** Replace engineering copy with concise localized player actions and native links matching current dedicated-only rules, preserving guide-only ownership and no rewards.

**Acceptance:** Fresh Russian player can follow first actions without external module docs; guide does not advertise unsupported solo gameplay.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** Artifact/registry-wide; no invented individual object ID.

**Source/evidence links:**

- [Client/ManagedPack/config/ftbquests/quests/chapters/getting_started.snbt:11](../../../../../Client/ManagedPack/config/ftbquests/quests/chapters/getting_started.snbt)
- [EVIDENCE/screenshots/actual/022-first-guide-english-engineering-prose.png](../../EVIDENCE/screenshots/actual/022-first-guide-english-engineering-prose.png)

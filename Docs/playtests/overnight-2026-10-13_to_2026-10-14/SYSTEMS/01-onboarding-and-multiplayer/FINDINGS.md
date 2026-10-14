# Evidence-backed findings

Runtime environment is versioned per case. Sources and static inclusion are never a system-wide gameplay pass.

## JEMQA-20261014-0037

**MEDIUM — Town sidebar gives a hidden no-town chat line and no founding action**

Status: **OPEN** · Evidence: **IN_GAME_OBSERVED**. First recorded observation: 2026-10-14T00:15:21.077229+00:00; latest: 2026-10-14T00:15:21.077242+00:00. Recheck: NOT_FIXED.

**Mode/environment:** Actual NON-OP first-town flow

**Attempt/reproduction:** No owned town; click Town in /smp, close menu and inspect chat history.

**Expected player experience:** A newcomer clicking Town gets a visible empty state explaining how to found a settlement.

**Actual behavior and impact:** Menu stays on Events. Only no-settlement chat line appears behind it; after closing/opening chat the message supplies no next step.

**Established cause / stated uncertainty:** SmpSidebar sends jem town but leaves screen open; SettlementCommands.status sends only jem.settlements.none chat for absent town.

**Proposed owner correction and native reuse:** Project an explicit native no-town response into visible Town empty state with localized bell-first instructions; preserve server authority, no guessed town/automatic creation.

**Acceptance:** Newcomer learns real bell interaction and owner creation requirement from Town route itself.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** Artifact/registry-wide; no invented individual object ID.

**Source/evidence links:**

- [Mods/JEMServer/src/main/java/com/siirio/jemserver/client/smp/SmpSidebar.java:54](../../../../../Mods/JEMServer/src/main/java/com/siirio/jemserver/client/smp/SmpSidebar.java)
- [Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/command/SettlementCommands.java:67](../../../../../Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/command/SettlementCommands.java)
- [EVIDENCE/screenshots/actual/040-town-sidebar-no-visible-empty-state.png](../../EVIDENCE/screenshots/actual/040-town-sidebar-no-visible-empty-state.png)
- [EVIDENCE/screenshots/actual/041-town-no-owned-town-chat.png](../../EVIDENCE/screenshots/actual/041-town-no-owned-town-chat.png)

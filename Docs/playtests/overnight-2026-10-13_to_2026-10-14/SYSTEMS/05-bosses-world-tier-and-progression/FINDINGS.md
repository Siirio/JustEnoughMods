# Evidence-backed findings

Runtime environment is versioned per case. Sources and static inclusion are never a system-wide gameplay pass.

## JEMQA-20261014-0028

**HIGH — Actual boss participant death is cancelled by the legacy fallback when PlayerRevive is absent**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T20:02:13.162065+00:00; latest: 2026-10-13T20:30:59.243080+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** In an explicitly isolated optional-mod-absence scenario with two real authenticated players, cause actual lethal damage during hosted group fight. Inspect cancellation, restored health and participant state; do not use fake multiplayer.

**Expected player experience:** Actual LivingDeathEvent immediately eliminates a participant without hidden lives, restored health or JEM post-death downing, even when an optional revive mod is unavailable.

**Actual behavior and impact:** HostedEncounters.death retains a branch for !ModList.isLoaded(playerrevive), group size >1, ALIVE and unused downing: it cancels LivingDeathEvent, sets DOWNED, starts bleedout and restores health to 1. Installed current pack includes PlayerRevive, so that branch is conditional and not claimed exercised tonight. Docs permits native installed pre-death downing, not cancellation of actual death.

**Established cause / stated uncertainty:** Five whys: dead participant remains alive -> event is cancelled and health restored -> old fallback substitutes JEM downing when native revive mod is missing -> latest death contract changed only the normal elimination path -> optional-dependency branch retained an obsolete second-life owner.

**Proposed owner correction and native reuse:** Remove/reconcile the legacy actual-death cancellation at the hosted encounter owner while retaining only approved native pre-death revive behavior; review all consumers of DOWNED/bleedout/revive markers.

**Acceptance:** Actual death always remains uncancelled and becomes eliminated; optional mod absence adds no hidden life or health; approved native pre-death downing remains distinct.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** Artifact/registry-wide; no invented individual object ID.

**Source/evidence links:**

- [AGENTS.md](../../../../../AGENTS.md)
- [Docs/events/BOSS_FIGHT.md:11](../../../../events/BOSS_FIGHT.md)
- [Mods/JEMWorldBossTiers/src/main/java/com/siirio/jemworldbosstiers/encounter/HostedEncounters.java:385](../../../../../Mods/JEMWorldBossTiers/src/main/java/com/siirio/jemworldbosstiers/encounter/HostedEncounters.java)

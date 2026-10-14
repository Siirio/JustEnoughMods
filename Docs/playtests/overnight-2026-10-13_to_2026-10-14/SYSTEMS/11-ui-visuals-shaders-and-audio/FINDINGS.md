# Evidence-backed findings

Runtime environment is versioned per case. Sources and static inclusion are never a system-wide gameplay pass.

## JEMQA-20261014-0024

**MEDIUM — Expanded profession card grids have fixed sizes without viewport scrolling or text wrapping**

Status: **OPEN** · Evidence: **IN_GAME_OBSERVED**. First recorded observation: 2026-10-13T19:44:41.823737+00:00; latest: 2026-10-13T21:30:19.625275+00:00. Recheck: REPRODUCED: horizontal text overflow; other resolutions pending.

**Mode/environment:** Authenticated dedicated-server Survival, fresh clone-only native account; no administrative XP/gear grants. Time set day after first death.

**Attempt/reproduction:** Open overview/tree at default/max GUI scales and smaller window; compare all ten cards, long RU labels and bottom controls.

**Expected player experience:** Profession names/actions remain readable and reachable at ordinary Minecraft GUI scales.

**Actual behavior and impact:** ProfessionsScreen requires 386 logical pixels width and 245 pixels of card rows; with its minimum startY 38 the last row reaches 277. ProfessionTreeScreen uses fixed 116px cards, draws long translated skill text with unwrapped drawString and has no viewport scroll/scissor/layout adjustment. These are demonstrable source bounds; actual screenshots at supported GUI scales remain blocked. Actual maximized test jem, configuredGUI4/ru_ru: Architect titles/state text cross card boundaries and cover neighboring columns. All ten profession overview cards fit at this specific configuration; vertical loss at smaller viewports remains source-only.

**Established cause / stated uncertainty:** Fixed source pixel grids are not constrained to scaled window dimensions; long content has no wrapping/overflow contract. Actual overlap depends on font/viewport and must be captured before redesign.

**Proposed owner correction and native reuse:** Fit or scroll the actual content viewport, wrap/shorten visible labels and keep actionable hover text separate; preserve server-owned skill/toggle validation. Exact custom-game-GUI taste pair is missing, so do not invent subjective art direction tonight.

**Acceptance:** Every profession/node/action remains reachable and readable at native supported scales/resolutions in RU/EN; actual before/after captures show no clipped card or inaccessible control.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** Artifact/registry-wide; no invented individual object ID.

**Source/evidence links:**

- [Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/client/ProfessionsScreen.java:16](../../../../../Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/client/ProfessionsScreen.java)
- [Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/client/ProfessionTreeScreen.java:18](../../../../../Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/client/ProfessionTreeScreen.java)
- [EVIDENCE/screenshots/actual/013-architect-tree-overflow-tooltip.png](../../EVIDENCE/screenshots/actual/013-architect-tree-overflow-tooltip.png)
- [EVIDENCE/screenshots/actual/014-architect-tree-overflow-no-hover.png](../../EVIDENCE/screenshots/actual/014-architect-tree-overflow-no-hover.png)

## JEMQA-20261014-0032

**HIGH — Player avatar missing in inventory and third person with non-finite EMF animation state**

Status: **OPEN** · Evidence: **IN_GAME_OBSERVED**. First recorded observation: 2026-10-13T21:30:19.666480+00:00; latest: 2026-10-13T21:30:19.666499+00:00. Recheck: REPRODUCED symptom; causal isolation pending.

**Mode/environment:** Authenticated dedicated-server Survival, fresh clone-only native account; no administrative XP/gear grants. Time set day after first death.

**Attempt/reproduction:** Join the fresh clone with the current native pack, remain stationary during initial login; open E after respawn and press F5. Observed current player health20/Survival.

**Expected player experience:** A living Survival player is visible in inventory and third person with the installed detailed-animation pack.

**Actual behavior and impact:** AtGUI4/ru_ru, inventory player area is blank and actual F5 camera shows no body. Native EMF3.2.4 logs var.body_rx=NaN, invalid body.rx and invalid cape.rx. Installed player.jem/player_cape.jem hashes equal canonical. Source allows SprintingVactor0 at unloaded/no-new-frame initialization but divides by it in body/leg translations; cape independently divides BodyHeightVelocity by frame_time0. The exact upstream variable origin/pack-isolation causal link remains pending; do not attribute every symptom to this candidate yet. Third-person body recovered after0.6second crouch without asset/code changes; inventory preview remains blank after actual movement. Screenshot023 and029 narrow the observation; no universal recovery or repair claim.

**Established cause / stated uncertainty:** First demonstrated incorrect state is native EMF var.body_rx=NaN. Candidate earlier owner: animation-state initialization permits a zero denominator before its smoothing state has started. Missing avatar is directly observed; causal isolation and earliest variable values must be established before a fix.

**Proposed owner correction and native reuse:** Inspect exact installed EMF evaluator and disable the detailed player model through native UI as a controlled comparison. If confirmed, initialize the natural sprint factor at its native equilibrium1 before divisions and define body-height velocity only for a positive elapsed frame; preserve existing animation timing/style.

**Acceptance:** Fresh join, stationary login, inventory, third person, movement, sprint, sneak, cape and repeat resource reload show a visible model without NaN; complete native update delivers any final canonical fix.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** `minecraft:player`

**Source/evidence links:**

- [EVIDENCE/screenshots/actual/010-survival-inventory-after-respawn.png](../../EVIDENCE/screenshots/actual/010-survival-inventory-after-respawn.png)
- [EVIDENCE/screenshots/actual/011-third-person-player-invisible.png](../../EVIDENCE/screenshots/actual/011-third-person-player-invisible.png)
- [EVIDENCE/logs/player-emf-failure.txt](../../EVIDENCE/logs/player-emf-failure.txt)
- [Client/ManagedPack/resourcepacks/JEM Detailed Animations Fix/assets/minecraft/emf/cem/player.jem:51](../../../../../Client/ManagedPack/resourcepacks/JEM Detailed Animations Fix/assets/minecraft/emf/cem/player.jem)
- [Client/ManagedPack/resourcepacks/JEM Detailed Animations Fix/assets/minecraft/emf/cem/player_cape.jem:25](../../../../../Client/ManagedPack/resourcepacks/JEM Detailed Animations Fix/assets/minecraft/emf/cem/player_cape.jem)
- [EVIDENCE/screenshots/actual/023-avatar-recovers-after-native-crouch.png](../../EVIDENCE/screenshots/actual/023-avatar-recovers-after-native-crouch.png)
- [EVIDENCE/screenshots/actual/029-inventory-avatar-still-blank-after-walking.png](../../EVIDENCE/screenshots/actual/029-inventory-avatar-still-blank-after-walking.png)
- Canonical lookup for `minecraft:player`: [Catalog/registries/items/minecraft.json](../../../../../Catalog/registries/items/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:player`: [Catalog/registries/blocks/minecraft.json](../../../../../Catalog/registries/blocks/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:player`: [Catalog/registries/entities/minecraft.json](../../../../../Catalog/registries/entities/minecraft.json) (registry identity must be respected).

## JEMQA-20261014-0036

**MEDIUM — SMP event hover uses a single horizontal tooltip that runs off both viewport edges**

Status: **OPEN** · Evidence: **IN_GAME_OBSERVED**. First recorded observation: 2026-10-14T00:15:21.070172+00:00; latest: 2026-10-14T00:15:21.070179+00:00. Recheck: NOT_FIXED.

**Mode/environment:** Actual non-op native SMP GUI

**Attempt/reproduction:** Open native /smp and hover Fishing card.

**Expected player experience:** The complete event objective is readable when hovering a truncated event card.

**Actual behavior and impact:** Fishing description tooltip is one long line across and beyond both screen edges atGUI4; objective details cannot be read. Cards themselves have deliberate ellipsis and semantic icons.

**Established cause / stated uncertainty:** SmpScreen renders hoveredLabel through single-Component tooltip overload without width-bounded font.split.

**Proposed owner correction and native reuse:** Preserve current card styling, wrap hover content to actual available GUI width and constrain placement; short labels unchanged.

**Acceptance:** Complete long Russian objective readable atGUI4 and smaller native viewports without crossing edges.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** Artifact/registry-wide; no invented individual object ID.

**Source/evidence links:**

- [Mods/JEMServer/src/main/java/com/siirio/jemserver/client/smp/SmpScreen.java:417](../../../../../Mods/JEMServer/src/main/java/com/siirio/jemserver/client/smp/SmpScreen.java)
- [EVIDENCE/screenshots/actual/039-native-smp-hub-and-unwrapped-event-hover.png](../../EVIDENCE/screenshots/actual/039-native-smp-hub-and-unwrapped-event-hover.png)

# Xaero Native Presentation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking. Reviewed and approved2026-10-14; user selected native inline execution and immediate push. Source implemented; compilation/runtime/visual verification remain unperformed under the project execution prohibition.

**Goal:** Restore the native Xaero appearance while retaining optional JEM territories, projects and events.

**Architecture:** Keep installed Xaero GuiMap as the screen owner. Local preferences gate only JEM rendering and project input; authoritative snapshots and private waypoint ownership remain unchanged.

**Tech Stack:** Java17, Forge1.20.1/47.4.10, installed Xaero World Map1.46.0 and Minimap26.4.2, existing narrow GuiMap Mixin.

**Spec:** [Approved design](../../economy/native-trading-design.md), especially Xaero and delivery sections. This workstream is independent of [native trading](2026-10-14-native-trading.md).

## Global Constraints

- Native GuiMap remains owner of layout, keys, dropdown, scale and waypoint.
- Additional polygons default hidden until enabled; shared terrain and boss locations remain available.
- No world-space event-wall changes; no changes to permissions or personal map-data sharing.
- No automated tests, fixtures or runtime scenarios. No Gradle, game launches, linters, formatters or static analysis without an explicit current-turn request.
- Do not mutate stable profiles, active AutoModpack installations, world saves or the release baseline.
- Resolve exact vanilla-GUI taste/technical knowledge before visual implementation; existing resource-pack taste cannot substitute for runtime GuiMap knowledge.

## Review Focus

1. Hidden projects must release mouse ownership even during a drag.
2. Overlapping regions at low zoom must leave terrain and native waypoints readable.
3. Preferences persist across reconnect; snapshots and drafts do not leak across servers.
4. Missing Xaero must not load its classes or crash the installed client.
5. Visibility must not change server actions, access or discovery/privacy contracts.

### Task 1: Visibility and native input ownership

**Files:**
- Create `Mods/JEMServer/src/main/java/com/siirio/jemserver/client/xaero/MapLayerPreferences.java` — client-only visibility, local persistence.
- Modify `Mods/JEMServer/src/main/java/com/siirio/jemserver/client/xaero/MapClient.java` — visibility-gated projection only.
- Modify `Mods/JEMServer/src/main/java/com/siirio/jemserver/client/xaero/ProjectRegionClient.java` — cancel unfinished local draft.
- Modify `Mods/JEMServer/src/main/java/com/siirio/jemserver/mixin/xaero/MapScreenMixin.java` — native dropdown toggles and guarded project input.
- Modify `Mods/JEMServer/src/main/resources/assets/jem_server/lang/{en_us,ru_ru}.json` — labels for the three visibility choices.

**New interfaces:**
```java
// MapLayerPreferences: local config only, not network state.
public enum Layer { TERRITORIES, PROJECTS, EVENTS }
public static boolean visible(Layer layer);
public static void setVisible(Layer layer, boolean visible);
// ProjectRegionClient: resets project, dimension, planning and dragging.
public static void cancel();
```

- [x] Read the exact installed GuiMap dropdown/API and existing client-config conventions before editing. Record the available public seam; retain the current return-list Mixin only where required.
- [x] Resolve an exact Taste Engine pair for native Xaero extension, grounded in the approved original-interface brief and installed resources. No new art or unrelated restyling.
- [x] Run Atlas lookup and before-change impact scan; include map rendering/privacy owners and actual installed-mod versions.
- [x] Store three booleans in a dedicated client config using Forge's client config mechanism, all false by default. Do not write native Xaero settings/waypoints. Report an actual persistence failure instead of silently changing ownership.
- [x] Append three localized visibility actions to the existing native right-click options; do not replace options or open a replacement map/settings screen. Toggle label reflects current state.
- [x] Gate `MapClient.render` separately for territory, project and event layers. Hidden projects skip draft rendering and call `cancel()` on hide/logout. `cancel()` clears all local planning fields without network mutations.
- [x] Guard press/release/move by connected state and project visibility. Entering an explicit new/extend project action enables the project layer before beginning its draft. Otherwise leave native mouse handling untouched.
- [x] Retain authoritative event/claim/project queries for existing actions; toggles do not hide permissions or change server snapshots.
- [x] Review the actual diff against hidden-drag, reconnect and missing-Xaero conditions; do not claim an unrun game check passed.

### Task 2: Quiet overlay drawing and completion

**Files:** `MapClient.java`, `MapScreenMixin.java`, `Docs/smp-gui/README.md`, `CHANGELOG.md`, applicable `Catalog/changes` and `Catalog/effective` records.

**Interface:** Extend the existing `MapClient.render(...)` signature with native hover block coordinates, passed from existing GuiMap shadows. Keep dimension/camera/scale ownership in GuiMap.

- [x] Remove persistent polygon fills from enabled JEM layers; draw one GUI-pixel borders from existing union meshes. Keep meshes cached on snapshot changes, not recomputed per frame.
- [x] Replace always-on event/project labels with a single hovered-region label, using existing `event(...)`/`project(...)` queries and native font/tooltip conventions. Bound the label to the viewport and preserve dropdown draw order.
- [x] Leave `XaeroLandmarks`, terrain transport, native waypoint files and world boundary geometry untouched. Preserve snapshot request cadence and existing server validation.
- [x] Review rendering coordinates under GUI scaling, negative camera positions, low zoom and overlapping regions. Runtime visual acceptance remains pending an explicit launch/check request.
- [x] Update the canonical map document and Atlas change/effective history; run the mandatory Atlas validation, not Gradle or automated tests.
- [x] Record `Bug / Client / unconditional JEM overlay fill and labels without visibility controls / in testing`, naming visibility gating, outlines and hovered labels as the mechanism. Mark resolved only after user acceptance.
- [x] Review task-only diff, commit and push source/docs/Atlas on the intended branch. Do not copy source-only work into runtime JARs or call it deployed.

## Later explicit acceptance and packaging

When the user requests those activities, use only `test jem` for local client checks: open native Xaero, pan/zoom and use native waypoint controls; enable each layer; hide projects during a drag; reconnect and verify persistence; confirm native private waypoints remain private. Build/package only on an explicit build request and publish only on an explicit release request. Source completion and observed runtime success are separate claims.

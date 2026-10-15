# Approved Logic Locks

## LOCKED: Native merchant and Xaero presentation
- Scope: JEMEconomy merchant runtime/menu/client screen, MarketRuntime; JEMServer client/xaero/MapClient and mixin/xaero/MapScreenMixin; native MerchantMenu/MerchantScreen and Xaero GuiMap integration.
- Approved signal: The user explicitly required currency replacement without replacing Minecraft trading, original Xaero GUI/controls/waypoint appearance, optional unobtrusive JEM layers, and a Buyer trading window with no chat.
- Protected behavior: Ordinary trading keeps native offers, slots, scrolling, interaction, resource-pack styling and restock ownership; JEM must not draw its own price/status/balance layer over `MerchantScreen`. Coin settlement remains authoritative and may retain only non-rendering selection/click adapters. The Buyer has a native trading window and does not deliver its interface/errors via chat. Xaero remains the native GUI/controls/waypoint-actions owner; approved terrain/boss sharing and private personal points remain intact. The only approved GuiMap addition is the thin always-visible Settlements boundary plus its owner-only preview/confirm expansion option. JEM-owned SMP GUI is outside this restoration.
- Allowed changes: Narrow non-rendering currency/payment adapters, native offer projections, Buyer offers, native shared-terrain/boss data adapters, secure trade-event context and necessary matching client/server synchronization; no JEM GuiMap overlay/menu/teleport Mixin, arbitrary item-price invention, independent offer lifecycle or GUI replacement.
- Implementation state: User runtime correction on 2026-10-14 removed broad JEM MerchantScreen and Xaero screen/menu/action replacement. Settlements Update 1 on 2026-10-15 explicitly approved a narrow `GuiMap` tail-render/right-click extension for server-authoritative settlement boundaries and +16-block expansion at one backed Gold Coin. It may not construct, replace, center or otherwise mutate `GuiMap`, expose personal waypoints, or restore project/event overlays. CoffeeGUI, native Xaero sharing adapters and server Coin transaction authority remain. Fresh runtime acceptance remains pending. Unpriced native-offer semantics remain server-denied and are never resolved by invented prices.
- Unlock condition: Explicit user approval of a specific replacement design.
- Created: 2026-10-14.

## LOCKED: Semantic icons across JEM UI
- Scope: JEM UI icon resolvers and menu item factories, especially Mods/JEMServer/client/smp/SmpIcons.java and Mods/JEMClaims/ClaimsMenus.java
- Approved signal: Залокай - использовать правильные icon для всех UI моментов
- Protected behavior: Every UI action and domain setting uses a recognizable semantic item icon; dyes are not used as generic enabled or disabled placeholders, and state is conveyed by text, accent, or glint.
- Reuse pattern: Preserve the existing implementation pattern unless the unlock condition is met.
- Allowed changes: Add or refine semantic mappings while preserving recognizable action/domain meaning and non-dye state presentation.
- Unlock condition: Explicit user request to change this behavior or evidence that the core execution pipeline changed.
- Created: 2026-09-29

## LOCKED: Client baseline and cumulative point updates
- Scope: `Client`, `tools/jem.py`, `tools/workspace.json`, `tools/state/release-baseline.json`, and `C:/Users/user/.codex/skills/minerent-deploy/SKILL.md`
- Approved signal: The user explicitly required stable-client fixation before every task, continuous synchronization and exact add/replace/delete ZIP delivery across parallel Codex sessions.
- Protected behavior: `Client/ManagedPack` is the only canonical full AutoModpack source and release-baseline owner; `Client/JustEnoughMods` is a bootstrap-only installer containing matching launcher metadata, `mods/jem-bootstrap.jar` and only required empty user directories. Every successful MineRENT deployment publishes the managed source through native AutoModpack, records its local date/time and changed/deleted paths, and includes the complete current `Client/ManagedPack/kubejs` tree in any manual point-update; then it promotes the published managed snapshot so later tasks form the next delta. The installer never duplicates gameplay mods/configs and release artifacts never include runtime-only personal files.
- Allowed changes: Extend deterministic release metadata or packaging output without merging installer and managed-pack ownership, changing AutoModpack deletion scope, client/server release parity or the rule that baseline promotion happens only after the complete MineRENT/client release succeeds.
- Unlock condition: Explicit user request to publish a new baseline after the friends have installed the current cumulative update.
- Created: 2026-10-02
- Amended: 2026-10-03 after the user made every MineRENT update a simultaneous distributed-client release and required each later task to start a new tracked delta.
- Amended: 2026-10-03 after the user reported that friend/shared ZIPs omitted Russian localizations already present in the maintainer client.
- Amended: 2026-10-06 after the user clarified that every point-update must contain the complete KubeJS tree rather than only the Russian localization overlay.
- Amended: 2026-10-11 after the user explicitly replaced the monolithic full client ZIP with a bootstrap-only installer backed by a separate canonical managed-pack source.

## LOCKED: One active event and SMP GUI specification
- Scope: `Docs/events/*.md`, `Docs/SERVER_COMMANDS.md`, and `Docs/smp-gui/README.md`
- Approved signal: The user explicitly requested one state of truth per event, a server command reference, and an SMP GUI folder containing visual assets and click logic.
- Protected behavior: Each event has one current document with primary and rejected behavior; old reports and session artifacts cannot override it; SMP GUI work starts from the current navigation contract and reusable asset inventory.
- Allowed changes: Update the owning document in the same task when the user changes behavior.
- Unlock condition: Explicit user request to replace this documentation model.
- Created: 2026-10-02

## LOCKED: Full-height filled event boundary presentation
- Scope: Mods/JEMServer/src/main/java/com/siirio/jemserver/client/smp/EventBoundaryRenderer.java renderFilledBoundary/wall geometry, TelegraphRenderType boundary material usage, and EventNetwork boundary height/color contract
- Approved signal: грани зон ты очень сделал хорошо, это всё правильно, пусть так и стоит можешь даже залокать. Наконец мы этого добились
- Protected behavior: Blood Moon, Resource Rush, and hosted boss boundaries remain continuous full-height filled translucent colored quad walls, visible from both sides, with no wireframe, rails, posts, debug boxes, gaps, camera-relative duplicate, or terrain-height truncation.
- Reuse pattern: Preserve the existing implementation pattern unless the unlock condition is met.
- Allowed changes: Narrow render backend, vertex format, material, buffering, shader-pack compatibility, and shadow-pass guards may change only to make the same approved geometry, color, opacity, and world-space placement visible with vanilla rendering and different shader packs.
- Unlock condition: Explicit user request to change this behavior or evidence that the core execution pipeline changed.
- Created: 2026-10-04

## LOCKED: Blood Moon participant boundary and terrain restoration ownership
- Scope: Blood Moon startup in `EventParty`, `EncounterContext` and `BloodMoon`; participant collision and teleport validation in `EventAreaHooks`, `BloodMoonSolidBoundary` and `ServerPlayerTeleportMixin`; Wave 3/5 temporary terrain ownership in `BloodMoonSetPieces`.
- Approved signal: The user explicitly required the Blood Moon boundary to activate only after every accepted participant enters and required every temporary Wave 3/5 attack block to restore safely across every ending and restart.
- Protected behavior: Before combat the visible wall is passable and no wave spawns; one authoritative transition starts combat only when the complete participant snapshot is physically inside; SOLID collision then applies only to active non-eliminated participants and their ridden entity, while outsiders remain passable and combat-isolated. Attack snapshots retain exact original and expected temporary block states, restore attack blocks before arena terrain, preserve unloaded pending entries across restart, and never overwrite a current block no longer owned by the event.
- Allowed changes: Optimize the same bounded participant checks, collision geometry or persisted restoration queue without adding a second lifecycle, movement-correction loop, global block scan or forced chunk loading.
- Unlock condition: Explicit user request to change Blood Moon entry, containment, outsider isolation or temporary-terrain restoration behavior.
- Created: 2026-10-10

## LOCKED: Author target-sized game assets instead of cutting references
- Scope: Production raster textures and item-model texture sets derived from visual references, especially Mods/JEMEconomy coin assets
- Approved signal: images are only references - lock that cutting such images is a bad practice since in game we see the AI bad cutting of the edges. Better alternative - create the images urself on needed size and style
- Protected behavior: Concept sheets and reference images remain reference-only: never crop, extract, background-remove, or stretch their depicted subjects into shipped game textures. Author each production image anew at its required native dimensions and game style, with intentional transparent edges and no AI cutout halos, clipped silhouettes, inherited backgrounds, labels, shadows, or perspective artifacts; validate the authored asset in every actual game presentation that consumes it.
- Reuse pattern: Preserve the existing implementation pattern unless the unlock condition is met.
- Allowed changes: Use the reference for approved motif, palette, material, silhouette, and lighting direction; regenerate or hand-author a new native-size asset, refine individual pixels and alpha, and replace an authored asset with another newly authored native-size version.
- Unlock condition: Explicit user request to change this behavior or evidence that the core execution pipeline changed.
- Created: 2026-10-13


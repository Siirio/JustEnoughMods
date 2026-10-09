# Approved Logic Locks

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
- Protected behavior: `Client` is the only clean distribution source; the release baseline is immutable during ordinary tasks; every successful MineRENT deployment must publish the same release to the clean/stable client and an exact friend point-update, record its local date/time and changed/deleted paths, and include the complete current `Client/JustEnoughMods/kubejs` tree as a required recipient-support layer even when unchanged from baseline; then promote the published Client snapshot so all later tasks form the next update; stable synchronization deletes only previously managed paths removed from `Client`; release artifacts never include runtime-only personal files.
- Allowed changes: Extend deterministic release metadata or packaging output without changing ownership, deletion scope, client/server release parity or the rule that baseline promotion happens only after the complete MineRENT/client release succeeds.
- Unlock condition: Explicit user request to publish a new baseline after the friends have installed the current cumulative update.
- Created: 2026-10-02
- Amended: 2026-10-03 after the user made every MineRENT update a simultaneous distributed-client release and required each later task to start a new tracked delta.
- Amended: 2026-10-03 after the user reported that friend/shared ZIPs omitted Russian localizations already present in the maintainer client.
- Amended: 2026-10-06 after the user clarified that every point-update must contain the complete KubeJS tree rather than only the Russian localization overlay.

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


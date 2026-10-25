# JEM FTB Quests — UI RESCUE v3 (replace previous questbook implementation instructions)

**Status:** urgent corrective implementation. **Target:** installed Minecraft Forge 1.20.1 FTB Quests build, not a newer-doc assumption. **Input:** current JEM repository/working tree, active quests, assets, installed mods/configs and the user's 7 failure screenshots dated 2026-10-25. Previous v2 spec is superseded wherever this document conflicts.

## Why the current implementation is a failure

The images show (1) more than a dozen left sidebar entries, duplicated/legacy navigation, and nested groups; (2) quest icons and title art extremely small relative to gigantic canvases; (3) massive decorative backgrounds, mostly unused area; (4) barely visible progress chains, sometimes 20+ nodes in a straight ascending diagonal; (5) magenta/black missing-texture symbols above some headings; (6) excessive long/narrow text wrapping; (7) bad opening viewport requiring major zoom/pan; (8) the intended map-like layouts flattened into sparse strings of nodes. **Do not defend or cosmetically touch up the current layout. Restructure it.**

## HARD CONTRACT — must pass before calling the job done

1. **Exactly 7 player-visible chapters/tabs in the uncollapsed sidebar. No nested groups, extra hub tab, starter tab, legacy tab, duplicate categories, or technical chapters.** The canonical entries (RU/EN localized) are:
   - Very Many Biomes / Очень много биомов
   - Acompany / В хорошей компании
   - Cozy Home / Уютный дом
   - Farm 'n Food / С грядки на стол
   - Cogs & Trains / Шестерни и поезда
   - Secrets of Magic / Тайны магии
   - Boss Busters / [short evocative RU translation approved by current localization; do not show any mod name]
   Retire/migrate existing standalone sidebar entries like Getting Started, Unavailable Content, Races, Exploration, Bosses and the 12 Eyes, Create, Living World, Events, 'JEM Atlas', duplicate 'Очень много биомов' etc. **Every existing valid quest remains reachable somewhere among the seven**; no orphaned real quest IDs. Do not show SMP-only event content in singleplayer if this conflicts with current JEM gameplay rules; do not delete that content when hiding it.
2. **No subsection is a real separate sidebar chapter.** Subsections are **zones/clusters inside the corresponding main chapter canvas**. These are not dummy quest links floating across empty maps. If 7 tabs are not achievable with stock FTB configuration and all required mechanics, make the smallest verified custom client navigation fix in the already-existing JEM UI module; don't silently expand the sidebar. Prefer 7 actual chapters over custom UI work.
3. **No magenta/black missing-texture markers, unresolved registry icons, checkerboards, or unloaded image placeholders.** Every referenced resource location must resolve under the correct assets namespace and case-sensitive path. Fix the source, don't mask it behind a colored rectangle. Replace invalid icons with actual valid installed in-game items if necessary.
4. **All quest IDs, existing task and reward definitions, dependency IDs, optional/unlock logic, completion persistence and translation entries remain intact**, except explicitly documented repairs for actually broken entries. Move their existing data instead of generating new IDs. Back up existing config first. Keep a machine-readable before/after ID inventory and a report of any unavoidable exceptions.
5. **At neutral/default player opening zoom, the layout has meaningful quest nodes and title in the immediately visible viewport.** For a 1920×1080 or 2048×1152 client window, test the actual displayed screen with normal Minecraft GUI scaling and a collapsed/unpinned and an expanded sidebar. Make quest buttons visibly interactive and reliably clickable (target ~24–40 px diameter on a normal full-HD screen, preferably ~28–36 px where feasible; no hard-coded pixel requirement overriding real client scaling). Never shrink all quest buttons just to fit a gigantic background. If per-chapter zoom cannot be saved with the stock build, evaluate existing persistent zoom settings and only then use a narrow client-level per-chapter initialization patch if required; `autofocus_id` only controls center, not zoom.
6. **At chapter entry, the combined quest content bounding envelope uses roughly 65–85% of the available viewport width and height.** No chapter with the major content in a tiny 10%-size corner or requiring a giant 300%-style corrective zoom. Preserve normal close-range readability; large chapters may have small optional peripheral continuation, but the central initial view must show its heading and multiple meaningful quest clusters. Do not claim all 100 nodes need to be visible if that would force tiny buttons; prioritize core clusters and normal navigation.
7. **Every chapter has large, attractive hero art.** Target central title graphic about 25–40% of available view width where aspect ratio allows (not the tiny 7%-width stickers currently shown); enough padding around it to avoid nodes sitting on top of letters. Do not inflate the background along with the title. Backplate should fit the title and be decorative, not a giant empty billboard. If a title PNG contains excessive transparent pixels, crop/adjust its image bounds and aspect ratio so the *visible* artwork is large.
8. **Decorative artwork is subordinate to content.** Non-interactive art may bleed slightly beyond the quest content, but should not create broad margins or dictate quest coordinates. Backgrounds should be tightly framed, correctly cropped, properly layered, and visible where content is present. Replace huge grey default empty fields and full-screen meaningless expanses with appropriately scaled available art; no new unapproved image generation.
9. **No chapter laid out as a single 15–25-quest upward-sloping diagonal.** A long sequence must fold through compact turns/short rows or split into justified branches. All dependencies stay truthful; visual folding does not mean changing unlock rules.
10. **Never put technical mod names, namespaces, 'vanilla/modded', or mod IDs in player-facing category, node title, subtitle, overlay or description text.** Registry and source mod names are mandatory in internal mapping/reporting only. The titles must remain Minecraft-like, brief, and localized RU + EN.

## Navigation mapping (MANDATORY)

Exactly 7 chapters; integrate these mechanisms as sections, not further chapters:

| Main chapter | Internal zones (not extra sidebar entries) |
| --- | --- |
| Very Many Biomes | Starting expedition, biomes, exotic discoveries, exploration structures, real travel progress |
| Acompany | Companions; Barnyard Buddies plus ordinary farm animals/horses from all installed sources; retained Alex's Mobs animals; other wild/exotic/aquatic families |
| Cozy Home | decoration/building boxes; foundational tools and crafting; Sophisticated Backpacks and Sophisticated Storage as meaningful utility wing; settlement comforts |
| Farm 'n Food | crops, seasons, kitchens, meals, farming loops, fishing/drinks, specialty food |
| Cogs & Trains | ~90% installed Create and train add-ons, ~10% conventional redstone; production, movement, logistics, railway systems |
| Secrets of Magic | enchanting, magical systems, relics and verified cross-content progression; if appropriate, related power abilities, but DO NOT bury important power progression in an unrelated chapter |
| Boss Busters | combat gear, world boss route, world-tier/power survivability, Nightfall bosses/gear, **Twelve Eyes campaign Tournament Bracket**, actual singleplayer/multiplayer restrictions |

The placement of the power system must be mechanics-driven: put combat/protection power progression with Boss Busters and magical/arcane abilities with Secrets of Magic where that makes sense. Do not duplicate the same quest just for thematic appearance; use an in-chapter label/click-through reference if truly necessary.

**Subsection visibility:** recognizable section labels and compact art within the chapter, not 7 more buttons per section in the sidebar. Questbook should open the last selected valid main chapter, otherwise a useful introductory one; there must be no mandatory hub requiring another click before quests appear.

## Geometric rules (from user approvals; NOT arbitrary shapes)

### Very Many Biomes — raw constellation
- Irregular star field with meaningful medium-length biome arms, specificity continuations for desert/cold/etc., and truly isolated rare/exotic discoveries.
- Fit arms around hero art rather than placing every point at a fixed large radius. Use short paths with purposeful elbows. No huge dead top-right field.
- Existing biome quests must be grouped by real biome taxonomy/current active registries; don't fabricate prerequisite edges to make the shape symmetrical.

### Acompany — dense, readable river delta
- A clear source/bond milestone and distinct winding tributaries for: 1 companions and their home/binding; 2 Barnyard Buddies and common livestock/horses irrespective of source; 3 Alex's Mobs only if actually enabled; 4 other useful families.
- Screenshot has 52 total quest objects: avoid scattering them across an enormous 6-column × 10-row vacuum with a postage-stamp logo. Use tightly packed, curving channels; short branch connections, visible node counts and a meaningful visual rhythm around the properly sized banner.

### Cozy Home — asymmetrical decor gallery
- Use the user's approximate box arrangement below as a **relative composition**, not a license to put 13 boxes 100 units apart:
  ```
        [01]                [09]     [08]
  [02]          [10][12][13]          [07]
        [03]       [04][05][06]
  ```
- Every box contains **1–3 genuine quest nodes**, relating internally to one installed building/decor mod or a closely related small cluster; player-facing labels have use-based whimsical names, not the mod name.
- Fancy central decoration near title, starter tools/materials toward entry edges. Additional compact utility wing for backpacks/storage/settlements. Do not leave title at tiny scale amidst huge empty boxes.
- Boxes are visual clusters **inside Cozy Home**, never sidebar chapters.

### Cogs & Trains — compact kinetic rail network
- Dominant real Create/mechanical/train progression with side workshops and factory sidings, short redstone feeder; do not force a lonely 25-node slash from bottom-left to top-right.
- Fit long production sequences in 2–3 rows connected by compact bends and short branches, without altering quest dependencies or fake joins.

### Farm 'n Food — productive patchwork
- A useful central kitchen/food hub and compact crop/fishing/season lanes. The most advanced cooking challenges form meaningful nearby continuations instead of sparse distant diamonds.

### Secrets of Magic — readable arcane web
- Distinct schools/abilities with restrained asymmetry and coherent small branches, not a giant star with tiny nodes at outer perimeter. Include power abilities only if grounded in actual mechanics.

### Boss Busters — fight atlas and Twelve Eyes bracket
- Outer combat-preparation, survival, power and Nightfall sections must be comprehensible, not just dumped randomly around the logo.
- **Twelve Eyes is a TRUE 12-entry tournament-bracket COMPOSITION within the same Boss Busters canvas**: 12 authentic main boss/eye paths feed visually toward the final convergence. The bracket must not pretend boss A is an unlock prerequisite for B where no actual dependency exists. Use labeled rounds/stages only as artwork metaphors (or progression actually supported by mechanics), not artificial objectives.
- Distinguish optional gatekeepers and non-campaign Nightfall fights from the 12 canonical bosses. Never award boss weapons prematurely and never change tier rules for the sake of geometry.

## Concrete auto-layout/scale rules

1. Compute visible quest objects and dependency graph per chapter. Lay out connected components locally with short readable edge lengths; group by the approved topic geometry and assign stable component boxes.
2. Compute a **quest bounding box**, plus **art bounding box** independently. For each chapter, output counts and bounds plus ratio `content_width / chapter_view_width` and `content_height / chapter_view_height` at real default zoom. **Do not use art bounds to force additional quest separation or derive canvas scale.**
3. Use proportional spacing: neighboring connected nodes typically 1.5–3.5 icon diameters apart; sibling lanes typically 2.5–5 diameters apart, except title/backplate reserved areas. Adjust to avoid link crossings and icon collisions. Do not place 3 nodes at x=5,15,25 when x=5,8,11 is fully readable at the chosen zoom.
4. Use two-dimensional space. Penalize monotonically increasing long diagonals, one-sided placement, and empty rectangular quadrants. Bend chains compactly while preserving directional progress; favor intentional 'S'/'Z' paths and short side branches over arbitrary serpentine zigzags.
5. Reserve title/backplate area as an **obstacle** when placing nodes; size based on actual cropped visible pixels. Hero title takes intentional center or off-center position as appropriate; don't park a logo in a void while quests drift into distant corners.
6. `autofocus_id`: point to a **real surviving quest or quest link near the meaningful initial center**, not an old removed placeholder; do not rely on `autofocus_id` to adjust zoom. Check persisted chapter camera state after reopen; ensure good first view in fresh world and existing world.
7. For default quest and icon scaling, inspect actual installed FTB Quests version settings for `default_quest_size`, each `size`, `icon_scale` if supported, and visual presets. Set a consistent baseline and make milestone nodes slightly larger. Do not globally enlarge IDs/shapes beyond clickable or screen-fit limits.
8. Asset rules: audit `images`, chapter icons, quest icons, custom text images, translations and resource paths; create a lookup test for path existence and case; inspect transparent alpha bounds and image aspect ratios. Replace missing-texture image/icon references with verified legitimate asset. Avoid using a 2048-wide JPG with large solid background as a tiny central title; use correctly cropped **transparent PNG artwork** from permitted existing project/source images, preserving pixel-art hard edges and alpha. No rough auto-extraction leaving halos or background noise.
9. Text labels: keep chapter/subsection labels to one readable line where possible; maximum 2 deliberately broken lines for long Russian; don't truncate into fragments like `Друзь/я из` or put labels atop magenta placeholder. Use an actual readable UI font scale and avoid overly many large labels scattered around the screen.
10. Ensure no effects of render scaling changes in GUI corrupt mouse hitboxes, drag/scroll, quest click targets, or resource-pack GUI compatibility.

## Audit & migration details

- Read actual working tree, quest file format, version-appropriate FTB GUI code/config, and source asset paths; find what the prior Codex session actually committed. Don't rebuild blindly from the old design brief.
- Create `before.json` and `after.json` mapping quest ID → chapter, task IDs/types, reward IDs/types, dependencies, location. Assert unchanged IDs, objectives and rewards. Flag changes explicitly. No secret deletion of 'Unavailable content' tasks: distinguish genuine obsolete placeholders vs real play content; inaccessible content must be correctly hidden or repaired.
- Inspect chapter-group definitions, sidebar group ordering, real chapter IDs, locale files, image entries and quest link targets. Collapse 10+ source chapters into the 7 approved real chapters. Repair orphan references. Remove obsolete empty chapter files/groups only after migration audit proves no quest lost.
- Preserve singleplayer vs SMP gating, including party/event/raid semantics; avoid inventing new game logic just to reorganize UI.
- Make deterministic reruns and include a dry-run report; use backups/branch and a PR rather than directly merging to main unless explicitly instructed.

## Visual acceptance — screenshot gate

Test the entire workflow **in Minecraft Forge 1.20.1** with real assets loaded at 1920×1080 or user's 2048×1152 equivalent and a normal GUI scale. Obtain *minimum* these screenshot proofs:

A. Sidebar fully expanded: **exactly 7 top-level chapters**, no nested/duplicate/legacy or tech chapters, names readable.
B. Very Many Biomes fresh open: substantial title visible, 2–4 meaningful constellations near one another, several recognizable quest buttons, no huge empty quadrant.
C. Acompany fresh open: large banner, several river tributaries and legible icons; no postage-stamp content.
D. Cozy Home fresh open: central title plus compact 1–3-node building clusters and storage wing in coherent visible spatial composition.
E. Cogs & Trains fresh open: bent/forking networks occupy both axes rather than a long slope.
F. Boss Busters fresh open: combat prep, Nightfall, power/survival and clear 12-eye tournament structure.
G. One reopened chapter after switching and one freshly created world: same reasonable entry experience. Verify both sidebar collapsed/expanded.

**No screenshots, no claim of visual completion**. If actual Minecraft launch cannot be run in execution environment, include an explicit `NOT VISUALLY VERIFIED` flag and clear reproduction steps; static validation does not count as passing UI acceptance. Don't claim that screenshot issues are fixed based only on parseable SNBT.

## Reject the PR automatically if any of the following hold

- Sidebar >7 player-visible chapter entries or any old duplicates;
- A chapter still shows >50% margin on at least two sides at its usual entrance viewport while the quest content could have been laid out compactly;
- Nodes/hero title are still postage-stamp small or all nodes are only visible after aggressive zoom out;
- Any magenta missing-texture graphic or invalid icon/atlas path;
- Any straight 10+ node diagonal requiring pan to find other nodes;
- Any quest/task/reward/dependency ID loss or broken title/translation;
- Any newly added mod-name-labeled chapter/quest text;
- Any evidence of edited-player-progress breakage;
- Only static file validation offered as proof of in-game UI success.

**Deliverables:** actual fixed questbook configs/resources and any minimal version-appropriate client patch, an ID-preservation migration report, a compact human-readable geometry/bounds report, A–G screenshots or honest testing limitations, and a PR. Fix the original work; do not write yet another design-only document.

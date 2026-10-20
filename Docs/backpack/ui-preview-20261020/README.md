# Backpacked UI source preview, 2026-10-20

## Current horizontal revision: H1

Later explicit user direction adds a native-family cream Furnace icon. The icon-only follow-up and updated composed previews are in [furnace-icon-20261020](../furnace-icon-20261020/README.md). It supersedes H1's intentionally omitted Furnace icon; H1 horizontal geometry remains the current source layout.

The user explicitly requested «лучше горизонтально» after viewing the vertical composition. This authorizes a new horizontal revision; it is not approval of runtime rendering. Current previews:

- [Configured9×5,1×](H1-08-configured-9x5-1x.png) / [2×](H1-08-configured-9x5-2x.png): current canonical native dimensions; both panels in one80px row.
- [Both active9×3,1×](H1-01-active-1x.png) / [2×](H1-01-active-2x.png).
- [Empty/Auto Feed off,1×](H1-02-idle-1x.png) / [2×](H1-02-idle-2x.png).
- [Furnace only,1×](H1-03-furnace-only-1x.png) / [2×](H1-03-furnace-only-2x.png).
- [Workbench only,1×](H1-06-workbench-only-1x.png) / [2×](H1-06-workbench-only-2x.png).
- [All disabled,1×](H1-04-all-disabled-1x.png) / [2×](H1-04-all-disabled-2x.png).
- [Smallest-height9×1,1×](H1-07-small-9x1-1x.png) / [2×](H1-07-small-9x1-2x.png): source-backed clearance below native side panels.

Editable source: matching `H1-*.aseprite`, generated directly by [compose-horizontal-H1.lua](compose-horizontal-H1.lua) inside Aseprite. The3-row study is300×330; configured5-row is300×370. Earlier vertical files and `compose.lua` below are history, superseded for simultaneous-panel placement. H0 records the first horizontal interpretation; H1 is its first correction round. The earlier vertical C0→C1 history remains intact.

Source and preview share112px Workbench left +4px gap +112px Furnace right, centered under the unchanged native storage frame. One enabled panel retains its previous centered112×80 coordinates. Shared `BackpackWorkbench` methods own row width/left/top and inventory shift for both actual menu slots and screen drawing/labels/click bounds. All appended slot indices, native handlers, saves and server authority remain unchanged. Source-owned `hasClickedOutside` includes the actual projected row; existing native popup priority remains.

Native evidence for the H1 correction: installed Config.Backpack.Inventory.Size allows rows1..11 and columns1..23, current `backpacked.backpack.toml` is9×5. Native `createAugmentsPanel` has three20px rows, two1px dividers and four2px gaps, total70px. `createActionsLayout` has at most five10px controls/four2px gaps, total58px. `init` centers both beside storage and `lambda$drawBackgroundWindow$34` adds5px label-background padding. A228px row projecting beyond the native frame starts below both padded bottoms+4px. Native frames at least228px wide and single-panel states need no added clearance. For native184px frame, small rows1/2/3 add20/11/1px clearance, then the same80px row; default5rows needs no extra clearance. H1 corrects the side-strip reference composition to the actual native LABEL_BACKGROUND and actual five-action geometry.

H1 audit: all seven states opened at native1× and nearest-neighbour2×; positive cream/icon and rejected green-icon reference01 reopened, native atlas and prior compact centered target06 used. Observed PASS: horizontal order/4px separation,18px slot scale, native frame width, valid sample craft/result, empty/off states, centered single panels, player-panel shift and small-row side-panel clearance. No clipping or overlapping control/panel pixels observed in these compositions. Native3D Furnace augment icon is deliberately absent, as before. Complete native widget rendering, hover/focus/hit/drag, small viewport scrolling and synchronized gameplay remain UNOBSERVED; no user/runtime approval is inferred. Root owns production build, artifact synchronization and native publication; layout author ran none of them. No new art resource or game texture was introduced.

## Previous vertical composition evidence

Requested by the user before updating the test server. These are **editor compositions, not game screenshots**. No server update, game/client launch, build, test or gameplay verification was performed to make them. Sample contents, a 9-column ×3-row backpack and three unlocked augment bays are illustrative; native capacity, locks, visible actions, locale and installed improvements can differ.

- [Active,1×](01-active-1x.png) / [2×](01-active-2x.png): Workbench and Furnace enabled, Auto Feed enabled; sample valid iron-pickaxe recipe, sample furnace half progress/fuel. The output stacks and progress are sample data.
- [Idle,1×](02-idle-1x.png) / [2×](02-idle-2x.png): empty Workbench/Furnace; Auto Feed disabled.
- [Furnace only,1×](03-furnace-only-1x.png) / [2×](03-furnace-only-2x.png): Workbench disabled, Furnace enabled; player inventory moves up by80 pixels.
- [All disabled,1×](04-all-disabled-1x.png) / [2×](04-all-disabled-2x.png): no inserted panels; original native grey player inventory. The source replaces that surface with pale Backpacked materials only while an insert exists.
- [Selector,1×](05-selector-1x.png) / [2×](05-selector-2x.png): separate native six-column selector composition with English-name sort sample, native cream silhouettes and disabled entries for the three installed sample augments. This is a separate state study, not an exact popup-position capture. Actual sorting uses localized names, Empty first, with disabled types filtered natively.

The Furnace augment's icon is intentionally **absent** in the middle left bay and the selector. `NativeItemAugmentIcon` calls Minecraft `GuiGraphics.renderItem(Items.FURNACE)`, a3D native block-item renderer. This editor composition does not substitute the block's front texture or invented artwork for that render. Apple is the native item texture (12px in the bay;16px in the selector). Runtime item rendering, native title/checker widget details, hover/focus, popup placement and scrolling remain unobserved. The preview supports panel/slot/progress geometry review; it is not complete pixel-identical runtime evidence.

## Source and editable composition

Each state has its own layered `.aseprite`. [compose.lua](compose.lua) uses Aseprite1.3.18.6 to compose native surfaces directly at300×410 pixels (selector300×190), save native sources and export1× PNG. [font-map.lua](font-map.lua) maps the actual vanilla bitmap-font metadata.2× exports are Aseprite MCP `render_sprite` with scale2, nearest-neighbour, without changing the source dimensions. `C0-active.aseprite` and `C0-active-1x.png` preserve the first candidate.

Pixels are original native Backpacked3.0.9 `textures/gui/backpack_assets.png`, existing WorkbenchIcon source pixels and vanilla Minecraft1.20.1 assets/font. `native/` retains unmodified reference texture extracts, font provider metadata and read-only `javap` evidence. Vanilla extraction source was the existing local `ComfyBuilding [JEM].jar`, only as a read-only vanilla1.20.1 asset carrier; this neither changes nor establishes that profile as the runtime target. These are documentation/reference compositions, not new production GUI textures or distributed replacement assets.

Source owners inspected:

- `Mods/JEMCompat/.../mixin/backpack/BackpackWorkbenchScreenMixin.java`: actual background drawing, progress/flame texture coordinates, player-panel replacement, viewport scrolling.
- `Mods/JEMCompat/.../mixin/backpack/BackpackWorkbenchMenuMixin.java`: actual slot positions and shared native menu.
- `Mods/JEMCompat/.../feature/backpack/BackpackWorkbench.java`:112×80 Workbench;112×80 Furnace;18px grid spacing.
- `Mods/JEMCompat/.../feature/backpack/BackpackClient.java`, `WorkbenchIcon.java`, `NativeItemAugmentIcon.java`: native icon registrations and unchanged cream Workbench.
- Exact installed `backpacked-forge-1.20.1-3.0.9.jar`: `BackpackScreen`, `AugmentPopupMenu`, `BackpackButtons`, `TextureDefinitions`, `BackpackScreen$AugmentIcon`, `AugmentType.compareTo`; Framework0.8.0 native nine-slice/subimage contracts.

For sample menu origin(x,y), native width184 and rows3: storage slots start(x+11,y+27); inserts start y+44+3×18, centered at x+(184−112)/2. Workbench slot interiors are(12+18c,12+18r), result(85,30). Furnace interiors are input(22,12), fuel(22,48), output(82,30); flame and arrow use the exact screen source. Player inventory begins y+44+3×18+insertedHeight. The sample canvas fits the entire menu; actual smaller GUI viewports may need the implemented scroll behavior.

## Visual audit

Exact Pair: `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/Pairs/Minecraft/GUI/Forge-1.20.1-jem-smp-screens/{TASTE,TECHNICAL}.md`; shared Minecraft ART_DIRECTION and visual-auditor read. Explicit brief preserves Backpacked's native family, not a new theme. Relevant originals inspected: `Docs/art/backpack-augment-bay/reference/2026-10-19/01.png` (green Workbench rejection; surrounding cream icons), `06-layout-target-tab-removed.png` (centered craft insert/pale lower panel/no projecting tab), and original native `backpack_assets.png`. The rejected green icon is absent. Orthographic GUI, actual18px slots, native dark brown exterior/pale surfaces/recessed slots; no new motif or generated pixels.

C0 was opened at native size. It revealed a side-bay Apple incorrectly composed16px instead of the native wrapper's12px, plus the inactive-state composition incorrectly retaining the pale inventory. C1 corrects both source composition errors, adds a valid sample craft/result, and preserves C0. This is **one correction round**, not a reset. All five final exports were opened and inspected at native1× and integer2× before delivery.

Native Backpacked source JAR SHA256: `2b9b07d873fc3a913e6f259cd0e85c45999e1f1c03927a7202fbd4f7fb185d14`.

| Axis | Verdict | Evidence/limit |
| --- | --- | --- |
| Native brief/reference fidelity | PASS for observed composition | Existing native materials, cream Workbench, compact central inserts, no side craft tab. |
| Pixel/source/export provenance | PASS | Layered Aseprite/Lua; original native pixels; native font; direct1× export and integer2×. |
| Slot scale/geometry/content hierarchy | PASS for shown sample |18px native slots;3×3+result; furnace3slots; no observed slot or panel overlap. |
| Values/material/pixel clusters | PASS for reused observed surfaces | Native pale content/dark perimeter and recessed slot edges retained. |
| Native Furnace3D item icon | UNOBSERVED | Deliberately omitted; cannot certify without actual Minecraft render. |
| Complete native widgets, hit/focus/hover, popup placement | UNOBSERVED | Editor state studies do not execute controls or reproduce every native widget detail. |
| Viewport scrolling, synchronization and gameplay | UNOBSERVED | No runtime/client launch or tests. |
| New geometry/animation/VFX | N/A | No new asset or effect created; Auto Feed has no separate screen/effect. |
| User approval | PENDING | Agent review is not player acceptance. |

Verdict: **source-layout preview ready for user review with the named unobserved regions; full runtime visual acceptance remains pending**. No production asset or Java was changed by this preview task.

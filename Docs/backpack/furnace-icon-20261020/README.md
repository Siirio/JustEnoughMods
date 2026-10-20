# Backpacked-family Furnace augment icon

User direction: create a Furnace icon in the native Backpacked style. This changes only the Furnace icon registration/resource; Auto Feed's Apple, approved cream Workbench and horizontal layout remain unchanged.

- [Composed9×5 preview,2×](F0-08-configured-9x5-2x.png) / [1×](F0-08-configured-9x5-1x.png).
- [Native selector composition,2×](F0-05-selector-2x.png) / [1×](F0-05-selector-1x.png).
- [Icon native12×12](furnace-1x.png) / [integer16× inspection](furnace-16x.png).
- [Editable native source](furnace.aseprite), two named layers; [painting operations/pixel rows](authoring.json).
- [Aseprite composition script](compose-furnace-icon-F0.lua), matching layered `F0-*.aseprite` state compositions. They are editor/sample-data evidence, **not runtime screenshots**.

Production resource: `Mods/JEMCompat/src/main/resources/assets/jemcompat/textures/gui/augment/furnace.png`,12×12 RGBA PNG exported **directly from Aseprite MCP**, SHA256 `26d524e4ba1670a58118f4b9f61f468f0a83322abc77be9967f130907f2cca65`. No image generation, external resize, quantization or cutout was used. Enlarged preview never becomes a game asset.

`BackpackClient.java` registers `FrameworkTexture.subImage(jemcompat:textures/gui/augment/furnace.png,0,0,12,12,12,12)`. Exact installed Framework0.8.0's six-integer subImage overload was inspected. Native Backpacked's bay wrapper draws12×12; the selector uses the icon's native12px dimensions. This replaces only Furnace's previous `renderItem(Items.FURNACE)` path and resolves the missing3D-icon evidence region from previous horizontal previews. Other runtime rendering and user acceptance remain unobserved.

## Brief, original references and C0 visual audit

Exact Pair: `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/Pairs/Minecraft/GUI/Forge-1.20.1-jem-smp-screens/{TASTE,TECHNICAL}.md`; native Backpacked-specific extraction takes precedence over unrelated GUI themes. ART_DIRECTION, canonical Aseprite contract and visual-auditor applied. Original references opened: `Docs/art/backpack-augment-bay/reference/2026-10-19/01.png` (native cream silhouettes and rejected green Workbench), original installed `backpack_assets.png`, native vanilla `furnace_front.png`, and current `WorkbenchIcon.java` pixel rows/colors. User already specified extracting native Backpacked style; no new style calibration was needed.

Noun/action: Furnace augment. Orthographic front silhouette at12px; sturdy rounded-corner masonry body, upper furnace opening and lower fuel opening. Cream `#EEDBC0` and shadow `#98866B` exactly match Workbench. Shadow forms body/recesses; cream forms masonry and the compact lower opening cue. No colored block texture, glow, flame animation or extra decoration.

C0 was inspected at native1× and integer16×, then inside native-size/2× horizontal backpack and selector compositions. Observed PASS: two-opening furnace morphology, native-sized coherent pixels, exact family palette, connected body, readable12px silhouette and centered native-button placement. Pixel/source/export provenance PASS: actual two-layer Aseprite and direct12×12 PNG. Composed geometry PASS for the inspected sample: no overlap and unchanged horizontal layout. Motion/effects N/A. Live rendering, actual control hit/focus/hover, server/client synchronization and player approval UNOBSERVED. Verdict **AGENT_PASS for icon/editor composition**, not USER_APPROVED or runtime acceptance. No correction round was required.

Atlas lookup and before-change impact scan were performed; the five uncovered Roles/Settlements paths were unrelated existing work. The owning backpack record/history now covers this icon. Root owns final Atlas rebuild/validation, production build, native publication, Git and release/world operations. Icon author did not run builds/tests/gameplay, change runtime/world state or deploy anything.

# USER_REJECTED — Born in Bloom R2

The user rejected this candidate on 2026-10-17: “фигня изображения ... they are ugly and odd”. It does not fit vanilla Minecraft. Retained only as negative evidence; no production source or installed artifact may use it. Original JAR restored byte-for-byte from native published generation18; no new generation was published.

Historical brief and attempted implementation follow; their source/install claims are superseded by this rejection and rollback.

# Born in Bloom — Living Growth R2

User request, 2026-10-17: remove the Blockbench texture ban and retry the Born in Bloom VFX at the quality of strong Minecraft mods. The previous blocky canvas and thin-ring/beam effects remain rejected. Codex implementation decisions below do not change ability mechanics.

References: exact Taste Engine pair `minecraft-runtime-vfx/forge-1.20.1-jem-race-abilities`; labelled VFX+01–06 for distinct growing/eroding silhouettes, +08 for grounded rupture and +14/+15 for compact decisive impact. Reject VFX-01/-02's floating ground strips, uniform rings and connecting beams; reject VFX-05's smooth vine tornado. These are visual principles, never copied art.

## Choreography and spatial contract

- Verdant Dominion (44 ticks; tier3 grove 100 ticks): 0–5 t seed cracks at accepted target feet; 5–15 t roots fork upward around the target in two perpendicular planes, with offset phase/height; 15–30 t mature grasp and small leaf crowns; 30–44 t branch fragments release. Tier3 holds mature growth after the opening, then erodes during the last 12 ticks. No smooth beams or perimeter circle. Existing server-selected, at most eight target IDs remain authoritative; no new target scan.
- Living Path (18 ticks): only actual packet path blocks; place the root at the block's top face, grow a compact leaf pair over 0–8 t, hold 8–12 t and release 12–18 t. No link from player to block and no invented path. Sample the existing path quality budget.
- Second Blossom (38 ticks): 0–5 t compressed seed; 5–14 t pointed gold/green petals open outward; 14–24 t six staggered leaf blades rise around the player's body; 24–38 t fragments separate and vanish. The ground flower and upright blades supply different silhouettes, with clear space at eye height. No screen overlay/camera shake.
- Photosynthesis (24 ticks): two small turning leaf glints rise from waist to shoulder, unfold, then dissolve. Sparse, lower intensity than either active ability; no healing ring.

Atlas: original 256×128 RGBA, 8 columns × 4 rows of 32×32 tiles, 1px-or-wider transparent gutters. Row0 grasping root; row1 opening botanical burst; row2 path sprout; row3 turning leaf. Eight frame slots per row contain changing growth/opening/erosion poses and deliberate holds, not tint-only copies. Native opaque pixel clusters with transparent empty space; no blurred fringe. Forest shadow, jade body, chartreuse growth, pale gold vein/core. Point sampling, no mipmaps. Frame selection is effect-specific; no global FPS change.

Carriers are world-aligned crossed quads plus ground quads, alpha blended with depth testing and no depth write. Crossed root planes avoid the side-on disappearance of a single sticker. The ground layer belongs at target feet/placed block top with a small z-fighting offset. No world edits, light sources, gameplay state, packet changes or new registries.

Budget: no new particle emitter. Reuse at most eight authoritative recipients, existing trail samples and existing active-instance limits; at most 16 root quads or 7 blossom quads per event. All materials batch once per frame through the existing race render pass; existing dimension/disconnect/expiry and Oculus shadow guards remain.

## Source/export and evidence

`author.mjs` contains the original pixel construction recipe and paints the atlas through real Blockbench MCP brush stamps. `born_in_bloom_r2.bbmodel` is the editable Blockbench source; `bloom_growth_r2.png` is its actual exported texture, consumed by the JEMRoles renderer. Opening/painting/exporting the source proves editability, not game acceptance.

State starts DESIGNED and advances to SOURCE_EDITABLE only after actual export/inspection. Real Minecraft capture is not authorized by this prompt's standing rules; runtime visual acceptance remains WAITING_FOR_REAL_GAME_CAPTURE. No self-awarded approval or claim of matching the best mods.

## R2 source/export inspection — 2026-10-17

State: SOURCE_EDITABLE. The native MCP brush calls completed; the exported 256×128 PNG and embedded texture in the openable free-format `.bbmodel` were retained. The exported atlas was inspected at native size and 4× nearest-neighbor scale: root fingers and leaf buds grow, the burst opens then breaks apart, the path sprout gains paired leaves and a crown, and turning leaves narrow before fragmenting. Empty atlas space is transparent, pixels are opaque and the tile gutters remain clear. These are original assets, not cutouts from reference sheets.

The source uses held early/mature frames intentionally. Remaining visual risk: crossed planes and low-resolution motifs need actual combat-distance, camera-angle, terrain-occlusion and shader review in Minecraft. Offline sprite previews do not prove spatial quality. No current candidate is USER_APPROVED. Real-game evidence state remains WAITING_FOR_REAL_GAME_CAPTURE.

Production `jar` and `reobfJar` completed. JAR SHA256: `a2f4da3537d7f625e5991a19bdd57c4d13918cb4d35e248209d288523d00db18`. Its texture matches this exact export, its new renderer class is present, and the same artifact was copied to clean ManagedPack and ordinary local Server. No game client was launched.

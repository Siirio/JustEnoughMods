# Verdant Grasp R3 — one pre-install candidate

State: DESIGNED; no production integration authorized. The user's 2026-10-17 Quality Engine is the acceptance contract. This is one candidate for Verdant Dominion, not a new atlas for every Bloom ability.

Visual idea: five uneven, rooted wooden fingers emerge around a clear centre, bend inward into a grasp, hold, then withdraw. Dense small leaf masses grow at the branch joints; only two branches bear pale pink buds. The focus is the inward grasp. No glowing outline, starburst, floating sticker, particle cloud or circular UI marker. Roots are solid angular wood with a muted olive moss accent; flowers are subordinate. One model unit corresponds to one native material texel, with 16 units per Minecraft block. Max height approximately one block; player eye space remains clear. All textures are original, not copied vanilla pixels.

References searched and visually inspected before authoring:

- VANILLA-A: [official azalea in-game screenshot](https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/screenshots/azalea-ingame.jpg), [article](https://www.minecraft.net/en-us/article/taking-inventory-azalea): compact foliage volume, olive green clusters, sparse pink accents.
- VANILLA-B: [official mangrove screenshot](https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/key-art/Mangrove_Carousel-B-768_Mangrove-Swamp_1200x670.jpg), [article](https://www.minecraft.net/en-us/mangrove-trees-build-a-better-world): substantial connected angular roots, bark material and visible support at the ground.
- USER-NEGATIVE: [R2 atlas](../rejected/born-in-bloom-r2/bloom_growth_r2.png): thin illustrative twigs, star-like burst and unrelated leaf blades rejected by the user. No geometry or painted pixels reused.

Vanilla images establish a material/form comparison, not user approval of this invented spell. Existing user-labelled VFX sheets remain in the Taste Engine Reference VFX collection. The pasted Quality Engine refers to a screenshot, but the current attachment contains text only; no additional screenshot is invented.

## Animation

2.2 seconds / 44 ticks, matching the tier1/2 accepted event duration. Stagger the five roots by 1 tick. 0–4t: soil-level rise; 4–11t: wood extends and upper joints curl inward; 9–16t: leaf clusters unfold, two buds appear; 16–32t: hold the grasp with restrained settling; 32–44t: foliage folds and roots withdraw. No disconnected sprite replacement. Bone names and geometry remain stable throughout the clip. Continuous movement uses deliberately authored key poses and interpolation, not independent generated frames. This is a once-only action; the preview replay includes a deliberate pause after complete withdrawal.

World anchor would be accepted target feet, oriented with a stable per-target yaw. No gameplay state is changed. The neutral flat ground surface in the editor is a labelled preview stage, not a game asset or new block. It must be omitted from a future runtime export. No real mob or Minecraft scene is fabricated in the preview.

## Candidate assets and review

Editable `.bbmodel`, original native 16×16 bark/foliage/bud textures, animation and reproducible MCP authoring recipe. Render the exact model from fixed cameras, make key-pose contact sheet and a moving preview, and inspect native texels at nearest-neighbour scale. Asset checks requested by the user cover PNG dimensions, palette/alpha and unexpected disconnected pixels; animation review covers stable bone/element IDs, key timing and frame discontinuity. Those checks do not authorize gameplay tests or game launch.

Real Minecraft capture and USER_APPROVED remain unavailable at the pre-install stage. No renderer/Java changes, JAR build, Client/Server synchronization or pack publication is part of this candidate.

## Inspection record

State: SOURCE_EDITABLE, pre-install candidate. Original native 16×16 materials were painted with the MCP brush; geometry, hierarchy and clip were authored with structured MCP tools. Fifty cuboids include the preview ground; there are no billboard sprites or extra particle emitters. Source and exported textures match byte-for-byte. The clip lasts2.2s/44ticks, has stable elements and authored joint/leaf/bud timing. The GIF uses45 genuine fixed-camera Blockbench captures, with a replay pause after withdrawal; it is not Minecraft footage.

R3a review found plank-like leaves and roots visible beneath the small preview plinth during withdrawal. Revised leaves have stepped volume and deliberate native-density UV samples; an extended neutral ground surface now occludes underground geometry. Retained R3a source and pose sheet in `iterations/`. Inspected final key poses, hero, materials at1x/nearest9x, and front/side/back native editor views. The empty centre remains visible in these views; scale, entity occlusion and shaders still require an eventual authorized game capture. No glow, particles or scenery hide the silhouettes.

Requested automatic asset inspection: all four PNGs16×16, four colors per original bark/foliage/bud material, one neutral stage color, intentional opaque material alpha, no isolated color pixels, stable unique geometry IDs, keyframes within44ticks,45 equal600×600 captured frames, no large unexplained adjacent-frame jump, and identical withdrawn start/end pixels. These are factual checks, not an artistic score or user approval. See `asset-audit.json`.

Native offscreen-view disposal reported `onContextMenu is not defined` in the pinned MCP plugin; the source, textures and all captures were saved before that optional cleanup failure. No game state was touched.

The visual hypothesis is woody volume plus compact foliage at vanilla-like texel density. It remains a proposed design, and these editor renders cannot prove that the result fits real vanilla terrain at combat distance. At candidate delivery no production renderer, packet, JAR, ManagedPack, Server or generation changed.

## Authorized sandbox connection — 2026-10-17

The user subsequently explicitly requested “Подключить R3 в jem sandbox”.
`Mods/JEMVisualSandboxR3` now owns the isolated runtime connection and its
reproducible native OBJ bake. F9 toggles the candidate at a selected ground
point in a solo world, with a three-second replay interval. Runtime geometry
comes from the exact source above; the editor stage is excluded. This is a
standalone sandbox extension, not an integration into canonical race combat.
Its production JAR was built and installed with an original-JAR backup; see
the module README for provenance and installation details. No Minecraft launch
or actual in-game inspection occurred. Final visual approval remains pending.

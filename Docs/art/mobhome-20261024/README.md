# Mob Home native model production

User references: small pale-stone cottage with terracotta roof and flowers, large log cabin with turf roof. Original photograph bytes are retained in this folder. They are subject references, not reusable texture pixels

The user rejected both Antigravity script-generated models on2026-10-24 and explicitly requested Codex production. This task-specific choice overrides Gemini ownership for these models. The user then selected "Keep this direction" for the actual C1 stone-house front preview. This accepts the direction, not unseen runtime exports or the large model

Source route: Blockbench MCP cuboid/UV construction, external native Aseprite painting with four editable layers, official-compatible headless Blockbench codec export and three.js model rendering. No photographic pixels, procedural texture noise, external raster writer or averaged-face renderer. The live editor was launched, but its desktop MCP endpoint did not answer, so file operations use the installed native headless route

C0 review found gable tier tips protruding through the pitched roof. C1 shortened those tiers and lowered the side-wall tops to the actual roof underside. C2's additional solid leaf crowns looked too rectangular, so the preferred runtime copy preserves C1 geometry and appearance. During atlas cropping, the texture's UV canvas metadata remained128 tall while its actual image was64. Correcting uv_height preserves native painted texels without resampling

The user subsequently requested optimization of both houses while retaining their appearance, and explicitly required closing the gaps between boards. Preserve the C1 stone master and neutral cabin source separately. The optimization uses a scoped source budget of at most48 cuboids per house, one128x64 atlas and no dynamic renderer. This budget is an implementation decision for these two assets, not a universal Blockbench limit

| Asset | Preserved construction | Optimized master | Change |
| --- | --- | --- | --- |
| Small stone house | 57 cuboids / 299 enabled faces | 44 cuboids / 211 enabled faces | 13 cuboids removed, about 23% |
| Large log house | 67 cuboids / 402 enabled faces | 47 cuboids / 217 enabled faces | 20 cuboids removed, about 30% |

The stone house keeps its arch, timber roof frame, pitched terracotta roof, open entrance, window, planters and flowers. Redundant header geometry and separate lantern cap/base objects are removed. Both houses replace stepped gable infill stacks with fitted front/interior cutout surfaces under the existing solid roof. These are bounded gable walls, not a replacement for the house shell. Collision translates their triangular wall volume into conservative two-unit stair strips

The cabin replaces27 individual spaced log segments with eight continuous wall/window sections and eight defining corner projections. Wall courses are painted in the native atlas, retaining their direction and material. Actual wall geometry is continuous, including the former0.2-unit inter-course gaps. The entry, window, frame, roof overhang and corner silhouette remain modeled. The blank name plate is omitted and the lantern uses its painted frame. Two anchored flower cards retain the small trough's planting

All pixels originate in editable Aseprite layers. Gable masks and continuous wall areas reuse authored pixels at native resolution without resampling. Each house has one128x64 atlas. There are no meshes, animations, block-entity renderers, rendering tickers or translucent material passes. Runtime per-cell element/quad counts are recorded separately from master counts because legal Java partitioning can increase the number of elements. No FPS measurement or player acceptance is inferred

Visual audit: inspected the two original photographs, C1 stone front, neutral cabin front/back and actual optimized front/left/rear-oblique captures. Reference/subject, native provenance, visible connections, open cavities and material separation are observed in editor evidence. Runtime rendering, shader compatibility, player collision and performance remain UNOBSERVED. The gable extent trial exposed pale pixels beyond the small roof, so its fitted earlier extent was restored. This is the user's authorized optimization revision of the earlier selected direction, not a reset of the prior C0/C1/C2 art history

Counter: C0, C1 and C2 inspected. Preferred geometry is C1. User-approved continuation direction allows the large house in this same production style. Later style changes require the user's direction, rather than silent extra automatic art loops

Actual packaged cell elements were reassembled through Blockbench MCP with their translated positions and cropped UVs. Both front renders retain the houses and show continuous walls. These captures are small_animal_home-baked-review.png and large_animal_home-baked-review.png. Final runtime resource costs are 88 elements / 330 quads across eight small cells and 122 / 395 across27 large cells. Partitioning increases element count without replicating the whole house. User deferred game launch, native publication and installation

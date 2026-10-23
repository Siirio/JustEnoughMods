# Large Animal Home — prompt for Gemini

Create a production-ready, editable Minecraft Java Forge1.20.1 animal-shelter model, a warm oak stable related to the supplied Small Animal Home. Deliver the actual `.bbmodel` project and crisp PNG texture/UV sources. A picture, concept turntable or renamed text file does not satisfy an editable model request. If real model creation is unavailable, disclose that and return a clearly labelled concept with front, side, back, top and three-quarter views.

The full exterior occupies exactly a3 x3 x3 Minecraft-block footprint: maximum48 x48 x48 model units. One block equals16 units. Keep every overhang, brace and roof part inside those bounds. Local origin is the footprint's bottom rear-left corner; +Y is up and +Z is the open front. Build integer-aligned Minecraft cuboids for static baked Java block models. No smooth mesh, permanent animation or block-entity renderer.

Use the small shelter's material and construction language: warm oak/brown timber, slightly desaturated straw/stone details, sharp one-texel edges, restrained highlights, strong but natural value separation. Consistent16 texels per Minecraft block, including between both shelter sizes. Larger silhouette and sturdier construction, not an enlarged/retextured kennel. No realism, bloom, gradients, glossy cartoon bevels, fantasy metal decorations, glyphs or random procedural scratches.

Mandatory physical geometry:

- Higher stepped/gabled roof carried by stout fitted timber columns.
- Simple credible crossbracing at the sides/rear, outside the required clear interior and doorway.
- Open front entrance at least34 units wide and37 units high.
- Central usable floor at least36 x36 units, with at least39 units of genuinely empty headroom above the actual floor surface. To fit within48 units total, keep the floor low and roof structure above that clear space.
- Genuine side openings at least8 x8 units. Painted dark windows do not count as openings.
- No columns or ornament intruding into central clearance or mount interaction space.
- No solid doorway plane, invisible collision cube, duplicate/coplanar faces, Z-fighting or blocked interior. Roof, floor and posts connect physically.

Both shelter sizes have the same pet eligibility. This larger stable is an aesthetic/building preference, never a requirement for a larger animal. Do not include a horse-size gate, pet scaling, feeding/storage mechanics or extra upgrades. A pet too large for the interior will recover safely outside or above buried terrain through separate game logic.

Technical delivery:

1. Actual `large_animal_home.bbmodel`, editable named structural groups/cuboids, sensible origins and editable UVs.
2. Native PNG texture exports and externally layered pixel sources for any newly painted atlas. Exactly16 texels per block where feasible; no large generated painting shrunk into pixel art. Reused native oak/log resources must be identified by their real paths.
3. Full-structure art master compatible with export into twenty-seven16x16x16 occupied cells and four horizontal orientations. Each exported Java-model cube stays within its own cell. Preserve the visible design through this split; do not export one unsupported48-unit Java block model and claim it is ready.
4. Front/side/back/top/three-quarter previews at equal scale, neutral-lit view through the actual front opening, and exact measured bounds, entrance/interior clearance, atlas dimensions and texel density. Disclose any export capability missing from your environment.

Use the accepted small model as the family reference if supplied, while preserving these distinct large-shelter dimensions and larger structural proportions. Produce one stable, not a collection or environment illustration. No pet, scenery, UI or runtime Java code is requested.

# Small Animal Home — prompt for Gemini

Create a production-ready, editable Minecraft Java Forge 1.20.1 animal-shelter model, a small warm oak kennel. Deliver the real `.bbmodel` project and its crisp PNG texture/UV sources. A picture of a model, turntable illustration or file renamed to `.bbmodel` does not satisfy the request. If your environment cannot create real model files, say so and return a clearly labelled concept with front, side, back, top and three-quarter views instead.

The complete exterior occupies exactly a 2 x 2 x 2 Minecraft-block footprint: maximum 32 x 32 x 32 model units. One block equals16 units. All roof overhangs and ornaments stay within that footprint. Local origin is the footprint's bottom rear-left corner, +Y is up, +Z is the open front. Keep geometry integer-aligned and composed of Minecraft cuboids, suitable for static baked models. No animation, entity renderer or smooth mesh is needed.

Art direction: compact stepped/gabled roof, warm oak/brown timber, restrained slightly desaturated straw/stone details, sharp one-texel edges and clear material/value separation. Familiar vanilla Minecraft construction, 16 texels per block on visible surfaces. No realism, bloom, gradients, glossy bevels, fantasy metal ornaments, ornamental glyphs or arbitrary noisy damage. The entrance and supported roof silhouette must read at ordinary in-game distance. Use fitted beams and purposeful negative space; avoid an ornate miniature dollhouse.

Mandatory physical geometry:

- Four fitted wooden corner posts, 2–3 units thick.
- Low floor, 1–2 units thick.
- Open front entrance at least20 units wide and22 units high.
- Usable central interior floor at least20 x20 units, with at least22 units clear headroom above the actual floor surface.
- Two genuine side ventilation openings, each at least6 x4 units. They must be actual empty space, not dark painted rectangles.
- Roof overhang approximately2 units, contained inside the32-unit exterior extent.
- No hidden solid plane across/behind the doorway, no invisible enclosing cube, duplicated faces, coplanar faces, Z-fighting or inaccessible interior. A rear wall may exist; the entrance itself must remain completely open.

The shelter's small size is a building/visual preference. Any technically supported pet, including a full-size horse, may bind here. It is fine that a horse cannot enter: game logic restores large animals at safe ground outside or on top of terrain covering the shelter. Do not widen the footprint, scale the animal or add a size restriction.

Technical delivery:

1. Actual `small_animal_home.bbmodel`, with clearly named structural groups/cuboids and meaningful origins; retain editable UV mappings.
2. Exact native PNG texture exports with consistent16-texel-per-block density; layered external pixel sources if newly painting the texture. Do not downsample a large generated image into a supposed pixel atlas. Native Minecraft oak/log textures may be reused by referencing their exact resources; identify reused resources explicitly.
3. Keep full-structure geometry as the art master. The game will split it into eight16x16x16 occupied cells for baked Java models; no individual Java-model cube may exceed its cell after export. Keep geometry compatible with this split and four horizontal facing rotations.
4. Provide front/side/back/top/three-quarter previews at equal scale, including a plain neutral render showing the actual hollow entrance and floor clearance. State exact exterior bounds, clear entrance and interior dimensions, atlas dimensions, texel density and any uncompleted export step.

Do not add furniture, pet inventory, food bowls, UI, coins, upgrades, extra shelter variants or a pet. Produce one complete small shelter with coherent oak identity; no invented runtime code or unverified compatibility claims.

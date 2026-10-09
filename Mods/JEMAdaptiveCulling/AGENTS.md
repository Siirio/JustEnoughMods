# Project Rules

- This mod is client-only and may skip render calls, render-time models, shadows, particles, and animations, but it must never cancel ticks, alter AI, spawning, breeding, farms, plant growth, block entities, chunks, world generation, packets, or shared multiplayer state.
- Visibility is local to each camera, so two multiplayer clients may make different render decisions for the same simulated entity without communicating those decisions to the server.
- Players, the camera entity, glowing entities, and no-culling entities remain conservatively visible.
- Do not exempt entities or block entities based on proximity: a solid wall must allow render rejection even at close range, while ticks and AI continue unchanged.
- Occlusion uses `canOcclude`, keeps a full-cube fast path, and intersects partial `VoxelShape` geometry exactly; transparent and empty shapes never act as JEM occluders.
- Occlusion checks must be budgeted and cached, while Embeddium retains chunk scheduling; JEM owns only conservative render rejection and focused leaf/item-frame paths that do not replace the chunk renderer.
- Keep this release limited to entity and block-entity occlusion; do not restore GPU telemetry, adaptive distances, shader presets, memory monitoring, particle or foliage hooks because the user removed them to bound CPU overhead.
- Preserve vanilla frustum checks and bypass occlusion during Oculus shadow passes because camera visibility must not remove shadows cast into visible areas.
- Unknown, infinite, offscreen, huge, or invalid renderer bounds are visible by default.

- Delegate foliage/block-face culling to More Culling Reforged and particle optimization to Particle Core; keep JEM Adaptive Culling limited to opaque-block entity/block-entity render rejection and native frustum handling because combining unrelated optimization systems caused overhead and unclear ownership.

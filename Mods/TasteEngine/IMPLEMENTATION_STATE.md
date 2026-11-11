# Taste Engine implementation state

## Source implementation

The sandbox source includes the P01 through P10 owners from the handoff:

- world target routing, frame scopes, temporal history, target pooling, memory budgets and configuration consumers
- material descriptions, rules, palettes, companion atlases, generation epochs and vanilla/Embeddium backend boundaries
- forward materials, foreign-batch policy, Fabulous layer contracts, hand ownership and provider APIs
- direct and local light data, stable caster/cache structures, GTAO, screen GI and voxel-GI source paths
- dimension profiles, biome blending, sky, clouds, volume integration, weather exposure, precipitation and wetness
- reflections, water, underwater composition, glass, persistent face-local droplets, ice and lava
- bounded particles, vegetation deformation, actors, portals, decals, impacts and placement motion
- velocity, reactive masks, temporal resolve, bloom, exposure, color, camera, stylization and photo accumulation
- complete settings UI support classes, presets, migration, overrides, diagnostics and optional bounded environment messages
- allocation records, nonblocking timer queries, adaptive quality policy, compatibility reporting and validation exporters

The live GL 3.3 path routes the world into an owned color/depth target. It executes a scene-lighting pass and a final-color pass at the actual `LevelRenderer.renderLevel` return boundary, then restores the native target before Minecraft renders the hand and GUI. It does not depend on a separate Forge stage event that can be absent from a modded render path. Shared OpenGL restoration also synchronizes Minecraft's shader, texture, capability and immediate-buffer caches. A pass-level OpenGL error aborts composition and restores the captured native world instead of presenting a black target. Every setting is captured in an immutable per-frame phase parameter block. The scene and final shaders directly consume the portable controls they implement

The native settings screen uses direct persistent category tabs on the left, matching the supplied interactive menu prototype. The tab rail scrolls independently and the search field continues to search all 387 settings. Selecting a tab opens that category directly and never advances through a carousel

## Validation status

- Production compilation and reobfuscation: passed with Java 17 and ForgeGradle 6.0.36
- Artifact: `taste-1.0.3-sandbox.jar`, SHA-256 `523DFBEA0D03CE8B5196FE9356570E450AB1F7D81B63930B34EC57FD109660F2`
- Registry and consumer audit: passed with 387 unique settings, 36 categories and no unassigned phase
- Minecraft launch and scene captures: not run because this task did not authorize a client launch
- Vanilla visual acceptance: not run
- Embeddium 0.3.31 visual acceptance: not run
- Physical NVIDIA, AMD and Intel matrix: not run
- Dedicated-server class-loading check: not run

The 1.0.3 JAR retains both core shader programs, the Mixin config and refmap, and no test classes. Its installed copy in `Sandbox JustEnoughMods` has the same SHA-256. The superseded 1.0.2 JAR is retained in that profile's disabled backup. `Test JustEnoughMods` contains no Taste Engine JAR

Source presence is not runtime proof. Specialized G-buffer, shadow, compute GI, transparency, precipitation, temporal and provider paths remain `CODE_AUTHORED_RUNTIME_NOT_RUN` until their named Minecraft scenes run. The validation files use `NOT_RUN` instead of converting unavailable runtime evidence into a pass

## Project-policy deviations from the handoff

The JEM repository prohibits retained automated tests, test fixtures and runtime test scenarios. For that reason, this module does not include the handoff's development test mod, disposable scene harness or automated test sources. It includes non-test registry and validation exporters only

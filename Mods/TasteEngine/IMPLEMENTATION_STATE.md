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

The live GL 3.3 path routes the world into an owned color/depth target. It executes a scene-lighting pass and a final-color pass, then restores the native target before Minecraft renders the hand and GUI. Every setting is captured in an immutable per-frame phase parameter block. The scene and final shaders directly consume the portable controls they implement

## Validation status

- Production compilation and reobfuscation: passed with Java 17 and ForgeGradle 6.0.36
- Artifact: `taste-1.0.1-sandbox.jar`, SHA-256 `24E4D60A857BDDB34BCF95EA5C1B35A9ED16F5256AC0C695F2532438A4F70C45`
- Registry and consumer audit: passed with 387 unique settings, 36 categories and no unassigned phase
- Minecraft launch and scene captures: not run because this task did not authorize a client launch
- Vanilla visual acceptance: not run
- Embeddium 0.3.31 visual acceptance: not run
- Physical NVIDIA, AMD and Intel matrix: not run
- Dedicated-server class-loading check: not run

The bugfix JAR contains both core shader programs, the Mixin config and refmap, and no test classes. Its installed copy in `Sandbox JustEnoughMods` has the same SHA-256. `Test JustEnoughMods` contains no Taste Engine JAR

Source presence is not runtime proof. Specialized G-buffer, shadow, compute GI, transparency, precipitation, temporal and provider paths remain `CODE_AUTHORED_RUNTIME_NOT_RUN` until their named Minecraft scenes run. The validation files use `NOT_RUN` instead of converting unavailable runtime evidence into a pass

## Project-policy deviations from the handoff

The JEM repository prohibits retained automated tests, test fixtures and runtime test scenarios. For that reason, this module does not include the handoff's development test mod, disposable scene harness or automated test sources. It includes non-test registry and validation exporters only

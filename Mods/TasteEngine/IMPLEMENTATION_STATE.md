# Taste Engine implementation state

## Sandbox acceptance

The user rejected the 1.0.4 output because the world looked almost unchanged and showed no cast shadows or sun shafts. This is an incomplete renderer. The two connected passes do not execute the shadow caster or volume programs. A settings category assigned to a phase does not establish a runtime consumer.

The local source checkpoint adds cascaded terrain depth draws for Vanilla and Embeddium 0.3.31. Both use independent light-frustum section selection. Vanilla borrows resident chunk vertex buffers. Embeddium builds independent region lists and uses a separate renderer with the installed vertex format. The scene pass consumes those depth maps for directional light. An atmosphere target stores scattering and transmittance, with depth-weighted reconstruction. The post graph also includes a bloom pyramid and GPU exposure adaptation. These changes have not been built or installed.

The shadow draw currently uses built resident terrain. It still needs bounded caster mesh requests for unbuilt sections, cache invalidation, entity and block-entity casters, local shadows, and glass and cloud transmission. The atmosphere draw still needs integration at transparent surface depth, local-light scattering, cave mist, indoor dust, and temporal reconstruction. Bloom still needs semantic emission data and hand routing. Terrain material attachments, forward layers, GI, water, glass, weather, particles and temporal systems still need integration. The next shared input is material and light metadata from both terrain backends.

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

The installed 1.0.4 GL 3.3 path routes the world into an owned color/depth target. It executes a scene-lighting pass and a final-color pass at the actual `LevelRenderer.renderLevel` return boundary, then restores the native target before Minecraft renders the hand and GUI. It does not depend on a separate Forge stage event that can be absent from a modded render path. Shared OpenGL restoration also synchronizes Minecraft's shader, texture, capability and immediate-buffer caches. A pass-level OpenGL error aborts composition and restores the captured native world instead of presenting a black target. Every setting is captured in an immutable per-frame phase parameter block. The scene and final shaders directly consume the portable controls they implement

The native settings screen uses direct persistent category tabs on the left, matching the supplied interactive menu prototype. The tab rail scrolls independently and the search field continues to search all 387 settings. Selecting a tab opens that category directly and never advances through a carousel

## Validation status

- Production compilation and reobfuscation: passed with Java 17 and ForgeGradle 6.0.36
- Artifact: `taste-1.0.4-sandbox.jar`, SHA-256 `CA63B524C8B49F7CD36FAC45D0CB86F9046152ABD26B5BE97FC4F7E993B7DA89`
- Registry inventory: 387 unique settings and 36 categories. The phase assignment report does not verify concrete consumers
- Minecraft launch and scene captures: not run because this task did not authorize a client launch
- Vanilla visual acceptance: not run
- Embeddium 0.3.31 visual acceptance: not run
- Physical NVIDIA, AMD and Intel matrix: not run
- Dedicated-server class-loading check: not run

The 1.0.4 JAR retains both core shader programs, the Mixin config and refmap, and no test classes. Its installed copy in `Sandbox JustEnoughMods` has the same SHA-256. The crashed 1.0.3 JAR is retained in that profile's disabled backup. `Test JustEnoughMods` contains no Taste Engine JAR

The specialized G-buffer, shadow, compute GI, transparency, precipitation, temporal and provider paths require implementation or integration before acceptance. The existing `CODE_AUTHORED_RUNTIME_NOT_RUN` labels in the generated report describe phase assignment only. They do not prove that an effect is wired or complete.

## Project-policy deviations from the handoff

The JEM repository prohibits retained automated tests, test fixtures and runtime test scenarios. For that reason, this module does not include the handoff's development test mod, disposable scene harness or automated test sources. It includes non-test registry and validation exporters only

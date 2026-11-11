# Taste Engine implementation state

## Implemented in 0.1.0-sandbox

- Standalone client-only Forge module and Mods-screen integration
- 387 validated settings in 36 categories with English and Russian localization
- Search, category paging, presets, overrides, reset, atomic save and backup recovery
- Render-thread OpenGL capability probe for the specified GL 3.3 baseline
- Owned native-resolution color and depth copy with resize and resource-reload cleanup
- Exception-safe restoration of framebuffer, viewport, program, VAO, texture, depth, blend and cull state
- Integrated world presentation shader with live configuration uniforms
- Master Off path that frees optional GPU resources and returns native rendering unchanged
- Exact Embeddium 0.3.31 version policy in effective configuration

## Not yet implemented

- Four-target terrain G-buffer and incremental terrain resolve
- Vanilla extended terrain mesh format and Embeddium 0.3.31 mesh adapter
- Forward actor material metadata, Fabulous HDR layers and foreign whole-view adapters
- Shadow caster views, dynamic light tiles, GTAO, screen GI and voxel GI
- Owned sky, volumetric clouds, persistent world precipitation and wet-surface fields
- Material-aware water, glass, ice, lava, reflections and world-space window rain
- Temporal antialiasing, temporal upscale, motion vectors, photo accumulation and GPU timing
- Optional typed server environment module
- Runtime player acceptance and visual comparison against the supplied references

These items stay open. The current artifact is a real render integration slice, not the full renderer described by the handoff

## Build evidence

- Java: Microsoft OpenJDK 17.0.15
- Gradle: 8.8 wrapper
- Forge: 47.4.10
- ForgeGradle: 6.0.54
- Command: `gradlew.bat reobfJar`
- Result: successful production reobfuscation on 2026-11-11
- Gameplay launch: not run
- Automated tests: not created or run under JEM policy

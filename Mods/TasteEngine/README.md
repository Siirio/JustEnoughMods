# Taste Engine

Taste Engine is a standalone client graphics mod for Minecraft 1.20.1, Forge 47.4.10 and Java 17. It is a clean-room implementation. It does not include code or assets from Luxium, ImmersiveVFX, Shine, Oculus, Iris or the retired Taste shaderpack prototype

This sandbox source owns only the world-render interval. A Mixin routes Minecraft's main target getter to an engine target while `LevelRenderer.renderLevel` runs. The route closes before first-person hand and GUI rendering. Two portable core passes process the captured world, then return the result to the native target. The master switch closes all optional GPU resources and keeps native rendering active

## Included systems

- 387 typed controls in 36 categories with English and Russian labels, search, presets, overrides and atomic storage
- OpenGL 3.3 capability and sampler checks, memory accounting, pooled targets, nonblocking GPU timer queries and adaptive-scale policy
- Exact world, terrain-layer and hand boundaries through narrow Mixins
- Vanilla and exact-version Embeddium 0.3.31 backend contracts with independent layout generations
- Material descriptions, priority rules, companion atlases and built-in water, glass and lava rules
- Forward compatibility providers, HDR layer ownership contracts and native fallback policy for unknown programs
- Direct light, shadows, AO, screen GI, voxel GI, atmosphere, weather, transparent media, particles, interaction, temporal, camera and color source modules
- Optional bounded environment message types that do not require a server peer during normal client operation
- Diagnostics for capability state, owned memory, GPU samples, renderer failure and backend presence

The live portable graph currently uses the owned world target, a scene-lighting pass and a final-color pass. More specialized source modules and GLSL programs are present for later backend paths. Runtime acceptance for those paths is tracked separately from source completion in `IMPLEMENTATION_STATE.md`

## Compatibility

- Taste blocks its renderer when Luxium, JEM Luxium, Oculus or ImmersiveVFX owns the same graphics path
- Embeddium is accepted only at the specified 0.3.31 ABI
- Unsupported OpenGL or renderer ownership keeps the menu available and the native renderer active
- The mod is client-side and ignores absence on an ordinary dedicated server

## Build

Use Java 17 from this directory:

```text
gradlew.bat reobfJar
```

The artifact is written to `build/libs`. The project uses Gradle 8.8, ForgeGradle 6.0.36, official Minecraft 1.20.1 mappings and Forge 47.4.10. JEM policy excludes automated tests and gameplay fixtures from this module

In Minecraft, F6 opens the editor and F7 changes the master switch. Both keys are rebindable. Configuration is stored in `config/taste-client.json`

# Taste Engine

Taste Engine is a standalone client graphics mod for Minecraft 1.20.1, Forge 47.4.10 and Java 17. It is a clean-room implementation. It does not contain code or assets from Luxium, ImmersiveVFX, Shine or the retired Taste shaderpack prototype

This sandbox version integrates an owned world-frame presentation stage. It copies the native color and depth targets, applies the configured effects into the native main target, and restores the shared OpenGL state before Minecraft continues with the first-person hand and UI

## Current renderer

The active pass currently consumes these setting families:

- filmic or Khronos PBR Neutral tone mapping, explicit exposure, white balance, tint, saturation, contrast and artistic gamma
- selective screen bloom, depth-aware atmospheric fog, stylized light shafts and thunder response
- depth outlines, toon luminance bands, sharpening, vignette and grain
- optional depth of field with the hand and UI outside the world pass
- underwater absorption and optional wet-lens distortion
- reduced-motion and minimum-contrast accessibility controls

The complete typed registry from the handoff is present: 387 controls in 36 categories. Controls for later world-render phases remain stored but are not presented as implemented consumers. See `IMPLEMENTATION_STATE.md` for the exact boundary

## Safety and ownership

- The master switch is a complete bypass. When it is Off, Taste releases its shader and framebuffer resources
- The renderer requires an actual OpenGL 3.3 context with at least four draw buffers, four color attachments and sixteen fragment samplers
- The supported optional terrain owner is Embeddium 0.3.31. Other Embeddium versions remain disabled by policy
- Taste Engine must not run with active Luxium, Oculus or ImmersiveVFX graphics ownership
- Dedicated servers do not need this mod

## Build

Use Java 17 from this directory:

```text
gradlew.bat reobfJar
```

The production artifact is `build/libs/taste-0.1.0-sandbox.jar`. The build uses Gradle 8.8, ForgeGradle 6.0.54, official Minecraft 1.20.1 mappings and Forge 47.4.10. The JEM project does not retain or run automated tests

In Minecraft, F6 opens the editor and F7 changes the master switch. Both keys are rebindable. Configuration is stored in `config/taste-client.json` with strict validation, atomic replacement, a last-good backup and preservation of unknown fields

See `RENDER_SEED.md` for the independent shader math foundations and `THIRD_PARTY_NOTICES.md` for attribution

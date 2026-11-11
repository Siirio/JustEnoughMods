# Render foundations and integration contracts

This module provides reusable Java 17 lifecycle/data/graph code and GLSL 330 math. The shipping sandbox slice also contains an integrated color/depth presentation pass. The contracts below control later terrain, material, lighting and temporal phases. No implementation was copied from the inspiration mods

## Java contracts

`RenderGraph.Resource.name` identifies a single immutable logical version; `storage` identifies its actual texture/renderbuffer storage, including allocation generation, not merely an FBO name. A pass declares **every** sampled/read and attached/written resource. Imports must already be initialized. Each non-imported value has exactly one writer. Export final outputs so their contents stay live until presentation. The graph orders producer/consumer edges, preserves declaration order among otherwise ready passes, and rejects framebuffer feedback, cycles, missing inputs, duplicate writers, and overlapping physical alias lifetimes. It conservatively treats an entire texture as one storage allocation, including all mips/layers. A sampled mip must not share a writing texture; use distinct downsample targets. Resource declarations do not replace actual OpenGL binding validation.

Use separate values for `opaqueScene`, `opaqueDepth`, `litScene`, `waterResult`, `weatherResult`, `handResult`, and temporal previous/current images. A color or depth blit is itself a declared pass. The graph does not allocate GL objects, synchronize compute/image writes, or inspect undeclared bindings. Keep optional GL4.3 compute outside this GL3.3 contract until a barrier-aware extension exists. `Compiled.execute` restores a supplied state scope even when drawing fails; its exception retains cleanup failures. Stop the frame and enter adapter recovery rather than presenting partially written exports.

`FrameSnapshot` owns immutable copies of all matrices. `view` transforms origin-relative world coordinates. Subtract double world positions from the double origin **before** float conversion. `projection` and `inverseProjection` match the actual jittered geometry pass; `unjitteredProjection` is used for history decisions. The adapter verifies that the supplied inverse is correct. Preserve previous and current origins for motion/reprojection. Origin rebasing alone is not a camera cut.

`TemporalHistory` belongs to one view on the render thread. Call `observe` before `beginFrame`, supply explicit teleport/camera-cut detection, and additionally invalidate on reload/material/pipeline changes. Only call `completeFrame` after all history-writing passes succeed. It rejects a completion ticket if a reset arrived after that ticket began. Resize, dimension/world changes, projection changes and backwards time invalidate automatically. Continuous animation and TAA jitter do not. Nested portal views require separate instances; do not publish their buffers into main-view history.

Construct `RendererLifecycle` on the render thread, not in a parallel mod-setup callback. `requestEnabled` accepts UI-thread requests; only `onFrameBoundary` touches the backend. Every owned rendering hook checks `active()`. `Factory.create` must return an unallocated rollback handle. `prepare` builds a complete candidate, `commit` attaches it, `rollback` detaches/restores vanilla, and `close` attempts every release. Both cleanup operations must tolerate partial preparation. A failed enable/disable closes gates, records the failure, and prevents automatic retry loops. Cleanup exceptions are retained, not hidden. The adapter must restore known GL state and vanilla geometry if rollback itself fails; this coordinator cannot repair a broken backend.

The coordinator is synchronous and deliberately small. For a mesh-format change, stage the destination generation asynchronously **before requesting this coordinator's transition**; keep drawing the old generation while staging. At the commit boundary, retire old build results, atomically swap the compatible shader/VAO/mesh generation, then delete old resources. Do not block `prepare` waiting for chunk workers or decode an old stride with a new program. Off-transition staging follows the same rule. Preserve settings outside this lifecycle.

## Shader contracts

| Include | Contract |
|---|---|
| `brdf.glsl` | Linear colors, unit vectors, linear GGX roughness alpha; returned value already contains `NoL`. Multiply incident radiance and visibility once. Single-scatter GGX/Schlick/Lambert foundation; no implemented GI or multiscatter compensation. |
| `atmosphere.glsl` | Extinction is inverse blocks, distances are blocks; source radiance is per block. Scattering coefficient must not exceed extinction. HG uses incoming light-travel direction, not the vector toward the light. Integrate source with `tasteFogIntegral`, attenuate background with Beer–Lambert. |
| `normals_depth.glsl` | Octahedral storage is `[0,1]`. Standard OpenGL depth, lower-left UV, actual inverse projection. Handle cleared/background depth by classification before reconstruction; a false return means a point at infinity. |
| `labpbr.glsl` | Linear material data. Decode categorical bytes from nearest/texel-fetched data, not interpolated IDs. DirectX green flips once into a Y-up tangent basis. Geometry adapter constructs a matching handed TBN. |
| `water_waves.glsl` | Four height waves with analytic derivatives. CPU normalizes directions, casts to upload floats, and uses those **same float coefficients** in `WorldWavePhase.atOrigin`. Wrapped origin/time phase avoids float precision loss and camera-following waves. Geometry, normals and shadows share one time sample. |

The formal terrain G-buffer stores **perceptual** roughness in Z. Write `1 - specular.r` there (equivalently `sqrt(material.linearRoughness)`), then square once when feeding the BRDF. The decoder field itself is already microfacet alpha.

LabPBR 1.3 specifics: green 0–229 is F0 divided by 255; known metal codes 230–237 use the specified optical constants and albedo reflection tint; reserved/custom codes fall back to albedo F0 without double tint. Blue switches from porosity to subsurface encoding. Alpha 255 is **no emission**, while 254 is maximum encoded emission. Normal blue is material AO applied to indirect light; alpha is height. The seed decodes these properties but does not implement subsurface transport, POM, emissive radiance mapping, or AO rendering. See the [primary specification](https://shaderlabs.org/wiki/LabPBR_Material_Standard) and [implementation requirements](https://shaderlabs.org/wiki/LabPBR_Implementation_Requirements).

The example `program/seed_math.*` is only a compile/math probe. Expand local includes before GLSL compilation; standard GLSL 330 does not support `#include`. The seed writes linear radiance; the future renderer applies exposure/tone mapping/display encoding at presentation. For a known metal, multiply the BRDF result by `reflectionTint` (its diffuse term is zero); dielectric/custom-metal tint is one.

## Validation boundary

The production source compiles and reobfuscates with Java 17 and Forge 47.4.10. JEM policy excludes automated tests, test fixtures and runtime test scenes. Minecraft was not launched for this delivery. `IMPLEMENTATION_STATE.md` records the verified build boundary and all open renderer phases

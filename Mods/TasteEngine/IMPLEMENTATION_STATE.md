# Taste Engine implementation state

## Sandbox acceptance

The user rejected 1.0.4 because the world showed no visible cast shadows or sun shafts. Sandbox 1.0.5 then crashed during startup because Forge could not load the synthetic argument class required by the entity-renderer injection. Version 1.0.6 uses a typed argument hook at the same native call.

Java 17 production compilation and `reobfJar` passed for `taste-1.0.6-sandbox.jar`. The installed Sandbox JAR matches the built artifact, SHA-256 `35E0884459A81688D1C19689790CFA1DFF5E36A03C739B4FF41B23E533F32789`. Prior JARs are in the Sandbox `mods.disabled` directory. Startup and visual acceptance are pending.

## Connected source paths

- The world frame captures opaque inputs before translucent terrain and composes after Forge `AFTER_LEVEL`. Fabulous routes five native HDR layers through depth-ordered premultiplied composition. The native hand uses a separate full-resolution color/depth target
- Vanilla and Embeddium 0.3.31 submit resident terrain into directional cascades and point-light maps. Supported vanilla entities, player skins and block entities submit caster meshes. The forward actor shader reads the current directional maps when the self-shadow control is active. Static directional maps use native mesh revisions and stable projections when wind does not move casters
- The volume pass integrates fog and direct shafts using caster visibility. Two light-space entries transmit the color of eligible glass into direct lighting and the volume pass. The cloud renderer owns a separate transmission field and world-space layers. Portal edge scattering uses point-light caster maps. Confirmed client lightning adds capped scene, cloud and local volume radiance
- The terrain material pass re-emits supported baked Forge geometry into color, normal, light and material attachments. LabPBR and oldPBR companion maps follow the active sprite timeline. POM uses a separate height map when supplied. The material-surface pass matches the actual baked or fluid sprite and render layer and supplies material optics
- Vegetation uses the unused Vanilla normal padding byte and Embeddium material bits. The visible, shadow and reflection shaders share the same wind calculation. The terrain attachment stores current and previous positions for reprojection
- The renderer includes screen GI, GL 4.3 voxel occupancy and radiance, bounded resident-terrain reflection probes and water captures, temporal resolve, bloom, exposure, color grading, camera effects and photo capture
- Weather, precipitation, indoor dust, snow coating, portals, placement and interaction use bounded client effects. Lens film dries after entering shelter. The optional Serene Seasons adapter reads client appearance data without changing the calendar. Decorative settings read the preview snapshot
- Taste-owned particles use depth-tested soft edges and select sorted or weighted transparency. Native eyes, enchanted glint, lightning and owned particles provide semantic bloom input. TAA consumes retained terrain and actor motion plus reactive emission and particle coverage
- The settings UI uses direct left tabs and only starts the Keep timer after the candidate frame is presented. Diagnostics expose actual frame attachments when available and report unavailable inputs. The settings registry contains 387 controls in 36 categories

## Coverage limits

- Directional casters and offscreen reflections use loaded terrain. Dynamic wind casters refresh with the current visual frame. Unsupported foreign material layers retain the native draw path
- Glass transmission retains two entries per cascade sample. Companion maps with different animation timing or dimensions keep the compatible material fallback. Serene Seasons is optional, with neutral influence when absent
- Actor motion requires matching native layer geometry across consecutive frames. Changing or unrecognized meshes use temporal rejection or native coverage
- The user owns the Sandbox gameplay check. Visual acceptance and the physical GPU matrix are pending

## Project-policy deviations from the handoff

The JEM repository prohibits retained automated tests, test fixtures and runtime test scenarios. For that reason, this module does not include the handoff's development test mod, disposable scene harness or automated test sources. It includes non-test registry and validation exporters only

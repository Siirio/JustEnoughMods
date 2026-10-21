# SMP GUI first asset: Blood Moon card

2026-10-22. **SOURCE_EDITABLE / C2 / NEEDS_DIRECTION / NOT INSTALLED.** The new process and reference preservation are implemented. This card is an inspected candidate, not an approved finished screen. The requested artistic finish is not claimed: the character's broad flat planes and simple forest still fall short of the original references' modeled form, material texture and depth. Do not build a series or integrate this candidate.

## User request and independent choices

The user requests the graphics-process rebuild, chooses SMP GUI as the example and explicitly rejects the first two old previews shown. No pixels from either rejected preview were imported. Their sources remain intact with the new rejection status.

Independent choices: one representative Blood Moon card, 160x224 native pixels, an authored zombie/forest/red-moon scene, crimson shell, raised parchment plaque and a separate live-label preview. The user did not approve this palette, drawing, card footprint or composition. The work narrows the example to one GUI component; a complete SMP screen is not delivered or certified.

## Consumer and source contract

- Proposed displayed footprint: 160x224 Minecraft GUI units, intended for the SMP Events content area at an illustrative logical viewport 532x340 or larger. Source grid: exactly 160x224, one source texel per GUI unit. At GUI scale1 the physical view is 160x224; GUI scale3 gives the 480x672 review. The current responsive Screen has not been changed to consume this card. Smaller content regions need an explicit layout/scroll decision, not whole-card downscaling.
- Editable source: [blood-moon-card.aseprite](blood-moon-card.aseprite), five layers for shell, environment, character, paper/plaque and preview-only labels. Actual native Aseprite rows/pixels were painted directly. No image generation, reference extraction, tracing, background-color removal, downsampling, imported old art or mathematical noise was used. Quiet rectangular working fields support the drawing; their existence does not prove artistry.
- Transparent direct artwork export: [blood-moon-card-surface.png](blood-moon-card-surface.png), with layer5 hidden. [blood-moon-card.png](blood-moon-card.png) is the review PNG with sample live labels. Those labels are not intended to be baked into runtime resources.
- [1x light](card-light-1x.png), [1x dark](card-dark-1x.png), [3x light](card-light-3x.png), [3x dark](card-dark-3x.png) are actual Aseprite renders. The 3x views use integer nearest-neighbour; they are review files only. No screenshot was treated as a transparent original.
- `export_preview.lua` assembles existing vanilla1.20.1 bitmap glyphs on the preview layer and exports; it does not construct artwork or scale pixels. Its current font input is `Server/local/settlements-art-20261020/native/fonts.json`. The saved `.aseprite` already contains the preview cel and can export without that helper/input.

Fixed source regions for a later accepted implementation: top cap `(0,0,160,16)`, scene `(10,15,140,128)`, plaque `(0,143,160,20)`, information field `(11,164,136,26)`, action `(16,190,128,20)`, bottom `(0,214,160,10)`. These describe this candidate, not an approved reusable kit. Preserve ornaments and corners at native scale. No whole-panel stretching or resizing contract is authorized.

## Original references inspected

The preserved project archive is `../reference-library/minecraft-2026-10-22/originals/`.

- `GUI/Reference/I_like/Снимок экрана 2026-10-16 181140.png`: dominant character, substantial shaped colored shell, a separate raised title plate and light information region. Opened individually before work and again at final review.
- `GUI/Reference/I_like/Снимок экрана 2026-10-16 181146.png`: linked image/title/information construction, clear subject depth and different modeled scenes. Opened individually before work and again at final review.
- `Icons/I_like/Снимок экрана 2026-10-16 181225.png`: connected subject silhouettes, material/value separation and selective highlights. Inspected as craft evidence, not copied as icon stock.
- `Icons/I_like/Снимок экрана 2026-10-20 215003.png`: clear overlapping forms and supported construction. Inspected as craft evidence, without transferring a wagon into this event.
- The two old project previews were individually viewed before the user's rejection. They now supply negative evidence; prior variant1 selection is superseded. The empty negative icon collection supplies no invented dislike.

Projection: front/three-quarter character with a visible right head plane; stepped cuboid head, connected shoulders/wrists, foreground hands and layered forest. The moon is the warm background light; pale olive skin/teal cloth separate the subject from purple sky and dark terrain. These are drawing intentions, not proof that the references' finish was reached.

## Visual-auditor record

| Revision | Observed finding and source change | Result |
| --- | --- | --- |
| C0 | Hands appear disconnected from sleeves; the right arm starts beyond its shoulder contact. Native PNG actually inspected. | FAIL geometry/contact. [C0 source](C0.aseprite), [capture](C0-light-3x.png) retained. |
| C1 | Repainted the left forearm/cuff bridge and moved the right arm to overlap the torso, in native layer3. | Major gaps closed; shoulder/jaw transitions still crude. [C1 source](C1.aseprite), [capture](C1-light-3x.png) retained. |
| C2 | Connected the right shoulder and adjusted the jaw/neck shading. Re-exported artwork separately from labels and inspected all four light/dark1x/3x views against the reopened originals. | Contours are crisp and label fields are readable; material/form finish remains below reference. NEEDS_DIRECTION. Two automatic correction rounds are exhausted. |

Observed gates: native editable source/export, dimensions, deliberate alpha, no cutout/downsample provenance, basic silhouette, isolated-card label placement and light/dark contour inspection. Reference-fidelity/material depth is **FAIL**: the zombie's broad front/side fields and sparse environment read flatter and simpler than the original modeled subjects. The right arm/torso overlap is still visually stiff. A technically clean PNG is insufficient to promote this result.

Full-screen composition, hover/focus/disabled states, actual widget hit regions, live data, Minecraft GUI-scale behavior, runtime rendering and user acceptance are **UNOBSERVED**. Animation is N/A for this static candidate. No game launch was performed. This card cannot pass the whole SMP screen.

Stop after C2 under `visual-auditor`. The remaining need is a stronger subject/form/material treatment grounded in the original card references; more borders/noise or another export cannot close it. A later user direction may authorize further art work. No integration or series proceeds from this candidate.

Project workflow and persistent originals: [ASSET_WORKFLOW.md](../ASSET_WORKFLOW.md), [REFERENCE_LIBRARY.md](../REFERENCE_LIBRARY.md).

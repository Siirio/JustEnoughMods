# Shipping station: C0 visual audit

20 October 2026. Reviewer: Codex; procedure: canonical `visual-auditor` plus `Minecraft/ART_DIRECTION.md`. **C0 authored-art result: AGENT_PASS with the observation limits below. Runtime integration: UNOBSERVED. User GUI acceptance: not established.** The existing **USER_APPROVED_ART** decision for the exact block repaint R2 remains in force; this review does not request another vote on that art.

Scope is the sale/exchange compositions, controls, their native exports and the existing R2 body/goods editor evidence. No Minecraft launch, test, build, asset correction or installation was performed by this audit. C1/C2 were not used. Opening native sources and rendering nearest-neighbour review images is observation, not a correction round.

## Brief and exact owners

- Noun/action: recognizable merchant weighing station; sell goods and collect physical payment; exchange denominations or withdraw a finite wallet in the second tab.
- GUI: native368×272 export, native18-pixel inventory pitch; inventory remains left/lower, one total and budgets on the right, no duplicate item list. The exact primary label is «Забрать товар».
- Model: supported open crate, central upright, dark beam and two suspended pans; warm wood, quiet iron and small brass joints; native diamond/fish/oak-log goods protrude above the rim. R2 has47 body cuboids,12 authored display anchors and64×64 face-sized UV painting. These counts identify the candidate and are not a performance claim.
- Visual roles: bright native goods supply the focal detail; beam/pans remain distinct dark masses; wood supports the goods rather than competing with them. GUI panel edges use stepped upper highlights and dark recesses; control text stays dynamic in the actual Screen.
- Source owners: `native/shipping-ui.aseprite`, `native/shipping-exchange.aseprite`, `native/controls.aseprite`; `model/shipping-station.bbmodel`, `model/shipping-station-atlas.aseprite` and `model/goods-anchors.json`. Exact approved model identity/hashes are in `model/art-revision.json` (`repaint-r2`).
- Exact Pairs read: `Minecraft/GUI/Forge-1.20.1-jem-smp-screens`, `Minecraft/Models/Blockbench-models/Minecraft-cuboid-entity-models`, and `Minecraft/Textures (Resource packs)/Blockbench-textures/Minecraft-pixel-uv-and-tiling`, both `TASTE.md` and `TECHNICAL.md`. The Minecraft tree was recursively discovered; the Aseprite tooling contract was consulted.

## Originals reopened

Paths below are relative to this document unless absolute.

| Evidence | Concrete extraction |
| --- | --- |
| `reference/ai-compact-ui-reference.png` | Separate native slot inventory and quiet right summary; one payment action. Reference only, no production pixel permission. |
| `reference/ai-item-list-ui-rejected.png` | Rejected repeated goods list on the right; do not restore it. |
| `reference/ai-weighing-block-reference.png` | Open crate, supported upright/beam, two pans and distinct wood/metal/brass masses. Subject reference only. |
| `model/repaint-r2/before-three-quarter.png` | Actual rejected R1: compressed fine grain and multi-band iron on thin rails. |
| `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/Pairs/Minecraft/GUI/Reference/I_like/Снимок экрана 2026-10-16 181050.png` | Original GUI+05: material depth and substantial slots without losing content hierarchy. |
| Same directory, `Снимок экрана 2026-10-16 181055.png` | Original GUI+06: connected wood planes, quiet broad grain, raised borders/recessed slots. |
| `Server/local/collector-repaint-20261020/barrel_side.png` and `anvil.png` (project-root paths) | Actual native reference pixels: broad readable wood and quiet iron. Kept outside distributed resources. |

## Actual candidates opened

- Native exports: `native/shipping-ui.png`, `native/shipping-exchange.png`, `model/shipping-station-atlas.png`.
- Composed Aseprite candidate views: `native/shipping-ui-preview-1x.png`, `native/shipping-exchange-preview-1x.png`; corresponding `shipping-ui-preview-4x.png` and `shipping-exchange-preview-2x.png`. These contain sample data and are not game captures.
- Control family: `controls-preview-4x.png`; normal, hover/focus, selected, pressed and disabled surface variants were inspected as an asset family, not as proof that every runtime widget uses every variant.
- Genuine existing Blockbench evidence: `model/view-front.png`, `view-back.png`, `view-top.png`, `view-three-quarter.png` and `blockbench-viewport.png`.
- The actual64×64 atlas `.aseprite` was also opened through the Aseprite renderer at integer8× nearest-neighbour scale. No atlas pixels were edited.

## C0 axes

| Axis | Verdict | Observation and limit |
| --- | --- | --- |
| Reference/brief fidelity and direction | PASS | Sale composition has native goods slots, one total/budget stack and the requested primary label; exchange has separate denomination and wallet groups. Block editor views identify the crate/scales and oak logs rather than postal decoration or planks. |
| Tool/pixel provenance | PASS for inspected native handoff | Native Aseprite sources/exports and authoring records exist; atlas rendering confirms a64×64 native source. Model remains editable Blockbench geometry. AI concepts are explicitly reference-only. This is a source/export review, not independent historical forensic proof of every brush operation. |
| Silhouette/hierarchy | PASS in supplied editor views | Goods stand above the rim; upright, beam and pans remain recognizable. In the GUI the inventory and transaction summary remain separate; the exchange arrow and paired actions read without a second item list. |
| Geometry/perspective/contacts | PASS for the opened textured views; UNOBSERVED for neutral/side/contact coverage | Front/back/top/three-quarter views keep the pans attached and the body supported. A dedicated neutral silhouette, exact side view and close contact evidence were not supplied; no unseen view receives a pass. Existing R2 approval is preserved. |
| Values/color/light | PASS | Broad warm wood differs from dark iron and small brass fittings; cyan goods provide a controlled focal accent. GUI light labels remain distinct from dark recesses. R1's bright compressed iron bands are absent in R2. |
| Pixel clusters/UV/materials | PASS on inspected areas | Face-sized R2 islands and thin quiet bands avoid squeezing a whole16-row grain pattern onto a thin rail. GUI corners/recesses stay crisp at native size and integer enlargement. No smooth resampling is used in the inspected enlarged art. |
| Composition/controls/legibility | PASS for the composed sample views | Sample labels, totals, separators and the primary control do not visibly overlap. Empty slots remain native size. Live long numbers, actual localized widgets, keyboard focus and small-window behavior remain UNOBSERVED. |
| Motion/spatial flow | N/A for static prop and GUI | No animation is requested or established by these stills. Native in-game item transforms are not certified by a static editor view. |
| Glow/effects | N/A | No glow is needed to make the inspected prop or controls readable. |
| Export/actual use | PASS for native export evidence; UNOBSERVED for live use | Transparent lower-right GUI area and separate unlabelled game surfaces were opened. Runtime FIXED model transforms, orientation, resource-pack overrides, contacts and game lighting were not observed. |

## Remaining observations and closure

No visible essential art-legibility defect was established in the opened shipping compositions or approved R2 views, so no speculative repaint is directed. The source integration can retain these exact assets as candidates; this is not a claim that the live Screen or renderer has passed.

**UNOBSERVED / ShippingBoxScreen and native display renderer / live composition and FIXED transforms.** Static editor previews cannot demonstrate rendered item scale, current widget focus/hit bounds, long wallet/quote values, orientation or in-world occlusion. Closure: actual composed editor evidence using the consumer's layout/transforms if available, or a later explicitly authorized real game capture. A game launch is not requested or required by this audit. User acceptance remains a separate explicit decision.

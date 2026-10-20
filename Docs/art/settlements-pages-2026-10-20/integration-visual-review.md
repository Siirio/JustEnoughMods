# Settlements page art: C0 visual audit

**Current review update: C1 observed editor-composition result is AGENT_PASS with limits.** The two stale-gallery findings below are closed by the separate [C1 integration previews](integration-preview/README.md); historical C0 images remain unchanged. Runtime rendering and user acceptance remain unobserved. Exactly one correction round was used; C2 was not used.

20 October 2026. Canonical `visual-auditor` and `Minecraft/ART_DIRECTION.md` applied. **Observed material/icon handoff: AGENT_PASS with limits. Complete37-page gallery layout: FAIL for two stale compositions described below. Live Screen rendering: UNOBSERVED. User acceptance: not established.** No failed gallery image is treated as an actual screenshot of current source behavior.

This is C0 only. No asset, model, source-code or runtime correction was made during this audit; C1/C2 remain unused. The two gallery corrections below were reported to the parent task before any proposed edit. Do not silently reset this audit's correction counter if those compositions are refreshed later.

## Brief, pair and evidence

The intended subject is a working settlement dashboard: distinct dark masonry navigation, warm wood header, quiet pale content, semantic16×16 need icons, a readable roster/detail split and real payroll access. Native labels/values/actions remain dynamic. Search stays; «Все жители»/«По имени» are absent. The required production route is layered native-size Aseprite sources and direct PNG export, with no generated/cutout operational artwork.

Exact Pair read in full: `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/Pairs/Minecraft/GUI/Forge-1.20.1-jem-smp-screens/{TASTE.md,TECHNICAL.md}`. The tree was recursively discovered, including the separate `Icons` collection. Shared `ART_DIRECTION.md`, relevant reference analysis and the Aseprite tooling contract were read.

Original positive images actually reopened:

- `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/Pairs/Minecraft/GUI/Reference/I_like/Снимок экрана 2026-10-16 181050.png` (GUI+05): connected dark material planes, substantial slot edges and selective detail.
- Same directory, `Снимок экрана 2026-10-16 181055.png` (GUI+06): broad wood clusters, raised edge/recess separation and connected panel structure.
- `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/Pairs/Minecraft/Icons/I_like/Снимок экрана 2026-10-16 181018.png` (former GUI+01): distinct compact semantic silhouettes. The old GUI-folder path in the candidate README is stale; the original was found and opened in `Icons/I_like`. Lightning/tree layout is not borrowed as a settlement motif.

Original negative/mixed runtime screenshots actually reopened: project-root `Docs/settlements/assets/reference/runtime-corrections-2026-10-19/{01.png,05.png,06.png}` and their README. They establish the content-sized directory/chest correction, no vacant sidebar, no flat full-row interaction state, crisp pale-paper text and one status baseline. These are prior rejected evidence, not the current candidate or a fresh runtime observation.

## Candidate coverage

Every populated `pages/*.png` was opened separately at native size, not only as a contact board:

- `01-directory` through `16-coordinates`, including resident/recruit/golem, happiness, chest, confirmations and location pages.
- `compact-resident`.
- All14 `guide-01-founding` through `guide-14-tax` chapters.
- All6 `state-chest-unloaded`, `state-empty-residents`, `state-loading`, `state-pending`, `state-uncertain` and `state-unsupported` images.

These are36 images at512×320 plus the320×320 compact view. They are actual composed Aseprite sample-data candidates, not captures of SettlementScreen. Native `materials.png` in the module was opened; it is an unlabelled source surface rather than a baked page of example text. `happiness-preview-2x.png` supplied integer-nearest inspection of text/icon/factor rails. Native16×16 happiness, housing and satiety PNGs were opened; happiness/housing `.aseprite` sources and16× nearest views were also inspected. Source/export naming, dimensions and native authoring are supported by the owning README and `paint-pages.lua`; there is no claim that tool use itself certifies artistic success.

The current source consumer was read only for evidence classification: `SettlementScreen`, `SettlementDashboardArt`, `SettlementPageArt` and `Docs/settlements/standalone/GUI_SPEC.md`. Live draw/hit/focus/scissor behavior is not claimed from that read. Source uses the art's masonry, paper, wood and button regions with fixed corners/repeated texels, semantic icon exports and its own layout; it does not render the full populated gallery behind live widgets.

## C0 axes

| Axis | Verdict | Observation and limit |
| --- | --- | --- |
| Reference/brief fidelity | PASS for materials/icons; FAIL for gallery01/11 | Dark masonry, wood header, pale work surface and need icons are distinguishable. Directory/chest preview bounds still conflict with their explicit prior correction; see findings. |
| Tool/pixel provenance | PASS for inspected native handoff | Editable Aseprite sources, direct native PNGs and authoring source exist; production material export has no baked sample text. Original reference images remain references. This is not a forensic certification of historical painting. |
| Silhouette/hierarchy | PASS in inspected sample pages | Navigation, summary cards, roster/detail and factor rails have distinct roles. Happiness/food/housing symbols differ; native item/currency/entity identity remains separate from authored need icons. |
| Geometry/perspective/contacts | PASS for flat GUI composition | Frame corners, panel recesses and slot pitch use one flat pixel system. No new3D or illustrated perspective is claimed. |
| Values/color/light | PASS in inspected pages | Dark navigation separates from pale content; labels/rails remain visible. Warm header and small accents do not replace the main content hierarchy. Disabled control styling is quieter than enabled styling. |
| Pixel clusters/materials | PASS for inspected surfaces | Material edges and native-size icons remain crisp at nearest enlargement; quiet paper does not fight text. No palette-size or detail-count score substitutes for this observation. |
| Composition/controls/legibility | PASS for sampled text fit; FAIL for gallery01/11 layout fidelity | All37 samples were opened; no clipped essential sample action or overlapping sample line was established. The two content-size violations remain open. Actual long names, hover/focus states and GUI-scale transitions are UNOBSERVED. |
| Motion/spatial flow | N/A | Static page/control deliverable. Screenshots do not establish transitions or interaction timing. |
| Glow/effects | N/A | No glow is used as a readability substitute. |
| Export/actual use | PASS for reusable surface separation; UNOBSERVED for live use | Native material/icon resources are separate from sample-data page previews. Current runtime composition, clipping, native entities and interactive focus need actual consumer evidence. |

## Findings and exact correction targets

**P2 / `pages/01-directory.png` and its native composition / unused section sidebar in a directory / FAIL.** The opened directory carries four section rows for a settlement that has not been selected and substantial empty lower content. This repeats the specific01 rejection's unused-sidebar problem, although the candidate is smaller than that old screenshot. Corrective action: refresh only this gallery composition from the current content-sized selector layout, retaining search, actual directory rows and the founding action without town-section navigation. Closure: open the fresh populated native-size preview and its integer-nearest view; confirm bounds follow the supplied row count and no nonfunctional town navigation remains.

**P2 / `pages/11-chest-content.png` and its native composition / full dashboard shell around a small slot grid / FAIL.** A162×108 grid remains centered in a512×320 full dashboard sheet with the town sidebar. The prior06 rejection explicitly requested bounds matching the storage content. Corrective action: redraw this gallery composition using the current compact storage layout, the native18-pixel slot pitch, coordinates/page information and feed action; remove its unused dashboard sidebar. Closure: open the updated populated compact preview with actual slot count and action/footer bounds.

These are **gallery evidence defects**, not demonstrated live bugs. Current source already derives directory/storage dimensions from content, and the parent's later storage `panelH<310` compact rule prevents the former short-sidebar overlap. Source reading supports that distinction but does not replace a composed consumer capture. The old gallery must not be shown as proof of the current screen's exact geometry.

**UNOBSERVED / source art integration / actual composed consumer states.** The37 screenshots establish page-art intent and sample composition, not current dynamic text positions, hit/focus bounds, clipping or payroll interaction. Closure can use authorized editor/export evidence of the consumer composition where available; otherwise record the limit for a later separately authorized user game review. This audit does not require or authorize launching Minecraft.

## Delivery boundary

Reusable material/icon exports may remain source-integrated as candidates with the above limits. The complete gallery cannot be described as passing all C0 layout gates while01/11 remain stale. Refreshing those previews is a bounded C1 correction if authorized; reopen the same originals and actual updated compositions afterward. No actual Screen acceptance, successful gameplay, runtime pass or user approval is implied by this document.

## C1: two consumer-coordinate editor compositions

The parent task authorized one correction round limited to new directory/chest editor previews. C0 sources, all37 historical page PNGs and client resources remain intact. The actual native Aseprite operation completed and its four resulting images were opened: `integration-preview/01-directory-c1.png`, `11-chest-content-c1.png` and each `-2x.png`. Editable three-layer `.aseprite` sources were retained. Existing materials,24-pixel frames, native glyph sources and sample item bitmaps were composed without repainting production pixels.

Before this formal correction/review the same original GUI+05/+06 positives and the actual negative runtime01/06 screenshots were reopened. The current `SettlementScreen`/`SettlementDashboardArt`/`SettlementPageArt` source coordinates were then used; no new game layout was invented to improve the preview.

- **Directory finding closed for C1.** The panel is460×277 for three rows: `148+3×43`. It has no town-section sidebar; search, three native-size cards, count and founding action use the current selector coordinates. The old full-dashboard C0 remains historical evidence rather than a claim about this source.
- **Chest finding closed for C1.** A54-slot container produces a364×276 panel: `168+6×18`, compact because276<310. With `cy=80` and `bottom=254`, the actual page-capacity formula yields36 visible slots, not54. The preview shows the9×4 grid, a disabled previous-page control, enabled next-page control, `Ячейки1–36из54`, and the native feed action without an unused sidebar. The sample chest is known and empty; no missing ItemRenderer output is hidden by fabricated goods.
- The20-pixel transparent review margin is outside each quoted panel; it retains the existing header's protruding vines. It is not additional GUI space or a larger live window.

| C1 axis | Verdict | Reinspection |
| --- | --- | --- |
| Reference/brief fidelity | PASS | Both new compositions remove the exact unused-sidebar/full-dashboard defect from the prior rejection. |
| Tool/pixel provenance | PASS | Aseprite directly composed/exported the existing native materials and bitmap glyphs; no production resource was repainted or replaced. |
| Silhouette/hierarchy | PASS | Header, directory rows and actions remain separate; chest controls frame the slot grid without town navigation. |
| Geometry/perspective/contacts | PASS for flat editor geometry | Frame corners, repeated texels and slot pitch follow the actual source. No3D claim is added. |
| Values/color/light | PASS | Existing pale content, dark text, wood controls and framing remain distinct at1× and2×. |
| Pixel clusters/materials | PASS | Existing material pixels remain native;2× evidence is nearest-neighbour review output. |
| Composition/controls/legibility | PASS for the two sampled editor states | Search, cards, coordinates, page indicator and footer actions remain readable without the rejected sidebar. Live cursor/focus/hit-testing, arbitrary names and viewport variation remain UNOBSERVED. |
| Motion/spatial flow | N/A | Static editor compositions. |
| Glow/effects | N/A | No new glow/effects. |
| Export/actual use | PASS for composed editor evidence; UNOBSERVED for runtime | Native Aseprite source and direct PNG outputs exist. Header text's1.4× scale and item bitmaps approximate the actual GPU font/ItemRenderer; this is expressly labelled editor evidence, not a captured game render. |

C1 closes the two composition findings for the new integration evidence. It does not rewrite the historical37-page gallery as a fresh runtime capture, certify every live state or replace the separate user-acceptance decision. No further automatic correction was necessary for the observed C1 states.

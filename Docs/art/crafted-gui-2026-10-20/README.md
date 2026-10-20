# Rejected crafted Settlements and SMP art — retained archive

The user rejected the implemented visual overhaul from `a0f1a26c` on 2026-10-20 and explicitly requested its removal from Settlements and SMP. The concept direction's earlier approval does not override that rejection. These generated originals, editable sources, manifests and export scripts remain an inactive provenance archive. No archived AI artwork is authorized for active Settlements or SMP production, and rerunning an export is not permission to restore it.

The rollback restores the preceding interface presentation while preserving live data, actions, gameplay, Professions removal and the later Digger/Skarrier/aircraft changes. The existing Market storefront is explicitly excluded and remains unchanged. Example text, counts and actions in reference images never authorized gameplay changes.

The sections below document the historical delivery and its export procedure; terms such as runtime, shipped, production or active pair describe that rejected attempt, not the current approved runtime. Native gameplay verification and the parent task's distribution/application evidence remain separate.

## Source boundary

`generated/` retains production artwork returned by ImageGen. These are purpose-built production atlases or standalone illustrations, not crops from the user's concept screenshots. `sources/<job>/` holds one native `.aseprite` document per shipped asset. Its **Generated production artwork** layer is the imported artwork at export resolution. Its locked, hidden **Protected geometry guide (not exported)** layer marks bounds and, for materials, fixed corner/edge regions. This is an editable imported-art source, not a claim that generated pixel clusters were hand-painted or mathematically guaranteed by a prompt.

The executable export contract is `Export-Assets.ps1` plus `export-assets.lua`. Aseprite performs every crop/resize and PNG export; resizing uses nearest-neighbour. No raster manipulation is performed through Python/PIL. Per-job JSON records original hashes, dimensions, names and destinations; CSV records exact source rectangles; export JSON records resulting PNG hashes. Generated originals remain available to revise the crop or regenerate an asset without relying on an installed JAR.

The received material image is 1254 × 1254 and has unequal row heights plus an unused footer. Its manifest therefore records explicit crop rectangles rather than assuming an exact 3 × 3 division. Aseprite remaps each source's 80-pixel corner region to an exact 8-pixel output corner, separately sampling its center and edges. This enforces the renderer's geometry; it does not claim the generated source obeyed the requested grid. Icon atlases are 1254 × 1254 RGBA; cell boundaries use floor-divided 4 × 4 coordinates so each output is separate.

After icon export, `cleanup-icons.lua` removes only disconnected alpha components of four pixels or fewer. It retains the imported layer hidden and adds a separate visible cleanup layer; `manifests/icon-cleanup.csv` records the exact removed pixel counts. This corrects sampled isolated flecks without inventing geometry. Some attached cutout fringes and the generated palette remain; no claim of exhaustive hand-painted cleanup is made. `preview-assets.lua` creates contact sheets from the final exported files at 4× nearest-neighbour scale. Rerun cleanup once after a fresh export; it is not a source painter or an automatic quality-acceptance gate.

`refine-icon-edges.lua` then removes bright red matte residue from non-red-semantic icons only where it touches transparency (R ≥ 220, G/B ≤ 65). Red-semantic blood moon, sad mood, roles seal, calendar/events and warning artwork is protected. The original hidden layer retains every removed source pixel. Exact boundary-removal counts are in `manifests/icon-edge-refinement.csv`. This is narrow silhouette cleanup, not a filter applied to every image.

The export order is: each explicit job → `cleanup-icons.lua` → `refine-icon-edges.lua` → `preview-assets.lua`. Each Lua script is run with `aseprite --batch --script-param root=<this absolute directory> --script <script path>`; initial job exports instead use `--script-param job=<explicit manifests/job.lua> --script export-assets.lua`. The successful production invocation used `C:/Users/user/.codex/tools/aseprite/build-1.3.18.6/bin/aseprite.exe`.

Treasury uses the same two cleanup steps with the extra `--script-param only=treasury`; its separate reports preserve the icon reports. It remains a standalone 160 × 128 transparent illustration. Its generated original, hidden imported layer, visible cleaned layer and protected hidden geometry guide are retained.

## Delivered art inventory

The final export contains 60 runtime PNGs and 48 editable native documents: nine shared materials exported to both namespaces, 32 individual icons, six event illustrations, one treasury illustration and three unchanged native ornaments. `manifests/export-dimensions.json` records every runtime PNG's exact size, transparent-pixel count and SHA-256. Asset export and native-source creation succeeded. Contact sheets and all six exported covers were visually inspected; actual Minecraft scale/layout and player acceptance remain separate integration review.

`manifests/owned-files.txt` is the exact project-relative file list produced by this asset work for explicit staging. It excludes the pre-existing unrelated `professions-standalone-proposal.md`; the asset worker did not edit that proposal. No Java, build output, runtime server data or clean distribution was modified by the asset worker.

## Renderer contract

| Family | Export size | Assembly |
| --- | --- | --- |
| Materials | 64 × 64 | Fixed 8-pixel corners; 48-pixel edge and center regions repeated by the renderer |
| Icons | 32 × 32 | Individual transparent assets; dynamic labels drawn separately |
| Event illustrations | 384 × 192 | Aspect-preserving center crop; live title/status/buttons remain separate |
| Treasury illustration | 160 × 128 | Standalone image with live balance outside the artwork |

Material names, in atlas row order: `stone_frame`, `paper_frame`, `dark_frame`, `wood_frame`, `card`, `control`, `control_hover`, `control_selected`, `control_disabled`. The same PNG bytes are exported into both JEM namespaces.

SMP icons: `profile`, `players`, `parties`, `events`, `market`, `settlements`, `rewards`, `roles`, `professions`, `back`, `calendar`, `boss`, `blood_moon`, `resource_rush`, `search`, `map`.

Settlements icons: `settlement`, `residents`, `happiness`, `chests`, `search`, `home`, `bread`, `lantern`, `coins`, `development`, `back`, `mood_happy`, `mood_neutral`, `mood_sad`, `check`, `warning`.

## Existing material reuse

SMP `crafted/ornaments/banner.png`, `lantern.png` and `vine.png` are unchanged copies from the canonical JEMSettlements `src/main/resources/assets/jem_settlements/textures/gui/dashboard/` assets. They are native project artwork reuse, not freshly generated icons. Their dimensions are 16 × 32, 16 × 28 and 40 × 32 respectively.

The active medium-specific pair is `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/Pairs/Minecraft/GUI/Forge-1.20.1-jem-smp-screens/`. The current explicit user brief permits the hybrid generated-art/exact-assembly process. Source editability, successful export, source integration, compiled artifacts and user acceptance are separate states. No production export alone establishes visual approval in Minecraft.

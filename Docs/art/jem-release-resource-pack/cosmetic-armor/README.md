# JEM Release Cosmetic Armor inventory

Status: `CANDIDATE_IN_TESTING`

The user reported that Cosmetic Armor's bright native inventory visually replaced the active JEM Release player interface and requested that the screen reuse the existing JEM Release assets in the same style

## Ownership

- Cosmetic Armor Reworked 1.20.1-v1a keeps all slot positions, menu behavior, player rendering, recipe-book behavior, toggle controls and synchronization
- `tools/build_jem_release_resource_pack.py` owns the deterministic resource-pack composite
- `JEM Release v0.3.zip` owns the shipped client override at `assets/cosmeticarmorreworked/textures/gui/cosarmorinventory.png`
- The existing JEM Release inventory atlas supplies the crafting area, inventory bank and dark recessed equipment-slot material
- Pixels outside Cosmetic Armor's rendered `176x166` screen region remain byte-identical to the installed mod atlas so its toggle-state sprites remain native

## Exact asset contract

- Canvas `256x256` RGBA with zero-based coordinates
- Rendered screen region `(0,0)` through `(175,165)`
- JEM inventory screen region copied in place without scaling or resampling
- JEM dark slot source rectangle `(7,83)` through `(24,100)`, size `18x18`
- Native equipment origins preserved at `(7,7)`, `(7,25)`, `(7,43)`, `(7,61)`, `(77,61)`, `(97,61)`, `(115,61)`, `(133,61)` and `(151,61)`
- Live player entity, item stacks, slot icons, labels, recipe book and toggle states remain runtime-drawn

The machine-readable geometry is in [geometry.json](geometry.json)

## Sources and evidence

- Installed mod artifact SHA-256 `c790e8671625b05ec0aa19272e0ea74ea95619f953e3c6f39a6736af0d386804`
- Final JEM Release archive SHA-256 `8992a57df1dbe4d3a7e09e72f96688e50df8004431b2f6d01e37530c14bda727`
- Pack entry SHA-256 `486b41ef994316273cc197fcac977bc609476e423cd6b5e56d003cd86bde47a8`
- Editable Aseprite source [cosarmorinventory.aseprite](production/cosarmorinventory.aseprite)
- Direct native PNG export [cosarmorinventory.png](production/cosarmorinventory.png)
- Native-size checker preview [native-checker-1x.png](previews/native-checker-1x.png)
- Nearest-neighbour checker preview [checker-4x.png](previews/checker-4x.png)
- Light-background preview [light-4x.png](previews/light-4x.png)
- Dark-background preview [dark-4x.png](previews/dark-4x.png)

The required resource-pack originals inspected for this adaptation were `Exact Wanted GUI Approach and Style 3.png`, `Exact Wanted GUI Approach and Style 4.png` and `Buttons and Layout 1.png`. Their applicable qualities are a coherent physical inventory, dark recessed slots, strong value separation and preserved functional grouping. This adaptation copies no reference pixels and introduces no generated artwork

## Review boundary

[visual-audit-c0.json](visual-audit-c0.json) records a static C0 pass with no automatic correction round. Runtime appearance and user acceptance remain unobserved because the client was not launched

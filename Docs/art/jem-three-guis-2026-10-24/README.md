# JEM Silver & Honey-Oak Vanilla GUI Candidates (2026-10-24)

This workspace contains the native Aseprite sources, candidate PNG exports, runtime-simulated and `1x` contour previews, UV/bytecode audit receipt (`geometry.json`), and `pack_format` 15 candidate resource pack ZIP for the three requested Minecraft 1.20.1 (`Forge 47.4.10`) container GUIs:

- Shared lower inventory and hotbar master: [`shared_inventory_master.aseprite`](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/jem-three-guis-2026-10-24/sources/shared_inventory_master.aseprite) and [`shared_inventory_master.png`](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/jem-three-guis-2026-10-24/candidates/shared_inventory_master.png) (`176x96` RGBA, containing the `176x86` lower silver `9x3` inventory + raised `9x1` hotbar module at `y=80..165` and the `70x10` warm honey-oak `"Inventory"` plaque at `x=4..73, y=70..79`)
- Player Inventory (`E`): [`inventory.aseprite`](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/jem-three-guis-2026-10-24/sources/inventory.aseprite) and [`inventory.png`](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/jem-three-guis-2026-10-24/candidates/inventory.png) (`256x256` RGBA, target `assets/minecraft/textures/gui/container/inventory.png`)
- Anvil Repair & Name: [`anvil.aseprite`](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/jem-three-guis-2026-10-24/sources/anvil.aseprite) and [`anvil.png`](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/jem-three-guis-2026-10-24/candidates/anvil.png) (`256x256` RGBA, target `assets/minecraft/textures/gui/container/anvil.png`)
- Librarian Trading Candidate: [`villager2.aseprite`](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/jem-three-guis-2026-10-24/sources/villager2.aseprite) and [`villager2.png`](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/jem-three-guis-2026-10-24/candidates/villager2.png) (`512x256` RGBA, held outside the active candidate ZIP until a profession-scoped GUI selector is added)

## Shared Master Audit & Pixel-Equality Proof

1. **Existing JEM Repository Search**:
   - Checked `Client/ManagedPack/resourcepacks` (`JEMLightDarkGUI` is an older procedural birch/slate pack) and `Catalog/GUI/vanilla` (vanilla `1.20.1` reference extraction)
   - Excluded `Docs/art/sophisticated-storage-silver-gui-2026-10-24` as required because it is an unapproved asset for a different mod (`sophisticatedcore`)
   - Authored one canonical native-size reusable master in [`shared_inventory_master.aseprite`](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/jem-three-guis-2026-10-24/sources/shared_inventory_master.aseprite)
2. **Exact Pixel Equality Across All Three Screens**:
   - Lower silver `9x3` inventory and raised forward `9x1` hotbar (`176x86`): pasted without modification into `inventory.png[0:176, 80:166]`, `anvil.png[0:176, 80:166]`, and `villager2.png[100:276, 80:166]` (`0` differing pixels across all three screens)
   - Shared warm honey-oak `"Inventory"` plaque (`70x10`): pasted without modification into `anvil.png[4:74, 70:80]` (behind `inventoryLabelX=8, inventoryLabelY=72`) and `villager2.png[103:173, 70:80]` (behind `inventoryLabelX=107, inventoryLabelY=72`) (`0` differing pixels)
   - Note on `inventory.png` at `y=70..79`: in vanilla `InventoryScreen.renderLabels`, Minecraft only draws `"Crafting"` at `(97, 6)` and does not draw `"Inventory"` at `(8, 72)` because the fourth armor slot (`7..24, 61..78`), dark player preview stage (`25..75, 7..78`), and offhand slot (`76..93, 61..78`) occupy `x=7..93, y=61..78`
   - JEM mod compatibility: `JEMRewards` ([`RewardsPanel.java`](file:///C:/MyProjects/Minecraft/JustEnoughMods/Src/JEMRewards/src/main/java/com/jem/rewards/client/screen/RewardsPanel.java)) blits `inventory.png` UV `(7, 83, 18, 18)` 1:1 for take-only reward slots, which maps directly to the top-left `18x18` recessed silver slot of the shared master

## Critical Runtime & Integration Findings (`villager2.png`)

1. **Profession-Specific Texture Selection Blocker**:
   - Disassembled Forge `1.20.1-47.4.10` `MerchantScreen` bytecode confirms `VILLAGER_LOCATION = new ResourceLocation("textures/gui/container/villager2.png")` is hardcoded globally for all villager professions and wandering traders
   - No OptiGUI, OptiFine, or custom profession GUI selector mod is installed in `Client/ManagedPack/mods`
   - Per the brief rules, [`villager2.png`](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/jem-three-guis-2026-10-24/candidates/villager2.png) is completed and verified in `candidates/` and `previews/`, and kept outside [`jem-silver-oak-gui-candidates-1.20.1.zip`](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/jem-three-guis-2026-10-24/pack/jem-silver-oak-gui-candidates-1.20.1.zip) so non-librarian villagers do not receive a librarian bookshop GUI
2. **Trade-Offer Button Opaque Fill Owner (`AbstractWidget.WIDGETS_LOCATION`)**:
   - Disassembled `MerchantScreen$TradeOfferButton` bytecode shows it extends `Button` -> `AbstractButton` and does not sample the unused `88x27` legacy sprite at `(279, 73)` in `villager2.png`
   - Instead, `AbstractButton.renderWidget` calls `GuiGraphics.blitNineSliced(AbstractWidget.WIDGETS_LOCATION, getX(), getY(), 88, 20, 20, 4, 200, 20, 0, 46 + i * 20)` from `textures/gui/widgets.png` before `MerchantScreen.render` draws the cost items, discount strikethrough, trade arrow, and result item on top
   - In [`villager2.png`](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/jem-three-guis-2026-10-24/candidates/villager2.png), we made the legacy `86x25` button interior at `(280..365, 74..98)` transparent (`2,150` pixels recorded in [`geometry.json`](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/jem-three-guis-2026-10-24/geometry.json)), and we report here that suppressing the runtime button backplate requires a small `MerchantScreen` / `TradeOfferButton` mixin in JEM (`JEMEconomy` already mixins into `MerchantScreen` via [`MerchantScreenMixin.java`](file:///C:/MyProjects/Minecraft/JustEnoughMods/Src/JEMEconomy/src/main/java/com/jem/economy/mixin/MerchantScreenMixin.java))
3. **JEM Economy Coin Price Overlay Compatibility**:
   - [`MerchantScreenMixin.java`](file:///C:/MyProjects/Minecraft/JustEnoughMods/Src/JEMEconomy/src/main/java/com/jem/economy/mixin/MerchantScreenMixin.java) renders `"Buy"` / `"Sell"` and coin prices at `leftPos + 108..268, topPos + 59..75` in `0xFF404040`
   - The counter apron at `x=105..269, y=58..68` in [`villager2.png`](file:///C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/jem-three-guis-2026-10-24/candidates/villager2.png) is authored as a calm sunlit honey-oak plank (`4.89:1` contrast against `0x404040`) so JEM coin prices and icons read clearly

## Visual-Auditor Passes (`C0` -> `C1` -> `C2`)

- **`C0` -> `C1` Fixes**:
  - Widened the shared `"Inventory"` honey-oak plaque from `x=6..67` (`62x10`) to `x=4..73` (`70x10` on `anvil.png`, `x=103..172` on `villager2.png`), brightened the oak face to `#DEA865` / `#E6B370`, and moved the corner pins to the `1px` inner bevel corners so minimum text contrast rose from `1.02:1` to `4.29:1`–`4.89:1`
  - Removed 5 isolated single-pixel white dots on the dark top-left slot frame corners of the shared inventory master and replaced them with coherent diagonal reflection slash clusters on the outer silver rails and raised hotbar apron
  - Grounded the Librarian open book and sloped oak lectern (`x=108..130, y=44..56`) directly onto the counter ledge instead of floating above the trade arrow
  - Rebuilt the `anvil.png` auxiliary error cross patch at `(176, 0, 28, 21)` over our dark-iron combine arrow patch `(99, 45, 28, 21)` so the `alpha=255` blit overlays seamlessly without a red background rectangle
- **`C1` -> `C2` Fixes**:
  - Replaced the boxy center arch block at `x=182..212, y=23..30` in `villager2.png` with a continuous upper bookshop gallery divided by a carved vertical oak post at `x=195..198, y=22..30`
  - Cleaned the top profile of the forged anvil silhouette at `x=10..50, y=17..41` in `anvil.png` by removing the small top bump so the specular anvil table, horn, hardy hole, waist, and stepped base read immediately at `1x`

## Verification Summary (`UNOBSERVED` Runtime Status)

- Dimensions & mode: `inventory.png` (`256x256` RGBA), `anvil.png` (`256x256` RGBA), `villager2.png` (`512x256` RGBA), `shared_inventory_master.png` (`176x96` RGBA)
- Alpha deltas: `0` pixels changed on `inventory.png` (all auxiliary regions outside `0..175, 0..165` are 100% byte-identical to `inventory-original.png`), `0` pixels changed on `anvil.png`, and `0` unexplained alpha deltas on `villager2.png` (`2,150` intentional transparent pixels in the legacy button interior `280..365, 74..98` + `6` intentional rounded bottom-left corner pixels at `100..102, 163..165` from the shared silver inventory master)
- Runtime status: `UNOBSERVED` (verified via static bytecode disassembly, UV geometry audit, and automated pixel checks without launching Minecraft or touching active pack profiles)

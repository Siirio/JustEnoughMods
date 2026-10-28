# Quick Skin main menu art

This folder preserves the user-supplied visual sources and the deterministic Aseprite extraction used for the 2026-10-28 FancyMenu main menu

## Sources

- `references/title-source.jpg` is the supplied white "Minecraft JEM Edition" lettering on blue
- `references/background-source.jpg` is the supplied Minecraft landscape
- `extract_title.ps1` removes the cyan title field by converting only neutral near-white title pixels into alpha-matted white lettering
- `extract_title.lua` preserves the same extraction as an Aseprite-native source script

## Production outputs

- `production/main_menu_background.png` is the lossless FancyMenu background
- `production/main_menu_title.png` is the transparent title texture
- `production/main_menu_title.aseprite` is the editable native title source
- `prepare_fancymenu_layout.ps1` deterministically applies the approved right-side text-only layout to the current tracked FancyMenu title-screen file
The captured FancyMenu widget identifiers correspond to Quick Skin's text action, player preview, rotation control and animation controls. FancyMenu leaves only the Change Skin text action visible directly below the player and suppresses every icon-sized control plus the JEM resource-pack button backgrounds. Quick Skin owns native model rendering and personal appearance state. For JEM Release and JEM Legacy, JEMRoles applies the production 25 percent size, X offset -432 and Y offset 104 in memory so a preserved mutable root config cannot restore stale coordinates. Legacy uses an independent reference point. JEMRoles also initializes the optional title-screen widget at -20 degrees yaw so the default pose faces into the composition. Personal skin hashes and appearance choices remain user-owned

The user-designated source of truth for `main_menu_background.png` alone is the copy in the closed `jem sandbox` profile. Before a future MineRENT release, that PNG must be synchronized from sandbox into ManagedPack and the release projection if its hash differs. This ownership does not extend to the title, FancyMenu layouts, button placement, Quick Skin configuration or either resource-pack profile

## Resource-pack profile routing

- The approved pastoral Quick Skin composition is the `JEM Release` title layout and loads only while `file/JEM Release v0.3.zip` is enabled
- The preserved original `Legacy JEM` layout loads only while `file/JEM Legacy v1.0.zip` is enabled
- The local `JEM Legacy v1.0.zip` combines CoffeeGUI first and ModdedCoffeeGUI second, so the modded extension fills additional GUI consumers inside one selectable pack
- When neither JEM pack is enabled, neither custom title layout loads and Minecraft keeps its default menu and GUI
- The private composite ZIP remains outside ManagedPack and Git because its vendor sources provide no redistribution license. FancyMenu configuration keeps the exact pack-name selectors without copying those resource-pack bytes

No generative or Gemini step is used in the extraction

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
The captured FancyMenu widget identifiers correspond to Quick Skin's text action, player preview, rotation control and animation controls. FancyMenu leaves only the Change Skin text action visible directly below the player and suppresses every icon-sized control plus the JEM resource-pack button backgrounds. Quick Skin's native client configuration owns the actual rendered model position and size. The production values are 25 percent size, X offset -422 and Y offset 119 for the user-marked lower-left area. JEMRoles initializes the optional title-screen widget at -20 degrees yaw so the default pose faces into the composition. Personal skin hashes and appearance choices remain user-owned

No generative or Gemini step is used in the extraction

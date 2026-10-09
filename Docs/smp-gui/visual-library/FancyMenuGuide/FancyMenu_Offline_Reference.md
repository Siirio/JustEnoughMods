# FancyMenu Offline Reference Pack

This pack indexes all **83 pages** currently listed in the FancyMenu English user documentation (`/docs/en-US/`) as checked on 2026-08-30.

## Important copyright note

This is **not a verbatim mirror** of the FancyMenu documentation. The live site is copyrighted. The files below are an original, compact reference that preserves the documentation structure, important procedures, settings, commands, paths, identifiers, dependencies and technical behavior in paraphrased form.

Each page file links back to the corresponding live documentation page so you can verify exact wording/screenshots and any later updates.


## Complete combined reference

## 1. Accessing Vanilla Sound Files

Source: https://docs.fancymenu.net/docs/en-US/vanilla-sounds

- Vanilla sounds can be used anywhere FancyMenu accepts OGG audio by referencing their Minecraft resource location.
- Resource locations follow `namespace:path`; a vanilla file under `assets/minecraft/sounds/...` uses the `minecraft:` namespace.
- Example: the bromeliad track can be referenced as `minecraft:sounds/music/game/bromeliad.ogg`.
- Asset browsers such as mcasset.cloud are useful for locating the internal vanilla paths.

## 2. Action Scripts

Source: https://docs.fancymenu.net/docs/en-US/action-scripts

- Action scripts run ordered actions from buttons, sliders, tickers, screen events, listeners and schedulers.
- They support If, Else-If, Else and While statements. While loops have a safety timeout; use Delay/Execute Later rather than a busy loop for timing.
- Action values can contain FancyMenu placeholders and event-provided `$$` values such as a slider's `$$value`.
- Important actions include joining/leaving worlds or servers, opening screens, sending chat/commands, variables, HTTP requests, resource-pack management, animator control, keybind mimicry, file operations, toast notifications and remote-server messaging.
- Imported scripts are powerful enough to modify files or contact servers, so use only trusted layouts.

## 3. Advanced Positioning & Sizing

Source: https://docs.fancymenu.net/docs/en-US/advanced-positioning-sizing

- Advanced Positioning/Sizing allows direct text-driven coordinates and dimensions and supports placeholders/calculations.
- Advanced Positioning uses the top-left screen corner as origin and ignores the normal element anchor for coordinate calculation.
- Use GUI width/height, GUI scale and element dimension placeholders with the Calculator placeholder for responsive formulas.
- For ordinary responsive menus, first try forced GUI scale plus layout Auto-Scaling before relying on advanced formulas.

## 4. Animations (FMA/AFMA)

Source: https://docs.fancymenu.net/docs/en-US/fma

- FancyMenu has two native animated texture formats: classic FMA and newer AFMA.
- AFMA is preferred for larger/complex animations because it is more efficient and uses a dedicated built-in creator.
- Classic FMA is effectively a ZIP-based frame animation with a `.fma` extension and remains supported for existing layouts.
- Extracted video frames can be turned into AFMA/FMA animations; fewer/smaller frames reduce memory usage.

## 5. APNGs

Source: https://docs.fancymenu.net/docs/en-US/apng

- FancyMenu supports animated PNGs, but compatibility depends on APNG encoding details.
- Use non-interlaced APNGs and avoid problematic compression settings; `.apng` must be the filename extension.
- For large or complex animations, AFMA is the preferred FancyMenu-native format.
- Store local APNG files in `config/fancymenu/assets/` and select them wherever animated images are accepted.

## 6. Browser JavaScript API

Source: https://docs.fancymenu.net/docs/en-US/browser-api

- Rinku-backed browser content receives `window.fancymenu` and the alias `window.FancyMenu`.
- `fancymenu.actions` can execute FancyMenu actions from page JavaScript.
- `fancymenu.placeholders` can asynchronously read FancyMenu placeholders and returns promises.
- Use the `fancymenu-ready` event or feature detection before using the bridge. Placeholder variables are passed as `name:value` strings.

## 7. Button & Slider Templates

Source: https://docs.fancymenu.net/docs/en-US/button-slider-templates

- A custom Button element can act as a template for many buttons or sliders in a layout.
- Templates can share dimensions, position, visibility, opacity, labels and custom textures; texture sharing is automatic when textures are configured.
- Button and slider template scopes are separate, allowing one active template for each kind.
- Template buttons are editor-only and do not appear in the final menu. For broad global vanilla styling, prefer Global Customizations.

## 8. Client < - > Server Data Sharing

Source: https://docs.fancymenu.net/docs/en-US/client-server-data-sharing

- FancyMenu can exchange identified data between a configured client and a server when FancyMenu is present on both sides.
- Client actions can send FM data to the current server, while listeners can react to incoming data.
- Treat network-provided values as untrusted and validate them before putting them into commands, paths, URLs or other sensitive actions.
- Server-side features also enable remote GUI/variable commands and server-backed placeholders/listeners.

## 9. Commands

Source: https://docs.fancymenu.net/docs/en-US/commands

- FancyMenu exposes commands for opening/closing screens, toggling layouts and reading/writing variables.
- `/openguiscreen <screen_identifier> [target]` opens supported vanilla/mod/custom screens; remote targets require server-side FancyMenu and permissions.
- `/closeguiscreen [target]` closes the current screen.
- `/fmlayout <layout_name> <true|false> [target]` controls layout enablement.
- `/fmvariable get <name>` and `/fmvariable set <name> <send_feedback> <value>` read and write FancyMenu variables.

## 10. Conditions (Requirements)

Source: https://docs.fancymenu.net/docs/en-US/conditions

- Requirements dynamically decide whether an element, layout or script branch is active.
- Add them to an element through its Loading Requirements menu; they are reevaluated while a screen is open.
- Layout-wide requirements can enable/disable entire layouts and cause the current screen to be rebuilt when the result changes.
- Action Scripts use requirements inside conditional statements.

## 11. Custom Cursor

Source: https://docs.fancymenu.net/docs/en-US/custom-cursor

- Add a Cursor element to replace the system cursor while its layout is active.
- Use an RGBA PNG and set Hotspot X/Y to the pixel that should act as the click point.
- Small images such as 32×32 or 64×64 are sensible choices.
- A Universal Layout can apply a cursor across multiple supported screens.

## 12. Custom GUIs

Source: https://docs.fancymenu.net/docs/en-US/custom-guis

- Custom GUIs are completely new screens that can be filled with FancyMenu elements.
- Create them from `Customization -> Custom GUIs -> Manage Custom GUIs`, then assign a unique identifier.
- Identifiers use lowercase letters, digits, `.`, `_` and `-` and cannot contain spaces.
- Custom GUIs always have screen customization enabled. They can be opened from actions/commands just like other supported screens.

## 13. Data Storage Locations

Source: https://docs.fancymenu.net/docs/en-US/data-storage-locations

- FancyMenu stores user-created assets and configuration under the active Minecraft instance, usually below `config/fancymenu/`.
- Local assets belong in `config/fancymenu/assets/`; panoramas, slideshows, localization data, layouts and other generated/config data have their own subdirectories.
- Do not assume the active instance is the global `.minecraft` directory; launchers commonly use per-profile folders.
- Variable values are persisted in FancyMenu's variable database and some element-specific persistent state may live in separate files.

## 14. Decoration Overlays

Source: https://docs.fancymenu.net/docs/en-US/decoration-overlays

- Decoration overlays add full-screen decorative rendering layers independent of ordinary element placement.
- FancyMenu supports overlay-style content including GLSL-based rendering in applicable versions.
- Use overlays for non-interactive visual treatment rather than controls; layer them with backgrounds/elements as needed.
- For shader overlays, the same FancyMenu GLSL runtime and uniforms used by GLSL backgrounds/elements apply.

## 15. Deleted Vanilla Elements

Source: https://docs.fancymenu.net/docs/en-US/deleted-vanilla-elements

- Deleting a visible vanilla/mod element in the editor hides it instead of permanently destroying it.
- Restore hidden elements from `Element -> Deleted Vanilla Elements` while the layout that hid them is open.
- If the element is not listed, you likely opened a different layout.

## 16. Dragger

Source: https://docs.fancymenu.net/docs/en-US/dragger

- A Dragger is an invisible element that users can move with the mouse outside the editor.
- Anchor other elements to it to create movable HUD/menu panels.
- `Save User Drag Offset` persists the user-selected offset across openings and restarts.
- Use an anchored visible element such as an Image or Shape as the visible body.

## 17. Element Animator

Source: https://docs.fancymenu.net/docs/en-US/element-animator

- The Animator element creates keyframe-based motion for other elements.
- It can animate position, size and anchor point; it does not directly animate every property such as opacity or rotation.
- One animator can target multiple elements and can use timing offsets.
- Actions can enable, disable, toggle or reset an animator by its identifier.

## 18. Element Identifiers

Source: https://docs.fancymenu.net/docs/en-US/element-identifiers

- Every FancyMenu element receives a unique identifier used by actions, placeholders, requirements and other systems.
- Right-click an element and use Copy Element Identifier to copy it exactly.
- Identifiers are the safest way to reference a specific element from another feature.

## 19. Element Opacity

Source: https://docs.fancymenu.net/docs/en-US/element-opacity

- Many elements support an opacity value from their context menu.
- Opacity inputs can use placeholders, enabling dynamic transparency tied to variables or calculations.
- Built-in fade-in/fade-out is simpler when you only need transitions as an element loads/unloads.
- Fade speed controls how quickly the transition occurs.

## 20. Elements

Source: https://docs.fancymenu.net/docs/en-US/elements

- Elements are the building blocks of layouts. Add them from the editor background's `New Element` menu.
- Core interactive elements include Button, Slider, Checkbox and Text Input Field; display elements include Image, Text, Item/Model, Player Entity, Progress Bar, Video and Browser.
- Behavior/utility elements include Audio, Music Controller, Ticker, Dragger, Cursor and Element Animator.
- Many elements support actions, requirements, placeholders, custom textures, sounds, rotation/tilt, opacity and anchors depending on type.
- Browser requires Rinku; native Video requires Watermedia V3 + Watermedia Binaries V3.

## 21. Essential Mod

Source: https://docs.fancymenu.net/docs/en-US/essential

- Essential provides its own FancyMenu integration for widgets it adds to the Title and Pause screens.
- Use Essential's supported integration path for its UI rather than assuming every widget behaves like a normal vanilla widget.
- If an Essential-owned widget is missing or behaves differently, reproduce with current versions and report to the project that owns the widget.

## 22. FancyMenu's UI Scale

Source: https://docs.fancymenu.net/docs/en-US/fancymenu-ui-scale

- FancyMenu's menu bar/context-menu UI has its own scale separate from Minecraft's normal GUI scale.
- Change it under `Customization -> Settings -> FancyMenu's UI`.
- Auto mode currently has a minimum automatic scale of 1.25, while scale 1 can still be selected manually.
- Full FancyMenu screens such as managers/editors generally follow Minecraft GUI scaling or their own fit-to-window logic.

## 23. FAQ

Source: https://docs.fancymenu.net/docs/en-US/faq

- For support, provide a clear problem description, `logs/latest.log`, Minecraft version, loader/version, FancyMenu version and useful screenshots/video.
- Use Layers for custom element ordering; some 3D/entity/item rendering cannot obey normal GUI layering in older versions.
- Current-screen layouts are required to edit vanilla widgets; universal layouts do not expose them.
- Scrollable/modded screens may have special limitations. The docs also cover templates, disabled buttons, Create screens, player entity quirks and common editor questions.

## 24. Fix Audio Files

Source: https://docs.fancymenu.net/docs/en-US/fix-audio

- If an audio file plays elsewhere but not in FancyMenu/Minecraft, re-encode it.
- OGG often works after conversion/re-encoding. PCM WAV around 48 kHz / 16-bit is a good compatibility target.
- Also verify Minecraft master/channel volume, selected sound category, file size and possible audio-mod conflicts.

## 25. Game Intro

Source: https://docs.fancymenu.net/docs/en-US/game-intro

- Game Intro plays animated content before the Title screen first appears.
- Configure it through Global Customizations: select media, skipping behavior, fade-out, skip text, volume and sound channel.
- Native video intros depend on Watermedia V3 + Watermedia Binaries V3 and OpenGL; Vulkan playback is not supported.
- A re-trigger option is available for testing.

## 26. Get Frames from Videos

Source: https://docs.fancymenu.net/docs/en-US/ffmpeg-frames

- Use FFmpeg to export PNG frames from a source video for later AFMA/FMA creation.
- `ffmpeg -i input.mp4 output_frames/frame_%04d.png` extracts every frame.
- `-vf "fps=10"` can reduce frame rate, and `-vf "scale=1280:720"` can resize frames.
- Reducing frame count and dimensions lowers animation file size and memory use.

## 27. Getting Started

Source: https://docs.fancymenu.net/docs/en-US/home

- The FancyMenu menu bar is the entry point to customization features; `Ctrl+Alt+C` toggles it.
- To customize a screen, enable Current Screen Customization, then create `Layouts -> New -> For Current Screen`.
- Use the Layout Editor to add and edit elements and existing vanilla/mod widgets.
- Elements use anchor points for responsive positioning. Save with `Layout -> Save` or `Ctrl+S`.

## 28. Global Customizations

Source: https://docs.fancymenu.net/docs/en-US/global-customizations

- Global Customizations apply common behavior/visual changes without editing every screen layout.
- They cover game intro, world/server icons, seamless loading, window icon/title, default GUI scale, fullscreen-on-launch, global button/slider textures/labels, menu backgrounds, menu music and click sounds.
- Global music tracks replace vanilla menu music on non-world menus and are randomly selected.
- For layout-specific styling or in-world audio, use layout elements instead.

## 29. GLSL Shader API

Source: https://docs.fancymenu.net/docs/en-US/glsl-shader-api

- FancyMenu can render GLSL as a menu background, element or decoration overlay.
- The runtime uses OpenGL/GLSL `#version 150` and supports single-pass and multipass Buffer A-D + Image setups.
- Shadertoy-style `mainImage` and direct `main` fragment entry points are supported.
- FancyMenu variables can be exposed as runtime uniforms such as `fmVarFloat_<name>` and `fmVarExists_<name>`; sanitize variable names carefully to avoid collisions.

## 30. Ice & Fire Main Menu

Source: https://docs.fancymenu.net/docs/en-US/ice-and-fire

- Ice and Fire's custom title screen can interfere with FancyMenu customization.
- Disable the mod's custom main-menu option in its client configuration (`iceandfire-client.toml`) and restart.
- Depending on mod version, the exact option format may differ; use the client config entry that controls the custom main menu.

## 31. Images

Source: https://docs.fancymenu.net/docs/en-US/images

- Image elements can load Minecraft/resource-pack locations, local files or web URLs.
- Images support features such as tinting, rounded corners, nine-slicing, repeating/tiling and parallax depending on configuration.
- Use `Restore Aspect Ratio` and Shift-resize to avoid distortion.
- Local images should be kept in `config/fancymenu/assets/` and sized close to their displayed resolution for memory/performance efficiency.

## 32. Import/Export Layouts

Source: https://docs.fancymenu.net/docs/en-US/share-layouts

- Share FancyMenu setups by packaging the required files from `config/fancymenu/` together with any referenced assets.
- Imported layouts/action scripts can perform powerful actions, so only import from trusted sources.
- For modpack distribution, place the same configuration/assets into the pack's config structure.

## 33. Incompatibility List

Source: https://docs.fancymenu.net/docs/en-US/incompatibility-list

- The docs maintain a community-reported incompatibility list that can change with FancyMenu, Minecraft, loader and mod versions.
- Known examples include mods that replace loading screens, alter menu rendering, break specific placeholders/audio behavior or make third-party buttons uncustomizable.
- Always retest with current versions before treating an old report as permanent.

## 34. JSON Paths

Source: https://docs.fancymenu.net/docs/en-US/jsonpath

- The JSON Parser placeholder uses Jayway JsonPath.
- Paths start at `$`; common operators include child access, array indices/slices, wildcard `*`, recursive `..` and filters `[?(...)]`.
- Functions such as `min()`, `max()`, `avg()`, `length()`, `sum()` and `keys()` can process path results.
- Use quoted string literals in filters and remember numeric and string values are not automatically equivalent.

## 35. Known Issues

Source: https://docs.fancymenu.net/docs/en-US/known-issues

- The current docs list unresolved issues for the latest FancyMenu build.
- At the time checked, there were no fixable issues listed.
- One documented rendering limitation is that Player Entity and Item elements may render in front of normal GUI layers on older Minecraft versions.

## 36. Layers and Groups

Source: https://docs.fancymenu.net/docs/en-US/layers-and-groups

- Use the Layers/Groups editor widget to control ordering and organize custom elements.
- Drag elements in the hierarchy or use move-up/move-down actions from the element context menu.
- Grouping helps manage complex layouts and shared structure.
- Some special renderers such as Player Entity/Item may not obey ordinary GUI layer ordering in certain Minecraft versions.

## 37. Layout Templates

Source: https://docs.fancymenu.net/docs/en-US/layout-templates

- Layout templates are reusable starting points for layouts so you can avoid rebuilding common structure repeatedly.
- Use templates for repeated branding, common controls or standard screen framing, then customize the resulting layout per screen.
- For visual styling that should affect every vanilla button/slider, Global Customizations may be more appropriate than cloning a full template.

## 38. Listeners

Source: https://docs.fancymenu.net/docs/en-US/listeners

- Listeners run Action Scripts when a particular event happens.
- Listener scripts receive event-specific read-only `$$` values such as key names, mouse buttons, text-event IDs, download result paths or network request IDs.
- Listener variables are strings, case-sensitive and scoped to that listener execution.
- Validate untrusted data from chat, remote servers, files and user input before using it in paths, URLs or commands.

## 39. Loading Screen

Source: https://docs.fancymenu.net/docs/en-US/loading-screen

- FancyMenu itself does not customize Minecraft's startup/resource-reload splash screen.
- Use the Drippy Loading Screen add-on for that screen.
- Do not confuse it with world/server loading screens, which FancyMenu can access through dummy screen instances.

## 40. Localizing Layouts

Source: https://docs.fancymenu.net/docs/en-US/localization

- Use the Localize Text placeholder: `{"placeholder":"local","values":{"key":"..."}}`.
- FancyMenu first checks active Minecraft language resources, then its custom localization files.
- Custom localization files can be JSON, `.lang` or `.properties` under `config/fancymenu/custom_locals/`.
- For automatic language switching, use normal resource-pack language files under `assets/<namespace>/lang/<language>.json`.
- Use the Is Game Language requirement to swap whole images/elements/layouts by locale.

## 41. Menu Background Music

Source: https://docs.fancymenu.net/docs/en-US/background-music

- FancyMenu can disable vanilla menu music, define a global replacement playlist or use Audio elements for per-layout music.
- Global tracks are configured under Global Customizations and play only when no world is loaded.
- A Music Controller element can separately enable/disable menu and world music on a screen.
- Use an Audio element when you need per-screen/in-world playlists, shuffle/order, custom volume/channel or loading requirements.

## 42. Menu Backgrounds

Source: https://docs.fancymenu.net/docs/en-US/menu-backgrounds

- Background types include vanilla, image, slideshow, cubic panorama, color, Browser, native Video, GLSL and add-on types.
- Configure from the layout editor background context menu under Menu Backgrounds.
- Multiple background types can be stacked, including across multiple active layouts.
- Transparent pixels on the bottom-most custom background reveal FancyMenu's black backing layer, so use an opaque base layer when needed.
- Native video backgrounds use Watermedia; old Rinku Video backgrounds are deprecated.

## 43. Modified FancyMenu Found

Source: https://docs.fancymenu.net/docs/en-US/modified-fancymenu

- The modified-JAR warning can indicate a changed FancyMenu file, a broken instance/loader launch, failed mod loading or a problematic launcher.
- Check `logs/latest.log`, reinstall/recreate the instance if necessary and use a supported launcher.
- Cracked/pirate launchers are specifically called out in the official docs as a common trigger.

## 44. Modpacks

Source: https://docs.fancymenu.net/docs/en-US/modpacks

- FancyMenu v3 setups can be shipped in modpacks by copying the relevant `config/fancymenu/` data and referenced assets into the pack.
- The active launcher instance is the source directory; it may not be the global `.minecraft` folder.
- Test the packaged result in a clean instance to verify all resources, paths and optional dependencies are included.
- Treat imported layouts/scripts as executable configuration and distribute only trusted content.

## 45. NBT Data Placeholder

Source: https://docs.fancymenu.net/docs/en-US/nbt-data-placeholder

- `nbt_data_get` reads client-visible entity/block-entity NBT; `nbt_data_get_server` uses server-side `/data get` behavior and needs FancyMenu on the server.
- Client sources support entities or blocks, selectors/UUID/name, absolute block coordinates, NBT paths, scaling and several return types.
- Server mode can additionally use full server selectors, relative/local block coordinates and command storage.
- Invalid targets/paths return an empty value and details are written to `logs/latest.log`.

## 46. Nine-Slicing & Tiling

Source: https://docs.fancymenu.net/docs/en-US/nine-slicing-and-tiling

- Nine-slicing preserves corners/borders while stretching a texture's center.
- It is supported for button/slider textures, Image elements, Progress Bars, Tooltips and global widget styles.
- Tiling repeats a seamless texture instead of stretching it and is useful for Image elements, image backgrounds and scroll header/footer textures.
- Set border sizes to match the fixed edge region of the source texture.

## 47. Open GUIs by Command

Source: https://docs.fancymenu.net/docs/en-US/opengui-command

- `/openguiscreen <screen_identifier> [target_players]` opens supported vanilla, mod or Custom GUI screens.
- Find a screen identifier with the debug overlay (`Ctrl+Alt+D`) and copy it exactly; identifiers are case-sensitive.
- Targeting other players requires FancyMenu server-side, FancyMenu on their clients and sufficient permissions.
- `/closeguiscreen [target_players]` closes the current screen.

## 48. OptiFine Alternatives

Source: https://docs.fancymenu.net/docs/en-US/optifine-alternatives

- FancyMenu documentation recommends modern alternatives rather than relying on OptiFine in modded setups where compatibility is important.
- On Forge/NeoForge, common replacements focus separately on rendering performance, shaders, connected textures, dynamic lights and zoom features instead of one monolithic mod.
- Choose alternatives compatible with your Minecraft version and loader, then verify them against FancyMenu's incompatibility list.

## 49. Optimizing Textures

Source: https://docs.fancymenu.net/docs/en-US/optimizing-textures

- FancyMenu uses supplied textures largely as-is, so oversized files waste memory and very small files blur when stretched.
- Use images close to their intended display resolution and preserve aspect ratio.
- Use nine-slicing or tiling for scalable UI panels/buttons rather than storing giant textures.
- Large animated or video-like assets should be reduced in resolution/frame count when possible.

## 50. Panoramas

Source: https://docs.fancymenu.net/docs/en-US/panoramas

- Custom cubic panoramas live under `config/fancymenu/panoramas/<name>/`.
- Each panorama uses `properties.txt`, a `panorama/` folder with exactly `panorama_0.png` through `panorama_5.png`, plus optional `overlay.png`.
- The six PNG faces must share dimensions.
- Properties include a unique runtime name plus optional speed, FOV, vertical angle and start rotation. Reload FancyMenu or restart, then select the panorama as a menu background.

## 51. Parallax Effect

Source: https://docs.fancymenu.net/docs/en-US/parallax

- Parallax offsets an Image background or supported element based on mouse movement.
- Configure independent X/Y intensity and optional inverted movement.
- Lower intensity works well for distant layers and stronger intensity for foreground layers.
- Background parallax conflicts with the option that slides wide images left-to-right.

## 52. Placeholders

Source: https://docs.fancymenu.net/docs/en-US/placeholders

- Placeholders insert live data into text, paths, action values and many element settings.
- Normal placeholders use JSON-like syntax and can be nested; examples include player/screen data, variables, calculations, options, JSON parsing, random/file text and many more.
- Some placeholders accept named values; Action/Listener context can also provide `$$` values.
- Remote/file/JSON placeholders may involve untrusted external data, so validate before feeding results into commands, paths or URLs.

## 53. Player Entities

Source: https://docs.fancymenu.net/docs/en-US/player-entities

- Player Entity elements can mirror the local player, show another real player by name, or use fully custom skin/cape/name data.
- Pose settings expose rotations for body parts and can use placeholders in advanced mode.
- On Minecraft 1.20.1+, normal resize handles can scale the entity; older versions use a scale property.
- Recent 1.20.1+ setups require the optional Fancy Entity Renderer dependency for Player Entity rendering.

## 54. Player Heads

Source: https://docs.fancymenu.net/docs/en-US/player-heads

- A simple way to show a player's head in an Image element is to use a web avatar service with the player-name placeholder.
- The official docs demonstrate Minotar URLs for flat and cube-style head renders.
- Because this is an external web dependency, availability/performance depends on the third-party service and network.

## 55. Positioning Elements

Source: https://docs.fancymenu.net/docs/en-US/positioning-elements

- FancyMenu positions elements relative to anchor points so layouts survive window resizing and GUI-scale changes.
- Elements can also be anchored to other elements, creating parent/child movement relationships.
- `Stay On Screen` prevents accidental off-screen placement; disable only when intentional.
- `Sticky Anchors` helps dynamically sized elements remain aligned to the intended side/center of their anchor.
- Forced GUI Scale and layout Auto-Scaling are additional tools for dense/responsive layouts.

## 56. Pre-Load Resources

Source: https://docs.fancymenu.net/docs/en-US/preload-layouts

- Pre-loading warms selected resources during startup/resource reload so they are ready before the target menu opens.
- Supported entries include images, animations, audio, video and text from local/web/resource-pack sources; live Browser pages are not preloaded.
- Preloading can increase startup time and memory/VRAM use, so include only assets that need immediate readiness.
- FancyMenu reload releases the cache but does not run the pre-loader; restart or Minecraft resource reload after changing the list.

## 57. Randomize Layouts

Source: https://docs.fancymenu.net/docs/en-US/randomize-layouts

- Enable Random Mode on several layouts and give them the same Random Group Identifier to choose one layout from the group.
- `Randomize Only First Time` keeps the first choice for the whole game session; otherwise a new choice can be made whenever the screen opens.
- Use separate minimal layouts to randomize only a background or one element instead of duplicating a full screen design.

## 58. Remote Server Communication

Source: https://docs.fancymenu.net/docs/en-US/remote-server-communication

- FancyMenu can open client-initiated WebSocket connections to external remote servers.
- Actions can connect, send data, close one connection by request ID or close all remote connections.
- Listeners can react to connection/data events and expose request-related `$$` values.
- For safety, the client initiates the connection. Prefer secure `wss://` endpoints and treat received data as untrusted.

## 59. Resources

Source: https://docs.fancymenu.net/docs/en-US/resources

- FancyMenu resource fields can use Minecraft/resource-pack locations, local files or direct web URLs.
- Minecraft resources use `namespace:path`; local assets conventionally live in `config/fancymenu/assets/`.
- Web resources should be direct file URLs for reliability/performance.
- Many resource-source fields support placeholders, enabling dynamic paths/URLs.

## 60. Rotating Elements

Source: https://docs.fancymenu.net/docs/en-US/rotating-elements

- Supported elements show a circular rotation grabber in the editor.
- Rotation can also be set manually and the direct input supports placeholders.
- Rotation is visual for interactive controls: a rotated button still uses its original unrotated hitbox.

## 61. Schedulers

Source: https://docs.fancymenu.net/docs/en-US/schedulers

- Schedulers run Action Scripts on a timed basis independently of a particular visible screen.
- They are better than a screen-bound Ticker for background/repeating automation.
- Schedulers have identifiers that can be started/stopped from actions (`start_scheduler` / `stop_scheduler`).
- Keep repeated work lightweight and avoid expensive actions such as constant FancyMenu/resource reloads.

## 62. Screen Identifiers

Source: https://docs.fancymenu.net/docs/en-US/screen-identifiers

- Screen identifiers tell FancyMenu which screen a layout/action refers to and are case-sensitive.
- `Ctrl+Alt+D` toggles the debug overlay, which shows the current identifier and lets you copy it.
- Built-in screens often use short identifiers; mod screens may use class-like identifiers; Custom GUIs use their configured identifier.
- Some screens cannot be directly constructed by Open Screen; mimic the normal vanilla/mod button when needed.

## 63. Scrollable Screens

Source: https://docs.fancymenu.net/docs/en-US/customizing-scrollable-screens

- FancyMenu cannot normally see the content inside many Minecraft scroll areas.
- `Customization -> Expose Scroll Area Content Of Current Screen` can experimentally expose normal widgets for some screens.
- Exposing stacks the widgets in the top-left for editing and removes the original scroll container; non-widget content in that scroll region is lost while exposure is enabled.
- Not every screen supports the feature.

## 64. Seamless World Loading

Source: https://docs.fancymenu.net/docs/en-US/seamless-world-loading

- Seamless World Loading is a Global Customization that uses a recent screenshot of a world/server as the next loading-screen background.
- The effect makes re-entering that world/server appear more continuous before the real world finishes rendering.
- Because it relies on stored screenshots, test the result with your world-loading layouts and privacy expectations.

## 65. Set/Get Minecraft Options

Source: https://docs.fancymenu.net/docs/en-US/minecraft-options

- Use the `minecraft_option_value` placeholder to read Minecraft settings and the Set Minecraft Option action to write them.
- Common option names include sound categories, FOV, gamma and render distance.
- Sliders can use `$$value` and set their pre-selected value from the matching placeholder.
- FOV uses an internal -1..1 mapping rather than the displayed 30..110 scale; the docs show a formula to convert it for labels.

## 66. Shortcuts & Keybinds

Source: https://docs.fancymenu.net/docs/en-US/shortcuts

- Layout Editor basics: Ctrl/Command+C/V copy/paste, Ctrl/Command+S save, Ctrl/Command+Z/Y undo/redo, Ctrl/Command+A select all, Delete delete, arrows nudge, Shift-resize preserves aspect ratio.
- Global shortcuts include `Ctrl+Alt+L` last layout, `Ctrl+Alt+C` menu bar, `Ctrl+Alt+R` reload FancyMenu and `Ctrl+Alt+D` debug overlay.
- The text editor supports standard selection/copy/paste plus duplicate line, move line, go-to-line and save shortcuts.

## 67. Sinytra Connector

Source: https://docs.fancymenu.net/docs/en-US/sinytra-connector

- The official FancyMenu docs do not provide support for Sinytra Connector and recommend removing it first when diagnosing instability/crashes.
- Connector mixes mods from different loaders, so compatibility issues may not be reproducible in a normal supported environment.

## 68. Slideshows

Source: https://docs.fancymenu.net/docs/en-US/slideshows

- Slideshows live under `config/fancymenu/slideshows/<name>/` with `properties.txt`, an `images/` directory and optional overlay.
- Supported images are `.png` and `.jpg`; use predictable zero-padded filenames for ordered playback.
- Metadata controls runtime name, base dimensions/position, duration, fade speed and randomization.
- Use the Slideshow element or Menu Backgrounds -> Slideshow after reloading FancyMenu/restarting.

## 69. Splash Text

Source: https://docs.fancymenu.net/docs/en-US/splash-text

- FancyMenu's custom Splash Text element replaces the hard-to-customize vanilla title splash.
- Sources can be vanilla splashes, a direct single line or a text file containing multiple candidate lines.
- Text supports placeholders, Minecraft formatting/component data, scale, rotation, color, shadow and bouncing.
- Enable refresh-on-screen-load when you want a new random splash each time.

## 70. Text Formatting

Source: https://docs.fancymenu.net/docs/en-US/text-formatting

- Text elements support Markdown-like rich formatting including headings, emphasis, lists, quotes, code, tables, links and images.
- FancyMenu adds click/hover text events that can be handled by listeners and expose `$$text_event_id`.
- Markdown image links can use web, local or Minecraft resource sources.
- Local referenced resources belong under `config/fancymenu/assets/`.

## 71. Text in Menus

Source: https://docs.fancymenu.net/docs/en-US/text-in-menus

- Use a Text element for one-line labels through large scrollable Markdown documents.
- Text can come from direct content, local files, resource-pack files or web text sources.
- Web text is useful for remotely updated changelogs/news, but FancyMenu caches fetched content for performance.
- Text supports wrapping, scrolling, placeholders and Markdown formatting.

## 72. Tilting Elements

Source: https://docs.fancymenu.net/docs/en-US/tilting-elements

- Supported elements have horizontal/vertical tilt grabbers.
- Tilt can be entered manually and can use placeholders for dynamic values.
- Like rotation, tilting is visual for interactive controls; their click areas stay untilted.

## 73. Title Screen Copyright

Source: https://docs.fancymenu.net/docs/en-US/copyright-notice

- The title-screen copyright notice cannot be fully removed by FancyMenu.
- It can be moved within screen bounds and its opacity can be adjusted, but the mod deliberately prevents making it completely invisible.

## 74. Title Screen Glyph

Source: https://docs.fancymenu.net/docs/en-US/title-screen-glyph

- The small green/blue diamond-like glyph on the Title screen often comes from Mod Menu update indicators, Forge/NeoForge version checking or Vanilla Realms notifications.
- Disable Mod Menu's update indicator in Mod Menu settings.
- Forge/NeoForge version checking can be disabled with `versionCheck = false` in `config/fml.toml`.
- Realms notification elements can be hidden from a current-screen Title layout when exposed as dedicated elements.

## 75. Toggle Parts of Layouts

Source: https://docs.fancymenu.net/docs/en-US/toggle

- Use a FancyMenu variable as persistent state, change it from a button action and add requirements to the elements/layout parts that should be visible for each state.
- This pattern supports simple on/off toggles and multi-state selectors.
- Variables plus requirements are the core building blocks; action scripts can advance the state.

## 76. Universal Layouts

Source: https://docs.fancymenu.net/docs/en-US/universal-layouts

- A Universal Layout is considered for every supported screen whose customization is enabled.
- Create one with `Layouts -> New -> For All Screens [Universal]`.
- Use whitelist/blacklist screen identifiers and layout-wide requirements to limit where it applies.
- Universal layouts are useful for shared logos, overlays, navigation and persistent Audio, but they do not expose vanilla widgets for editing.

## 77. Vanilla Elements

Source: https://docs.fancymenu.net/docs/en-US/vanilla-elements

- Create a layout for the current screen to edit existing vanilla/mod widgets.
- You can modify labels/textures and move/resize widgets after assigning a non-Original anchor; you cannot replace their built-in action script.
- Automated Clicks can invoke a widget's original left-click callback when the screen loads.
- Delete hides a widget and it can be restored later. Some title-screen non-widget elements are intended to be hidden/replaced by custom elements.
- Mods that replace the parent screen can prevent FancyMenu customizations from applying.

## 78. Variables

Source: https://docs.fancymenu.net/docs/en-US/variables

- Variables store reusable text values available to layouts, actions, placeholders, requirements, listeners, schedulers and Custom GUIs.
- Create/manage them under `Customization -> Variables -> Manage Variables`; values can also be set from actions.
- Read with the Get Stored Variable placeholder and combine with Calculator/requirements for counters, state machines and dynamic UI.
- Values persist in `config/fancymenu/user_variables.db`; names are case-sensitive. Missing/empty variables return `0` from the get-variable placeholder.
- Do not store passwords/tokens in FancyMenu variables.

## 79. Videos (MP4)

Source: https://docs.fancymenu.net/docs/en-US/video

- Native Video elements/backgrounds and video Game Intro require Watermedia V3 + Watermedia Binaries V3 and an OpenGL renderer.
- Old Rinku video types are deprecated for new layouts.
- Actions can control video element/background volume, playback position and paused state; a listener can react to playback states.
- Video is not supported on loading screens; use short AFMA/FMA animations there instead.

## 80. Widget Locators

Source: https://docs.fancymenu.net/docs/en-US/widget-locators

- Widget locators identify vanilla/mod widgets so FancyMenu actions such as Mimic Vanilla/Mod Button can target the exact control.
- A locator combines screen/widget identity rather than relying only on visible label text.
- Copy/use the locator exactly as shown by FancyMenu debugging/editor tools; it is especially useful for mod screens that cannot be opened directly by identifier.

## 81. Window Icon & Title

Source: https://docs.fancymenu.net/docs/en-US/window-customization

- Global Customizations can replace the Minecraft window icon and title.
- Custom icons use PNG (and ICNS for macOS where required).
- FancyMenu's docs warn that the icon should contain at least one fully transparent pixel to avoid an LWJGL/Minecraft rendering glitch.
- Configure all required icon sizes before enabling the custom icon.

## 82. World Creation Screen

Source: https://docs.fancymenu.net/docs/en-US/world-creation-screen

- The Create World/New World screen is unusually complex and has become harder to customize in newer Minecraft versions because of tabs and stacked widgets.
- The official docs recommend avoiding heavy edits to its existing widgets/elements.
- A custom background is generally safer than a full reconstruction of the screen.

## 83. World Loading Screens

Source: https://docs.fancymenu.net/docs/en-US/dummy-screen-instances

- Fast-closing world/server loading screens can be opened as Dummy Screen Instances from `Tools -> Dummy Screen Instances` so you can edit them without them disappearing.
- Examples include the Level Loading Screen and Generic Dirt Message Screen.
- These are separate from Minecraft's startup/resource-reload splash screen, which requires Drippy Loading Screen.

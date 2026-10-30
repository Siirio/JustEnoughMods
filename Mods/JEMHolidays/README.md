# JEM Holidays

JEM Holidays is the standalone owner for annual JEM presentation and bounded seasonal world-time changes

## Halloween calendar

- The client local calendar enables Halloween from October 30 through November 2 inclusive
- November 3 through October 29 uses the normal presentation
- The current state is applied on startup and re-evaluated at the next local midnight
- The system timezone is checked every 15 minutes so an operating-system timezone change cannot leave a stale date indefinitely
- A state transition runs only when the calendar crosses the seasonal boundary, preventing repeated toggles around midnight

## Resource ownership

- The controller places `file/JEM Jalloween v0.1.zip` at seasonal top priority during Halloween
- When the separately installed `Halloween GUI 1.20` pack is available, it is placed above JEM Jalloween so its GUI textures override the release fallback
- Before the first seasonal transition, the controller records the selected positions of only the seasonal packs
- On November 3 it removes every seasonal addition, including the GUI pack when no restoration file exists, and restores any previously selected seasonal pack near its saved neighbors
- Unrelated pack additions, removals and relative priority remain untouched
- Closing the resource-pack screen schedules one delayed reconciliation so it does not overlap the screen's own resource reload
- The Jalloween pack supplies reversible autumn grass and foliage colormaps, seasonal leaves, the menu marker and other seasonal presentation without changing biome or world data
- The Halloween GUI archive is the official CurseForge file `9143523` with only its opaque panorama overlay removed, so every supplied GUI texture remains readable while FancyMenu keeps the approved JEM background
- The Jalloween FancyMenu layout uses its own purple 190 by 20 button backgrounds for normal, hover and inactive states, while ordinary layouts retain the existing blue set

## Audio status

- AudioYT 1.0.1 remains the only extractor, decoder and playback engine
- During Halloween the main menu loops `2PNX8arJLoU`
- In worlds the controller rotates `neUXoluDW1k` and `EU5XFHhuLzY`; the rejected FNAF track `jSWLy5iMWV4` remains excluded
- Moving between the main menu and a world stops the JEM-owned AudioYT sound before starting the other playlist
- Only sounds in Minecraft's background `MUSIC` category are rejected while the seasonal playlist is active; ambience, effects, voices and `RECORDS` music discs are untouched
- While a vanilla record or positional AudioYT jukebox is audible, the seasonal AudioYT track continues silently and resumes at its current position after the player leaves the audible radius, preventing overlapping music without interrupting the disc
- The AudioYT duration limit is temporarily disabled only while Halloween playback is active and its configured value is restored when the season ends
- Seasonal volume follows Minecraft's MUSIC and MASTER sliders without the competing gain writes that caused periodic white-noise bursts
- On November 3 JEM Holidays stops only its owned AudioYT sound and returns Minecraft background music to its ordinary scheduler
- No YouTube recording is bundled or downloaded by JEM Holidays itself

## Halloween night

- From October 30 through November 2 the logical server keeps every skylit non-fixed-time dimension at midnight
- Dedicated multiplayer uses the server operating-system timezone because one authoritative world cannot follow different client timezones, while client presentation continues to use each client operating-system timezone
- The controller does not change `doDaylightCycle`, world data formats or biome data
- On November 3 it stops writing day time and the existing daylight rule continues ordinary time from the current night
- AudioYT 1.0.1 is installed on the dedicated server as well as clients because the upstream mod declares both-side behavior and a mandatory Forge channel for its jukebox functionality

## Halloween mob pumpkins

- During the Halloween calendar window rendered humanoid mobs receive a full-bright vanilla jack-o-lantern over their heads
- Zombies and their variants, skeletons, villagers, illagers, witches, piglins, endermen and other mobs using a humanoid renderer are included
- Players, animals, spiders, slimes, creepers, ravagers and other non-humanoid mobs are excluded, so the seasonal decoration never replaces player head equipment or covers unrelated creature silhouettes
- Pumpkin size follows the mob's rendered bounding-box width and height, preserving a proportional result for babies, ordinary mobs and large bosses
- The decoration is presentation-only and does not change equipment, drops, light levels, collision or server state

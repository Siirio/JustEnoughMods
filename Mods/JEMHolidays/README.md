# JEM Holidays

JEM Holidays is the standalone client-only owner for annual JEM presentation changes

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
- On November 3 it removes seasonal additions and restores any previously selected seasonal pack near its saved neighbors
- Unrelated pack additions, removals and relative priority remain untouched
- Closing the resource-pack screen schedules one delayed reconciliation so it does not overlap the screen's own resource reload
- The Jalloween pack supplies reversible autumn grass and foliage colormaps, seasonal leaves, the menu marker and other seasonal presentation without changing biome or world data

## Audio status

Automatic AudioYT playback is disabled in 1.0.1 after sandbox acceptance exposed two upstream limitations: the requested ambience videos exceed the observed 600-second loading boundary, and the AudioYT Java Sound output intermittently produces harsh white-noise bursts. The FNAF track `jSWLy5iMWV4` was also rejected stylistically and has been removed

JEM Holidays does not suppress Minecraft music while seasonal streaming is disabled. It does not implement a replacement extractor or playback engine, and no recording or AudioYT binary is redistributed. Seasonal calendar, packs, autumn visuals and menu theme remain fully local and offline

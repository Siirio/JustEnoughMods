# Time and seasons HUD

The user requested two circular, native pixel HUD indicators in the 2026-10-23
Food/Pet Shelters/Seasonal HUD v2 specification. JEMCompat owns client rendering;
Minecraft owns synchronized day time, and Serene Seasons owns seasonal state and
configuration. No JEM calendar, time mutation, server packet or gameplay mechanic
is introduced here.

## Ownership and presentation

- `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/client/time/TimeSeasonHud.java`
  draws the two indicators through Forge client events. Dedicated servers never
  register this subscriber.
- `client/time/SeasonHudState.java` is a presentation-only snapshot.
- `compat/seasons/SeasonHudAdapter.java` is the only new class referencing Serene
  Seasons. It is invoked only after the client checks the native mod ID.
- `config/JEMClientConfig.java` owns `showTimeHud`, `showSeasonHud`,
  `timeHudOffsetX`, and `timeHudOffsetY`. Both indicators default enabled, at
  (8, 8) GUI pixels with a 4-pixel gap. If one is hidden, the remaining indicator
  takes the first position. Configured positions are clamped to the GUI viewport.
- Resource contract: `assets/jemcompat/textures/gui/time/day_cycle.png` and
  `season_cycle.png`, each exactly 24x24 with transparency. Spring is centered
  upward, summer rightward, autumn downward, winter leftward; quarter boundaries
  run diagonally. Production art is subject to the project visual approval gate.

F1 hides both indicators. They render on the ordinary HUD and above chat and
container screens, where a real mouse pointer permits native tooltips. Full-screen
books, menus and other modal screens keep their existing composition. Offsets and
individual toggles let players avoid other overlays; no global overlay layout is
claimed. Tooltips use the current client language.

Day progression reads the native dimension celestial angle every frame, with no
extrapolated wall-clock timer. Approximate world-clock text reads synchronized
day time with native fixed-time dimension behavior. The native clock coordinate
is not an assumed real-time day duration; sleeping, time commands and changed
server time rates are reflected in the source value. There are no tick counts in
player-facing text. Dawn/day/dusk/night labels describe that native clock phase.

## Installed integration evidence

Inspected installed `SereneSeasons-forge-1.20.1-9.1.0.3.jar` through exact class
signatures and bytecode, together with `Server/config/sereneseasons/seasons.toml`.
The installed configuration whitelists `minecraft:overworld`; each sub-season is
8 Minecraft days and each season therefore 24 Minecraft days. These values are
evidence of this local configuration, never copied into the HUD calculation.

- `ISeasonState` supplies `getCycleDuration`, `getSeasonDuration`,
  `getSeasonCycleTicks`, `getSubSeason`, and `getTropicalSeason`.
- `SeasonTime` derives all durations from `ModConfig.seasons` and its native
  `SubSeason.VALUES`, rather than an independent JEM duration table.
- `CalendarItem` uses `ModConfig.seasons.isDimensionWhitelisted` and
  `SeasonHelper.getSeasonState`. This adapter follows those same native owners.
- `ModConfig.init` registers seasons configuration with GlitchCore `ConfigSync`.
- `SeasonHandlerClient.clientSeasonCycleTicks` is the native client dimension
  state maintained by Serene Seasons. The HUD waits until that dimension has
  native state. This is an initialization check, not a separate proof of first
  server packet receipt; the HUD intentionally shares the native calendar's
  synchronization semantics.
- Existing native translation keys `desc.sereneseasons.<subseason>` and
  `desc.sereneseasons.<tropicalseason>` provide season names. In tropical biomes,
  the tooltip also names the actual tropical season; the wheel remains the
  requested four-season calendar rather than fabricating tropical quadrants.

Season progress is native elapsed season time divided by native season duration;
the pointer uses native elapsed cycle time divided by native cycle duration.
Non-whitelisted dimensions show a dimmed neutral wheel with the localized
"No seasonal cycle here" tooltip. When Serene Seasons is absent, its adapter is
not invoked and the season indicator is hidden. No day/season lengths change.

## Verification boundary

Source and installed bytecode inspected only. No automated tests, fixtures,
Gradle build, gameplay launch, linter, formatter or static-analysis run was added
or performed for this HUD subtask. Asset approval, actual compiled artifact
verification and player acceptance are separate task-wide gates; this document
does not claim them complete.

## Task-wide packaging and art handoff

Production Java compilation/reobfuscation succeeded during the parent task after adding the already-installed GlitchCore0.0.1.1 as compile-only: the native Serene Seasons config inherits its Config class. A later final packaging receipt records the newest aggregate result. No runtime launch or player acceptance is implied. The two exact HUD textures are not yet approved/supplied. The user rejected the authored day/night candidates and chose local Gemini/manual drawing; no rejected PNG was copied into module resources. See the [visual handoff](../art/food-pets-season-hud-2026-10-23/README.md). Installation/publication remains pending actual production art.

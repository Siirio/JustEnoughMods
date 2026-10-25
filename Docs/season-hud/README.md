# Time and seasons HUD

## Current inventory placement, 2026-10-25

The user requested the two indicators only while player inventory is open, at the top right, and explicitly selected circular icons through Gemini. Survival and creative player inventory share this presentation. Other screens and the ordinary gameplay HUD no longer draw the pair

The existing 24x24 footprint, four-pixel gap, native calendar reads and tooltips remain. Horizontal offset now measures the right margin, vertical offset the top margin. With defaults, the day dial begins at `screenWidth - 60, 8` and the season dial at `screenWidth - 32, 8`. A single enabled dial uses the rightmost position. Offsets remain clamped to the viewport. No native inventory slots or controls move

The user selected4 Argon in Work, reviewed the final native pair and explicitly approved activation. The approved original24px layer grids were painted through the local Aseprite MCP and exported directly. [Editable source, native previews and visual-auditor evidence](../art/inventory-season-hud-2026-10-25/README.md) retain the actual provenance. Earlier rejected Chat candidates remain excluded

`timeHud.showInventoryDayNight` and `timeHud.showInventorySeasons` now default true. The inventory-specific keys replace the unfinished HUD's old false-default switches, so existing profiles gain this explicitly approved inventory feature through normal Forge config loading. Existing right/top offset values retain their keys. No raw profile config copy or state-pointer edit is used. Both indicators can be disabled separately through their native client configuration

The earlier top-left, general-HUD and missing-art/default-off descriptions below are historical and superseded by this explicit placement and activation request

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
- `config/JEMClientConfig.java` owns `timeHud.showDayNight`, `timeHud.showSeasons`,
  `timeHud.horizontalOffset`, and `timeHud.verticalOffset`. Both indicators default disabled while the user's approved textures are missing. When enabled they render at
  (8, 8) GUI pixels with a 4-pixel gap. If one is hidden, the remaining indicator
  takes the first position. Configured positions are clamped to the GUI viewport.
- Resource contract: `assets/jemcompat/textures/gui/time/day_cycle.png` and
  `season_cycle.png`, each exactly 24x24 with transparency. Spring is centered
  upward, summer rightward, autumn downward, winter leftward; quarter boundaries
  run diagonally. Production art is subject to the project visual approval gate.

The rejected clock and season candidates are excluded from production. The user
reserved those icons for their own authoring. Food and existing seasonal gameplay
can be delivered without enabling this unfinished HUD. Supply each approved PNG
at the exact resource path above before enabling its toggle in
`config/jemcompat-client.toml`. Existing profiles with previously enabled toggles
must set both toggles to `false` until those resources are supplied. Default changes
do not override an existing Forge configuration. No replacement art, native-icon
substitute, silent missing-resource guard or new visual design is introduced.

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

Production Java compilation/reobfuscation succeeded during the parent task after adding the already-installed GlitchCore0.0.1.1 as compile-only: the native Serene Seasons config inherits its Config class. A later final packaging receipt records the newest aggregate result. No runtime launch or player acceptance is implied. The two exact HUD textures are not yet approved/supplied. The user rejected the authored day/night candidates and chose local Gemini/manual drawing; no rejected PNG was copied into module resources. See the [visual handoff](../art/food-pets-season-hud-2026-10-23/README.md). HUD activation remains pending actual production art. Food and existing seasonal gameplay may be installed and published with both optional indicators explicitly disabled.

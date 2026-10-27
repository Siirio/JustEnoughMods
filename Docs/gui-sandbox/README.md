# JEM GUI Sandbox

User request, 2026-10-22: put fully clickable copies of the existing JEM GUIs in
`jem sandbox`, with no underlying gameplay logic; add native GUI mods including
Curios, Backpacked and the then-installed TrashSlot. The user explicitly selected
reproducing the existing screens rather than redesigning them.

Open **F8**, **/jemgui**, **/smp**, **/settlements** or **/death** in a sandbox world.
`/jemgui <screen>` opens an individual view; the gallery has pagination on smaller
windows. F8 returns to the gallery. Escape closes an ordinary page. Reset demo data
restores the initial in-memory sample; leaving the world also resets it.

## Gallery coverage

| Route | Native presentation reused |
| --- | --- |
| `smp`, `profile`, `players`, `parties`, `prizes` | JEM Server's SMP screens, sidebar, forms, search, details, lists and lobby controls |
| `settlements`, `settlement-directory`, `founding`, `settlement-guide` | JEM Settlements' dashboard, resident details, happiness, storage, treasury, territory, founding and guide pages |
| `death`, `bosses`, `landmarks`, `homes` | JEM Server's native menu renderer with local destination entries and an inert confirmation |
| `market` | JEM Economy's native storefront, category/search/quantity controls and price cells |
| `encounter`, `busy-arena` | Boss/Raid preparation, mode and group selection, occupied-arena state |
| `result`, `defeat` | Existing result table and reward controls |
| `races`, `race-progression` | Current race selection/progression screen and native detached-entity preview renderer |
| `empty-events` | Native empty-list presentation |
| `inventory` | Ordinary Minecraft inventory with the installed native mods' controls; normal native behavior |
| `reset` | Reset gallery data |

JEM actions acknowledge locally or open another presentation state. Their demo
balance, ready flags and reward selections exist only in memory. They do not send
gameplay packets, teleport, modify real inventories, create settlements, start
events, change the actual race or save player/world state. Location controls show
a destination inspection sheet; they do not replace Xaero's real map.

The separately installed native mods retain their normal behavior: Curios,
Backpacked, JEI, Better Advancements and FTB Quests,
with Framework, Balm, Architectury, FTB Library, FTB Teams and Patchouli. Use their
native inventory controls, key bindings, books and blocks. Existing Recruits,
Tom's Storage, Sophisticated Storage and Xaero mods remain in the sandbox. These
native mods are not claimed to be inert JEM mockups. Production quest progress and
test-world data are not copied into the sandbox.

The later 2026-10-26 request removed TrashSlot from the production pack and from
future sandbox installer inputs. A previously installed sandbox copy is historical
profile state and is not modified by this installer source change.

## Cinematic container interactions

`JEMCinematicInteractions` is a separate sandbox-only owner so the behavior does
not depend on the gallery's copied screens. In every recipe-aware container it
closes and removes the vanilla recipe-book control, leaving the installed JEI
ingredient list on the right and bookmarks on the left.

Hold Shift and the right mouse button, then sweep across item stacks. Every newly
crossed stack uses the active menu's native `QUICK_MOVE` action, so the gesture
works from block or machine to player inventory and from player inventory back to
the container. Vanilla and modded menus retain their own legal input, output and
filter rules.

## Source and installation

`Mods/JEMGuiSandbox` owns the gallery, local demo transports and build packaging.
`prepare.py` admits an explicit list of the current domain presentation sources
and immutable view records. It relocates classes and JEM resource namespaces,
preserves native PNG bytes and localization keys, and writes source SHA-256
provenance into the artifact. Domain source owners remain unchanged; generated
screen copies stay inside `build/` and are not separately maintained.

Race previews delegate only to `AbilityPreviewRenderer` from the existing
`jem-races-1.1.0-sandbox.jar`; the dependency is explicit. Selection/progression
requests from the copied screen terminate at the gallery's local transport. The
module is for this development profile and is not a shareable production mod.

Build only when authorized:

```powershell
cd Mods/JEMGuiSandbox
./gradlew.bat jar --offline -x test -x check
python install.py
```

The installer refuses a running `jem sandbox` or an AutoModpack-managed target,
resolves mandatory native dependencies from exact installed metadata, saves
replaced files under that profile's `visual-backups`, and copies only its explicit
artifact set. Its receipt records installed hashes. It does not launch the game.

Production `Client/ManagedPack`, local Server, `test jem`, stable profiles, native
AutoModpack publication and the release baseline are outside this installation.

## Visual evidence and limits

This request reuses existing layouts and original pixels; it commissions no new
GUI artwork. The active Taste Engine pair is
`Minecraft/GUI/Forge-1.20.1-jem-smp-screens`. The original settlement screenshot
`Docs/settlements/assets/reference/runtime-corrections-2026-10-19/01.png` was
inspected for its actual paper/wood/metal surfaces, resident list and native control
geometry. Its classification remains a correction reference, not approval of the
current render. The build preserves the owning sources rather than redrawing
their appearance or reviving rejected SMP art studies.

Visual-auditor result: actual runtime composition, responsive behavior, focus,
animations and full click coverage are **UNOBSERVED**. No game, gameplay test,
automated test, linter or static analyzer was run. Compilation and reobfuscation
establish artifact production only; manual acceptance remains outstanding.

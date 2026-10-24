# Russian client resource localization

The 2026-10-24 user request requires Russian interface text when Russian is selected in Minecraft, including the native Xaero map screens. The owning overrides are `Client/ManagedPack/kubejs/assets/<namespace>/lang/ru_ru.json`. They supplement the installed mods' Russian resources through KubeJS. They do not force the client's selected language or replace native screens, controls, map markers or saved personal map data

## Installed evidence and authored coverage

The installed archives were read directly, including declared nested JARs. The following table describes this task's seven resource namespaces, rather than claiming every string in every installed mod has been observed in game

| Namespace | Installed source | English keys | Native Russian keys | Override entries |
| --- | --- | ---: | ---: | ---: |
| `xaeroworldmap` | `xaeroworldmap-forge-1.20.1-1.46.0.jar` | 322 | 0 | 322 |
| `xaerominimap` | `xaerominimap-forge-1.20.1-26.4.2.jar` | 646 | 569 | 77 |
| `xaerolib` | Nested `xaerolib-forge-1.20.1-1.7.1.jar` and `1.7.3.jar` | 46 | 10 | 36 |
| `mapsyncer` | `mapsyncer-1.0.3-forge-1.20.1.jar` | 85 | 0 | 85 |
| `ftbteams` | `ftb-teams-forge-2001.3.2.jar` | 90 | 0 | 90 |
| `ftblibrary` | `ftb-library-forge-2001.2.13.jar` | 99 | 53 | 47 |
| `ftbquests` | `ftb-quests-forge-2001.4.22.jar` | 584 | 568 | 66 |

All English resource keys in these seven namespaces have a Russian value after merging native resources and these overrides. The 723 override entries include 27 deliberately unchanged brand names, technical unit symbols, X/Y coordinates and format-only templates. These include FTB Teams, FTB Quests, JEI, REI, FE/RF, EMC and `%1$s (%2$s)`. They are not untranslated instructions. Existing native Russian prose remains in its original owner

The concrete cause of the English Xaero World Map interface was the absence of a native `ru_ru.json` in the installed World Map JAR. Minimap and the nested XaeroLib had incomplete Russian resources. Minecraft therefore fell back to the English values for those missing keys even with Russian selected. The correction supplies the exact installed keys through the client resource owner, preserving the native map implementation

Translations cover map controls, waypoint menus, map selection and connections, dimension and cave controls, server-enforced profile settings, export and failure messages, MapSyncer command feedback, team management and newer FTB quest/library controls. The authored text preserves positional and ordinary printf arguments, literal percent escapes, section-sign formatting sequences and the original number of explicit line breaks

## Resource boundary

A resource translation applies only where the native producer requests a translation key. It cannot translate arbitrary `Component.literal` text, literal server messages, user-created names, chat, personal waypoint labels, quest prose stored separately in quest data, or a filename used as an identifier. Command syntax, registry IDs, profile IDs and configuration enum tokens retain their actual spelling. Language selection does not grant access to disabled server features or alter map privacy

MapSyncer's administrative interval messages receive both a native engine-unit argument and a seconds argument. Its configuration tooltip also explains that native setting's units. The resource owner preserves those argument meanings and cannot calculate a new formatted duration from them. Gameplay duration formatting and any producer-side literal correction belong to their corresponding source owner

These are authored resource and archive-inspection results. No game launch, visual acceptance, automated gameplay test, stable-profile change, MineRENT deployment or release-baseline promotion was performed for this localization work. Artifact synchronization and native AutoModpack publication are recorded by the enclosing task's delivery owner

## Complete installed-resource amendment

The completed resource pass changes 173 Russian overlay namespaces, adds 10245 keys and revises 0 existing values. [Coverage](coverage-20261024.json) records each exact source path, key counts and final hash. This includes native item/block/entity/effect names, current UI/settings text, tooltips and native guide/bestiary fragments beyond the seven map/FTB namespaces above

Authored Russian text supplements genuine missing prose. Exact complete-phrase reuse uses only an unambiguous Russian translation already present in the installed resource corpus. Artist/music credits, brand names, technical syntax, IDs, symbols and formatting-only entries are intentionally not replaced with fabricated Cyrillic. FancyToasts and PatPat use native lenient JSON syntax and already have Russian coverage, so a strict external JSON parser error was not treated as an in-game defect

A separate JEM title bug crossed the server/client boundary: `VanillaMenus` resolved Component titles with the server language before storing plain item metadata. `MenuTitles` now transports bounded serialized components and the client resolves them at rendering. Invalid new metadata displays an explicit localized error. Valid legacy literal metadata remains readable only when the new field is absent. Boss leaderboard and official landmark names use native/existing translation keys. Private creator names remain literal. Standard dimension names share one common localized helper, while unknown dimensions retain their actual IDs

Source review found and corrected the narrow Creative rewards fallback, its JEI reserved area and English day pluralization. Source builds and stopped-profile native application are recorded separately from player acceptance. No complete visual or runtime language guarantee is claimed from archive inspection alone

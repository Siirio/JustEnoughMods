# JustEnoughMods universal

Minecraft Java Edition 1.20.1, Forge 47.4.10, Java 17. The same JEM gameplay JARs support Windows, Linux and macOS, integrated singleplayer and a matching JEM dedicated server. This archive contains the complete client pack, including Bootstrap. Native game and graphics support still depends on the installed Java, launcher, drivers and third-party mods; Linux/macOS runtime acceptance has not been performed on the Windows development host.

## Fresh installation

1. Create a new, separate Minecraft 1.20.1 instance in a Forge-compatible launcher. Install Forge 47.4.10 and select Java 17 for that instance.
2. Close Minecraft. Extract the contents of `JustEnoughMods/` into that new instance's game directory. The `mods`, `config`, `defaultconfigs`, `kubejs`, `resourcepacks` and other pack folders belong directly in the game directory.
3. Start the instance through your launcher. Create a singleplayer world or join a server running the matching JEM modules. Select resource packs and compatible shaders using their ordinary native controls. Shaders are disabled by default; included shaderpacks do not all support macOS GPUs.

All gameplay content is installed locally. Singleplayer does not require a login service or an update server after installation. Multiplayer requires the selected game server to be reachable. The archive does not include the Minecraft/Forge runtime or a Java installation; the launcher installs these separately.

Use a short game-directory path when convenient. Bootstrap now uses short independent temporary filenames and Java NIO extended Windows paths; ordinary filesystem permissions, free space and maximum individual filenames still apply. Linux paths are case-sensitive: retain the archive's exact names. No PowerShell, batch file or Windows registry change is required to play the pack on Linux/macOS.

## Existing AutoModpack installation

Keep its native update flow. Do not extract this full archive over an active AutoModpack-managed profile or edit its active-state/cache files. When Bootstrap offers an ordinary install/update, review the listed files, check "Согласен с рисками" and select "Скачать". Changed server identity/certificate and actual download errors retain native error handling.

## World features and separate modules

Economy defaults to enabled. Set `[economy] enabled=false` in the world's `serverconfig/jem_economy-server.toml` to make currency dormant while retaining its records; Settlements then uses its own emerald treasury. Existing legacy merchants whose synthetic/native offers cannot be safely distinguished remain blocked while Economy is off, with an explanatory notice; their saved data is retained.

Events default to off. A dedicated host may explicitly set the top-level `enabled=true` in the world's `serverconfig/jem-events-server.toml`. Integrated/LAN has no SMP, parties or JEM Events, including Raid. Ordinary native boss combat and World Tier remain available. Dedicated Raid requires JEMServer; normal domain GUIs work independently. Claim personal item rewards through the normal E inventory panel; `/market` is Economy-owned. The pack supplies one shared x2 ordinary Minecraft XP baseline.

Professions, Villager Talking and Claims are development modules and are excluded. Currency Core is part of Economy; all production races are in one Races JAR. Pack Core and Menu Support are retired. Do not add their old JARs alongside this pack.

Individual JEM modules still require the exact foundations/native engines listed in `Docs/universal/README.md` in the source repository. "Universal" does not mean every Forge client can join an arbitrary non-JEM server. A dedicated server must use its server-side mod selection; the complete client `mods` folder includes client-only mods and must not be copied wholesale to a dedicated server.

`manifest.json` beside this guide records every included file's SHA-256 and size. Worlds, credentials, personal maps, launcher profiles and Java arguments are not included.

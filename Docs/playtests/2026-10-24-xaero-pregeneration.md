# Chunky to shared Xaero local trial - 2026-10-24

## Requested behavior and scope

The user requested a MineRENT Chunky radius of 20000 blocks around X -7228, Z -1380, followed by shared Xaero coverage of generated terrain. They selected a local `test jem` trial before deployment and explicitly chose to operate Minecraft themselves. MineRENT is not receiving this integration during the trial. Stable profiles, personal waypoints, homes, death points and the client release baseline are outside the trial.

## Candidate and ownership

Existing Unified Xaero Worldmap 2.0.1 remains the native explored-terrain owner with WorldMap 1.46.0. Its installed server API accepts and merges client-produced terrain and contains no chunk-to-map renderer. Official MapSyncer 1.0.3 for Forge 1.20.1 is an experimental converter alongside it, not an accepted production dependency or a replacement shared store. Its server-side map cache is temporary trial state. Native `null` is its expected Overworld Xaero directory name.

Candidate: `mapsyncer-1.0.3-forge-1.20.1.jar`, Modrinth project AW5A8Q2T, version 71emSyU6, SHA-512 `68b1ae0ee8640ab4ecffcdbd4edc53d96b362f6d44dfe9fb7704c04070a79b41b7ecbc3b6f74be6708671960df0ac4bcbd4ac84ffbe37585766f78e957ab216d`. Packaged metadata declares GPL-3.0 while the current Modrinth page says LGPL-3.0-only. No third-party source was copied or modified. This discrepancy must be resolved before a modified or redistributed production integration.

## Environment and observed blocker

The ordinary local Server runtime uses the separate save `Server/local/xaero-pregen-20261024/world` and loopback-only binding. Its original properties and artifact receipts remain privately under the same ignored directory. The original `Server/world` was not opened. MapSyncer currently writes its cache under `Server/server_map_cache`, independently of the world path, so this temporary cache must not become a release input.

A first region conversion returned success and wrote a format 6.8 `region.xaero`, but reported `NoSuchFieldException: AIR` in `PlaceholderBlockGetterFactory.initializeClasses`. Installed bytecode looks up Mojmap field/method strings reflectively inside an SRG production runtime. A successful conversion message therefore does not establish correct block interpretation or visible rendering. This remains a separate candidate limitation.

During remote pregeneration, the local server watchdog terminated the runtime. Evidence: ignored `Server/crash-reports/crash-2026-10-24_14.06.21-server.txt`. The causal chain is chunk load, BloodMoonLoot.scrubInvalid, unconditional container slot reads, pending structure-chest loot unpacking, and expensive native/modded loot generation on the server thread. The cleanup owner had no invalidation-history or pending-loot guard. Codex corrected that owner without changing native loot, the watchdog or event-item provenance. Existing materialized invalidated items retain their original deletion rules. Player acceptance remains in testing.

The production `:JEMServer:jar` build succeeded with no test/formatter/analyzer tasks. Exact fresh JAR SHA-256: `f7b4a19e91fc41c79fb52ffc89444f1cf100903cc77d1d4575fbc44a3b39045c`. The same artifact was copied to local Server and ManagedPack. Atlas record: `blood-moon.materialized-loot-only`.

## Native delivery

Generation 69 introduced only MapSyncer and the manifest and was fully applied through native AutoModpack. Generation 70 updates only JEMServer and the manifest, token `830fe0cbf7d2a018a9b34dca928546b50257d2e5`. Final client installation and runtime readiness are recorded below after observation. No raw managed files, native state pointers or trust policies were edited.

## Manual acceptance

Use only `test jem` and the local loopback server. The remote test area is centered at X -7228, Z -1380 with radius 128 blocks. Do not travel there before inspecting the map: the acceptance criterion is terrain received from generated chunks without player exploration. The same candidate must preserve already explored terrain, receive later changes, retain Unified catch-up and avoid sharing personal markers. No client rendering or multi-player acceptance is inferred from server logs or ZIP creation.

## Release boundary and cleanup

This is an active local trial, not a MineRENT release. Before any release, stop the local trial cleanly, restore the original server properties, remove experimental MapSyncer from Server and ManagedPack unless separately approved for production, move the trial map cache under ignored local evidence, then stage/preview/publish and apply the resulting native generation. Keep the source-built BloodMoonLoot fix unless explicitly reverted. Do not use rollback to generation 68 as a substitute for preparing current fixed artifacts. The isolated save and private restoration material must not be uploaded or committed.

## Observed handoff

Generation 70 is fully installed in `test jem`: active token equals the local published head `830fe0cbf7d2a018a9b34dca928546b50257d2e5`, both MapSyncer and JEMServer SHA-256 match Server and ManagedPack, and no native transaction remains pending. The two existing loopback terrain-map folders were privately backed up before client review. Personal waypoint files were not changed by preparation.

After the BloodMoonLoot fix, Chunky processed 64 remote chunks before the local sample was deliberately paused. A conversion started immediately after pause hit another watchdog in MapSyncer's forced `saveAllChunks`, waiting for outstanding generation futures. Evidence: `crash-2026-10-24_14.26.35-server.txt`. A paused Chunky task is not proof that all pending generation work is drained. This is a separate lifecycle limitation and the watchdog was retained.

A subsequent clean runtime start left Chunky paused. Converting the already saved Overworld completed six regions with six updated, two empty MCA files skipped and zero conversion failures. The remote cache regions `-15_-3.zip` and `-15_-4.zip` are present alongside four spawn-region files. The separate AIR reflection error remains recorded and prevents a claim that visual fidelity or all-block semantics passed.

The user was given `127.0.0.1:31604`, `/mapsyncer sync overworld`, and native map key M. Acceptance requires seeing remote generated terrain before visiting it, then confirming exploration/Unified coexistence. Automatic join synchronization and multi-player recovery remain unverified. No automatic conversion was scheduled while its force-flush behavior remains unaccepted. The local server is retained for the user's manual check, with this isolated save and experimental generation active. MineRENT is unchanged by the integration trial.

The user postponed the manual client check. Preparation is complete, while map rendering, automatic distribution and multi-player acceptance remain pending.

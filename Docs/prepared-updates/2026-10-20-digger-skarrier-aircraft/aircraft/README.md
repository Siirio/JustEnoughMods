# Create Immersive Aircraft: prepared native resource-pack activation

Prepared on 2026-10-20. The user subsequently authorized local Server/Client application; current delivery state is in the parent delivery.json. The user requested automatic installation and activation for players receiving the server pack, then explicitly required preparation without updating Server or Client.

## Future managed changes

Paths below are relative to `Client/ManagedPack`. The `files/` tree is a future overlay, not a second active distribution owner.

| Operation | Path |
| --- | --- |
| Add | `config/paxi/resourcepacks/Create Immersive Aircrafts Resource Pack 1.20.1 - 3.0.zip` |
| Replace | `config/paxi/resourcepack_load_order.json` |
| Delete | `resourcepacks/Create Immersive Aircrafts Resource Pack 1.20.1 - 3.0.zip` |

The ZIP moves unchanged from the ordinary optional resource-pack folder into Paxi's required source. Delete only the exact old managed path, through normal AutoModpack ownership; do not delete unrelated user resource packs. The current Paxi load order is empty. If another change adds Paxi packs before deployment, merge this one exact filename into that newer order rather than overwrite the newer configuration.

## Native mechanism and activation

Evidence is the exact installed `Paxi-1.20-Forge-4.0.jar`, inspected with `javap -c -p` (class/member inspection, no runtime launch). No custom Java, resource-pack manager, bootstrap patch, or `options.txt` mutation is required.

* `PaxiForge` constructs `RESOURCE_PACK_DIRECTORY` from Forge's config directory plus `paxi/resourcepacks`, and registers `PaxiRepositorySource` through `AddPackFindersEvent` for `CLIENT_RESOURCES`.
* `PaxiRepositorySource.m_7686_` loads ZIPs or pack folders and calls `Pack.m_245429_` with the filename as ID, the boolean `true` for required, and `Pack.Position.TOP`. Its ordering reader uses the JSON field `loadOrder`, with exact filenames including `.zip`.
* `MixinPackRepositoryForge.paxi_buildEnabledProfilesForge` starts with the player's selected resource-pack stream, removes only explicitly ordered Paxi entries from that list, and re-inserts required Paxi packs using their declared top position. It then includes any other required packs. Unrelated selected packs remain selected and retain their relative order.
* `PaxiPackSource.PACK_SOURCE_PAXI` also passes `true` to `PackSource`'s auto-add constructor.

Thus a fresh profile needs no existing selected-pack entry: once the required native AutoModpack generation is committed and Minecraft starts with Paxi and the ZIP in its config tree, repository discovery adds the pack and selection rebuilding requires it. An existing client follows the same route after native application and restart. The managed projection no longer contains the old ordinary ZIP; the required Paxi ID is the plain ZIP basename. A historical unmanaged copy may remain in an existing player gameDir and stay selected under its old file ID. Native ownership preserves that copy; the identical required Paxi pack still supplies the intended top-priority appearance. The installer template currently lists the old ordinary file ID, but this is not needed for activation and this preparation does not change the template or anyone's options.

Paxi places this required pack at top priority during its selection rebuild. Only its provided `immersive_aircraft` resources are overridden; personal packs are not removed or reordered relative to each other. Another pack overriding the same resource at a higher effective position could still affect appearance. Any later competing required pack must be reviewed in Paxi's shared load order.

AutoModpack currently publishes `config` in its required `main` group; `config/paxi/**` is neither excluded nor editable. The native generation therefore owns this ZIP and the shared ordering configuration. `options.txt` is excluded from distribution. There is no separate server resource-pack URL, download prompt, or download-on-join mechanism added here.

**Downloading alone is insufficient:** activation depends on completed native application and the subsequent client start/resource repository load. No runtime activation has been exercised during this preparation. Future deployment must follow the normal native preview/publication/application gates; do not raw-copy files into an active AutoModpack game directory or mutate active-state pointers.

## Dependencies and scope

Already installed: `Paxi-1.20-Forge-4.0.jar`, `YungsApi-1.20-Forge-4.0.6.jar`, `immersive_aircraft-1.5.0+1.20.1-forge.jar`, and `create-1.20.1-6.0.8.jar`. Paxi metadata requires Minecraft `[1.20,1.21)`, Forge `[46.0.2,)`, and YUNG's API `[1.20-Forge-4.0.1,)`. No new mod dependency is introduced. The corresponding existing `config/paxi/datapacks/Create Immersive Aircrafts Datapack 1.20.1 - 3.0.zip` remains untouched.

The resource pack declares `pack_format: 15` and description `Texture Changes for Create Immersive Aircraft`; its assets are under `assets/immersive_aircraft/`. It changes textures, item colors and English translations, not registry or recipe ownership.

## Asset provenance and rights

The prepared ZIP is an unmodified byte-for-byte copy of the already distributed local asset at `Client/ManagedPack/resourcepacks/Create Immersive Aircrafts Resource Pack 1.20.1 - 3.0.zip`; no external asset was downloaded or reauthored. Size: 190791 bytes. SHA-256: `6474ca9582bccb35c96851f5c8f51657fe0d09b139004144c30cad88264d2fea`.

The archive contains no LICENSE, README, or credits file. Preserve the original filename, archive bytes, and any upstream rights; this preparation does not grant a new license or claim authorship. Its original upstream licensing metadata is not recorded in the archive and remains unknown. Paxi's own JAR declares LGPLv3; that license does not license this unrelated resource pack.

Installed Paxi SHA-256: `dd18dd9696d099137b170076676e8411e3d8385af15941f68472f21167266f1a`. Installed YUNG's API SHA-256: `101c30d01d0cf9d86190f578eb1e43bd06b64092e95662a00ef52946f472c7b1`.

No Client/ManagedPack, Server, launcher profile, publication, release baseline, or personal options file was changed by this preparation. No build, automated test, game launch, or runtime acceptance was performed.

## Subsequent local authorization

The user superseded the preparation-only boundary. The exact overlay has now been applied to canonical ManagedPack and ordinary local Server; optional duplicate ZIP removed. Native publication/application are tracked by the parent delivery.json. The original preparation evidence above remains provenance, not a claim that deployment is still forbidden.

Final local application: generation47 is installed in test jem with token85e8b2a533d66a90721f1d3d97d04d740185b58a and no pending transaction. A historical unmanaged ordinary ZIP remains in this profile; it was preserved rather than deleted outside native ownership. New clean installations receive the required Paxi copy.

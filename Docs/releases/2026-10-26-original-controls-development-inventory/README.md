# Original keybinds and development inventory

Captured 2026-10-26T17:05:22+05:00 in Asia/Yekaterinburg from the original `JustEnoughMods` saved controls and the current development files.

The original AutoModpack active generation still contains Backpacked 3.0.9. Preserve it and all its saved keybinds until the MineRENT update. Future removal belongs to the active post-release preset. Keep this historical snapshot unchanged.

This is a metadata and controls snapshot. Existing source edits, runtime installation, original profile, release baseline and hosted server are outside this commit.

Saved 250 keybind rows exactly, including unassigned and retained legacy mappings. [Raw keybinds](original-keybinds.txt), [structured keybinds](original-keybinds.json), [extra keybind configuration](original-keybind-config/config), [original resource-pack order](original-resource-pack-order.json). These are saved disk settings, with the last-save timestamp in [snapshot metadata](snapshot.json).

Full names, versions, evidence paths and SHA-256 identities are in [mod inventory](mods.csv), [pack inventory](packs.csv) and [structured inventory](inventory.json). Embedded JARs and multiple mod IDs in one JAR are identified explicitly. An embedded `rubidium` compatibility ID does not mean standalone Rubidium was installed.

A pack version comes from declared metadata or an explicitly labelled filename. "not declared" means the author supplied no version that can be established here. `pack_format` is kept separately as Minecraft compatibility metadata. Folder and KubeJS content is identified by a deterministic content SHA-256. Presence in the inventory does not establish enabled state. Original enabled pack order is saved separately.

## Original backpack controls

| Action | Saved binding |
| --- | --- |
| `key_key.backpack` | `key.keyboard.unknown` |
| `key_key.backpacked.open_management` | `key.keyboard.right.bracket` |

## Development mods

| Mod name | Mod version | Mod ID | File |
| --- | --- | --- | --- |
| Alex's Caves | 2.0.2 | `alexscaves` | mods/alexscaves-2.0.2.jar |
| Alex's Mobs | 1.22.9 | `alexsmobs` | mods/alexsmobs-1.22.9.jar |
| Almanac | 1.0.2 | `almanac` | mods/almanac-1.20.x-forge-1.0.2.jar |
| Animal Feeding Trough | 1.1.0+1.20.1-forge | `animal_feeding_trough` | mods/animal_feeding_trough-1.1.0+1.20.1-forge.jar |
| AppleSkin | 2.5.1+mc1.20.1 | `appleskin` | mods/appleskin-forge-mc1.20.1-2.5.1.jar |
| Aquamirae | 7.1.13 | `aquamirae` | mods/aquamirae-forge-1.20.1-7.1.13.jar |
| Architectury | 9.2.14 | `architectury` | mods/architectury-9.2.14-forge.jar |
| BadOptimizations | 2.4.1 | `badoptimizations` | mods/BadOptimizations-2.4.1-1.20.1.jar |
| MixinExtras | 0.3.2 | `mixinextras` | mods/BadOptimizations-2.4.1-1.20.1.jar!/META-INF/jars/forge-mixinextras-forge-0.3.2.jar |
| MixinExtras-0.3.2 | 0.3.2 | `` | mods/BadOptimizations-2.4.1-1.20.1.jar!/META-INF/jars/forge-mixinextras-forge-0.3.2.jar!/META-INF/jars/MixinExtras-0.3.2.jar |
| BaguetteLib | 2.0.5 | `baguettelib` | mods/baguettelib-1.20.1-Forge-2.0.5.jar |
| Balm | 7.3.42 | `balm` | mods/balm-forge-1.20.1-7.3.42.jar |
| KumaAPI | 20.1.12 | `kuma_api` | mods/balm-forge-1.20.1-7.3.42.jar!/META-INF/jarjar/kuma-api-forge-20.1.12+1.20.1.jar |
| Barnyard Buddies | 0.6..4 | `barnyardbuddies` | mods/BarnyardBuddies_1.20.1Forge_V0.6.4.jar |
| Building But Better | 2.0pre4 | `bbb` | mods/bbb-forge-2.0pre4.jar |
| MidnightLib | 1.4.2 | `midnightlib` | mods/bbb-forge-2.0pre4.jar!/META-INF/jars/midnightlib-1.4.2-forge.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/bbb-forge-2.0pre4.jar!/META-INF/jars/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/bbb-forge-2.0pre4.jar!/META-INF/jars/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| Create: Bells & Whistles | 0.4.3-1.20.x | `bellsandwhistles` | mods/bellsandwhistles-0.4.5-1.20.x-Create6.0+.jar |
| Better Advancements | 0.6.0.73 | `betteradvancements` | mods/BetterAdvancements-Forge-1.20.1-0.6.0.73.jar |
| BetterF3 | 7.0.2 | `betterf3` | mods/BetterF3-7.0.2-Forge-1.20.1.jar |
| Better village | 3.3.1 | `bettervillage` | mods/bettervillage-forge-1.20.1-3.3.1-all.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/bettervillage-forge-1.20.1-3.3.1-all.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/bettervillage-forge-1.20.1-3.3.1-all.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| Blast From The Past | 1.0.3 | `blastfromthepast` | mods/blastfromthepast-1.0.3+1.20.1.jar |
| Bosses'Rise | 2.1.2 | `block_factorys_bosses` | mods/block_factorys_bosses-2.1.2-forge-1.20.1.jar |
| Blueprint | 7.1.5 | `blueprint` | mods/blueprint-1.20.1-7.1.5.jar |
| CameraOverhaul | 2.1.1-forge+mc.1.20-1.20.5 | `cameraoverhaul` | mods/CameraOverhaul-v2.1.1-forge%2Bmc%5B1.20-1.20.5%5D.jar |
| Casual Swing | 0.1.0-1.20.1-Forge | `casualswing` | mods/casualswing-0.1.0-1.20.1-Forge.jar |
| Flywheel | 1.0.6-beta-266 | `flywheel` | mods/casualswing-0.1.0-1.20.1-Forge.jar!/META-INF/jarjar/flywheel-forge-1.20.1-1.0.6-beta-266.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/casualswing-0.1.0-1.20.1-Forge.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/casualswing-0.1.0-1.20.1-Forge.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| Ponder | 1.0.91 | `ponder` | mods/casualswing-0.1.0-1.20.1-Forge.jar!/META-INF/jarjar/Ponder-Forge-1.20.1-1.0.91.jar |
| Chat Heads | 0.15.7 | `chat_heads` | mods/chat_heads-0.15.7-forge-1.20.jar |
| mixinextras-forge-0.5.0 | 0.5.0 | `` | mods/chat_heads-0.15.7-forge-1.20.jar!/META-INF/jars/mixinextras-forge-0.5.0.jar |
| MixinExtras-0.5.0 | 0.5.0 | `` | mods/chat_heads-0.15.7-forge-1.20.jar!/META-INF/jars/mixinextras-forge-0.5.0.jar!/META-INF/jars/MixinExtras-0.5.0.jar |
| ChatAnimation | 1.3.0 | `chatanimation` | mods/chatanimation-forge-1.3.0%2Bmc1.20.1.jar |
| mixinextras-forge-0.5.4 | 0.5.4 | `` | mods/chatanimation-forge-1.3.0%2Bmc1.20.1.jar!/META-INF/jars/mixinextras-forge-0.5.4.jar |
| MixinExtras-0.5.4 | 0.5.4 | `` | mods/chatanimation-forge-1.3.0%2Bmc1.20.1.jar!/META-INF/jars/mixinextras-forge-0.5.4.jar!/META-INF/jars/MixinExtras-0.5.4.jar |
| Cherished Worlds | 6.1.7+1.20.1 | `cherishedworlds` | mods/cherishedworlds-forge-6.1.7+1.20.1.jar |
| Chimes | 2.1.1 | `chimes` | mods/Chimes-v2.1.1-1.20.1-Forge.jar |
| Chunky | 1.3.146 | `chunky` | mods/Chunky-1.3.146.jar |
| Cinematic Cataclysm | 1.0.1 | `cinematiccataclysm` | mods/cinematiccataclysm-1.0.1-1.20.1.jar |
| mixinextras-forge-0.5.0 | 0.5.0 | `` | mods/cinematiccataclysm-1.0.1-1.20.1.jar!/META-INF/jarjar/mixinextras-forge-0.5.0.jar |
| MixinExtras-0.5.0 | 0.5.0 | `` | mods/cinematiccataclysm-1.0.1-1.20.1.jar!/META-INF/jarjar/mixinextras-forge-0.5.0.jar!/META-INF/jars/MixinExtras-0.5.0.jar |
| Citadel | 2.6.3 | `citadel` | mods/citadel-2.6.3-1.20.1.jar |
| clickadv mod | 1.20.1-3.9 | `clickadv` | mods/clickadv-1.20.1-3.9.jar |
| Cloth Config v10 API | 11.1.136 | `cloth_config` | mods/cloth-config-11.1.136-forge.jar |
| Clumps | 12.0.0.4 | `clumps` | mods/Clumps-forge-1.20.1-12.0.0.4.jar |
| Companions! | 1.3.3 | `companions` | mods/companions-forge-1.20.1-1.3.3.jar |
| Reconnectible Chains | 2.2.5 | `connectiblechains` | mods/connectiblechains-forge-1.20.1-2.2.5.jar |
| MixinExtras | 0.3.5 | `mixinextras` | mods/connectiblechains-forge-1.20.1-2.2.5.jar!/META-INF/jarjar/mixinextras-forge-0.3.5.jar |
| MixinExtras-0.3.5 | 0.3.5 | `` | mods/connectiblechains-forge-1.20.1-2.2.5.jar!/META-INF/jarjar/mixinextras-forge-0.3.5.jar!/META-INF/jars/MixinExtras-0.3.5.jar |
| Connectivity Mod | 1.20.1-7.6 | `connectivity` | mods/connectivity-1.20.1-7.6.jar |
| Controlling | 12.0.2 | `controlling` | mods/Controlling-forge-1.20.1-12.0.2.jar |
| Cool Rain | 1.0.3 | `coolrain` | mods/coolrain-1.20.1-Forge-1.0.3.jar |
| Copper-Age-Backport | 0.1.5 | `copperagebackport` | mods/copperagebackport-forge-1.20.1-0.1.5.jar |
| CoroUtil | 1.20.1-1.3.7 | `coroutil` | mods/coroutil-forge-1.20.1-1.3.7.jar |
| Create | 6.0.8 | `create` | mods/create-1.20.1-6.0.8.jar |
| Registrate-MC1.20-1.3.3 | MC1.20-1.3.3 | `` | mods/create-1.20.1-6.0.8.jar!/META-INF/jarjar/Registrate-MC1.20-1.3.3.jar |
| Flywheel | 1.0.5 | `flywheel` | mods/create-1.20.1-6.0.8.jar!/META-INF/jarjar/flywheel-forge-1.20.1-1.0.5.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/create-1.20.1-6.0.8.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/create-1.20.1-6.0.8.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| Ponder | 1.0.91 | `ponder` | mods/create-1.20.1-6.0.8.jar!/META-INF/jarjar/Ponder-Forge-1.20.1-1.0.91.jar |
| Create Train Parts | 0.5.1 | `create_train_parts` | mods/create_train_parts-0.5.1-1.20.1-6.0.8-291.jar |
| Create | 6.0.8-291 | `create` | mods/create_train_parts-0.5.1-1.20.1-6.0.8-291.jar!/META-INF/jarjar/create-1.20.1-6.0.8-291-slim.jar |
| Registrate-MC1.20-1.3.3 | MC1.20-1.3.3 | `` | mods/create_train_parts-0.5.1-1.20.1-6.0.8-291.jar!/META-INF/jarjar/Registrate-MC1.20-1.3.3.jar |
| Flywheel | 1.0.6-beta-266 | `flywheel` | mods/create_train_parts-0.5.1-1.20.1-6.0.8-291.jar!/META-INF/jarjar/flywheel-forge-1.20.1-1.0.6-beta-266.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/create_train_parts-0.5.1-1.20.1-6.0.8-291.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/create_train_parts-0.5.1-1.20.1-6.0.8-291.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| Ponder | 1.0.91 | `ponder` | mods/create_train_parts-0.5.1-1.20.1-6.0.8-291.jar!/META-INF/jarjar/Ponder-Forge-1.20.1-1.0.91.jar |
| CreateBetterFps | 1.1.2 | `createbetterfps` | mods/createbetterfps-1.20.1-1.1.2.jar |
| Create Railways Navigator | 1.20.1-beta-0.9.1-C6 | `createrailwaysnavigator` | mods/createrailwaysnavigator-forge-1.20.1-beta-0.9.1-C6.jar |
| CreativeCore | 2.12.40 | `creativecore` | mods/CreativeCore_FORGE_v2.12.40_mc1.20.1.jar |
| Creeper Overhaul | 3.0.2 | `creeperoverhaul` | mods/creeperoverhaul-3.0.2-forge.jar |
| Resourceful Lib | 2.1.20 | `resourcefullib` | mods/creeperoverhaul-3.0.2-forge.jar!/META-INF/jars/resourcefullib-forge-1.20.1-2.1.20.jar |
| yabn | 1.0.3 | `com_teamresourceful_yabn` | mods/creeperoverhaul-3.0.2-forge.jar!/META-INF/jars/resourcefullib-forge-1.20.1-2.1.20.jar!/META-INF/jars/yabn-1.0.3.jar |
| bytecodecs | 1.0.2 | `com_teamresourceful_bytecodecs` | mods/creeperoverhaul-3.0.2-forge.jar!/META-INF/jars/resourcefullib-forge-1.20.1-2.1.20.jar!/META-INF/jars/bytecodecs-1.0.2.jar |
| Critters n' Crawlers | 2.2.5 | `cnc` | mods/Critters n' Crawlers-2.2.5-mc1.20.1.jar |
| Cupboard utilities | 1.20.1-4.1 | `cupboard` | mods/cupboard-1.20.1-4.1.jar |
| Curios API | 5.14.1+1.20.1 | `curios` | mods/curios-forge-5.14.1+1.20.1.jar |
| Cut Through | 8.0.2 | `cutthrough` | mods/CutThrough-v8.0.2-1.20.1-Forge.jar |
| Deep Dark: Regrowth | 1.2.6.1 | `deep_dark_regrowth` | mods/Deep Dark Regrowth 1.2.6.1 - 1.20.1.jar |
| Deltabox Lib | 2.2.0 | `deltaboxlib` | mods/deltaboxlib-forge-2.2.0+1.20.1.jar |
| 细节盔甲条：重装 | 2.7.0+1.20.1-forge | `detailab` | mods/Detail Armor Bar Re-equipment-2.7.0+1.20.1-forge.jar |
| Diagonal Fences | 8.1.5 | `diagonalfences` | mods/DiagonalFences-v8.1.5-1.20.1-Forge.jar |
| Diagonal Blocks | 8.0.6 | `diagonalblocks` | mods/DiagonalFences-v8.1.5-1.20.1-Forge.jar!/META-INF/jarjar/diagonalblocks-forge-8.0.6.jar |
| Diagonal Walls | 8.0.4 | `diagonalwalls` | mods/DiagonalWalls-v8.0.4-1.20.1-Forge.jar |
| Diagonal Blocks | 8.0.6 | `diagonalblocks` | mods/DiagonalWalls-v8.0.4-1.20.1-Forge.jar!/META-INF/jarjar/diagonalblocks-forge-8.0.6.jar |
| Draggable Lists | 1.0.1 | `draggable_lists` | mods/draggable_lists-mc1.20-1.0.1-rev.824b2f3.jar |
| DragonLib | 1.20.1-beta-3.0.28 | `dragonlib` | mods/dragonlib-forge-1.20.1-beta-3.0.28.jar |
| Architectury | 9.1.12 | `architectury` | mods/dragonlib-forge-1.20.1-beta-3.0.28.jar!/META-INF/jars/architectury-forge-9.1.12.jar |
| mixinextras-forge-0.5.3 | 0.5.3 | `` | mods/dragonlib-forge-1.20.1-beta-3.0.28.jar!/META-INF/jars/mixinextras-forge-0.5.3.jar |
| MixinExtras-0.5.3 | 0.5.3 | `` | mods/dragonlib-forge-1.20.1-beta-3.0.28.jar!/META-INF/jars/mixinextras-forge-0.5.3.jar!/META-INF/jars/MixinExtras-0.5.3.jar |
| MmmMmmMmmMmm | 1.20-2.0.11 | `dummmmmmy` | mods/dummmmmmy-1.20-2.0.12-forge.jar |
| mixinextras-forge-0.5.0 | 0.5.0 | `` | mods/dummmmmmy-1.20-2.0.12-forge.jar!/META-INF/jars/mixinextras-forge-0.5.0.jar |
| MixinExtras-0.5.0 | 0.5.0 | `` | mods/dummmmmmy-1.20-2.0.12-forge.jar!/META-INF/jars/mixinextras-forge-0.5.0.jar!/META-INF/jars/MixinExtras-0.5.0.jar |
| Dusty Decorations | 1.13 | `dustydecorations` | mods/DustyDecorations_1.20.1Forge_V1.13.jar |
| Easy Anvils | 8.0.2 | `easyanvils` | mods/EasyAnvils-v8.0.2-1.20.1-Forge.jar |
| Easy Magic | 8.0.1 | `easymagic` | mods/EasyMagic-v8.0.1-1.20.1-Forge.jar |
| Epic Fight & Iron's Spellbook animation compat | 2.6.0 | `efiscompat` | mods/efiscompat-2.6.0.jar |
| Epic Fight Skill - Iron's Spells 'n Spellbooks | 1.0.3 | `efs_iss` | mods/efs_iss-1.0.3.jar |
| Elysium | 1.1.3 | `elysium_api` | mods/ElysiumAPI-1.20.1-1.1.3.jar |
| Embeddium | 0.3.31+mc1.20.1 | `embeddium` | mods/embeddium-0.3.31+mc1.20.1.jar |
| Rubidium (Embeddium) | 0.7.1 | `rubidium` | mods/embeddium-0.3.31+mc1.20.1.jar |
| MixinExtras | 0.3.5 | `mixinextras` | mods/embeddium-0.3.31+mc1.20.1.jar!/META-INF/jarjar/mixinextras-forge-0.3.5.jar |
| MixinExtras-0.3.5 | 0.3.5 | `` | mods/embeddium-0.3.31+mc1.20.1.jar!/META-INF/jarjar/mixinextras-forge-0.3.5.jar!/META-INF/jars/MixinExtras-0.3.5.jar |
| Enderman Overhaul | 1.0.4 | `endermanoverhaul` | mods/endermanoverhaul-forge-1.20.1-1.0.4.jar |
| End Remastered | 5.3.3-R-1.20.1 | `endrem` | mods/endrem_forge-5.3.3-R-1.20.1.jar |
| Enhanced AI | 3.3.7.3 | `enhancedai` | mods/enhancedai-3.3.7.3.jar |
| MixinExtras | 0.5.0-beta.5 | `mixinextras` | mods/enhancedai-3.3.7.3.jar!/META-INF/jarjar/mixinextras-forge-0.5.0-beta.5.jar |
| MixinExtras-0.5.0-beta.5 | 0.5.0-beta.5 | `` | mods/enhancedai-3.3.7.3.jar!/META-INF/jarjar/mixinextras-forge-0.5.0-beta.5.jar!/META-INF/jars/MixinExtras-0.5.0-beta.5.jar |
| Entity Model Features | 3.2.4 | `entity_model_features` | mods/entity_model_features-3.2.4-1.20.1-forge.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/entity_model_features-3.2.4-1.20.1-forge.jar!/META-INF/jars/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/entity_model_features-3.2.4-1.20.1-forge.jar!/META-INF/jars/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| Entity Texture Features | 7.1 | `entity_texture_features` | mods/entity_texture_features_1.20.1-forge-7.1.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/entity_texture_features_1.20.1-forge-7.1.jar!/META-INF/jars/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/entity_texture_features_1.20.1-forge-7.1.jar!/META-INF/jars/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| Epic Fight | 20.14.17 | `epicfight` | mods/epic-fight-20.14.17-mc1.20.1-forge.jar |
| Epic Fight - Invincible | 20.14.8.2 | `invincible` | mods/epic-fight-invincible-lib-20.14.8.2-mc1.20.1-forge.jar |
| Epic fight - Avalon | 20.12.6.4 | `epic_fight_avalon` | mods/epic_fight_avalon-20.12.6.4.jar |
| EpicFight Nightfall | 3.4.1 | `efn` | mods/EpicFight%20Nightfall-3.4.1-all.jar |
| EpicArcLib | 1.0.0 | `epicarclib` | mods/EpicFight%20Nightfall-3.4.1-all.jar!/META-INF/jarjar/epicarclib-1.0.0.jar |
| GeckoLib 4 | 4.8.2 | `geckolib` | mods/EpicFight%20Nightfall-3.4.1-all.jar!/META-INF/jarjar/geckolib-388172-7025129.jar |
| mclib-20 | 20 | `` | mods/EpicFight%20Nightfall-3.4.1-all.jar!/META-INF/jarjar/geckolib-388172-7025129.jar!/META-INF/jarjar/mclib-20.jar |
| VIX | 1.0.9 | `vix` | mods/EpicFight%20Nightfall-3.4.1-all.jar!/META-INF/jarjar/guhaos-vix-1531111-8258354.jar |
| MixinSquared | 0.2.0-beta.6 | `mixinsquared` | mods/EpicFight%20Nightfall-3.4.1-all.jar!/META-INF/jarjar/guhaos-vix-1531111-8258354.jar!/META-INF/jarjar/mixinsquared-forge-0.2.0-beta.6.jar |
| MixinSquared-0.2.0-beta.6 | 0.2.0-beta.6 | `` | mods/EpicFight%20Nightfall-3.4.1-all.jar!/META-INF/jarjar/guhaos-vix-1531111-8258354.jar!/META-INF/jarjar/mixinsquared-forge-0.2.0-beta.6.jar!/META-INF/jars/MixinSquared-0.2.0-beta.6.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/EpicFight%20Nightfall-3.4.1-all.jar!/META-INF/jarjar/guhaos-vix-1531111-8258354.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/EpicFight%20Nightfall-3.4.1-all.jar!/META-INF/jarjar/guhaos-vix-1531111-8258354.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| MixinSquared | 0.2.0-beta.6 | `mixinsquared` | mods/EpicFight%20Nightfall-3.4.1-all.jar!/META-INF/jarjar/mixinsquared-forge-0.2.0-beta.6.jar |
| MixinSquared-0.2.0-beta.6 | 0.2.0-beta.6 | `` | mods/EpicFight%20Nightfall-3.4.1-all.jar!/META-INF/jarjar/mixinsquared-forge-0.2.0-beta.6.jar!/META-INF/jars/MixinSquared-0.2.0-beta.6.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/EpicFight%20Nightfall-3.4.1-all.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/EpicFight%20Nightfall-3.4.1-all.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| Epic Fight Patch | 1.0.0 | `epicfight_cataclysm` | mods/EpicFight_X_L_Enders_Cataclysm-1.0.jar |
| Exposure | 1.9.21 | `exposure` | mods/exposure-forge-1.20.1-1.9.21.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/exposure-forge-1.20.1-1.9.21.jar!/META-INF/jars/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/exposure-forge-1.20.1-1.9.21.jar!/META-INF/jars/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| Fairy Lights | 7.0.0 | `fairylights` | mods/fairylights-7.0.0-1.20.1.jar |
| FancyMenu | 3.9.12 | `fancymenu` | mods/fancymenu_forge_3.9.12_MC_1.20.1.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/fancymenu_forge_3.9.12_MC_1.20.1.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/fancymenu_forge_3.9.12_MC_1.20.1.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| japng-0.5.3 | 0.5.3 | `` | mods/fancymenu_forge_3.9.12_MC_1.20.1.jar!/META-INF/jarjar/japng-0.5.3.jar |
| animated-gif-lib-for-java-animated-gif-lib-1.7 | animated-gif-lib-1.7 | `` | mods/fancymenu_forge_3.9.12_MC_1.20.1.jar!/META-INF/jarjar/animated-gif-lib-for-java-animated-gif-lib-1.7.jar |
| FancyToasts | 1.5.1 | `fancytoasts` | mods/fancytoasts-1.5.1-forge-1.20.1.jar |
| Faster Ladder Climbing | 1.1.0 | `fasterladderclimbing` | mods/fasterladderclimbing-forge-1.20.1-1.1.0-1.20.1.jar |
| FastFurnace | 8.0.2 | `fastfurnace` | mods/FastFurnace-1.20.1-8.0.2.jar |
| FastQuit | 3.0.2+1.20.1forge | `fastquit` | mods/Fastquit-3.0.2%2B1.20.1forge.jar |
| MixinExtras | 0.3.6 | `mixinextras` | mods/Fastquit-3.0.2%2B1.20.1forge.jar!/META-INF/jarjar/mixinextras-forge-0.3.6.jar |
| MixinExtras-0.3.6 | 0.3.6 | `` | mods/Fastquit-3.0.2%2B1.20.1forge.jar!/META-INF/jarjar/mixinextras-forge-0.3.6.jar!/META-INF/jars/MixinExtras-0.3.6.jar |
| Fast Suite | 5.1.2 | `fastsuite` | mods/FastSuite-1.20.1-5.1.2.jar |
| Fast Workbench | 8.0.4 | `fastbench` | mods/FastWorkbench-1.20.1-8.0.4.jar |
| FDLib | 1.0.9 | `fdlib` | mods/fdlib-1.0.9-1.20.1.jar |
| mixinextras-forge-0.5.0 | 0.5.0 | `` | mods/fdlib-1.0.9-1.20.1.jar!/META-INF/jarjar/mixinextras-forge-0.5.0.jar |
| MixinExtras-0.5.0 | 0.5.0 | `` | mods/fdlib-1.0.9-1.20.1.jar!/META-INF/jarjar/mixinextras-forge-0.5.0.jar!/META-INF/jars/MixinExtras-0.5.0.jar |
| Ferrite Core | 6.0.1 | `ferritecore` | mods/ferritecore-6.0.1-forge.jar |
| FishingReal | 1.9.1-backport | `fishingreal` | mods/fishingreal-forge-1.20.1-1.9.1-backport.jar |
| Flashier Flashlights | 1.2.0 | `flashlightmod` | mods/Flashier%20Flashlights%201.2.0.jar |
| jcodec-0.2.5 | 0.2.5 | `` | mods/Flashier%20Flashlights%201.2.0.jar!/META-INF/jarjar/jcodec-0.2.5.jar |
| Flerovium | 1.2.19 | `flerovium` | mods/flerovium-forge-1.20.1-1.2.19-all.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/flerovium-forge-1.20.1-1.2.19-all.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/flerovium-forge-1.20.1-1.2.19-all.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| FPS Reducer | 1.20.1-2.5.1 | `fpsreducer` | mods/FpsReducer2-forge-1.20.1-2.5.1.jar |
| Fragmentum | 1.5.2 | `fragmentum` | mods/fragmentum-forge-1.20.1-1.5.2.jar |
| luaj-core-3.0.8-figura | 3.0.8-figura | `` | mods/fragmentum-forge-1.20.1-1.5.2.jar!/META-INF/jarjar/luaj-core-3.0.8-figura.jar |
| luaj-jse-3.0.8-figura | 3.0.8-figura | `` | mods/fragmentum-forge-1.20.1-1.5.2.jar!/META-INF/jarjar/luaj-jse-3.0.8-figura.jar |
| YetAnotherConfigLib | 3.6.6+1.20.1-forge | `yet_another_config_lib_v3` | mods/fragmentum-forge-1.20.1-1.5.2.jar!/META-INF/jarjar/yet-another-config-lib-3.6.6+1.20.1-forge.jar |
| common-image-3.12.0 | 3.12.0 | `` | mods/fragmentum-forge-1.20.1-1.5.2.jar!/META-INF/jarjar/yet-another-config-lib-3.6.6+1.20.1-forge.jar!/META-INF/jarjar/common-image-3.12.0.jar |
| common-io-3.12.0 | 3.12.0 | `` | mods/fragmentum-forge-1.20.1-1.5.2.jar!/META-INF/jarjar/yet-another-config-lib-3.6.6+1.20.1-forge.jar!/META-INF/jarjar/common-io-3.12.0.jar |
| common-lang-3.12.0 | 3.12.0 | `` | mods/fragmentum-forge-1.20.1-1.5.2.jar!/META-INF/jarjar/yet-another-config-lib-3.6.6+1.20.1-forge.jar!/META-INF/jarjar/common-lang-3.12.0.jar |
| imageio-core-3.12.0 | 3.12.0 | `` | mods/fragmentum-forge-1.20.1-1.5.2.jar!/META-INF/jarjar/yet-another-config-lib-3.6.6+1.20.1-forge.jar!/META-INF/jarjar/imageio-core-3.12.0.jar |
| imageio-metadata-3.12.0 | 3.12.0 | `` | mods/fragmentum-forge-1.20.1-1.5.2.jar!/META-INF/jarjar/yet-another-config-lib-3.6.6+1.20.1-forge.jar!/META-INF/jarjar/imageio-metadata-3.12.0.jar |
| imageio-webp-3.12.0 | 3.12.0 | `` | mods/fragmentum-forge-1.20.1-1.5.2.jar!/META-INF/jarjar/yet-another-config-lib-3.6.6+1.20.1-forge.jar!/META-INF/jarjar/imageio-webp-3.12.0.jar |
| gson-0.2.1 | 0.2.1 | `` | mods/fragmentum-forge-1.20.1-1.5.2.jar!/META-INF/jarjar/yet-another-config-lib-3.6.6+1.20.1-forge.jar!/META-INF/jarjar/gson-0.2.1.jar |
| json-0.2.1 | 0.2.1 | `` | mods/fragmentum-forge-1.20.1-1.5.2.jar!/META-INF/jarjar/yet-another-config-lib-3.6.6+1.20.1-forge.jar!/META-INF/jarjar/json-0.2.1.jar |
| mixinextras-forge-0.5.0 | 0.5.0 | `` | mods/fragmentum-forge-1.20.1-1.5.2.jar!/META-INF/jarjar/mixinextras-forge-0.5.0.jar |
| MixinExtras-0.5.0 | 0.5.0 | `` | mods/fragmentum-forge-1.20.1-1.5.2.jar!/META-INF/jarjar/mixinextras-forge-0.5.0.jar!/META-INF/jars/MixinExtras-0.5.0.jar |
| Framework | 0.8.0 | `framework` | mods/framework-forge-1.20.1-0.8.0.jar |
| FTB Library | 2001.2.13 | `ftblibrary` | mods/ftb-library-forge-2001.2.13.jar |
| FTB Quests | 2001.4.22 | `ftbquests` | mods/ftb-quests-forge-2001.4.22.jar |
| FTB Teams | 2001.3.2 | `ftbteams` | mods/ftb-teams-forge-2001.3.2.jar |
| FTB XMod Compat | 2.1.3 | `ftbxmodcompat` | mods/ftb-xmod-compat-forge-2.1.3.jar |
| Fusion | 1.3.14+a | `fusion` | mods/fusion-1.3.14a-forge-mc1.20.1.jar |
| Fancy World Animations | 1.2.31 | `fwa` | mods/fwa%2B1.20.1-forge-1.2.31.jar |
| Fzzy Config | 0.7.6+1.20.1+forge | `fzzy_config` | mods/fzzy_config-0.7.6+1.20.1+forge.jar |
| tomlkt-jvm-0.3.7 | 0.3.7 | `` | mods/fzzy_config-0.7.6+1.20.1+forge.jar!/META-INF/jars/tomlkt-jvm-0.3.7.jar |
| jankson-1.2.3 | 1.2.3 | `` | mods/fzzy_config-0.7.6+1.20.1+forge.jar!/META-INF/jars/jankson-1.2.3.jar |
| GeckoLib 4 | 4.8.4 | `geckolib` | mods/geckolib-forge-1.20.1-4.8.4.jar |
| mclib-20 | 20 | `` | mods/geckolib-forge-1.20.1-4.8.4.jar!/META-INF/jarjar/mclib-20.jar |
| GlitchCore | 0.0.1.1 | `glitchcore` | mods/GlitchCore-forge-1.20.1-0.0.1.1.jar |
| Golem Overhaul | 1.1.1 | `golemoverhaul` | mods/golemoverhaul-forge-1.20.1-1.1.1.jar |
| Gouge | 1.5.1 | `gouge` | mods/gouge-1.5.1-mc1.20.1-forge.jar |
| Gpu memory leak fix | 1.20.1-1.8 | `gpumemleakfix` | mods/gpumemleakfix-1.20.1-1.8.jar |
| Gravestone Mod | 1.20.1-1.0.35 | `gravestone` | mods/gravestone-forge-1.20.1-1.0.35.jar |
| Guard Villagers | 1.20.1-1.6.19 | `guardvillagers` | mods/guardvillagers-1.20.1-1.6.19.jar |
| Handcrafted | 3.0.6 | `handcrafted` | mods/handcrafted-forge-1.20.1-3.0.6.jar |
| Horseman | 1.3.17 | `horseman` | mods/horseman-1.20.1-1.3.17-forge.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/horseman-1.20.1-1.3.17-forge.jar!/META-INF/jars/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/horseman-1.20.1-1.3.17-forge.jar!/META-INF/jars/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| ImmediatelyFast | 1.5.5+1.20.4 | `immediatelyfast` | mods/ImmediatelyFast-Forge-1.5.5%2B1.20.4.jar |
| MixinExtras | 0.3.6 | `mixinextras` | mods/ImmediatelyFast-Forge-1.5.5%2B1.20.4.jar!/META-INF/jars/mixinextras-forge-0.3.6.jar |
| MixinExtras-0.3.6 | 0.3.6 | `` | mods/ImmediatelyFast-Forge-1.5.5%2B1.20.4.jar!/META-INF/jars/mixinextras-forge-0.3.6.jar!/META-INF/jars/MixinExtras-0.3.6.jar |
| Reflect-1.6.2 | 1.6.2 | `` | mods/ImmediatelyFast-Forge-1.5.5%2B1.20.4.jar!/META-INF/jars/Reflect-1.6.2.jar |
| Immersive Aircraft | 1.5.0+1.20.1 | `immersive_aircraft` | mods/immersive_aircraft-1.5.0+1.20.1-forge.jar |
| MathParser.org-mXparser-5.2.1 | 5.2.1 | `` | mods/immersive_aircraft-1.5.0+1.20.1-forge.jar!/META-INF/jars/MathParser.org-mXparser-5.2.1.jar |
| Immersive Melodies | 0.8.0+1.20.1 | `immersive_melodies` | mods/immersive_melodies-0.8.0%2B1.20.1-forge.jar |
| Immersive Weathering | 1.20.1-2.0.5 | `immersive_weathering` | mods/immersive_weathering-1.20.1-2.0.5-forge.jar |
| Immersive Lanterns | 1.0.6 | `immersivelanterns` | mods/immersivelanterns-forge-1.0.6-1.20.1.jar |
| MixinExtras | 0.3.5 | `mixinextras` | mods/immersivelanterns-forge-1.0.6-1.20.1.jar!/META-INF/jars/mixinextras-forge-0.3.5.jar |
| MixinExtras-0.3.5 | 0.3.5 | `` | mods/immersivelanterns-forge-1.0.6-1.20.1.jar!/META-INF/jars/mixinextras-forge-0.3.5.jar!/META-INF/jars/MixinExtras-0.3.5.jar |
| incision | 1.0.9 | `incision` | mods/incision-1.0.10-forge-1.20.1.jar |
| InControl | 1.20-9.5.0 | `incontrol` | mods/incontrol-1.20-9.5.0.jar |
| Infinity Buttons | 1.20.1-4.0.7 | `infinitybuttons` | mods/infinitybuttons-1.20.1-4.0.7.jar |
| InsaneLib | 1.23.4.6 | `insanelib` | mods/insanelib-1.23.4.6.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/insanelib-1.23.4.6.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/insanelib-1.23.4.6.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| Iron's Lib | 1.20.1-1.0.2 | `irons_lib` | mods/irons_lib-1.20.1-1.0.2.jar |
| Iron's Spells 'n Spellbooks | 1.20.1-3.15.6 | `irons_spellbooks` | mods/irons_spellbooks-1.20.1-3.15.6.jar |
| Item Obliterator | 2.3.0 | `item_obliterator` | mods/Item-Obliterator-NeoForge-MC1.20.1-2.3.1.jar |
| Jack's Gravestones | 1.0.5 | `jacksgravestones` | mods/jacks-gravestones-forge-1.20.1-1.0.6.jar |
| Jade | 11.13.3+forge | `jade` | mods/Jade-1.20.1-Forge-11.13.3.jar |
| Jaden's Nether Expansion | 2.3.5 | `netherexp` | mods/Jadens-Nether-Expansion-2.3.5.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/Jadens-Nether-Expansion-2.3.5.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/Jadens-Nether-Expansion-2.3.5.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| JamLib | 1.3.6+1.20.1-patch.1 | `jamlib` | mods/jamlib-forge-1.3.6%2B1.20.1-patch.1.jar |
| jankson-1.2.3 | 1.2.3 | `` | mods/jamlib-forge-1.3.6%2B1.20.1-patch.1.jar!/META-INF/jars/jankson-1.2.3.jar |
| Just Enough Items | 15.49.0.194 | `jei` | mods/jei-1.20.1-forge-15.49.0.194.jar |
| JEM Advancements | 5.1.0 | `jem_advancements` | mods/jem-advancements-5.1.0.jar |
| AutoModpack | 5.0.0-rc.1 | `automodpack` | mods/jem-bootstrap.jar |
| JEM Economy | 1.3.0 | `jem_economy` | mods/jem-economy-1.3.0.jar |
| JEM Identity | 1.0.0 | `jem_identity` | mods/jem-identity-1.0.0.jar |
| JEM Races | 2.0.0 | `jem_roles` | mods/jem-races-2.0.0.jar |
| JEM Rewards | 1.0.0 | `jem_rewards` | mods/jem-rewards-1.0.0.jar |
| JEM Server | 2.0.0-dev | `jem_server` | mods/jem-server-2.0.0-dev.jar |
| JEM Settlements | 2.1.0 | `jem_settlements` | mods/jem-settlements-2.1.0.jar |
| JEM World Boss Tiers | 2.5.0-dev | `jem_world_boss_tiers` | mods/jem-world-boss-tiers-2.5.0-dev.jar |
| JustEnoughMods Adaptive Culling | 1.4.1 | `jem_adaptive_culling` | mods/jem_adaptive_culling-1.4.1.jar |
| JEM Drill Lifecycle | 1.0.0 | `jem_drill_lifecycle` | mods/jem_drill_lifecycle-1.0.0.jar |
| JEM Twelve Eyes Campaign | 1.7.0-dev | `jem_twelve_eyes` | mods/jem_twelve_eyes-1.7.0-dev.jar |
| JustEnoughMods Compatibility | 3.7.0 | `jemcompat` | mods/jemcompat-3.7.0.jar |
| JEM Food | 1.0.0 | `jemfood` | mods/jemfood-1.0.0.jar |
| Kaleidoscope Compat | 2.6.5-forge+mc1.20.1 | `kaleidoscope_compat` | mods/kaleidoscope_compat-2.6.5-forge%2Bmc1.20.1.jar |
| Kaleidoscope End | 1.0.14-forge+mc1.20.1 | `kaleidoscope_end` | mods/kaleidoscope_end-1.0.14-forge%2Bmc1.20.1.jar |
| mixinextras-forge-0.5.0 | 0.5.0 | `` | mods/kaleidoscope_end-1.0.14-forge%2Bmc1.20.1.jar!/META-INF/jarjar/mixinextras-forge-0.5.0.jar |
| MixinExtras-0.5.0 | 0.5.0 | `` | mods/kaleidoscope_end-1.0.14-forge%2Bmc1.20.1.jar!/META-INF/jarjar/mixinextras-forge-0.5.0.jar!/META-INF/jars/MixinExtras-0.5.0.jar |
| Kaleidoscope Nether | 1.1.8-forge+mc1.20.1 | `kaleidoscope_nether` | mods/kaleidoscope_nether-1.1.8-forge%2Bmc1.20.1.jar |
| Kaleidoscope Cookery | 1.4.1-forge+mc1.20.1 | `kaleidoscope_cookery` | mods/kaleidoscopecookery-1.4.1-forge%2Bmc1.20.1.jar |
| Kaleidoscope Doll | 1.4.1-forge+mc1.20.1 | `kaleidoscope_doll` | mods/kaleidoscopedoll-1.4.1-forge+mc1.20.1.jar |
| Kambrik | 6.1.1+1.20.1 | `kambrik` | mods/Kambrik-6.1.1+1.20.1-forge.jar |
| Kiwi Library | 11.10.3+forge | `kiwi` | mods/Kiwi-1.20.1-Forge-11.10.3.jar |
| MixinExtras | 0.3.2 | `mixinextras` | mods/Kiwi-1.20.1-Forge-11.10.3.jar!/META-INF/jarjar/mixinextras-forge-0.3.2.jar |
| MixinExtras-0.3.2 | 0.3.2 | `` | mods/Kiwi-1.20.1-Forge-11.10.3.jar!/META-INF/jarjar/mixinextras-forge-0.3.2.jar!/META-INF/jars/MixinExtras-0.3.2.jar |
| Knight Lib | 2.0.0 | `knightlib` | mods/knightlib-forge-1.20.1-2.0.0.jar |
| Konkrete | 1.8.0 | `konkrete` | mods/konkrete_forge_1.8.0_MC_1.20-1.20.1.jar |
| kotlinforforge-4.12.0-all | 4.12.0 | `` | mods/kotlinforforge-4.12.0-all.jar |
| kfflang-4.12.0 | 4.12.0 | `` | mods/kotlinforforge-4.12.0-all.jar!/META-INF/jarjar/kfflang-4.12.0.jar |
| kfflib-4.12.0 | 4.12.0 | `` | mods/kotlinforforge-4.12.0-all.jar!/META-INF/jarjar/kfflib-4.12.0.jar |
| Kotlin For Forge | 4.12.0 | `kotlinforforge` | mods/kotlinforforge-4.12.0-all.jar!/META-INF/jarjar/kffmod-4.12.0.jar |
| Ksyxis | 1.4.4 | `ksyxis` | mods/Ksyxis-1.4.4.jar |
| KubeJS Create | 2001.3.0-build.8 | `kubejs_create` | mods/kubejs-create-forge-2001.3.0-build.8.jar |
| KubeJS | 2001.6.5-build.26 | `kubejs` | mods/kubejs-forge-2001.6.5-build.26.jar |
| MixinExtras | 0.2.0-rc.2 | `mixinextras` | mods/kubejs-forge-2001.6.5-build.26.jar!/META-INF/jars/mixinextras-forge-0.2.0-rc.2.jar |
| MixinExtras-0.2.0-rc.2 | 0.2.0-rc.2 | `` | mods/kubejs-forge-2001.6.5-build.26.jar!/META-INF/jars/mixinextras-forge-0.2.0-rc.2.jar!/META-INF/jars/MixinExtras-0.2.0-rc.2.jar |
| cataclysm | 3.31 | `cataclysm` | mods/L_Enders_Cataclysm-3.31.jar |
| mixinextras-forge-0.5.3 | 0.5.3 | `` | mods/L_Enders_Cataclysm-3.31.jar!/META-INF/jarjar/mixinextras-forge-0.5.3.jar |
| MixinExtras-0.5.3 | 0.5.3 | `` | mods/L_Enders_Cataclysm-3.31.jar!/META-INF/jarjar/mixinextras-forge-0.5.3.jar!/META-INF/jars/MixinExtras-0.5.3.jar |
| LegendaryMonsters | 1.20.1 | `legendary_monsters` | mods/legendarymonsters-2.2.2%20MC%201.20.1.jar |
| Let Me Despawn | 1.5.0 | `letmedespawn` | mods/letmedespawn-1.20.x-forge-1.5.0.jar |
| [Let's Do] Bakery | 2.0.6 | `bakery` | mods/letsdo-bakery-forge-2.0.6.jar |
| [Let's Do] Beachparty | 2.0.3 | `beachparty` | mods/letsdo-beachparty-forge-2.0.3.jar |
| [Let's Do] Brewery | 2.0.6 | `brewery` | mods/letsdo-brewery-forge-2.0.6.jar |
| [Let's Do] Farm & Charm | 1.0.14 | `farm_and_charm` | mods/letsdo-farm_and_charm-forge-1.0.14.jar |
| [Let's Do] HerbalBrews | 1.0.12 | `herbalbrews` | mods/letsdo-herbalbrews-forge-1.0.12.jar |
| [Let's Do] Vinery | 1.4.41 | `vinery` | mods/letsdo-vinery-forge-1.4.41.jar |
| Library ferret | 4.0.0 | `libraryferret` | mods/libraryferret-forge-1.20.1-4.0.0.jar |
| LionfishAPI | 3.0 | `lionfishapi` | mods/lionfishapi-3.0.jar |
| LitematicaTool | 2.0.0 | `litematicatool` | mods/litematicatool-forge-2.0.0.jar |
| Lithostitched | 1.4.11 | `lithostitched` | mods/lithostitched-forge-1.20.1-1.4.11.jar |
| Lodestone | 1.20.1-1.6.4.1 | `lodestone` | mods/lodestone-1.20.1-1.6.4.1.jar |
| Loot Journal | 6.2.1 | `loot_journal` | mods/loot_journal-forge-1.20.1-6.2.1.jar |
| mixinextras-forge-0.5.0 | 0.5.0 | `` | mods/loot_journal-forge-1.20.1-6.2.1.jar!/META-INF/jarjar/mixinextras-forge-0.5.0.jar |
| MixinExtras-0.5.0 | 0.5.0 | `` | mods/loot_journal-forge-1.20.1-6.2.1.jar!/META-INF/jarjar/mixinextras-forge-0.5.0.jar!/META-INF/jars/MixinExtras-0.5.0.jar |
| Luminous Beasts | 1.2.7 | `luminous_beasts` | mods/Luminous%20Beasts%20V1.2.7%20-%20Forge%201.20.1.jar |
| Luminous Nether | 1.3.3 | `luminous_nether` | mods/Luminous%20Nether%20V1.3.3%20-%20Forge%201.20.1.jar |
| Luminous:Tag | 1.1 | `luminoustag` | mods/LuminousTag V1.1 - Forge 1.20.1.jar |
| 3D Maps | 1.0.1-1.20.1-forge | `maps3d` | mods/maps3d_1.0.1-1.20.1-forge.jar |
| MapSyncer | 1.0.3 | `mapsyncer` | mods/mapsyncer-1.0.3-forge-1.20.1.jar |
| Melody | 1.0.2 | `melody` | mods/melody_forge_1.0.3_MC_1.20.1-1.20.4.jar |
| MidnightLib | 1.9.2 | `midnightlib` | mods/midnightlib-forge-1.9.2%2B1.20.1.jar |
| Minecraft Cursor | 3.11.3+1.20.1 | `minecraft_cursor` | mods/minecraft-cursor-forge-3.11.3%2B1.20.1.jar |
| io.github.fishstiz.minecraft-cursor-common-api-3.11.3+1.20.1 | 3.11.3+1.20.1 | `` | mods/minecraft-cursor-forge-3.11.3%2B1.20.1.jar!/META-INF/jarjar/io.github.fishstiz.minecraft-cursor-common-api-3.11.3+1.20.1.jar |
| MOA DECOR: HOLIDAYS | 1.20.1 | `moa_decor_holidays` | mods/MOAdecor+HOLIDAYS+1.20.1.jar |
| Mob Home | 1.0.0 | `mobhome` | mods/mobhome-1.0.0.jar |
| Modefite - Item Definition Backport | 1.0.3+1.20.1 | `modefite` | mods/modefite-forge-1.0.3%2B1.20.1.jar |
| ModernFix | 5.27.77+mc1.20.1 | `modernfix` | mods/modernfix-forge-5.27.77%2Bmc1.20.1.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/modernfix-forge-5.27.77%2Bmc1.20.1.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/modernfix-forge-5.27.77%2Bmc1.20.1.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| MoogsMineshaftsReimagined | 1.0.2 | `mmr` | mods/MoogsMineshaftsReimagined-1.20-1.0.2.jar |
| MoogsNetherStructures | 3.0.0 | `mns` | mods/MoogsNetherStructures-1.20-3.0.0.jar |
| MoogsOceanStructures | 1.1.0 | `mos` | mods/MoogsOceanStructures-universal-1.20-1.1.0.jar |
| Moog's Structure Lib | 3.1.2 | `moogs_structures` | mods/MoogsStructureLib-forge-1.20-3.1.2.jar |
| MoogsTemplesReimagined | 2.0.3 | `mtr` | mods/MoogsTemplesReimagined-universal-1.20-2.0.3.jar |
| MoogsVoyagerStructures | 5.0.8 | `mvs` | mods/MoogsVoyagerStructures-1.20-5.0.8.jar |
| Moonlight Library | 1.20-2.16.35 | `moonlight` | mods/moonlight-1.20-2.16.35-forge.jar |
| MixinExtras | 0.3.6 | `mixinextras` | mods/moonlight-1.20-2.16.35-forge.jar!/META-INF/jars/mixinextras-forge-0.3.6.jar |
| MixinExtras-0.3.6 | 0.3.6 | `` | mods/moonlight-1.20-2.16.35-forge.jar!/META-INF/jars/mixinextras-forge-0.3.6.jar!/META-INF/jars/MixinExtras-0.3.6.jar |
| MixinSquared | 0.1.1 | `mixinsquared` | mods/moonlight-1.20-2.16.35-forge.jar!/META-INF/jars/mixinsquared-forge-0.1.1.jar |
| MixinSquared-0.1.1 | 0.1.1 | `` | mods/moonlight-1.20-2.16.35-forge.jar!/META-INF/jars/mixinsquared-forge-0.1.1.jar!/META-INF/jars/MixinSquared-0.1.1.jar |
| More Totems of Undying | 2.16.0 | `moretotems` | mods/More Totems of Undying-forge-1.20.1-2.16.0.jar |
| More Culling | 0.24.6 | `moreculling` | mods/moreculling-0.24.6-all.jar |
| mixinextras-forge-0.5.4 | 0.5.4 | `` | mods/moreculling-0.24.6-all.jar!/META-INF/jarjar/mixinextras-forge-0.5.4.jar |
| MixinExtras-0.5.4 | 0.5.4 | `` | mods/moreculling-0.24.6-all.jar!/META-INF/jarjar/mixinextras-forge-0.5.4.jar!/META-INF/jars/MixinExtras-0.5.4.jar |
| MixinSquared | 0.2.0 | `mixinsquared` | mods/moreculling-0.24.6-all.jar!/META-INF/jarjar/mixinsquared-forge-0.2.0.jar |
| MixinSquared-0.2.0 | 0.2.0 | `` | mods/moreculling-0.24.6-all.jar!/META-INF/jarjar/mixinsquared-forge-0.2.0.jar!/META-INF/jars/MixinSquared-0.2.0.jar |
| Mouse Tweaks | 2.25.1 | `mousetweaks` | mods/MouseTweaks-forge-mc1.20.1-2.25.1.jar |
| MRU | 1.0.33+1.20.1 | `mru` | mods/mru-1.0.33%2B1.20.1-forge.jar |
| mixinextras-forge-0.5.0 | 0.5.0 | `` | mods/mru-1.0.33%2B1.20.1-forge.jar!/META-INF/jarjar/mixinextras-forge-0.5.0.jar |
| MixinExtras-0.5.0 | 0.5.0 | `` | mods/mru-1.0.33%2B1.20.1-forge.jar!/META-INF/jarjar/mixinextras-forge-0.5.0.jar!/META-INF/jars/MixinExtras-0.5.0.jar |
| MyServerIsCompatible | 1.0 | `myserveriscompatible` | mods/MyServerIsCompatible-1.20-1.0.jar |
| Nature's Spirit | 2.2.5-1.20.1 | `natures_spirit` | mods/natures_spirit-2.2.5-1.20.1.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/natures_spirit-2.2.5-1.20.1.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/natures_spirit-2.2.5-1.20.1.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| Nature's Compass | 1.20.1-1.12.0-forge | `naturescompass` | mods/NaturesCompass-1.20.1-1.12.0-forge.jar |
| Necronomicon | 1.6.0 | `necronomicon` | mods/Necronomicon-Forge-1.6.0+1.20.1.jar |
| No Resource Pack Warnings Forge | 1.0.0 | `no_resource_pack_warnings_forge` | mods/no_resource_pack_warnings_forge-1.20.1-1.0.0.jar |
| Nullscape | 1.2.8 | `nullscape` | mods/Nullscape_1.20.x_v1.2.8.jar |
| Obscure Tooltips | 3.10.0 | `obscure_tooltips` | mods/obscure_tooltips-forge-1.20.1-3.10.0.jar |
| mixinextras-forge-0.5.0 | 0.5.0 | `` | mods/obscure_tooltips-forge-1.20.1-3.10.0.jar!/META-INF/jarjar/mixinextras-forge-0.5.0.jar |
| MixinExtras-0.5.0 | 0.5.0 | `` | mods/obscure_tooltips-forge-1.20.1-3.10.0.jar!/META-INF/jarjar/mixinextras-forge-0.5.0.jar!/META-INF/jars/MixinExtras-0.5.0.jar |
| Oceanic Realms | 1.0.7 | `oceanicrealms` | mods/oceanicrealms-1.0.7-last-laster-ver.jar |
| Oculus | 1.8.0 | `oculus` | mods/oculus-mc1.20.1-1.8.0.jar |
| jcpp-1.4.14 | 1.4.14 | `` | mods/oculus-mc1.20.1-1.8.0.jar!/META-INF/jars/jcpp-1.4.14.jar |
| P1nero's Epic Cataclysm | 20.14.0.9 | `p1nero_ec` | mods/p1nero_ec-20.14.0.9.jar |
| PacketFixer | 3.3.2 | `packetfixer` | mods/packetfixer-3.3.2-1.18-1.20.4-merged.jar |
| Paraglider | 20.1.3 | `paraglider` | mods/Paraglider-forge-20.1.3.jar |
| Particle Core | 0.3.3+1.20.1+forge | `particle_core` | mods/particle_core-0.3.3+1.20.1+forge.jar |
| conditional mixin | 0.6.4 | `conditional_mixin` | mods/particle_core-0.3.3+1.20.1+forge.jar!/META-INF/jars/conditional-mixin-forge-0.6.4.jar |
| mixinextras-forge-0.5.3 | 0.5.3 | `` | mods/particle_core-0.3.3+1.20.1+forge.jar!/META-INF/jars/mixinextras-forge-0.5.3.jar |
| MixinExtras-0.5.3 | 0.5.3 | `` | mods/particle_core-0.3.3+1.20.1+forge.jar!/META-INF/jars/mixinextras-forge-0.5.3.jar!/META-INF/jars/MixinExtras-0.5.3.jar |
| Particular Reforged | 1.5.7 | `particular` | mods/particular-1.20.1-Forge-1.5.7.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/particular-1.20.1-Forge-1.5.7.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/particular-1.20.1-Forge-1.5.7.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| Patchouli | 1.20.1-85-FORGE | `patchouli` | mods/Patchouli-1.20.1-85-FORGE.jar |
| PatPat | 1.3.1+1.20.1+forge | `patpat` | mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/PatPat-1.3.1%2B1.20.1%2Bforge.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| Paxi | 1.20-Forge-4.0 | `paxi` | mods/Paxi-1.20-Forge-4.0.jar |
| Pehkui | 3.8.2+1.20.1-forge | `pehkui` | mods/Pehkui-3.8.2+1.20.1-forge.jar |
| MixinExtras | 0.3.6 | `mixinextras` | mods/Pehkui-3.8.2+1.20.1-forge.jar!/META-INF/jars/mixinextras-forge-0.3.6.jar |
| MixinExtras-0.3.6 | 0.3.6 | `` | mods/Pehkui-3.8.2+1.20.1-forge.jar!/META-INF/jars/mixinextras-forge-0.3.6.jar!/META-INF/jars/MixinExtras-0.3.6.jar |
| Placebo | 8.6.3 | `placebo` | mods/Placebo-1.20.1-8.6.3.jar |
| Platform | 1.3.4 | `platform` | mods/Platform-forge-1.20.1-1.3.4.jar |
| Player Animator | 1.0.2-rc1+1.20 | `playeranimator` | mods/player-animation-lib-forge-1.0.2-rc1+1.20.jar |
| PlayerRevive | 2.0.31 | `playerrevive` | mods/PlayerRevive_FORGE_v2.0.31_mc1.20.1.jar |
| Polymorph | 0.49.10+1.20.1 | `polymorph` | mods/polymorph-forge-0.49.10+1.20.1.jar |
| SpectreLib | 0.13.17+1.20.1 | `spectrelib` | mods/polymorph-forge-0.49.10+1.20.1.jar!/META-INF/jarjar/spectrelib-forge-0.13.17+1.20.1.jar |
| Primal | 1.1.6 | `primal` | mods/primal-1.1.6%2B1.20.1.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/primal-1.1.6%2B1.20.1.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/primal-1.1.6%2B1.20.1.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| Pro Placer | 8.0.3 | `proplacer` | mods/ProPlacer-v8.0.3-1.20.1-Forge.jar |
| Punchy | 2.7e | `punchy` | mods/punchy-2.7e-forge-1.20.1.jar |
| Punchy Epic Fight Compat | 1.0.0 | `punchy_epicfight_compat` | mods/punchy_epicfight_compat-%20Epic%20Fight..jar |
| Puzzles Lib | 8.1.33 | `puzzleslib` | mods/PuzzlesLib-v8.1.33-1.20.1-Forge.jar |
| Puzzles Access Api | 20.1.1 | `puzzlesaccessapi` | mods/PuzzlesLib-v8.1.33-1.20.1-Forge.jar!/META-INF/jarjar/puzzlesaccessapi-forge-20.1.1.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/PuzzlesLib-v8.1.33-1.20.1-Forge.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/PuzzlesLib-v8.1.33-1.20.1-Forge.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| QuickSkin | 2.6.2.4 | `quickskin` | mods/Quick%20Skin%20-%20Forge%20-%201.20.1-2.6.2.4.jar |
| Radium | 0.12.4+git.26c9d8e | `radium` | mods/radium-mc1.20.1-0.12.4+git.26c9d8e.jar |
| NanoLiveConfig-1.2.1 | 1.2.1 | `` | mods/radium-mc1.20.1-0.12.4+git.26c9d8e.jar!/META-INF/jars/NanoLiveConfig-1.2.1.jar |
| Villager Recruits Mod | 1.15.2 | `recruits` | mods/recruits-1.20.1-1.15.2.jar |
| Resourcefulconfig | 2.1.3 | `resourcefulconfig` | mods/resourcefulconfig-forge-1.20.1-2.1.3.jar |
| Resourceful Lib | 2.1.29 | `resourcefullib` | mods/resourcefullib-forge-1.20.1-2.1.29.jar |
| yabn | 1.0.3 | `com_teamresourceful_yabn` | mods/resourcefullib-forge-1.20.1-2.1.29.jar!/META-INF/jars/yabn-1.0.3.jar |
| bytecodecs | 1.0.2 | `com_teamresourceful_bytecodecs` | mods/resourcefullib-forge-1.20.1-2.1.29.jar!/META-INF/jars/bytecodecs-1.0.2.jar |
| Rhino | 2001.2.3-build.10 | `rhino` | mods/rhino-forge-2001.2.3-build.10.jar |
| Ribbits | 1.20.1-Forge-3.0.5 | `ribbits` | mods/Ribbits-1.20.1-Forge-3.0.5.jar |
| Right Click Harvest | 4.6.1+1.20.1 | `rightclickharvest` | mods/rightclickharvest-forge-4.6.1+1.20.1.jar |
| Rotten Creatures | 1.1.0 | `rottencreatures` | mods/rottencreatures-forge-1.20.1-1.1.0.jar |
| Screenshot Viewer | 1.3.2-forge-mc1.20.1 | `screenshot_viewer` | mods/screenshot_viewer-1.3.2-forge-mc1.20.1.jar |
| Searchables | 1.0.3 | `searchables` | mods/Searchables-forge-1.20.1-1.0.3.jar |
| Secret Doors | 1.1.0 | `secretdoors` | mods/secretdoors-1.20.1-v1.1.0.jar |
| Serene Seasons | 9.1.0.3 | `sereneseasons` | mods/SereneSeasons-forge-1.20.1-9.1.0.3.jar |
| Shulker Tooltip | 1.13.1 | `shulkertooltip` | mods/ShulkerTooltip-1.13.1.jar |
| ShutterUp | 1.1.1 | `shutterup` | mods/shutterup-forge-1.20.1-1.1.1.jar |
| Siege Weapons Mod | 0.2.5 | `siegeweapons` | mods/siegeweapons-1.20.1-0.2.5.jar |
| Simply Bows | 0.1.4 | `simplybows` | mods/simplybows-forge-neoforge-0.1.4.jar |
| Simply Tooltips | 0.1.5-1.20.1 | `simplytooltips` | mods/SimplyTooltips-forge-0.1.5-1.20.1.jar |
| Skarrier Mobs | 1.1.1 | `skarrier_mobs` | mods/skarrier_mobs-1.1.1-forge-1.20.1.jar |
| Small Ships | 2.0.0-b1.4 | `smallships` | mods/smallships-forge-1.20.1-2.0.0-b1.4.jar |
| Snow! Real Magic! | 10.7.0 | `snowrealmagic` | mods/SnowRealMagic-1.20.1-Forge-10.7.0.jar |
| Fabric API Base | 0.4.32+ef105b4977 | `fabric_api_base` | mods/SnowRealMagic-1.20.1-Forge-10.7.0.jar!/META-INF/jarjar/fabric-api-base-0.4.32+ef105b4977.jar |
| Fabric Renderer API (v1) | 3.2.2+cf68abbe77 | `fabric_renderer_api_v1` | mods/SnowRealMagic-1.20.1-Forge-10.7.0.jar!/META-INF/jarjar/fabric-renderer-api-v1-3.2.2+cf68abbe77.jar |
| Fabric Renderer - Indigo | 1.5.3+b5b2da4177 | `fabric_renderer_indigo` | mods/SnowRealMagic-1.20.1-Forge-10.7.0.jar!/META-INF/jarjar/fabric-renderer-indigo-1.5.3+b5b2da4177.jar |
| Snow Under Trees | 1.4.12 | `snowundertrees` | mods/snowundertrees-1.20.1-1.4.12.jar |
| Snowy Spirit | 1.20-3.0.10 | `snowyspirit` | mods/snowyspirit-1.20-3.0.10.jar |
| Sodium Dynamic Lights | 1.0.9 | `sodiumdynamiclights` | mods/sodiumdynamiclights-forge-1.0.10-1.20.1.jar |
| Fabric API Base | 0.4.31+ef105b4977 | `fabric_api_base` | mods/sodiumdynamiclights-forge-1.0.10-1.20.1.jar!/META-INF/jars/fabric-api-base-0.4.31+ef105b4977.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/sodiumdynamiclights-forge-1.0.10-1.20.1.jar!/META-INF/jars/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/sodiumdynamiclights-forge-1.0.10-1.20.1.jar!/META-INF/jars/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| Sodium Extras | 1.0.6 | `sodiumextras` | mods/sodiumextras-forge-1.0.7-1.20.1.jar |
| MixinExtras | 0.3.5 | `mixinextras` | mods/sodiumextras-forge-1.0.7-1.20.1.jar!/META-INF/jars/mixinextras-forge-0.3.5.jar |
| MixinExtras-0.3.5 | 0.3.5 | `` | mods/sodiumextras-forge-1.0.7-1.20.1.jar!/META-INF/jars/mixinextras-forge-0.3.5.jar!/META-INF/jars/MixinExtras-0.3.5.jar |
| Some More Blocks | 1.0.7-bp | `somemoreblocks` | mods/SomeMoreBlocks@forge-1.20.1-1.0.7-bp.jar |
| Sooty Chimneys | 1.3.4 | `sootychimneys` | mods/sootychimneys-forge-1.20.1-1.3.4.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/sootychimneys-forge-1.20.1-1.3.4.jar!/META-INF/jars/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/sootychimneys-forge-1.20.1-1.3.4.jar!/META-INF/jars/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| Sophisticated Backpacks | 3.26.14.2226 | `sophisticatedbackpacks` | mods/sophisticatedbackpacks-1.20.1-3.26.14.2226.jar |
| Sophisticated Core | 1.5.15.2427 | `sophisticatedcore` | mods/sophisticatedcore-1.20.1-1.5.15.2427.jar |
| Sophisticated Storage | 1.5.9.2197 | `sophisticatedstorage` | mods/sophisticatedstorage-1.20.1-1.5.9.2197.jar |
| Sounds | 2.2.1+1.20.1+forge | `sounds` | mods/Sounds-2.2.1+1.20.1+forge.jar |
| imageio-webp-3.10.0 | 3.10.0 | `` | mods/Sounds-2.2.1+1.20.1+forge.jar!/META-INF/jars/imageio-webp-3.10.0.jar |
| common-image-3.10.0 | 3.10.0 | `` | mods/Sounds-2.2.1+1.20.1+forge.jar!/META-INF/jars/common-image-3.10.0.jar |
| YetAnotherConfigLib | 3.5.0+1.20.1-forge | `yet_another_config_lib_v3` | mods/Sounds-2.2.1+1.20.1+forge.jar!/META-INF/jars/yet-another-config-lib-3.5.0+1.20.1-forge.jar |
| imageio-core-3.10.0 | 3.10.0 | `` | mods/Sounds-2.2.1+1.20.1+forge.jar!/META-INF/jars/yet-another-config-lib-3.5.0+1.20.1-forge.jar!/META-INF/jars/imageio-core-3.10.0.jar |
| imageio-webp-3.10.0 | 3.10.0 | `` | mods/Sounds-2.2.1+1.20.1+forge.jar!/META-INF/jars/yet-another-config-lib-3.5.0+1.20.1-forge.jar!/META-INF/jars/imageio-webp-3.10.0.jar |
| imageio-metadata-3.10.0 | 3.10.0 | `` | mods/Sounds-2.2.1+1.20.1+forge.jar!/META-INF/jars/yet-another-config-lib-3.5.0+1.20.1-forge.jar!/META-INF/jars/imageio-metadata-3.10.0.jar |
| common-lang-3.10.0 | 3.10.0 | `` | mods/Sounds-2.2.1+1.20.1+forge.jar!/META-INF/jars/yet-another-config-lib-3.5.0+1.20.1-forge.jar!/META-INF/jars/common-lang-3.10.0.jar |
| common-io-3.10.0 | 3.10.0 | `` | mods/Sounds-2.2.1+1.20.1+forge.jar!/META-INF/jars/yet-another-config-lib-3.5.0+1.20.1-forge.jar!/META-INF/jars/common-io-3.10.0.jar |
| common-image-3.10.0 | 3.10.0 | `` | mods/Sounds-2.2.1+1.20.1+forge.jar!/META-INF/jars/yet-another-config-lib-3.5.0+1.20.1-forge.jar!/META-INF/jars/common-image-3.10.0.jar |
| json-0.2.1 | 0.2.1 | `` | mods/Sounds-2.2.1+1.20.1+forge.jar!/META-INF/jars/yet-another-config-lib-3.5.0+1.20.1-forge.jar!/META-INF/jars/json-0.2.1.jar |
| gson-0.2.1 | 0.2.1 | `` | mods/Sounds-2.2.1+1.20.1+forge.jar!/META-INF/jars/yet-another-config-lib-3.5.0+1.20.1-forge.jar!/META-INF/jars/gson-0.2.1.jar |
| gson-0.2.1 | 0.2.1 | `` | mods/Sounds-2.2.1+1.20.1+forge.jar!/META-INF/jars/gson-0.2.1.jar |
| imageio-core-3.10.0 | 3.10.0 | `` | mods/Sounds-2.2.1+1.20.1+forge.jar!/META-INF/jars/imageio-core-3.10.0.jar |
| Architectury | 9.2.14 | `architectury` | mods/Sounds-2.2.1+1.20.1+forge.jar!/META-INF/jars/architectury-forge-9.2.14.jar |
| common-lang-3.10.0 | 3.10.0 | `` | mods/Sounds-2.2.1+1.20.1+forge.jar!/META-INF/jars/common-lang-3.10.0.jar |
| imageio-metadata-3.10.0 | 3.10.0 | `` | mods/Sounds-2.2.1+1.20.1+forge.jar!/META-INF/jars/imageio-metadata-3.10.0.jar |
| common-io-3.10.0 | 3.10.0 | `` | mods/Sounds-2.2.1+1.20.1+forge.jar!/META-INF/jars/common-io-3.10.0.jar |
| MixinExtras | 0.5.0-beta.2 | `mixinextras` | mods/Sounds-2.2.1+1.20.1+forge.jar!/META-INF/jars/mixinextras-forge-0.5.0-beta.2.jar |
| MixinExtras-0.5.0-beta.2 | 0.5.0-beta.2 | `` | mods/Sounds-2.2.1+1.20.1+forge.jar!/META-INF/jars/mixinextras-forge-0.5.0-beta.2.jar!/META-INF/jars/MixinExtras-0.5.0-beta.2.jar |
| json-0.2.1 | 0.2.1 | `` | mods/Sounds-2.2.1+1.20.1+forge.jar!/META-INF/jars/json-0.2.1.jar |
| Spawn | 4.0.7-1.20.1 | `spawn` | mods/spawn-4.0.7-1.20.1.jar |
| Spider Overhaul | 0.0.6-Forge-v1.20 | `spider_overhaul` | mods/SpiderOverhaul-0.0.6-Forge-v1.20.jar |
| Spyglass Improvements | 1.5.12b | `spyglass_improvements` | mods/spyglass_improvements-forge-1.5.12b%2Bmc1.20%2Bforge.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/spyglass_improvements-forge-1.5.12b%2Bmc1.20%2Bforge.jar!/META-INF/jars/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/spyglass_improvements-forge-1.5.12b%2Bmc1.20%2Bforge.jar!/META-INF/jars/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| Status Effect Bars | 1.0.3 | `statuseffectbars` | mods/statuseffectbars-1.20.1-Forge-1.0.3.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/statuseffectbars-1.20.1-Forge-1.0.3.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/statuseffectbars-1.20.1-Forge-1.0.3.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| Create: Steam 'n' Rails | 1.7.3+forge-mc1.20.1 | `railways` | mods/Steam_Rails-1.7.3%2Bforge-mc1.20.1.jar |
| mixinextras-forge-0.5.3 | 0.5.3 | `` | mods/Steam_Rails-1.7.3%2Bforge-mc1.20.1.jar!/META-INF/jars/mixinextras-forge-0.5.3.jar |
| MixinExtras-0.5.3 | 0.5.3 | `` | mods/Steam_Rails-1.7.3%2Bforge-mc1.20.1.jar!/META-INF/jars/mixinextras-forge-0.5.3.jar!/META-INF/jars/MixinExtras-0.5.3.jar |
| StreamsReflowing | 2.13.1 | `streamsreflowing` | mods/StreamsReflowing-1.20.1-forge-2.13.1.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/StreamsReflowing-1.20.1-forge-2.13.1.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/StreamsReflowing-1.20.1-forge-2.13.1.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| Tectonic | 3.0.17 | `tectonic` | mods/tectonic-3.0.17-forge-1.20.1.jar |
| TerraBlender | 3.0.1.10 | `terrablender` | mods/TerraBlender-forge-1.20.1-3.0.1.10.jar |
| Terralith | 2.5.4 | `terralith` | mods/Terralith_1.20.x_v2.5.4.jar |
| Terralith ReStoned | 1.3 | `terralith_restoned` | mods/Terralith_Restoned_v1.3.jar |
| Tiny Item Animations | 1.20-1.1 | `tia` | mods/tia-1.20-1.1-forge.jar |
| Tide | 2.1.1 | `tide` | mods/tide-forge-1.20.1-2.1.1-hotfix.jar |
| TinyInv | 1.20.1-1.1.0.1 | `tinyinv` | mods/tinyinv-1.20.1-1.1.0.1.jar |
| Tom's Simple Storage Mod | 1.7.1 | `toms_storage` | mods/toms_storage-1.20-1.7.1.jar |
| TPA | 2.0.0 | `tpa` | mods/tpa-2.0.0.jar |
| Trade Cycling | 1.20.1-1.0.18 | `trade_cycling` | mods/trade-cycling-forge-1.20.1-1.0.18.jar |
| HT's TreeChop | 0.19.0 | `treechop` | mods/TreeChop-1.20.1-forge-0.19.0-fixed.jar |
| Trek | 1-B0.6.2 | `mr_trek` | mods/trek-B0.6.2%20(1).jar |
| Trials Chambers | 2.3.3 | `trials` | mods/Trials-2.3.3.jar |
| Trotting Wagons | 1.0.4 | `trotting_wagons` | mods/trotting_wagons-1.0.4.jar |
| True Ending: Ender Dragon Overhaul | 1-v1.1.0c | `mr_limesplatus_ending` | mods/tru.e-ending-v1.1.0c.jar |
| Tumbleweed | 0.5.5 | `tumbleweed` | mods/Tumbleweed-forge-1.20.1-0.5.5.jar |
| TxniLib | 1.0.24 | `txnilib` | mods/txnilib-forge-1.0.24-1.20.1.jar |
| Forgified Fabric API | 0.92.2+1.11.12+1.20.1 | `fabric_api` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar |
| fabric-loader-2.6.0+0.15.0+1.20.1-full | 2.6.0+0.15.0+1.20.1 | `` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-loader-2.6.0+0.15.0+1.20.1-full.jar |
| MixinExtras | 0.3.5 | `mixinextras` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/mixinextras-forge-0.3.5.jar |
| MixinExtras-0.3.5 | 0.3.5 | `` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/mixinextras-forge-0.3.5.jar!/META-INF/jars/MixinExtras-0.3.5.jar |
| Fabric API Base | 0.4.31+ef105b4977 | `fabric_api_base` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-api-base-0.4.31+ef105b4977.jar |
| Fabric API Lookup API (v1) | 1.6.36+67f9824077 | `fabric_api_lookup_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-api-lookup-api-v1-1.6.36+67f9824077.jar |
| Fabric Biome API (v1) | 13.0.13+dc36698e77 | `fabric_biome_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-biome-api-v1-13.0.13+dc36698e77.jar |
| Fabric Block API (v1) | 1.0.11+0e6cb7f777 | `fabric_block_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-block-api-v1-1.0.11+0e6cb7f777.jar |
| Fabric BlockView API (v2) | 1.0.1+0767707077 | `fabric_block_view_api_v2` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-block-view-api-v2-1.0.1+0767707077.jar |
| Fabric BlockRenderLayer Registration (v1) | 1.1.41+1d0da21e77 | `fabric_blockrenderlayer_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-blockrenderlayer-v1-1.1.41+1d0da21e77.jar |
| Fabric Client Tags | 1.1.2+5d6761b877 | `fabric_client_tags_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-client-tags-api-v1-1.1.2+5d6761b877.jar |
| Fabric Command API (v2) | 2.2.13+561530ec77 | `fabric_command_api_v2` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-command-api-v2-2.2.13+561530ec77.jar |
| Fabric Content Registries (v0) | 4.0.11+a670df1e77 | `fabric_content_registries_v0` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-content-registries-v0-4.0.11+a670df1e77.jar |
| Fabric Convention Tags | 1.5.5+fa3d1c0177 | `fabric_convention_tags_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-convention-tags-v1-1.5.5+fa3d1c0177.jar |
| Fabric Data Attachment API (v1) | 1.0.0+30ef839e77 | `fabric_data_attachment_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-data-attachment-api-v1-1.0.0+30ef839e77.jar |
| Fabric Data Generation API (v1) | 12.3.4+369cb3a477 | `fabric_data_generation_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-data-generation-api-v1-12.3.4+369cb3a477.jar |
| Fabric Dimensions API (v1) | 2.1.54+8005d10d77 | `fabric_dimensions_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-dimensions-v1-2.1.54+8005d10d77.jar |
| Fabric Entity Events (v1) | 1.6.0+4ca7515277 | `fabric_entity_events_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-entity-events-v1-1.6.0+4ca7515277.jar |
| Fabric Events Interaction (v0) | 0.6.2+0d0bd5a777 | `fabric_events_interaction_v0` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-events-interaction-v0-0.6.2+0d0bd5a777.jar |
| Fabric Game Rule API (v1) | 1.0.40+683d4da877 | `fabric_game_rule_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-game-rule-api-v1-1.0.40+683d4da877.jar |
| Fabric Item API (v1) | 2.1.28+4d0bbcfa77 | `fabric_item_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-item-api-v1-2.1.28+4d0bbcfa77.jar |
| Fabric Item Group API (v1) | 4.0.12+c9161c2d77 | `fabric_item_group_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-item-group-api-v1-4.0.12+c9161c2d77.jar |
| Fabric Key Binding API (v1) | 1.0.37+561530ec77 | `fabric_key_binding_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-key-binding-api-v1-1.0.37+561530ec77.jar |
| Fabric Lifecycle Events (v1) | 2.2.22+afab492177 | `fabric_lifecycle_events_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-lifecycle-events-v1-2.2.22+afab492177.jar |
| Fabric Loot API (v2) | 1.2.1+eb28f93e77 | `fabric_loot_api_v2` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-loot-api-v2-1.2.1+eb28f93e77.jar |
| Fabric Message API (v1) | 5.1.9+52cc178c77 | `fabric_message_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-message-api-v1-5.1.9+52cc178c77.jar |
| Fabric Mining Level API (v1) | 2.1.50+561530ec77 | `fabric_mining_level_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-mining-level-api-v1-2.1.50+561530ec77.jar |
| Fabric Model Loading API (v1) | 1.0.3+6274ab9d77 | `fabric_model_loading_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-model-loading-api-v1-1.0.3+6274ab9d77.jar |
| Fabric Networking API (v1) | 1.3.11+503a202477 | `fabric_networking_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-networking-api-v1-1.3.11+503a202477.jar |
| Fabric Object Builder API (v1) | 11.1.3+4bd998fa77 | `fabric_object_builder_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-object-builder-api-v1-11.1.3+4bd998fa77.jar |
| Fabric Particles (v1) | 1.1.2+78e1ecb877 | `fabric_particles_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-particles-v1-1.1.2+78e1ecb877.jar |
| Fabric Recipe API (v1) | 1.0.21+514a076577 | `fabric_recipe_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-recipe-api-v1-1.0.21+514a076577.jar |
| Fabric Registry Sync (v0) | 2.3.3+1c0ea72177 | `fabric_registry_sync_v0` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-registry-sync-v0-2.3.3+1c0ea72177.jar |
| Fabric Renderer API (v1) | 3.2.1+cf68abbe77 | `fabric_renderer_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-renderer-api-v1-3.2.1+cf68abbe77.jar |
| Fabric Renderer - Indigo | 1.5.2+b5b2da4177 | `fabric_renderer_indigo` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-renderer-indigo-1.5.2+b5b2da4177.jar |
| Fabric Rendering Fluids (v1) | 3.0.28+4ac5e37a77 | `fabric_rendering_fluids_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-rendering-fluids-v1-3.0.28+4ac5e37a77.jar |
| Fabric Rendering (v1) | 3.0.8+66e9a48f77 | `fabric_rendering_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-rendering-v1-3.0.8+66e9a48f77.jar |
| Fabric Resource Conditions API (v1) | 2.3.8+9e342fc177 | `fabric_resource_conditions_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-resource-conditions-api-v1-2.3.8+9e342fc177.jar |
| Fabric Resource Loader (v0) | 0.11.10+bcd08ed377 | `fabric_resource_loader_v0` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-resource-loader-v0-0.11.10+bcd08ed377.jar |
| Fabric Screen API (v1) | 2.0.8+45a670a577 | `fabric_screen_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-screen-api-v1-2.0.8+45a670a577.jar |
| Fabric Screen Handler API (v1) | 1.3.30+561530ec77 | `fabric_screen_handler_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-screen-handler-api-v1-1.3.30+561530ec77.jar |
| Fabric Sound API (v1) | 1.0.13+4f23bd8477 | `fabric_sound_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-sound-api-v1-1.0.13+4f23bd8477.jar |
| Fabric Transfer API (v1) | 3.3.5+631c9cd677 | `fabric_transfer_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-transfer-api-v1-3.3.5+631c9cd677.jar |
| Fabric Transitive Access Wideners (v1) | 4.3.1+1880499877 | `fabric_transitive_access_wideners_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-transitive-access-wideners-v1-4.3.1+1880499877.jar |
| Fabric Command API (v1) | 1.2.34+f71b366f77 | `fabric_command_api_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-command-api-v1-1.2.34+f71b366f77.jar |
| Fabric Models (v0) | 0.4.2+7c3892a477 | `fabric_models_v0` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-models-v0-0.4.2+7c3892a477.jar |
| Fabric Rendering Data Attachment (v1) | 0.3.37+a6081afc77 | `fabric_rendering_data_attachment_v1` | mods/txnilib-forge-1.0.24-1.20.1.jar!/META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar!/META-INF/jarjar/fabric-rendering-data-attachment-v1-0.3.37+a6081afc77.jar |
| Unearthed Journey | 2.2.3 | `unearthed_journey` | mods/unearthed_journey-2.2.3.jar |
| Unified Xaero Worldmap | 2.0.1 | `unified_xaero_worldmap` | mods/unified-xaero-worldmap-2.0.1+1.20.1-forge.jar |
| VanillaBackport | 1.1.7.10 | `vanillabackport` | mods/VanillaBackport-forge-1.20.1-1.1.7.10.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/VanillaBackport-forge-1.20.1-1.1.7.10.jar!/META-INF/jars/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/VanillaBackport-forge-1.20.1-1.1.7.10.jar!/META-INF/jars/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |
| Visuality: Reforged | 2.2.0 | `visuality` | mods/visuality-forge-2.2.0.jar |
| MixinExtras | 0.2.0-beta.6 | `mixinextras` | mods/visuality-forge-2.2.0.jar!/META-INF/jarjar/mixinextras-forge-0.2.0-beta.6.jar |
| MixinExtras-0.2.0-beta.6 | 0.2.0-beta.6 | `` | mods/visuality-forge-2.2.0.jar!/META-INF/jarjar/mixinextras-forge-0.2.0-beta.6.jar!/META-INF/jars/MixinExtras-0.2.0-beta.6.jar |
| Wakes | 1.4.1 | `wakes` | mods/wakes-1.20.1-Forge-1.4.1.jar |
| What Are They Up To | 1.20.1-1.2.3 | `watut` | mods/watut-forge-1.20.1-1.2.3.jar |
| Whaleborne | 1.20.1-1.2.4b | `whaleborne` | mods/whaleborne-1.20.1-1.2.4b.jar |
| World Play Time | 1.2.3 | `worldplaytime` | mods/worldplaytime-1.2.3-1.20.x-FORGE.jar |
| Xaero's Minimap | 26.4.2 | `xaerominimap` | mods/xaerominimap-forge-1.20.1-26.4.2.jar |
| XaeroLib | 1.7.1 | `xaerolib` | mods/xaerominimap-forge-1.20.1-26.4.2.jar!/META-INF/jarjar/xaerolib-forge-1.20.1-1.7.1.jar |
| Xaero's World Map | 1.46.0 | `xaeroworldmap` | mods/xaeroworldmap-forge-1.20.1-1.46.0.jar |
| XaeroLib | 1.7.3 | `xaerolib` | mods/xaeroworldmap-forge-1.20.1-1.46.0.jar!/META-INF/jarjar/xaerolib-forge-1.20.1-1.7.3.jar |
| XL Packets | 1.18.2-2.1 | `xlpackets` | mods/xlpackets-1.18.2-2.1.jar |
| Xtra Arrows 3 | 3.0.12 | `xtraarrows` | mods/xtraarrows-3.0.12-forge-mc1.20.1.jar |
| YetAnotherConfigLib | 3.6.6+1.20.1-forge | `yet_another_config_lib_v3` | mods/yet_another_config_lib_v3-3.6.6+1.20.1-forge.jar |
| common-image-3.12.0 | 3.12.0 | `` | mods/yet_another_config_lib_v3-3.6.6+1.20.1-forge.jar!/META-INF/jarjar/common-image-3.12.0.jar |
| common-io-3.12.0 | 3.12.0 | `` | mods/yet_another_config_lib_v3-3.6.6+1.20.1-forge.jar!/META-INF/jarjar/common-io-3.12.0.jar |
| common-lang-3.12.0 | 3.12.0 | `` | mods/yet_another_config_lib_v3-3.6.6+1.20.1-forge.jar!/META-INF/jarjar/common-lang-3.12.0.jar |
| imageio-core-3.12.0 | 3.12.0 | `` | mods/yet_another_config_lib_v3-3.6.6+1.20.1-forge.jar!/META-INF/jarjar/imageio-core-3.12.0.jar |
| imageio-metadata-3.12.0 | 3.12.0 | `` | mods/yet_another_config_lib_v3-3.6.6+1.20.1-forge.jar!/META-INF/jarjar/imageio-metadata-3.12.0.jar |
| imageio-webp-3.12.0 | 3.12.0 | `` | mods/yet_another_config_lib_v3-3.6.6+1.20.1-forge.jar!/META-INF/jarjar/imageio-webp-3.12.0.jar |
| gson-0.2.1 | 0.2.1 | `` | mods/yet_another_config_lib_v3-3.6.6+1.20.1-forge.jar!/META-INF/jarjar/gson-0.2.1.jar |
| json-0.2.1 | 0.2.1 | `` | mods/yet_another_config_lib_v3-3.6.6+1.20.1-forge.jar!/META-INF/jarjar/json-0.2.1.jar |
| YUNG's API | 1.20-Forge-4.0.6 | `yungsapi` | mods/YungsApi-1.20-Forge-4.0.6.jar |
| YUNG's Better Desert Temples | 1.20-Forge-3.0.3 | `betterdeserttemples` | mods/YungsBetterDesertTemples-1.20-Forge-3.0.3.jar |
| YUNG's Better Nether Fortresses | 1.20-Forge-2.0.6 | `betterfortresses` | mods/YungsBetterNetherFortresses-1.20-Forge-2.0.6.jar |
| YUNG's Better Ocean Monuments | 1.20-Forge-3.0.4 | `betteroceanmonuments` | mods/YungsBetterOceanMonuments-1.20-Forge-3.0.4.jar |
| Fast Noise Mod | 1.0.13 | `zfastnoise` | mods/zfastnoise-1.0.13%2B1.20%2Bforge.jar |
| mixinextras-forge-0.5.0 | 0.5.0 | `` | mods/zfastnoise-1.0.13%2B1.20%2Bforge.jar!/META-INF/jars/mixinextras-forge-0.5.0.jar |
| MixinExtras-0.5.0 | 0.5.0 | `` | mods/zfastnoise-1.0.13%2B1.20%2Bforge.jar!/META-INF/jars/mixinextras-forge-0.5.0.jar!/META-INF/jars/MixinExtras-0.5.0.jar |
| Zipline | 1.3.0 | `zipline` | mods/zipline-forge-1.20.1-1.3.0.jar |
| MixinExtras | 0.4.1 | `mixinextras` | mods/zipline-forge-1.20.1-1.3.0.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar |
| MixinExtras-0.4.1 | 0.4.1 | `` | mods/zipline-forge-1.20.1-1.3.0.jar!/META-INF/jarjar/mixinextras-forge-0.4.1.jar!/META-INF/jars/MixinExtras-0.4.1.jar |

## Development resource packs

| Name | Version | Version source | File |
| --- | --- | --- | --- |
| %5B1.6%5D%20Enhanced%20Boss%20Bars | not declared | not declared | resourcepacks/%5B1.6%5D%20Enhanced%20Boss%20Bars.zip |
| %C2%A74Drodi's%20Illagers%20%5Bv5.3.2%5D | 5.3.2 | filename | resourcepacks/%C2%A74Drodi's%20Illagers%20%5Bv5.3.2%5D.zip |
| %C2%A74Drodi's%20Slimes%20FA%20%5BV1.1%5D | 1.1 | filename | resourcepacks/%C2%A74Drodi's%20Slimes%20FA%20%5BV1.1%5D.zip |
| %C2%A76Dynamic%C2%A79Surroundings%20%C2%A723 | not declared | not declared | resourcepacks/%C2%A76Dynamic%C2%A79Surroundings%20%C2%A723.zip |
| %C2%A79Drodi's%20Illagers%20x%20FA%20%5Bv5.2%5D | 5.2 | filename | resourcepacks/%C2%A79Drodi's%20Illagers%20x%20FA%20%5Bv5.2%5D.zip |
| %C2%A7aDrodi's%20Slimes%20%5BV1.3%5D | 1.3 | filename | resourcepacks/%C2%A7aDrodi's%20Slimes%20%5BV1.3%5D.zip |
| 3D%20Furnaces | not declared | not declared | resourcepacks/3D%20Furnaces.zip |
| 3D%20Sugar%20Cane | not declared | not declared | resourcepacks/3D%20Sugar%20Cane.zip |
| AL's Boss Rush 1.1 | 1.1 | filename | resourcepacks/AL's Boss Rush 1.1.zip |
| AL's Boss Rush+FA 1.1 | 1.1 | filename | resourcepacks/AL's Boss Rush+FA 1.1.zip |
| AL's Skeletons Revamped 1.6.1 | 1.6.1 | filename | resourcepacks/AL's Skeletons Revamped 1.6.1.zip |
| AL's Skeletons Revamped+FA 1.3.1 | 1.3.1 | filename | resourcepacks/AL's Skeletons Revamped+FA 1.3.1.zip |
| AL's Zombies Revamped 1.6 | 1.6 | filename | resourcepacks/AL's Zombies Revamped 1.6.zip |
| AL's Zombies Revamped+FA 1.3 | 1.3 | filename | resourcepacks/AL's Zombies Revamped+FA 1.3.zip |
| better-hoppers-1.20.1 | not declared | not declared | resourcepacks/better-hoppers-1.20.1.zip |
| better_methyst_1.20.x | not declared | not declared | resourcepacks/better_methyst_1.20.x.zip |
| betterclicksound | not declared | not declared | resourcepacks/betterclicksound.zip |
| Blockier Goat Horn v1.2 | 1.2 | filename | resourcepacks/Blockier Goat Horn v1.2.zip |
| CoffeeGUI_1.20.1_release_v2.2.1 | 2.2.1 | filename | resourcepacks/CoffeeGUI_1.20.1_release_v2.2.1.zip |
| cubic-sun-moon-v1.8.5 | 1.8.5 | filename | resourcepacks/cubic-sun-moon-v1.8.5.zip |
| DetailedAnimationsReworked - V1.15 | 1.15 | filename | resourcepacks/DetailedAnimationsReworked - V1.15.zip |
| Drigo 3D Lantern`s x Punchy | not declared | not declared | resourcepacks/Drigo 3D Lantern`s x Punchy.zip |
| Enchantment%20Outlines-3 | not declared | not declared | resourcepacks/Enchantment%20Outlines-3.zip |
| Epic%203D%20Beds | not declared | not declared | resourcepacks/Epic%203D%20Beds.zip |
| epicfight_custom_armors | not declared | not declared | resourcepacks/epicfight_custom_armors.zip |
| FA%2BAll_Extensions-v1.8.1 | 1.8.1 | filename | resourcepacks/FA%2BAll_Extensions-v1.8.1.zip |
| FA%2BDetails-v2.2.1 | 2.2.1 | filename | resourcepacks/FA%2BDetails-v2.2.1.zip |
| FA%2BEmissive-v1.6 | 1.6 | filename | resourcepacks/FA%2BEmissive-v1.6.zip |
| Fire_Rekindled | not declared | not declared | resourcepacks/Fire_Rekindled.zip |
| Fishing%20Rod%203D%20Reimagined | not declared | not declared | resourcepacks/Fishing%20Rod%203D%20Reimagined.zip |
| Fluffy%20Carpets%20v1.1 | 1.1 | pack.mcmeta | resourcepacks/Fluffy%20Carpets%20v1.1.zip |
| FreshAnimations_v1.10.4 | 1.10.4 | filename | resourcepacks/FreshAnimations_v1.10.4.zip |
| Fusion Connected Glass v1.0.1 for Minecraft 1.20-1.21.8 | 1.0.1 | filename | resourcepacks/Fusion Connected Glass v1.0.1 for Minecraft 1.20-1.21.8.zip |
| Fyoncle's%203D%20Trims | not declared | not declared | resourcepacks/Fyoncle's%203D%20Trims.zip |
| Icon%20Xaero's%201.22 | 1.22 | filename | resourcepacks/Icon%20Xaero's%201.22.zip |
| Itsme64's+smaller+shield+and+totem+[1.6.1] | not declared | not declared | resourcepacks/Itsme64's+smaller+shield+and+totem+[1.6.1].zip |
| JEM Detailed Animations Fix | not declared | not declared | resourcepacks/JEM Detailed Animations Fix |
| JEM Mace Compatibility | not declared | not declared | resourcepacks/JEM Mace Compatibility |
| JEM_GUI_Moonlit_Slate_1.20.1 | not declared | not declared | resourcepacks/JEM_GUI_Moonlit_Slate_1.20.1.zip |
| JEM_GUI_Sunlit_Birch_1.20.1 | not declared | not declared | resourcepacks/JEM_GUI_Sunlit_Birch_1.20.1.zip |
| Low%20Fire%20PVP | not declared | not declared | resourcepacks/Low%20Fire%20PVP.zip |
| Mace%203D | not declared | not declared | resourcepacks/Mace%203D.zip |
| Mizuno%20Better%20Leaves%202.2 | 2.2 | filename | resourcepacks/Mizuno%20Better%20Leaves%202.2.zip |
| Mizuno's%20Better%20Crops%201.1 | 1.1 | filename | resourcepacks/Mizuno's%20Better%20Crops%201.1.zip |
| More Piglins_FA_1.20.1 | not declared | not declared | resourcepacks/More Piglins_FA_1.20.1.zip |
| More%20Piglins%201.21.8%20-%201.20.x | not declared | not declared | resourcepacks/More%20Piglins%201.21.8%20-%201.20.x.zip |
| Nekos Enhanced Tools and Armour Plus V1.1 | 1.1 | filename | resourcepacks/Nekos Enhanced Tools and Armour Plus V1.1.zip |
| TSS MC-SE (1.20) | not declared | not declared | resourcepacks/TSS MC-SE (1.20).zip |
| Undopia_3D_Iron_Bars_1.20.1_v.1.0 | 1.0 | filename | resourcepacks/Undopia_3D_Iron_Bars_1.20.1_v.1.0.zip |
| Undopia_3D_Rails_1.20.x_v.1.0 | 1.0 | filename | resourcepacks/Undopia_3D_Rails_1.20.x_v.1.0.zip |
| Undopia_3D_Sculk_Shrieker_1.20.1_v.1.0 | 1.0 | filename | resourcepacks/Undopia_3D_Sculk_Shrieker_1.20.1_v.1.0.zip |
| VISIBLE%20ORES%20PLUS%20v1.19.0 | 1.19.0 | filename | resourcepacks/VISIBLE%20ORES%20PLUS%20v1.19.0.zip |
| Create Immersive Aircrafts Resource Pack 1.20.1 - 3.0 | 3.0 | filename | config/paxi/resourcepacks/Create Immersive Aircrafts Resource Pack 1.20.1 - 3.0.zip |
| JEM Building Hammer | not declared | not declared | config/paxi/resourcepacks/JEM Building Hammer.zip |
| kubejs/assets | not declared | content SHA-256 identifies this snapshot | kubejs/assets |

## Development data packs

| Name | Version | Version source | File |
| --- | --- | --- | --- |
| Create Immersive Aircrafts Datapack 1.20.1 - 3.0 | 3.0 | filename | config/paxi/datapacks/Create Immersive Aircrafts Datapack 1.20.1 - 3.0.zip |
| disable_epicfight_mobs | not declared | not declared | config/paxi/datapacks/disable_epicfight_mobs |
| jem-content-consolidation | not declared | not declared | config/paxi/datapacks/jem-content-consolidation |
| jem-oceanic-worldgen-safety | not declared | not declared | config/paxi/datapacks/jem-oceanic-worldgen-safety |
| jem-profession-skills | not declared | not declared | config/paxi/datapacks/jem-profession-skills |
| jem-twelve-eye-gates | not declared | not declared | config/paxi/datapacks/jem-twelve-eye-gates |
| just-enough-guide | not declared | not declared | config/paxi/datapacks/just-enough-guide |
| kubejs/data | not declared | content SHA-256 identifies this snapshot | kubejs/data |

## Development shader packs

| Name | Version | Version source | File |
| --- | --- | --- | --- |
| [Кинематографический]ComplementaryUnbound_r5.5.1 + EuphoriaPatches_1.6.5 | 1.6.5 | filename | shaderpacks/[Кинематографический]ComplementaryUnbound_r5.5.1 + EuphoriaPatches_1.6.5 |
| [Комфортный ALT]ComplementaryReimagined_r5.5.1 | 5.5.1 | filename | shaderpacks/[Комфортный ALT]ComplementaryReimagined_r5.5.1.zip |
| [Комфортный]ComplementaryReimagined_r5.5.1 + EuphoriaPatches_1.6.5 | 1.6.5 | filename | shaderpacks/[Комфортный]ComplementaryReimagined_r5.5.1 + EuphoriaPatches_1.6.5 |
| Bliss_v2.1.2_(Chocapic13_Shaders_edit) | 2.1.2 | filename | shaderpacks/Bliss_v2.1.2_(Chocapic13_Shaders_edit).zip |
| Bloop-1.8.0-Alpha-3 | not declared | not declared | shaderpacks/Bloop-1.8.0-Alpha-3.zip |
| BSLClassic_v10.0 | 10.0 | filename | shaderpacks/BSLClassic_v10.0.zip |
| BVS - Best Vanilla Shader 1.9.3 | 1.9.3 | filename | shaderpacks/BVS - Best Vanilla Shader 1.9.3.zip |
| ComplementaryUnbound_r5.5.1 | 5.5.1 | filename | shaderpacks/ComplementaryUnbound_r5.5.1.zip |
| Fantasy Shaders Reimagined v1.1 | 1.1 | filename | shaderpacks/Fantasy Shaders Reimagined v1.1.zip |
| Hysteria-Shaders-Universal-v1.1.0 | 1.1.0 | filename | shaderpacks/Hysteria-Shaders-Universal-v1.1.0.zip |
| MakeUp-UltraFast-9.3e | not declared | not declared | shaderpacks/MakeUp-UltraFast-9.3e.zip |
| Nostalgia_v5.1 | 5.1 | filename | shaderpacks/Nostalgia_v5.1.zip |
| photon_v1.2a | 1.2a | filename | shaderpacks/photon_v1.2a.zip |
| PixelCraftShaders_r1.2 | 1.2 | filename | shaderpacks/PixelCraftShaders_r1.2.zip |
| PotatoShaders_v1.0d | 1.0d | filename | shaderpacks/PotatoShaders_v1.0d.zip |
| rethinking-voxels_r0.1-beta9 | not declared | not declared | shaderpacks/rethinking-voxels_r0.1-beta9.zip |
| Sildur's+Vibrant+Shaders+v1.51+Extreme-VL | 1.51 | filename | shaderpacks/Sildur's+Vibrant+Shaders+v1.51+Extreme-VL.zip |
| Solas%20Shader%20V3.7 | 3.7 | filename | shaderpacks/Solas%20Shader%20V3.7.zip |
| superDuperVanilla | not declared | not declared | shaderpacks/superDuperVanilla.zip |

## JEM source modules

Source metadata includes production, development-only and retired entries from `tools/module-inventory.json`. Retired entries are historical and are not active pack dependencies. [Structured module metadata](development-modules.json)

| Module | Mod name | Source version | Status |
| --- | --- | --- | --- |
| JEMAchievementGuide | JEM Advancements | 5.1.0 | production |
| JEMAdaptiveCulling | JustEnoughMods Adaptive Culling | 1.4.1 | production |
| JEMClaims | JEMClaims | not declared | development |
| JEMCompat | JustEnoughMods Compatibility | 3.7.0 | production |
| JEMDrillLifecycle | JEM Drill Lifecycle | 1.0.0 | production |
| JEMEconomy | JEM Economy | 1.3.0 | production |
| JEMGuiSandbox | JEM GUI Sandbox | 1.0.0 | development |
| JEMIdentity | JEM Identity | 1.0.0 | production |
| JEMProfessions | JEM Professions | 1.0.0 | development |
| JEMRewards | JEM Rewards | 1.0.0-dev | production |
| JEMRoles | JEM Races | 2.0.0 | production |
| JEMServer | JEM Server | 2.0.0-dev | production |
| JEMSettlements | JEM Settlements | 2.1.0 | production |
| JEMTwelveEyes | JEM Twelve Eyes Campaign | 1.7.0-dev | production |
| JEMVillagerTalking | JEM Villager Talking | 1.0.0-dev | development |
| JEMWorldBossTiers | JEM World Boss Tiers | 2.5.0-dev | production |
| JEMCurrencyCore | JEMCurrencyCore | not declared | retired |
| JEMMenuSupport | JEMMenuSupport | not declared | retired |
| JEMPackCore | JEMPackCore | not declared | retired |
| JEMVisualSandboxR3 | JEMVisualSandboxR3 | not declared | retired |
| JEMBootstrap | JEMBootstrap | 5.0.0-rc.1 / jem.10 | upstream-patch |
| MobHome | Mob Home | 1.0.0 | production |
| JEMFood | JEM Food | 1.0.0 | production |

## Capture scope

- Original bootstrap JAR and native active projection are inventoried separately
- Current ManagedPack and ordinary local Server have their own inventories
- External Paxi, OpenLoader, global and server world datapack folders are included when present
- KubeJS assets and data are recorded as virtual packs
- Mods own their built-in default resources and data, covered by the mod version and JAR hash
- World saves, credentials, account details, personal waypoints and non-control options are excluded
- No mod removal, keybind filtering, production build, gameplay launch or deployment was performed

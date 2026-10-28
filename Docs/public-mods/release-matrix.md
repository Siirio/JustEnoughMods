# Public Forge 1.20.1 release matrix

This matrix freezes the approved standalone CurseForge scope from 2026-10-28. It is the canonical public-project list. A project is not a release candidate until its own readiness record passes every applicable gate

| Public project | Technical mod ID | Owner module | Side | Required dependencies | Optional dependencies | Mods it supports | Preserved identities | Migration source |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| JEM Animal Hints | `jem_animal_hints` | `JEMAnimalHints` | Both-sided | Forge 47.4.10+, Minecraft 1.20.1, Jade 11.13.3+ | Supported animal mods through data-driven entries | Minecraft, Alex's Mobs, Alex's Caves, Primal, Spawn | Existing `jemcompat:taming_item` and `jemcompat:breeding_item` Jade UIDs, `jemcompat/taming` datapack path, EN/RU translation meaning | Animal Jade providers and catalogs in `JEMCompat` |
| JEM Drill Durability | `jem_drill_lifecycle` | `JEMDrillLifecycle` | Client and server required | Forge 47.4.10+, Minecraft 1.20.1, Create 6.0.8+ | Jade 11+ | Create, Jade | Mod ID, drill wear NBT, configuration keys, Jade condition semantics | Existing standalone `JEMDrillLifecycle` |
| JEM Mob Home | `mobhome` | `MobHome` | Client and server required | Forge 47.4.10+, Minecraft 1.20.1 | Audited animal and companion mods through isolated adapters | Minecraft tameables, Alex's Mobs, Alex's Caves, Companions, Iron's Spells 'n Spellbooks, EFN, Primal, Spawn, Enderman Overhaul, Cataclysm, CNC | `mobhome` registries and network namespace, `jem_companions` journal filename and fields, block/item IDs, saved bindings and recovery data | Existing standalone `MobHome` |
| JEM Excavation | `jem_excavation` | `JEMExcavation` | Client and server required | Forge 47.4.10+, Minecraft 1.20.1 | None | Minecraft and Forge-compatible tools and blocks using native mining contracts | `jem_pack_core:excavation` enchantment ID, levels, item enchantment NBT, mining rules and preview behavior | Excavation registration, events, client preview and Mixins in `JEMCompat` |
| JEM Freecam | `jem_freecam` | `JEMFreecam` | Client-only | Forge 47.4.10+, Minecraft 1.20.1 | Epic Fight | Minecraft, Epic Fight 20.14.17 | Existing key translation and binding, collision-limited camera behavior and optional Epic Fight hooks | Free-camera controller and Mixins in `JEMCompat` |
| JEM Food Blessings | `jemfood` | `JEMFood` | Client and server required | Forge 47.4.10+, Minecraft 1.20.1 | None | Minecraft and Forge foods using native food and effect contracts | `jemfood` mod, registry and channel namespaces, food quality NBT, talent data, Food Blessing state and tick-based gameplay timing | Existing standalone `JEMFood` |
| JEM Seasons HUD | `jem_seasons_hud` | `JEMSeasonsHud` | Client-only | Forge 47.4.10+, Minecraft 1.20.1 | Serene Seasons 9.1.0.3+ | Minecraft, Serene Seasons | Current HUD placement, visibility and EN/RU text, migrated `jemcompat-client.toml` time HUD values | Time and season HUD plus Serene Seasons adapter in `JEMCompat` |
| JEM Inventory Shortcuts | `jem_inventory_shortcuts` | `JEMInventoryShortcuts` | Client and server required | Forge 47.4.10+, Minecraft 1.20.1 | Sophisticated Core, Sophisticated Storage, Sophisticated Backpacks | Minecraft menus, JEI, Sophisticated Core, Sophisticated Storage, Sophisticated Backpacks | `jemcompat:inventory_delete` channel and protocol, Delete and Shift+Delete bindings and fail-closed slot rules | Inventory key handler, network and optional storage adapter in `JEMCompat` |
| In Chat FTB | `in_chat_ftb` | `JEMInChatFTB` | Client-only | Forge 47.4.10+, Minecraft 1.20.1, FTB Quests 2001.4.22 | FTB Library as the upstream transitive API dependency | FTB Quests, FTB Library | Mod ID, clickable cyan quest-name presentation and native quest-page navigation | Existing standalone `JEMInChatFTB` |
| JEM Adaptive Culling | `jem_adaptive_culling` | `JEMAdaptiveCulling` | Client-only | Forge 47.4.10+, Minecraft 1.20.1 | Embeddium 0.3.31, Oculus 1.8.0 | Minecraft renderer, Embeddium, Oculus | Mod ID, configuration keys, visibility caches, geometry invalidation and compatibility exclusions | Existing standalone `JEMAdaptiveCulling` |
| Unofficial Luxium Compatibility | `jem_luxium` | `JEMLuxium` | Client-only | Forge 47.4.10+, Minecraft 1.20.1, official unmodified Luxium 2.8.0-pre-alpha, Embeddium 0.3.31, Oculus 1.8.0 | None | Luxium, Embeddium, Oculus | Mod ID, mutual-exclusion state, restart persistence and managed-context reload behavior | Existing standalone `JEMLuxium` |

## Future standalone candidate

`JEM Serene Seasons Compatibility` is planned as a separate multi-mod compatibility project after the frozen eleven. It is not part of the current release count and is not yet a release candidate

| Planned project | Provisional mod ID | Provisional side | Required dependency | Mods it supports | Migration source |
| --- | --- | --- | --- | --- | --- |
| JEM Serene Seasons Compatibility | `jem_serene_seasons_compat` | Server gameplay, final side declaration pending clean-instance proof | Serene Seasons 9.1.0.3+ | Alex's Caves, Beachparty, Blast from the Past, Brewery, Deep Dark Regrowth, Farm & Charm, HerbalBrews, Kaleidoscope End, Kaleidoscope Nether, Luminous Nether, Nature's Spirit, Nether's Exoticism, Oceanic Realms, Primal, Snowy Spirit, Spawn, Unearthed Journey, Vinery | Existing `sereneseasons` crop tags plus the guarded Spawn sunflower adapter in `JEMCompat` |

The future project must keep all 18 content integrations optional and data-driven where tags are sufficient. Every cross-mod tag entry must use the optional `required: false` form instead of a required plain ID. Spawn remains an isolated version-reviewed adapter because its mature sunflower seed lifecycle requires code. Missing content mods must leave the project loadable and must not create missing-registry errors

## Attribution policy

JEM Animal Hints, JEM Drill Durability, In Chat FTB, JEM Seasons HUD, and JEM Adaptive Culling declare upstream integrations as dependencies. They do not carry additional credit obligations beyond accurate dependency and compatibility information

Unofficial Luxium Compatibility is the only project in this matrix with the special unofficial and Vinlanx credit block. Its required public line is:

> This is unofficial compatability mod - credits to the author of Luxium for an insanely optimized shaders

The line must be followed by a statement that the project is not affiliated with or endorsed by Luxium or Vinlanx. The project may link only to the official Luxium project or download page. It must never bundle, mirror, patch, repackage, or redistribute Luxium code, shaders, assets, branding, or JAR content

## Side meanings

- Client-only projects use `displayTest="IGNORE_SERVER_VERSION"` and are never required by a dedicated server

- Client and server required projects contain shared gameplay or server validation and require matching compatible artifacts on both sides in multiplayer

- Optional integrations must remain class-load safe when absent

- No project in this release matrix is a dedicated-server-only mod

## Evidence used for the freeze

The matrix was reconciled with `tools/module-inventory.json`, current module metadata, and the installed managed-pack artifacts. The inspected integration versions were Jade 11.13.3, Create 6.0.8, FTB Quests 2001.4.22, Serene Seasons 9.1.0.3, Embeddium 0.3.31, Oculus 1.8.0, and Luxium 2.8.0-pre-alpha

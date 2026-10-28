# Public Forge 1.20.1 release matrix

This matrix freezes the approved standalone CurseForge scope from 2026-10-28. It is the canonical public-project list. A project is not a release candidate until its own readiness record passes every applicable gate

| Public project | Technical mod ID | Owner module | Side | Required dependencies | Optional dependencies | Preserved identities | Migration source |
| --- | --- | --- | --- | --- | --- | --- | --- |
| JEM Animal Hints | `jem_animal_hints` | `JEMAnimalHints` | Client-only | Forge 47.4.10+, Minecraft 1.20.1, Jade 11.13.3+ | Supported animal mods through runtime-safe adapters | Existing `jemcompat:taming_item` and `jemcompat:breeding_item` Jade UIDs, `jemcompat/taming_items` datapack path, EN/RU translation meaning | Animal Jade providers and catalogs in `JEMCompat` |
| JEM Drill Durability | `jem_drill_lifecycle` | `JEMDrillLifecycle` | Client and server required | Forge 47.4.10+, Minecraft 1.20.1, Create 6.0.8+ | Jade 11+ | Mod ID, drill wear NBT, configuration keys, Jade condition semantics | Existing standalone `JEMDrillLifecycle` |
| JEM Mob Home | `mobhome` | `MobHome` | Client and server required | Forge 47.4.10+, Minecraft 1.20.1 | Audited animal and companion mods through isolated adapters | `mobhome` registries and network namespace, `jem_companions` journal filename and fields, block/item IDs, saved bindings and recovery data | Existing standalone `MobHome` |
| JEM Excavation | `jem_excavation` | `JEMExcavation` | Client and server required | Forge 47.4.10+, Minecraft 1.20.1 | None | `jem_pack_core:excavation` enchantment ID, levels, item enchantment NBT, mining rules and preview behavior | Excavation registration, events, client preview and Mixins in `JEMCompat` |
| JEM Freecam | `jem_freecam` | `JEMFreecam` | Client-only | Forge 47.4.10+, Minecraft 1.20.1 | Epic Fight | Existing key translation and binding, collision-limited camera behavior and optional Epic Fight hooks | Free-camera controller and Mixins in `JEMCompat` |
| JEM Food Blessings | `jemfood` | `JEMFood` | Client and server required | Forge 47.4.10+, Minecraft 1.20.1 | None | `jemfood` mod, registry and channel namespaces, food quality NBT, talent data, Food Blessing state and tick-based gameplay timing | Existing standalone `JEMFood` |
| JEM Seasons HUD | `jem_seasons_hud` | `JEMSeasonsHud` | Client-only | Forge 47.4.10+, Minecraft 1.20.1 | Serene Seasons 9.1.0.3+ | Current HUD placement, visibility and EN/RU text, migrated `jemcompat-client.toml` time HUD values | Time and season HUD plus Serene Seasons adapter in `JEMCompat` |
| JEM Inventory Shortcuts | `jem_inventory_shortcuts` | `JEMInventoryShortcuts` | Client and server required | Forge 47.4.10+, Minecraft 1.20.1 | Sophisticated Core, Sophisticated Storage, Sophisticated Backpacks | `jemcompat:inventory_delete` channel and protocol, Delete and Shift+Delete bindings and fail-closed slot rules | Inventory key handler, network and optional storage adapter in `JEMCompat` |
| In Chat FTB | `in_chat_ftb` | `JEMInChatFTB` | Client-only | Forge 47.4.10+, Minecraft 1.20.1, FTB Quests 2001.4.22 | FTB Library as the upstream transitive API dependency | Mod ID, clickable cyan quest-name presentation and native quest-page navigation | Existing standalone `JEMInChatFTB` |
| JEM Adaptive Culling | `jem_adaptive_culling` | `JEMAdaptiveCulling` | Client-only | Forge 47.4.10+, Minecraft 1.20.1 | Embeddium 0.3.31, Oculus 1.8.0 | Mod ID, configuration keys, visibility caches, geometry invalidation and compatibility exclusions | Existing standalone `JEMAdaptiveCulling` |
| Unofficial Luxium Compatibility | `jem_luxium` | `JEMLuxium` | Client-only | Forge 47.4.10+, Minecraft 1.20.1, official unmodified Luxium 2.8.0-pre-alpha, Embeddium 0.3.31, Oculus 1.8.0 | None | Mod ID, mutual-exclusion state, restart persistence and managed-context reload behavior | Existing standalone `JEMLuxium` |

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

# Native versus JEM ownership

- 01-onboarding-and-multiplayer: JEMServer, JEMIdentity, native Server Login, Xaero.
- 02-economy-and-market: JEMEconomy; native merchant owns stock/restock/XP.
- 03-professions-and-races: JEMProfessions and JEMRoles, native TinyInv and Iron adapters.
- 04-weapons-armor-magic-and-combat: JEMWorldBossTiers combat contract; native Epic Fight/Iron/weapon mods.
- 05-bosses-world-tier-and-progression: JEMWorldBossTiers and JEMTwelveEyes; native summons/puzzles remain native.
- 06-settlements-villagers-and-workers: JEMSettlements; Recruits and Workers remain native entity owners.
- 07-mountables-entities-and-ai: JEMSettlements mount evidence; native entity taming/riding.
- 08-blocks-biomes-worldgen-and-builds: Native blocks/structures/dimensions; JEM campaign sites and protection adapters.
- 09-create-machinery-transport-and-native-mods: Create/native transport; JEMDrillLifecycle and isolated compatibility adapters.
- 10-ftb-quests-guides-and-events: JEMAchievementGuide, JEMServer events; FTB is guidance projection.
- 11-ui-visuals-shaders-and-audio: Client screens/renderers project authoritative server state.
- 12-home-inventory-persistence-and-network: JEMServer /home, TinyInv inventory, native AutoModpack generation.
- 13-performance-crashes-and-compatibility: Forge/JVM/native logs; JEM caches and bounded adapters.
- 14-other-legacy-and-cross-system: Catalog generator and changes; each module owns one gameplay fact.

Source-confirmed overlap: CoinMerchantMenu duplicates native notifyTrade stock mutation. Settlement storage transaction recovery and Atlas cross-registry merging require deeper review. No runtime behavior override has been applied to these systems tonight.

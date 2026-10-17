# Profession progression

## Sandbox-only integration — 2026-10-17

User asks to bring the existing test-server profession tree into `jem sandbox` and prefers the prior reference for cohesive wooden panels, icons and compact arrangement. Build `Mods/JEMProfessions` with `sandboxJar`: separate output `build/sandbox-profile/libs/jem-professions-1.0.0-sandbox.jar`. Default production profile retains its original radial screen and unlock behavior; its source screen moved byte-for-byte into src/production. No ordinary runtime JAR is replaced.

F7 or `/professions` opens the authoritative native snapshot in an integrated sandbox world. Source-owned JEMProfessions gameplay and canonical78node catalog/data remain owners. Puffish Skills0.19.2 owns XP, levels, points, unlocked skills and persistence. Existing real IdentityPolicy resolves the integrated owner UUID; the Identity1.0.0 already embedded in full Settlements satisfies it. No second XP counter, copied progression state or identity adapter is introduced.

Sandbox packaging reads canonical `Client/ManagedPack/config/paxi/datapacks/jem-profession-skills/data` and embeds native data resources into its own build output. It does not rewrite Client, Server or source graph. Missing Create/Iron item icons are presentation-mapped to recognizable Minecraft items through sandbox-icons.json; no whole Create/Iron distribution is added. Exact native Puffish and TinyInv1.1.0.1 plus its existing default config are installed only into sandbox. Paxi is not required because native SkillsMod resource reload reads the embedded `puffish_skills/config.json` namespace.

Availability is deliberately honest: seven existing core effects (ore_sense, vein_miner, vein_master, stone_step, lava_shield, cave_breath, double_catch) and TinyInv architect_hotbar are available. Other catalog entries lack active standalone effect owners; they are visibly unavailable rather than charging points for fictional gameplay. One sandbox availability resource feeds UI, server unlock admission and native packaged requirements. Sandbox packet admission checks support, actual native AFFORDABLE state, positive points and existing level/parent requirements. Native unavailable definitions require2147483647points; TinyInv node retains a real required_mods condition. Existing sandbox worlds may still retain historical native unlock state; no saves are rewritten. Production policy is not silently changed by this sandbox-specific correction.

The selected profession displays connected branches, semantic item nodes, live points/levels, native unlock state, readable effect/requirement text and a fixed detail panel. Black race-origin stage remains a different protected visual contract. Source/build evidence does not establish runtime appearance or gameplay acceptance. No tests or gameplay client were launched.

Exact artifacts, hashes, backups and sandbox target: [delivery](../settlements/standalone/sandbox-only-iteration-delivery.json).

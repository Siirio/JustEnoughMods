# Codex / web-research agent: expand the native-mechanics evidence layer

Goal: make the existing JustEnoughMods Atlas useful for real gameplay design, FTB Quests, new mod integrations and compatibility/logic analysis. **This is a repeatable research instruction, not permission to implement any mod or rewrite canon.**

## Mandatory input sequence

1. Read `AGENTS.md`, `Catalog/README.md`, `Catalog/research/README.md` and the canonical registry rules. Run the usual repository status checks if local access permits.
2. Run `python Catalog/generator/query_web_mechanics.py coverage`. This is the current installation-led worklist; work through every unresearched **content** mod, then libraries/infrastructure separately. Explicitly label `not applicable` for library-only mods rather than inventing gameplay.
3. For each mod, read exact mod/JAR version from `Catalog/manifest.json`, namespaced IDs from `Catalog/registries`, JEM changes from `Catalog/changes` and the exact config/source where present.
4. **Use real web search.** Prefer the current author's documentation, changelog for the exact Forge 1.20.1 version, official GitHub/wiki, and then labelled third-party community material. Do not trust generic SEO sites over native evidence; resolve contradictory statements. If web access is absent, do not fabricate sources or facts.
5. Collect author-documented feature loops, player controls, inputs/outputs, worldgen, biome/dimension restrictions, structure access, mob AI and combat counters, drops, crafting/machine usage, optional dependencies and third-party compatibility advisories. Cover **every distinct major gameplay system** of the mod, not just the first few search hits or item lists.
6. Record concise original paraphrases in `Catalog/research/web-mechanics/<mod_id>.json` with `source_ids`, `reviewed_on`, exact manifest version, `registry_refs` only where established, `depends_on` only where causally justified, and `verification: EXTERNAL_SOURCE_ONLY`, `runtime_verified: false`. Never copy external site text wholesale.
7. For each mechanic, suggest one action-oriented FTB Quest seed with observable completion; use `custom_criterion` when a standard FTB item task cannot prove the action. If no appropriate criterion exists, say so explicitly and make this a blocker.
8. Mark all version conflicts, unknown NBT/capabilities, unproven worldgen behavior, optional-mod absences and static registry false positives. Do not turn a project-page bug workaround into an actual JEM defect without checking installed versions and reproducing it.
9. Run the small **research data** validator `python Catalog/generator/query_web_mechanics.py audit --check-registry` as an explicit part of the research work, without launching Minecraft, Gradle, a general linter or broad project tests. Do not mutate stable profiles, Client/Server runtime targets or release baselines.
10. Update `Docs/catalog/WEB_MECHANICS_FIRST_PASS.md` (or a new dated coverage handoff) with exact researched mod IDs, source counts, still-unresearched mod IDs and important risk leads. Keep older verified research unless the installed version changed; annotate supersession instead of silently overwriting evidence.

## Prioritize discovery by actual player journeys

- **Exploration:** how to reach each biome/dimension/structure; what tools or conditions are required; how to find the entry point in Survival.
- **Creatures and bosses:** spawn conditions; tame/ride/breed behavior; meaningful enemy counters; spawn restrictions; loot; world/party/boss lifecycle interactions.
- **Machines and systems:** inputs, catalysts, operating conditions, controls, production outputs, failure states, mod integration.
- **Equipment and spells:** what they actually do, costs, cooldowns, mana, attack channels, durability, special actions; do not guess numeric runtime values.
- **Compatibility:** mandatory and optional mods, exact-version API/Forge/SMP packet restrictions, duplication routes, impossible progress gates, cross-mod worldgen tags, rendering/VFX conflicts.
- **FTB Quests:** teach one useful action per step, make optional discoveries genuinely optional, and do not turn a passive explanation into a fake completion check.

## Stop conditions / truth boundary

- A source cannot be opened or only covers a newer version: keep that feature `EXTERNAL_SOURCE_ONLY` with a version caveat, or omit the claim.
- An item ID cannot be proven: leave `registry_refs` empty and note the exact check required; never invent an ID.
- A quest action cannot be observed by installed FTB criteria: mark `custom_criterion`; do not substitute a generic checkmark to pretend it works.
- A runtime behavior depends on multiple mods or server config: verify against the effective JEM state before suggesting a fix.
- When all installed mod IDs have research entries, **coverage still does not equal correctness**. Runtime, code and actual gameplay remain separate verification stages.

## PR acceptance summary

Every batch should say: (1) which exact mod IDs and pack versions were researched; (2) number of source-backed mechanics and prerequisite edges; (3) official/community source URLs; (4) known unknowns; (5) validated research-data structure; (6) no false claims of in-game testing or complete Atlas coverage.

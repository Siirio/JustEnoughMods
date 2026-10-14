# JEM Power Balance System

This folder is the human entry point for combat balance. The machine-readable inventory is generated into `Catalog/views/power/`; native gameplay remains owned by Minecraft, installed mods and the existing JEM modules.

The system deliberately does not reduce combat to one score. It tracks damage, cadence, range/area, control, sustain, defense, resources, acquisition and progression separately. An unknown value is never treated as zero.

## Rebuild and query

From the repository root:

```text
python Catalog/generator/extract_power_mechanics.py
python Catalog/generator/build_power_views.py
python Catalog/generator/query_atlas.py power minecraft:diamond_sword
python Catalog/generator/query_atlas.py power "Ray of Siphoning"
python Catalog/generator/query_atlas.py power minecraft:wither
python Catalog/generator/query_atlas.py power recruit
```

Run the exact-artifact extractor first when installed JARs or native registrations change. It records owner classes, channel signals and only the small set of bytecode formulas it can reconstruct without guessing; the view generator then composes that evidence with JEM rules.

Each result follows the same chain:

`Original -> Modifiers -> JEM tweaks -> Effective -> Acquisition -> Assessment`

An exact registry/spell ID is preferred. A display-name fragment is accepted and may return several candidates. For stack-dependent entries such as `irons_spellbooks:scroll`, supply the spell ID and level stored in the actual stack; the carrier item ID alone is insufficient.

## Files and ownership

- `BALANCE_RULES.md`: approved player-facing rules and correction boundaries.
- `Weapons.md`, `Armor.md`, `Bosses.md`, `Mobs.md`, `Allies.md`: Phase 2 native-mechanics entry points by domain.
- `Balance.md`: confirmed source findings and the boundary before numeric balancing.
- `FORMULAS.md`: source-traced composition rules.
- `SCENARIOS.md`: the 16 reference scenarios and what can currently be calculated.
- `COVERAGE.md`: honest denominators and unresolved evidence.
- `CASE_STUDIES.md`: beam investigation and compact before/after decisions.
- `Catalog/power-sources/`: small exact-version evidence overlays where registry JSON cannot express bytecode-only facts. They cite artifact hashes and are not gameplay owners.
- `Catalog/views/power/`: regenerated inventory, expressions, warnings, scenarios and lookup index.

To request a tweak, identify the exact ID, scenario, observed value, World Tier, PvE/PvP context, relevant ItemStack data and desired player experience. The correction belongs in the native/config/data owner when possible; the power catalog then records the resulting expression and evidence.

## Safety boundary

The generator reads checked-in Atlas data, exact-version evidence and owning source. It never launches Minecraft, changes gameplay, rewrites configs, auto-nerfs entries or claims runtime verification. Warnings are review prompts, not automatic balance changes.

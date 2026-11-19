# Long-Term Ideas

This file is the canonical backlog for ideas that are not selected for the current update. An entry records intent only. It does not authorize source, configuration, pack, server, or release changes.

## Backlog rules

- Add every future long-term idea to this file
- Keep one entry per concept and update that entry instead of creating a second owner
- Record the user's intent separately from implementation proposals
- Do not treat an entry as approved work
- When the user selects an idea for an update, move the active specification to its owning design document and mark the backlog entry as promoted
- Keep a short link to the new owner after promotion

## Regional biome identity

Status: concept only

### User direction

Biomes should have stronger mechanical identity. Some resources and living things should be much more common in suitable biomes. Some hostile mobs and special mechanics can exist only in specific biomes.

Candidate applications include:

- Weight selected ores toward suitable geology and biome families
- Give fauna and flora clear regional distributions
- Make selected hostile mobs biome-exclusive
- Add biome-specific environmental or gameplay mechanics
- Use large probability differences when exclusivity is not suitable

### Design intent

Exploration should reveal different resources, threats, and opportunities instead of changing only scenery. The player should learn where to search and should have a reason to build routes between regions.

### Guardrails for later design

- Inspect installed world generation, biome tags, spawn rules, quests, structures, and existing worlds before selecting exact content
- Prefer source-backed biome tags and native mod rules over item or entity name matching
- Preserve unique native biome identities instead of flattening them into one JEM table
- Give the player an in-game way to discover important regional rules
- Do not make required progression depend on blind searching
- Decide existing-world behavior before changing generation or spawn distribution
- Record exact probabilities, exclusions, dimensions, heights, and fallback routes only when this idea is promoted

### Open design work

- Select the first ores, mobs, fauna, flora, and mechanics that need regional ownership
- Decide which entries are exclusive and which use probability weighting
- Define how Nature's Compass, maps, quests, villagers, or guide entries communicate the regions
- Measure travel cost after the transportation update is defined

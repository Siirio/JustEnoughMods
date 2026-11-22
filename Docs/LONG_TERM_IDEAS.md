# Long-Term Ideas

This file is the canonical backlog for ideas that are not selected for the current update. An entry records intent only. It does not authorize source, configuration, pack, server, or release changes.

## Backlog rules

- Add every future long-term idea to this file
- Keep one entry per concept and update that entry instead of creating a second owner
- Record the user's intent separately from implementation proposals
- Do not treat an entry as approved work
- When the user selects an idea for an update, move the active specification to its owning design document and mark the backlog entry as promoted
- Keep a short link to the new owner after promotion

## Multiplayer boss scaling without hosted groups

Status: concept only

### User direction

Ordinary bosses can become harder when several players attack them, but this update must not implement that scaling. Bosses must use native aggression without a hosted group, lobby, barrier, or combat-admission flow

### Design intent

Future scaling can answer the real number and strength of active attackers without restoring the removed arena architecture

### Guardrails for later design

- Derive active attackers from recent damage and current native boss targets
- Do not require a party, group, lobby, ready state, barrier, portal, or first-hit confirmation
- Apply scaling once from a stable encounter epoch and prevent retry or reload stacking
- Preserve single-player native boss values when only one player is active
- Remove scaling when the encounter ends or the boss is restored
- Keep Boss Raid participant and reward records separate from combat-stat ownership

### Open design work

- Select the attacker observation window
- Select health, damage, armor, and reinforcement curves
- Define join-late and leave-early behavior
- Define persistence and reload behavior
- Measure the result against every registered structural boss before promotion

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

## Distance-scaled structure loot

Status: concept only

### User direction

Structures found farther from world spawn should have better loot. The improvement must have a hard cap. Extreme coordinates must never create absurd rewards such as stacks of diamonds, netherite, or enchanted golden apples.

### Design intent

Long expeditions should have a stronger reward than repeated searches near spawn. Distance must improve the quality of an ordinary structure roll without replacing progression, boss rewards, or rare handcrafted loot tables.

### Guardrails for later design

- Use a bounded curve with an explicit maximum distance and maximum reward tier
- Cap both item value and rare-item quantity
- Keep structure identity relevant because a distant village chest and a distant endgame structure must not become equivalent
- Do not scale unique boss rewards, quest rewards, player containers, or generated containers outside the selected structure classes
- Decide how Nether and End coordinates map to the Overworld distance model
- Do not reroll or multiply already generated loot
- Provide deterministic Atlas-owned numbers before implementation

### Open design work

- Select eligible structure and loot-table classes
- Define the distance curve, cap distance, tier boundaries, and per-tier budget
- Define hard limits for diamonds, netherite materials, enchanted golden apples, and other exceptional items
- Decide whether the system uses loot-table conditions, structure metadata, or a server-owned adapter

## Climate-driven ice melting

Status: concept only

### User direction

Ice should melt in every non-snowy biome within one Minecraft day.

### Design intent

Permanent ice routes should belong to cold regions. Warm and temperate regions should require a different transport route or active maintenance.

### Guardrails for later design

- Use the canonical biome climate or biome tags instead of biome-name matching
- Specify whether the rule includes ice, packed ice, blue ice, frosted ice, and modded ice blocks
- Decide whether natural and player-placed ice use the same rule
- Define unloaded-chunk behavior without forcing chunk loads
- Specify whether melting creates water, air, or the block's native result in each dimension
- Preserve snowy-biome ice routes and do not change boat speed in this concept

### Open design work

- Select the exact cold-biome predicate and affected block tags
- Define how the one-Minecraft-day deadline is stored and resumed
- Inspect world generation, Create contraptions, waterlogging, dimensions, and existing ice-road use before implementation

# Approved Logic Locks

## LOCKED: Global World Tier 2 progression contract
- Scope: World tier derivation, encounter tier snapshots, reward tier lookup, legitimate progression, and rematch semantics in JEMWorldBossTiers
- Approved signal: "Approved. The architecture is correct" and "Lock these additional requirements before coding"
- Protected behavior: one global tier per world; thresholds 0/3/10/18/32; every enrolled production boss contributes exactly one unique defeat and scales with the encounter's World Tier snapshot, including Luxtructosaurus and all campaign subordinate bosses; explicitly balanced bosses retain their authored T1-T5 profiles while bosses without explicit attributes receive the shared health-and-damage curve; existing encounters retain their snapshot; rewards always use the current global tier; spawn eggs are legitimate first-kill provenance; `/kill` may finish an already validated first encounter; command spawning remains ineligible; rematches snapshot the current global tier and never count toward unique progression; ordinary JEM-owned rematches use one persisted 72,000 Overworld-game-tick due time while Raid replacement is immediate and separate; completed authoritative boss advancements reconcile immediately with login reconciliation retained as recovery; every online player receives the global tier advancement and later joiners synchronize to the current tier
- Reuse pattern: server-owned SavedData, persistent entity encounter data, data-driven profiles, event-driven updates, and narrow compatibility adapters
- Allowed changes: balancing data and compatibility adapters that preserve these invariants
- Unlock condition: explicit user request to change one of these invariants
- Created: 2026-09-15
- Updated: 2026-09-16 after explicit immediate-reconciliation and `/kill` testing clarification
- Updated: 2026-09-16 after the user required every enrolled boss to progress the world tier while retaining the three-boss Tier II threshold
- Updated: 2026-09-16 after the user clarified that the roster must include all distinct Bosses Rise, Cataclysm, and Legendary Monsters bosses
- Updated: 2026-09-17 after the user required every registered boss to both progress and scale, including Luxtructosaurus and campaign subordinate bosses
- Updated: 2026-09-21 after the user required every defeated registered arena in the Overworld, Nether, and End to schedule a non-progressing rematch after one Overworld Minecraft day without relocking historically completed prerequisites
- Updated: 2026-09-21 after the user required per-boss respawn ownership so native Cataclysm respawners, Cornelia's Shell Horn, native eggs, and any other declared native strategy are never duplicated by the JEM scheduler
- Updated: 2026-10-09 after the user superseded the one-day rule with exactly three Minecraft days, persisted the due time across restart, and separated ordinary scheduling from immediate Raid replacement

- Updated: 2026-09-25 after the user requested earlier Tier IV; only its unique-defeat threshold changed from 20 to 18, while Tier II remains 3, Tier III remains 10, Tier V remains 32, all existing defeat keys and schema 2 remain intact, and seven Luminous prerequisite native victory advancements now use the existing reconciliation path

## LOCKED: Progression enchantment cap
- Scope: Progression enchantment damage scaling and its player-facing description
- Approved signal: the user explicitly required progression only through Tier III and no gain from III to IV or IV to V
- Protected behavior: Tier I grants no bonus, Tier II grants 5%, Tier III through Tier V grant 10%, and the description explicitly states that later tier transitions add no power
- Reuse pattern: a single permanent rare enchantment whose bonus reads the global World Tier at damage time
- Allowed changes: localization and presentation fixes that preserve the cap
- Unlock condition: explicit user request to change the Progression curve
- Created: 2026-09-17

## LOCKED: Progression equipment durability and armor stacking
- Scope: Progression-enchanted ordinary equipment and registered boss equipment protection, armor eligibility, and overlapping armor defenses
- Approved signal: the user approved normal durability with working Mending and Unbreaking plus slightly layered armor defenses
- Protected behavior: eligible equipment keeps normal wear and may break from durability loss; dropped equipment is protected from fire, lava, despawn, and the void; ordinary ArmorItem equipment and explicitly allowlisted weak custom armor are eligible while boss rewards and strong functional armor are excluded; the strongest applicable Progression armor defense applies fully and each additional applicable defense contributes one quarter strength
- Reuse pattern: item tags for compatibility boundaries, normal vanilla durability, server-authoritative damage events, and multiplicative diminishing overlap
- Allowed changes: balance values and additional audited low-power armor entries that preserve the compatibility boundary
- Unlock condition: explicit user request to change durability, loss protection, armor eligibility, or overlap behavior
- Created: 2026-09-18

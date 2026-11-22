# Boss Raid

Current contract, 2026-11-22: Boss Raid records participation and completion around native structural boss combat. It does not own the combat lifecycle

## Eligibility and combat

- Raid eligibility still derives from a registered boss profile, historical legitimate defeat, and the registered structure arena
- The scheduled dedicated-server event reserves one compatible registered arena and binds its exact canonical boss UUID
- The boss keeps native aggression, damage, size, movement, attacks, summons, phases, loot, boss bar, and music
- No raid group, lobby, invitation, ready state, confirmation, retry, hosted Start, barrier, portal, teleport restriction, containment, or outsider combat gate exists
- No raid health, damage, size, reinforcement, or participant-count scaling is applied

## Participation records

The server adds a player to the raid record when damage from that player or an attributed owner reaches the event boss. The event stores the exact player UUID and contribution marker. On the boss's legitimate native death, the server commits the arena participation receipt before it closes the event and increments the raid completion records for those participants

Participation records do not grant exclusive damage or targeting rights. Other players can enter, leave, target, and be targeted through native behavior. Future multiplayer scaling remains a concept in `Docs/LONG_TERM_IDEAS.md`

## Structure repair

Raid acquisition and later structure repair use the same canonical arena identity and exact live boss UUID as ordinary combat. JEM never adopts a command, spawn-egg, tomb, or unrelated nearby boss as the arena owner

Blood Moon and Resource Rush are unchanged

## Verification status

The source compiles and retains durable participant and arena records. A later explicitly authorized multiplayer session must verify native combat and reward acceptance

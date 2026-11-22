# Boss Fight

Current contract, 2026-11-22: every registered structural boss uses its native combat flow. This contract replaces the retired hosted Boss Fight, Campaign combat, and ordinary arena lifecycle

## Native combat

- A structural boss can target players and receive damage as soon as its native mod permits
- Registered bosses treat non-spectator Creative players as valid native combat targets. The native selector, range, line of sight, team, attack-type, sleep, phase, and AI rules remain active
- JEM does not create a party, group, lobby, invitation, ready check, confirmation, retry, or first-hit admission flow
- JEM does not hold AI, invulnerability, boss bars, music, attacks, summons, or movement for an explicit Start action
- JEM does not add a visible wall, physical collision, portal, teleport restriction, safe-exit route, containment rule, or participant-only combat gate
- Campaign prerequisites can still control campaign progression, but they do not host or isolate combat

## Retained structure ownership

WorldBossTiers keeps the registered structure identity, verified native anchor, canonical live boss UUID, legitimate defeat history, and ordinary respawn schedule. Repair can restore only the exact boss UUID owned by that registered arena. It cannot adopt or recreate a boss that came from a command, spawn egg, tomb, or unregistered position

Old hosted lobby, group, attempt, support, and barrier fields are migration input only. Entity load removes them and restores native state. Retired barrier blocks are removed when their chunks load

Blood Moon and Resource Rush keep their separate territory, spectator, targeting, collision, and reward contracts

## Verification status

The Creative-targeting source has not received an authorized gameplay run. A later explicitly authorized player session must verify native aggression in Creative, death, revival, and overlapping structures

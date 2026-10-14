# Balance rules

## Player promise

Progression should feel powerful without erasing positioning, timing, preparation or counterplay. Early mistakes should be survivable when the attack is ordinary and readable. Legendary progression may be exceptional, but serious attacks must remain understandable and avoidable rather than becoming hidden one-shots or permanent invulnerability.

## Rules

1. Preserve native identity. Melee, magic, bosses, troops and mounts keep their own mechanics; JEM composes verified owners instead of flattening them into one universal multiplier.
2. Compare within a real scenario. Burst, sustained damage, range, area, control, healing, durability, mana/cooldown, acquisition and target mitigation all matter.
3. Unknown is not zero. Partial formulas stay symbolic and carry the evidence gap.
4. PvP is separate. Iron's Spells uses a JEM tier factor of exactly `1.00` against players; PvE tier factors must not leak into PvP conclusions.
5. Cooperation scales deliberately. Hosted bosses use participant and optional raid factors; troop strength also includes target access, AI uptime, congestion, cost and survival, not merely `one unit * count`.
6. Mercy is contextual, not hidden global adaptation. A weaker player may receive clear preparation/counterplay opportunities, but the system does not silently normalize every encounter to the player's gear.
7. Acquisition must match power. Easy, repeatable or cheap acquisition is evaluated against later rewards in the same role. Rarity alone is not proof; exact loot/config evidence is required.
8. Fix the owner. Prefer data/config/tag, native API/event, isolated adapter and only then a narrow Mixin. Never globally nerf every spell because one stack is suspicious.
9. Detection may be automatic; correction may not. Generated warnings identify evidence and the likely owner, but never rewrite gameplay without review.
10. Source review and gameplay observation are distinct evidence levels. `source_verified` does not mean player-tested or balanced.

## Evidence levels

- `atlas_static`: observed in installed resources; runtime registration or semantics may remain unknown.
- `source_verified`: exact owning JEM source/config establishes the rule.
- `exact_jar_bytecode`: exact installed third-party artifact establishes the rule.
- `runtime_observed`: a named scenario was actually observed; none was added by this task.

No entry is called fully resolved until its native expression, applicable modifiers, acquisition and representative target context are all known.
